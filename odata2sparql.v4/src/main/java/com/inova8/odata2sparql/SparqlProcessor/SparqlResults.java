package com.inova8.odata2sparql.SparqlProcessor;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;
import com.inova8.odata2sparql.RdfModel.RdfEntity;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfEdmType;

class SparqlResults {
	private final Log log = LogFactory.getLog(SparqlResults.class);
	private final Map<String, RdfEntity> entitySetResultsMap = new HashMap<String, RdfEntity>();
	private final Map<String, Map<String, List<Object>>> navPropertyResults = new HashMap<String, Map<String, List<Object>>>();
	private ArrayList<Map<String, Object>> entitySetResults;
	private EntityCollection entityCollection;

	// TODO clarification of expanded structure
	// Entityset dataproperty: List<Map<Subject, Map<Property,Value>>>
	// Expanded to first level: List<Map<Subject, Map<navProp, Map<Object, Map<Property,Value>>>>>
	// Expanded to second level: List<Map<Subject, Map<navProp, Map<Object, Map<navProp, Map<Object, Map<Property,Value>>>>>>>

	public SparqlResults() {
		super();
	}

	public Map<String, RdfEntity> getEntitySetResultsMap() {
		return entitySetResultsMap;
	}

	public List<Map<String, Object>> getEntitySetResults() throws ODataException {
		if (entitySetResults == null) {
			throw new ODataException("No data found");
		}
		return (List<Map<String, Object>>) entitySetResults;
	}

	public EntityCollection getEntityCollection() throws ODataException {
		if (entityCollection == null) {
			throw new ODataException("No data found");
		}
		return entityCollection;
	}

	public Entity getEntity() throws ODataException {
		if (entityCollection == null) {
			throw new ODataException("No data found");
		}
		return entityCollection.iterator().next();
	}
	public Map<String, Object> getEntityResults() {
		if (entitySetResults.isEmpty()) {
			return null;
		} else {
			return (Map<String, Object>) entitySetResults.get(0);
		}
	}

	public Map<String, Map<String, List<Object>>> getNavPropertyResults() {
		return navPropertyResults;
	}

	List<Map<String, Object>> getLinks(String entityKey, String navProperty) {
		ArrayList<Map<String, Object>> links = new ArrayList<Map<String, Object>>();

		if (navPropertyResults.containsKey(entityKey) && navPropertyResults.get(entityKey).containsKey(navProperty)) {
			for (int index = 0; index < navPropertyResults.get(entityKey).get(navProperty).size(); index++) {
				RdfEntity navLink = (RdfEntity) (navPropertyResults.get(entityKey).get(navProperty).get(index));
				HashMap<String, Object> link = new HashMap<String, Object>();
				//No point looking up Id property as we know it is the same as the subject
				link.put(RdfConstants.SUBJECT, RdfEntity.URLEncodeEntityKey(navLink.getSubject().toString()));
				links.add(link);
			}
		}

		return links;
	}

	SparqlResults build() {
		Iterator<Map.Entry<String, RdfEntity>> entitySetResultsMapIterator = entitySetResultsMap.entrySet().iterator();
		while (entitySetResultsMapIterator.hasNext()) {
			Entry<String, RdfEntity> entitySetResultsMapEntry = entitySetResultsMapIterator.next();
			RdfEntity rdfEntity = entitySetResultsMapEntry.getValue();
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
								rdfEntity.put(rdfProperty.propertyName, Cast(value, rdfProperty.propertyTypeName));
							} else {
								log.error("Ignoring operation property statement that is not part of EDM:"
										+ propertyNode.getLocalName());
							}
						}
					} else {
						rdfProperty = rdfSubjectEntityType
								.findProperty(RdfModel.rdfToOdata(propertyNode.getLocalName()));
						if (rdfProperty != null) {
							rdfEntity.put(rdfProperty.propertyName, Cast(value, rdfProperty.propertyTypeName));
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
					if (!rdfEntity.containsKey(primaryKey.getPrimaryKeyName())) {
						//Delete rdfEntity because part of its key is missing
						entitySetResultsMapIterator.remove();
						break;
					}
				}
			}
		}
		entitySetResults = new ArrayList<Map<String, Object>>();
		for (Map.Entry<String, RdfEntity> rdfEntityEntry : entitySetResultsMap.entrySet()) {
			RdfEntity rdfEntity = rdfEntityEntry.getValue();
			if (!rdfEntity.isExpandedEntity() || rdfEntity.isTargetEntity()) {
				entitySetResults.add((Map<String, Object>) rdfEntity);
			}
		}
		//TODO This seems overweight ... can the rdfEntity be cast?
		entityCollection = new EntityCollection();
		for (Map.Entry<String, RdfEntity> rdfEntityEntry : entitySetResultsMap.entrySet()) {
			RdfEntity rdfEntity = rdfEntityEntry.getValue();
			if (!rdfEntity.isExpandedEntity() || rdfEntity.isTargetEntity()) {
				entityCollection.getEntities().add(toEntity(rdfEntity));
			}
		}
		return this;
	}

	Entity toEntity(RdfEntity rdfEntity) {
		Entity entity = new Entity();
		Iterator it = rdfEntity.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			String propertyName = (String) pair.getKey();
			Object propertyValue = pair.getValue();
			entity.addProperty(new Property(null, propertyName, ValueType.PRIMITIVE, propertyValue.toString()));
		}
		entity.setId(createId(rdfEntity));
		return entity;
	}

	private URI createId(RdfEntity rdfEntity) {
		String id = "";
		for (RdfPrimaryKey primaryKey : rdfEntity.getEntityType().getPrimaryKeys()) {
			if (rdfEntity.containsKey(primaryKey.getPrimaryKeyName())) {
				id += rdfEntity.get(primaryKey.getPrimaryKeyName()).toString();
			}
		}
		try {
			return new URI(rdfEntity.getEntityType().entityTypeName + "(" + String.valueOf(id) + ")");
		} catch (URISyntaxException e) {
			throw new ODataRuntimeException("Unable to create id for entity: "
					+ rdfEntity.getEntityType().entityTypeName, e);
		}
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

	void addNavPropertyObjectValues(String subject, String associationName, RdfEntity rdfObjectEntity) {

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
	Map<String, Object> retrieveEntryResultsData(String subjectEntity, String navigationPropertyName) {
		if (!navPropertyResults.containsKey(subjectEntity)
				|| !navPropertyResults.get(subjectEntity).containsKey(navigationPropertyName)) {
			//if (navPropertyResults.get(subjectEntity) == null) {
			return null;
		} else {
			return (Map<String, Object>) navPropertyResults.get(subjectEntity).get(navigationPropertyName).get(0);
		}
	}

	@SuppressWarnings("unchecked")
	List<Map<String, Object>> retrieveFeedResultData(String subjectEntity, String navigationPropertyName) {
		ArrayList<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		if (navPropertyResults.containsKey(subjectEntity)
				&& navPropertyResults.get(subjectEntity).containsKey(navigationPropertyName)) {
			for (Object rdfEntity : navPropertyResults.get(subjectEntity).get(navigationPropertyName)) {
				//Check rdfEntity exists in results, if not it either returned no data or was removed because incomplete PK
				if (entitySetResultsMap.containsKey(((RdfEntity) rdfEntity).getSubject()))
					results.add((Map<String, Object>) rdfEntity);
			}
			return results;
		} else {
			return null;
		}
	}




}
