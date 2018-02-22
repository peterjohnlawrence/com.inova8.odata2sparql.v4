package com.inova8.odata2sparql.SparqlBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.CustomQueryOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.SparqlExpressionVisitor.SparqlExpressionVisitor;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;
import com.inova8.odata2sparql.uri.UriType;

//	Query Pseudocode
//	================
//	
//	ResourcePath
//	------------
//	
//	Defined by 
//		entitySetUriInfo.getNavigationSegments() and entitySetUriInfo.getTargetEntitySet()
//	or
//		entitySetUriInfo.getStartEntitySet()
//		
//		
//	/entitySet()
//	
//		?resource a [rdfs:subClassOf* :entitySet]
//	
//	/entitySet(:id)
//	
//		VALUES(?resource){(:id0)}
//	
//	/entitySet(:id)/navProp1
//	
//		?id :navProp1  ?resource
//		
//	/entitySet(:id)/navProp1(:id1)
//	
//		?id :navProp1  :id1 .
//		
//	/entitySet(:id){/navPropN(:idN)}*/{navProp}?
//	
//		
//		CONSTRUCT{
//	      ?resource a ?resourceType
//			?resource ?resource_p ?resource_o
//			...#select constructs
//			...#expand constructs
//		} 
//		WHERE {
//			OPTIONAL{ #select=*
//				?resource ?resource_p ?resource_o .
//			}
//			{
//				SELECT #select=*
//					?resource
//	
//				/entitySet()
//				?resource a [rdfs:subClassOf* :entitySet]
//				
//				/entitySet(:id)/navProp1
//				?id :navProp1  ?resource	
//				
//				/entitySet(:id)
//				VALUES(?resource){(:id)}
//	
//				/entitySet(:id)/navProp1(:id1)
//				?id :navProp1  :id1 . #validate relationship
//				VALUES(?resource){(:id1)}
//				
//				/entitySet(:id){/navPropN(:idN)}*/navProp
//				?id :navProp1  :id1 . #validate relationships
//				:id1 :navProp2  :id2 .
//				:id2 :navProp3  :id3 . 
//				...
//				:idN :navProp  ?resource
//				
//			}
//		}
//		
//		
//	Expand
//	------
//	
//	$expand=np1/np2/np3/, np4...
//		CONSTRUCT{
//			...#type construct
//			...#path constructs
//			...#select constructs
//			?resource	:np1	?resource_np1 .
//			?resource_np1 :np2 ?resource_np1_np2 .
//			?resource_np1_np2 :np3 ?resource_np1_np2_np3 .
//			?resource	:np4	?resource_np4 .
//			...
//		} 
//		WHERE {
//			...#select clauses
//			SELECT ?resource ?resource_np1 ?resource_np1_np2 ?resource_np1_np2_np3 ?resource_np4 
//			{
//				...
//				OPTIONAL{
//					?resource	:np1	?resource_np1 .
//					OPTIONAL{
//						?resource_np1 :np2 ?resource_np1_np2 .
//						OPTIONAL{
//							?resource_np1_np2 :np3 ?resource_np1_np2_np3 .
//							...
//						}
//					}
//				}
//				OPTIONAL{
//					?resource	:np4	?resource_np4 .
//				}	
//				SELECT ?resource
//				{
//				...#path clauses
//				}
//			}
//		}
//		
//	Note
//		If no filter conditions on properties within path then path is optional, otherwise not
//		An inverse property swotches subject and object position:
//		
//		$expand=np1/ip2/np3/...
//	
//			CONSTRUCT{
//				...
//				...#path constructs
//				...#select constructs
//				?resource	:np1	?resource_np1 .
//				?resource_np1_ip2 :ip2 ?resource_np1 .
//				?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 
//				...
//			} 
//			WHERE {
//				...#select clauses
//				SELECT ?resource ?resource_np1 ?resource_np1_ip2 ?resource_np1_ip2_np3 
//				{
//					...
//					...#path clauses
//					...
//					OPTIONAL{	#/np1/
//						?resource	:np1	?resource_np1 .
//						OPTIONAL{	#/np1/np2/
//							?resource_np1_ip2 :ip2 ?resource_np1 .
//							OPTIONAL{	#/np1/ip2/np3/
//								?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
//								...
//							}
//						}
//					}
//					SELECT ?resource
//					{
//					...#path clauses
//					}		
//				}
//			}
//		
//	Select
//	------
//	Note
//		Selected values must already appear in path
//		
//	$select=dpa, np1/dpb, np1/np2/dpc, ...
//	
//		CONSTRUCT{
//			...
//			...#expand constructs
//			?resource	?resource_p   ?resource_o .
//			?resource_np1	?resource_np1_p ?resource_np1_o  .
//			?resource_np1_np2 ?resource_np1_np2_p ?resource_np1_np2_o .	
//			...
//		} 
//		WHERE {	#/
//			OPTIONAL {
//				?resource ?resource_p ?resource_o .
//				VALUES(?resource_p){(:dpa)}
//			}	
//			OPTIONAL { ?resource :np1 ?resource_np1 . 
//			|| based on if path has filter associated
//			{	#/np1/
//				OPTIONAL {
//					?resource_np1 ?resource_np1_p ?resource_np1_o .
//					VALUES(?resource_np1_p){(:dpb)}
//				}
//				OPTIONAL { ?resource_np1 :np2 ?resource_np1_np2 . 
//				|| based on if path has filter associated
//				{	#/np1/np2/
//					OPTIONAL {
//						?resource_np1_np2 ?resource_np1_np2_p ?resource_np1_np2_o .
//						VALUES(?resource_np1_np2_p){(:dpc)}
//					}
//					...
//				}
//			}
//			{
//				SELECT ?resource ?resource_np1 ?resource_np1_np2  
//				...#path clauses
//				...#expand clauses
//			}
//		}
//	
//	Filter
//	------
//	Note
//		Filtered values must already appear in path
//		
//	$filter=condition({npN/}*dpN)
//		
//		CONSTRUCT{
//			...
//			...#expand constructs
//			...#select constructs
//			...
//		} WHERE 
//		{
//			...
//			...#select clauses
//			...
//			{	SELECT ?resource ?resource_np1 ?resource_np1_ip2 ?resource_np1_ip2_np3 
//				WHERE {
//					...
//					...#path clauses
//					...
//					{	#filter=condition(dp)
//						?resource :dp ?resource_dp_o .
//						FILTER(condition(?resource_sp_o))			
//					}
//					{	#/np1/
//						?resource	:np1	?resource_np1 .
//						{	#filter=condition(np1/dp1)
//							?resource_np1 :dp1 ?resource_dp1_o .
//							FILTER(condition(?resource_dp1_o))					
//						}
//						{	#/np1/np2/
//							?resource_np1 :np2 ?resource_np1_np2  .
//							{	#filter=condition(np1/np2/dp2)
//								?resource_np1_np2 :dp2 ?resource_np1_np2_dp2_o.
//								FILTER(condition(?resource_np1_np2_dp2_o))					
//							}
//							{	#/np1/ip2/np3/
//								?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
//								...
//							}
//						}
//					}
//					SELECT DISTINCT
//						?resource
//					WHERE {
//						...#path clauses
//						{	#filter=condition(dp)
//							?resource :dp ?resource_dp_o .
//							FILTER(condition(?resource_sp_o))			
//						}
//						{	#/np1/
//							?resource	:np1	?resource_np1 .
//							{	#filter=condition(np1/dp1)
//								?resource_np1 :dp1 ?resource_dp1_o .
//								FILTER(condition(?resource_dp1_o))					
//							}
//							{	#/np1/np2/
//								?resource_np1 :np2 ?resource_np1_np2  .
//								{	#filter=condition(np1/np2/dp2)
//									?resource_np1_np2 :dp2 ?resource_np1_np2_dp2_o.
//									FILTER(condition(?resource_np1_np2_dp2_o))					
//								}
//								{	#/np1/ip2/np3/
//									?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
//									...
//								}
//							}
//						}				
//					}	GROUP BY ?resource LIMIT $top		
//				}
//			}
//		}

public class SparqlQueryBuilder {
	

	private final Logger log = LoggerFactory.getLogger(SparqlQueryBuilder.class);
	private final RdfModel rdfModel;
	private final RdfModelToMetadata rdfModelToMetadata;

	private final UriType uriType;
	private final UriInfo uriInfo;

	private RdfEntityType rdfEntityType = null;
	private RdfEntityType rdfTargetEntityType = null;
	private EdmEntitySet edmEntitySet = null;
	private EdmEntitySet edmTargetEntitySet = null;
	private ExpandOption expandOption;
	//private SelectOption selectOption;

	private Boolean isPrimitiveValue = false;
	private SparqlExpressionVisitor filterClause;
	private SparqlFilterClausesBuilder filterClauses;
	private HashSet<String> selectPropertyMap;

	private static final boolean DEBUG = true;

	public SparqlQueryBuilder(RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, UriInfo uriInfo,
			UriType uriType) throws EdmException, ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		super();
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.uriInfo = uriInfo;
		this.uriType = uriType;
		// Prepare what is required to create the SPARQL
		prepareBuilder();
		log.info("Builder for URIType: " + uriType.toString());
	}

	private void prepareBuilder() throws EdmException, ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		// Prepare what is required to create the SPARQL
		// get
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourceParts.get(0);
		this.edmEntitySet = uriResourceEntitySet.getEntitySet();
		this.rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		// By default
		this.edmTargetEntitySet = edmEntitySet;
		this.rdfTargetEntityType = rdfEntityType;
		UriResource lastSegment;
		this.expandOption = uriInfo.getExpandOption();
		//this.selectOption = uriInfo.getSelectOption();
		// TODO V2 expandSelectTreeNode =
		// UriParser.createExpandSelectTree(uriInfo.getSelectOption(),
		// uriInfo.getExpandOption());
		// TODO V2 expandSelectNavPropertyMap =
		// createExpandSelectNavPropertyMap(uriInfo.getSelectOption(),
		// uriInfo.getExpandOption());
		filterClauses = new SparqlFilterClausesBuilder( rdfModel,  rdfModelToMetadata,  uriInfo,
				 uriType);
		switch (this.uriType) {
		case URI1: {
			edmTargetEntitySet = edmEntitySet;
			rdfTargetEntityType = rdfEntityType;
			filterClause = filterClauses.getFilterClause();//filterClause(uriInfo.getFilterOption(), rdfEntityType);
		}
			break;
		case URI2: {
			edmTargetEntitySet = edmEntitySet;
			rdfTargetEntityType = rdfEntityType;
		}
			break;
		case URI5: {
			UriResource lastResourcePart = resourceParts.get(resourceParts.size() - 1);
			int minSize = 2;
			if (lastResourcePart.getSegmentValue().equals("$value")) {
				minSize++;
				this.setIsPrimitiveValue(true);
			}
			lastSegment = resourceParts.get(resourceParts.size() - minSize);
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			}
		}
			break;
		case URI6A: {
			lastSegment = resourceParts.get(resourceParts.size() - 1);
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			}
		}
			break;
		case URI6B: {
			lastSegment = resourceParts.get(resourceParts.size() - 1);
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
				filterClause = filterClauses.getFilterClause();//filterClause(uriInfo.getFilterOption(), rdfTargetEntityType);
			}
		}
			break;
		case URI7A: {

			break;
		}
		case URI7B: {

			break;
		}
		case URI15: {
			lastSegment = resourceParts.get(resourceParts.size() - 2);
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
				filterClause = filterClauses.getFilterClause();//filterClause(uriInfo.getFilterOption(), rdfTargetEntityType);
			} else {
				filterClause = filterClauses.getFilterClause();//filterClause(uriInfo.getFilterOption(), rdfEntityType);
			}
		}
			break;
		case URI16: {
			throw new ODataApplicationException("Invalid request type " + this.uriType.toString(),
					HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
		}
		default:
			throw new ODataApplicationException("Unhandled request type " + this.uriType.toString(),
					HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
		}
		selectPropertyMap = createSelectPropertyMap(rdfTargetEntityType,uriInfo.getSelectOption());
	}

	public SparqlStatement prepareConstructSparql()
			throws EdmException, ODataApplicationException, OData2SparqlException, ExpressionVisitException {

		StringBuilder prepareConstruct = new StringBuilder("");
		prepareConstruct.append(this.rdfModel.getRdfPrefixes().sparqlPrefixes());
		prepareConstruct.append(construct());
		prepareConstruct.append("WHERE {\n");
		prepareConstruct.append(where());
		prepareConstruct.append("}");
		prepareConstruct.append(defaultLimitClause());
		return new SparqlStatement(prepareConstruct.toString());
	}

	public SparqlStatement prepareCountEntitySetSparql()
			throws ODataApplicationException, EdmException, OData2SparqlException {

		StringBuilder prepareCountEntitySet = new StringBuilder("");
		prepareCountEntitySet.append(this.rdfModel.getRdfPrefixes().sparqlPrefixes());
		prepareCountEntitySet.append("\t").append("SELECT ");
		prepareCountEntitySet.append("(COUNT(DISTINCT *").append(") AS ?COUNT)").append("\n");
		prepareCountEntitySet.append(selectExpandWhere(""));
		return new SparqlStatement(prepareCountEntitySet.toString());
	}

//	public SparqlStatement prepareExistsEntitySetQuery() throws ODataApplicationException, EdmException {
//
//		StringBuilder prepareCountEntitySet = new StringBuilder("");
//		prepareCountEntitySet.append("\t").append("SELECT ");
//		prepareCountEntitySet.append("(BOUND(?" + edmTargetEntitySet.getEntityType().getName() + "_s")
//				.append(expandSelectTreeNodeVariables(rdfTargetEntityType.entityTypeName, this.expandSelectTreeNode))
//				.append(") AS ?EXISTS)").append("\n");
//		prepareCountEntitySet.append(selectExpandWhere("")).append("LIMIT 1");
//		return new SparqlStatement(prepareCountEntitySet.toString());
//	}

//	private SparqlExpressionVisitor filterClause(FilterOption filter, RdfEntityType entityType)
//			throws ODataApplicationException, ExpressionVisitException {
//		SparqlExpressionVisitor sparqlExpressionVisitor = new SparqlExpressionVisitor(rdfModel, rdfModelToMetadata,
//				entityType,"");
//		if (filter != null) {
//			Expression filterExpression = filter.getExpression();
//			final Object visitorResult;
//
//			final String result;
//			// if (filter != null) {
//
//			visitorResult = filterExpression.accept(sparqlExpressionVisitor);
//			result = new String((String) visitorResult);
//			sparqlExpressionVisitor.setConditionString(result);
//			// }
//		}
//		return sparqlExpressionVisitor;
//	}

	private StringBuilder construct() throws EdmException {
		StringBuilder construct = new StringBuilder("CONSTRUCT {\n");
		if (this.rdfTargetEntityType.isOperation()) {
			construct.append(constructOperation(rdfTargetEntityType, "",false));
		} else {
			construct.append(targetEntityIdentifier(edmTargetEntitySet.getEntityType().getName(), "\t"));
			construct.append(constructType(rdfTargetEntityType, edmTargetEntitySet.getEntityType().getName(), "\t"));
			construct.append(constructPath());
		}
		construct.append(constructExpandSelect());
		construct.append("}\n");
		return construct;
	}

	private StringBuilder targetEntityIdentifier(String key, String indent) throws EdmException {
		StringBuilder targetEntityIdentifier = new StringBuilder();
		if (DEBUG)
			targetEntityIdentifier.append(indent).append("#targetEntityIdentifier\n");
		targetEntityIdentifier.append(indent).append("?" + key + "_s <" + RdfConstants.TARGETENTITY + "> true .\n");
		return targetEntityIdentifier;
	}

	private StringBuilder constructType(RdfEntityType rdfEntityType, String key, String indent) throws EdmException {
		StringBuilder constructType = new StringBuilder();
		if (DEBUG)
			constructType.append(indent).append("#constructType\n");
		String type = rdfEntityType.getIRI();
		constructType.append(indent).append("?" + key + "_s <" + RdfConstants.ASSERTEDTYPE + "> <" + type + "> .\n");
		return constructType;
	}

	private StringBuilder constructOperation(RdfEntityType rdfOperationType, String indent, Boolean isExpand) throws EdmException {
		StringBuilder constructOperation = new StringBuilder();
		if (DEBUG)
			constructOperation.append(indent).append("#constructOperation\n");
		String type = rdfOperationType.getIRI();
		constructOperation.append(indent + "\t");
		constructOperation.append("?"+rdfOperationType.getEntityTypeName() +"_key ");
		if(isExpand)
			constructOperation.append("<" + RdfConstants.ASSERTEDTYPE + "> <" + type + "> ;\n");
		else
			constructOperation.append("<http://targetEntity> true ; <" + RdfConstants.ASSERTEDTYPE + "> <" + type + "> ;\n");
		for (RdfProperty property : rdfOperationType.getProperties()) {
			constructOperation.append(indent + "\t\t")
					.append(" <" + property.getPropertyURI() + "> ?" + property.getVarName() + " ;\n");
		}
		constructOperation.replace(constructOperation.length() - 2, constructOperation.length() - 1, ".");
		return constructOperation;
	}

	private StringBuilder constructPath() throws EdmException {
		StringBuilder constructPath = new StringBuilder();
		if (DEBUG)
			constructPath.append("\t#constructPath\n");
		String key = edmTargetEntitySet.getEntityType().getName();
		constructPath.append("\t").append("?" + key + "_s ?" + key + "_p ?" + key + "_o .\n");
		return constructPath;
	}

	private StringBuilder constructExpandSelect() throws EdmException {
		StringBuilder constructExpandSelect = new StringBuilder();
		if (DEBUG)
			constructExpandSelect.append("\t#constructExpandSelect\n");
		if (this.expandOption != null)
			constructExpandSelect.append(expandItemsConstruct(rdfTargetEntityType, rdfTargetEntityType.entityTypeName,
					this.expandOption.getExpandItems(), "\t"));//rdfTargetEntityType.entityTypeName, this.expandOption.getExpandItems(), "\t"));
		return constructExpandSelect;
	}

	private StringBuilder where() throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
		StringBuilder where = new StringBuilder();
		if (this.rdfTargetEntityType.isOperation()) {
			where.append(clausesOperationProperties(this.rdfTargetEntityType));
		} else {
			where.append(clausesPathProperties());
		}
		where.append(clausesExpandSelect());
		if (this.rdfEntityType.isOperation()) {
			where.append(selectOperation());
		} else if (this.rdfTargetEntityType.isOperation()) {
			where.append(selectOperation());
		} else {
			where.append(selectExpand());
		}
		return where;
	}

	private StringBuilder clausesPathProperties() throws EdmException {
		StringBuilder clausesPathProperties = new StringBuilder();
		if (DEBUG)
			clausesPathProperties.append("\t#clausesPathProperties\n");
		clausesPathProperties.append(clausesSelect(this.selectPropertyMap,edmTargetEntitySet.getEntityType().getName(),
				edmTargetEntitySet.getEntityType().getName(), rdfTargetEntityType, "\t"));
		return clausesPathProperties;
	}

	private StringBuilder clausesOperationProperties(RdfEntityType rdfOperationType)
			throws EdmException, OData2SparqlException {
		StringBuilder clausesOperationProperties = new StringBuilder();
		if (DEBUG)
			clausesOperationProperties.append("\t#clausesOperationProperties\n");
		clausesOperationProperties.append("\t").append(filterOperationQuery(rdfOperationType)).append("\n");
		clausesOperationProperties.append("\t{\n").append(preprocessOperationQuery(rdfOperationType)).append("\t}\n");
		clausesOperationProperties.append("BIND(UUID()  AS ?" + rdfOperationType.getEntityTypeName() + "_key)\n");
		return clausesOperationProperties;
	}

	private StringBuilder filterOperationQuery(RdfEntityType rdfOperationType)
			throws EdmException, OData2SparqlException {
		StringBuilder filter = new StringBuilder();
		if (DEBUG)
			filter.append("#operationFilter\n");
		if (filterClause != null)
			filter.append(filterClauses.getFilter()).append("\n");
		return filter;
	}

	private String getQueryOptionText(List<CustomQueryOption> queryOptions, String parameter) {

		for (CustomQueryOption queryOption : queryOptions) {
			if (queryOption.getName().equals(parameter))
				return queryOption.getText();
		}
		return null;

	}

	private String preprocessOperationQuery(RdfEntityType rdfOperationType) throws EdmException, OData2SparqlException {
		List<CustomQueryOption> queryOptions = null;
		if (rdfOperationType.isFunctionImport()) {
			queryOptions = uriInfo.getCustomQueryOptions();
		}
		String queryText = rdfOperationType.queryText;
		for (Entry<String, com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter> functionImportParameterEntry : rdfOperationType
				.getFunctionImportParameters().entrySet()) {
			com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter functionImportParameter = functionImportParameterEntry
					.getValue();
			String parameterValue = getQueryOptionText(queryOptions, functionImportParameter.getName());

			if (parameterValue != null) {
				queryText = queryText.replaceAll("\\?" + functionImportParameter.getName(), parameterValue);
			} else {
				if (!functionImportParameter.isNullable())
					throw new OData2SparqlException(
							"FunctionImport cannot be called without values for non-nullable parameters");
			}
		}
		if ((uriType != UriType.URI15) && (uriType != UriType.URI6B)) {
			queryText += limitClause();
		}
		return queryText;
	}

	private StringBuilder clausesExpandSelect() throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
		StringBuilder clausesExpandSelect = new StringBuilder();
		if (DEBUG)
			clausesExpandSelect.append("\t#clausesExpandSelect\n");
		if (this.expandOption != null) {
			clausesExpandSelect.append(expandItemsWhere(rdfTargetEntityType, rdfTargetEntityType.entityTypeName,
					this.expandOption.getExpandItems(), "\t"));
		}
		return clausesExpandSelect;
	}

	private StringBuilder selectOperation() throws EdmException {
		StringBuilder selectOperation = new StringBuilder();
		if (DEBUG)
			selectOperation.append("\t#selectOperation\n");
		selectOperation.append(clausesPath_KeyPredicateValues("\t"));
		return selectOperation;
	}

	private StringBuilder selectExpand() throws EdmException, OData2SparqlException {
		StringBuilder selectExpand = new StringBuilder();
		if (DEBUG)
			selectExpand.append("\t#selectExpand\n");
		selectExpand.append("\t").append("{\tSELECT\n");
		selectExpand.append("\t\t\t").append("?" + edmTargetEntitySet.getEntityType().getName() + "_s");
		// TODO V2 what about select option?
		if (this.expandOption != null)
			selectExpand.append(filterClauses.getExpandItemVariables());
					//expandItemsVariables(rdfTargetEntityType, rdfTargetEntityType.entityTypeName, this.expandOption));
		selectExpand.append("\n");
		selectExpand.append(selectExpandWhere("\t\t"));
		selectExpand.append("\t").append("}\n");
		return selectExpand;
	}

	private StringBuilder selectExpandWhere(String indent) throws EdmException, OData2SparqlException {
		StringBuilder selectExpandWhere = new StringBuilder();
		if (DEBUG)
			selectExpandWhere.append(indent).append("#selectExpandWhere\n");
		selectExpandWhere.append(indent).append("WHERE {\n");
		selectExpandWhere.append(filter(indent + "\t"));
		switch (uriType) {
		case URI1:
			// nothing required for any entitySet query
			break;
		case URI2:
		case URI5:
		case URI6A:
		case URI6B:
		case URI15:
		case URI16:
			selectExpandWhere.append(clausesPath(indent + "\t"));
			break;
		default:
			selectExpandWhere.append("#Unhandled URIType: " + this.uriType + "\n");
		}
		selectExpandWhere.append(clausesFilter(indent + "\t"));
		selectExpandWhere.append(clausesExpandFilter(indent + "\t"));
		switch (uriType) {
		case URI1:
			selectExpandWhere.append(selectPath());
			selectExpandWhere.append(indent).append("}\n");
			break;
		case URI2:
		case URI5:
		case URI6A:
		case URI15:
		case URI16:
			selectExpandWhere.append(indent).append("}\n");
			break;
		case URI6B:
			selectExpandWhere.append(indent).append("} ").append(limitClause()).append("\n");
			break;
		default:
			selectExpandWhere.append("#Unhandled URIType: " + this.uriType + "\n");
		}

		return selectExpandWhere;
	}

	private StringBuilder filter(String indent) {
		StringBuilder filter = new StringBuilder().append(indent);
		if (DEBUG)
			filter.append("#filter\n");
		StringBuilder filterClause= filterClauses.getFilter();
		if (filterClause.length() > 0)
			filter.append(indent).append(filterClause).append("\n");
		return filter;
	}

	private StringBuilder exists(String indent) {
		StringBuilder exists = new StringBuilder();
		if (DEBUG)
			exists.append(indent).append("#exists\n");
		exists.append(indent);
		exists.append("{{?" + rdfEntityType.entityTypeName + "_s ?p1 ?o1 } UNION {?s1 ?" + rdfEntityType.entityTypeName
				+ "_s ?o1} UNION {?s3 ?p3 ?" + rdfEntityType.entityTypeName + "_s} }\n");
		return exists;
	}

	private StringBuilder clausesPath(String indent) throws EdmException, OData2SparqlException {
		StringBuilder clausesPath = new StringBuilder().append(indent);
		if (DEBUG)
			clausesPath.append("#clausesPath\n");
		switch (this.uriType) {
		case URI1: {
			clausesPath.append(clausesPath_URI1(indent));
			break;
		}
		case URI2: {
			clausesPath.append(clausesPath_URI2(indent));
			break;
		}
		case URI5: {
			clausesPath.append(clausesPath_URI5(indent));
			break;
		}
		case URI6A: {
			clausesPath.append(clausesPath_URI2(indent));
			break;
		}
		case URI6B: {
			clausesPath.append(clausesPath_URI1(indent));
			break;
		}
		case URI15: {
			clausesPath.append(clausesPath_URI15(indent));
			break;
		}
		case URI16: {
			clausesPath.append(clausesPath_URI16(indent));
			clausesPath.append(exists(indent));
			break;
		}
		default:
			clausesPath.append("#Unhandled URIType: " + this.uriType + "\n");
			break;
		}
		return clausesPath;
	}

	private StringBuilder valuesSubClassOf(RdfEntityType rdfEntityType) {
		StringBuilder valuesSubClassOf = new StringBuilder();
		valuesSubClassOf.append("VALUES(?class){").append("(<" + rdfEntityType.getIRI() + ">)");
		for (RdfEntityType subType : rdfEntityType.getAllSubTypes()) {
			valuesSubClassOf.append("(<" + subType.getIRI() + ">)");
		}
		return valuesSubClassOf;
	}

	private StringBuilder clausesPath_URI1(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (uriInfo.getUriResourceParts().size() > 1) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(indent).append("?" + rdfEntityType.entityTypeName
					+ "_s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n");
			// clausesPath.append(indent).append(
			// "?class (<http://www.w3.org/2000/01/rdf-schema#subClassOf>)* <" +
			// rdfEntityType.getIRI() + "> .\n");

			clausesPath.append(indent).append(valuesSubClassOf(rdfEntityType)).append("}\n");
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI2(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (uriInfo.getUriResourceParts().size() > 1) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI5(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();

		if (uriInfo.getUriResourceParts().size() > (this.isPrimitiveValue() ? 3 : 2)) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI15(String indent) throws EdmException, OData2SparqlException {
		StringBuilder clausesPath = new StringBuilder();
		if (uriInfo.getUriResourceParts().size() > 2) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			if (rdfTargetEntityType.isOperation()) {
				clausesPath.append(indent).append(preprocessOperationQuery(rdfTargetEntityType));
			} else {
				clausesPath.append(indent).append("?" + rdfEntityType.entityTypeName
						+ "_s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n");
				// clausesPath.append(indent).append(
				// "?class (<http://www.w3.org/2000/01/rdf-schema#subClassOf>)*
				// <"
				// + rdfEntityType.getIRI() + "> .\n");
				clausesPath.append(indent).append(valuesSubClassOf(rdfEntityType)).append("}\n");
			}
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI16(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (uriInfo.getUriResourceParts().size() > 2) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder primaryKey_Variables(RdfEntityType rdfentityType) {
		StringBuilder primaryKey_Variables = new StringBuilder();
		if (rdfEntityType.isOperation()) {
			for (RdfPrimaryKey primaryKey : rdfEntityType.getPrimaryKeys()) {
				primaryKey_Variables.append("?").append(primaryKey.getPrimaryKeyName()).append(" ");
			}
		} else {
			primaryKey_Variables.append("?").append(rdfEntityType.entityTypeName).append("_s");
		}
		return primaryKey_Variables;
	}

	private StringBuilder clausesPath_KeyPredicateValues(String indent) throws EdmException {
		StringBuilder clausesPath_KeyPredicateValues = new StringBuilder();
		String key = "";
		int segmentSize = uriInfo.getUriResourceParts().size();
		if ((uriType == UriType.URI4) || (uriType == UriType.URI5) || (uriType == UriType.URI15))
			segmentSize--;
		if (rdfEntityType.isOperation()) {
			// TODO make sure not a complex or value resourceParts URI5 URI3
			if (segmentSize == 1) {
				key = primaryKey_Variables(rdfEntityType).toString();
				// for (RdfPrimaryKey primaryKey :
				// rdfEntityType.getPrimaryKeys()) {
				// key = key + "?" + primaryKey.getPrimaryKeyName() + " ";
				// }
			} else {
				if (segmentSize > 2) {
					log.error(
							"Too many navigation properties for operation:" + uriInfo.getUriResourceParts().toString());
				} else {
					RdfAssociation navProperty = rdfEntityType
							.findNavigationProperty(uriInfo.getUriResourceParts().get(1).getSegmentValue());
					key = rdfTargetEntityType.entityTypeName;
					clausesPath_KeyPredicateValues.append(indent).append("VALUES(?" + key + "_s)");
					// TODO to get key predicates for function import
					String keyPredicate = navProperty.getVarName();
					// if (uriInfo.getKeyPredicates() != null &&
					// !uriInfo.getKeyPredicates().isEmpty()) {
					if (((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates().size() != 0) {
						for (UriParameter entityKey : ((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0))
								.getKeyPredicates()) {
							if (entityKey.getName().equals(keyPredicate)) {
								String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey.getText());
								String expandedKey = rdfModel.getRdfPrefixes()
										.expandPrefix(decodedEntityKey.substring(1, decodedEntityKey.length() - 1));
								clausesPath_KeyPredicateValues.append("{(<" + expandedKey + ">)}");
							}
						}
					}
					return clausesPath_KeyPredicateValues;
				}
			}
		} else if (rdfTargetEntityType.isOperation()) {
			// TODO make sure not a complex or value resourceParts
			if (segmentSize > 2) {
				log.error("Too many navigation properties for operation:" + uriInfo.getUriResourceParts().toString());
			} else {
				RdfAssociation navProperty = rdfEntityType
						.findNavigationProperty(uriInfo.getUriResourceParts().get(1).getSegmentValue());
				if (navProperty != null) {
					key = "?" + rdfTargetEntityType
							.findNavigationProperty(navProperty.getInversePropertyOf().getLocalName()).getVarName();
				} else {
					log.error("Failed to locate operation navigation property:"
							+ uriInfo.getUriResourceParts().get(0).getSegmentValue());
				}
			}
		} else {
			key = primaryKey_Variables(rdfEntityType).toString();
			// key = "?" + rdfEntityType.entityTypeName + "_s";
		}

		if (((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates().size() != 0) {
			clausesPath_KeyPredicateValues.append(indent).append("VALUES(" + key + ") {(");
			for (UriParameter entityKey : ((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0))
					.getKeyPredicates()) {
				String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey.getText());
				// String leading and trailing single quote from key
				String expandedKey = rdfModel.getRdfPrefixes()
						.expandPrefix(decodedEntityKey.substring(1, decodedEntityKey.length() - 1));
				if (expandedKey.equals(RdfConstants.SPARQL_UNDEF)) {
					clausesPath_KeyPredicateValues.append(RdfConstants.SPARQL_UNDEF);
				} else {
					clausesPath_KeyPredicateValues.append("<" + expandedKey + ">");
				}

			}
			clausesPath_KeyPredicateValues.append(")}\n");
		}
		return clausesPath_KeyPredicateValues;
	}

	private StringBuilder clausesPathNavigation(String indent, List<UriResource> navigationSegments,
			List<UriParameter> entityKeys) throws EdmException {
		StringBuilder clausesPathNavigation = new StringBuilder();

		String path = edmTargetEntitySet.getEntityType().getName();
		boolean isFirstSegment = true;
		Integer index = 0;
		String pathVariable = "";
		String targetVariable = "";
		int segmentSize = navigationSegments.size();
		if ((uriType == UriType.URI5) || (uriType == UriType.URI4) || (uriType == UriType.URI15))
			segmentSize--;
		if (this.isPrimitiveValue())
			segmentSize--;
		Integer lastIndex = segmentSize;
		for (index = 1; index < segmentSize; index++) {
//			UriResource priorNavigationSegment = navigationSegments.get(index - 1);
//			String namespace = "";
//			if (priorNavigationSegment.getKind().equals(UriResourceKind.entitySet)) {
//				namespace = ((UriResourceEntitySet) priorNavigationSegment).getEntitySet().getEntityType()
//						.getNamespace();
//			} else if (priorNavigationSegment.getKind().equals(UriResourceKind.navigationProperty)) {
//				namespace = ((UriResourceNavigation) priorNavigationSegment).getProperty().getType().getNamespace();
//			}
			// index++;
			UriResource navigationSegment = navigationSegments.get(index);
			EdmNavigationProperty predicate = ((UriResourceNavigation) navigationSegment).getProperty();
			RdfAssociation navProperty = rdfModelToMetadata.getMappedNavigationProperty(
					new FullQualifiedName(predicate.getType().getNamespace(), predicate.getName()));
			if (isFirstSegment) {
				// Not possible to have more than one key field is it?
				for (UriParameter entityKey : entityKeys) {
					String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey.getText());
					String expandedKey = rdfModel.getRdfPrefixes()
							.expandPrefix(decodedEntityKey.substring(1, decodedEntityKey.length() - 1));
					pathVariable = "<" + expandedKey + ">";
				}
			} else {
				pathVariable = "?" + path + "_s";
			}
			if (index.equals(lastIndex - 1)) {
				targetVariable = "?" + edmTargetEntitySet.getEntityType().getName() + "_s";
			} else {
				targetVariable = "?" + path + navProperty.getAssociationName() + "_s";
			}
			if (navProperty.IsInverse()) {
				clausesPathNavigation.append(indent).append("{\n");
				clausesPathNavigation.append(indent).append("\t"+ targetVariable + " <"	+ navProperty.getInversePropertyOf().getIRI() + "> " + pathVariable + " .\n");
				clausesPathNavigation.append(indent).append("} UNION {\n");
				clausesPathNavigation.append(indent).append("\t"+ pathVariable + " <" + navProperty.getAssociationIRI() + "> " + targetVariable + " .\n");
				clausesPathNavigation.append(indent).append("}\n");
			} else {
				clausesPathNavigation.append(indent)
						.append(pathVariable + " <" + navProperty.getAssociationIRI() + "> " + targetVariable + " .\n");
			}
			path += predicate.getName();
			isFirstSegment = false;

		}
		return clausesPathNavigation;
	}

	private StringBuilder clausesExpandFilter(String indent) {
		StringBuilder clausesExpandFilter = new StringBuilder().append(indent);
		if (DEBUG)
			clausesExpandFilter.append("#clausesExpandFilter\n");
		if (this.expandOption != null)
			clausesExpandFilter.append(filterClauses.getClausesExpandFilter(indent));
					//.append(expandItemsFilter(rdfEntityType, rdfEntityType.entityTypeName, this.expandOption, indent));
		return clausesExpandFilter;
	}

	private StringBuilder selectPath() throws EdmException, OData2SparqlException {
		StringBuilder selectPath = new StringBuilder();

		if (DEBUG)
			selectPath.append("\t\t\t#selectPath\n");
		selectPath.append("\t\t\t").append("{\tSELECT DISTINCT\n");
		selectPath.append("\t\t\t\t\t").append("?" + edmTargetEntitySet.getEntityType().getName() + "_s\n");
		selectPath.append("\t\t\t\t").append("WHERE {\n");
		selectPath.append(filter("\t\t\t\t\t"));
		selectPath.append(clausesPath("\t\t\t\t\t"));
		selectPath.append(clausesFilter("\t\t\t\t\t"));
		selectPath.append(clausesExpandFilter("\t\t\t\t\t"));
		selectPath.append("\t\t\t\t").append("} GROUP BY ?" + edmTargetEntitySet.getEntityType().getName() + "_s")
				.append(limitClause()).append("\n");
		selectPath.append("\t\t\t").append("}\n");
		return selectPath;
	}

	private StringBuilder clausesFilter(String indent) {
		StringBuilder clausesFilter = new StringBuilder().append(indent);
		if (DEBUG)
			clausesFilter.append("#clausesFilter\n");
		if (this.filterClause != null
				&& this.filterClause.getNavPropertyPropertyFilters().get(rdfTargetEntityType.entityTypeName) != null) {
			//clausesFilter.append(clausesFilter(null, rdfTargetEntityType.entityTypeName, indent, this.filterClause.getNavPropertyPropertyFilters().get(rdfTargetEntityType.entityTypeName).getPropertyFilters()));
			clausesFilter.append(filterClauses.getClausesFilter(indent));
		} else {
			// clausesFilter.append(clausesFilter(null,
			// rdfEntityType.entityTypeName, indent,null));
		}
		return clausesFilter;
	}

	//	private HashMap<String, RdfAssociation> createExpandSelectNavPropertyMap(List<SelectItem> select,
	//			List<ArrayList<NavigationPropertySegment>> expand) throws EdmException {
	//		HashMap<String, RdfAssociation> expandSelectNavPropertyMap = new HashMap<String, RdfAssociation>();
	//		for (SelectItem selectItem : select) {
	//			for (NavigationPropertySegment navigationPropertySegment : selectItem.getNavigationPropertySegments()) {
	//				expandSelectNavPropertyMap.put(navigationPropertySegment.getNavigationProperty().getName(),
	//						rdfModelToMetadata.getMappedNavigationProperty(
	//								navigationPropertySegment.getNavigationProperty().getRelationship()));
	//			}
	//		}
	//		for (ArrayList<NavigationPropertySegment> navigationPropertySegments : expand) {
	//			for (NavigationPropertySegment navigationPropertySegment : navigationPropertySegments) {
	//				expandSelectNavPropertyMap.put(navigationPropertySegment.getNavigationProperty().getName(),
	//						rdfModelToMetadata.getMappedNavigationProperty(
	//								navigationPropertySegment.getNavigationProperty().getRelationship()));
	//			}
	//		}
	//		return expandSelectNavPropertyMap;
	//	}

	private StringBuilder expandItemsConstruct(RdfEntityType targetEntityType, String targetKey,
			List<ExpandItem> expandItems, String indent) throws EdmException {
		StringBuilder expandSelectTreeNodeConstruct = new StringBuilder();
		// for (Entry<String, ExpandOption> expandTreeNodeLinksEntry :
		// expandTreeNode
		// .getLinks().entrySet()) {
		for (ExpandItem expandItem : expandItems) {
			List<UriResource> resourceParts = expandItem.getResourcePath().getUriResourceParts();
			UriResourceNavigation resourceNavigation = (UriResourceNavigation) resourceParts.get(0);

			RdfAssociation navProperty = rdfModelToMetadata
					.getMappedNavigationProperty(new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(), //resourceNavigation.getProperty().getType().getNamespace(),
							resourceNavigation.getProperty().getName()));

			String nextTargetKey = targetKey + resourceNavigation.getProperty().getName(); // expandItem.getText();
			RdfEntityType nextTargetEntityType = navProperty.getRangeClass();

			// V4 RdfAssociation navProperty =this.expandSelectNavPropertyMap.get(expandItem.getKey());
			if (navProperty.getRangeClass().isOperation()) {
				expandSelectTreeNodeConstruct.append(indent + "\t")
						.append("?" + targetKey + "_s <" + navProperty.getAssociationIRI() + ">\n");
				expandSelectTreeNodeConstruct.append(indent)
						.append(constructOperation(navProperty.getRangeClass(), indent,true));
			} else if (navProperty.getDomainClass().isOperation()) {

			} else {
				expandSelectTreeNodeConstruct.append(indent + "\t").append(
						"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
			}

			if (navProperty.getRangeClass().isOperation()) {
			} else {
				expandSelectTreeNodeConstruct
						.append(constructType(navProperty.getRangeClass(), nextTargetKey, indent + "\t"));
				expandSelectTreeNodeConstruct.append(indent + "\t")
						.append("?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey + "_o .\n");
			}
			if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
				expandSelectTreeNodeConstruct.append(expandItemsConstruct(nextTargetEntityType, nextTargetKey,
						expandItem.getExpandOption().getExpandItems(), indent + "\t"));//nextTargetKey, expandItem.getExpandOption().getExpandItems(), indent+ "\t"));
			}
		}
		return expandSelectTreeNodeConstruct;
	}

	private StringBuilder expandItemsWhere(RdfEntityType targetEntityType, String targetKey,
			List<ExpandItem> expandItems, String indent) throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
		StringBuilder expandSelectTreeNodeWhere = new StringBuilder();
		// for (Entry<String, ExpandOption> expandSelectTreeNodeLinksEntry :
		// expandSelectTreeNode
		// .getLinks().entrySet()) {
		for (ExpandItem expandItem : expandItems) {
			List<UriResource> resourceParts = expandItem.getResourcePath().getUriResourceParts();
			UriResourceNavigation resourceNavigation = (UriResourceNavigation) resourceParts.get(0);

			RdfAssociation navProperty = rdfModelToMetadata
					.getMappedNavigationProperty(new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(), //resourceNavigation.getProperty().getType().getNamespace(),//resourceNavigation.getProperty().getType().getNamespace(),
							resourceNavigation.getProperty().getName()));

			String nextTargetKey = targetKey + resourceNavigation.getProperty().getName(); // + expandSelectTreeNodeLinksEntry.getKey();
			RdfEntityType nextTargetEntityType = navProperty.getRangeClass();
			//RdfAssociation navProperty = expandSelectNavPropertyMap.get(expandSelectTreeNodeLinksEntry.getKey());
			if (navProperty.getDomainClass().isOperation()) {
				for (RdfProperty property : navProperty.getDomainClass().getProperties()) {
					if (property.getPropertyTypeName().equals(navProperty.getRangeClass().getIRI()))
						expandSelectTreeNodeWhere
								.append(indent).append("BIND(?" + property.getVarName() + " AS ?" + nextTargetKey + "_s)\n");
				}
			}
			expandSelectTreeNodeWhere.append(indent);
			// Not optional if filter imposed on path but should really be equality like filters, not negated filters
			//SparqlExpressionVisitor expandFilterClause;
			if (expandItem.getFilterOption() == null){
				expandSelectTreeNodeWhere.append("OPTIONAL");
			}
			expandSelectTreeNodeWhere.append("{\n");
			if (navProperty.getRangeClass().isOperation()) {
				expandSelectTreeNodeWhere.append(clausesOperationProperties(navProperty.getRangeClass()));
				// BIND(?order as ?Order_s)
				// BIND(?prod as ?Orderorder_orderSummaryorderSummary_product_s
				// )
				for (RdfProperty property : navProperty.getRangeClass().getProperties()) {
					if (property.getPropertyTypeName().equals(navProperty.getDomainClass().getIRI()))
						expandSelectTreeNodeWhere
								.append("BIND(?" + property.getVarName() + " AS ?" + targetKey + "_s)\n");
				}
			} else {
				if (navProperty.getDomainClass().isOperation()) {
					// Nothing to add as BIND assumed to be created
				} else if (navProperty.IsInverse()) {
					expandSelectTreeNodeWhere.append(indent).append("\t").append("{\n");
					expandSelectTreeNodeWhere.append(indent).append("\t").append("\t").append("?" + nextTargetKey + "_s <"
							+ navProperty.getInversePropertyOf().getIRI() + "> ?" + targetKey + "_s .\n");
					expandSelectTreeNodeWhere.append(indent).append("\t").append("}UNION{\n");
					expandSelectTreeNodeWhere.append(indent).append("\t").append("\t").append("?" + targetKey + "_s <"
							+ navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
					expandSelectTreeNodeWhere.append(indent).append("\t").append("}\n");
					
				} else {
					expandSelectTreeNodeWhere.append(indent).append("\t").append("?" + targetKey + "_s <"
							+ navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
				}
				expandSelectTreeNodeWhere.append(
						clausesSelect(createSelectPropertyMap(navProperty.getRangeClass(),expandItem.getSelectOption()), nextTargetKey, nextTargetKey, navProperty.getRangeClass(), indent + "\t"));
			}
			if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
				expandSelectTreeNodeWhere.append(expandItemsWhere(nextTargetEntityType, nextTargetKey,
						expandItem.getExpandOption().getExpandItems(), indent + "\t"));
			}
			expandSelectTreeNodeWhere.append(indent).append("}\n");
		}
		return expandSelectTreeNodeWhere;
	}

	private StringBuilder clausesSelect(HashSet<String> selectPropertyMap, String nextTargetKey, String navPath, RdfEntityType targetEntityType,
			String indent) {
		StringBuilder clausesSelect = new StringBuilder();
		clausesSelect.append(indent);
		// Case URI5 need to fetch only one property as given in resourceParts
		if (navPath.equals(nextTargetKey) || this.filterClause.getNavPropertyPropertyFilters().containsKey(navPath)) {
		} else {
			clausesSelect.append("OPTIONAL");
		}
		clausesSelect.append("{\n");
		clausesSelect.append(indent).append("\t")
				.append("?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey + "_o .\n");

		if (selectPropertyMap != null) {
			clausesSelect.append(indent).append("\t").append("VALUES(?" + nextTargetKey + "_p){");
				for (String selectProperty : selectPropertyMap) {
					clausesSelect.append("(<" + selectProperty + ">)");
				}
			clausesSelect.append("}\n");
		} else if (!this.rdfTargetEntityType.getProperties().isEmpty()) {
			clausesSelect.append(indent).append("\t").append("VALUES(?" + nextTargetKey + "_p){");
			for (RdfModel.RdfProperty selectProperty : targetEntityType.getInheritedProperties()) {
				clausesSelect.append("(<" + selectProperty.getPropertyURI() + ">)");
			}
			clausesSelect.append("}\n");
		} else {
			// Assume select=*, and fetch all non object property values
			clausesSelect.append(indent).append("\t")
					.append("FILTER(!isIRI(?" + nextTargetKey + "_o) && !isBLANK(?" + nextTargetKey + "_o))\n");
		}
		clausesSelect.append(indent).append("}\n");
		return clausesSelect;
	}

	private StringBuilder limitClause() {
		StringBuilder limitClause = new StringBuilder();
		// if
		// (this.uriType.equals(UriType.URI1)||this.uriType.equals(UriType.URI6B))
		// {
		int top;
		int skip;
		int defaultLimit = rdfModel.getRdfRepository().getModelRepository().getDefaultQueryLimit();
		if (uriInfo.getTopOption() == null) {
			top = defaultLimit;
			skip = 0;
		} else {
			top = uriInfo.getTopOption().getValue();

			if ((top > defaultLimit) && (defaultLimit != 0))
				top = defaultLimit;
			if (uriInfo.getSkipOption() == null) {
				skip = 0;
			} else {
				skip = uriInfo.getSkipOption().getValue();
			}
		}
		if (top > 0 || skip > 0) {

			if (top > 0) {
				limitClause.append(" LIMIT ").append(top);
			}
			if (skip > 0) {
				limitClause.append(" OFFSET ").append(skip);
			}
		}
		// }
		return limitClause;
	}

	private StringBuilder defaultLimitClause() {
		StringBuilder defaultLimitClause = new StringBuilder();
		int defaultLimit = rdfModel.getRdfRepository().getModelRepository().getDefaultQueryLimit();
		defaultLimitClause.append(" LIMIT ").append(defaultLimit);
		return defaultLimitClause;
	}

	private  HashSet<String> createSelectPropertyMap(RdfEntityType entityType, SelectOption selectOption) throws EdmException {
		// Align variables
		//RdfEntityType entityType = rdfTargetEntityType;
		//String key = entityType.entityTypeName;
		HashSet<String> valueProperties = new  HashSet<String>();
		if (selectOption != null) {
			for (SelectItem property : selectOption.getSelectItems()) {
				RdfEntityType segmentEntityType = entityType;

				if (property.isStar()) {
					// TODO Does/should segmentEntityType.getProperties get inherited properties as well?
					// TODO Why not get all
					for (RdfProperty rdfProperty : segmentEntityType.getProperties()) {
						if (rdfProperty.propertyNode != null) {
							valueProperties.add(rdfProperty.propertyNode.getIRI().toString());
							// emptyClause = false;
						}
					}

				} else if (property.getResourcePath() != null) {
					if (!property.getResourcePath().equals(RdfConstants.SUBJECT)) {
						RdfProperty rdfProperty = null;
						try {
							rdfProperty = segmentEntityType.findProperty(
									property.getResourcePath().getUriResourceParts().get(0).getSegmentValue());
							if (rdfProperty == null)
								throw new EdmException("Failed to locate property:"
										+ property.getResourcePath().getUriResourceParts().get(0).getSegmentValue());
						} catch (EdmException e) {
							log.error("Failed to locate property:"
									+ property.getResourcePath().getUriResourceParts().get(0).getSegmentValue());
							throw new EdmException("Failed to locate property:"
									+ property.getResourcePath().getUriResourceParts().get(0).getSegmentValue());
						}

						if (rdfProperty.getIsKey()) {
							// TODO specifically asked for key so should be added to VALUES
							valueProperties.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
						} else {
							valueProperties.add(rdfProperty.propertyNode.getIRI().toString());
						}
					}
				} 
			}
			return valueProperties;
		}
		return null;
	}

	public Boolean isPrimitiveValue() {
		return isPrimitiveValue;
	}

	public void setIsPrimitiveValue(Boolean isPrimitiveValue) {
		this.isPrimitiveValue = isPrimitiveValue;
	}

	public SparqlStatement prepareEntityLinksSparql(	) throws EdmException, ODataApplicationException, OData2SparqlException {

		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResource lastResourcePart = resourceParts.get(resourceParts.size() - 1);
		int minSize = 1;
		if (lastResourcePart.getSegmentValue().equals("$ref")) { // which it should do
			minSize++;
		}
		UriResourceNavigation uriNavigation = (UriResourceNavigation) resourceParts.get(resourceParts.size() - minSize);
		EdmNavigationProperty edmNavigationProperty = uriNavigation.getProperty();

		UrlValidator urlValidator = new UrlValidator();
		String expandedKey =  rdfModel.getRdfPrefixes().expandPredicateKey( ((UriResourceEntitySet) resourceParts.get(0)).getKeyPredicates().get(0).getText());
		
		String key = rdfEntityType.entityTypeName;

		if (urlValidator.isValid(expandedKey)) {
		} else {
			throw new EdmException("Invalid key: " + ((UriResourceEntitySet) resourceParts.get(0)).getKeyPredicates().get(0).getText(), null);
		}
		RdfAssociation rdfProperty = rdfEntityType.findNavigationProperty(edmNavigationProperty.getName());
		String expandedProperty = rdfProperty.getAssociationIRI();
		StringBuilder sparql = new StringBuilder(
				//			"CONSTRUCT {?" + key + "_s <" + expandedProperty + "> ?" + key + "_o . ?"+ key +"_o <http://targetEntity> true . }\n");
				"CONSTRUCT { ?" + key + "_o <http://targetEntity> true . }\n");
		if (rdfProperty.IsInverse()) {
			String expandedInverseProperty = rdfProperty.getInversePropertyOfURI().toString();
			sparql.append(
					"WHERE {?" + key + "_o ?" + key + "_p ?" + key + "_s . \nVALUES(?" + key + "_s ?" + key + "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedInverseProperty + ">)}\n}");
		} else {
			sparql.append(
					"WHERE {?" + key + "_s ?" + key + "_p ?" + key + "_o .\n VALUES(?" + key + "_s ?" + key + "_p){(");
			sparql.append("<" + expandedKey + "> ");
			sparql.append("<" + expandedProperty + ">)}\n}");
		}

		return new SparqlStatement(sparql.toString());
	}
}