package com.inova8.odata2sparql.SparqlStatement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Comparator;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfTriple;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfTripleSet;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexTypePropertyPair;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfEdmType;

class SparqlEntityCollection extends EntityCollection {
	private final Logger log = LoggerFactory.getLogger(SparqlEntityCollection.class);
	private final Map<String, SparqlEntity> entitySetResultsMap = new TreeMap<String, SparqlEntity>();
	private final Map<String, Map<String, List<Object>>> navPropertyResults = new TreeMap<String, Map<String, List<Object>>>();
	//	private final Map<String, Integer> counts = new TreeMap<String, Integer>();
	private RdfEdmProvider sparqlEdmProvider;

	// Clarification of expanded structure
	// Entityset dataproperty: List<Map<Subject, Map<Property,Value>>>
	// Expanded to first level: List<Map<Subject, Map<navProp, Map<Object, Map<Property,Value>>>>>
	// Expanded to second level: List<Map<Subject, Map<navProp, Map<Object, Map<navProp, Map<Object, Map<Property,Value>>>>>>>

	SparqlEntityCollection(RdfEdmProvider sparqlEdmProvider, RdfEntityType entityType, RdfTripleSet results,
			ExpandOption expand, SelectOption select) {
		super();
		this.sparqlEdmProvider = sparqlEdmProvider;
		this.toSparqlEntityCollection(entityType, results, expand, select);
	}

	public Map<String, SparqlEntity> getEntitySetResultsMap() {
		return entitySetResultsMap;
	}

	public EntityCollection getEntityCollection() throws ODataException {

		return this;
	}

	public Entity getFirstEntity() throws ODataException {
		if (!this.getEntities().isEmpty()) {			
			return this.iterator().next();
		} else {
			return null;
		}
	}
	public Entity findEntity(String key) {
		if (!this.getEntities().isEmpty()) {
			for (Entity entity : this.getEntities()) {
				//TODO could be more efficient?
				if (entity.getProperty(RdfConstants.SUBJECT).getValue().toString().equals(key)) return entity;
			}
		} else {
			return null;
		}
		return null; 
 }
	public Map<String, Map<String, List<Object>>> getNavPropertyResults() {
		return navPropertyResults;
	}

	List<Map<String, Object>> getLinks(String entityKey, String navProperty) {
		ArrayList<Map<String, Object>> links = new ArrayList<Map<String, Object>>();

		if (navPropertyResults.containsKey(entityKey) && navPropertyResults.get(entityKey).containsKey(navProperty)) {
			for (int index = 0; index < navPropertyResults.get(entityKey).get(navProperty).size(); index++) {
				SparqlEntity navLink = (SparqlEntity) (navPropertyResults.get(entityKey).get(navProperty).get(index));
				TreeMap<String, Object> link = new TreeMap<String, Object>();
				// No point looking up Id property as we know it is the same as the subject
				link.put(RdfConstants.SUBJECT, SparqlEntity.URLEncodeEntityKey(navLink.getSubject().toString()));
				links.add(link);
			}
		}

		return links;
	}

	private void addNavPropertyObjectValues(String subject, String associationName, SparqlEntity rdfObjectEntity) {

		List<Object> navPropertyObjectValues;
		Map<String, List<Object>> navProperties;

		if (!navPropertyResults.containsKey(subject)) {
			navProperties = new TreeMap<String, List<Object>>();
			navPropertyObjectValues = new ArrayList<Object>();
		} else {
			navProperties = navPropertyResults.get(subject);
			// navPropertyObjectValues = navProperties.get(associationName);
			if (!navProperties.containsKey(associationName)) {
				navPropertyObjectValues = new ArrayList<Object>();
			} else {
				navPropertyObjectValues = navProperties.get(associationName);
			}
		}

		navPropertyObjectValues.add(rdfObjectEntity);
		navProperties.put(associationName, navPropertyObjectValues);
		navPropertyResults.put(subject, navProperties);
		// Mark as an expanded entity that should not be returned as part of the
		// entitySet results unless it is also a targetEntity
		rdfObjectEntity.setExpandedEntity(true);
	}

	private SparqlEntityCollection toSparqlEntityCollection(RdfEntityType rdfEntityType, RdfTripleSet results,
			ExpandOption expand, SelectOption select) throws EdmException {

		try {
			while (results.hasNext()) {
				RdfTriple triple = results.next();
				RdfNode subjectNode = triple.getSubject();
				RdfNode propertyNode = triple.getPredicate();
				RdfNode objectNode = triple.getObject();

				SparqlEntity rdfSubjectEntity = findOrCreateEntity(subjectNode);
// TODO maybe not required
//				if ((expand == null || expand.getExpandItems().isEmpty())
//						&& (select == null || select.getSelectItems().isEmpty())) {
//					rdfSubjectEntity.setEntityType(rdfEntityType);
//				}
				if (objectNode.isIRI() || objectNode.isBlank()) {
					// Must be a navigation property pointing to an expanded entity
					if (propertyNode.getIRI().toString().equals(RdfConstants.ASSERTEDTYPE)) {
						rdfSubjectEntity
								.setEntityType(sparqlEdmProvider.getRdfModel().getOrCreateEntityType(objectNode));
					} else if (propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {
						//TODO what can we use this for
						//rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getLiteralObject());
				
					} else if (propertyNode.getIRI().toString().equals(RdfConstants.MATCHING)) {
						//TODO add to local linkset
						SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode);
						if(!rdfObjectEntity.equals(rdfSubjectEntity)) {
								rdfSubjectEntity.addMatching(rdfObjectEntity);
								rdfObjectEntity.addMatching(rdfSubjectEntity);
						}							
					} else {
						RdfAssociation rdfAssociation = rdfSubjectEntity.getEntityType()
								.findNavigationProperty(propertyNode);
						RdfComplexTypePropertyPair rdfComplexTypeProperty;
						if (rdfAssociation != null) {
							//if (propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {
							//will only get here if rdfs_type in $expand
							//} else {
							// Locate which of the $expand this is related to
							SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode);

							rdfObjectEntity.setEntityType(rdfAssociation.getRangeClass());
							this.addNavPropertyObjectValues(rdfSubjectEntity.getSubject(),
									rdfAssociation.getEDMAssociationName(), rdfObjectEntity);
							// fixes #7 add to the Entity
							findOrCreateLink(rdfSubjectEntity, rdfAssociation, rdfObjectEntity);
							//}
						} else if ((rdfComplexTypeProperty = rdfSubjectEntity.getEntityType()
								.findComplexProperty(propertyNode)) != null) {
							rdfAssociation = rdfComplexTypeProperty.getRdfNavigationProperty();
							//Could be associated with a complexType's navigation property
							SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode);
							rdfObjectEntity.setEntityType(rdfAssociation.getRangeClass());
							//							this.addNavPropertyObjectValues(rdfSubjectEntity.getSubject(),
							//									rdfAssociation.getEDMAssociationName(), rdfObjectEntity);
							// fixes #7 add to the Entity
							findOrCreateComplexLink(rdfSubjectEntity, rdfComplexTypeProperty, rdfObjectEntity);

						} else {
							// fixes #10 could be a datatypeProperty with a object (xrd:anyURI) as its value
							rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getIRI());
						}
						if (rdfSubjectEntity.getEntityType().isOperation()) {
							// An operation so need to use these as the primary key of the record.
							//TODO propertyNode.getLocalName() is not the same as the navigationproperty
							if (rdfSubjectEntity.getEntityType().findNavigationProperty(propertyNode) != null) {
								rdfSubjectEntity.addProperty(new Property(null,
										rdfSubjectEntity.getEntityType().findNavigationProperty(propertyNode)
												.getRelatedKey(),
										ValueType.PRIMITIVE,
										SparqlEntity.URLEncodeEntityKey(sparqlEdmProvider.getRdfModel().getRdfPrefixes()
												.toQName(objectNode, RdfConstants.QNAME_SEPARATOR))));
							}
						}
						if (rdfAssociation.hasFkProperty()) {
							//Add QName of FK URI as literal property so that dumb BI tools can thing in relational table terms
							rdfSubjectEntity.addProperty((new Property(null,
									rdfAssociation.getFkProperty().getEDMPropertyName(), ValueType.PRIMITIVE,
									SparqlEntity.URLEncodeEntityKey(sparqlEdmProvider.getRdfModel().getRdfPrefixes()
											.toQName(objectNode, RdfConstants.QNAME_SEPARATOR)))));
						}
					}
				} else if (objectNode.isBlank()) {
					// Must be a navigation property pointing to an expanded
					// entity, but they should really be eliminated from the
					// query in the first place
				} else if (propertyNode.getIRI().toString().equals(RdfConstants.TARGETENTITY)) {
					// Mark any targetEntity so that recursive queries can be
					// executed
					rdfSubjectEntity.setTargetEntity(true);
				} else if (propertyNode.getIRI().toString().startsWith(RdfConstants.COUNT)) {
					//Provides counted value hashed by subject/navigationPropertyName
					RdfAssociation rdfAssociation = rdfSubjectEntity.getEntityType().findNavigationProperty(
							propertyNode.getIRI().toString().substring(RdfConstants.COUNT.length() + 1));
					//Fixes #77						counts.put(
					//							subjectNode.getIRI().toString()
					//									+ propertyNode.getIRI().toString().substring(RdfConstants.COUNT.length()),
					//							(Integer) objectNode.getLiteralObject());
					findOrCreateLinkCount(rdfSubjectEntity, rdfAssociation, (Integer) objectNode.getLiteralObject());
				} else if (rdfSubjectEntity.getEntityType().isOperation()
						&& (rdfSubjectEntity.getEntityType().findNavigationProperty(propertyNode) != null)) {
					//Fixes #81 OK this is an operation that is returning a literal for a primaryKey so  use UNDEF 
					rdfSubjectEntity.getDatatypeProperties().put(propertyNode, RdfConstants.UNDEFVALUE);

				} else {// Must be a property with a value, so put it into a
						// TreeMap for processing the second time round when we
						// know the property
					rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getLiteralObject());
				}
			}
		} catch (OData2SparqlException e) {
			e.printStackTrace();
		} finally {
			results.close();
		}
		//This should only be if matching is enabled
		this.mergeMatching();
		return this.build();
	}

	private void findOrCreateComplexLink(SparqlEntity rdfSubjectEntity,
			RdfComplexTypePropertyPair rdfComplexTypeProperty, SparqlEntity rdfObjectEntity) {
		RdfAssociation rdfAssociation = rdfComplexTypeProperty.getRdfNavigationProperty();

		Property complexProperty = rdfSubjectEntity
				.getProperty(rdfComplexTypeProperty.getRdfComplexType().getComplexTypeName());
		ComplexValue complexValue = null;
		if (complexProperty == null) {
			complexValue = new ComplexValue();
			complexProperty = new Property(null, rdfComplexTypeProperty.getRdfComplexType().getComplexTypeName(),
					ValueType.COMPLEX, complexValue);
			rdfSubjectEntity.addProperty(complexProperty);
		} else {
			complexValue = (ComplexValue) complexProperty.getValue();
		}

		if (rdfComplexTypeProperty.getRdfProperty() != null) {
			//Add property value
			//Problem!!!
		} else if (rdfComplexTypeProperty.getRdfNavigationProperty() != null) {

			RdfAssociation complexNavigationProperty = rdfComplexTypeProperty.getRdfNavigationProperty();
			Link navigationLink = complexValue.getNavigationLink(complexNavigationProperty.getAssociationName());
			if (navigationLink == null) {
				navigationLink = new Link();
				navigationLink.setTitle(complexNavigationProperty.getAssociationName());
				if (navigationLink.getRel() == null)
					navigationLink.setRel(
							"http://docs.oasis-open.org/odata/ns/related/" + rdfAssociation.getEDMAssociationName());
				complexValue.getNavigationLinks().add(navigationLink);
			}
//TODO this should simply be the navigation  to the complex property, eg Employee('NWD~ContractEmployee-2')/employer
			navigationLink.setHref(rdfObjectEntity.getId().toString());

			if (rdfAssociation.getDomainCardinality().equals(Cardinality.MANY)) {
				// to MANY, MULTIPLE
				EntityCollection inlineEntitySet = navigationLink.getInlineEntitySet();
				if (inlineEntitySet == null) {
					inlineEntitySet = new EntityCollection();
					navigationLink.setInlineEntitySet(inlineEntitySet);
				}
				inlineEntitySet.getEntities().add(rdfObjectEntity);
				Property valueProperty = null;
				for (Property property : complexValue.getValue()) {
					if (property.getName().equals(complexNavigationProperty.getAssociationName())) {
						valueProperty = property;
						((EntityCollection) valueProperty.getValue()).getEntities().add(rdfObjectEntity);
						break;
					}
				}
				if (valueProperty == null) {
					EntityCollection entityCollection = new EntityCollection();
					entityCollection.getEntities().add(rdfObjectEntity);
					valueProperty = new Property(null, complexNavigationProperty.getAssociationName(),
							ValueType.COLLECTION_ENTITY, entityCollection);
					complexValue.getValue().add(valueProperty);
				}
			} else {
				// to ONE
				navigationLink.setInlineEntity(rdfObjectEntity);
				Property valueProperty = new Property(null, complexNavigationProperty.getAssociationName(),
						ValueType.ENTITY, rdfObjectEntity);
				complexValue.getValue().add(valueProperty);
			}
		}
	}

	private Link findOrCreateLink(SparqlEntity rdfSubjectEntity, RdfAssociation rdfAssociation,
			SparqlEntity rdfObjectEntity) {
		Link link = null;
		if (rdfSubjectEntity.getNavigationLinks() == null) {
			link = new Link();
			link.setTitle(rdfAssociation.getEDMAssociationName());
			rdfSubjectEntity.getNavigationLinks().add(link);
		} else {
			for (Link searchLink : rdfSubjectEntity.getNavigationLinks()) {
				if (searchLink.getTitle().equals(rdfAssociation.getEDMAssociationName())) {
					link = searchLink;
					break;
				}
			}
			if (link == null) {
				link = new Link();
				link.setTitle(rdfAssociation.getEDMAssociationName());
				rdfSubjectEntity.getNavigationLinks().add(link);
			}
		}
		if (rdfAssociation.getDomainCardinality().equals(Cardinality.MANY)) {
			// to MANY, MULTIPLE
			EntityCollection inlineEntitySet = link.getInlineEntitySet();
			if (inlineEntitySet == null) {
				inlineEntitySet = new EntityCollection();
			}
			inlineEntitySet.getEntities().add(rdfObjectEntity);
			link.setInlineEntitySet(inlineEntitySet);
		} else {
			// to ONE
			link.setInlineEntity(rdfObjectEntity);
		}
		// Required to make sure rel is not empty
		if (link.getRel() == null)
			link.setRel("http://docs.oasis-open.org/odata/ns/related/" + rdfAssociation.getEDMAssociationName());
		return link;
	}

	private Link findOrCreateLinkCount(SparqlEntity rdfSubjectEntity, RdfAssociation rdfAssociation, int count) {
		Link link = null;
		if (rdfSubjectEntity.getNavigationLinks() == null) {
			link = new Link();
			link.setTitle(rdfAssociation.getEDMAssociationName());
			rdfSubjectEntity.getNavigationLinks().add(link);
		} else {
			for (Link searchLink : rdfSubjectEntity.getNavigationLinks()) {
				if (searchLink.getTitle().equals(rdfAssociation.getEDMAssociationName())) {
					link = searchLink;
					break;
				}
			}
			if (link == null) {
				link = new Link();
				link.setTitle(rdfAssociation.getEDMAssociationName());
				rdfSubjectEntity.getNavigationLinks().add(link);
			}
		}
		if (rdfAssociation.getDomainCardinality().equals(Cardinality.MANY)) {
			// to MANY, MULTIPLE
			EntityCollection inlineEntitySet = link.getInlineEntitySet();
			if (inlineEntitySet == null) {
				inlineEntitySet = new EntityCollection();
			}
			inlineEntitySet.setCount(count);
			link.setInlineEntitySet(inlineEntitySet);
		} else {
			// to ONE, therefore no count 
		}
		// Required to make sure rel is not empty
		if (link.getRel() == null)
			link.setRel("http://docs.oasis-open.org/odata/ns/related/" + rdfAssociation.getEDMAssociationName());
		return link;
	}

	private SparqlEntity findOrCreateEntity(RdfNode subjectNode) {//, RdfEntityType rdfEntityType) {
		SparqlEntity rdfEntity;
		rdfEntity = entitySetResultsMap.get(
				sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode, RdfConstants.QNAME_SEPARATOR));
		if (rdfEntity == null) {
			rdfEntity = new SparqlEntity(subjectNode, sparqlEdmProvider.getRdfModel().getRdfPrefixes());
			// Only add at build time
			// this.getEntities().add(rdfEntity);
			entitySetResultsMap.put(
					sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode, RdfConstants.QNAME_SEPARATOR),
					rdfEntity);
		}
		return rdfEntity;
	}
	private void  mergeMatching() {
		Iterator<Map.Entry<String, SparqlEntity>> entitySetResultsMapIterator = entitySetResultsMap.entrySet()
				.iterator();
		while (entitySetResultsMapIterator.hasNext()) {
			Entry<String, SparqlEntity> entitySetResultsMapEntry = entitySetResultsMapIterator.next();
			SparqlEntity rdfEntity = entitySetResultsMapEntry.getValue();	
			TreeMap<RdfNode, Object> mergedDatatypeProperties = new  TreeMap<RdfNode, Object>(
					new Comparator<RdfNode>() {
						@Override
						public int compare(RdfNode o1, RdfNode o2) {
							return o1.toString().compareTo(o2.toString());
						}
					}				
					);
			ArrayList<Property> mergedProperties = new  ArrayList<Property>();
			ArrayList<Link> mergedNavigationLinks = new  ArrayList<Link>();
			for( SparqlEntity matchingEntity: rdfEntity.getMatching()) {			
				mergedDatatypeProperties.putAll(matchingEntity.getDatatypeProperties());
				mergedProperties.addAll(matchingEntity.getProperties());
				mergedNavigationLinks.addAll(matchingEntity.getNavigationLinks());
			}	
			mergedDatatypeProperties.putAll(rdfEntity.getDatatypeProperties());
			mergedProperties.addAll(rdfEntity.getProperties());
			mergedNavigationLinks.addAll(rdfEntity.getNavigationLinks());
			rdfEntity.getDatatypeProperties().putAll(mergedDatatypeProperties);
			rdfEntity.getProperties().addAll(mergedProperties);
			rdfEntity.getNavigationLinks().addAll(mergedNavigationLinks);
		}
	}
	private SparqlEntityCollection build() {
		Iterator<Map.Entry<String, SparqlEntity>> entitySetResultsMapIterator = entitySetResultsMap.entrySet()
				.iterator();
		while (entitySetResultsMapIterator.hasNext()) {
			Entry<String, SparqlEntity> entitySetResultsMapEntry = entitySetResultsMapIterator.next();
			SparqlEntity rdfEntity = entitySetResultsMapEntry.getValue();
			
			
			// only leave target entities in collection, the rest will be
			// accessed via links
			if (rdfEntity.isTargetEntity()) {
				this.getEntities().add(rdfEntity);
			}

			//TODO Test openType
			//Property openvalue = new Property(null, "openproperty", ValueType.PRIMITIVE, "openvalue");
			//rdfEntity.addProperty(openvalue);

			//Fixes #77				for (Link navigationLink : rdfEntity.getNavigationLinks()) {
			//				Integer count = counts
			//						.get(rdfEntity.getSubjectNode().getIRI().toString() + "/" + navigationLink.getTitle());
			//				if ((count != null) && (navigationLink.getInlineEntitySet() != null))
			//					navigationLink.getInlineEntitySet().setCount(count);
			//			}
			for (Entry<RdfNode, Object> entry : rdfEntity.getDatatypeProperties().entrySet()) {
				RdfNode propertyNode = entry.getKey();
				Object value = entry.getValue();
				RdfEntityType rdfSubjectEntityType = rdfEntity.getEntityType();
				if (rdfSubjectEntityType != null) {
					RdfProperty rdfProperty = null;
					String rdfPropertyLocalName = RdfModel.rdfToOdata(propertyNode.getLocalName());
					if (rdfSubjectEntityType.isOperation()) {
						// test to make sure a objectproperty first?
						if (!propertyNode.getIRI().toString().equals(RdfConstants.ASSERTEDTYPE)
								&& !propertyNode.getIRI().equals(RdfConstants.RDF_TYPE)) {
							RdfAssociation rdfNavigationProperty = rdfSubjectEntityType
									.findNavigationProperty(rdfPropertyLocalName);
							if (rdfNavigationProperty != null) {
								rdfProperty = rdfSubjectEntityType.findProperty(rdfNavigationProperty.getRelatedKey());
							} else {
								rdfProperty = rdfSubjectEntityType.findProperty(rdfPropertyLocalName);
							}
							if ((rdfProperty != null)) {
								rdfEntity.addProperty(new Property(null, rdfProperty.propertyName, ValueType.PRIMITIVE,
										Cast(value, rdfProperty.propertyTypeName)));
							} else {
								log.error("Ignoring operation property statement that is not part of EDM:"
										+ propertyNode.getLocalName());
							}
						}
					} else {
						if (!propertyNode.getIRI().equals(RdfConstants.ASSERTEDTYPE)
								&& !propertyNode.getIRI().equals(RdfConstants.RDF_TYPE)) {
							rdfProperty = rdfSubjectEntityType.findProperty(rdfPropertyLocalName);
							RdfComplexTypePropertyPair rdfComplexTypeProperty;
							if (rdfProperty != null) {
								// rdfEntity.put(rdfProperty.propertyName,
								// Cast(value, rdfProperty.propertyTypeName));
								rdfEntity.addProperty(new Property(null, rdfProperty.propertyName, ValueType.PRIMITIVE,
										Cast(value, rdfProperty.propertyTypeName)));
							} else if ((rdfComplexTypeProperty = rdfSubjectEntityType
									.findComplexProperty(rdfPropertyLocalName)) != null) {
								//It could be part of a complex property
								Property complexProperty = rdfEntity
										.getProperty(rdfComplexTypeProperty.getRdfComplexType().getComplexTypeName());
								ComplexValue complexValue = null;
								if (complexProperty == null) {
									complexValue = new ComplexValue();
									complexProperty = new Property(null,
											rdfComplexTypeProperty.getRdfComplexType().getComplexTypeName(),
											ValueType.COMPLEX, complexValue);
									rdfEntity.addProperty(complexProperty);
								} else {
									complexValue = (ComplexValue) complexProperty.getValue();
								}
								if (rdfComplexTypeProperty.getRdfProperty() != null) {
									//Add property value
									Property valueProperty = new Property(null,
											rdfComplexTypeProperty.getRdfProperty().propertyName, ValueType.PRIMITIVE,
											Cast(value, rdfComplexTypeProperty.getRdfProperty().propertyTypeName));
									complexValue.getValue().add(valueProperty);
								} else if (rdfComplexTypeProperty.getRdfNavigationProperty() != null) {
									//Build link to entity
									//RdfAssociation complexNavigationProperty = rdfComplexTypeProperty.getRdfNavigationProperty();
									//									Link navigationLink = new Link();
									//									navigationLink.setHref(complexNavigationProperty.getRangeName() + "('" + sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(value.toString(), RdfConstants.QNAME_SEPARATOR) + "')");
									//									navigationLink.setTitle(complexNavigationProperty.getAssociationName());
									//									//navigationLink.setType(complexNavigationProperty.getRangeName());
									//									navigationLink.setRel("edit");
									//									navigationLink.setInlineEntity((SparqlEntity) value);
									//									complexValue.getNavigationLinks().add(navigationLink);
								}
							} else {
								log.info("Ignoring property statement that is not part of EDM: "
										+ propertyNode.getLocalName());
							}
						}
					}
				} else {
					log.error("Cannot get entityType of :" + rdfEntity.toString());
				}
			}
			// need to check if the RdfEntity, especially operation types are complete with keys
			if ((rdfEntity.getEntityType() != null) && (rdfEntity.getEntityType().isOperation())) {
				// Do you have all your keys defined. Olingo will fail otherwise
				for (RdfPrimaryKey primaryKey : rdfEntity.getEntityType().getPrimaryKeys()) {
					if (!rdfEntity.containsProperty(primaryKey.getPrimaryKeyName())) {
						// Delete rdfEntity because part of its key is missing
						//entitySetResultsMapIterator.remove();
						//break;
						//Fixes #75
						rdfEntity.addProperty(new Property(null, primaryKey.getPrimaryKeyName(), ValueType.PRIMITIVE,
								RdfConstants.UNDEFVALUE));
					}
				}
			}
		}

		//Fixes #77		this.setCount(counts.get(this.entityType.getIRI()));
		return this;
	}

	private Object Cast(Object value, String propertyTypeName) {
		EdmPrimitiveTypeKind propertyType = RdfEdmType.getEdmType(propertyTypeName);
		try {
			switch (propertyType.toString()) {
			case "Binary":
				return (Byte[]) value;
			case "Boolean":
				if (value instanceof java.lang.Boolean) {
					return value;
				} else if ((int) value == 0) {
					return false;
				} else if ((int) value != 0) {
					return true;
				}
				return null;
			case "Byte":
				return (Byte) value;
			case "Date":
				// EdmSimpleType instance =
				// org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance();
				// instance.valueOfString(value, EdmLiteralKind.JSON, null,
				// org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind.DateTime.getClass());
				return DatatypeConverter.parseDateTime(value.toString());
			case "DateTime":
				// EdmSimpleType instance =
				// org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance();
				// instance.valueOfString(value, EdmLiteralKind.JSON, null,
				// org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind.DateTime.getClass());
				return DatatypeConverter.parseDateTime(value.toString());
			case "DateTimeOffset":
				return new Timestamp(DatatypeConverter.parseDateTime(value.toString()).getTimeInMillis());
			case "Decimal":
				if (value instanceof java.math.BigDecimal) {
					return (BigDecimal) value;
				} else {
					return new BigDecimal(value.toString());
				}
			case "Double":
				if (value instanceof java.math.BigDecimal) {
					return ((BigDecimal) value).doubleValue();
				} else if (value instanceof Integer) {
					return ((Integer) value).doubleValue();
				} else if (value instanceof Float) {
					return ((Float) value).doubleValue();
				} else {
					return (Double) value;
				}
			case "Guid":
				return (UUID) value;
			case "Int16":
				if (value instanceof java.math.BigDecimal) {
					return ((BigDecimal) value).shortValue();
				} else {
					return (Short) value;
				}
			case "Int32":
				if (value instanceof java.math.BigDecimal) {
					return ((BigDecimal) value).intValue();
				} else {
					return (Integer) value;
				}
			case "Int64":
				if (value instanceof java.math.BigDecimal) {
					return ((BigDecimal) value).longValue();
				} else {
					return (Long) value;
				}
			case "SByte":
				return (Byte) value;
			case "Single":
				if (value instanceof java.math.BigDecimal) {
					return ((BigDecimal) value).toString();
				} else if (value instanceof java.math.BigInteger) {
					return ((BigInteger) value).toString();
				} else if (value instanceof Integer) {
					return ((Integer) value).toString();
				} else if (value instanceof javax.xml.datatype.XMLGregorianCalendar) {
					return ((XMLGregorianCalendar) value).toString();
				} else {
					return (String) value;
				}
			case "String":
				if (value instanceof java.math.BigDecimal) {
					return ((BigDecimal) value).toString();
				} else {
					return String.valueOf(value);
				}
			case "Time":
				return DatatypeConverter.parseTime(value.toString());
			default:
				return null;
			}
		} catch (Exception e) {
			log.error(value + " cannot be cast to " + propertyTypeName.toString());
		}
		return null;
	}
}
