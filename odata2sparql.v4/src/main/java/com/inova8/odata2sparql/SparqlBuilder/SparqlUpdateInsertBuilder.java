package com.inova8.odata2sparql.SparqlBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.NavigationSegment;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.core.edm.provider.EdmImplProv;
import org.apache.olingo.odata2.core.uri.UriParserImpl;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfEntity;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;

public class SparqlUpdateInsertBuilder {
	@SuppressWarnings("unused")
	private final Log log = LogFactory.getLog(SparqlStatement.class);
	private final RdfModel rdfModel;
	private final RdfEdmProvider rdfEdmProvider;

	public SparqlUpdateInsertBuilder(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfModel = rdfEdmProvider.getRdfModel();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	public SparqlStatement generateInsertQuery(RdfEntityType entityType, ODataEntry entry) throws Exception {
		if (entityType.isOperation()) {
			String insertText = entityType.getInsertText();
			if (insertText != null) {
				StringBuilder insertQuery = generateOperationFromTemplate(insertText, entityType, null, entry);
				return new SparqlStatement(insertQuery.toString());
			} else {
				throw new ODataApplicationException("No insertBody for insertQuery of " + entityType.entityTypeName,
						null);
			}
		} else {
			StringBuilder insertQuery = generateInsertProperties(entityType, null, entry);
			return new SparqlStatement(insertQuery.toString() + "WHERE {}");
		}
	}

	public SparqlStatement generateInsertLinkQuery(EdmEntitySet entitySet, EdmEntitySet targetEntitySet,
			RdfEntityType entityType, String entityKey, NavigationSegment navigationSegment, List<String> entry)
			throws Exception {
		StringBuilder insertLinkQuery = generateInsertLinks(entitySet, targetEntitySet, entityType, entityKey,
				navigationSegment, entry);
		return new SparqlStatement(insertLinkQuery.toString() + "WHERE {}");
	}

	private StringBuilder generateOperationFromTemplate(String template, RdfEntityType entityType,
			List<KeyPredicate> entityKeys, ODataEntry entry) {
		return generateOperationFromTemplate(template, entityType, entityKeys, entry, null, null);
	}

	private StringBuilder generateOperationFromTemplate(String template, RdfEntityType entityType,
			List<KeyPredicate> entityKeys, ODataEntry entry, String property, Object value) {
		Pattern p = Pattern.compile("(##(INSERT|DELETE|UPDATE|UPDATEPROPERTY)VALUES(.*?)##).*?", Pattern.DOTALL);
		Matcher m = p.matcher(template);
		//boolean b = m.matches();
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

	private String deleteValuesReplace(String group, RdfEntityType entityType, List<KeyPredicate> entityKeys) {
		String deleteReplace = group;
		UrlValidator urlValidator = new UrlValidator();
		for (KeyPredicate entityKey : entityKeys) {
			RdfProperty property;
			try {
				property = entityType.findProperty(entityKey.getProperty().getName());
				String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(entityKey.getLiteral().toString());
				if (!urlValidator.isValid(expandedKey)) {
					log.error("Invalid key: " + entityKey, null);
					//throw new EdmException("Invalid key: " + entityKey);
				}
				deleteReplace = deleteReplace.replaceAll("\\?" + property.getVarName() + "\\b",
						"<" + expandedKey + ">");

			} catch (EdmException e) {
				log.warn("Failure finding property for :" + entityKey.toString());
			}
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

	private String insertValuesReplace(String group, RdfEntityType entityType, ODataEntry entry) {
		String insertReplace = group;
		for (Entry<String, Object> prop : entry.getProperties().entrySet()) {
			if (prop.getValue() != null) {
				RdfProperty property = entityType.findProperty(prop.getKey());
				if (property.getIsKey()) {
					insertReplace = insertReplace.replaceAll("\\?" + property.getVarName() + "\\b",
							"<" + prop.getValue().toString() + ">");
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

	private StringBuilder generateInsertProperties(RdfEntityType entityType, List<KeyPredicate> entityKeys,
			ODataEntry entry) throws ODataApplicationException {
		StringBuilder insertProperties = new StringBuilder("INSERT { ");
		StringBuilder properties = new StringBuilder();
		UrlValidator urlValidator = new UrlValidator();
		//String entityKey=entityKeys.get(0).getLiteral();
		String entityKey = null;
		boolean first = true;
		for (Entry<String, Object> prop : entry.getProperties().entrySet()) {
			if (prop.getValue() != null) {
				if (!first) {
					properties.append(" ;\n");
				} else {
					first = false;
				}
				RdfProperty property = entityType.findProperty(prop.getKey());
				if (property.getIsKey()) {
					properties
							.append("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + entityType.getIRI() + ">");
					entityKey = prop.getValue().toString();
				} else {
					properties.append("<" + property.propertyNode.getIRI() + "> ");
					properties.append("\"" + prop.getValue().toString() + "\"");
				}
			}
		}
		properties.append(" .\n");
		if (entityKey == null) {
			//This means that the keys are not in the property list so it must be part of the entity identity
			//Assume we are dealing with only pure RDF with a single key
			entityKey = entityKeys.get(0).getLiteral();
			//throw new ODataApplicationException("Key not found ", null);			
		}

		String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(entityKey);
		if (urlValidator.isValid(expandedKey)) {
			insertProperties.append("<" + expandedKey + ">");
			insertProperties.append(properties);
			insertProperties.append("}");
			return insertProperties;
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKeys, null);
		}
	}

	private StringBuilder generateInsertLinks(EdmEntitySet entitySet, EdmEntitySet targetEntitySet,
			RdfEntityType entityType, String entityKey, NavigationSegment navigationSegment, List<String> entry)
			throws ODataException, URISyntaxException {
		StringBuilder links = new StringBuilder("INSERT { ");
		UrlValidator urlValidator = new UrlValidator();
		UriParserImpl uriParser = new UriParserImpl(new EdmImplProv(rdfEdmProvider));

		RdfAssociation navigationProperty = entityType
				.findNavigationProperty(navigationSegment.getNavigationProperty().getName());
		String navigationPropertyUri=null;
		if(navigationProperty.IsInverse()){
			navigationPropertyUri= "<" + navigationProperty.getInversePropertyOf().getIRI()+ ">";
		}else{
			navigationPropertyUri= "<" + navigationProperty.getAssociationIRI() + ">";
		} 
		String expandedKeyUri = "<" + rdfModel.getRdfPrefixes().expandPrefix(entityKey) + ">";
		for (String link : entry) {
			if (link != null) {
				List<KeyPredicate> linkUri = uriParser.getKeyFromEntityLink(targetEntitySet, link,
						new URI("http://localhost:8080/odata2sparql/2.0/NW/"));
				if (navigationProperty.IsInverse()) {
					links.append("<" + rdfModel.getRdfPrefixes().expandPrefix(linkUri.get(0).getLiteral()) + ">")
							.append(navigationPropertyUri).append(expandedKeyUri).append(".");
				} else {
					links.append(expandedKeyUri).append(navigationPropertyUri)
							.append("<" + rdfModel.getRdfPrefixes().expandPrefix(linkUri.get(0).getLiteral()) + ">")
							.append(".");
				}

			}
		}

		links.append("}");
		return links;
	}

	public SparqlStatement generateDeleteQuery(RdfEntityType entityType, List<KeyPredicate> entityKeys)
			throws ExceptionVisitExpression, ODataApplicationException, EdmException {
		if (entityType.isOperation()) {
			String deleteText = entityType.getDeleteText();
			if (deleteText != null) {
				return new SparqlStatement(
						generateOperationFromTemplate(deleteText, entityType, entityKeys, null).toString());
			} else {
				throw new ODataApplicationException("No deleteBody for deleteQuery of " + entityType.entityTypeName,
						null);
			}
		} else {
			UrlValidator urlValidator = new UrlValidator();

			String key = entityType.entityTypeName;
			StringBuilder deleteQuery = new StringBuilder(
					"DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}WHERE {");
			//Only need one key for a RDF entity
			String entityKey = entityKeys.get(0).getLiteral();
			String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(entityKey);
			if (urlValidator.isValid(expandedKey)) {
				deleteQuery.append("{?" + key + "_s ?" + key + "_p ?" + key + "_o .{VALUES(?" + key + "_s ){(<"
						+ expandedKey + ">)}}}");
				deleteQuery.append("UNION");
				deleteQuery.append("{?" + key + "_s ?" + key + "_p ?" + key + "_o .{VALUES(?" + key + "_o ){(<"
						+ expandedKey + ">)}}}");
				deleteQuery.append("}");
				return new SparqlStatement(deleteQuery.toString());
			} else {
				throw new ODataApplicationException("Invalid key: " + entityKey, null);
			}
		}
	}

	public SparqlStatement generateUpdateQuery(RdfEntityType entityType, List<KeyPredicate> entityKeys,
			ODataEntry entry) throws Exception {
		if (entityType.isOperation()) {
			String updateText = entityType.getUpdateText();
			if (updateText != null) {
				return new SparqlStatement(
						generateOperationFromTemplate(updateText, entityType, entityKeys, entry).toString());
			} else {
				throw new ODataApplicationException("No updateBody for updateQuery of " + entityType.entityTypeName,
						null);
			}
		} else {
			UrlValidator urlValidator = new UrlValidator();
			String key = entityType.entityTypeName;
			StringBuilder sparql = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}");
			sparql.append(generateInsertProperties(entityType, entityKeys, entry));

			sparql.append("WHERE { OPTIONAL{ ?" + key + "_s ?" + key + "_p ?" + key + "_o .");
			//Only need one key for an RDF entity
			String entityKey = entityKeys.get(0).getLiteral();
			sparql.append(generateUpdatePropertyValues(entityType, entityKey, entry));

			String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
			if (urlValidator.isValid(expandedKey)) {
				sparql.append("VALUES(?" + key + "_s ){(<" + expandedKey + ">)}");
				sparql.append("} }");
				return new SparqlStatement(sparql.toString());
			} else {
				throw new ODataApplicationException("Invalid key: " + entityKey, null);
			}
		}
	}
	public SparqlStatement generateUpdateLinkQuery(EdmEntitySet entitySet, EdmEntitySet targetEntitySet,
			RdfEntityType entityType, String entityKey, String targetEntityKey, NavigationSegment navigationSegment, List<String> entry) throws Exception {
		
			StringBuilder links = new StringBuilder("DELETE ");
			UrlValidator urlValidator = new UrlValidator();
			UriParserImpl uriParser = new UriParserImpl(new EdmImplProv(rdfEdmProvider));

			String expandedKeyUri = "<" + rdfModel.getRdfPrefixes().expandPrefix(entityKey) + ">";
			String expandedTargetKeyUri = "<" + rdfModel.getRdfPrefixes().expandPrefix(targetEntityKey) + ">";
			
			RdfAssociation navigationProperty = entityType
					.findNavigationProperty(navigationSegment.getNavigationProperty().getName());
			String navigationPropertyUri=null;
			if(navigationProperty.IsInverse()){
				navigationPropertyUri= "<" + navigationProperty.getInversePropertyOf().getIRI()+ ">";
				links.append("{").append(expandedTargetKeyUri).append(navigationPropertyUri).append(expandedKeyUri).append(".}");
			}else{
				navigationPropertyUri= "<" + navigationProperty.getAssociationIRI() + ">";
				links.append("{").append(expandedKeyUri).append(navigationPropertyUri).append(expandedTargetKeyUri).append(".}");
			} 
			links.append("INSERT{ ");
			for (String link : entry) {
				if (link != null) {
					List<KeyPredicate> linkUri = uriParser.getKeyFromEntityLink(targetEntitySet, link,
							new URI("http://localhost:8080/odata2sparql/2.0/NW/"));
					if (navigationProperty.IsInverse()) {
						links.append("<" + rdfModel.getRdfPrefixes().expandPrefix(linkUri.get(0).getLiteral()) + ">")
								.append(navigationPropertyUri).append(expandedKeyUri).append(".");
					} else {
						links.append(expandedKeyUri).append(navigationPropertyUri)
								.append("<" + rdfModel.getRdfPrefixes().expandPrefix(linkUri.get(0).getLiteral()) + ">")
								.append(".");
					}

				}
			}

			links.append("}");
			return new SparqlStatement(links.toString());
			
	}
	public SparqlStatement generateDeleteLinkQuery(EdmEntitySet entitySet, EdmEntitySet targetEntitySet,
			RdfEntityType entityType, String entityKey, String targetEntityKey, NavigationSegment navigationSegment) throws Exception {
		
			StringBuilder links = new StringBuilder("DELETE ");
			UrlValidator urlValidator = new UrlValidator();
			UriParserImpl uriParser = new UriParserImpl(new EdmImplProv(rdfEdmProvider));

			String expandedKeyUri = "<" + rdfModel.getRdfPrefixes().expandPrefix(entityKey) + ">";
			String expandedTargetKeyUri = "<" + rdfModel.getRdfPrefixes().expandPrefix(targetEntityKey) + ">";
			
			RdfAssociation navigationProperty = entityType
					.findNavigationProperty(navigationSegment.getNavigationProperty().getName());
			String navigationPropertyUri=null;
			if(navigationProperty.IsInverse()){
				navigationPropertyUri= "<" + navigationProperty.getInversePropertyOf().getIRI()+ ">";
				links.append("{").append(expandedTargetKeyUri).append(navigationPropertyUri).append(expandedKeyUri).append(".}");
			}else{
				navigationPropertyUri= "<" + navigationProperty.getAssociationIRI() + ">";
				links.append("{").append(expandedKeyUri).append(navigationPropertyUri).append(expandedTargetKeyUri).append(".}");
			} 
			return new SparqlStatement(links.toString());		
	}
	private StringBuilder generateUpdatePropertyValues(RdfEntityType entityType, String entityKey, ODataEntry entry)
			throws ODataApplicationException {
		StringBuilder updatePropertyValues = new StringBuilder("VALUES( ?" + entityType.entityTypeName + "_p){");
		StringBuilder properties = new StringBuilder();
		boolean first = true;
		for (Entry<String, Object> prop : entry.getProperties().entrySet()) {
			if (prop.getValue() != null) {
				if (!first) {
					properties.append(" ;\n");
				} else {
					first = false;
				}
				RdfProperty property = entityType.findProperty(prop.getKey());
				if (property.getIsKey()) {
					entityKey = prop.getValue().toString();
				} else {
					updatePropertyValues.append("(<" + property.propertyNode.getIRI() + ">) ");
				}
			}
		}
		return updatePropertyValues.append("}.");
	}

	public SparqlStatement generateEntitySimplePropertyQuery(RdfEntityType entityType, String entityKey,
			String property) throws ODataApplicationException {
		UrlValidator urlValidator = new UrlValidator();
		StringBuilder sparql = new StringBuilder("SELECT ?VALUE WHERE {");
		String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
		if (urlValidator.isValid(expandedKey)) {
			sparql.append("<" + expandedKey + "> ");
			String expandedProperty = entityType.findProperty(property).propertyNode.getIRI().toString();
			if (urlValidator.isValid(expandedProperty)) {
				sparql.append("<" + expandedProperty + "> ?VALUE }");
			} else {
				throw new ODataApplicationException("Invalid property: " + property, null);
			}
			return new SparqlStatement(sparql.toString());
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKey, null);
		}
	}

	public SparqlStatement generateEntitySimplePropertyValueQuery(RdfEntityType entityType,
			List<KeyPredicate> entityKeys, String property) throws ODataApplicationException {
		if (entityType.isOperation()) {
			String updatePropertyText = entityType.getUpdatePropertyText();
			if (updatePropertyText != null) {
				return generateOperationUpdateEntitySimplePropertyValueQuery(updatePropertyText, entityType, entityKeys,
						property, null);
			} else {
				throw new ODataApplicationException(
						"No updatePropertyBody for updatePropertyQuery of " + entityType.entityTypeName, null);
			}
		} else {
			UrlValidator urlValidator = new UrlValidator();
			String entityKey = entityKeys.get(0).getLiteral();
			String key = entityType.entityTypeName;
			StringBuilder sparql = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}WHERE { ?"
					+ key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?" + key + "_p){(");
			String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
			if (urlValidator.isValid(expandedKey)) {
				sparql.append("<" + expandedKey + "> ");
				String expandedProperty = entityType.findProperty(property).propertyNode.getIRI().toString();
				if (urlValidator.isValid(expandedProperty)) {
					sparql.append("<" + expandedProperty + ">)}}");
				} else {
					throw new ODataApplicationException("Invalid property: " + property, null);
				}
				return new SparqlStatement(sparql.toString());
			} else {
				throw new ODataApplicationException("Invalid key: " + entityKey, null);
			}
		}
	}

	public SparqlStatement generateUpdateEntitySimplePropertyValueQuery(RdfEntityType entityType,
			List<KeyPredicate> entityKeys, String property, Object entry) throws ODataApplicationException {
		if (entityType.isOperation()) {
			String updatePropertyText = entityType.getDeleteText();
			if (updatePropertyText != null) {
				return generateOperationUpdateEntitySimplePropertyValueQuery(updatePropertyText, entityType, entityKeys,
						property, entry);
			} else {
				throw new ODataApplicationException(
						"No updatePropertyBody for updatePropertyQuery of " + entityType.entityTypeName, null);
			}
		} else {
			UrlValidator urlValidator = new UrlValidator();
			//Only need one key for an RDF entity
			String entityKey = entityKeys.get(0).getLiteral();
			String key = entityType.entityTypeName;
			String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
			String expandedProperty = entityType.findProperty(property).propertyNode.getIRI().toString();
			String value = entry.toString();
			if (urlValidator.isValid(expandedKey)) {

				if (urlValidator.isValid(expandedProperty)) {

				} else {
					throw new ODataApplicationException("Invalid property: " + property, null);
				}
			} else {
				throw new ODataApplicationException("Invalid key: " + entityKey, null);
			}

			StringBuilder sparql = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}");
			sparql.append("INSERT{<" + expandedKey + "> " + "<" + expandedProperty + "> \"" + value + "\"}");
			sparql.append("WHERE {OPTIONAL { ?" + key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?"
					+ key + "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedProperty + ">)}}}");

			return new SparqlStatement(sparql.toString());
		}
	}

	private SparqlStatement generateOperationUpdateEntitySimplePropertyValueQuery(String updatePropertyText,
			RdfEntityType entityType, List<KeyPredicate> entityKeys, String property, Object entry) {
		// TODO Auto-generated method stub
		return new SparqlStatement(generateOperationFromTemplate(entityType.getUpdatePropertyText(), entityType,
				entityKeys, null, property, entry).toString());
	}

	public SparqlStatement generateEntityLinksQuery(RdfEntityType entityType, NavigationSegment navigationProperty,
			EdmEntitySet targetEntitySet, String entityKey) throws EdmException, ODataApplicationException {
		UrlValidator urlValidator = new UrlValidator();
		String key = entityType.entityTypeName;

		String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
		if (urlValidator.isValid(expandedKey)) {
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKey, null);
		}
		RdfAssociation rdfProperty = entityType
				.findNavigationProperty(navigationProperty.getNavigationProperty().getName());
		String expandedProperty = rdfProperty.getAssociationIRI();
		StringBuilder sparql = new StringBuilder(
				"CONSTRUCT {?" + key + "_s <" + expandedProperty + "> ?" + key + "_o .}");
		if (rdfProperty.IsInverse()) {
			String expandedInverseProperty = rdfProperty.getInversePropertyOfURI().toString();
			sparql.append(
					"WHERE {?" + key + "_o ?" + key + "_p ?" + key + "_s . VALUES(?" + key + "_s ?" + key + "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedInverseProperty + ">)}}");
		} else {
			sparql.append(
					"WHERE {?" + key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?" + key + "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedProperty + ">)}}");
		}

		return new SparqlStatement(sparql.toString());
	}

	public SparqlStatement generateEntityLinksCountQuery(RdfEntityType entityType, NavigationSegment navigationProperty,
			EdmEntitySet targetEntitySet, String entityKey) throws EdmException, ODataApplicationException {
		UrlValidator urlValidator = new UrlValidator();
		String key = entityType.entityTypeName;
		String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
		if (urlValidator.isValid(expandedKey)) {
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKey, null);
		}
		RdfAssociation rdfProperty = entityType
				.findNavigationProperty(navigationProperty.getNavigationProperty().getName());
		String expandedProperty = rdfProperty.getAssociationIRI();
		StringBuilder sparql = new StringBuilder("SELECT (count(*) as ?COUNT) ");
		if (rdfProperty.IsInverse()) {
			String expandedInverseProperty = rdfProperty.getInversePropertyOfURI().toString();
			sparql.append(
					"WHERE {?" + key + "_o ?" + key + "_p ?" + key + "_s . VALUES(?" + key + "_s ?" + key + "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedInverseProperty + ">)}}");
		} else {
			sparql.append(
					"WHERE {?" + key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?" + key + "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedProperty + ">)}}");
		}

		return new SparqlStatement(sparql.toString());
	}
}
