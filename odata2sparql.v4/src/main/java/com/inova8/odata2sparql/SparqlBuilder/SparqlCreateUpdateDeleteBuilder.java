/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;

import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;
import com.inova8.odata2sparql.uri.UriUtils;

/**
 * The Class SparqlCreateUpdateDeleteBuilder.
 */
public class SparqlCreateUpdateDeleteBuilder {
	
	/** The log. */
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(SparqlStatement.class);
	
	/** The rdf model. */
	private final RdfModel rdfModel;
	
	/** The rdf edm provider. */
	private final RdfEdmProvider rdfEdmProvider;
	
	/**
	 * Instantiates a new sparql create update delete builder.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 */
	//private final RdfEdmProvider rdfEdmProvider;
	public SparqlCreateUpdateDeleteBuilder(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
		this.rdfModel = this.rdfEdmProvider.getRdfModel();
		//	this.rdfEdmProvider = rdfEdmProvider;
	}

	/**
	 * Generate insert entity.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param entry the entry
	 * @return the sparql statement
	 * @throws ODataException the o data exception
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public SparqlStatement generateInsertEntity(RdfResourceParts rdfResourceParts, RdfEntityType entityType,
			Entity entry) throws ODataException,OData2SparqlException {

		if (entityType.isOperation()) {
			String insertText = entityType.getInsertText();
			if (insertText != null) {
				StringBuilder insertQuery = generateOperationFromTemplate(insertText, entityType, null, entry);
				return new SparqlStatement(insertQuery.toString());
			} else {
				throw new OData2SparqlException("No insertBody for insertQuery of " + entityType.entityTypeName, null);
			}
		} else {
			StringBuilder insertQuery = generateInsertProperties(rdfResourceParts, entityType, null, entry);
			return new SparqlStatement(insertQuery.toString());
		}
	}

	/**
	 * Generate operation from template.
	 *
	 * @param template the template
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param entry the entry
	 * @return the string builder
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private StringBuilder generateOperationFromTemplate(String template, RdfEntityType entityType,
			List<UriParameter> entityKeys, Entity entry) throws OData2SparqlException {
		return generateOperationFromTemplate(template, entityType, entityKeys, entry, null, null);
	}

	/**
	 * Generate operation from template.
	 *
	 * @param template the template
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param entry the entry
	 * @param property the property
	 * @param value the value
	 * @return the string builder
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private StringBuilder generateOperationFromTemplate(String template, RdfEntityType entityType,
			List<UriParameter> entityKeys, Entity entry, String property, Object value) throws OData2SparqlException {
		Pattern p = Pattern.compile("(##(INSERT|DELETE|UPDATE|UPDATEPROPERTY)VALUES(.*?)##).*?", Pattern.DOTALL);
		Matcher m = p.matcher(template);
		List<String> replacements = new ArrayList<String>();
		List<Integer> starts = new ArrayList<Integer>();
		List<Integer> ends = new ArrayList<Integer>();
		while (m.find()) {
			starts.add(m.start(1));
			ends.add(m.end(1));
			switch (m.group(2)) {
			case "INSERT":
				replacements.add(insertValuesReplace(m.group(3), entityType, entry));
				break;
			case "DELETE":
				replacements.add(deleteValuesReplace(m.group(3), entityType, entityKeys));
				break;
			case "UPDATEPROPERTY":
				replacements.add(updatePropertiesReplace(m.group(3), entityType, property, value));
				break;
			default:
				break;
			}
		}
		StringBuilder stringBuffer = new StringBuilder(template);
		for (int i = replacements.size() - 1; i >= 0; i--) {
			stringBuffer.replace(starts.get(i), ends.get(i), replacements.get(i));
		}
		return stringBuffer;
	}

	/**
	 * Delete values replace.
	 *
	 * @param group the group
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @return the string
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private String deleteValuesReplace(String group, RdfEntityType entityType, List<UriParameter> entityKeys)
			throws OData2SparqlException {
		String deleteReplace = group;
		for (UriParameter entityKey : entityKeys) {
			RdfProperty property;
			property = entityType.findProperty(entityKey.getName());//entityKeys.get(0).getText()
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKey.getText());
			deleteReplace = deleteReplace.replaceAll("\\?" + property.getVarName() + "\\b", "<" + expandedKey + ">");
		}
		deleteReplace = deleteReplace.replaceAll("\\?\\S+", "UNDEF");
		return deleteReplace;
	}

	/**
	 * Update properties replace.
	 *
	 * @param group the group
	 * @param entityType the entity type
	 * @param property the property
	 * @param value the value
	 * @return the string
	 */
	private String updatePropertiesReplace(String group, RdfEntityType entityType, String property, Object value) {
		String updatePropertiesReplace = group;
		RdfProperty rdfProperty = entityType.findProperty(property);
		updatePropertiesReplace = updatePropertiesReplace.replaceAll("\\?predicate",
				"<" + rdfProperty.getPropertyURI() + ">");
		if (value != null)
			updatePropertiesReplace = updatePropertiesReplace.replaceAll("\\?value", "'''" + value.toString() + "'''");
		else
			updatePropertiesReplace = updatePropertiesReplace.replaceAll("\\?value", "UNDEF");
		return updatePropertiesReplace;
	}

	/**
	 * Insert values replace.
	 *
	 * @param group the group
	 * @param entityType the entity type
	 * @param entry the entry
	 * @return the string
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private String insertValuesReplace(String group, RdfEntityType entityType, Entity entry)
			throws OData2SparqlException {
		String insertReplace = group;
		for (Property prop : entry.getProperties()) {
			if (prop.getValue() != null) {
				RdfProperty property = entityType.findProperty(prop.getName());
				if (property.getIsKey()) {
					insertReplace = insertReplace.replaceAll("\\?" + property.getVarName() + "\\b",
							"<" + this.rdfModel.getRdfPrefixes().expandPrefix(prop.getValue().toString()) + ">");
				} else {
					insertReplace = insertReplace.replaceAll("\\?" + property.getVarName() + "\\b",
							"\"" + prop.getValue().toString() + "\"");
				}
			}
		}
		//Replace any unmatched variables with UNDEF
		insertReplace = insertReplace.replaceAll("\\?\\S+", "UNDEF");
		return insertReplace;
	}

	/**
	 * Generate insert properties.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param entry the entry
	 * @return the string builder
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws ODataException the o data exception
	 */
	private StringBuilder generateInsertProperties(RdfResourceParts rdfResourceParts, RdfEntityType entityType,
			List<UriParameter> entityKeys, Entity entry) throws OData2SparqlException, ODataException {
		String entityName = rdfResourceParts.getEntitySet().getRdfEntityType().getEntityTypeName();
		if (rdfResourceParts.getNavPathString() != null) {
			entityName = entityName + rdfResourceParts.getLastNavPropertyName();//.getNavPathString();
		}
		entityType = rdfResourceParts.getResponseRdfEntityType();
		StringBuilder insertProperties = new StringBuilder();

		addDelete(entityName, insertProperties);

		insertProperties.append("INSERT {\n");
		addAdded(entityName, insertProperties);
		if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
			addChangeLogging(entityName, insertProperties);
		insertProperties.append("}\n");

		insertProperties.append("WHERE {\n");
		addInsertPropertyValues(entityType, entityKeys, entry, entityName, insertProperties, rdfResourceParts);
		addCurrentGraphQuery(entityName, insertProperties);

		if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
			addChangeLoggingParameters(entityName, insertProperties);

		insertProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<")
				.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<")
				.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl())
				.append(">) as ?addedGraph)\n");
		insertProperties.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");
		insertProperties.append("}\n");

		return insertProperties;
	}

	/**
	 * Generate update properties.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param entry the entry
	 * @return the string builder
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private StringBuilder generateUpdateProperties(RdfResourceParts rdfResourceParts, RdfEntityType entityType,
			List<UriParameter> entityKeys, Entity entry) throws OData2SparqlException {
		String entityName = rdfResourceParts.getEntitySet().getRdfEntityType().getEntityTypeName();
		if (rdfResourceParts.getNavPathString() != null) {
			entityName = entityName + rdfResourceParts.getLastNavPropertyName();//.getNavPathString();
		}
		entityType = rdfResourceParts.getResponseRdfEntityType();
		StringBuilder updateProperties = new StringBuilder();

		addDelete(entityName, updateProperties);

		updateProperties.append("INSERT {\n");
		addAdded(entityName, updateProperties);
		if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
			addChangeLogging(entityName, updateProperties);
		updateProperties.append("}\n");

		updateProperties.append("WHERE {\n");
		addUpdatePropertyValues(entityType, entityKeys, entry, entityName, updateProperties, rdfResourceParts);
		addCurrentGraphQuery(entityName, updateProperties);

		if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
			addChangeLoggingParameters(entityName, updateProperties);

		updateProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<")
				.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<")
				.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl())
				.append(">) as ?addedGraph)\n");
		updateProperties.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");
		updateProperties.append("}\n");

		return updateProperties;
	}

	/**
	 * Adds the current graph query.
	 *
	 * @param entityName the entity name
	 * @param insertProperties the insert properties
	 */
	protected void addCurrentGraphQuery(String entityName, StringBuilder insertProperties) {
		insertProperties.append("\tOPTIONAL{\n");
		insertProperties.append("\t\tGRAPH ?currentGraph \n");
		insertProperties.append("\t\t{\n");
		insertProperties.append("\t\t\t?").append(entityName).append("_s ?").append(entityName).append("_p ?")
				.append(entityName).append("_o .\n");
		insertProperties.append("\t\t} \n");
		insertProperties.append("\t}\n");
		insertProperties.append("\tBIND( IF(?").append(entityName).append("_o=?").append(entityName)
				.append("_no,FALSE,TRUE) as ?updated)\n");
		insertProperties.append("\tBIND(IF(BOUND(?updated),?updated,TRUE) as ?revisedUpdated)\n");
		insertProperties.append("\tFILTER(?revisedUpdated)\n");
	}

	/**
	 * Adds the current graph insert query.
	 *
	 * @param entityName the entity name
	 * @param insertProperties the insert properties
	 */
	protected void addCurrentGraphInsertQuery(String entityName, StringBuilder insertProperties) {
		insertProperties.append("\tOPTIONAL{\n");
		insertProperties.append("\t\tGRAPH ?currentGraph \n");
		insertProperties.append("\t\t{\n");
		insertProperties.append("\t\t\t?").append(entityName).append("_s ?").append(entityName).append("_p ?")
				.append(entityName).append("_no .\n");
		insertProperties.append("\t\t} \n");
		insertProperties.append("\t}\n");
		insertProperties.append("\tBIND( IF(?").append(entityName).append("_o=?").append(entityName)
				.append("_no,FALSE,TRUE) as ?updated)\n");
		insertProperties.append("\tBIND(IF(BOUND(?updated),?updated,TRUE) as ?revisedUpdated)\n");
		insertProperties.append("\tFILTER(?revisedUpdated)\n");
	}

	/**
	 * Adds the insert property values.
	 *
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param entity the entity
	 * @param entityName the entity name
	 * @param insertPropertyValues the insert property values
	 * @param rdfResourceParts the rdf resource parts
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws ODataException the o data exception
	 */
	protected void addInsertPropertyValues(RdfEntityType entityType, List<UriParameter> entityKeys, Entity entity,
			String entityName, StringBuilder insertPropertyValues, RdfResourceParts rdfResourceParts)
			throws OData2SparqlException, ODataException {

	String entityKey = (entityKeys != null)
				? entityKeys.get(0).getText().substring(1, entityKeys.get(0).getText().length() - 1)
				: null;
		String expandedKey = null;

		if (entityKeys != null && entityKeys.size() > 0) {
			//Use supplied key
			entityKey = entityKeys.get(0).getText().replace("'", "");
			expandedKey = rdfModel.getRdfPrefixes().expandPredicate(entityKey);
		} else {
			//First find key within properties
			expandedKey = findKey(entityType, entity);
		}
		if(expandedKey==null) throw new ODataException("Body must included subjectId of the new entity");
		
		//Add check to disable updates if key already exists
		//{FILTER NOT EXISTS {<http://northwind.com/ACategory> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://northwind.com/model/Category1>}}
		insertPropertyValues.append("{FILTER NOT EXISTS {<").append(expandedKey).append("> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + entityType.getURL()  +">}}\n");
		
		insertPropertyValues.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?")
		.append(entityName).append("_no){\n");	
		
		//Create insert for any navigation property that is included:
		if (rdfResourceParts.size() > 1) {
			//(<entityKey> <navigationproperty> <expandedKey>)
			String localKey = rdfResourceParts.getSubjectId();
			String expandedLocalKey = rdfModel.getRdfPrefixes().expandPredicate(localKey);
			EdmNavigationProperty navigationProperty = rdfResourceParts.getAsNavigationProperty(1)
					.getEdmNavigationProperty();
			String navigationPropertyKey = rdfResourceParts.getEntitySet().getRdfEntityType()
					.findNavigationPropertyByEDMNavigationPropertyName(navigationProperty.getName())
					.getNavigationPropertyIRI();
			insertPropertyValues.append("\t\t(<").append(expandedLocalKey).append("> <").append(navigationPropertyKey)
					.append("> <").append(expandedKey).append(">)\n");
		}

		buildEntityStatements(entityType, entity, insertPropertyValues, expandedKey);
		insertPropertyValues.append("\t}\n");
	}

	/**
	 * Find key.
	 *
	 * @param entityType the entity type
	 * @param entity the entity
	 * @return the string
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	protected String findKey(RdfEntityType entityType, Entity entity) throws OData2SparqlException {
		String entityKey;
		String expandedKey = null;
		for (Property prop : entity.getProperties()) {
			RdfProperty property = entityType.findProperty(prop.getName());
			if (property != null) {
				if (property.getIsKey()) {
					entityKey = prop.getValue().toString();
					expandedKey = rdfModel.getRdfPrefixes().expandPredicate(entityKey);
					UrlValidator urlValidator = new UrlValidator();
					if (!urlValidator.isValid(expandedKey)) {
						throw new OData2SparqlException(
								"Invalid key: " + entityKey + " for " + entityType.getEntityTypeName(), null);
					}
					break;
				}
			}
		}
		return expandedKey;
	}

	/**
	 * Builds the entity statements.
	 *
	 * @param entityType the entity type
	 * @param entity the entity
	 * @param insertPropertyValues the insert property values
	 * @param expandedKey the expanded key
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	protected void buildEntityStatements(RdfEntityType entityType, Entity entity, StringBuilder insertPropertyValues,
			String expandedKey) throws OData2SparqlException {
		//Now create VALUES statement for all dataproperties
		for (Property prop : entity.getProperties()) {
			RdfProperty property = entityType.findProperty(prop.getName());
			insertPropertyValues.append("\t\t(");
			if (property != null) {
				if (property.getIsKey()) {
					insertPropertyValues.append("<").append(expandedKey).append(
							"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + entityType.getURL() + ">");
				} else {
					insertPropertyValues.append("<").append(expandedKey)
							.append("> <" + property.getPropertyURI() + "> ");
					if (prop.getValue() != null) {
						insertPropertyValues.append(castObjectToXsd(prop.getValue()));
					} else {
						insertPropertyValues.append("UNDEF");
					}
				}
			} else {
				RdfNavigationProperty association = entityType
						.findNavigationPropertyByEDMNavigationPropertyName(prop.getName());
				insertPropertyValues.append("<").append(expandedKey)
						.append("> <" + association.getNavigationPropertyIRI() + "> ");
				insertPropertyValues.append(castObjectToXsd(prop.getValue()));
			}
			insertPropertyValues.append(")\n");
		}
		//Now create VALUES statement for all navigation property bindings
		for ( Link  navigationBinding : entity.getNavigationBindings()) {
			RdfNavigationProperty navigationProperty = entityType.findNavigationProperty(navigationBinding.getTitle());
			if (navigationProperty != null) {
				insertPropertyValues.append("\t\t(");
				insertPropertyValues.append("<").append(expandedKey)
						.append("> <" + navigationProperty.getNavigationPropertyIRI() + "> ");
				String expandedSubjectKey =UriUtils.objectToSubjectUri(navigationBinding.getBindingLink(), rdfModel.getRdfPrefixes());
				insertPropertyValues.append("<").append(expandedSubjectKey).append(">");
				insertPropertyValues.append(")\n");
			}
		}	
		//Now create VALUES statement for all navigation property links
		for ( Link  navigationLink : entity.getNavigationLinks()) {
			RdfNavigationProperty navigationProperty = entityType.findNavigationProperty(navigationLink.getTitle());
			if (navigationProperty != null) {

				if(navigationProperty.getDomainCardinality().equals(Cardinality.MANY)||navigationProperty.getDomainCardinality().equals(Cardinality.MULTIPLE)){
					//Iterate through array of entities
					for(Entity dependentEntity: navigationLink.getInlineEntitySet()) {
						buildDependentEntityStatements(insertPropertyValues, navigationProperty, dependentEntity);
					}
				}else {
					//Process single entity
					Entity dependentEntity=	navigationLink.getInlineEntity();
					buildDependentEntityStatements(insertPropertyValues, navigationProperty, dependentEntity);
				}
			}
		}
	}

	/**
	 * Builds the dependent entity statements.
	 *
	 * @param insertPropertyValues the insert property values
	 * @param navigationProperty the navigation property
	 * @param dependentEntity the dependent entity
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	protected void buildDependentEntityStatements(StringBuilder insertPropertyValues,
			RdfNavigationProperty navigationProperty, Entity dependentEntity) throws OData2SparqlException {
		String dependentKey = findKey(navigationProperty.getRangeClass(), dependentEntity);
		if(dependentKey!=null) {
			buildEntityStatements(navigationProperty.getRangeClass(),dependentEntity,insertPropertyValues,dependentKey);
		}else {
			log.error("Failed to locate dependent entitykey within dependent entity:"
					+ dependentEntity.toString());
			throw new EdmException("Failed to locate dependent entitykey within dependent entity:"
					+  dependentEntity.toString());
		}
	}

	/**
	 * Adds the update property values.
	 *
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param entry the entry
	 * @param entityName the entity name
	 * @param updatePropertyValues the update property values
	 * @param rdfResourceParts the rdf resource parts
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	protected void addUpdatePropertyValues(RdfEntityType entityType, List<UriParameter> entityKeys, Entity entry,
			String entityName, StringBuilder updatePropertyValues, RdfResourceParts rdfResourceParts)
			throws OData2SparqlException {
		updatePropertyValues.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?")
				.append(entityName).append("_no){\n");
		String entityKey = (entityKeys != null)
				? entityKeys.get(0).getText().substring(1, entityKeys.get(0).getText().length() - 1)
				: null;
		String expandedKey = null;
		String bodyKey = null;
		String expandedBodyKey = " UNDEF ";
		//Find body key
		for (Property prop : entry.getProperties()) {
			if (prop.getName().equals(RdfConstants.SUBJECT)) {
				bodyKey = prop.getValue().toString();
				expandedBodyKey = rdfModel.getRdfPrefixes().expandPredicate(bodyKey);
				UrlValidator urlValidator = new UrlValidator();
				if (!urlValidator.isValid(expandedBodyKey)) {
					throw new OData2SparqlException("Invalid key: " + bodyKey + " in body", null);
				}
				expandedBodyKey = " <" + expandedBodyKey + "> ";
				break;
			}
		}
		if ((rdfResourceParts.getUriType() == UriType.URI6B) && (bodyKey != null)) {
			if (!bodyKey.equals(rdfResourceParts.getTargetSubjectId()))
				throw new OData2SparqlException("Key in body, " + bodyKey + ", must match key in target: "
						+ rdfResourceParts.getTargetSubjectId(), null);
		}
		if (entityKeys != null && entityKeys.size() > 0) {
			//Use supplied key
			entityKey = entityKeys.get(0).getText().replace("'", "");
			expandedKey = rdfModel.getRdfPrefixes().expandPredicate(entityKey);
		} else {
			expandedKey = findKey(entityType, entry);
		}
		//Create insert for any navigation property that is included:

		//(<entityKey> <navigationproperty> <expandedKey>)
		expandedKey = " <" + expandedKey + "> ";
		String navigationPropertyKey = " UNDEF ";
		String expandedNavPropKey = expandedBodyKey;

		if (rdfResourceParts.getUriType() == UriType.URI6A || rdfResourceParts.getUriType() == UriType.URI6B) {

			EdmNavigationProperty edmNavigationProperty = rdfResourceParts.getAsNavigationProperty(1)
					.getEdmNavigationProperty();
			RdfNavigationProperty navProperty = rdfResourceParts.getEntitySet().getRdfEntityType()
					.findNavigationPropertyByEDMNavigationPropertyName(edmNavigationProperty.getName());
			navigationPropertyKey = " <" + navProperty.getNavigationPropertyIRI() + "> ";

			if (rdfResourceParts.getUriType() == UriType.URI6B) {
				String navPropKey = rdfResourceParts.getTargetSubjectId().replace("'", "");
				expandedNavPropKey = " <" + rdfModel.getRdfPrefixes().expandPredicate(navPropKey) + "> ";
			} else if (rdfResourceParts.getUriType() == UriType.URI6A) {

			}

			if (navProperty.IsInverse()) {
				String inverseNavigationPropertyKey = " <"
						+ navProperty.getInverseNavigationProperty().getNavigationPropertyIRI() + "> ";
				updatePropertyValues.append("\t\t(").append(expandedNavPropKey).append(inverseNavigationPropertyKey)
						.append(expandedKey).append(")\n");
			}
			updatePropertyValues.append("\t\t(").append(expandedKey).append(navigationPropertyKey)
					.append(expandedNavPropKey).append(")\n");
			//change because now the properties refer to the navigationEntity
			expandedKey = expandedNavPropKey;
		}

		//Now create VALUES statement for all properties
		for (Property prop : entry.getProperties()) {
			RdfProperty property = entityType.findProperty(prop.getName());
			updatePropertyValues.append("\t\t(");
			if (property != null) {
				if (property.getIsKey()) {
					updatePropertyValues.append(expandedKey)
							.append(" <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + entityType.getURL() + ">");
				} else {
					updatePropertyValues.append(expandedKey).append(" <" + property.getPropertyURI() + "> ");
					if (prop.getValue() != null) {
						updatePropertyValues.append(castObjectToXsd(prop.getValue()));
					} else {
						updatePropertyValues.append("UNDEF");
					}
				}
			} else {
				RdfNavigationProperty association = entityType
						.findNavigationPropertyByEDMNavigationPropertyName(prop.getName());
				updatePropertyValues.append("<").append(expandedKey)
						.append("> <" + association.getNavigationPropertyIRI() + "> ");
				updatePropertyValues.append(castObjectToXsd(prop.getValue()));
			}
			updatePropertyValues.append(")\n");
		}
		updatePropertyValues.append("\t}\n");
	}

	/**
	 * Adds the delete property values.
	 *
	 * @param expandedKey the expanded key
	 * @param entityName the entity name
	 * @param deleteProperties the delete properties
	 * @param rdfResourceParts the rdf resource parts
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	protected void addDeletePropertyValues(String expandedKey, String entityName, StringBuilder deleteProperties,
			RdfResourceParts rdfResourceParts) throws OData2SparqlException {
		deleteProperties.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?")
				.append(entityName).append("_o").append("){\n");
		//Create insert for any navigation property that is included:

		//(<entityKey> <navigationproperty> <expandedKey>)
		expandedKey = " <" + expandedKey + "> ";
		String navigationPropertyKey = " UNDEF ";
		String expandedNavPropKey = " UNDEF ";

		//URI1 entitySet
		//URI2 entity
		//URI6A entity/navigationProperty, so no entity allowed
		//URI6B entity/navigationEntitySet optionally with an entitykey specified

		if (rdfResourceParts.getUriType() == UriType.URI6A || rdfResourceParts.getUriType() == UriType.URI6B
				|| ((rdfResourceParts.getUriType() == UriType.URI2) && rdfResourceParts.size() > 1)) {
			EdmNavigationProperty edmNavigationProperty = rdfResourceParts.getAsNavigationProperty(1)
					.getEdmNavigationProperty();
			RdfNavigationProperty navProperty = rdfResourceParts.getEntitySet().getRdfEntityType()
					.findNavigationPropertyByEDMNavigationPropertyName(edmNavigationProperty.getName());
			navigationPropertyKey = " <" + navProperty.getNavigationPropertyIRI() + "> ";

			if (rdfResourceParts.getUriType() == UriType.URI6B || rdfResourceParts.getUriType() == UriType.URI2) {
				String navPropKey = rdfResourceParts.getTargetSubjectId().replace("'", "");
				expandedNavPropKey = " <" + rdfModel.getRdfPrefixes().expandPredicate(navPropKey) + "> ";
			}
			if (navProperty.IsInverse()) {
				String inverseNavigationPropertyKey = " <"
						+ navProperty.getInverseNavigationProperty().getNavigationPropertyIRI() + "> ";
				deleteProperties.append("\t\t(").append(expandedNavPropKey).append(inverseNavigationPropertyKey)
						.append(expandedKey).append(")\n");
			}
			deleteProperties.append("\t\t(").append(expandedKey).append(navigationPropertyKey)
					.append(expandedNavPropKey).append(")\n");

		} else {
			deleteProperties.append("\t\t(").append(expandedKey).append(" UNDEF ").append(" UNDEF ").append(")\n");
			//		deleteProperties.append("\t\t(").append(" UNDEF ").append(navigationPropertyKey).append(" UNDEF ").append(")\n");
			deleteProperties.append("\t\t(").append(" UNDEF ").append(" UNDEF ").append(expandedKey).append(")");
		}
		deleteProperties.append("\t}\n");
	}

	/**
	 * Adds the delete entity reference.
	 *
	 * @param expandedKey the expanded key
	 * @param entityName the entity name
	 * @param deleteProperties the delete properties
	 * @param rdfResourceParts the rdf resource parts
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	protected void addDeleteEntityReference(String expandedKey, String entityName, StringBuilder deleteProperties,
			RdfResourceParts rdfResourceParts) throws OData2SparqlException {
		deleteProperties.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?")
				.append(entityName).append("_o").append("){\n");
		expandedKey = " <" + expandedKey + "> ";

		if ((rdfResourceParts.getLastResourcePart() != null) && rdfResourceParts.getLastResourcePart()
				.getUriResourceKind().equals(UriResourceKind.navigationProperty)) {
			String expandedNavPropKey = " UNDEF ";
			RdfNavigationProperty navProperty = rdfResourceParts.getLastNavProperty();
			String navigationPropertyKey = " <" + navProperty.getNavigationPropertyIRI() + "> ";
			if (rdfResourceParts.getUriType() == UriType.URI6B || rdfResourceParts.getUriType() == UriType.URI2
					|| rdfResourceParts.getUriType() == UriType.URI7A) {
				if (rdfResourceParts.getTargetSubjectId() != null) {
					String navPropKey = rdfResourceParts.getTargetSubjectId().replace("'", "");
					expandedNavPropKey = " <" + rdfModel.getRdfPrefixes().expandPredicate(navPropKey) + "> ";
				}
			}
			if (navProperty.IsInverse()) {
				String inverseNavigationPropertyKey = " <"
						+ navProperty.getInverseNavigationProperty().getNavigationPropertyIRI() + "> ";
				deleteProperties.append("\t\t(").append(expandedNavPropKey).append(inverseNavigationPropertyKey)
						.append(expandedKey).append(")\n");
			}
			deleteProperties.append("\t\t(").append(expandedKey).append(navigationPropertyKey)
					.append(expandedNavPropKey).append(")\n");

		} else {
			deleteProperties.append("\t\t(").append(expandedKey).append(" UNDEF ").append(" UNDEF ").append(")\n");
			deleteProperties.append("\t\t(").append(" UNDEF ").append(" UNDEF ").append(expandedKey).append(")");
		}
		deleteProperties.append("\t}\n");
	}

	/**
	 * Adds the change logging parameters.
	 *
	 * @param entityName the entity name
	 * @param changeLoggingParameters the change logging parameters
	 */
	protected void addChangeLoggingParameters(String entityName, StringBuilder changeLoggingParameters) {
		changeLoggingParameters.append("\tBIND(NOW() as ?now)\n");
		changeLoggingParameters.append("\tBIND(IRI(CONCAT(\"")
				.append(rdfModel.getRdfRepository().getDataRepository().getChangeGraphUrl())
				.append("/C-\",SHA1(CONCAT(STR(?").append(entityName).append("_s))),\"-\",STR(?now))) as ?change)\n");
		changeLoggingParameters.append("\tBIND( IF(!?revisedUpdated,\"\",IF(BOUND(?").append(entityName)
				.append("_no),?change,?").append(entityName).append("_no )) as ?addChange)\n");
		changeLoggingParameters.append(
				"\tBIND(IF(!?revisedUpdated,\"\", IF(BOUND(?currentGraph),?change,?currentGraph)) as ?deleteChange)\n");
		changeLoggingParameters.append("\tBIND(IF(!?revisedUpdated,\"\",IRI(CONCAT(\"")
				.append(rdfModel.getRdfRepository().getDataRepository().getChangeGraphUrl())
				.append("/A-\",SHA1(CONCAT(STR(?addChange),STR(?").append(entityName).append("_s),STR(?")
				.append(entityName).append("_p))),\"-\",STR(?now)))) as ?addedChange)\n");
		changeLoggingParameters.append("\tBIND(IF(!?revisedUpdated,\"\",IRI(CONCAT(\"")
				.append(rdfModel.getRdfRepository().getDataRepository().getChangeGraphUrl())
				.append("/D-\",SHA1(CONCAT(STR(?deleteChange),STR(?").append(entityName).append("_s),STR(?")
				.append(entityName).append("_p))),\"-\",STR(?now)))) as ?deletedChange)\n");
	}

	/**
	 * Adds the added.
	 *
	 * @param entityName the entity name
	 * @param insert the insert
	 */
	protected void addAdded(String entityName, StringBuilder insert) {
		insert.append("\tGRAPH ?addedGraph\n");
		insert.append("\t{\n");
		insert.append("\t\t?").append(entityName).append("_s ?").append(entityName).append("_p ?").append(entityName)
				.append("_no\n");
		insert.append("\t}\n");
	}

	/**
	 * Adds the delete.
	 *
	 * @param entityName the entity name
	 * @param delete the delete
	 */
	protected void addDelete(String entityName, StringBuilder delete) {
		delete.append("DELETE {\n");
		delete.append("\tGRAPH ?deletedGraph\n");
		delete.append("\t{\n");
		delete.append("\t\t?").append(entityName).append("_s ?").append(entityName).append("_p ?").append(entityName)
				.append("_o\n");
		delete.append("\t}\n");
		delete.append("}\n");
	}

	/**
	 * Adds the change logging header.
	 *
	 * @param entityName the entity name
	 * @param changelogging the changelogging
	 */
	protected void addChangeLoggingHeader(String entityName, StringBuilder changelogging) {
		changelogging.append("\t##Changes\n");
		changelogging.append("\tGRAPH <").append(rdfModel.getRdfRepository().getDataRepository().getChangeGraphUrl())
				.append(">\n");
		changelogging.append("\t{?change a <http://inova8.com/odata4sparql#Change> ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#created>  ?now.\n");
	}

	/**
	 * Adds the change logging added change.
	 *
	 * @param entityName the entity name
	 * @param changelogging the changelogging
	 */
	protected void addChangeLoggingAddedChange(String entityName, StringBuilder changelogging) {
		changelogging.append("\t?change <http://inova8.com/odata4sparql#added> ?addedChange .\n");
		changelogging.append("\t?addedChange \n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#graph> ?addedGraph  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#subject> ?").append(entityName).append("_s  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#predicate>  ?").append(entityName).append("_p  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#object>  ?").append(entityName).append("_no .\n");
	}

	/**
	 * Adds the change logging deleted change.
	 *
	 * @param entityName the entity name
	 * @param changelogging the changelogging
	 */
	protected void addChangeLoggingDeletedChange(String entityName, StringBuilder changelogging) {
		changelogging.append("\t?change <http://inova8.com/odata4sparql#deleted> ?deletedChange .\n");
		changelogging.append("\t?deletedChange \n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#graph> ?deletedGraph  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#subject> ?").append(entityName).append("_s  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#predicate>  ?").append(entityName).append("_p  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#object>  ?").append(entityName).append("_o .\n");
	}

	/**
	 * Adds the deleted logging.
	 *
	 * @param entityName the entity name
	 * @param changelogging the changelogging
	 */
	protected void addDeletedLogging(String entityName, StringBuilder changelogging) {
		addChangeLoggingHeader(entityName, changelogging);
		addChangeLoggingDeletedChange(entityName, changelogging);
		changelogging.append("\t}\n");
	}

	/**
	 * Adds the added logging.
	 *
	 * @param entityName the entity name
	 * @param changelogging the changelogging
	 */
	protected void addAddedLogging(String entityName, StringBuilder changelogging) {
		addChangeLoggingHeader(entityName, changelogging);
		addChangeLoggingAddedChange(entityName, changelogging);
		changelogging.append("\t}\n");
	}

	/**
	 * Adds the change logging.
	 *
	 * @param entityName the entity name
	 * @param changelogging the changelogging
	 */
	protected void addChangeLogging(String entityName, StringBuilder changelogging) {
		addChangeLoggingHeader(entityName, changelogging);
		addChangeLoggingAddedChange(entityName, changelogging);
		addChangeLoggingDeletedChange(entityName, changelogging);
		changelogging.append("\t}\n");
	}

	/**
	 * Cast object to xsd.
	 *
	 * @param object the object
	 * @return the string
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private String castObjectToXsd(Object object) throws OData2SparqlException {
		switch (object.getClass().getName()) {
		case "Null":
			return "null";
		case "java.util.GregorianCalendar":
			return "\"" + ((java.util.GregorianCalendar) object).toZonedDateTime().toLocalDateTime().toString()
					+ "\"^^xsd:dateTime";
		case "Edm.DateTimeOffset":
		case "java.lang.String":
			return "'''" + object.toString() + "'''";
		case "Edm.Guid":
			return "guid\"" + object.toString() + "\"";
		case "Edm.Binary":
			return "X\"" + object.toString() + "\"";
		default:
			return "\"" + object.toString() + "\"";
		}
	}

	/**
	 * Generate delete entity.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @return the sparql statement
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public SparqlStatement generateDeleteEntity(RdfResourceParts rdfResourceParts, RdfEntityType entityType,
			List<UriParameter> entityKeys) throws OData2SparqlException {
		if (entityType.isOperation()) {
			String deleteText = entityType.getDeleteText();
			if (deleteText != null) {
				return new SparqlStatement(
						generateOperationFromTemplate(deleteText, entityType, entityKeys, null).toString());
			} else {
				throw new OData2SparqlException("No deleteBody for deleteQuery of " + entityType.entityTypeName, null);
			}
		} else {
			StringBuilder deleteEntity = new StringBuilder();

			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKeys.get(0).getText());
			String entityName = entityType.getEntityTypeName();
			addDelete(entityName, deleteEntity);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl()) {
				deleteEntity.append("INSERT {\n");
				addChangeLogging(entityName, deleteEntity);
				deleteEntity.append("}\n");
			}
			deleteEntity.append("WHERE {\n");
			addDeletePropertyValues(expandedKey, entityName, deleteEntity, rdfResourceParts);
			addCurrentGraphQuery(entityName, deleteEntity);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
				addChangeLoggingParameters(entityName, deleteEntity);

			deleteEntity.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl())
					.append(">) as ?addedGraph)\n");
			deleteEntity.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");
			deleteEntity.append("}\n");

			return new SparqlStatement(deleteEntity.toString());
		}
	}

	/**
	 * Generate delete entity reference.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @return the sparql statement
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public SparqlStatement generateDeleteEntityReference(RdfResourceParts rdfResourceParts, RdfEntityType entityType)
			throws OData2SparqlException {
		if (entityType.isOperation()) {
			//TODO
			return null;
		} else {
			StringBuilder deleteEntityReference = new StringBuilder();

			String expandedKey = rdfResourceParts.getValidatedSubjectIdUrl();
			String entityName = entityType.getEntityTypeName();

			entityName = entityName
					+ (rdfResourceParts.getLastPropertyName() != null ? rdfResourceParts.getLastPropertyName() : "");
			addDelete(entityName, deleteEntityReference);
			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl()) {
				deleteEntityReference.append("INSERT {\n");
				addDeletedLogging(entityName, deleteEntityReference);
				deleteEntityReference.append("}\n");
			}
			deleteEntityReference.append("WHERE {\n");

			addDeleteEntityReference(expandedKey, entityName, deleteEntityReference, rdfResourceParts);

			addCurrentGraphQuery(entityName, deleteEntityReference);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
				addChangeLoggingParameters(entityName, deleteEntityReference);

			deleteEntityReference.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl())
					.append(">) as ?addedGraph)\n");
			deleteEntityReference
					.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");
			deleteEntityReference.append("}\n");

			return new SparqlStatement(deleteEntityReference.toString());
		}
	}

	/**
	 * Generate update entity.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param entry the entry
	 * @return the sparql statement
	 * @throws Exception the exception
	 */
	public SparqlStatement generateUpdateEntity(RdfResourceParts rdfResourceParts, RdfEntityType entityType,
			List<UriParameter> entityKeys, Entity entry) throws Exception {
		if (entityType.isOperation()) {
			String updateText = entityType.getUpdateText();
			if (updateText != null) {
				return new SparqlStatement(
						generateOperationFromTemplate(updateText, entityType, entityKeys, entry).toString());
			} else {
				throw new OData2SparqlException("No updateBody for updateQuery of " + entityType.entityTypeName, null);
			}
		} else {
			StringBuilder sparql = new StringBuilder();
			sparql.append(generateUpdateProperties(rdfResourceParts, entityType, entityKeys, entry));
			return new SparqlStatement(sparql.toString());
		}
	}

	/**
	 * Generate update entity simple property value.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param property the property
	 * @param entry the entry
	 * @return the sparql statement
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public SparqlStatement generateUpdateEntitySimplePropertyValue(RdfResourceParts rdfResourceParts,
			RdfEntityType entityType, List<UriParameter> entityKeys, String property, Object entry)
			throws OData2SparqlException {
		if (entityType.isOperation()) {
			String updatePropertyText = entityType.getDeleteText();
			if (updatePropertyText != null) {
				return generateOperationUpdateEntitySimplePropertyValue(updatePropertyText, entityType, entityKeys,
						property, entry);
			} else {
				throw new OData2SparqlException(
						"No updatePropertyBody for updatePropertyQuery of " + entityType.entityTypeName, null);
			}
		} else {

			String entityName = entityType.getEntityTypeName();
			StringBuilder insertProperties = new StringBuilder();

			addDelete(entityName, insertProperties);
			insertProperties.append("INSERT {\n");
			addAdded(entityName, insertProperties);
			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
				addChangeLogging(entityName, insertProperties);
			insertProperties.append("}\n");

			insertProperties.append("WHERE {\n");

			String entityKey = rdfResourceParts.getTargetSubjectId();//entityKeys.get(0).getText();
			if (entityKey == null)
				entityKey = rdfResourceParts.getSubjectId();
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKey);
			String expandedProperty = entityType.findProperty(property).getPropertyURI();
			String value = entry.toString();

			insertProperties.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?")
					.append(entityName).append("_no){(");
			insertProperties.append("<").append(expandedKey).append(">");
			insertProperties.append(" <").append(expandedProperty).append(">");
			insertProperties.append(" '''" + value + "'''");
			insertProperties.append(")}\n");
			addCurrentGraphQuery(entityName, insertProperties);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
				addChangeLoggingParameters(entityName, insertProperties);

			insertProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl())
					.append(">) as ?addedGraph)\n");
			insertProperties
					.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");
			insertProperties.append("}\n");

			return new SparqlStatement(insertProperties.toString());
		}
	}

	/**
	 * Generate operation update entity simple property value.
	 *
	 * @param updatePropertyText the update property text
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param property the property
	 * @param entry the entry
	 * @return the sparql statement
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private SparqlStatement generateOperationUpdateEntitySimplePropertyValue(String updatePropertyText,
			RdfEntityType entityType, List<UriParameter> entityKeys, String property, Object entry)
			throws OData2SparqlException {
		return new SparqlStatement(generateOperationFromTemplate(entityType.getUpdatePropertyText(), entityType,
				entityKeys, null, property, entry).toString());
	}

	/**
	 * Generate delete entity simple property value.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param property the property
	 * @return the sparql statement
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public SparqlStatement generateDeleteEntitySimplePropertyValue(RdfResourceParts rdfResourceParts,
			RdfEntityType entityType, List<UriParameter> entityKeys, String property) throws OData2SparqlException {
		if (entityType.isOperation()) {
			String updatePropertyText = entityType.getUpdatePropertyText();
			if (updatePropertyText != null) {
				return generateOperationUpdateEntitySimplePropertyValue(updatePropertyText, entityType, entityKeys,
						property, null);
			} else {
				throw new OData2SparqlException(
						"No updatePropertyBody for updatePropertyQuery of " + entityType.entityTypeName, null);
			}
		} else {

			String entityKey = rdfResourceParts.getTargetSubjectId();//entityKeys.get(0).getText();
			if (entityKey == null)
				entityKey = rdfResourceParts.getSubjectId();
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKey);//rdfModel.getRdfPrefixes().expandPredicateKey(entityKeys.get(0).getText());

			String entityName = entityType.getEntityTypeName();
			String expandedProperty = entityType.findProperty(property).getPropertyURI();
			StringBuilder deleteProperty = new StringBuilder();

			addDelete(entityName, deleteProperty);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl()) {
				deleteProperty.append("INSERT {\n");
				addChangeLogging(entityName, deleteProperty);
				deleteProperty.append("}\n");
			}
			deleteProperty.append("WHERE {\n");

			deleteProperty.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p){(");
			deleteProperty.append("<").append(expandedKey).append(">");
			deleteProperty.append(" <").append(expandedProperty).append(">");
			deleteProperty.append(")}\n");

			addCurrentGraphQuery(entityName, deleteProperty);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
				addChangeLoggingParameters(entityName, deleteProperty);

			deleteProperty.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl())
					.append(">) as ?addedGraph)\n");
			deleteProperty.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");
			deleteProperty.append("}\n");
			return new SparqlStatement(deleteProperty.toString());

		}
	}

	/**
	 * Generate insert entity reference.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param entityKeys the entity keys
	 * @param navigationProperty the navigation property
	 * @param requestEntityReferences the request entity references
	 * @return the sparql statement
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public SparqlStatement generateInsertEntityReference(RdfResourceParts rdfResourceParts, RdfEntityType entityType,
			List<UriParameter> entityKeys, RdfNavigationProperty navigationProperty, List<URI> requestEntityReferences)
			throws OData2SparqlException {

		if (entityType.isOperation()) {
			//TODO
			return null;
		} else {
			String entityName = entityType.getEntityTypeName();
			StringBuilder insertProperties = new StringBuilder();

			insertProperties.append("INSERT {\n");
			addAdded(entityName, insertProperties);
			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl()) {
				addChangeLoggingHeader(entityName, insertProperties);
				addChangeLoggingAddedChange(entityName, insertProperties);
				insertProperties.append("\t}\n");
			}
			insertProperties.append("}\n");

			insertProperties.append("WHERE {\n");
			String expandedKey = rdfResourceParts.getValidatedSubjectIdUrl();
			String expandedProperty = navigationProperty.getNavigationPropertyIRI();
			String expandedInverseProperty = navigationProperty.getInversePropertyOfURI();

			insertProperties.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?")
					.append(entityName).append("_no){\n");

			for (URI requestEntityReference : requestEntityReferences) {
				String key = getExpandedLinkEntitykey(requestEntityReference);
				insertProperties.append("\t\t(<").append(expandedKey).append(">");
				insertProperties.append(" <").append(expandedProperty).append(">");
				insertProperties.append("<" + key + ">)\n");
				if (navigationProperty.IsInverse()) {
					insertProperties.append("\t\t(<" + key + ">");
					insertProperties.append(" <").append(expandedInverseProperty).append(">");
					insertProperties.append("<").append(expandedKey).append(">)\n");
				}
			}
			insertProperties.append("\t}\n");
			addCurrentGraphInsertQuery(entityName, insertProperties);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
				addChangeLoggingParameters(entityName, insertProperties);

			insertProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<")
					.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl())
					.append(">) as ?addedGraph)\n");
			insertProperties
					.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");
			insertProperties.append("}\n");

			return new SparqlStatement(insertProperties.toString());
		}
	}

	/**
	 * Gets the expanded link entitykey.
	 *
	 * @param requestEntityReference the request entity reference
	 * @return the expanded link entitykey
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private String getExpandedLinkEntitykey(URI requestEntityReference) throws OData2SparqlException {
		String[] parts = requestEntityReference.getPath().split("/");
		String linkEntityUri = parts[parts.length - 1];
		String linkEntityKey = linkEntityUri.substring(linkEntityUri.indexOf("(") + 1, linkEntityUri.length() - 1);
		String expandedLinkEntityKey = rdfModel.getRdfPrefixes().expandPredicateKey(linkEntityKey);
		return expandedLinkEntityKey;
	}

	/**
	 * Generate update entity reference.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param entityType the entity type
	 * @param navigationProperty the navigation property
	 * @param requestEntityReferences the request entity references
	 * @return the sparql statement
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws ODataException the o data exception
	 */
	public SparqlStatement generateUpdateEntityReference(RdfResourceParts rdfResourceParts, RdfEntityType entityType,
			RdfNavigationProperty navigationProperty, List<URI> requestEntityReferences) throws OData2SparqlException, ODataException {
		if (entityType.isOperation()) {
			//TODO
			return null;
		} else {
			if (navigationProperty == null) {
				throw new ODataException("PUT/PATCH of entity/$ref not supported (yet), as this would involve updating its immutable URI");
			} else {
				String entityName = rdfResourceParts.getEntitySet().getRdfEntityType().getEntityTypeName();
				if (rdfResourceParts.getNavPathString() != null) {
					entityName = entityName + rdfResourceParts.getLastNavPropertyName();
				}
				entityType = rdfResourceParts.getResponseRdfEntityType();
				StringBuilder updateProperties = new StringBuilder();

				addDelete(entityName, updateProperties);

				updateProperties.append("INSERT {\n");
				addAdded(entityName, updateProperties);
				if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
					addChangeLogging(entityName, updateProperties);
				updateProperties.append("}\n");

				updateProperties.append("WHERE {\n");
				String expandedKey = rdfResourceParts.getValidatedSubjectIdUrl();
				String expandedProperty = navigationProperty.getNavigationPropertyIRI();
				String expandedInverseProperty = navigationProperty.getInversePropertyOfURI();

				updateProperties.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName)
						.append("_p ?").append(entityName).append("_no){\n");
				for (URI requestEntityReference : requestEntityReferences) {

					String key = getExpandedLinkEntitykey(requestEntityReference);
					updateProperties.append("\t\t(<").append(expandedKey).append(">");
					updateProperties.append(" <").append(expandedProperty).append(">");
					updateProperties.append("<" + key + ">)\n");
					if (navigationProperty.IsInverse()) {
						updateProperties.append("\t\t(<" + key + ">");
						updateProperties.append(" <").append(expandedInverseProperty).append(">");
						updateProperties.append("<").append(expandedKey).append(">)\n");
					}
				}
				updateProperties.append("\t}\n");
				addCurrentGraphQuery(entityName, updateProperties);

				if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())
					addChangeLoggingParameters(entityName, updateProperties);

				updateProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<")
						.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<")
						.append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl())
						.append(">) as ?addedGraph)\n");
				updateProperties
						.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");
				updateProperties.append("}\n");

				return new SparqlStatement(updateProperties.toString());
			}

		}
	}
}
