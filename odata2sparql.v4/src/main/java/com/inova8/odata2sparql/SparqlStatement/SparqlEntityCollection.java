package com.inova8.odata2sparql.SparqlStatement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import com.inova8.odata2sparql.Constants.RdfConstants;
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
	private final Log log = LogFactory.getLog(SparqlEntityCollection.class);
	private final Map<String, SparqlEntity> entitySetResultsMap = new HashMap<String, SparqlEntity>();
	private final Map<String, Map<String, List<Object>>> navPropertyResults = new HashMap<String, Map<String, List<Object>>>();

	//	private final ArrayList<Map<String, Object>> entitySetResults;
	//	private EntityCollection entityCollection;

	// TODO clarification of expanded structure
	// Entityset dataproperty: List<Map<Subject, Map<Property,Value>>>
	// Expanded to first level: List<Map<Subject, Map<navProp, Map<Object, Map<Property,Value>>>>>
	// Expanded to second level: List<Map<Subject, Map<navProp, Map<Object, Map<navProp, Map<Object, Map<Property,Value>>>>>>>

	SparqlEntityCollection(RdfEdmProvider sparqlEdmProvider, RdfEntityType entityType, RdfTripleSet results,
			ExpandOption expand, SelectOption select) {
		super();

		this.toSparqlEntityCollection(sparqlEdmProvider, entityType, results, expand, select);
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
				//No point looking up Id property as we know it is the same as the subject
				link.put(RdfConstants.SUBJECT, SparqlEntity.URLEncodeEntityKey(navLink.getSubject().toString()));
				links.add(link);
			}
		}

		return links;
	}

	// TODO from UCDetector: Method "SparqlEntityCollection.retrieveEntryResultsData(String,String)" has 0 references
	//	private URI createId(SparqlEntity rdfEntity) {
	//		String id = "";
	//		for (RdfPrimaryKey primaryKey : rdfEntity.getEntityType().getPrimaryKeys()) {
	//			if (rdfEntity.containsProperty(primaryKey.getPrimaryKeyName())) {
	//				id += rdfEntity.getProperty(primaryKey.getPrimaryKeyName()).toString();
	//			}
	//		}
	//		try {
	//			return new URI(rdfEntity.getEntityType().entityTypeName + "(" + String.valueOf(id) + ")");
	//		} catch (URISyntaxException e) {
	//			throw new ODataRuntimeException("Unable to create id for entity: "
	//					+ rdfEntity.getEntityType().entityTypeName, e);
	//		}
	//	}

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
			case "DateTime":
				//EdmSimpleType instance = org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance();
				//instance.valueOfString(value, EdmLiteralKind.JSON, null, org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind.DateTime.getClass());
				return DatatypeConverter.parseDateTime(value.toString());
			case "DateTimeOffset":
				return (Calendar) value;
			case "Decimal":
				return (BigDecimal) value;
			case "Double":
				if (value instanceof java.math.BigDecimal) {
					return ((BigDecimal) value).doubleValue();
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
					return ((BigDecimal) value).floatValue();
				} else {
					return (Float) value;
				}
			case "String":
				return (String) value;
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

	private void addNavPropertyObjectValues(String subject, String associationName, SparqlEntity rdfObjectEntity) {

		List<Object> navPropertyObjectValues;
		Map<String, List<Object>> navProperties;

		if (!navPropertyResults.containsKey(subject)) {
			navProperties = new HashMap<String, List<Object>>();
			navPropertyObjectValues = new ArrayList<Object>();
		} else {
			navProperties = navPropertyResults.get(subject);
			//navPropertyObjectValues = navProperties.get(associationName);
			if (!navProperties.containsKey(associationName)) {
				navPropertyObjectValues = new ArrayList<Object>();
			} else {
				navPropertyObjectValues = navProperties.get(associationName);
			}
		}

		navPropertyObjectValues.add(rdfObjectEntity);
		navProperties.put(associationName, navPropertyObjectValues);
		navPropertyResults.put(subject, navProperties);
		//Mark as an expanded entity that should not be returned as part of the entitySet results unless it is also a targetEntity
		rdfObjectEntity.setExpandedEntity(true);
	}

	@SuppressWarnings("unchecked")
	// TODO from UCDetector: Method "SparqlEntityCollection.retrieveEntryResultsData(String,String)" has 0 references
	Map<String, Object> retrieveEntryResultsData(String subjectEntity, String navigationPropertyName) { // NO_UCD (unused code)
		if (!navPropertyResults.containsKey(subjectEntity)
				|| !navPropertyResults.get(subjectEntity).containsKey(navigationPropertyName)) {
			//if (navPropertyResults.get(subjectEntity) == null) {
			return null;
		} else {
			return (Map<String, Object>) navPropertyResults.get(subjectEntity).get(navigationPropertyName).get(0);
		}
	}

	@SuppressWarnings("unchecked")
	// TODO from UCDetector: Method "SparqlEntityCollection.retrieveFeedResultData(String,String)" has 0 references
	List<Map<String, Object>> retrieveFeedResultData(String subjectEntity, String navigationPropertyName) { // NO_UCD (unused code)
		ArrayList<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		if (navPropertyResults.containsKey(subjectEntity)
				&& navPropertyResults.get(subjectEntity).containsKey(navigationPropertyName)) {
			for (Object rdfEntity : navPropertyResults.get(subjectEntity).get(navigationPropertyName)) {
				//Check rdfEntity exists in results, if not it either returned no data or was removed because incomplete PK
				if (entitySetResultsMap.containsKey(((SparqlEntity) rdfEntity).getSubject()))
					results.add((Map<String, Object>) rdfEntity);
			}
			return results;
		} else {
			return null;
		}
	}

	private HashMap<String, RdfAssociation> buildNavPropertiesMap(RdfEdmProvider edmProvider,
	//TODO V2			List<ArrayList<NavigationPropertySegment>> expand, List<SelectItem> select) 
			ExpandOption expand, SelectOption select) throws EdmException {

		HashMap<String, RdfAssociation> navPropertiesMap = new HashMap<String, RdfAssociation>();
		if (expand != null) {
			//Add selected navigation properties even if not expanded.
			for (SelectItem selectItem : select.getSelectItems()) {
				//TODO V2
				//			for (NavigationPropertySegment navigationPropertySegment : selectItem.getNavigationPropertySegments()) {
				//				RdfAssociation rdfAssociation = edmProvider.getMappedNavigationProperty(new FullQualifiedName(
				//						navigationPropertySegment.getNavigationProperty().getRelationship().getNamespace(),
				//						navigationPropertySegment.getNavigationProperty().getRelationship().getName()));
				//				navPropertiesMap.put(rdfAssociation.getAssociationNodeIRI(), rdfAssociation);
				//			}
			}
		}
		if (expand != null) {
			//TODO V2
			//			for (ArrayList<NavigationPropertySegment> navigationPropertySegments : expand) {
			//				for (NavigationPropertySegment navigationPropertySegment : navigationPropertySegments) {
			//					RdfAssociation rdfAssociation = edmProvider.getMappedNavigationProperty(new FullQualifiedName(
			//							navigationPropertySegment.getNavigationProperty().getRelationship().getNamespace(),
			//							navigationPropertySegment.getNavigationProperty().getRelationship().getName()));
			//					navPropertiesMap.put(rdfAssociation.getAssociationNodeIRI(), rdfAssociation);
			//				}
			//			}
		}
		return navPropertiesMap;
	}

	private SparqlEntityCollection toSparqlEntityCollection(RdfEdmProvider sparqlEdmProvider,
			RdfEntityType rdfEntityType,
			//TODO V2			RdfTripleSet results, List<ArrayList<NavigationPropertySegment>> expand, List<SelectItem> select)
			RdfTripleSet results, ExpandOption expand, SelectOption select) throws EdmException {

		Map<String, SparqlEntity> rdfEntitiesMap = this.getEntitySetResultsMap();

		HashMap<String, RdfAssociation> navPropertiesMap = buildNavPropertiesMap(sparqlEdmProvider, expand, select);

		try {
			while (results.hasNext()) {
				RdfTriple triple = results.next();
				RdfNode subjectNode = triple.getSubject();
				RdfNode propertyNode = triple.getPredicate();
				RdfNode objectNode = triple.getObject();

				SparqlEntity rdfSubjectEntity = findOrCreateEntity(sparqlEdmProvider, rdfEntitiesMap, subjectNode,
						rdfEntityType);
				//this.getEntities().add(rdfSubjectEntity);
				if ((expand == null || expand.getExpandItems().isEmpty())
						&& (select == null || select.getSelectItems().isEmpty())) {
					rdfSubjectEntity.setEntityType(rdfEntityType);
				}
				if (objectNode.isIRI() || objectNode.isBlank()) {// Must be a navigation property pointing to an expanded entity
					if (propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {
						rdfSubjectEntity.setEntityType(sparqlEdmProvider.getRdfModel()
								.getOrCreateEntityType(objectNode));
					}
					RdfAssociation rdfAssociation = navPropertiesMap.get(propertyNode.getIRI().toString());
					if (rdfAssociation != null) {
						// Locate which of the $expand this is related to
						SparqlEntity rdfObjectEntity = findOrCreateEntity(sparqlEdmProvider, rdfEntitiesMap,
								objectNode, rdfEntityType);
						//this.getEntities().add(rdfObjectEntity);
						rdfObjectEntity.setEntityType(rdfAssociation.getRangeClass());
						this.addNavPropertyObjectValues(rdfSubjectEntity.getSubject(),
								rdfAssociation.getEDMAssociationName(), rdfObjectEntity);
						//TODO V2 add to the Entity  
						//						Link link = new Link();
						//						link.setTitle(rdfAssociation.getEDMAssociationName();
						//						link.setInlineEntity(expandEntity);
						//						entity.getNavigationLinks().add(link);
					}
					if (rdfSubjectEntity.getEntityType().isOperation()) {
						// An operation so need to use these as the primary key of the record.
						if (rdfSubjectEntity.getEntityType().findNavigationProperty(propertyNode.getLocalName()) != null) {
							rdfSubjectEntity.addProperty(new Property(null, rdfSubjectEntity.getEntityType()
									.findNavigationProperty(propertyNode.getLocalName()).getRelatedKey(),
									ValueType.PRIMITIVE, SparqlEntity.URLEncodeEntityKey(sparqlEdmProvider
											.getRdfModel().getRdfPrefixes().toQName(objectNode))));
							//							rdfSubjectEntity.put(
							//									rdfSubjectEntity.getEntityType()
							//											.findNavigationProperty(propertyNode.getLocalName()).getRelatedKey(), RdfEntity
							//											//.URLEncodeEntityKey(objectNode.toQName(sparqlEdmProvider.getRdfModel().getRdfPrefixes())));
							//											.URLEncodeEntityKey(sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(objectNode)));						}
						}
					}
				} else if (objectNode.isBlank()) {
					//Must be a navigation property pointing to an expanded entity, but they should really be eliminated from the query in the first place
				} else if (propertyNode.getIRI().toString().equals(RdfConstants.TARGETENTITY)) {
					//Mark any targetEntity so that recursive queries can be executed
					rdfSubjectEntity.setTargetEntity(true);
				} else {// Must be a property with a value, so put it into a hashmap for processing the second time round when we know the property
					rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getLiteralObject());
				}
			}
		} catch (OData2SparqlException e) {
			e.printStackTrace();
		}
		return this.build();
	}

	private SparqlEntity findOrCreateEntity(RdfEdmProvider sparqlEdmProvider, Map<String, SparqlEntity> rdfEntitiesMap,
			RdfNode subjectNode, RdfEntityType rdfEntityType) {
		SparqlEntity rdfEntity;
		rdfEntity = rdfEntitiesMap.get(sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode));
		if (rdfEntity == null) {
			rdfEntity = new SparqlEntity(subjectNode, sparqlEdmProvider.getRdfModel().getRdfPrefixes());
			this.getEntities().add(rdfEntity);
			rdfEntitiesMap.put(sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode), rdfEntity);
		}
		return rdfEntity;
	}

	private SparqlEntityCollection build() {
		Iterator<Map.Entry<String, SparqlEntity>> entitySetResultsMapIterator = entitySetResultsMap.entrySet()
				.iterator();
		while (entitySetResultsMapIterator.hasNext()) {
			Entry<String, SparqlEntity> entitySetResultsMapEntry = entitySetResultsMapIterator.next();
			SparqlEntity rdfEntity = entitySetResultsMapEntry.getValue();
			for (Entry<RdfNode, Object> entry : rdfEntity.getDatatypeProperties().entrySet()) {
				RdfNode propertyNode = entry.getKey();
				Object value = entry.getValue();
				RdfEntityType rdfSubjectEntityType = rdfEntity.getEntityType();
				if (rdfSubjectEntityType != null) {//TODO rdfEntity.getEntityType() failing or getting rdfs_Resource by mistake, not the correct type of the entity
					RdfProperty rdfProperty = null;
					if (rdfSubjectEntityType.isOperation()) {
						//test to make sure a objectproperty first?
						if (!propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {
							RdfAssociation rdfNavigationProperty = rdfSubjectEntityType.findNavigationProperty(RdfModel
									.rdfToOdata(propertyNode.getLocalName()));
							if (rdfNavigationProperty != null) {
								rdfProperty = rdfSubjectEntityType.findProperty(rdfNavigationProperty.getRelatedKey());
							} else {
								rdfProperty = rdfSubjectEntityType.findProperty(RdfModel.rdfToOdata(propertyNode
										.getLocalName()));
							}
							if (rdfProperty != null) {
								//rdfEntity.put(rdfProperty.propertyName, Cast(value, rdfProperty.propertyTypeName));				
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
							//rdfEntity.put(rdfProperty.propertyName, Cast(value, rdfProperty.propertyTypeName));
							rdfEntity.addProperty(new Property(null, rdfProperty.propertyName, ValueType.PRIMITIVE,
									Cast(value, rdfProperty.propertyTypeName)));
						} else {
							log.error("Ignoring property statement that is not part of EDM:"
									+ propertyNode.getLocalName());
						}
					}
				} else {
					log.error("Cannot get entityType of :" + rdfEntity.toString());
				}
			}
			// need to check if the RdfEntity, especially operation types are complete with keys
			if (rdfEntity.getEntityType().isOperation()) {
				//Do you have all your keys defined. Olingo will fail otherwise
				for (RdfPrimaryKey primaryKey : rdfEntity.getEntityType().getPrimaryKeys()) {
					if (!rdfEntity.containsProperty(primaryKey.getPrimaryKeyName())) {
						//Delete rdfEntity because part of its key is missing
						entitySetResultsMapIterator.remove();
						break;
					}
				}
			}
		}
		return this;
	}

}
