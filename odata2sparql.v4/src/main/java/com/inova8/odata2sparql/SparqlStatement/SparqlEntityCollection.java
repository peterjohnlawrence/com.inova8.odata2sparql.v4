package com.inova8.odata2sparql.SparqlStatement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.XMLGregorianCalendar;

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
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfEdmType;

class SparqlEntityCollection extends EntityCollection {
	private final Logger log = LoggerFactory.getLogger(SparqlEntityCollection.class);
	private final Map<String, SparqlEntity> entitySetResultsMap = new HashMap<String, SparqlEntity>();
	private final Map<String, Map<String, List<Object>>> navPropertyResults = new HashMap<String, Map<String, List<Object>>>();
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

	public Map<String, Map<String, List<Object>>> getNavPropertyResults() {
		return navPropertyResults;
	}

	List<Map<String, Object>> getLinks(String entityKey, String navProperty) {
		ArrayList<Map<String, Object>> links = new ArrayList<Map<String, Object>>();

		if (navPropertyResults.containsKey(entityKey) && navPropertyResults.get(entityKey).containsKey(navProperty)) {
			for (int index = 0; index < navPropertyResults.get(entityKey).get(navProperty).size(); index++) {
				SparqlEntity navLink = (SparqlEntity) (navPropertyResults.get(entityKey).get(navProperty).get(index));
				HashMap<String, Object> link = new HashMap<String, Object>();
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
			navProperties = new HashMap<String, List<Object>>();
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


	private SparqlEntityCollection toSparqlEntityCollection(RdfEntityType rdfEntityType,RdfTripleSet results, ExpandOption expand, SelectOption select) throws EdmException {

		try {
			while (results.hasNext()) {
				RdfTriple triple = results.next();
				RdfNode subjectNode = triple.getSubject();
				RdfNode propertyNode = triple.getPredicate();
				RdfNode objectNode = triple.getObject();

				SparqlEntity rdfSubjectEntity = findOrCreateEntity(
						/* sparqlEdmProvider, rdfEntitiesMap, */ subjectNode, rdfEntityType);
				// this.getEntities().add(rdfSubjectEntity);
				if ((expand == null || expand.getExpandItems().isEmpty())
						&& (select == null || select.getSelectItems().isEmpty())) {
					rdfSubjectEntity.setEntityType(rdfEntityType);
				}
				if (objectNode.isIRI() || objectNode.isBlank()) {
					// Must be a navigation property pointing to an expanded entity
					if (propertyNode.getIRI().toString().equals(RdfConstants.ASSERTEDTYPE)) {
						rdfSubjectEntity
								.setEntityType(sparqlEdmProvider.getRdfModel().getOrCreateEntityType(objectNode));
					}
					RdfAssociation rdfAssociation = rdfSubjectEntity.getEntityType()
							.findNavigationProperty(propertyNode);
					// RdfAssociation rdfAssociation = navPropertiesMap.get(propertyNode.getIRI().toString());
					if (rdfAssociation != null) {
						//if (propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {
							//will only get here if rdfs_type in $expand
						//} else {
							// Locate which of the $expand this is related to
							SparqlEntity rdfObjectEntity = findOrCreateEntity(objectNode, rdfEntityType);

							rdfObjectEntity.setEntityType(rdfAssociation.getRangeClass());
							this.addNavPropertyObjectValues(rdfSubjectEntity.getSubject(),
									rdfAssociation.getEDMAssociationName(), rdfObjectEntity);
							// fixes #7 add to the Entity
							findOrCreateLink(rdfSubjectEntity, rdfAssociation, rdfObjectEntity);
						//}
					} else {
						// fixes #10 could be a datatypeProperty with a object (xrd:anyURI) as its value
						rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getIRI());
					}
					if (rdfSubjectEntity.getEntityType().isOperation()) {
						// An operation so need to use these as the primary key of the record.
						if (rdfSubjectEntity.getEntityType()
								.findNavigationProperty(propertyNode.getLocalName()) != null) {
							rdfSubjectEntity.addProperty(new Property(null,
									rdfSubjectEntity.getEntityType().findNavigationProperty(propertyNode.getLocalName())
											.getRelatedKey(),
									ValueType.PRIMITIVE, SparqlEntity.URLEncodeEntityKey(
											sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(objectNode,RdfConstants.QNAME_SEPARATOR))));
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
				} else {// Must be a property with a value, so put it into a
						// hashmap for processing the second time round when we
						// know the property
					rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getLiteralObject());
				}
			}
		} catch (OData2SparqlException e) {
			e.printStackTrace();
		}
		return this.build();
	}

	private void findOrCreateLink(SparqlEntity rdfSubjectEntity, RdfAssociation rdfAssociation,
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

	}

	private SparqlEntity findOrCreateEntity(RdfNode subjectNode, RdfEntityType rdfEntityType) {
		SparqlEntity rdfEntity;
		rdfEntity = entitySetResultsMap.get(sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode,RdfConstants.QNAME_SEPARATOR));
		if (rdfEntity == null) {
			rdfEntity = new SparqlEntity(subjectNode, sparqlEdmProvider.getRdfModel().getRdfPrefixes());
			// Only add at build time
			// this.getEntities().add(rdfEntity);
			entitySetResultsMap.put(sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode,RdfConstants.QNAME_SEPARATOR), rdfEntity);
		}
		return rdfEntity;
	}

	private SparqlEntityCollection build() {
		Iterator<Map.Entry<String, SparqlEntity>> entitySetResultsMapIterator = entitySetResultsMap.entrySet()
				.iterator();
		while (entitySetResultsMapIterator.hasNext()) {
			Entry<String, SparqlEntity> entitySetResultsMapEntry = entitySetResultsMapIterator.next();
			SparqlEntity rdfEntity = entitySetResultsMapEntry.getValue();
			// only leave target entities in collection, the rest will be
			// accessed via links
			if (rdfEntity.isTargetEntity())
				this.getEntities().add(rdfEntity);
			for (Entry<RdfNode, Object> entry : rdfEntity.getDatatypeProperties().entrySet()) {
				RdfNode propertyNode = entry.getKey();
				Object value = entry.getValue();
				RdfEntityType rdfSubjectEntityType = rdfEntity.getEntityType();
				if (rdfSubjectEntityType != null) {
					RdfProperty rdfProperty = null;
					if (rdfSubjectEntityType.isOperation()) {
						// test to make sure a objectproperty first?
						if (!propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {
							RdfAssociation rdfNavigationProperty = rdfSubjectEntityType
									.findNavigationProperty(RdfModel.rdfToOdata(propertyNode.getLocalName()));
							if (rdfNavigationProperty != null) {
								rdfProperty = rdfSubjectEntityType.findProperty(rdfNavigationProperty.getRelatedKey());
							} else {
								rdfProperty = rdfSubjectEntityType
										.findProperty(RdfModel.rdfToOdata(propertyNode.getLocalName()));
							}
							if (rdfProperty != null) {
								// rdfEntity.put(rdfProperty.propertyName,
								// Cast(value, rdfProperty.propertyTypeName));
								rdfEntity.addProperty(new Property(null, rdfProperty.propertyName, ValueType.PRIMITIVE,
										Cast(value, rdfProperty.propertyTypeName)));
							} else {
								log.error("Ignoring operation property statement that is not part of EDM:"
										+ propertyNode.getLocalName());
							}
						}
					} else {
						rdfProperty = rdfSubjectEntityType
								.findProperty(RdfModel.rdfToOdata(propertyNode.getLocalName()));
						if (rdfProperty != null) {
							// rdfEntity.put(rdfProperty.propertyName,
							// Cast(value, rdfProperty.propertyTypeName));
							rdfEntity.addProperty(new Property(null, rdfProperty.propertyName, ValueType.PRIMITIVE,
									Cast(value, rdfProperty.propertyTypeName)));
						} else {
							log.info("Ignoring property statement that is not part of EDM:"
									+ propertyNode.getLocalName());
						}
					}
				} else {
					log.error("Cannot get entityType of :" + rdfEntity.toString());
				}
			}
			// need to check if the RdfEntity, especially operation types are
			// complete with keys
			if (rdfEntity.getEntityType().isOperation()) {
				// Do you have all your keys defined. Olingo will fail otherwise
				for (RdfPrimaryKey primaryKey : rdfEntity.getEntityType().getPrimaryKeys()) {
					if (!rdfEntity.containsProperty(primaryKey.getPrimaryKeyName())) {
						// Delete rdfEntity because part of its key is missing
						entitySetResultsMapIterator.remove();
						break;
					}
				}
			}
		}
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
				return  new Timestamp( DatatypeConverter.parseDateTime(value.toString()).getTimeInMillis());  
			case "Decimal":
				return (BigDecimal) value;
			case "Double":
				if (value instanceof java.math.BigDecimal) {
					return ((BigDecimal) value).doubleValue();
				} else if (value instanceof Integer) {
					return ((Integer) value).doubleValue();
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
					return (String) value;
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
