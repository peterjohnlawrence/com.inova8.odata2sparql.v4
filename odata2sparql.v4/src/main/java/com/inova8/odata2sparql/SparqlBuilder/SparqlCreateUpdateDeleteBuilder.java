package com.inova8.odata2sparql.SparqlBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
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

public class SparqlCreateUpdateDeleteBuilder {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(SparqlStatement.class);
	private final RdfModel rdfModel;

	public SparqlCreateUpdateDeleteBuilder(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfModel = rdfEdmProvider.getRdfModel();
	}

	public SparqlStatement generateInsertEntity(RdfEntityType entityType, Entity entry) throws Exception {
		if (entityType.isOperation()) {
			String insertText = entityType.getInsertText();
			if (insertText != null) {
				StringBuilder insertQuery = generateOperationFromTemplate(insertText, entityType, null, entry);
				return new SparqlStatement(insertQuery.toString());
			} else {
				throw new OData2SparqlException("No insertBody for insertQuery of " + entityType.entityTypeName, null);
			}
		} else {
			StringBuilder insertQuery = generateInsertProperties(entityType, null, entry);
			return new SparqlStatement(insertQuery.toString() + "WHERE {}");
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
			updatePropertiesReplace = updatePropertiesReplace.replaceAll("\\?value", "\"" + value.toString() + "\"");
		else
			updatePropertiesReplace = updatePropertiesReplace.replaceAll("\\?value", "UNDEF");
		return updatePropertiesReplace;
	}

	private String insertValuesReplace(String group, RdfEntityType entityType, Entity entry) throws OData2SparqlException {
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

	private StringBuilder generateInsertProperties(RdfEntityType entityType, List<UriParameter> entityKeys, Entity entry)
			throws OData2SparqlException {
		StringBuilder insertProperties = new StringBuilder("INSERT { ");
		StringBuilder properties = new StringBuilder();
		//String entityKey=entityKeys.get(0).getLiteral();
		String entityKey = (entityKeys!=null) ? entityKeys.get(0).getText().substring(1, entityKeys.get(0).getText().length() - 1): null; 
		boolean first = true;
		for (Property prop : entry.getProperties()) {
			if (prop.getValue() != null) {
				if (!first) {
					properties.append(" ;\n");
				} else {
					first = false;
				}
				RdfProperty property = entityType.findProperty(prop.getName());
				if (property != null) {
					if (property.getIsKey()) {
						properties.append(
								"<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + entityType.getURL() + ">");
						entityKey = prop.getValue().toString();
					} else {
						properties.append("<" + property.getPropertyURI() + "> ");
						properties.append(castObjectToXsd(prop.getValue()));
					}
				} else {
					RdfNavigationProperty association = entityType.findNavigationPropertyByEDMNavigationPropertyName(prop.getName());
					properties.append("<" + association.getNavigationPropertyIRI() + "> ");
					properties.append(castObjectToXsd(prop.getValue()));
				}
			}
		}
		if(properties.length()>0) {
		properties.append(" .\n");
		if (entityKey == null) {
			throw new OData2SparqlException("No keys " + entityType.getPrimaryKeys().toString() + " specified for "
					+ entityType.getEntityTypeName(), null);
		} else {
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicate(entityKey);
			UrlValidator urlValidator = new UrlValidator();
			if (urlValidator.isValid(expandedKey)) {
				insertProperties.append("<" + expandedKey + ">");
				insertProperties.append(properties);
				insertProperties.append("}");
				return insertProperties;
			} else {
				throw new OData2SparqlException("Invalid key: " + entityKey + " for " + entityType.getEntityTypeName(),
						null);
			}
		}}else {
			return properties;
		}
	}

	private String castObjectToXsd(Object object) throws OData2SparqlException {
		switch (object.getClass().getName()) {
		case "Null":
			return "null";
		case "java.util.GregorianCalendar":
			return "\"" + ((java.util.GregorianCalendar) object).toZonedDateTime().toLocalDateTime().toString()
					+ "\"^^xsd:dateTime";
		//		case "org.apache.olingo.odata2.core.ep.entry.ODataEntryImpl":
		//			String odataUrl= ((org.apache.olingo.odata2.core.ep.entry.ODataEntryImpl)object).getMetadata().getUri();
		//			String entityUrl= RdfEntity.URLDecodeEntityKey(getExpandedMetadaEntitykey( odataUrl));
		//			return "<" + entityUrl + ">";
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

	public SparqlStatement generateDeleteEntity(RdfEntityType entityType, List<UriParameter> entityKeys)
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

			String key = entityType.entityTypeName;
			StringBuilder deleteQuery = new StringBuilder(
					"DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}WHERE {");
			//Only need one key for a RDF entity
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKeys.get(0).getText());

			//			UrlValidator urlValidator = new UrlValidator();
			//			if (urlValidator.isValid(expandedKey)) {
			deleteQuery.append("{?" + key + "_s ?" + key + "_p ?" + key + "_o .{VALUES(?" + key + "_s ){(<"
					+ expandedKey + ">)}}}");
			deleteQuery.append("UNION");
			deleteQuery.append("{?" + key + "_s ?" + key + "_p ?" + key + "_o .{VALUES(?" + key + "_o ){(<"
					+ expandedKey + ">)}}}");
			deleteQuery.append("}");
			return new SparqlStatement(deleteQuery.toString());
			//			} else {
			//				throw new OData2SparqlException("Invalid key: " + entityKeys.get(0).getText(), null);
			//			}
		}
	}

	public SparqlStatement generateUpdateEntity(RdfEntityType entityType, List<UriParameter> entityKeys, Entity entry)
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
			String key = entityType.entityTypeName;
			StringBuilder sparql = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}");
			sparql.append(generateInsertProperties(entityType, entityKeys, entry));

			sparql.append("WHERE { OPTIONAL{ ?" + key + "_s ?" + key + "_p ?" + key + "_o .");
			//Only need one key for an RDF entity
			String entityKey = entityKeys.get(0).getText();
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKey);
			sparql.append(generateUpdatePropertyValues(entityType, entityKey, entry));
			//			UrlValidator urlValidator = new UrlValidator();
			//			if (urlValidator.isValid(expandedKey)) {
			sparql.append("VALUES(?" + key + "_s ){(<" + expandedKey + ">)}");
			sparql.append("} }");
			return new SparqlStatement(sparql.toString());
			//			} else {
			//				throw new OData2SparqlException("Invalid key: " + entityKey, null);
			//			}
		}
	}

	private StringBuilder generateUpdatePropertyValues(RdfEntityType entityType, String entityKey, Entity entry) {
		StringBuilder updatePropertyValues = new StringBuilder("VALUES( ?" + entityType.entityTypeName + "_p){");
		StringBuilder properties = new StringBuilder();
		boolean first = true;
		for (Property prop : entry.getProperties()) {
			//if (prop.getValue() != null) {
				if (!first) {
					properties.append(" ;\n");
				} else {
					first = false;
				}
				RdfProperty property = entityType.findProperty(prop.getName());
				if (property.getIsKey()) {
					entityKey = prop.getValue().toString();
				} else {
					updatePropertyValues.append("(<" + property.getPropertyURI() + ">) ");
				}
			//}
		}
		return updatePropertyValues.append("}.");
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
			//Only need one key for an RDF entity
			String entityKey = entityKeys.get(0).getText();
			String key = entityType.entityTypeName;
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKey);
			String expandedProperty = entityType.findProperty(property).getPropertyURI();
			String value = entry.toString();
			//
			//			if (urlValidator.isValid(expandedProperty)) {
			//
			//			} else {
			//				throw new OData2SparqlException("Invalid property: " + property, null);
			//			}

			StringBuilder sparql = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}");
			sparql.append("INSERT{<" + expandedKey + "> " + "<" + expandedProperty + "> \"" + value + "\"}");
			sparql.append("WHERE {OPTIONAL { ?" + key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?"
					+ key + "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedProperty + ">)}}}");

			return new SparqlStatement(sparql.toString());
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
			String entityKey = entityKeys.get(0).getText();
			String key = entityType.entityTypeName;
			StringBuilder sparql = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}WHERE { ?"
					+ key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?" + key + "_p){(");
			String expandedKey = rdfModel.getRdfPrefixes().expandPredicateKey(entityKey);
			sparql.append("<" + expandedKey + "> ");
			String expandedProperty = entityType.findProperty(property).getPropertyURI();
			//			UrlValidator urlValidator = new UrlValidator();
			//			if (urlValidator.isValid(expandedProperty)) {
			sparql.append("<" + expandedProperty + ">)}}");
			//			} else {
			//				throw new OData2SparqlException("Invalid property: " + property, null);
			//			}
			return new SparqlStatement(sparql.toString());

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
			RdfNavigationProperty navigationProperty, List<UriParameter> navigationEntityKeys) throws OData2SparqlException {
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
