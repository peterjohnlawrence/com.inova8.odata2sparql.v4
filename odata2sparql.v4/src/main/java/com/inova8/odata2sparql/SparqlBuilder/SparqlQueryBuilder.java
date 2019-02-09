package com.inova8.odata2sparql.SparqlBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
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
import com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.SparqlExpressionVisitor.SparqlExpressionVisitor;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;
import com.inova8.odata2sparql.uri.RdfResourceParts;
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
	private UriInfo uriInfo;
	private RdfResourceParts rdfResourceParts;

	private RdfEntityType rdfEntityType = null;
	private RdfEntityType rdfTargetEntityType = null;
	private RdfProperty rdfComplexProperty = null;
	private EdmEntitySet edmEntitySet = null;
	private EdmEntitySet edmTargetEntitySet = null;
	private EdmComplexType edmPathComplexType = null;
	private ExpandOption expandOption;
	//private SelectOption selectOption;

	private Boolean isPrimitiveValue = false;
	private SparqlExpressionVisitor filterClause;
	private SparqlFilterClausesBuilder filterClauses;
	private HashSet<String> selectPropertyMap;

	private static final boolean DEBUG = true;

	public SparqlQueryBuilder(RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, UriInfo uriInfo,
			UriType uriType, RdfResourceParts rdfResourceParts)
			throws EdmException, ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		super();
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.uriInfo = uriInfo;
		this.uriType = uriType;
		this.rdfResourceParts = rdfResourceParts;
		// Prepare what is required to create the SPARQL
		prepareBuilder();
		log.info("Builder for URIType: " + uriType.toString());
	}

	private void prepareBuilder()
			throws EdmException, ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		// Prepare what is required to create the SPARQL
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourceParts.get(0);
		UriResourceComplexProperty complexProperty;
		UriResource lastResourcePart;
		int minSize;
		this.edmEntitySet = uriResourceEntitySet.getEntitySet();
		this.rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		// By default
		this.edmTargetEntitySet = edmEntitySet;
		this.rdfTargetEntityType = rdfEntityType;
		UriResource lastSegment;
		this.expandOption = uriInfo.getExpandOption();
		filterClauses = new SparqlFilterClausesBuilder(rdfModel, rdfModelToMetadata, uriInfo, uriType);
		switch (this.uriType) {
		case URI1: {
			edmTargetEntitySet = edmEntitySet;
			rdfTargetEntityType = rdfEntityType;
			filterClause = filterClauses.getFilterClause();
		}
			break;
		case URI2:
			edmTargetEntitySet = rdfResourceParts.getResponseEntitySet();//edmEntitySet;
			rdfTargetEntityType = rdfResourceParts.getResponseRdfEntityType();//rdfEntityType;
			break;
		case URI3:
			edmTargetEntitySet = edmEntitySet;
			rdfTargetEntityType = rdfEntityType;
			//complexProperty =((UriResourceComplexProperty) resourceParts.get(1));
			//edmPathComplexType = complexProperty.getComplexType();
			//rdfComplexProperty = rdfTargetEntityType.findProperty(edmPathComplexType.getName());			
			rdfComplexProperty = this.rdfResourceParts.getLastComplexProperty();
			break;
		case URI4:
			lastResourcePart = resourceParts.get(resourceParts.size() - 1);
			minSize = 2;
			if (lastResourcePart.getSegmentValue().equals("$value")) {
				minSize++;
				this.setIsPrimitiveValue(true);
			}
			lastSegment = resourceParts.get(resourceParts.size() - minSize);
			if (lastSegment.getKind().equals(UriResourceKind.complexProperty)) {
				edmTargetEntitySet = edmEntitySet;
				rdfTargetEntityType = rdfEntityType;
				complexProperty = ((UriResourceComplexProperty) lastSegment);
				edmPathComplexType = complexProperty.getComplexType();
				rdfComplexProperty = rdfTargetEntityType.findProperty(edmPathComplexType.getName());
			}
			break;
		case URI5:
			lastResourcePart = resourceParts.get(resourceParts.size() - 1);
			minSize = 2;
			if (lastResourcePart.getSegmentValue().equals("$value")) {
				minSize++;
				this.setIsPrimitiveValue(true);
			}
			lastSegment = resourceParts.get(resourceParts.size() - minSize);
			edmTargetEntitySet = rdfResourceParts.getResponseEntitySet();
			rdfTargetEntityType = rdfResourceParts.getResponseRdfEntityType();
			//			if (lastSegment instanceof UriResourceNavigation) {
			//				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
			//				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
			//				edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
			//				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			//			}
			break;
		case URI6A: {
			lastSegment = resourceParts.get(resourceParts.size() - 1);
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				if (resourceParts.size() > 2) {
					UriResourceComplexProperty penultimateSegment = (UriResourceComplexProperty) resourceParts
							.get(resourceParts.size() - 2);
					EdmNavigationProperty navigationProperty = penultimateSegment.getComplexType()
							.getNavigationProperty(edmNavigationProperty.getName());
					edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet,
							penultimateSegment.getComplexType(), navigationProperty);
					rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
				} else {

					edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
					rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
				}
			}
		}
			break;
		case URI6B:
			lastSegment = resourceParts.get(resourceParts.size() - 1);
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				if (resourceParts.size() > 2) {
					//could be a complexType
					UriResource penultimateSegment = resourceParts.get(resourceParts.size() - 2);
					if (penultimateSegment.getKind().equals(UriResourceKind.complexProperty)) {
						//Complextype with navigation property
						complexProperty = ((UriResourceComplexProperty) penultimateSegment);
						edmPathComplexType = complexProperty.getComplexType();
						edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmPathComplexType,
								edmNavigationProperty);
					} else {
						edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
					}
				} else {
					edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
				}
				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
				filterClause = filterClauses.getFilterClause();//filterClause(uriInfo.getFilterOption(), rdfTargetEntityType);
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
		//TODO testing only
		//only for expanditems where first part is a complexType
		//However olingo has blocked changing the selectItems collection:-(
		/*		UriInfoResource resourcePath = this.uriInfo.getExpandOption().getExpandItems().get(0).getResourcePath(); 
				SelectItemImpl selectItem = new SelectItemImpl();
				selectItem.setResourcePath(resourcePath);	
				List<SelectItem> selectItems = this.uriInfo.getSelectOption().getSelectItems();
				selectItems.add(selectItem);*/
		//TODO Workaround to select everything  when complexType in expand
		//Fixes #97
		//		if (this.uriInfo.getExpandOption() != null) {
		//			for (ExpandItem expandItem : this.uriInfo.getExpandOption().getExpandItems()) {
		//				if (!expandItem.isStar()) {
		//					UriInfoResource resourcePath = expandItem.getResourcePath();
		//					if (resourcePath.getUriResourceParts().get(0).getKind().equals(UriResourceKind.complexProperty)
		//							&& this.uriInfo.getSelectOption() != null) {
		//						((SelectItemImpl) (this.uriInfo.getSelectOption().getSelectItems().get(0))).setStar(true);
		//						break;
		//					}
		//				} else {
		//					break;
		//				}
		//			}
		//		}
		selectPropertyMap = createSelectPropertyMap(rdfTargetEntityType, this.uriInfo.getSelectOption());

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

	private StringBuilder construct() throws EdmException {
		StringBuilder construct = new StringBuilder("CONSTRUCT {\n");
		String key = edmTargetEntitySet.getEntityType().getName();
		if (this.rdfTargetEntityType.isOperation()) {
			construct.append(constructOperation(key, rdfTargetEntityType, "", false));
		} else {
			construct.append(targetEntityIdentifier(key, "\t"));
			construct.append(constructType(rdfTargetEntityType, key, "\t"));
			if (this.rdfModel.getRdfRepository().isWithMatching())
				construct.append(matching(key, "\t"));
			if ((this.uriInfo.getCountOption() != null) && (this.uriInfo.getCountOption().getValue())) {
				construct.append("\t").append("#entitySetCount\n");
				construct.append("\t").append(
						"<" + rdfTargetEntityType.getURL() + "> <" + RdfConstants.COUNT + "> ?" + key + "_count.\n");
			}
			construct.append(constructPath());
		}
		construct.append(constructComplex());
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
		String type = rdfEntityType.getURL();
		constructType.append(indent).append("?" + key + "_s <" + RdfConstants.ASSERTEDTYPE + "> <" + type + "> .\n");

		return constructType;
	}

	private StringBuilder matching(String key, String indent) throws EdmException {
		StringBuilder matching = new StringBuilder();
		if (DEBUG)
			matching.append(indent).append("#matching\n");
		matching.append(indent).append("?" + key + "_s <" + RdfConstants.MATCHING + "> ?" + key + "_sm .\n");
		return matching;
	}

	private StringBuilder constructOperation(String nextTargetKey, RdfEntityType rdfOperationType, String indent,
			Boolean isExpand) throws EdmException {
		StringBuilder constructOperation = new StringBuilder();
		if (DEBUG)
			constructOperation.append(indent).append("#constructOperation\n");
		String type = rdfOperationType.getURL();
		constructOperation.append(indent + "\t");
		constructOperation.append("?" + nextTargetKey + "_s ");
		if (isExpand)
			constructOperation.append("<" + RdfConstants.ASSERTEDTYPE + "> <" + type + "> ;\n");
		else
			constructOperation
					.append("<http://targetEntity> true ; <" + RdfConstants.ASSERTEDTYPE + "> <" + type + "> ;\n");
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

	private StringBuilder constructComplex() throws EdmException {
		StringBuilder constructComplex = new StringBuilder();
		if (DEBUG)
			constructComplex.append("\t#constructComplex\n");
		if (this.edmPathComplexType != null)
			constructComplex.append(complexConstruct(rdfTargetEntityType, rdfTargetEntityType.entityTypeName, "\t"));
		return constructComplex;
	}

	private StringBuilder complexConstruct(RdfEntityType targetEntityType, String targetKey, String indent)
			throws EdmException {
		StringBuilder complexConstruct = new StringBuilder();
		if (this.edmPathComplexType != null)
			complexConstruct.append(
					expandComplexConstruct(rdfTargetEntityType, rdfTargetEntityType.entityTypeName, "\t") + "\t");
		return complexConstruct;
	}

	private StringBuilder constructExpandSelect() throws EdmException {
		StringBuilder constructExpandSelect = new StringBuilder();
		if (DEBUG)
			constructExpandSelect.append("\t#constructExpandSelect\n");
		if (this.expandOption != null)
			constructExpandSelect.append(expandItemsConstruct(rdfTargetEntityType, rdfTargetEntityType.entityTypeName,
					this.expandOption.getExpandItems(), "\t"));
		return constructExpandSelect;
	}

	private StringBuilder where()
			throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
		StringBuilder where = new StringBuilder();
		if (this.rdfEntityType.isOperation()) {
			where.append(selectOperation());
		} else if (this.rdfTargetEntityType.isOperation()) {
			where.append(selectOperation());
		} else {
			where.append(selectExpand());
		}
		if (this.rdfTargetEntityType.isOperation()) {
			where.append(
					clausesOperationProperties(this.rdfTargetEntityType.getEntityTypeName(), this.rdfTargetEntityType));
		} else {
			where.append(clausesPathProperties());
		}
		where.append(clausesComplex());
		where.append(clausesExpandSelect());
		if ((this.uriInfo.getCountOption() != null) && (this.uriInfo.getCountOption().getValue())) {
			where.append(selectPathCount());
		}
		return where;
	}

	private StringBuilder clausesPathProperties() throws EdmException {
		StringBuilder clausesPathProperties = new StringBuilder();
		if (DEBUG)
			clausesPathProperties.append("\t#clausesPathProperties\n");
		StringBuilder clausesSelect = clausesSelect(this.selectPropertyMap,
				edmTargetEntitySet.getEntityType().getName(), edmTargetEntitySet.getEntityType().getName(),
				rdfTargetEntityType, "\t");
		if (clausesSelect.length() > 0) {
			clausesPathProperties.append("\t{\n").append(clausesSelect).append("\t}\n");
		}
		return clausesPathProperties;
	}

	private StringBuilder clausesOperationProperties(String nextTargetKey, RdfEntityType rdfOperationType)
			throws EdmException, OData2SparqlException {
		StringBuilder clausesOperationProperties = new StringBuilder();
		if (DEBUG)
			clausesOperationProperties.append("\t#clausesOperationProperties\n");
		clausesOperationProperties.append("\t{");
		clausesOperationProperties.append("\n\t").append(filterOperationQuery(rdfOperationType));
		clausesOperationProperties.append("\t{\n").append(preprocessOperationQuery(rdfOperationType)).append("\t}\n");
		//Ensures that URN of deduced key is repeatable so all results always in same order
		//clausesOperationProperties.append("BIND(UUID()  AS ?" + nextTargetKey + "_s)\n");
		clausesOperationProperties.append("BIND(").append(operationUUID(rdfOperationType))
				.append(" AS ?" + nextTargetKey + "_s)\n");
		clausesOperationProperties.append("\t}");
		return clausesOperationProperties;
	}

	private StringBuilder operationUUID(RdfEntityType rdfOperationType) {
		StringBuilder operationUUID = new StringBuilder();
		// IRI(CONCAT("urn:",MD5(CONCAT(STR(?entity),STR( ?property)))))
		operationUUID.append("IRI(CONCAT(\"urn:\",MD5(CONCAT(");
		for (RdfPrimaryKey key : rdfOperationType.getPrimaryKeys()) {
			operationUUID.append("COALESCE(STR(?").append(key.getPrimaryKeyName()).append("),\"\"),");
		}
		operationUUID.deleteCharAt(operationUUID.length() - 1);
		return operationUUID.append("))))");
	}

	private StringBuilder filterOperationQuery(RdfEntityType rdfOperationType)
			throws EdmException, OData2SparqlException {
		StringBuilder filter = new StringBuilder();
		if (DEBUG)
			filter.append("#operationFilter\n");
		if (filterClause != null) {
			if (!filterClause.getFilterClause().isEmpty())
				filter.append(filterClauses.getFilter()).append("\n");
		}
		return filter;
	}

	private String getQueryOptionText(List<CustomQueryOption> queryOptions,
			FunctionImportParameter functionImportParameter) {

		for (CustomQueryOption queryOption : queryOptions) {
			if (queryOption.getName().equals(functionImportParameter.getName()))
				switch (functionImportParameter.getType()) {
				//Fixes #86
				case "http://www.w3.org/2000/01/rdf-schema#Resource":
					String resource = queryOption.getText().replace("'", "")
							.replaceAll(RdfConstants.QNAME_SEPARATOR_ENCODED, RdfConstants.QNAME_SEPARATOR_RDF);
					return resource;
				default:
					return queryOption.getText();
				}
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
			String parameterValue = getQueryOptionText(queryOptions, functionImportParameter);

			if (parameterValue != null) {
				queryText = queryText.replaceAll("\\?" + functionImportParameter.getName(), parameterValue);
			} else {
				if (!functionImportParameter.isNullable())
					throw new OData2SparqlException("FunctionImport (" + rdfOperationType.getEntityTypeName()
							+ ") cannot be called without values for non-nullable parameters");
			}
		}
		if ((uriType != UriType.URI15) && (uriType != UriType.URI6B)) {
			queryText += limitClause();
		}
		return queryText;
	}

	private StringBuilder clausesComplex()
			throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
		StringBuilder clausesComplex = new StringBuilder();
		if (DEBUG)
			clausesComplex.append("\t#clausesComplex\n");
		if (this.edmPathComplexType != null) {
			clausesComplex.append(expandComplex(rdfTargetEntityType, rdfTargetEntityType.entityTypeName, "\t"));
		}
		return clausesComplex;
	}

	private StringBuilder clausesExpandSelect()
			throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
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
		if ((uriType.equals(UriType.URI6B)) && limitClause().length() > 0) {
			selectExpand.append("\t").append("{\tSELECT ");
			selectExpand.append("?" + edmTargetEntitySet.getEntityType().getName() + "_s\n");
		} else {
			//selectExpand.append("\t").append("{\n");
			//Fixes #89 ... by forcing a projection, and thus execution first
			selectExpand.append("\t").append("{\tSELECT *\n");
		}
		//Fixes #79
		//		if (this.expandOption != null)
		//			selectExpand.append(filterClauses.getExpandItemVariables());
		selectExpand.append(selectExpandWhere("\t\t"));
		selectExpand.append("\t").append("}\n");
		return selectExpand;
	}

	private StringBuilder selectExpandWhere(String indent) throws EdmException, OData2SparqlException {
		StringBuilder selectExpandWhere = new StringBuilder();
		if (DEBUG)
			selectExpandWhere.append(indent).append("#selectExpandWhere\n");
		selectExpandWhere.append(indent).append("{\n");
		selectExpandWhere.append(filter(indent + "\t"));
		switch (uriType) {
		case URI1:
			// nothing required for any entitySet query
			break;
		case URI2:
		case URI3:
		case URI4:
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
		selectExpandWhere.append(search(indent + "\t"));
		selectExpandWhere.append(clausesFilter(indent + "\t"));
		selectExpandWhere.append(clausesExpandFilter(indent + "\t"));
		switch (uriType) {
		case URI1:
			selectExpandWhere.append(selectPath());
			selectExpandWhere.append(indent).append("}\n");
			break;
		case URI2:
		case URI3:
		case URI4:
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
		StringBuilder filterClause = filterClauses.getFilter();
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
		switch (this.uriType) {
		case URI1:
			clausesPath.append(clausesPath_URI1(indent));
			break;
		case URI2:
			clausesPath.append(clausesPath_URI2(indent));
			break;
		case URI3:
			clausesPath.append(clausesPath_URI3(indent));
			break;
		case URI4:
			clausesPath.append(clausesPath_URI4(indent));
			break;
		case URI5:
			clausesPath.append(clausesPath_URI5(indent));
			break;
		case URI6A:
			clausesPath.append(clausesPath_URI6A(indent));
			break;
		case URI6B:
			clausesPath.append(clausesPath_URI6B(indent));
			break;
		case URI15:
			clausesPath.append(clausesPath_URI15(indent));
			break;
		case URI16:
			clausesPath.append(clausesPath_URI16(indent));
			clausesPath.append(exists(indent));
			break;
		default:
			clausesPath.append("#clausesPath\n");
			clausesPath.append("#Unhandled URIType: " + this.uriType + "\n");
			break;
		}
		return clausesPath;
	}

	private StringBuilder clausesMatch(String key1, String key2, String indent) {
		StringBuilder clausesMatch = new StringBuilder();
		if (DEBUG)
			clausesMatch.append(indent).append("#clausesMatch\n");
		clausesMatch.append(indent).append(createMatchFromTemplate(key1, key2) + "\n");
		return clausesMatch;
	}

	private StringBuilder clausesMatch(String key, String indent) {
		StringBuilder clausesMatch = new StringBuilder();
		if (DEBUG)
			clausesMatch.append(indent).append("#clausesMatch\n");
		clausesMatch.append(indent).append(createMatchFromTemplate(key, key + "m") + "\n");
		return clausesMatch;
	}

	private StringBuilder clausesMatchNavigationKey(String key1, String key2, String indent) {
		StringBuilder clausesMatch = new StringBuilder();
		if (DEBUG)
			clausesMatch.append(indent).append("#clausesMatchNavigationKey\n");
		clausesMatch.append(indent).append("FILTER EXISTS{" + createMatchFromTemplate(key1, key2) + "}\n");
		return clausesMatch;
	}

	private String createMatchFromTemplate(String key1, String key2) {
		String template = this.rdfModel.getRdfRepository().getMatch();
		return template.replace("key1", key1).replaceAll("key2", key2);
	}

	private StringBuilder valuesSubClassOf(RdfEntityType rdfEntityType) {
		StringBuilder valuesSubClassOf = new StringBuilder();
		valuesSubClassOf.append("VALUES(?class){").append("(<" + rdfEntityType.getURL() + ">)");
		for (RdfEntityType subType : rdfEntityType.getAllSubTypes()) {
			valuesSubClassOf.append("(<" + subType.getURL() + ">)");
		}
		return valuesSubClassOf;
	}

	private StringBuilder clausesPath_URI1(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI1\n");
		if (uriInfo.getUriResourceParts().size() > 1) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(indent).append(valuesSubClassOf(rdfEntityType)).append("}\n");
			if (this.rdfModel.getRdfRepository().isWithMatching()) {
				clausesPath.append(indent).append("?" + rdfEntityType.entityTypeName
						+ "_sm <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n");
				clausesPath.append(clausesMatch("?" + rdfEntityType.entityTypeName + "_sm",
						"?" + rdfEntityType.entityTypeName + "_s", indent));
			} else {
				clausesPath.append(indent).append("?" + rdfEntityType.entityTypeName
						+ "_s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n");
			}
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI2(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI2\n");
		if (uriInfo.getUriResourceParts().size() > 1) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI3(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI3\n");
		if (uriInfo.getUriResourceParts().size() > 2) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI4(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI4\n");

		if (uriInfo.getUriResourceParts().size() > 2) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI5(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI5\n");
		if (uriInfo.getUriResourceParts().size() > (this.isPrimitiveValue() ? 3 : 2)) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI6A(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI6A\n");
		if (uriInfo.getUriResourceParts().size() > 1) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI6B(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI6B\n");
		if (uriInfo.getUriResourceParts().size() > 1) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			clausesPath.append(indent).append(valuesSubClassOf(rdfEntityType)).append("}\n");
			if (this.rdfModel.getRdfRepository().isWithMatching()) {
				clausesPath.append(indent).append("?" + rdfEntityType.entityTypeName
						+ "_sm <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n");
				clausesPath.append(clausesMatch("?" + rdfEntityType.entityTypeName + "_sm",
						"?" + rdfEntityType.entityTypeName + "_s", indent));
			} else {
				clausesPath.append(indent).append("?" + rdfEntityType.entityTypeName
						+ "_s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n");
			}
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI15(String indent) throws EdmException, OData2SparqlException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI15\n");
		if (uriInfo.getUriResourceParts().size() > 2) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
		} else {
			if (rdfTargetEntityType.isOperation()) {
				clausesPath.append(indent).append(preprocessOperationQuery(rdfTargetEntityType));
			} else {
				clausesPath.append(indent).append(valuesSubClassOf(rdfEntityType)).append("}\n");
				clausesPath.append(indent).append("?" + rdfEntityType.entityTypeName
						+ "_s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n");
			}
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI16(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI16\n");
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
			} else {
				if (segmentSize > 2) {
					log.error(
							"Too many navigation properties for operation:" + uriInfo.getUriResourceParts().toString());
				} else {
					RdfAssociation navProperty = rdfEntityType.findNavigationPropertyByEDMAssociationName(
							uriInfo.getUriResourceParts().get(1).getSegmentValue());
					key = "?" + rdfTargetEntityType.entityTypeName;
					clausesPath_KeyPredicateValues.append(indent).append("VALUES(" + key + "_s");// #116
					if (this.rdfModel.getRdfRepository().isWithMatching()) {
						clausesPath_KeyPredicateValues.append("m)");
					}
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
					if (this.rdfModel.getRdfRepository().isWithMatching()) {
						clausesPath_KeyPredicateValues.append(clausesMatch(key + "_s", indent));// #116
					}
					return clausesPath_KeyPredicateValues;
				}
			}
		} else if (rdfTargetEntityType.isOperation()) {
			// TODO make sure not a complex or value resourceParts
			if (segmentSize > 2) {
				log.error("Too many navigation properties for operation:" + uriInfo.getUriResourceParts().toString());
			} else {
				RdfAssociation navProperty = rdfEntityType.findNavigationPropertyByEDMAssociationName(
						uriInfo.getUriResourceParts().get(1).getSegmentValue());
				if (navProperty != null) {
					//key = "?" + rdfTargetEntityType
					//		.findNavigationPropertyByEDMAssociationName(navProperty.getInversePropertyOf().getLocalName()).getVarName();
					//Fixes #85
					key = "?" + rdfTargetEntityType.findNavigationPropertyByEDMAssociationName(
							navProperty.getInverseAssociation().getAssociationName()).getVarName();
				} else {
					log.error("Failed to locate operation navigation property:"
							+ uriInfo.getUriResourceParts().get(1).getSegmentValue());
				}
			}
		} else {
			key = primaryKey_Variables(rdfEntityType).toString();
		}

		if (((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates().size() != 0) {
			//need to sort the values into same order as keys (treemap order)
			if (this.rdfModel.getRdfRepository().isWithMatching() && !rdfEntityType.isOperation()) {
				clausesPath_KeyPredicateValues.append(indent).append("VALUES(" + key + "m)");
			} else {
				clausesPath_KeyPredicateValues.append(indent).append("VALUES(" + key + ")");
			}
			TreeMap<String, String> keyValues = new TreeMap<String, String>();
			for (UriParameter entityKey : ((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0))
					.getKeyPredicates()) {
				String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey.getText());
				// Strip leading and trailing single quote from key
				String expandedKey = rdfModel.getRdfPrefixes()
						.expandPrefix(decodedEntityKey.substring(1, decodedEntityKey.length() - 1));
				if (expandedKey.equals(RdfConstants.SPARQL_UNDEF)) {
					keyValues.put(entityKey.getName(), " " + RdfConstants.SPARQL_UNDEF + " ");
				} else {
					keyValues.put(entityKey.getName(), "<" + expandedKey + ">");
				}
			}
			clausesPath_KeyPredicateValues.append("{(");
			for (String value : keyValues.values()) {
				clausesPath_KeyPredicateValues.append(value);
			}
			clausesPath_KeyPredicateValues.append(")}\n");
		}
		if (this.rdfModel.getRdfRepository().isWithMatching() && !rdfEntityType.isOperation()) {
			clausesPath_KeyPredicateValues.append(clausesMatch(key, indent));
		}
		return clausesPath_KeyPredicateValues;
	}

	private StringBuilder clausesPathNavigation(String indent, List<UriResource> navigationSegments,
			List<UriParameter> entityKeys) throws EdmException {
		StringBuilder clausesPathNavigation = new StringBuilder();

		String path = edmEntitySet.getEntityType().getName();//edmTargetEntitySet.getEntityType().getName();
		boolean isFirstSegment = true;
		Integer index = 0;
		String keyVariable = "";
		String pathVariable = "";
		String targetVariable = "";
		String sourceVariable = "";
		String matchTargetVariable;
		int segmentSize = navigationSegments.size();
		if ((uriType == UriType.URI3) || (uriType == UriType.URI4) || (uriType == UriType.URI5)
				|| (uriType == UriType.URI15))
			segmentSize--;
		if ((uriType == UriType.URI4))
			segmentSize--;
		if (this.isPrimitiveValue())
			segmentSize--;
		Integer lastIndex = segmentSize;
		for (index = 1; index < segmentSize; index++) {
			UriResource navigationSegment = navigationSegments.get(index);
			if (!navigationSegment.getKind().equals(UriResourceKind.complexProperty)) {
				UriResourceNavigation uriResourceNavigation = ((UriResourceNavigation) navigationSegment);
				EdmNavigationProperty predicate = uriResourceNavigation.getProperty();
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
					if (this.rdfModel.getRdfRepository().isWithMatching()) {
						keyVariable = "?key_s";
						clausesPathNavigation.append(clausesMatch(keyVariable, pathVariable, indent));
						pathVariable = keyVariable;
					}
				} else {
					pathVariable = "?" + path + "_s";
				}
				if (index.equals(lastIndex - 1)) {
					targetVariable = "?" + edmTargetEntitySet.getEntityType().getName() + "_s";
					if (isFirstSegment) {
						sourceVariable = pathVariable;
					} else {
						sourceVariable = pathVariable;
					}
				} else {
					targetVariable = "?" + path + navProperty.getAssociationName() + "_s";
					sourceVariable = pathVariable;
				}
				if (this.rdfModel.getRdfRepository().isWithMatching()) {
					matchTargetVariable = "?" + path + navProperty.getAssociationName() + "_sm";
				} else {
					matchTargetVariable = targetVariable;
				}
				if (navProperty.IsInverse()) {
					clausesPathNavigation.append(indent).append("{\n");
					clausesPathNavigation.append(indent).append("\t" + matchTargetVariable + " <"
							+ navProperty.getInversePropertyOf().getIRI() + "> " + sourceVariable + " .\n");
					clausesPathNavigation.append(indent).append("} UNION {\n");
					clausesPathNavigation.append(indent).append("\t" + sourceVariable + " <"
							+ navProperty.getAssociationIRI() + "> " + matchTargetVariable + " .\n");
					clausesPathNavigation.append(indent).append("}\n");
				} else {
					clausesPathNavigation.append(indent).append(sourceVariable + " <" + navProperty.getAssociationIRI()
							+ "> " + matchTargetVariable + " .\n");
				}
				if (this.rdfModel.getRdfRepository().isWithMatching()) {
					clausesPathNavigation.append(clausesMatch(matchTargetVariable, targetVariable, indent));
					if (uriResourceNavigation.getKeyPredicates().size() > 0) {
						String navigationKey = "";
						for (UriParameter entityKey : uriResourceNavigation.getKeyPredicates()) {
							String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey.getText());
							String expandedKey = rdfModel.getRdfPrefixes()
									.expandPrefix(decodedEntityKey.substring(1, decodedEntityKey.length() - 1));
							navigationKey = "<" + expandedKey + ">";
						}
						clausesPathNavigation.append(clausesMatchNavigationKey(targetVariable, navigationKey, indent));
					}
				}
				path += navProperty.getAssociationName();
				isFirstSegment = false;
			}
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

	private StringBuilder search(String indent) {
		StringBuilder search = new StringBuilder().append(indent);
		if (DEBUG)
			search.append("#search\n");
		if (this.uriInfo.getSearchOption() != null) {
			String searchText = this.uriInfo.getSearchOption().getText();
			if (searchText.startsWith("\"")) {
				searchText = searchText.substring(1, searchText.length() - 1);
			}
			if (this.uriInfo.getSearchOption() != null)
				switch (uriType) {
				case URI1:
				case URI15:
					switch (this.rdfModel.getRdfRepository().getTextSearchType()) {
					case RDF4J_LUCENE:
						search.append(indent)
								.append("?" + rdfEntityType.entityTypeName + "_s <" + RdfConstants.URI_LUCENE_MATCHES
										+ "> [ <" + RdfConstants.URI_LUCENE_QUERY + "> '" + searchText + "' ] .\n");
						break;
					case HALYARD_ES:
						search.append(indent).append("?" + rdfEntityType.entityTypeName + "_s ?p '" + searchText
								+ "'^^<" + RdfConstants.URI_HALYARD_SEARCH + "> .\n");
						break;
					case DEFAULT:
					default:
						search.append(indent).append("?" + rdfEntityType.entityTypeName
								+ "_s ?p ?searchvalue . FILTER( REGEX(?searchvalue ,'" + searchText + "', \"i\")) .\n");

					}
					break;
				case URI6B:
					switch (this.rdfModel.getRdfRepository().getTextSearchType()) {
					case RDF4J_LUCENE:
						search.append(indent)
								.append("?" + rdfTargetEntityType.entityTypeName + "_s <"
										+ RdfConstants.URI_LUCENE_MATCHES + "> [ <" + RdfConstants.URI_LUCENE_QUERY
										+ "> '" + searchText + "' ] .\n");
						break;
					case HALYARD_ES:
						search.append(indent).append("?" + rdfTargetEntityType.entityTypeName + "_s ?p '" + searchText
								+ "'^^<" + RdfConstants.URI_HALYARD_SEARCH + "> .\n");
						break;
					case DEFAULT:
					default:
						search.append(indent).append("?" + rdfTargetEntityType.entityTypeName
								+ "_s ?p ?searchvalue . FILTER( REGEX(?searchvalue ,'" + searchText + "', \"i\")) .\n");

					}
					break;
				default:
					break;
				}
		}
		return search;
	}

	private StringBuilder selectPath() throws EdmException, OData2SparqlException {
		StringBuilder selectPath = new StringBuilder();
		String indent;
		if (DEBUG)
			selectPath.append("\t\t\t#selectPath\n");

		if (limitSet()) {
			selectPath.append("\t\t\t").append("{\tSELECT DISTINCT\n");
			selectPath.append("\t\t\t\t\t").append("?" + edmTargetEntitySet.getEntityType().getName() + "_s");
			if (this.rdfModel.getRdfRepository().isWithMatching())
				selectPath.append(" ?" + edmTargetEntitySet.getEntityType().getName() + "_sm");
			selectPath.append("\n\t\t\t\t").append("WHERE {\n");
			indent = "\t\t\t\t";
		} else {
			selectPath.append("\t\t\t").append("{\n");
			indent = "\t\t\t";
		}
		selectPath.append(filter(indent + "\t"));
		selectPath.append(search(indent + "\t"));
		selectPath.append(clausesPath(indent + "\t"));
		selectPath.append(clausesFilter(indent + "\t"));
		selectPath.append(clausesExpandFilter(indent + "\t"));
		if (limitSet()) {
			selectPath.append(indent).append("}") // GROUP BY   ?" + edmTargetEntitySet.getEntityType().getName() + "_s")
					.append(limitClause()).append("\n");
		}
		selectPath.append("\t\t\t").append("}\n");
		return selectPath;
	}

	private StringBuilder selectPathCount() throws EdmException, OData2SparqlException {
		StringBuilder selectPath = new StringBuilder();

		if (DEBUG)
			selectPath.append("\t#selectPathCount\n");
		selectPath.append("\t").append("{\tSELECT \n");
		selectPath.append("\t\t\t").append("(COUNT(?" + edmTargetEntitySet.getEntityType().getName() + "_s) as ?"
				+ edmTargetEntitySet.getEntityType().getName() + "_count)\n");
		selectPath.append("\t\t").append("WHERE {\n");
		selectPath.append(filter("\t\t\t"));
		selectPath.append(search("\t\t\t"));
		selectPath.append(clausesPath("\t\t\t"));
		selectPath.append(clausesFilter("\t\t\t"));
		selectPath.append(clausesExpandFilter("\t\t\t"));
		selectPath.append("\t\t").append("}").append("\n");
		selectPath.append("\t").append("}\n");
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

	private StringBuilder expandComplexConstruct(RdfEntityType targetEntityType, String targetKey, String indent)
			throws EdmException {
		StringBuilder expandItemsConstruct = new StringBuilder();

		for (String navigationPropertyName : this.edmPathComplexType.getNavigationPropertyNames()) {
			RdfAssociation navigationProperty = rdfModelToMetadata.getMappedNavigationProperty(
					new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(), navigationPropertyName));
			expandItemsConstruct
					.append(expandComplexPropertyConstruct(targetEntityType, targetKey, indent, navigationProperty,
							targetKey + navigationProperty.getAssociationName(), navigationProperty.getRangeClass()));

		}
		return expandItemsConstruct;
	}

	private StringBuilder expandItemsConstruct(RdfEntityType targetEntityType, String targetKey,
			List<ExpandItem> expandItems, String indent) throws EdmException {
		StringBuilder expandItemsConstruct = new StringBuilder();
		for (ExpandItem expandItem : expandItems) {
			if (expandItem.isStar()) {
				if (expandItem.getResourcePath() != null) {
					List<UriResource> expandResourcePathParts = expandItem.getResourcePath().getUriResourceParts();
					UriResource lastExpandResourcePath = expandResourcePathParts
							.get(expandResourcePathParts.size() - 1);
					if (lastExpandResourcePath.getKind().equals(UriResourceKind.complexProperty)) {
						UriResourceComplexProperty complexProperty = ((UriResourceComplexProperty) lastExpandResourcePath);
						EdmComplexType edmComplexPropertyType = complexProperty.getComplexType();
						rdfComplexProperty = rdfTargetEntityType.findProperty(edmComplexPropertyType.getName());
						for (RdfAssociation navigationProperty : rdfComplexProperty.getComplexType()
								.getNavigationProperties().values()) {
							expandItemsConstruct.append(expandItemConstruct(targetEntityType, targetKey, indent,
									expandItem, navigationProperty, targetKey + navigationProperty.getAssociationName(),
									navigationProperty.getRangeClass()));
						}
					}
				} else {
					for (RdfAssociation navigationProperty : targetEntityType.getNavigationProperties()) {
						if (validateOperationCallable(navigationProperty.getRangeClass())) {
							expandItemsConstruct.append(expandItemConstruct(targetEntityType, targetKey, indent,
									expandItem, navigationProperty, targetKey + navigationProperty.getAssociationName(),
									navigationProperty.getRangeClass()));
						}
					}
				}
			} else {
				List<UriResource> resourceParts = expandItem.getResourcePath().getUriResourceParts();
				//Only navigation supported in RDFS+ is one level of complexProperty, hence code is not generic
				UriResource firstResourcePart = resourceParts.get(0);
				UriResourceNavigation resourceNavigation = null;
				if (firstResourcePart instanceof UriResourceNavigation) {
					resourceNavigation = (UriResourceNavigation) resourceParts.get(0);
				} else if (firstResourcePart instanceof UriResourceComplexProperty) {
					resourceNavigation = (UriResourceNavigation) resourceParts.get(1);
				}

				RdfAssociation navProperty = rdfModelToMetadata.getMappedNavigationProperty(
						new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(), //resourceNavigation.getProperty().getType().getNamespace(),
								resourceNavigation.getProperty().getName()));

				String nextTargetKey = targetKey + resourceNavigation.getProperty().getName();
				RdfEntityType nextTargetEntityType = navProperty.getRangeClass();

				expandItemsConstruct.append(expandItemConstruct(targetEntityType, targetKey, indent, expandItem,
						navProperty, nextTargetKey, nextTargetEntityType));
			}
		}
		return expandItemsConstruct;
	}

	private Boolean validateOperationCallable(RdfEntityType rdfEntityType) {
		if (rdfEntityType.isOperation()) {
			if (!this.rdfModel.getRdfRepository().getExpandOperations()) {
				return false;
			}
			if (rdfEntityType.isFunctionImport()) {
				if (!rdfEntityType.getFunctionImportParameters().isEmpty()) {
					//Expecting parameters so should be in uri
					for (Entry<String, com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter> functionImportParameterEntry : rdfEntityType
							.getFunctionImportParameters().entrySet()) {
						com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter functionImportParameter = functionImportParameterEntry
								.getValue();
						String parameterValue = getQueryOptionText(uriInfo.getCustomQueryOptions(),
								functionImportParameter);
						// null value so not set
						if (parameterValue == null)
							return false;
					}
					return true;
				} else {
					//Not expecting paramaters so no problems
					return false;
				}
			} else {
				//Not a function import so no problems
				return true;
			}
		} else {
			return true;
		}
	}

	private StringBuilder expandComplexPropertyConstruct(RdfEntityType targetEntityType, String targetKey,
			String indent, RdfAssociation navProperty, String nextTargetKey, RdfEntityType nextTargetEntityType) {
		StringBuilder expandComplexPropertyConstruct = new StringBuilder();

		if (navProperty.getRangeClass().isOperation()) {
			expandComplexPropertyConstruct.append(indent + "\t").append(
					"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
			expandComplexPropertyConstruct.append(indent)
					.append(constructOperation(nextTargetKey, navProperty.getRangeClass(), indent, true));
		} else if (navProperty.getDomainClass().isOperation()) {

		} else {
			expandComplexPropertyConstruct.append(indent + "\t").append(
					"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
		}
		if (navProperty.getRangeClass().isOperation()) {
			//			expandItemConstruct.append("?" + rdfOperationType.getEntityTypeName() + "_key .\n");
			//			expandItemConstruct.append("?" + nextTargetKey + "_key .\n");
		} else {

			expandComplexPropertyConstruct
					.append(constructType(navProperty.getRangeClass(), nextTargetKey, indent + "\t"));
			if (this.rdfModel.getRdfRepository().isWithMatching())
				expandComplexPropertyConstruct.append(matching(nextTargetKey, indent + "\t"));
			expandComplexPropertyConstruct.append(indent + "\t")
					.append("?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey + "_o .\n");
		}

		return expandComplexPropertyConstruct;
	}

	private StringBuilder expandItemConstruct(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfAssociation navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType) {
		StringBuilder expandItemConstruct = new StringBuilder();

		if (navProperty.getRangeClass().isOperation()) {
			expandItemConstruct.append(indent + "\t").append(
					"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
			expandItemConstruct.append(indent)
					.append(constructOperation(nextTargetKey, navProperty.getRangeClass(), indent, true));
		} else if (navProperty.getDomainClass().isOperation()) {

		} else {
			expandItemConstruct.append(indent + "\t").append(
					"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
		}
		if (navProperty.getRangeClass().isOperation()) {
			//			expandItemConstruct.append("?" + rdfOperationType.getEntityTypeName() + "_key .\n");
			//			expandItemConstruct.append("?" + nextTargetKey + "_key .\n");
		} else {

			expandItemConstruct.append(constructType(navProperty.getRangeClass(), nextTargetKey, indent + "\t"));
			if (this.rdfModel.getRdfRepository().isWithMatching())
				expandItemConstruct.append(matching(nextTargetKey, indent + "\t"));
			if ((expandItem.getCountOption() != null) && (expandItem.getCountOption().getValue())) {
				expandItemConstruct.append(indent).append("\t#entityNavigationSetCount\n");
				expandItemConstruct.append(indent).append("\t?" + targetKey + "_s <" + RdfConstants.COUNT + "/"
						+ navProperty.getAssociationName() + "> ?" + nextTargetKey + "_count.\n");
			}
			expandItemConstruct.append(indent + "\t")
					.append("?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey + "_o .\n");
		}
		if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
			expandItemConstruct.append(expandItemsConstruct(nextTargetEntityType, nextTargetKey,
					expandItem.getExpandOption().getExpandItems(), indent + "\t"));
		}
		return expandItemConstruct;
	}

	private StringBuilder expandComplex(RdfEntityType targetEntityType, String targetKey, String indent)
			throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
		StringBuilder expandComplex = new StringBuilder();
		if (this.edmPathComplexType != null) {
			for (String navigationPropertyName : this.edmPathComplexType.getNavigationPropertyNames()) {

				RdfAssociation navigationProperty = rdfModelToMetadata.getMappedNavigationProperty(
						new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(), navigationPropertyName));

				String nextTargetKey = targetKey + navigationPropertyName;
				RdfEntityType nextTargetEntityType = navigationProperty.getRangeClass();

				expandComplex.append(expandComplexNavigationProperty(targetEntityType, targetKey, indent,
						navigationProperty, nextTargetKey, nextTargetEntityType));
			}
		}
		return expandComplex;
	}

	private StringBuilder expandComplexNavigationProperty(RdfEntityType targetEntityType, String targetKey,
			String indent, RdfAssociation navProperty, String nextTargetKey, RdfEntityType nextTargetEntityType)
			throws OData2SparqlException, ODataApplicationException, ExpressionVisitException {

		StringBuilder expandComplexNavigationProperty = new StringBuilder();

		// Not optional if filter imposed on path but should really be equality like filters, not negated filters
		//SparqlExpressionVisitor expandFilterClause;

		//TODO performance fix and to avoid OPTIONAL when no subselect but use otherwise
		expandComplexNavigationProperty.append(indent).append("#expandComplexNavigationProperty\n").append(indent);
		if (navProperty.getDomainClass().isOperation()) {//Fixes #103 || limitSet()) {
			expandComplexNavigationProperty.append("OPTIONAL");
		} else {
			expandComplexNavigationProperty.append("UNION");
		}
		expandComplexNavigationProperty.append("{\n");
		expandComplexNavigationProperty.append(indent);
		if (navProperty.getDomainClass().isOperation()) {
			expandComplexNavigationProperty.append(indent)
					.append("BIND(?" + navProperty.getRelatedKey() + " AS ?" + nextTargetKey + "_s)\n");
		}
		expandComplexNavigationProperty.append(indent).append("\t{\n");
		if (navProperty.getRangeClass().isOperation()) {
			expandComplexNavigationProperty
					.append(clausesOperationProperties(nextTargetKey, navProperty.getRangeClass()));
		} else {
			expandComplexNavigationProperty.append(indent).append("\t\t\t{")
					.append("SELECT ?" + targetKey + "_s ?" + nextTargetKey + "_s {\n");

			String matchingTargetKey = "?" + nextTargetKey + "_s";
			if (this.rdfModel.getRdfRepository().isWithMatching()) {
				matchingTargetKey = matchingTargetKey + "m";
			}
			if (navProperty.getDomainClass().isOperation()) {
				// Nothing to add as BIND assumed to be created
			} else if (navProperty.IsInverse()) {

				expandComplexNavigationProperty.append(indent).append("\t\t\t\t{\n");
				expandComplexNavigationProperty.append(indent).append("\t\t\t\t\t").append(matchingTargetKey + " <"
						+ navProperty.getInversePropertyOf().getIRI() + "> ?" + targetKey + "_s .\n");
				expandComplexNavigationProperty.append(indent).append("\t\t\t\t").append("}UNION{\n");
				expandComplexNavigationProperty.append(indent).append("\t\t\t\t\t").append(
						"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> " + matchingTargetKey + " .\n");
				expandComplexNavigationProperty.append(indent).append("\t\t\t\t").append("}\n");

			} else {
				expandComplexNavigationProperty.append(indent).append("\t\t\t\t").append(
						"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> " + matchingTargetKey + " .\n");
			}
			if (this.rdfModel.getRdfRepository().isWithMatching())
				expandComplexNavigationProperty.append(clausesMatch("?" + nextTargetKey + "_s", indent + "\t\t\t\t"));

			expandComplexNavigationProperty.append(indent).append("\t\t\t}\n");
			expandComplexNavigationProperty.append(indent).append("\t\t}\n");
			expandComplexNavigationProperty.append(indent).append("\t\tVALUES(?" + nextTargetKey + "_p){");

			for (RdfProperty complexProperty : nextTargetEntityType.getInheritedProperties()) {
				expandComplexNavigationProperty.append("(<" + complexProperty.getPropertyURI() + ">)");
			}
			expandComplexNavigationProperty.append("}\n");

			expandComplexNavigationProperty.append(indent)
					.append("\t\t?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey + "_o .\n");
		}
		expandComplexNavigationProperty.append(indent).append("\t}\n");
		expandComplexNavigationProperty.append(indent).append("}\n");
		return expandComplexNavigationProperty;
	}

	private StringBuilder expandItemsWhere(RdfEntityType targetEntityType, String targetKey,
			List<ExpandItem> expandItems, String indent)
			throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
		StringBuilder expandItemsWhere = new StringBuilder();

		for (ExpandItem expandItem : expandItems) {
			if (expandItem.isStar()) {
				if (expandItem.getResourcePath() != null) {
					List<UriResource> expandResourcePathParts = expandItem.getResourcePath().getUriResourceParts();
					UriResource lastExpandResourcePath = expandResourcePathParts
							.get(expandResourcePathParts.size() - 1);
					if (lastExpandResourcePath.getKind().equals(UriResourceKind.complexProperty)) {
						UriResourceComplexProperty complexProperty = ((UriResourceComplexProperty) lastExpandResourcePath);
						EdmComplexType edmComplexPropertyType = complexProperty.getComplexType();
						rdfComplexProperty = rdfTargetEntityType.findProperty(edmComplexPropertyType.getName());
						for (RdfAssociation navigationProperty : rdfComplexProperty.getComplexType()
								.getNavigationProperties().values()) {
							if (validateOperationCallable(navigationProperty.getRangeClass())) {
								expandItemsWhere.append(expandItemWhere(targetEntityType, targetKey, indent, expandItem,
										navigationProperty, targetKey + navigationProperty.getAssociationName(),
										navigationProperty.getRangeClass()));
							}
						}
					}
				} else {
					for (RdfAssociation navigationProperty : targetEntityType.getNavigationProperties()) {
						if (validateOperationCallable(navigationProperty.getRangeClass())) {
							expandItemsWhere.append(expandItemWhere(targetEntityType, targetKey, indent, expandItem,
									navigationProperty, targetKey + navigationProperty.getAssociationName(),
									navigationProperty.getRangeClass()));
						}
					}
				}
			} else {
				List<UriResource> resourceParts = expandItem.getResourcePath().getUriResourceParts();
				//Only navigation supported in RDFS+ is one level of complexProperty, hence code is not generic
				UriResource firstResourcePart = resourceParts.get(0);
				UriResourceNavigation resourceNavigation = null;
				if (firstResourcePart instanceof UriResourceNavigation) {
					resourceNavigation = (UriResourceNavigation) resourceParts.get(0);
				} else if (firstResourcePart instanceof UriResourceComplexProperty) {
					resourceNavigation = (UriResourceNavigation) resourceParts.get(1);
				}

				RdfAssociation navProperty = rdfModelToMetadata.getMappedNavigationProperty(new FullQualifiedName(
						targetEntityType.getSchema().getSchemaPrefix(), resourceNavigation.getProperty().getName()));

				String nextTargetKey = targetKey + resourceNavigation.getProperty().getName();
				RdfEntityType nextTargetEntityType = navProperty.getRangeClass();

				expandItemsWhere.append(expandItemWhere(targetEntityType, targetKey, indent, expandItem, navProperty,
						nextTargetKey, nextTargetEntityType));
			}
		}
		return expandItemsWhere;
	}

	private StringBuilder expandItemWhere(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfAssociation navProperty, String nextTargetKey, RdfEntityType nextTargetEntityType)
			throws OData2SparqlException, ODataApplicationException, ExpressionVisitException {

		StringBuilder expandItemWhere = new StringBuilder();

		// Not optional if filter imposed on path but should really be equality like filters, not negated filters
		//SparqlExpressionVisitor expandFilterClause;

		//TODO performance fix and to avoid OPTIONAL when no subselect but use otherwise
		expandItemWhere.append(indent).append("#expandItemWhere\n").append(indent);
		if (navProperty.getDomainClass().isOperation()) {//Fixes #103 || limitSet()) {
			expandItemWhere.append("OPTIONAL");
		} else {
			expandItemWhere.append("UNION");//.append("UNION");
		}
		expandItemWhere.append("{\n");
		expandItemWhere.append(indent);
		if (navProperty.getDomainClass().isOperation()) {
			expandItemWhere.append(indent)
					.append("BIND(?" + navProperty.getRelatedKey() + " AS ?" + nextTargetKey + "_s");
			if (this.rdfModel.getRdfRepository().isWithMatching()) { //Fix #117
				expandItemWhere.append("m");
			}
			expandItemWhere.append(")\n");
		}
		expandItemWhere.append(indent).append("{\n");
		if (navProperty.getRangeClass().isOperation()) {
			expandItemWhere.append(clausesOperationProperties(nextTargetKey, navProperty.getRangeClass()));
		} else {
			HashSet<String> selectedProperties = createSelectPropertyMap(navProperty.getRangeClass(),
					expandItem.getSelectOption());
			if (selectedProperties != null && selectedProperties.isEmpty())
				return new StringBuilder();
			if (navProperty.getDomainClass().isOperation()) {
				expandItemWhere.append(indent).append("\t\t{").append(" {\n");
			} else {
				expandItemWhere.append(indent).append("\t\t{")
						.append("SELECT ?" + targetKey + "_s ?" + nextTargetKey + "_s {\n");
			}
			String matchingTargetKey = "?" + nextTargetKey + "_s";
			if (this.rdfModel.getRdfRepository().isWithMatching()) {
				matchingTargetKey = matchingTargetKey + "m";
			}
			if (navProperty.getDomainClass().isOperation()) {
				// Nothing to add as BIND assumed to be created
			} else if (navProperty.IsInverse()) {

				expandItemWhere.append(indent).append("\t\t\t\t{\n");
				expandItemWhere.append(indent).append("\t\t\t\t\t").append(matchingTargetKey + " <"
						+ navProperty.getInversePropertyOf().getIRI() + "> ?" + targetKey + "_s .\n");
				expandItemWhere.append(indent).append("\t\t\t\t").append("}UNION{\n");
				expandItemWhere.append(indent).append("\t\t\t\t\t").append(
						"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> " + matchingTargetKey + " .\n");
				expandItemWhere.append(indent).append("\t\t\t\t").append("}\n");

			} else {
				expandItemWhere.append(indent).append("\t\t\t\t").append(
						"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> " + matchingTargetKey + " .\n");
			}
			if (this.rdfModel.getRdfRepository().isWithMatching())
				expandItemWhere.append(clausesMatch("?" + nextTargetKey + "_s", indent + "\t\t\t\t"));
			expandItemWhere.append(indent).append("\t\t\t}");

			if ((expandItem.getTopOption() != null)) {
				expandItemWhere.append(" LIMIT " + expandItem.getTopOption().getValue());
			} else if ((expandItem.getSelectOption() == null) && (expandItem.getCountOption() != null)
					&& (expandItem.getCountOption().getValue())) {
				// Fixes #78 by setting limit even if $top not specified, as it cannot be in OpenUI5.
				expandItemWhere.append(" LIMIT 0");
			}
			if ((expandItem.getSkipOption() != null)) {
				expandItemWhere.append(" OFFSET " + expandItem.getSkipOption().getValue());
			}
			expandItemWhere.append("\n" + indent).append("\t\t}\n");
			expandItemWhere.append(clausesSelect(selectedProperties, nextTargetKey, nextTargetKey,
					navProperty.getRangeClass(), indent + "\t"));
		}
		expandItemWhere.append(indent).append("\t}\n");
		if ((expandItem.getCountOption() != null) && expandItem.getCountOption().getValue()) {
			expandItemWhere.append(expandItemWhereCount(targetEntityType, targetKey, indent, expandItem, navProperty,
					nextTargetKey, nextTargetEntityType));
		}
		if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
			expandItemWhere.append(expandItemsWhere(nextTargetEntityType, nextTargetKey,
					expandItem.getExpandOption().getExpandItems(), indent + "\t"));
		}
		expandItemWhere.append(indent).append("}\n");
		return expandItemWhere;
	}

	private StringBuilder expandItemWhereCount(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfAssociation navProperty, String nextTargetKey, RdfEntityType nextTargetEntityType)
			throws OData2SparqlException, ODataApplicationException, ExpressionVisitException {

		StringBuilder expandItemWhereCount = new StringBuilder();
		expandItemWhereCount.append(indent).append("\t#expandItemWhereCount\n");
		//TODO UNION or OPTIONAL, currently OPTIONAL improves performance
		expandItemWhereCount.append(indent).append("\tUNION{ SELECT ?").append(targetKey)
				.append("_s (COUNT(?" + nextTargetKey + "_s) as ?" + nextTargetKey + "_count)\n").append(indent)
				.append("\tWHERE {\n");
		// Not optional if filter imposed on path but should really be equality like filters, not negated filters
		//SparqlExpressionVisitor expandFilterClause;

		if (navProperty.getDomainClass().isOperation()) {
			for (RdfProperty property : navProperty.getDomainClass().getProperties()) {
				if (property.getPropertyTypeName().equals(navProperty.getRangeClass().getURL()))
					expandItemWhereCount.append(indent)
							.append("BIND(?" + property.getVarName() + " AS ?" + nextTargetKey + "_s)\n");
			}
		}

		if (navProperty.getRangeClass().isOperation()) {
			expandItemWhereCount.append(clausesOperationProperties(nextTargetKey, navProperty.getRangeClass()));
			// BIND(?order as ?Order_s)
			// BIND(?prod as ?Orderorder_orderSummaryorderSummary_product_s
			// )
			for (RdfProperty property : navProperty.getRangeClass().getProperties()) {
				if (property.getPropertyTypeName().equals(navProperty.getDomainClass().getURL()))
					expandItemWhereCount.append("BIND(?" + property.getVarName() + " AS ?" + targetKey + "_s)\n");
			}
		} else {
			String nextKeyVar = "?" + nextTargetKey + "_s";
			String keyvar = "?" + targetKey + "_s";
			if (this.rdfModel.getRdfRepository().isWithMatching()) {
				nextKeyVar = nextKeyVar + "m";
				keyvar = keyvar + "m1";
				expandItemWhereCount.append(clausesMatch("?" + targetKey + "_s", keyvar, indent + "\t\t"));
			}
			//Fixes Issue #93
			if (navProperty.getDomainClass().isOperation()) {
				// Nothing to add as BIND assumed to be created
			} else if (navProperty.IsInverse()) {
				expandItemWhereCount.append(indent).append("\t\t{\n");
				expandItemWhereCount.append(indent).append("\t\t\t").append(
						nextKeyVar + " <" + navProperty.getInversePropertyOf().getIRI() + "> " + keyvar + " .\n");
				expandItemWhereCount.append(indent).append("\t\t").append("}UNION{\n");
				expandItemWhereCount.append(indent).append("\t\t\t")
						.append(keyvar + " <" + navProperty.getAssociationIRI() + "> " + nextKeyVar + " .\n");
				expandItemWhereCount.append(indent).append("\t\t").append("}\n");

			} else {
				expandItemWhereCount.append(indent).append("\t")
						.append(keyvar + " <" + navProperty.getAssociationIRI() + "> " + nextKeyVar + " .\n");
			}
			if (this.rdfModel.getRdfRepository().isWithMatching())
				expandItemWhereCount.append(clausesMatch("?" + nextTargetKey + "_s", nextKeyVar, indent + "\t\t"));
		}
		expandItemWhereCount.append(indent).append("\t} GROUP BY ?").append(targetKey).append("_s\n").append(indent)
				.append("}\n");
		return expandItemWhereCount;
	}

	private StringBuilder clausesSelect(HashSet<String> selectPropertyMap, String nextTargetKey, String navPath,
			RdfEntityType targetEntityType, String indent) {
		StringBuilder clausesSelect = new StringBuilder();
		Boolean hasProperties = false;
		// Case URI5 need to fetch only one property as given in resourceParts
		if (navPath.equals(nextTargetKey) || this.filterClause.getNavPropertyPropertyFilters().containsKey(navPath)) {
		} else {
			clausesSelect.append("OPTIONAL");
		}
		if (selectPropertyMap != null && !selectPropertyMap.isEmpty()) {
			clausesSelect.append(indent).append("\tVALUES(?" + nextTargetKey + "_p){");
			for (String selectProperty : selectPropertyMap) {
				hasProperties = true;
				clausesSelect.append("(<" + selectProperty + ">)");
			}
			clausesSelect.append("}\n");
		} else if (this.uriType.equals(UriType.URI3) || this.uriType.equals(UriType.URI4)) {
			clausesSelect.append(indent).append("\tVALUES(?" + nextTargetKey + "_p){");
			StringBuilder complexPropertyString = complexProperties(this.rdfComplexProperty);
			if (complexPropertyString.length() > 0)
				hasProperties = true;
			clausesSelect.append(complexPropertyString);
			clausesSelect.append("}\n");
		} else if (!this.rdfTargetEntityType.getProperties().isEmpty()) {
			clausesSelect.append(indent).append("\tVALUES(?" + nextTargetKey + "_p){");
			for (RdfModel.RdfProperty selectProperty : targetEntityType.getInheritedProperties()) {
				hasProperties = true;
				if (selectProperty.getIsComplex()) {
					clausesSelect.append(complexProperties(selectProperty));
				} else {
					clausesSelect.append("(<" + selectProperty.getPropertyURI() + ">)");
				}
			}
			clausesSelect.append("}\n");
		} else {
			// Assume select=*, and fetch all non object property values
			hasProperties = true;
			clausesSelect.append(indent)
					.append("FILTER(!isIRI(?" + nextTargetKey + "_o) && !isBLANK(?" + nextTargetKey + "_o))\n");
		}
		clausesSelect.append(indent)
				.append("\t?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey + "_o .\n");
		if (hasProperties)
			return clausesSelect;
		else
			return new StringBuilder();
	}

	private StringBuilder complexProperties(RdfModel.RdfProperty selectProperty) {
		StringBuilder complexProperties = new StringBuilder();
		for (RdfProperty complexProperty : selectProperty.getComplexType().getProperties().values()) {
			if (complexProperty.getIsComplex()) {
				complexProperties.append(complexProperties(complexProperty));
			} else {
				complexProperties.append("(<" + complexProperty.getPropertyURI() + ">)");
			}
		}
		for (RdfAssociation complexNavigationProperty : selectProperty.getComplexType().getNavigationProperties()
				.values()) {
			complexProperties.append("(<" + complexNavigationProperty.getAssociationIRI() + ">)");
		}
		return complexProperties;
	}

	private HashSet<String> complexPropertiesSet(RdfModel.RdfProperty selectProperty) {
		HashSet<String> complexProperties = new HashSet<String>();
		for (RdfProperty complexProperty : selectProperty.getComplexType().getProperties().values()) {
			if (complexProperty.getIsComplex()) {
				complexProperties.addAll(complexPropertiesSet(complexProperty));
			} else {
				complexProperties.add(complexProperty.getPropertyURI());
			}
		}
		return complexProperties;
	}

	//	private HashSet<String> complexNavigationPropertiesSet(RdfModel.RdfProperty selectProperty) {
	//		HashSet<String> complexProperties = new HashSet<String>();
	//		for (RdfProperty complexProperty : selectProperty.getComplexType().getProperties().values()) {
	//			if (complexProperty.getIsComplex()) {
	//				complexProperties.addAll(complexNavigationPropertiesSet(complexProperty));
	//			}
	//		}
	//		for (RdfAssociation complexNavigationProperty : selectProperty.getComplexType().getNavigationProperties()
	//				.values()) {
	//			complexProperties.add(complexNavigationProperty.getAssociationIRI());
	//		}
	//		return complexProperties;
	//	}

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

	private boolean limitSet() {
		return ((uriInfo.getTopOption() != null) || (uriInfo.getSkipOption() != null));
	}

	private StringBuilder defaultLimitClause() {
		StringBuilder defaultLimitClause = new StringBuilder();
		int defaultLimit = rdfModel.getRdfRepository().getModelRepository().getDefaultQueryLimit();
		defaultLimitClause.append(" LIMIT ").append(defaultLimit);
		return defaultLimitClause;
	}

	private HashSet<String> createSelectPropertyMap(RdfEntityType entityType, SelectOption selectOption)
			throws EdmException {
		// Align variables
		//RdfEntityType entityType = rdfTargetEntityType;
		//String key = entityType.entityTypeName;
		HashSet<String> valueProperties = new HashSet<String>();
		List<UriResource> resourceParts = this.uriInfo.getUriResourceParts();
		switch (this.uriType) {
		case URI3:
			UriResource complexProperty3 = resourceParts.get(resourceParts.size() - 1);
			RdfProperty rdfSegmentComplexProperty3 = entityType.findProperty(complexProperty3.getSegmentValue());
			if (rdfSegmentComplexProperty3 != null)
				valueProperties.addAll(complexPropertiesSet(rdfSegmentComplexProperty3));
			return valueProperties;
		case URI4:
			UriResource complexProperty4 = resourceParts.get(resourceParts.size() - 2);
			@SuppressWarnings("unused")
			RdfProperty rdfSegmentComplexProperty4 = entityType.findProperty(complexProperty4.getSegmentValue());
			@SuppressWarnings("unused")
			UriResource complexPropertyProperty4 = resourceParts.get(resourceParts.size() - 1);
			return valueProperties;
		case URI5:
			UriResource primitiveProperty = resourceParts.get(resourceParts.size() - 1);
			RdfProperty rdfSegmentProperty = entityType.findProperty(primitiveProperty.getSegmentValue());
			if (rdfSegmentProperty != null && !rdfSegmentProperty.propertyName.equals(RdfConstants.SUBJECT))
				valueProperties.add(rdfSegmentProperty.getPropertyURI());
			return valueProperties;
		default:
			if (selectOption != null) {
				for (SelectItem property : selectOption.getSelectItems()) {
					RdfEntityType segmentEntityType = entityType;

					if (property.isStar()) {
						// TODO Does/should segmentEntityType.getProperties get inherited properties as well?
						// TODO Why not get all
						for (RdfProperty rdfProperty : segmentEntityType.getProperties()) {
							if (rdfProperty.getPropertyURI() != null) {
								valueProperties.add(rdfProperty.getPropertyURI());
								// emptyClause = false;
							}
						}

					} else if (property.getResourcePath() != null) {
						if (!property.getResourcePath().toString().equals(RdfConstants.SUBJECT)) {
							RdfProperty rdfProperty = null;
							try {
								String segmentName = property.getResourcePath().getUriResourceParts().get(0)
										.getSegmentValue();
								rdfProperty = segmentEntityType.findProperty(segmentName);
								if (rdfProperty == null)
									if (segmentEntityType
											.findNavigationPropertyByEDMAssociationName(segmentName) == null) {
										throw new EdmException("Failed to locate property:" + property.getResourcePath()
												.getUriResourceParts().get(0).getSegmentValue());
									} else {
										// TODO specifically asked for key so should be added to VALUES even though no details of a selected navigationproperty need be included other than link, unless included in subsequent $expand
										// See http://docs.oasis-open.org/odata/odata/v4.0/os/part2-url-conventions/odata-v4.0-os-part2-url-conventions.html#_Toc372793861 5.1.3 System Query Option $select
										valueProperties.add(RdfConstants.RDF_TYPE);
									}
								else if (rdfProperty.getIsKey()) {
									valueProperties.add(RdfConstants.RDF_TYPE);
								} else if (rdfProperty.getIsComplex()) {
									// Complex so include all primitive sub-properties
									valueProperties.addAll(complexPropertiesSet(rdfProperty));

								} else {
									valueProperties.add(rdfProperty.getPropertyURI());
								}
							} catch (EdmException e) {
								log.error("Failed to locate property:"
										+ property.getResourcePath().getUriResourceParts().get(0).getSegmentValue());
								throw new EdmException("Failed to locate property:"
										+ property.getResourcePath().getUriResourceParts().get(0).getSegmentValue());
							}

						}
					}
				}
				return valueProperties;
			}
		}
		return null;
	}

	public Boolean isPrimitiveValue() {
		return isPrimitiveValue;
	}

	public void setIsPrimitiveValue(Boolean isPrimitiveValue) {
		this.isPrimitiveValue = isPrimitiveValue;
	}

	public SparqlStatement prepareEntityLinksSparql()
			throws EdmException, ODataApplicationException, OData2SparqlException {
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResource lastResourcePart = resourceParts.get(resourceParts.size() - 1);
		int minSize = 1;
		if (lastResourcePart.getSegmentValue().equals("$ref")) { // which it should do
			minSize++;
		}
		UriResourceNavigation uriNavigation = (UriResourceNavigation) resourceParts.get(resourceParts.size() - minSize);
		EdmNavigationProperty edmNavigationProperty = uriNavigation.getProperty();

		UrlValidator urlValidator = new UrlValidator();
		String expandedKey = rdfModel.getRdfPrefixes()
				.expandPredicateKey(((UriResourceEntitySet) resourceParts.get(0)).getKeyPredicates().get(0).getText());

		String key = rdfEntityType.entityTypeName;

		if (urlValidator.isValid(expandedKey)) {
		} else {
			throw new EdmException(
					"Invalid key: " + ((UriResourceEntitySet) resourceParts.get(0)).getKeyPredicates().get(0).getText(),
					null);
		}
		RdfAssociation rdfProperty = rdfEntityType
				.findNavigationPropertyByEDMAssociationName(edmNavigationProperty.getName());
		String expandedProperty = rdfProperty.getAssociationIRI();
		StringBuilder sparql = new StringBuilder("CONSTRUCT { ?" + key + "_o <http://targetEntity> true .");	
		sparql.append("?" + key + "_o <" + RdfConstants.ASSERTEDTYPE + "> <" + rdfProperty.getRangeClass().getURL() + "> .}\n");
		//		if (rdfProperty.IsInverse()) {
		//			String expandedInverseProperty = rdfProperty.getInversePropertyOfURI().toString();
		//			sparql.append("WHERE {VALUES(?" + key + "_s ?" + key + "_p){(");
		//			sparql.append("<" + expandedKey + "> ");
		//			sparql.append("<" + expandedInverseProperty + ">)}\n?" + key + "_o ?" + key + "_p ?" + key + "_s .}");
		//		} else {
		//			sparql.append("WHERE {VALUES(?" + key + "_s ?" + key + "_p){(");
		//			sparql.append("<" + expandedKey + "> ");
		//			sparql.append("<" + expandedProperty + ">)}\n?" + key + "_s ?" + key + "_p ?" + key + "_o .}");
		//		}

		sparql.append("WHERE { { <" + expandedKey + ">  <" + expandedProperty + "> ?" + key + "_o .}");
		if (rdfProperty.IsInverse()) {
			String expandedInverseProperty = rdfProperty.getInversePropertyOfURI().toString();
			sparql.append(" \nUNION { ?" + key + "_o " + " <" + expandedInverseProperty + "> <" + expandedKey + ">.}");
		}
		sparql.append("}");
		return new SparqlStatement(sparql.toString());
	}
}