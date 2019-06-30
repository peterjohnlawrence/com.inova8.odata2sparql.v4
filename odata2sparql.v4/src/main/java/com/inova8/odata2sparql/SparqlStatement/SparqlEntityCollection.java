package com.inova8.odata2sparql.SparqlStatement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
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
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexTypePropertyPair;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfEdmType;
import com.inova8.odata2sparql.Utils.RdfNodeComparator;

class SparqlEntityCollection extends EntityCollection {
	private final Logger log = LoggerFactory.getLogger(SparqlEntityCollection.class);
	private final Map<String, SparqlEntity> entitySetResultsMap = new TreeMap<String, SparqlEntity>();
	private final Map<String, Map<String, List<Object>>> navPropertyResults = new TreeMap<String, Map<String, List<Object>>>();
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

	//	public Map<String, SparqlEntity> getEntitySetResultsMap() {
	//		return entitySetResultsMap;
	//	}

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
				if (entity.getProperty(RdfConstants.SUBJECT).getValue().toString().equals(key))
					return entity;
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

	private void addNavPropertyObjectValues(String subject, String navigationPropertyName,
			SparqlEntity rdfObjectEntity) {

		List<Object> navPropertyObjectValues;
		Map<String, List<Object>> navProperties;

		if (!navPropertyResults.containsKey(subject)) {
			navProperties = new TreeMap<String, List<Object>>();
			navPropertyObjectValues = new ArrayList<Object>();
		} else {
			navProperties = navPropertyResults.get(subject);
			// navPropertyObjectValues = navProperties.get(associationName);
			if (!navProperties.containsKey(navigationPropertyName)) {
				navPropertyObjectValues = new ArrayList<Object>();
			} else {
				navPropertyObjectValues = navProperties.get(navigationPropertyName);
			}
		}
		navPropertyObjectValues.add(rdfObjectEntity);
		navProperties.put(navigationPropertyName, navPropertyObjectValues);
		navPropertyResults.put(subject, navProperties);
		// Mark as an expanded entity that should not be returned as part of the
		// entitySet results unless it is also a targetEntity
		rdfObjectEntity.setExpandedEntity(true);
	}

	private SparqlEntityCollection toSparqlEntityCollection(RdfEntityType rdfEntityType, RdfTripleSet results,
			ExpandOption expand, SelectOption select) throws EdmException {
		
		//First pass through data to assert types
		ArrayList<RdfTriple> allResults = new ArrayList<RdfTriple>();
		try {
			while (results.hasNext()) {
				RdfTriple triple = results.next();
				allResults.add(triple);
				RdfNode propertyNode = triple.getPredicate();
				String propertyNodeURI = propertyNode.getIRI().toString();
				if( propertyNodeURI.equals(RdfConstants.TARGETENTITY)) {
					// Mark any targetEntity so that recursive queries can be executed
					SparqlEntity rdfSubjectEntity = findOrCreateEntity( triple.getSubject());
					RdfEntityType targetEntityType = sparqlEdmProvider.getRdfModel().getOrCreateEntityType(triple.getObject());
					rdfSubjectEntity.assertTargetEntityType(targetEntityType);
				}else if( propertyNodeURI.equals(RdfConstants.ASSERTEDTYPE)) {
					SparqlEntity rdfSubjectEntity = findOrCreateEntity( triple.getSubject());
					rdfSubjectEntity.assertEntityType(sparqlEdmProvider.getRdfModel().getOrCreateEntityType(triple.getObject()));
				}else if( propertyNodeURI.equals(RdfConstants.ASSERTEDSHAPE)) {
					SparqlEntity rdfSubjectEntity = findOrCreateEntity( triple.getSubject());
					rdfSubjectEntity.assertEntityType(sparqlEdmProvider.getRdfModel().getOrCreateEntityTypeFromShape(triple.getObject()));
				}		
			}
		} catch (OData2SparqlException e) {
			e.printStackTrace();
		} finally {
			results.close();
		}
		//Second pass through data to assign data and object properties based on asserted types
		try {
			Iterator<RdfTriple> allResultsIterator = allResults.iterator();
			while (allResultsIterator.hasNext()) {
				RdfTriple triple = allResultsIterator.next();
				
				RdfNode subjectNode = triple.getSubject();
				RdfNode propertyNode = triple.getPredicate();
				RdfNode objectNode = triple.getObject();
				
				SparqlEntity rdfSubjectEntity = findOrCreateEntity(subjectNode);
				if (objectNode.isIRI() || objectNode.isBlank()) {
					// Must be a navigation property pointing to an expanded entity
					if (propertyNode.getIRI().toString().equals(RdfConstants.TARGETENTITY)) {
						//Already processed
					} else if (propertyNode.getIRI().toString().equals(RdfConstants.ASSERTEDTYPE)) {
						//Already processed		
					}  else if (propertyNode.getIRI().toString().equals(RdfConstants.ASSERTEDSHAPE)) {
						//Already processed
					}else if (propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {						
					
						SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode);
						this.addNavPropertyObjectValues(rdfSubjectEntity.getSubject(),
								RdfConstants.RDF_TYPE_EDMNAME, rdfObjectEntity);
						findOrCreateRdfTypeLink(rdfSubjectEntity,  rdfObjectEntity);
					} else if (propertyNode.getIRI().toString().equals(RdfConstants.MATCHING)) {
						//TODO add to local linkset
						SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode);
						if (!rdfObjectEntity.equals(rdfSubjectEntity)) {
							rdfSubjectEntity.addMatching(rdfObjectEntity);
							rdfObjectEntity.addMatching(rdfSubjectEntity);
						}
					} else {
						//TODO This all assumes that there is a defined entityType for the subjectEntity
						RdfNavigationProperty rdfNavigationProperty = rdfSubjectEntity.getEntityType()
								.findNavigationProperty(propertyNode);
						RdfComplexTypePropertyPair rdfComplexTypeProperty;
						if (rdfNavigationProperty != null) {
							//if (propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {
							//will only get here if rdfs_type in $expand
							//} else {
							// Locate which of the $expand this is related to
							SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode);
							if(!rdfObjectEntity.isTargetEntity()) {
								rdfObjectEntity.setEntityType(rdfNavigationProperty.getRangeClass());
							}
							this.addNavPropertyObjectValues(rdfSubjectEntity.getSubject(),
									rdfNavigationProperty.getEDMNavigationPropertyName(), rdfObjectEntity);
							// fixes #7 add to the Entity
							findOrCreateLink(rdfSubjectEntity, rdfNavigationProperty, rdfObjectEntity);
							//}
						} else if ((rdfComplexTypeProperty = rdfSubjectEntity.getEntityType()
								.findComplexProperty(propertyNode)) != null) {
							if (rdfComplexTypeProperty.isNavigationProperty()) {
								rdfNavigationProperty = rdfComplexTypeProperty.getRdfNavigationProperty();
								//Could be associated with a complexType's navigation property
								SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode);
								rdfObjectEntity.assertEntityType(rdfNavigationProperty.getRangeClass());
								// fixes #7 add to the Entity
								findOrCreateComplexLink(rdfSubjectEntity, rdfComplexTypeProperty, rdfObjectEntity);
							} else if (rdfComplexTypeProperty.isComplexProperty()) {
								SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode);
								rdfObjectEntity.assertEntityType(rdfComplexTypeProperty.getRdfComplexProperty().getRdfObjectPropertyShape().getPropertyNode().getEntityType());
								findOrCreateComplexLink(rdfSubjectEntity, rdfComplexTypeProperty, rdfObjectEntity);
							} else if (rdfComplexTypeProperty.isProperty()) {
								log.error("Should be handling rdfComplexTypePropertyy case: "
										+ rdfComplexTypeProperty.toString());
							}

						//} else if(false){
							// It could be that the subjectEntity appears twice with different entityTypes.

						} else {
							// fixes #10 could be a datatypeProperty with a object (xrd:anyURI) as its value
							rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getIRI());
						}
						if (rdfSubjectEntity.getEntityType().isOperation()) {
							// An operation so need to use these as the primary key of the record.
							if (rdfSubjectEntity.getEntityType().findNavigationProperty(propertyNode) != null) {
								rdfSubjectEntity.addProperty(new Property(null,
										rdfSubjectEntity.getEntityType().findNavigationProperty(propertyNode)
												.getRelatedKey(),
										ValueType.PRIMITIVE,
										SparqlEntity.URLEncodeEntityKey(sparqlEdmProvider.getRdfModel().getRdfPrefixes()
												.toQName(objectNode, RdfConstants.QNAME_SEPARATOR))));
							}
						}
						if (rdfNavigationProperty != null && rdfNavigationProperty.hasFkProperty()) {
							//Add QName of FK URI as literal property so that dumb BI tools can think in relational table terms
							rdfSubjectEntity.addProperty((new Property(null,
									rdfNavigationProperty.getFkProperty().getEDMPropertyName(), ValueType.PRIMITIVE,
									SparqlEntity.URLEncodeEntityKey(sparqlEdmProvider.getRdfModel().getRdfPrefixes()
											.toQName(objectNode, RdfConstants.QNAME_SEPARATOR)))));
						}
					}
				} else if (objectNode.isBlank()) {
					// Must be a navigation property pointing to an expanded
					// entity, but they should really be eliminated from the
					// query in the first place
				}  else if (propertyNode.getIRI().toString().startsWith(RdfConstants.COUNT)) {
					//Provides counted value hashed by subject/navigationPropertyName
					RdfNavigationProperty rdfNavigationProperty = rdfSubjectEntity.getEntityType()
							.findNavigationProperty(
									propertyNode.getIRI().toString().substring(RdfConstants.COUNT.length() + 1));
					//Fixes #77						counts.put(
					//							subjectNode.getIRI().toString()
					//									+ propertyNode.getIRI().toString().substring(RdfConstants.COUNT.length()),
					//							(Integer) objectNode.getLiteralObject());
					findOrCreateLinkCount(rdfSubjectEntity, rdfNavigationProperty,
							(Integer) objectNode.getLiteralObject());
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
		}
		//This should only be if matching is enabled
		this.mergeMatching();
		return this.build();
	}

	@SuppressWarnings("unchecked")
	private void findOrCreateComplexLink(SparqlEntity rdfSubjectEntity,
			RdfComplexTypePropertyPair rdfComplexTypeProperty, SparqlEntity rdfObjectEntity) {
		RdfNavigationProperty rdfNavigationProperty = rdfComplexTypeProperty.getRdfNavigationProperty();

		Property complexProperty = rdfSubjectEntity
				.getProperty(rdfComplexTypeProperty.getEquivalentComplexPropertyName());
		ComplexValue complexValue = null;
		if (complexProperty == null) {
			complexValue = new ComplexValue();
			complexProperty = new Property(null, rdfComplexTypeProperty.getEquivalentComplexPropertyName(),
					ValueType.COMPLEX, complexValue);
			rdfSubjectEntity.addProperty(complexProperty);
		} else {
			complexValue = (ComplexValue) complexProperty.getValue();
		}

		if (rdfComplexTypeProperty.isProperty()) {
			//Add property value
			log.error("Ignoring complextype property statement has datatype value:"
					+ rdfComplexTypeProperty.getRdfComplexType().getComplexTypeName() + " : "
					+ rdfComplexTypeProperty.getRdfProperty().getEDMPropertyName());
		} else if (rdfComplexTypeProperty.isNavigationProperty()) {

			RdfNavigationProperty complexNavigationProperty = rdfComplexTypeProperty.getRdfNavigationProperty();
			Link navigationLink = complexValue.getNavigationLink(complexNavigationProperty.getNavigationPropertyName());
			if (navigationLink == null) {
				navigationLink = new Link();
				navigationLink.setTitle(complexNavigationProperty.getNavigationPropertyName());
				if (navigationLink.getRel() == null)
					navigationLink.setRel("http://docs.oasis-open.org/odata/ns/related/"
							+ rdfNavigationProperty.getEDMNavigationPropertyName());
				complexValue.getNavigationLinks().add(navigationLink);
			}
			//TODO this should simply be the navigation  to the complex property, eg Employee('NWD~ContractEmployee-2')/employer
			navigationLink.setHref(rdfObjectEntity.getId().toString());

			if (rdfNavigationProperty.getDomainCardinality().equals(Cardinality.MANY)) {
				// to MANY, MULTIPLE
				EntityCollection inlineEntitySet = navigationLink.getInlineEntitySet();
				if (inlineEntitySet == null) {
					inlineEntitySet = new EntityCollection();
					navigationLink.setInlineEntitySet(inlineEntitySet);
				}
				inlineEntitySet.getEntities().add(rdfObjectEntity);
				Property valueProperty = null;
				for (Property property : complexValue.getValue()) {
					if (property.getName().equals(complexNavigationProperty.getNavigationPropertyName())) {
						valueProperty = property;
						((EntityCollection) valueProperty.getValue()).getEntities().add(rdfObjectEntity);
						break;
					}
				}
				if (valueProperty == null) {
					EntityCollection entityCollection = new EntityCollection();
					entityCollection.getEntities().add(rdfObjectEntity);
					valueProperty = new Property(null, complexNavigationProperty.getNavigationPropertyName(),
							ValueType.COLLECTION_ENTITY, entityCollection);
					complexValue.getValue().add(valueProperty);
				}
			} else {
				// to ONE
				navigationLink.setInlineEntity(rdfObjectEntity);
				Property valueProperty = new Property(null, complexNavigationProperty.getNavigationPropertyName(),
						ValueType.ENTITY, rdfObjectEntity);
				complexValue.getValue().add(valueProperty);
			}
		} else if (rdfComplexTypeProperty.isComplexProperty()) {
			RdfComplexProperty rdfComplexProperty = rdfComplexTypeProperty.getRdfComplexProperty();
			Link navigationLink = complexValue.getNavigationLink(rdfComplexProperty.getComplexPropertyName());
			if (navigationLink == null) {
				navigationLink = new Link();
				navigationLink.setTitle(rdfComplexProperty.getComplexPropertyName());
				if (navigationLink.getRel() == null)
					navigationLink.setRel("http://docs.oasis-open.org/odata/ns/related/"
							+ rdfComplexProperty.getComplexPropertyName());
				complexValue.getNavigationLinks().add(navigationLink);
			}
			//TODO this should simply be the navigation  to the complex property, eg Employee('NWD~ContractEmployee-2')/employer
			navigationLink.setHref(rdfObjectEntity.getId().toString());

			if (rdfComplexProperty.getCardinality().equals(Cardinality.MANY)) {
				// to MANY, MULTIPLE
				EntityCollection inlineEntitySet = navigationLink.getInlineEntitySet();
				if (inlineEntitySet == null) {
					inlineEntitySet = new EntityCollection();
					navigationLink.setInlineEntitySet(inlineEntitySet);
				}
				inlineEntitySet.getEntities().add(rdfObjectEntity);
				Property valueProperty = null;

				for (Property property : complexValue.getValue()) {
					if (property.getName().equals(rdfComplexProperty.getComplexPropertyName())) {
						valueProperty = property;
						break;
					}
				}
				if (valueProperty == null) {
					List<SparqlEntity> complexValueCollection = new ArrayList<SparqlEntity>();
					complexValueCollection.add(rdfObjectEntity);
					valueProperty = new Property(null, rdfComplexProperty.getComplexPropertyName(),
							ValueType.COLLECTION_COMPLEX, complexValueCollection);
					complexValue.getValue().add(valueProperty);
				} else {
					((List<SparqlEntity>) valueProperty.getValue()).add(rdfObjectEntity);
				}
			} else {
				// to ONE
				navigationLink.setInlineEntity(rdfObjectEntity);
				Property valueProperty = new Property(null, rdfComplexProperty.getComplexPropertyName(),
						ValueType.COMPLEX, rdfObjectEntity);
				complexValue.getValue().add(valueProperty);
			}
		}
	}

	private Link findOrCreateLink(SparqlEntity rdfSubjectEntity, RdfNavigationProperty rdfNavigationProperty,
			SparqlEntity rdfObjectEntity) {
		Link link = null;
		if (rdfSubjectEntity.getNavigationLinks() == null) {
			link = new Link();
			link.setTitle(rdfNavigationProperty.getEDMNavigationPropertyName());
			rdfSubjectEntity.getNavigationLinks().add(link);
		} else {
			for (Link searchLink : rdfSubjectEntity.getNavigationLinks()) {
				if (searchLink.getTitle().equals(rdfNavigationProperty.getEDMNavigationPropertyName())) {
					link = searchLink;
					break;
				}
			}
			if (link == null) {
				link = new Link();
				link.setTitle(rdfNavigationProperty.getEDMNavigationPropertyName());
				rdfSubjectEntity.getNavigationLinks().add(link);
			}
		}
		if (rdfNavigationProperty.getDomainCardinality().equals(Cardinality.MANY)) {
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
			link.setRel("http://docs.oasis-open.org/odata/ns/related/"
					+ rdfNavigationProperty.getEDMNavigationPropertyName());
		return link;
	}

	private Link findOrCreateRdfTypeLink(SparqlEntity rdfSubjectEntity, 
			SparqlEntity rdfObjectEntity) {
		 String edmNavigationPropertyName = RdfConstants.RDF_TYPE_EDMNAME;
		Link link = null;
		if (rdfSubjectEntity.getNavigationLinks() == null) {
			link = new Link();
			link.setTitle(edmNavigationPropertyName);
			rdfSubjectEntity.getNavigationLinks().add(link);
		} else {
			for (Link searchLink : rdfSubjectEntity.getNavigationLinks()) {
				if (searchLink.getTitle().equals(edmNavigationPropertyName)) {
					link = searchLink;
					break;
				}
			}
			if (link == null) {
				link = new Link();
				link.setTitle(edmNavigationPropertyName);
				rdfSubjectEntity.getNavigationLinks().add(link);
			}
		}
		EntityCollection inlineEntitySet = link.getInlineEntitySet();
		if (inlineEntitySet == null) {
			inlineEntitySet = new EntityCollection();
		}
		inlineEntitySet.getEntities().add(rdfObjectEntity);
		link.setInlineEntitySet(inlineEntitySet);
		// Required to make sure rel is not empty
		if (link.getRel() == null)
			link.setRel("http://docs.oasis-open.org/odata/ns/related/"
					+edmNavigationPropertyName);
		return link;
	}
	private Link findOrCreateLinkCount(SparqlEntity rdfSubjectEntity, RdfNavigationProperty rdfNavigationProperty,
			int count) {
		Link link = null;
		if (rdfSubjectEntity.getNavigationLinks() == null) {
			link = new Link();
			link.setTitle(rdfNavigationProperty.getEDMNavigationPropertyName());
			rdfSubjectEntity.getNavigationLinks().add(link);
		} else {
			for (Link searchLink : rdfSubjectEntity.getNavigationLinks()) {
				if (searchLink.getTitle().equals(rdfNavigationProperty.getEDMNavigationPropertyName())) {
					link = searchLink;
					break;
				}
			}
			if (link == null) {
				link = new Link();
				link.setTitle(rdfNavigationProperty.getEDMNavigationPropertyName());
				rdfSubjectEntity.getNavigationLinks().add(link);
			}
		}
		if (rdfNavigationProperty.getDomainCardinality().equals(Cardinality.MANY)) {
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
			link.setRel("http://docs.oasis-open.org/odata/ns/related/"
					+ rdfNavigationProperty.getEDMNavigationPropertyName());
		return link;
	}

	private SparqlEntity findOrCreateEntity(RdfNode subjectNode) {//, RdfEntityType rdfEntityType) {
		SparqlEntity rdfEntity;
		rdfEntity = entitySetResultsMap.get(
				sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode, RdfConstants.QNAME_SEPARATOR));
		if (rdfEntity == null) {
			rdfEntity = new SparqlEntity(subjectNode, sparqlEdmProvider.getRdfModel().getRdfPrefixes());
			// Only add at build time
			entitySetResultsMap.put(
					sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode, RdfConstants.QNAME_SEPARATOR),
					rdfEntity);
		}
		return rdfEntity;
	}

	private void mergeMatching() {
		Iterator<Map.Entry<String, SparqlEntity>> entitySetResultsMapIterator = entitySetResultsMap.entrySet()
				.iterator();
		while (entitySetResultsMapIterator.hasNext()) {
			Entry<String, SparqlEntity> entitySetResultsMapEntry = entitySetResultsMapIterator.next();
			SparqlEntity rdfEntity = entitySetResultsMapEntry.getValue();
			TreeMap<RdfNode, Object> mergedDatatypeProperties = new TreeMap<RdfNode, Object>(new RdfNodeComparator());
			ArrayList<Property> mergedProperties = new ArrayList<Property>();
			ArrayList<Link> mergedNavigationLinks = new ArrayList<Link>();
			for (SparqlEntity matchingEntity : rdfEntity.getMatching()) {
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

	private void resolveComplexProperties(SparqlEntity rdfEntity, List<Property> properties) {
		for (Property property : properties) {
			ValueType propertyValueType = property.getValueType();
			switch (propertyValueType) {
			case COMPLEX:
				Object complexValue = property.getValue();
				switch (complexValue.getClass().getName()) {
				case "com.inova8.odata2sparql.SparqlStatement.SparqlEntity":
					log.error("Should be handling COMPLEX case: " + complexValue.toString());
					break;
				default:
					resolveComplexProperties(rdfEntity, ((ComplexValue) complexValue).getValue());
					break;
				}
				break;

			case COLLECTION_COMPLEX:
				//Everything in the collection should be complex, if Entity then convert to complex val
				@SuppressWarnings("unchecked")
				List<Object> complexCollection = (List<Object>) property.getValue();
				for (ListIterator<Object> complexCollectionIterator = complexCollection
						.listIterator(); complexCollectionIterator.hasNext();) {
					Object complexCollectionElement = complexCollectionIterator.next();
					switch (complexCollectionElement.getClass().getName()) {
					case "com.inova8.odata2sparql.SparqlStatement.SparqlEntity":
						ComplexValue resolvedComplexValue;// = new ComplexValue();
						SparqlEntity sparqlEntity = (SparqlEntity) complexCollectionElement;
						resolvedComplexValue = (ComplexValue) sparqlEntity.getProperty(sparqlEntity.getEntityType().getEntityTypeName()).getValue();
						complexCollectionIterator.set(resolvedComplexValue);
						break;
					default:
						resolveComplexProperties(rdfEntity, ((ComplexValue) complexCollectionElement).getValue());
						break;
					}
				}
				break;

			default:
				break;
			}
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

			//resolveComplexProperties(rdfEntity, rdfEntity.getProperties());

			for (Entry<RdfNode, Object> entry : rdfEntity.getDatatypeProperties().entrySet()) {
				RdfNode propertyNode = entry.getKey();
				Object value = entry.getValue();
				RdfEntityType rdfSubjectEntityType = rdfEntity.getEntityType();
				if (rdfSubjectEntityType != null) {
					RdfProperty rdfProperty = null;
					String rdfPropertyLocalName = RdfModel.rdfToOdata(propertyNode.getLocalName());
					if (rdfSubjectEntityType.isOperation()) {
						// test to make sure a objectproperty first?
						if (!propertyNode.getIRI().toString().equals(RdfConstants.ASSERTEDTYPE) && !propertyNode.getIRI().toString().equals(RdfConstants.ASSERTEDSHAPE)
								&& !propertyNode.getIRI().equals(RdfConstants.RDF_TYPE)) {
							RdfNavigationProperty rdfNavigationProperty = rdfSubjectEntityType
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
						if (!propertyNode.getIRI().equals(RdfConstants.ASSERTEDTYPE) && !propertyNode.getIRI().toString().equals(RdfConstants.ASSERTEDSHAPE)
								&& !propertyNode.getIRI().equals(RdfConstants.RDF_TYPE)) {
							rdfProperty = rdfSubjectEntityType.findProperty(rdfPropertyLocalName);
							RdfComplexTypePropertyPair rdfComplexTypeProperty;
							if (rdfProperty != null) {

								rdfEntity.addProperty(new Property(null, rdfProperty.propertyName, ValueType.PRIMITIVE,
										Cast(value, rdfProperty.propertyTypeName)));
							} else if ((rdfComplexTypeProperty = rdfSubjectEntityType
									.findComplexProperty(rdfPropertyLocalName)) != null) {
								//It could be part of a complex property
								Property complexProperty = rdfEntity
										.getProperty(rdfComplexTypeProperty.getEquivalentComplexPropertyName());
								ComplexValue complexValue = null;
								if (complexProperty == null) {
									complexValue = new ComplexValue();
									complexProperty = new Property(null,
											rdfComplexTypeProperty.getEquivalentComplexPropertyName(),
											ValueType.COMPLEX, complexValue);
									rdfEntity.addProperty(complexProperty);
								} else {
									complexValue = (ComplexValue) complexProperty.getValue();
								}
								if (rdfComplexTypeProperty.isProperty()) {
									//Add property value
									Property valueProperty = new Property(null,
											rdfComplexTypeProperty.getRdfProperty().propertyName, ValueType.PRIMITIVE,
											Cast(value, rdfComplexTypeProperty.getRdfProperty().propertyTypeName));
									complexValue.getValue().add(valueProperty);
								} else if (rdfComplexTypeProperty.isNavigationProperty()) {
									//Build link to entity
									log.error("Ignoring navigationProperty statement: " + propertyNode.getLocalName());
								} else if (rdfComplexTypeProperty.isComplexProperty()) {
									//Build link to complex
									log.error("Ignoring complexProperty statement: " + propertyNode.getLocalName());
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
		entitySetResultsMapIterator = entitySetResultsMap.entrySet().iterator();
		while (entitySetResultsMapIterator.hasNext()) {
			Entry<String, SparqlEntity> entitySetResultsMapEntry = entitySetResultsMapIterator.next();
			SparqlEntity rdfEntity = entitySetResultsMapEntry.getValue();
			resolveComplexProperties(rdfEntity, rdfEntity.getProperties());
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
