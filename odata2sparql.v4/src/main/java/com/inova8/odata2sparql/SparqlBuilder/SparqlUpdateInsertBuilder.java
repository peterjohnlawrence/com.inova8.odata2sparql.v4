package com.inova8.odata2sparql.SparqlBuilder;

import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.NavigationSegment;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;

import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;

public class SparqlUpdateInsertBuilder {
	@SuppressWarnings("unused")
	private final Log log = LogFactory.getLog(SparqlStatement.class);
	private final RdfModel rdfModel;

	public SparqlUpdateInsertBuilder(RdfModel rdfModel) {
		super();
		this.rdfModel = rdfModel;
	}

	//private SPARQLProfile profile = RdfConnection.SPARQLProfile.DEFAULT;
//	@Deprecated
//	public SPARQLProfile getProfile() {
//		return this.sparqlODataProvider.getRdfRepository().getDataEndpoint().getProfile();
//		//		return profile;
//	}

	public SparqlStatement generateInsertQuery(RdfEntityType entityType, ODataEntry entry) throws Exception {

		StringBuilder insertQuery = generateInsertProperties(entityType, null, entry);
		return new SparqlStatement(insertQuery.toString() + "WHERE {}");
	}

	private StringBuilder generateInsertProperties(RdfEntityType entityType, String entityKey, ODataEntry entry)
			throws ODataApplicationException {
		StringBuilder insertProperties = new StringBuilder("INSERT { ");
		StringBuilder properties = new StringBuilder();
		UrlValidator urlValidator = new UrlValidator();
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
		if (entityKey == null)
			throw new ODataApplicationException("Key not found ", null);
		String expandedKey =rdfModel.getRdfPrefixes().expandPrefix(entityKey);
		if (urlValidator.isValid(expandedKey)) {
			insertProperties.append("<" + expandedKey + ">");
			insertProperties.append(properties);
			insertProperties.append("}");
			return insertProperties;
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKey, null);
		}
	}

	public SparqlStatement generateDeleteQuery(RdfEntityType entityType, String entityKey)
			throws ExceptionVisitExpression, ODataApplicationException, EdmException {
		UrlValidator urlValidator = new UrlValidator();
		String key = entityType.entityTypeName;
		StringBuilder deleteQuery = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}WHERE {");
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
	public SparqlStatement generateUpdateQuery(RdfEntityType entityType, String entityKey, ODataEntry entry)
			throws Exception {
		UrlValidator urlValidator = new UrlValidator();
		String key = entityType.entityTypeName;
		StringBuilder sparql = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}");
		sparql.append(generateInsertProperties(entityType, entityKey, entry));

		sparql.append("WHERE { ?" + key + "_s ?" + key + "_p ?" + key + "_o .");
		sparql.append(generateUpdatePropertyValues(entityType, entityKey, entry));
		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPrefix(SparqlEntity.URLDecodeEntityKey(entityKey));
		if (urlValidator.isValid(expandedKey)) {
			sparql.append("VALUES(?" + key + "_s ){(<" + expandedKey + ">)}");
			sparql.append("}");
			return new SparqlStatement(sparql.toString());
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKey, null);
		}
	}
	private StringBuilder generateUpdatePropertyValues(RdfEntityType entityType, String entityKey, ODataEntry entry)
  			throws Exception {
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
 					updatePropertyValues
 							.append("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + entityType.getIRI() + ">");
 					entityKey = prop.getValue().toString();
 				} else {
 					updatePropertyValues.append("(<" + property.propertyNode.getIRI() + ">) ");
 				}
 			}
 		}
 		return updatePropertyValues.append("}.");
 	}
	public SparqlStatement generateEntitySimplePropertyQuery(RdfEntityType entityType, String entityKey, String property)
			throws Exception {
		UrlValidator urlValidator = new UrlValidator();
		StringBuilder sparql = new StringBuilder("SELECT ?VALUE WHERE {");
		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPrefix(SparqlEntity.URLDecodeEntityKey(entityKey));
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

	public SparqlStatement generateEntitySimplePropertyValueQuery(RdfEntityType entityType, String entityKey,
			String property) throws ODataApplicationException {
		UrlValidator urlValidator = new UrlValidator();
		String key = entityType.entityTypeName;
		StringBuilder sparql = new StringBuilder("DELETE {?" + key + "_s ?" + key + "_p ?" + key + "_o .}WHERE { ?"
				+ key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?" + key + "_p){(");
		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPrefix(SparqlEntity.URLDecodeEntityKey(entityKey));
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

	public SparqlStatement generateUpdateEntitySimplePropertyValueQuery(RdfEntityType entityType, String entityKey,
			String property, Object entry) throws ODataApplicationException {
		UrlValidator urlValidator = new UrlValidator();
		String key = entityType.entityTypeName;
		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPrefix(SparqlEntity.URLDecodeEntityKey(entityKey));
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
		sparql.append("WHERE {OPTIONAL { ?" + key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?" + key
				+ "_p){(");
		sparql.append("<" + expandedKey + "> ");
		sparql.append("<" + expandedProperty + ">)}}}");

		return new SparqlStatement(sparql.toString());

	}

	public SparqlStatement generateEntityLinksQuery(RdfEntityType entityType, NavigationSegment navigationProperty,
			EdmEntitySet targetEntitySet, String entityKey) throws EdmException, ODataApplicationException {
		UrlValidator urlValidator = new UrlValidator();
		String key = entityType.entityTypeName;

		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPrefix(SparqlEntity.URLDecodeEntityKey(entityKey));
		if (urlValidator.isValid(expandedKey)) {
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKey, null);
		}
		RdfAssociation rdfProperty = entityType.findNavigationProperty(navigationProperty.getNavigationProperty()
				.getName());
		String expandedProperty = rdfProperty.getAssociationIRI();
		StringBuilder sparql = new StringBuilder("CONSTRUCT {?" + key + "_s <" + expandedProperty + "> ?" + key
				+ "_o .}");
		if (rdfProperty.IsInverse()) {
			String expandedInverseProperty = rdfProperty.getInversePropertyOfURI().toString();
			sparql.append("WHERE {?" + key + "_o ?" + key + "_p ?" + key + "_s . VALUES(?" + key + "_s ?" + key
					+ "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedInverseProperty + ">)}}");
		} else {
			sparql.append("WHERE {?" + key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?" + key
					+ "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedProperty + ">)}}");
		}

		return new SparqlStatement(sparql.toString());
	}

	public SparqlStatement generateEntityLinksCountQuery(RdfEntityType entityType,
			NavigationSegment navigationProperty, EdmEntitySet targetEntitySet, String entityKey) throws EdmException,
			ODataApplicationException {
		UrlValidator urlValidator = new UrlValidator();
		String key = entityType.entityTypeName;
		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPrefix(SparqlEntity.URLDecodeEntityKey(entityKey));
		if (urlValidator.isValid(expandedKey)) {
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKey, null);
		}
		RdfAssociation rdfProperty = entityType.findNavigationProperty(navigationProperty.getNavigationProperty()
				.getName());
		String expandedProperty = rdfProperty.getAssociationIRI();
		StringBuilder sparql = new StringBuilder("SELECT (count(*) as ?COUNT) ");
		if (rdfProperty.IsInverse()) {
			String expandedInverseProperty = rdfProperty.getInversePropertyOfURI().toString();
			sparql.append("WHERE {?" + key + "_o ?" + key + "_p ?" + key + "_s . VALUES(?" + key + "_s ?" + key
					+ "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedInverseProperty + ">)}}");
		} else {
			sparql.append("WHERE {?" + key + "_s ?" + key + "_p ?" + key + "_o . VALUES(?" + key + "_s ?" + key
					+ "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedProperty + ">)}}");
		}

		return new SparqlStatement(sparql.toString());
	}
}
