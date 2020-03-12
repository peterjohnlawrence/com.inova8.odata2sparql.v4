package com.inova8.odata2sparql.SparqlBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.uri.UriParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;

import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;
import com.inova8.odata2sparql.uri.RdfResourceParts;

public class SparqlCreateUpdateDeleteBuilder {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(SparqlStatement.class);
	private final RdfModel rdfModel;
	//private final RdfEdmProvider rdfEdmProvider;
	public SparqlCreateUpdateDeleteBuilder(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfModel = rdfEdmProvider.getRdfModel();
	//	this.rdfEdmProvider = rdfEdmProvider;
	}

	public SparqlStatement generateInsertEntity(RdfResourceParts rdfResourceParts, RdfEntityType entityType, Entity entry) throws Exception {
		
		
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

	private StringBuilder generateOperationFromTemplate(String template, RdfEntityType entityType,
			List<UriParameter> entityKeys, Entity entry) throws OData2SparqlException {
		return generateOperationFromTemplate(template, entityType, entityKeys, entry, null, null);
	}

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

	private StringBuilder generateInsertProperties(RdfResourceParts rdfResourceParts, RdfEntityType entityType, List<UriParameter> entityKeys,
			Entity entry) throws OData2SparqlException {
		String entityName = rdfResourceParts.getEntitySet().getRdfEntityType().getEntityTypeName();
		if(rdfResourceParts.getNavPathString()!=null ) {
			entityName = entityName + rdfResourceParts.getNavPathString();
		}
		entityType= rdfResourceParts.getResponseRdfEntityType();
		StringBuilder insertProperties = new StringBuilder();
		
		addDelete(entityName, insertProperties);		

		insertProperties.append("INSERT {\n");
		addAdded(entityName, insertProperties);	
		if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl()) 
			addChangeLogging(entityName, insertProperties);
		insertProperties.append("}\n");
		
		insertProperties.append("WHERE {\n");
		addInsertPropertyValues(entityType, entityKeys, entry, entityName, insertProperties,rdfResourceParts);
		addCurrentCurrentGraphQuery(entityName, insertProperties);	
				
		if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())  addChangeLoggingParameters(entityName, insertProperties);
		
		insertProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<").append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<").append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append(">) as ?addedGraph)\n");
		insertProperties.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");		
		insertProperties.append("}\n");
		
		return insertProperties;
	}

	protected void addCurrentCurrentGraphQuery(String entityName, StringBuilder insertProperties) {
		insertProperties.append("\tOPTIONAL{\n");
		insertProperties.append("\t\tGRAPH ?currentGraph \n");
		insertProperties.append("\t\t{\n");
		insertProperties.append("\t\t\t?").append(entityName).append("_s ?").append(entityName).append("_p ?").append(entityName).append("_o .\n");
		insertProperties.append("\t\t} \n");
		insertProperties.append("\t}\n");	
		insertProperties.append("\tBIND( IF(?").append(entityName).append("_o=?").append(entityName).append("_no,FALSE,TRUE) as ?updated)\n");
		insertProperties.append("\tBIND(IF(BOUND(?updated),?updated,TRUE) as ?revisedUpdated)\n");
		insertProperties.append("\tFILTER(?revisedUpdated)\n");
	}

	protected void addInsertPropertyValues(RdfEntityType entityType, List<UriParameter> entityKeys, Entity entry,
			String entityName, StringBuilder insertPropertyValues,RdfResourceParts rdfResourceParts) throws OData2SparqlException {
		insertPropertyValues.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?")
				.append(entityName).append("_no){\n");
		String entityKey = (entityKeys != null)
				? entityKeys.get(0).getText().substring(1, entityKeys.get(0).getText().length() - 1)
				: null;
		String expandedKey = null;

		if(entityKeys!=null && entityKeys.size()>0) {
			//Use supplied key
			entityKey = entityKeys.get(0).getText().replace("'", "");
			expandedKey = rdfModel.getRdfPrefixes().expandPredicate(entityKey);		
		}else {
			//First find key within properties
			for (Property prop : entry.getProperties()) {	
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
		}
		//Create insert for any navigation property that is included:
		if(rdfResourceParts.size()>1 ) {
			//(<entityKey> <navigationproperty> <expandedKey>)
			String localKey = rdfResourceParts.getLocalKey().replace("'", "");
			String expandedLocalKey = rdfModel.getRdfPrefixes().expandPredicate(localKey);	
			EdmNavigationProperty navigationProperty = rdfResourceParts.getAsNavigationProperty(1).getEdmNavigationProperty();
			String navigationPropertyKey = rdfResourceParts.getEntitySet().getRdfEntityType().findNavigationPropertyByEDMNavigationPropertyName(navigationProperty.getName()).getNavigationPropertyIRI();
			insertPropertyValues.append("\t\t(<").append(expandedLocalKey).append("> <").append(navigationPropertyKey).append("> <").append(expandedKey).append(">)\n");
		}

		//Now create VALUES statement for all properties
		for (Property prop : entry.getProperties()) {
			RdfProperty property = entityType.findProperty(prop.getName());
			insertPropertyValues.append("\t\t(");
			if (property != null) {
				if (property.getIsKey()) {
					insertPropertyValues.append("<").append(expandedKey).append(
							"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + entityType.getURL() + ">");
				} else {
					insertPropertyValues.append("<").append(expandedKey).append("> <" + property.getPropertyURI() + "> ");
					if (prop.getValue() != null) {
						insertPropertyValues.append(castObjectToXsd(prop.getValue()));
					}else {
						insertPropertyValues.append("UNDEF");
					}
				}
			} else {
				RdfNavigationProperty association = entityType
						.findNavigationPropertyByEDMNavigationPropertyName(prop.getName());
				insertPropertyValues.append("<").append(expandedKey).append("> <" + association.getNavigationPropertyIRI() + "> ");
				insertPropertyValues.append(castObjectToXsd(prop.getValue()));
			}
			insertPropertyValues.append(")\n");
		}
		insertPropertyValues.append("\t}\n");
	}

	protected void addChangeLoggingParameters(String entityName, StringBuilder changeLoggingParameters) {
		changeLoggingParameters.append("\tBIND(NOW() as ?now)\n");		
		changeLoggingParameters.append("\tBIND(IRI(CONCAT(\"").append(rdfModel.getRdfRepository().getDataRepository().getChangeGraphUrl()).append("/\",SHA1(CONCAT(STR(?").append(entityName).append("_s))),\"-\",STR(?now))) as ?change)\n");			
		changeLoggingParameters.append("\tBIND( IF(!?revisedUpdated,\"\",IF(BOUND(?").append(entityName).append("_no),?change,?").append(entityName).append("_no )) as ?addChange)\n");
		changeLoggingParameters.append("\tBIND(IF(!?revisedUpdated,\"\", IF(BOUND(?currentGraph),?change,?currentGraph)) as ?deleteChange)\n");
		changeLoggingParameters.append("\tBIND(IF(!?revisedUpdated,\"\",IRI(CONCAT(\"").append(rdfModel.getRdfRepository().getDataRepository().getChangeGraphUrl()).append("/added/\",SHA1(CONCAT(STR(?addChange),STR(?").append(entityName).append("_s),STR(?").append(entityName).append("_p))),\"-\",STR(?now)))) as ?addedChange)\n");
		changeLoggingParameters.append("\tBIND(IF(!?revisedUpdated,\"\",IRI(CONCAT(\"").append(rdfModel.getRdfRepository().getDataRepository().getChangeGraphUrl()).append("/deleted/\",SHA1(CONCAT(STR(?deleteChange),STR(?").append(entityName).append("_s),STR(?").append(entityName).append("_p))),\"-\",STR(?now)))) as ?deletedChange)\n");
	}


	protected void addAdded(String entityName, StringBuilder insert) {
		insert.append("\tGRAPH ?addedGraph\n");
		insert.append("\t{\n");
		insert.append("\t\t?").append(entityName).append("_s ?").append(entityName).append("_p ?")
				.append(entityName).append("_no\n");
		insert.append("\t}\n");
	}

	protected void addDelete(String entityName, StringBuilder delete) {
		delete.append("DELETE {\n");
		delete.append("\tGRAPH ?deletedGraph\n");
		delete.append("\t{\n");
		delete.append("\t\t?").append(entityName).append("_s ?").append(entityName).append("_p ?")
				.append(entityName).append("_o\n");
		delete.append("\t}\n");		
		delete.append("}\n");
	}

	protected void addChangeLogging(String entityName, StringBuilder changelogging) {
		changelogging.append("\t##Changes\n");
		changelogging.append("\tGRAPH <").append(rdfModel.getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">\n");
		changelogging.append("\t{?change a <http://inova8.com/odata4sparql#Change> ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#created>  ?now;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#added> ?addedChange ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#deleted> ?deletedChange .\n");
		changelogging.append("\t?addedChange \n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#graph> ?addedGraph  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#subject> ?").append(entityName).append("_s  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#predicate>  ?").append(entityName).append("_p  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#object>  ?").append(entityName).append("_no .\n");
		changelogging.append("\t?deletedChange \n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#graph> ?deletedGraph  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#subject> ?").append(entityName).append("_s  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#predicate>  ?").append(entityName).append("_p  ;\n");
		changelogging.append("\t\t<http://inova8.com/odata4sparql#object>  ?").append(entityName).append("_o .\n");
		changelogging.append("\t}\n");
	}

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

	public SparqlStatement generateDeleteEntity(RdfResourceParts rdfResourceParts, RdfEntityType entityType, List<UriParameter> entityKeys)
			throws OData2SparqlException {
		if (entityType.isOperation()) {
			String deleteText = entityType.getDeleteText();
			if (deleteText != null) {
				return new SparqlStatement(
						generateOperationFromTemplate(deleteText, entityType, entityKeys, null).toString());
			} else {
				throw new OData2SparqlException("No deleteBody for deleteQuery of " + entityType.entityTypeName, null);
			}
		} else {

			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKeys.get(0).getText());
			String entityName = entityType.getEntityTypeName();
			StringBuilder insertProperties = new StringBuilder();
			
			addDelete(entityName, insertProperties);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl()) {
				insertProperties.append("INSERT {\n");
				addChangeLogging(entityName, insertProperties);
				insertProperties.append("}\n");
			}			
			insertProperties.append("WHERE {\n");
			insertProperties.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?").append(entityName).append("_o").append("){");
			//Create insert for any navigation property that is included:
	
			//(<entityKey> <navigationproperty> <expandedKey>)
			expandedKey = " <" + expandedKey + "> ";	
			String navigationPropertyKey =" UNDEF ";
			String expandedNavPropKey =" UNDEF ";
			if(rdfResourceParts.size()>1 ) {
				EdmNavigationProperty navigationProperty = rdfResourceParts.getAsNavigationProperty(1).getEdmNavigationProperty();
				navigationPropertyKey =  " <" +  rdfResourceParts.getEntitySet().getRdfEntityType().findNavigationPropertyByEDMNavigationPropertyName(navigationProperty.getName()).getNavigationPropertyIRI()+"> ";
				
				if( rdfResourceParts.getTargetSubjectId() !="" ) {
				String navPropKey = rdfResourceParts.getTargetSubjectId().replace("'", "");
				expandedNavPropKey = " <" + rdfModel.getRdfPrefixes().expandPredicate(navPropKey) +"> " ;
				}		
			}
			insertProperties.append("(").append(expandedKey).append(navigationPropertyKey).append(expandedNavPropKey).append(")");
			insertProperties.append("}\n");
			addCurrentCurrentGraphQuery(entityName, insertProperties);	
					
			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())  addChangeLoggingParameters(entityName, insertProperties);
			
			insertProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<").append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<").append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append(">) as ?addedGraph)\n");
			insertProperties.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");		
			insertProperties.append("}\n");
			
			return new SparqlStatement(insertProperties.toString());	
		}
	}

	public SparqlStatement generateUpdateEntity(RdfResourceParts rdfResourceParts, RdfEntityType entityType, List<UriParameter> entityKeys, Entity entry)
			throws Exception {
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
			sparql.append(generateInsertProperties(rdfResourceParts,entityType, entityKeys, entry));
			return new SparqlStatement(sparql.toString());
		}
	}

	public SparqlStatement generateUpdateEntitySimplePropertyValue(RdfEntityType entityType,
			List<UriParameter> entityKeys, String property, Object entry) throws OData2SparqlException {
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
			
			String entityKey = entityKeys.get(0).getText();
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKey);
			String expandedProperty = entityType.findProperty(property).getPropertyURI();
			String value = entry.toString();
			
			insertProperties.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p ?").append(entityName).append("_no){(");
			insertProperties.append("<").append(expandedKey).append(">");
			insertProperties.append(" <").append(expandedProperty).append(">");
			insertProperties.append(" '''" + value + "'''");
			insertProperties.append(")}\n");
			addCurrentCurrentGraphQuery(entityName, insertProperties);	
					
			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())  addChangeLoggingParameters(entityName, insertProperties);
			
			insertProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<").append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<").append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append(">) as ?addedGraph)\n");
			insertProperties.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");		
			insertProperties.append("}\n");
			
			return new SparqlStatement(insertProperties.toString());	
		}
	}

	private SparqlStatement generateOperationUpdateEntitySimplePropertyValue(String updatePropertyText,
			RdfEntityType entityType, List<UriParameter> entityKeys, String property, Object entry)
			throws OData2SparqlException {
		return new SparqlStatement(generateOperationFromTemplate(entityType.getUpdatePropertyText(), entityType,
				entityKeys, null, property, entry).toString());
	}

	public SparqlStatement generateDeleteEntitySimplePropertyValue(RdfEntityType entityType,
			List<UriParameter> entityKeys, String property) throws OData2SparqlException {
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
	
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKeys.get(0).getText());
			String entityName = entityType.getEntityTypeName();
			String expandedProperty = entityType.findProperty(property).getPropertyURI();
			StringBuilder insertProperties = new StringBuilder();
			
			addDelete(entityName, insertProperties);

			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl()) {
				insertProperties.append("INSERT {\n");
				addChangeLogging(entityName, insertProperties);
				insertProperties.append("}\n");
			}			
			insertProperties.append("WHERE {\n");

			insertProperties.append("\tVALUES(?").append(entityName).append("_s ?").append(entityName).append("_p){(");
			insertProperties.append("<").append(expandedKey).append(">");
			insertProperties.append(" <").append(expandedProperty).append(">");
			insertProperties.append(")}\n");
			
			addCurrentCurrentGraphQuery(entityName, insertProperties);	
					
			if (rdfModel.getRdfRepository().getDataRepository().isChangeGraphUrl())  addChangeLoggingParameters(entityName, insertProperties);
			
			insertProperties.append("\tBIND( IF(BOUND(?deletedChange),COALESCE(?deletedGraph,<").append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append("> ),<").append(rdfModel.getRdfRepository().getDataRepository().getInsertGraphUrl()).append(">) as ?addedGraph)\n");
			insertProperties.append("\tBIND( IF(isIRI(?deletedChange),?currentGraph,<http://fake>) as ?deletedGraph)\n");		
			insertProperties.append("}\n");		
			return new SparqlStatement(insertProperties.toString());	
			
		}
	}

	public SparqlStatement generateInsertLinkQuery(RdfEntityType entityType, List<UriParameter> entityKeys,
			RdfNavigationProperty navigationProperty, List<URI> requestEntityReferences) throws OData2SparqlException {

		if (entityType.isOperation()) {
			//TODO
			return null;
		} else {
			StringBuilder insertLinks = new StringBuilder("INSERT { ");
			String navigationPropertyUri = null;
			if (navigationProperty.IsInverse()) {
				navigationPropertyUri = "<" + navigationProperty.getInversePropertyOf().getIRI() + ">";
			} else {
				navigationPropertyUri = "<" + navigationProperty.getNavigationPropertyIRI() + ">";
			}
			String expandedKeyUri = "<" + rdfModel.getRdfPrefixes().expandPredicateKey(entityKeys.get(0).getText())
					+ ">";
			for (URI requestEntityReference : requestEntityReferences) {
				if (requestEntityReference != null) {
					String expandedLinkEntityKey = getExpandedLinkEntitykey(requestEntityReference);
					if (navigationProperty.IsInverse()) {
						insertLinks.append("<" + expandedLinkEntityKey + ">").append(navigationPropertyUri)
								.append(expandedKeyUri).append(".");
					} else {
						insertLinks.append(expandedKeyUri).append(navigationPropertyUri)
								.append("<" + expandedLinkEntityKey + ">").append(".");
					}
				}
			}
			insertLinks.append("}");
			return new SparqlStatement(insertLinks.toString() + "WHERE {}");
		}
	}

	private String getExpandedLinkEntitykey(URI requestEntityReference) throws OData2SparqlException {
		String[] parts = requestEntityReference.getPath().split("/");
		String linkEntityUri = parts[parts.length - 1];
		String linkEntityKey = linkEntityUri.substring(linkEntityUri.indexOf("(") + 1, linkEntityUri.length() - 1);
		String expandedLinkEntityKey = rdfModel.getRdfPrefixes().expandPredicateKey(linkEntityKey);
		return expandedLinkEntityKey;
	}

	public SparqlStatement generateDeleteLinkQuery(RdfEntityType entityType, List<UriParameter> entityKeys,
			RdfNavigationProperty navigationProperty, List<UriParameter> navigationEntityKeys)
			throws OData2SparqlException {
		if (entityType.isOperation()) {
			//TODO
			return null;
		} else {
			StringBuilder deleteLinks = new StringBuilder("DELETE ");
			String expandedKeyUri = "<" + rdfModel.getRdfPrefixes().expandPredicateKey(entityKeys.get(0).getText())
					+ ">";
			String navigationPropertyUri = null;
			if (navigationProperty.IsInverse()) {
				navigationPropertyUri = "<" + navigationProperty.getInversePropertyOf().getIRI() + ">";
				if (navigationProperty.getRangeCardinality().equals(Cardinality.MANY)) {
					String expandedTargetKeyUri = "<"
							+ rdfModel.getRdfPrefixes().expandPredicateKey(navigationEntityKeys.get(0).getText()) + ">";
					deleteLinks.append("{").append(expandedTargetKeyUri).append(navigationPropertyUri)
							.append(expandedKeyUri).append(".}");
				} else {
					deleteLinks.append("{").append("?target ").append(navigationPropertyUri).append(expandedKeyUri)
							.append(".}WHERE{").append("?target ").append(navigationPropertyUri).append(expandedKeyUri)
							.append(".}");
				}
			} else {
				navigationPropertyUri = "<" + navigationProperty.getNavigationPropertyIRI() + ">";
				//String expandedTargetKeyUri = "<" + rdfModel.getRdfPrefixes().expandPredicateKey(navigationEntityKeys.get(0).getText()) + ">";
				if (navigationProperty.getDomainCardinality().equals(Cardinality.MANY)) {
					String expandedTargetKeyUri = "<"
							+ rdfModel.getRdfPrefixes().expandPredicateKey(navigationEntityKeys.get(0).getText()) + ">";
					deleteLinks.append("{").append(expandedKeyUri).append(navigationPropertyUri)
							.append(expandedTargetKeyUri).append(".}");
				} else {
					deleteLinks.append("{").append(expandedKeyUri).append(navigationPropertyUri).append("?target ")
							.append(".}WHERE{").append(expandedKeyUri).append(navigationPropertyUri).append("?target ")
							.append(".}");
				}
			}
			return new SparqlStatement(deleteLinks.toString());
		}
	}

	public SparqlStatement generateUpdateLinkQuery(RdfEntityType entityType, List<UriParameter> entityKeys,
			RdfNavigationProperty navigationProperty, List<UriParameter> navigationEntityKeys,
			List<URI> requestEntityReferences) throws OData2SparqlException {
		if (entityType.isOperation()) {
			//TODO
			return null;
		} else {
			StringBuilder links = new StringBuilder("DELETE ");

			String expandedKeyUri = "<" + rdfModel.getRdfPrefixes().expandPredicateKey(entityKeys.get(0).getText())
					+ ">";
			String expandedTargetKeyUri = "<"
					+ rdfModel.getRdfPrefixes().expandPredicateKey(navigationEntityKeys.get(0).getText()) + ">";

			String navigationPropertyUri = null;
			if (navigationProperty.IsInverse()) {
				navigationPropertyUri = "<" + navigationProperty.getInversePropertyOf().getIRI() + ">";
				links.append("{").append(expandedTargetKeyUri).append(navigationPropertyUri).append(expandedKeyUri)
						.append(".}");
			} else {
				navigationPropertyUri = "<" + navigationProperty.getNavigationPropertyIRI() + ">";
				links.append("{").append(expandedKeyUri).append(navigationPropertyUri).append(expandedTargetKeyUri)
						.append(".}");
			}
			links.append("INSERT{ ");
			for (URI requestEntityReference : requestEntityReferences) {
				if (requestEntityReference != null) {
					String expandedLinkEntityKey = getExpandedLinkEntitykey(requestEntityReference);
					if (navigationProperty.IsInverse()) {
						links.append("<" + expandedLinkEntityKey + ">").append(navigationPropertyUri)
								.append(expandedKeyUri).append(".");
					} else {
						links.append(expandedKeyUri).append(navigationPropertyUri)
								.append("<" + expandedLinkEntityKey + ">").append(".");
					}
				}
			}
			links.append("}");
			return new SparqlStatement(links.toString());
		}
	}
}
