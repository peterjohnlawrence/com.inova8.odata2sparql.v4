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
		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
		if (urlValidator.isValid(expandedKey)) {
			sparql.append("VALUES(?" + key + "_s ){(<" + expandedKey + ">)}");
			sparql.append("}");
			return new SparqlStatement(sparql.toString());
		} else {
			throw new ODataApplicationException("Invalid key: " + entityKey, null);
		}
	}

	public SparqlStatement generateEntitySimplePropertyQuery(RdfEntityType entityType, String entityKey, String property)
			throws ODataApplicationException {
		UrlValidator urlValidator = new UrlValidator();
		StringBuilder sparql = new StringBuilder("SELECT ?VALUE WHERE {");
		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
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
				.expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
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
				.expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
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
				.expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
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
				.expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
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
//	@Deprecated
//	SparqlStatement generateEntitiesQuery(RdfEntityType entityType, List<KeyPredicate> oEntityKeys,
//			FilterExpression filter, List<SelectItem> select, Integer top, Integer skip,
//			List<ArrayList<NavigationPropertySegment>> expand) throws EdmException, ExceptionVisitExpression,
//			ODataApplicationException {
//		String key = entityType.entityTypeName;
//		ExpandSelectTreeNode expandSelectTreeNode = UriParser.createExpandSelectTree(select, expand);
//		HashMap<String, RdfAssociation> expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(select, expand);
//		StringBuilder entitiesQuery = new StringBuilder("CONSTRUCT {");
//		if (entityType.isOperation()) { // SPARQL select query
//			entitiesQuery.append(generateOperationConstruct(entityType));
//			entitiesQuery.append("} WHERE {{" + entityType.queryText);
//			entitiesQuery.append("}");
//
//			entitiesQuery.append(generateOperationWhere(entityType, oEntityKeys, filter));
//			entitiesQuery.append("}");
//			entitiesQuery.append(limitClause(top, skip));
//			return new SparqlStatement(entitiesQuery.toString());
//
//		} else { // pure entity
//			entitiesQuery.append(generateEntityConstruct(key));
//			entitiesQuery
//					.append(generateExpandConstruct(key, expand, expandSelectTreeNode, expandSelectNavPropertyMap));
//			entitiesQuery.append("} WHERE {");
//			entitiesQuery.append(" ?" + key + "_s ?" + key + "_p ?" + key + "_o .");
//			// TODO No point returning any IRI as these will be deferred anyway
//			// TODO Could there be a more efficient way of eliminating the predicates such as only searching for datatype predicates?
//			entitiesQuery.append("FILTER((!isIRI(?" + key + "_o) || (?"+ key+ "_p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)) && !isBlank(?" + key + "_o)) .");
//			//Duplicating some clauses which then causes performance issues
//			entitiesQuery.append(generateExpandWhere(key, expandSelectTreeNode, expandSelectNavPropertyMap));
//
//			entitiesQuery.append(propertyValuesClause(entityType, select));
//
//			String expandSelectTreeNodeSelect = expandSelectTreeNodeSelect(key, expandSelectTreeNode,
//					expandSelectNavPropertyMap);
//
//			entitiesQuery.append(generateEntityWhere(entityType, oEntityKeys, expandSelectTreeNodeSelect, filter, top,
//					skip));
//
//			entitiesQuery.append("}");
//			return new SparqlStatement(entitiesQuery.toString());
//		}
//
//	};
//	@Deprecated
//	private HashMap<String, RdfAssociation> createExpandSelectNavPropertyMap(List<SelectItem> select,
//			List<ArrayList<NavigationPropertySegment>> expand) throws EdmException {
//		HashMap<String, RdfAssociation> expandSelectNavPropertyMap = new HashMap<String, RdfAssociation>();
//
//		for (SelectItem selectItem : select) {
//
//			for (NavigationPropertySegment navigationPropertySegment : selectItem.getNavigationPropertySegments()) {
//				expandSelectNavPropertyMap.put(
//						navigationPropertySegment.getNavigationProperty().getName(),
//						sparqlODataProvider.getMappedNavigationProperty(new FullQualifiedName(navigationPropertySegment
//								.getNavigationProperty().getRelationship().getNamespace(), navigationPropertySegment
//								.getNavigationProperty().getRelationship().getName())));
//			}
//		}
//		for (ArrayList<NavigationPropertySegment> navigationPropertySegments : expand) {
//			for (NavigationPropertySegment navigationPropertySegment : navigationPropertySegments) {
//				expandSelectNavPropertyMap.put(
//						navigationPropertySegment.getNavigationProperty().getName(),
//						sparqlODataProvider.getMappedNavigationProperty(new FullQualifiedName(navigationPropertySegment
//								.getNavigationProperty().getRelationship().getNamespace(), navigationPropertySegment
//								.getNavigationProperty().getRelationship().getName())));
//			}
//		}
//
//		return expandSelectNavPropertyMap;
//	}
//	@Deprecated
//	private String limitClause(Integer top, Integer skip) {
//		String limitClause = "";
//		if (top > 0 || skip > 0) {
//
//			if (top > 0) {
//				limitClause = limitClause + " LIMIT " + top.toString();
//			}
//			if (skip > 0) {
//				limitClause = limitClause + " OFFSET " + skip.toString();
//			}
//		}
//		return limitClause;
//	}
//	@Deprecated
//	private String generateOperationWhere(RdfEntityType entityType, List<KeyPredicate> oEntityKeys,
//			FilterExpression filter) throws EdmException, ExceptionVisitExpression, ODataApplicationException {
//		UrlValidator defaultValidator = new UrlValidator();
//		StringBuilder operationWhere = new StringBuilder("");
//		if (oEntityKeys.isEmpty()) {
//
//		} else {
//			operationWhere.append("\nVALUES(");
//			for (KeyPredicate keyPredicate : oEntityKeys) {
//				operationWhere.append("?" + keyPredicate.getProperty().getName() + " ");
//			}
//			operationWhere.append(") {(");
//			for (KeyPredicate keyPredicate : oEntityKeys) {
//				//Is this a URI, blankNode or literal?
//				String decodedEntityKey = RdfEntity.URLDecodeEntityKey(keyPredicate.getLiteral());
//				String expandedKey = sparqlODataProvider.getRdfModel().getRdfPrefixes().expandPrefix(decodedEntityKey);
//				if (defaultValidator.isValid(expandedKey)) {
//					operationWhere.append("<" + expandedKey + "> ");
//				} else {
//					operationWhere.append("\"" + expandedKey + "\"");
//				}
//
//			}
//			operationWhere.append(")}");
//		}
//
//		SparqlExpressionVisitor filterClause = filterClause(filter, entityType);
//		operationWhere.append(filterClause.getPropertyClause());// propertyClauses(entityType, properties));
//		operationWhere.append(filterClause.getFilterClause());
//
//		return operationWhere.toString();
//	}
//	@Deprecated
//	private String generateExpandWhere(String key, ExpandSelectTreeNode expandSelectTreeNode,
//			HashMap<String, RdfAssociation> expandSelectNavPropertyMap) throws EdmException {
//		StringBuilder expandWhere = new StringBuilder();
//		if (!expandSelectNavPropertyMap.isEmpty()) {
//			expandWhere.append("{")
//					.append(expandSelectTreeNodeWhere(key, expandSelectTreeNode, expandSelectNavPropertyMap))
//					.append("} .");
//		}
//		return expandWhere.toString();
//	}
//	@Deprecated
//	private String expandSelectTreeNodeWhere(String targetKey, ExpandSelectTreeNode expandSelectTreeNode,
//			HashMap<String, RdfAssociation> expandSelectNavPropertyMap) {
//		StringBuilder expandSelectTreeNodeWhere = new StringBuilder();
//		String nextTargetKey = "";
//		for (Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry : expandSelectTreeNode.getLinks()
//				.entrySet()) {
//			nextTargetKey = targetKey + expandSelectTreeNodeLinksEntry.getKey();
//			RdfAssociation navProperty = expandSelectNavPropertyMap.get(expandSelectTreeNodeLinksEntry.getKey());
//			if (navProperty.IsInverse()) {
//				expandSelectTreeNodeWhere.append("?" + nextTargetKey + "_s <" + navProperty.getInversePropertyOfURI()
//						+ "> ?" + targetKey + "_s . ");
//			} else {
//				expandSelectTreeNodeWhere.append("?" + targetKey + "_s <" + navProperty.getAssociationURI() + "> ?"
//						+ nextTargetKey + "_s . ");
//			}
//			if (expandSelectTreeNodeLinksEntry.getValue() != null) {
//				//TODO suppose part of the tree is both an end-of-expand and a expand separately, we would need to add this is both cases.
//				expandSelectTreeNodeWhere.append(expandSelectTreeNodeWhere(nextTargetKey,
//						expandSelectTreeNodeLinksEntry.getValue(), expandSelectNavPropertyMap));
//			} //else{
//				//TODO is this really correct for expand
//			expandSelectTreeNodeWhere.append("?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey
//					+ "_o . ");
//			//TODO expandSelectTreeNodeWhere.append("FILTER(!isIRI(?" + nextTargetKey + "_o) && !isBlank(?" + nextTargetKey + "_o))");
//			//}
//		}
//		if (expandSelectTreeNode.getLinks().isEmpty()) {
//			//	expandSelectTreeNodeWhere.append("?" + targetKey + "_s ?" + targetKey + "_p ?" + targetKey + "_o . ");
//			//TODO expandSelectTreeNodeWhere.append("FILTER(!isIRI(?" + targetKey + "_o) && !isBlank(?" + targetKey + "_o))");
//		}
//		return expandSelectTreeNodeWhere.toString();
//	}
//	@Deprecated
//	private String generateEntityConstruct(String key) {
//		return " ?" + key + "_s ?" + key + "_p ?" + key + "_o .";
//	};
//	@Deprecated
//	private String generateOperationConstruct(RdfEntityType entityType) {
//		StringBuilder operationConstruct = new StringBuilder("[] ");
//		for (RdfProperty property : entityType.getProperties()) {
//			operationConstruct.append(" <" + property.getPropertyURI() + "> ?" + property.varName + " ;");
//		}
//		operationConstruct.deleteCharAt(operationConstruct.length() - 1).append(" .");
//		return operationConstruct.toString();
//	};
//	@Deprecated
//	private String generateExpandConstruct(String key, List<ArrayList<NavigationPropertySegment>> expand,
//			ExpandSelectTreeNode expandSelectTreeNode, HashMap<String, RdfAssociation> expandSelectNavPropertyMap)
//			throws EdmException {
//		//TODO use ExpandSelect tree instead 
//		StringBuilder expandConstruct = new StringBuilder();
//		expandConstruct.append(expandSelectTreeNodeConstruct(key, expandSelectTreeNode, expandSelectNavPropertyMap));
//		return expandConstruct.toString();
//	}
//	@Deprecated
//	private String expandSelectTreeNodeConstruct(String targetKey, ExpandSelectTreeNode expandSelectTreeNode,
//			HashMap<String, RdfAssociation> expandSelectNavPropertyMap) {
//		StringBuilder expandSelectTreeNodeConstruct = new StringBuilder();
//		String nextTargetKey = "";
//		for (Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry : expandSelectTreeNode.getLinks()
//				.entrySet()) {
//			nextTargetKey = targetKey + expandSelectTreeNodeLinksEntry.getKey();
//			RdfAssociation navProperty = expandSelectNavPropertyMap.get(expandSelectTreeNodeLinksEntry.getKey());
//			expandSelectTreeNodeConstruct.append("?" + targetKey + "_s <" + navProperty.getAssociationURI() + "> ?"
//					+ nextTargetKey + "_s .");
//			if ((expandSelectTreeNodeLinksEntry.getValue() != null)
//					&& !expandSelectTreeNodeLinksEntry.getValue().getLinks().isEmpty()) {
//				//TODO suppose part of the tree is both an end-of-expand and a expand separately, we would need to add this is both cases.
//				expandSelectTreeNodeConstruct.append(expandSelectTreeNodeConstruct(nextTargetKey,
//						expandSelectTreeNodeLinksEntry.getValue(), expandSelectNavPropertyMap));
//			} //TODO else {
//			expandSelectTreeNodeConstruct.append("?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey
//					+ "_o .");
//			//TODO}
//		}
//		return expandSelectTreeNodeConstruct.toString();
//	}
//	@Deprecated
//	private String expandSelectTreeNodeSelect(String targetKey, ExpandSelectTreeNode expandSelectTreeNode,
//			HashMap<String, RdfAssociation> expandSelectNavPropertyMap) {
//		StringBuilder expandSelectTreeNodeSelect = new StringBuilder();
//		String nextTargetKey = "";
//		for (Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry : expandSelectTreeNode.getLinks()
//				.entrySet()) {
//			nextTargetKey = targetKey + expandSelectTreeNodeLinksEntry.getKey();
//			RdfAssociation navProperty = expandSelectNavPropertyMap.get(expandSelectTreeNodeLinksEntry.getKey());
//			if (navProperty.IsInverse()) {
//				expandSelectTreeNodeSelect.append("?" + nextTargetKey + "_s <" + navProperty.getInversePropertyOfURI() + "> ?"
//						+ targetKey + "_s .");
//			} else {
//				expandSelectTreeNodeSelect.append("?" + targetKey + "_s <" + navProperty.getAssociationURI() + "> ?"
//						+ nextTargetKey + "_s .");
//			}
//			if ((expandSelectTreeNodeLinksEntry.getValue() != null)
//					&& !expandSelectTreeNodeLinksEntry.getValue().getLinks().isEmpty()) {
//				//TODO suppose part of the tree is both an end-of-expand and a expand separately, we would need to add this is both cases.
//				expandSelectTreeNodeSelect.append(expandSelectTreeNodeSelect(nextTargetKey,
//						expandSelectTreeNodeLinksEntry.getValue(), expandSelectNavPropertyMap));
//			} //TODO else {
//				//expandSelectTreeNodeSelect.append("?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey
//				//		+ "_o .");
//				//TODO}
//		}
//		return expandSelectTreeNodeSelect.toString();
//	}
//@Deprecated
//	SparqlStatement generateEntityCountQuery(RdfEntityType entityType, List<KeyPredicate> oEntityKeys,
//			FilterExpression filter) throws ExceptionVisitExpression, ODataApplicationException, EdmException {
//		String key = entityType.entityTypeName;
//		StringBuilder entityCountQuery;
//		if (entityType.isOperation()) {
//			entityCountQuery = new StringBuilder("SELECT (COUNT( * ) AS ?COUNT) WHERE {");
//			entityCountQuery.append(entityType.queryText);
//			entityCountQuery.append("}");
//			entityCountQuery.append(generateOperationWhere(entityType, oEntityKeys, filter));
//		} else {
//			entityCountQuery = new StringBuilder("SELECT (COUNT( DISTINCT ?" + key + "_s ) AS ?COUNT) WHERE {");
//			//TODO is it correct not tpo have a navigation path in the subselect ... could be required on complex nestd queries
//			entityCountQuery.append(generateEntityWhere(entityType, oEntityKeys, "", filter, 0, 0));
//			entityCountQuery.append("}");
//		}
//		return new SparqlStatement(entityCountQuery.toString());
//	}
//@Deprecated
//	private String generateEntityWhere(RdfEntityType entityType, List<KeyPredicate> oEntityKeys,
//			String expandSelectTreeNodeSelect, FilterExpression filter, Integer top, Integer skip)
//			throws ExceptionVisitExpression, ODataApplicationException {
//		String key = entityType.entityTypeName;
//
//		String primaryKeyClause = primaryKeyClause(entityType, oEntityKeys);
//
//		StringBuilder entityWhere = new StringBuilder();
//
//		SparqlExpressionVisitor filterClause = filterClause(filter, entityType);
//
//		if (this.getProfile().equals(SPARQLProfile.AG)) {
//			entityWhere.append(" { SELECT DISTINCT ?" + key + "_s  WHERE { ");
//		} else {
//			//TODO need to add any navigationproperty with a filter on the dependent property
//			//TODO entityWhere.append(" { SELECT  ?" + key + "_s  WHERE { ");
//			entityWhere.append(" { SELECT  ?" + key + "_s ");
//			entityWhere.append(filterClause.getNavigationPropertySubjects());
//			entityWhere.append(" WHERE { ");
//		}
//
//		entityWhere.append(primaryKeyClause);
//		//TODO SparqlExpressionVisitor filterClause = filterClause(filter, entityType);
//		entityWhere.append(filterClause.getPropertyClause());// propertyClauses(entityType, properties));
//		//TODO add the path through the navigation properties
//		entityWhere.append(expandSelectTreeNodeSelect);
//		entityWhere.append(filterClause.getFilterClause());
//		entityWhere.append("}");
//		String aggregateClause = aggregateClause(key, top, skip, filterClause.getAggregateFilterClause());
//		entityWhere.append(aggregateClause);
//		entityWhere.append("}");
//
//		return entityWhere.toString();
//	}
//@Deprecated
//	private SparqlExpressionVisitor filterClause(FilterExpression filter, RdfEntityType entityType)
//			throws ExceptionVisitExpression, ODataApplicationException {
//		SparqlExpressionVisitor sparqlExpressionVisitor = new SparqlExpressionVisitor(sparqlODataProvider, entityType);
//		if (filter != null) {
//			@SuppressWarnings("unused")
//			String filterClause = (String) filter.accept(sparqlExpressionVisitor);
//		}
//		return sparqlExpressionVisitor;
//
//	}
//@Deprecated
//	private String propertyValuesClause(RdfEntityType entityType, List<SelectItem> selectProperties)
//			throws EdmException {
//		HashMap<String, HashSet<String>> values = new HashMap<String, HashSet<String>>();
//		Boolean invalidClause = false;
//		Boolean emptyClause = true;
//		String selectClause = "";
//		String key = entityType.entityTypeName;
//		if ((selectProperties == null) || selectProperties.isEmpty()) {
//			//TODO If there are no filters on the predicates we cannot assume that the subject will have at least one property from the domain of datatypeproperties.
//			//			HashSet<String> valueProperties = new  HashSet<String>();
//			//			values.put(key, valueProperties);
//			//			selectClause = "VALUES(?" + key + "_p){";
//			//			for (RdfProperty rdfProperty : entityType.getProperties()) {
//			//				if (rdfProperty.propertyNode != null) {
//			//					selectClause += "(<" + rdfProperty.propertyNode.getURI() + ">)";
//			//					valueProperties.add(rdfProperty.propertyNode.getURI().toString());
//			//					emptyClause = false;
//			//				}
//			//			}
//		} else {
//
//			for (SelectItem property : selectProperties) {
//				HashSet<String> valueProperties;
//				RdfEntityType segmentEntityType = entityType;
//				RdfEntityType priorSegmentEntityType = null;
//				key = entityType.entityTypeName;
//				//check property.getNavigationPropertySegments() 
//				// if so then 
//				for (NavigationPropertySegment navigationPropertySegment : property.getNavigationPropertySegments()) {
//					priorSegmentEntityType = segmentEntityType;
//					segmentEntityType = sparqlODataProvider.getRdfEntityTypefromEdmEntitySet(navigationPropertySegment
//							.getTargetEntitySet());
//					key = key + navigationPropertySegment.getNavigationProperty().getName();
//				}
//				if (!values.containsKey(key)) {
//					valueProperties = new HashSet<String>();
//					values.put(key, valueProperties);
//				} else {
//					valueProperties = values.get(key);
//				}
//
//				if (property.isStar()) {
//					for (RdfProperty rdfProperty : segmentEntityType.getProperties()) {
//						if (rdfProperty.propertyNode != null) {
//							valueProperties.add(rdfProperty.propertyNode.getURI().toString());
//							emptyClause = false;
//						}
//					}
//
//				} else if (property.getProperty() != null) {
//					if (!property.getProperty().getName().equals(RdfConstants.SUBJECT)) {
//						RdfProperty rdfProperty = null;
//						try {
//							rdfProperty = segmentEntityType.findProperty(property.getProperty().getName());
//						} catch (EdmException e) {
//							log.error("Failed to locate property:" + property.getProperty().getName());
//						}
//
//						if (rdfProperty.getIsKey()) {
//							//TODO specifically asked for key so should be added to VALUES
//							valueProperties.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
//							emptyClause = false;
//
//						} else {
//							valueProperties.add(rdfProperty.propertyNode.getURI().toString());
//							emptyClause = false;
//						}
//					}
//				} else {
//					//Must be a navigation property
//					RdfAssociation rdfAssociation = null;
//					try {
//						//TODO which of the navigation properties???
//						rdfAssociation = priorSegmentEntityType.findNavigationProperty(property
//								.getNavigationPropertySegments().get(0).getNavigationProperty().getName());
//					} catch (EdmException e) {
//						log.error("Failed to locate navigation property:"
//								+ property.getNavigationPropertySegments().get(0).getNavigationProperty().getName());
//					}
//					//TODO do not add any value restrictions on navigation properties
//					//					if (rdfAssociation.isInverse){
//					//						valueProperties.add(rdfAssociation.getInversePropertyOf().getURI().toString());
//					//					}else{
//					//						valueProperties.add(rdfAssociation.getAssociationURI().toString());
//					//					}
//					//					emptyClause = false;
//				}
//
//			}
//		}
//		if (invalidClause) {
//			return "";
//		} else if (emptyClause) {
//			return "";
//		} else {
//			selectClause = "";
//			for (Entry<String, HashSet<String>> entry : values.entrySet()) {
//				if (!entry.getValue().isEmpty()) {
//					selectClause += "VALUES(?" + entry.getKey() + "_p){";
//					HashSet<String> valueProperties = entry.getValue();
//					for (String propertyURI : valueProperties) {
//						selectClause += "(<" + propertyURI + ">)";
//					}
//					//TODO Virtuoso workaround, performance seems to demand at least two values in a VALUES clause
//					if (entry.getValue().size() == 1 && this.getProfile().equals(RdfConnection.SPARQLProfile.VIRT)) {
//						selectClause += "(<" + valueProperties.toArray()[0] + ">)";
//					}
//					selectClause += "} .";
//				}
//			}
//			return selectClause;
//		}
//	}
//	@Deprecated
//	private String aggregateClause(String key, Integer top, Integer skip, String filterClause) {
//		String aggregateFilter = "";
//		if (top > 0 || skip > 0 || (filterClause != "")) {
//
//			if (this.getProfile().equals(RdfConnection.SPARQLProfile.AG)) {
//				//TODO Version 4.14 seems to have a performance problem with this ORDER BY ... 
//				//TODO aggregateFilter = "ORDER BY ?" + key + "_s ";
//				aggregateFilter = "";
//				if (filterClause != "") {
//					log.warn("HAVING clause might not work without GROUP BY");
//					aggregateFilter = aggregateFilter + "HAVING(AVG(IF(" + filterClause + ",1,0))=1)";
//				}
//			} else {
//				aggregateFilter = "GROUP BY ?" + key + "_s ";
//				if (filterClause != "") {
//					aggregateFilter = aggregateFilter + "HAVING(AVG(IF(" + filterClause + ",1,0))=1)";
//				}
//			}
//			aggregateFilter = aggregateFilter + limitClause(top, skip);
//		}
//		return aggregateFilter;
//	}
//	@Deprecated
//	private String primaryKeyClause(RdfEntityType entityType, List<KeyPredicate> entityKeys) {
//
//		String primaryKeyFilter = null;
//		String key = entityType.entityTypeName;
//		if (entityKeys != null && !entityKeys.isEmpty()) {
//			// VALUES(?s){( :param ) }.
//			primaryKeyFilter = " VALUES(?" + key + "_s) {";
//			for (KeyPredicate entityKey : entityKeys) {
//				String decodedEntityKey = RdfEntity.URLDecodeEntityKey(entityKey.getLiteral());
//				String expandedKey = sparqlODataProvider.getRdfModel().getRdfPrefixes().expandPrefix(decodedEntityKey);
//				primaryKeyFilter += "(<" + expandedKey + ">)";
//			}
//			primaryKeyFilter += "} ";
//		} else {
//			primaryKeyFilter = " ?"
//					+ key
//					+ "_s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class . ?class (<http://www.w3.org/2000/01/rdf-schema#subClassOf>)* <"
//					+ entityType.getEntityTypeNode().getURI() + "> . ";
//
//		}
//		return primaryKeyFilter;
//	}
//	@Deprecated
//	SparqlStatement generateNavPropertyQuery(RdfEntityType targetEntityType, List<KeyPredicate> oEntityKeys,
//			FilterExpression filter, List<RdfAssociation> navProperties, Integer top, Integer skip)
//			throws ExceptionVisitExpression, ODataApplicationException, EdmException {
//		StringBuilder sb = new StringBuilder("CONSTRUCT {");
//		String key;
//
//		key = targetEntityType.entityTypeName;
//
//		if (targetEntityType.isOperation()) {
//
//			sb.append(generateOperationConstruct(targetEntityType));
//			sb.append("} WHERE {" + targetEntityType.queryText);
//			sb.append(limitClause(top, skip) + "}");
//			sb.append(generateOperationWhere(targetEntityType, oEntityKeys, filter));
//
//		} else {
//			sb.append("?" + key + "_s ?" + key + "_p ?" + key + "_o .  } WHERE {");
//
//			sb.append(generateNavPropWhere(targetEntityType, oEntityKeys, filter, navProperties, top, skip));
//
//			sb.append("{ ?" + key + "_s ?" + key + "_p ?" + key + "_o .");
//			// TODO No point returning any IRI as these will be deferred anyway
//			// TODO Could there be a more efficient way of eliminating the predicates such as only searching for datatype predicates?
//			//sb.append("FILTER(!isIRI(?" + key + "_o) && !isBlank(?" + key + "_o) )");
//
//			sb.append(propertyValuesClause(targetEntityType, null));
//
//			sb.append("}");
//
//			sb.append("UNION{BIND(<" + RdfConstants.RDF_SUBJECT + ">  as ?" + key + "_p).BIND(?" + key + "_s  as ?"
//					+ key + "_o).}");
//
//			sb.append("}");
//		}
//
//		return new SparqlStatement(sb.toString());
//	}
//	@Deprecated
//	public SparqlStatement generateNavPropertyCountQuery(RdfEntityType targetEntityType, List<KeyPredicate> oEntityKey,
//			FilterExpression filter, List<RdfAssociation> navProperties, Integer top, Integer skip)
//			throws ExceptionVisitExpression, ODataApplicationException, EdmException {
//		String key = targetEntityType.entityTypeName;
//		StringBuilder sb = new StringBuilder("SELECT (COUNT( DISTINCT ?" + key + "_s ) AS ?COUNT) WHERE {");
//		return new SparqlStatement(sb
//				.append(generateNavPropWhere(targetEntityType, oEntityKey, filter, navProperties, top, skip))
//				.append("}").toString());
//	}
//	@Deprecated
//	private String generateNavPropWhere(RdfEntityType targetEntityType, List<KeyPredicate> oEntityKeys,
//			FilterExpression filter, List<RdfAssociation> navProperties, Integer top, Integer skip)
//			throws ExceptionVisitExpression, ODataApplicationException, EdmException {
//		String key = targetEntityType.entityTypeName;
//		StringBuilder navPropWhere = new StringBuilder();
//		navPropWhere.append(" {");
//		navPropWhere.append("SELECT ?" + key + "_s  WHERE { "
//				+ navPropertyClause(targetEntityType, oEntityKeys, navProperties));
//		SparqlExpressionVisitor filterClause = filterClause(filter, targetEntityType);
//		navPropWhere.append(filterClause.getPropertyClause());
//		navPropWhere.append(filterClause.getFilterClause());
//		navPropWhere.append("}");
//		String aggregateClause = aggregateClause(key, top, skip, filterClause.getAggregateFilterClause());
//		navPropWhere.append(aggregateClause);
//		navPropWhere.append("}");
//
//		return navPropWhere.toString();
//	}
//	@Deprecated
//	private String navPropertyClause(RdfEntityType targetEntityType, List<KeyPredicate> oEntityKeys,
//			List<RdfAssociation> navProperties) throws EdmException {
//
//		StringBuilder propertyClause = new StringBuilder();
//		String key = targetEntityType.entityTypeName;
//		RdfAssociation association;
//
//		String expandedKey = null;
//
//		if (navProperties.get(0).domainClass.isOperation()) {
//			association = navProperties.get(0);
//			propertyClause.append("VALUES(?" + key + "_s){(");
//			for (KeyPredicate entityKey : oEntityKeys) {
//				if (entityKey.getProperty().getName().equals(association.getRelatedKey())) {
//
//					propertyClause.append(sparqlODataProvider.createSPARQLLiteral(entityKey.getLiteral()));
//				}
//			}
//			propertyClause.append(")}");
//		} else { // pure entity
//			association = navProperties.get(0);
//			//TODO Handle multiple keys
//			for (KeyPredicate entityKey : oEntityKeys) {
//				expandedKey = sparqlODataProvider.createSPARQLLiteral(entityKey.getLiteral());
//			}
//			String propertyKey;
//			if (association.IsInverse()) {
//				propertyKey = association.getInversePropertyOfURI().toString();
//				propertyClause.append("?" + key + "_s <" + propertyKey + "> " + expandedKey + " .");
//			} else {
//				propertyKey = association.getAssociationNode().getURI().toString();
//				propertyClause.append(expandedKey + " <" + propertyKey + ">  ?" + key + "_s .");
//			}
//
//		}
//		return propertyClause.toString();
//	}
//	@Deprecated
//	SparqlStatement generateEntityExistsQuery(RdfEntityType entityType, String entityKey)
//			throws ODataApplicationException {
//		UrlValidator urlValidator = new UrlValidator();
//		String key = entityType.entityTypeName;
//		StringBuilder sparql = new StringBuilder("SELECT  (BOUND (?" + key + "_s ) as ?EXISTS)  WHERE {?" + key
//				+ "_s ?" + key + "_p ?" + key + "_o .");
//		String expandedKey = sparqlODataProvider.getRdfModel().getRdfPrefixes()
//				.expandPrefix(RdfEntity.URLDecodeEntityKey(entityKey));
//		if (urlValidator.isValid(expandedKey)) {
//			sparql.append("{VALUES(?" + key + "_s ){(<" + expandedKey + ">)}}");
//			sparql.append("UNION");
//			sparql.append("{VALUES(?" + key + "_o ){(<" + expandedKey + ">)}}");
//			sparql.append("} LIMIT 1");
//			return new SparqlStatement(sparql.toString());
//		} else {
//			throw new ODataApplicationException("Invalid key: " + entityKey, null);
//		}
//	}
}
