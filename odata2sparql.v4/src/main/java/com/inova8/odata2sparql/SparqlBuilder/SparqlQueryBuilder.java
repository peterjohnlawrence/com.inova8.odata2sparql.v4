package com.inova8.odata2sparql.SparqlBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
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
import org.eclipse.rdf4j.model.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfShapedNavigationProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;
import com.inova8.odata2sparql.SparqlExpressionVisitor.SparqlExpressionVisitor;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;
import com.inova8.odata2sparql.uri.UriUtils;
/*
	Query Pseudocode
	================
	
	ResourcePath
	------------
	
	Defined by 
		entitySetUriInfo.getNavigationSegments() and entitySetUriInfo.getTargetEntitySet()
	or
		entitySetUriInfo.getStartEntitySet()
		
		
	/entitySet()
	
		?resource a [rdfs:subClassOf* :entitySet]
	
	/entitySet(:id)
	
		VALUES(?resource){(:id0)}
	
	/entitySet(:id)/navProp1
	
		?id :navProp1  ?resource
		
	/entitySet(:id)/navProp1(:id1)
	
		?id :navProp1  :id1 .
		
	/entitySet(:id){/navPropN(:idN)}*{navProp}?
	
		
		CONSTRUCT{
	      ?resource a ?resourceType
			?resource ?resource_p ?resource_o
			...#select constructs
			...#expand constructs
		} 
		WHERE {
			OPTIONAL{ #select=*
				?resource ?resource_p ?resource_o .
			}
			{
				SELECT #select=*
					?resource
	
				entitySet()
				?resource a [rdfs:subClassOf* :entitySet]
				
				entitySet(:id)/navProp1
				?id :navProp1  ?resource	
				
				entitySet(:id)
				VALUES(?resource){(:id)}
	
				entitySet(:id)/navProp1(:id1)
				?id :navProp1  :id1 . #validate relationship
				VALUES(?resource){(:id1)}
				
				entitySet(:id){navPropN(:idN)}*navProp
				?id :navProp1  :id1 . #validate relationships
				:id1 :navProp2  :id2 .
				:id2 :navProp3  :id3 . 
				...
				:idN :navProp  ?resource
				
			}
		}
		
		
	Expand
	------
	
	$expand=np1/np2/np3/, np4...
		CONSTRUCT{
			...#type construct
			...#path constructs
			...#select constructs
			?resource	:np1	?resource_np1 .
			?resource_np1 :np2 ?resource_np1_np2 .
			?resource_np1_np2 :np3 ?resource_np1_np2_np3 .
			?resource	:np4	?resource_np4 .
			...
		} 
		WHERE {
			...#select clauses
			SELECT ?resource ?resource_np1 ?resource_np1_np2 ?resource_np1_np2_np3 ?resource_np4 
			{
				...
				OPTIONAL{
					?resource	:np1	?resource_np1 .
					OPTIONAL{
						?resource_np1 :np2 ?resource_np1_np2 .
						OPTIONAL{
							?resource_np1_np2 :np3 ?resource_np1_np2_np3 .
							...
						}
					}
				}
				OPTIONAL{
					?resource	:np4	?resource_np4 .
				}	
				SELECT ?resource
				{
				...#path clauses
				}
			}
		}
		
	Note
		If no filter conditions on properties within path then path is optional, otherwise not
		An inverse property swotches subject and object position:
		
		$expand=np1/ip2/np3/...
	
			CONSTRUCT{
				...
				...#path constructs
				...#select constructs
				?resource	:np1	?resource_np1 .
				?resource_np1_ip2 :ip2 ?resource_np1 .
				?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 
				...
			} 
			WHERE {
				...#select clauses
				SELECT ?resource ?resource_np1 ?resource_np1_ip2 ?resource_np1_ip2_np3 
				{
					...
					...#path clauses
					...
					OPTIONAL{	#/np1/
						?resource	:np1	?resource_np1 .
						OPTIONAL{	#/np1/np2/
							?resource_np1_ip2 :ip2 ?resource_np1 .
							OPTIONAL{	#/np1/ip2/np3/
								?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
								...
							}
						}
					}
					SELECT ?resource
					{
					...#path clauses
					}		
				}
			}
		
	Select
	------
	Note
		Selected values must already appear in path
		
	$select=dpa, np1/dpb, np1/np2/dpc, ...
	
		CONSTRUCT{
			...
			...#expand constructs
			?resource	?resource_p   ?resource_o .
			?resource_np1	?resource_np1_p ?resource_np1_o  .
			?resource_np1_np2 ?resource_np1_np2_p ?resource_np1_np2_o .	
			...
		} 
		WHERE {	#/
			OPTIONAL {
				?resource ?resource_p ?resource_o .
				VALUES(?resource_p){(:dpa)}
			}	
			OPTIONAL { ?resource :np1 ?resource_np1 . 
			|| based on if path has filter associated
			{	#/np1/
				OPTIONAL {
					?resource_np1 ?resource_np1_p ?resource_np1_o .
					VALUES(?resource_np1_p){(:dpb)}
				}
				OPTIONAL { ?resource_np1 :np2 ?resource_np1_np2 . 
				|| based on if path has filter associated
				{	#/np1/np2/
					OPTIONAL {
						?resource_np1_np2 ?resource_np1_np2_p ?resource_np1_np2_o .
						VALUES(?resource_np1_np2_p){(:dpc)}
					}
					...
				}
			}
			{
				SELECT ?resource ?resource_np1 ?resource_np1_np2  
				...#path clauses
				...#expand clauses
			}
		}
	
	Filter
	------
	Note
		Filtered values must already appear in path
		
	$filter=condition({npN/}*dpN)
		
		CONSTRUCT{
			...
			...#expand constructs
			...#select constructs
			...
		} WHERE 
		{
			...
			...#select clauses
			...
			{	SELECT ?resource ?resource_np1 ?resource_np1_ip2 ?resource_np1_ip2_np3 
				WHERE {
					...
					...#path clauses
					...
					{	#filter=condition(dp)
						?resource :dp ?resource_dp_o .
						FILTER(condition(?resource_sp_o))			
					}
					{	#/np1/
						?resource	:np1	?resource_np1 .
						{	#filter=condition(np1/dp1)
							?resource_np1 :dp1 ?resource_dp1_o .
							FILTER(condition(?resource_dp1_o))					
						}
						{	#/np1/np2/
							?resource_np1 :np2 ?resource_np1_np2  .
							{	#filter=condition(np1/np2/dp2)
								?resource_np1_np2 :dp2 ?resource_np1_np2_dp2_o.
								FILTER(condition(?resource_np1_np2_dp2_o))					
							}
							{	#/np1/ip2/np3/
								?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
								...
							}
						}
					}
					SELECT DISTINCT
						?resource
					WHERE {
						...#path clauses
						{	#filter=condition(dp)
							?resource :dp ?resource_dp_o .
							FILTER(condition(?resource_sp_o))			
						}
						{	#/np1/
							?resource	:np1	?resource_np1 .
							{	#filter=condition(np1/dp1)
								?resource_np1 :dp1 ?resource_dp1_o .
								FILTER(condition(?resource_dp1_o))					
							}
							{	#/np1/np2/
								?resource_np1 :np2 ?resource_np1_np2  .
								{	#filter=condition(np1/np2/dp2)
									?resource_np1_np2 :dp2 ?resource_np1_np2_dp2_o.
									FILTER(condition(?resource_np1_np2_dp2_o))					
								}
								{	#/np1/ip2/np3/
									?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
									...
								}
							}
						}				
					}	GROUP BY ?resource LIMIT $top		
				}
			}
		}
*/
/*
ExpandItemWhere := 
	OperationExpandItemWhere | 
	implicitExpandItemWhere | 
	standardExpandItemWhere

OperationExpandItemWhere := 
	UNION{	BindVariables
		Service?{
			{
				OperationClause
			}
			[UNION?
			expandItemsWhere]?
		}
	}

BindVariables :=
	BIND(?key AS ?TargetKey)

Service :=
	SERVICE <ServiceURL> 

OperationClause :=
	SelectObjectPropertyValues

implicitExpandItemWhere := 
	OPTIONAL {
		SelectImplicitExpandProperties?
		[UNION?
		ExpandItemWhereCount]?
		[UNION?
		expandItemsWhere]?
	}

standardExpandItemWhere  :=
	UNION {
		SelectExpandProperties?
		[UNION?
		ExpandItemWhereCount]?
		[UNION?
		expandItemsWhere]?
	}
	
SelectImplicitExpandProperties :=
	{
		OPTIONAL?{
			SelectImplicitObjectProperties
		}
		SelectObjectPropertyValues
	}

SelectExpandProperties :=
	{
		OPTIONAL?{
			SelectObjectProperties
		}
		SelectObjectPropertyValues
	}

SelectObjectProperties :=
	SELECT ObjectPropertyVariables {
			ObjectPropertyStatement |
			ObjectPropertyWithInverseStatement
		} LimitClause?
	
ObjectPropertyVariables :=
	?TargetKey	?NextTargetKey 

ObjectPropertyStatement :=
	?TargetKey ObjectProperty ?NextTargetKey 

ObjectPropertyWithInverseStatement :=
	{
		?TargetKey <ObjectProperty> ?NextTargetKey 
	}UNION{
		?NextTargetKey <InverseObjectProperty> ?TargetKey 
	}

LimitClause :=
	[ORDERBY OrderByvariable]? [LIMIT LimitValue [OFFSET OffsetValue]?]?

SelectObjectPropertyValues :=
	VALUES ( ?NextTargetPredicateKey ){( <NextTargetPredicate> )*}
	?NextTargetKey ?NextTargetPredicateKey ?NextTargetObject

ExpandItemWhereCount :=
	{	SELECT ?TargetKey (COUNT(DISTINCT ?NextTargetKey ) AS ?NextTargetKey_count )
		WHERE{
			ObjectPropertyStatement |
			ObjectPropertyWithInverseStatement
		} GROUP BY ?TargetKey
	
	}
*/


public class SparqlQueryBuilder {

	private final Logger log = LoggerFactory.getLogger(SparqlQueryBuilder.class);
	//private final RdfEdmProvider rdfEdmProvider;
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
	private HashSet<SparqlFilterClausesBuilder> lambdaAllfilterClauses = new HashSet<SparqlFilterClausesBuilder>();
	private TreeSet<String> selectPropertyMap;

	private static final boolean DEBUG = true;

	final String propertyPathRegex = "([!^]*)?\\(([^)]*)\\)";
	final Pattern propertyPathPattern = Pattern.compile(propertyPathRegex, Pattern.MULTILINE);

	final String propertyRegex = "([^|~]*)~([^|]*)|(<[^|>]*>)";
	final Pattern propertyPattern = Pattern.compile(propertyRegex, Pattern.MULTILINE);

	RdfRepository proxyDatasetRepository;

	private TreeMap<String, RdfEdmProvider> proxiedRdfEdmProviders = new TreeMap<String, RdfEdmProvider>();

	//	public SparqlQueryBuilder(RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, UriInfo uriInfo,
	//			UriType uriType, RdfResourceParts rdfResourceParts)
	//			throws EdmException, ODataApplicationException, ExpressionVisitException, OData2SparqlException {
	//		super();
	//		this.rdfEdmProvider = null;
	//		this.rdfModel = rdfModel;
	//		this.rdfModelToMetadata = rdfModelToMetadata;
	//		this.uriInfo = uriInfo;
	//		this.uriType = uriType;
	//		this.rdfResourceParts = rdfResourceParts;
	//		// Prepare what is required to create the SPARQL
	//		prepareBuilder();
	//		log.info("Builder for URIType: " + uriType.toString());
	//	}
	public SparqlQueryBuilder(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, UriType uriType,
			RdfResourceParts rdfResourceParts)
			throws EdmException, ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		super();
		//		this.rdfEdmProvider =rdfEdmProvider;
		this.rdfModel = rdfEdmProvider.getRdfModel();
		this.rdfModelToMetadata = rdfEdmProvider.getEdmMetadata();
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
		//UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourceParts.get(0);
		UriResourceComplexProperty complexProperty;
		UriResource lastResourcePart;
		int minSize;
		this.edmEntitySet = this.rdfResourceParts.getEntitySet().getEdmEntitySet();
		this.rdfEntityType = this.rdfResourceParts.getEntitySet().getRdfEntityType();
		// By default
		this.edmTargetEntitySet = edmEntitySet;
		this.rdfTargetEntityType = rdfEntityType;
		UriResource lastSegment;
		this.expandOption = uriInfo.getExpandOption();
		filterClauses = new SparqlFilterClausesBuilder(rdfModel, rdfModelToMetadata, uriInfo, uriType,
				this.rdfResourceParts);
		switch (this.uriType) {
		case URI1: {
			edmTargetEntitySet = edmEntitySet;
			rdfTargetEntityType = rdfEntityType;
			filterClause = filterClauses.getFilterClause();
		}
			break;
		case URI2:
			edmTargetEntitySet = this.rdfResourceParts.getResponseEntitySet();
			rdfTargetEntityType = this.rdfResourceParts.getResponseRdfEntityType();
			break;
		case URI3:
			edmTargetEntitySet = edmEntitySet;
			rdfTargetEntityType = rdfEntityType;
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
					//could be a complexType
					UriResource penultimateSegment = resourceParts.get(resourceParts.size() - 2);
					if (penultimateSegment.getKind().equals(UriResourceKind.complexProperty)) {
						EdmNavigationProperty navigationProperty = ((UriResourceComplexProperty) penultimateSegment)
								.getComplexType().getNavigationProperty(edmNavigationProperty.getName());
						edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet,
								((UriResourceComplexProperty) penultimateSegment).getComplexType(), navigationProperty);
						rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
					} else {
						EdmEntityType edmTargetType = ((UriResourceNavigation) uriResourceNavigation).getProperty()
								.getType();
						edmTargetEntitySet = Util.getNavigationTargetEntitySet(
								edmEntitySet.getEntityContainer().getEntitySets(), edmTargetType,
								edmNavigationProperty);
						rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
					}
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
						EdmEntityType edmTargetType = ((UriResourceNavigation) uriResourceNavigation).getProperty()
								.getType();
						edmTargetEntitySet = Util.getNavigationTargetEntitySet(
								edmEntitySet.getEntityContainer().getEntitySets(), edmTargetType,
								edmNavigationProperty);
					}
				} else {
					edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
				}
				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
				filterClause = filterClauses.getFilterClause();
			}
			break;
		case URI7A: {

			break;
		}
		case URI7B: {

			break;
		}
		case URI11:
			edmTargetEntitySet = this.rdfResourceParts.getResponseEntitySet();
			rdfTargetEntityType = this.rdfResourceParts.getResponseRdfEntityType();
			break;
		case URI15: {
			lastSegment = resourceParts.get(resourceParts.size() - 2);
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
				filterClause = filterClauses.getFilterClause();
			} else {
				filterClause = filterClauses.getFilterClause();
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

		prepareConstruct.append(construct());
		prepareConstruct.append("WHERE {\n");
		prepareConstruct.append(where());
		prepareConstruct.append("}");
		prepareConstruct.append(defaultLimitClause());
		//TODO return new SparqlStatement(this.rdfModel.getRdfPrefixes().sparqlPrefixes().append(prepareConstruct).toString());
		return new SparqlStatement(sparqlPrefixes().append(prepareConstruct).toString());
	}

	public SparqlStatement prepareCountEntitySetSparql()
			throws ODataApplicationException, EdmException, OData2SparqlException {

		StringBuilder prepareCountEntitySet = new StringBuilder("");
		prepareCountEntitySet.append("\t").append("SELECT ");
		prepareCountEntitySet.append("(COUNT(DISTINCT *").append(") AS ?COUNT)").append("\n");
		prepareCountEntitySet.append(selectExpandWhere(""));
		return new SparqlStatement(
				this.rdfModel.getRdfPrefixes().sparqlPrefixes().append(prepareCountEntitySet).toString());
	}

	private StringBuilder construct() throws EdmException, OData2SparqlException {
		StringBuilder construct = new StringBuilder("CONSTRUCT {\n");
		String key = edmTargetEntitySet.getEntityType().getName();
		if (this.rdfTargetEntityType.isOperation()) {
			construct.append(constructOperation(key, rdfTargetEntityType, "", false));
		} else {
			construct.append(targetEntityIdentifier(rdfTargetEntityType, key, "\t"));
			//construct.append(constructType(rdfTargetEntityType, key, "\t"));
			if (this.rdfModel.getRdfRepository().isWithMatching())
				construct.append(matching(key, "\t"));
			if ((this.uriInfo.getCountOption() != null) && (this.uriInfo.getCountOption().getValue())) {
				construct.append("\t").append("#entitySetCount\n");
				construct.append("\t").append(
						"<" + rdfTargetEntityType.getURL() + "> <" + RdfConstants.COUNT + "> ?" + key + "_count.\n");
			}
			if (this.rdfTargetEntityType.isNodeShape()) {
				construct.append(constructNodeShapePath());
			} else {
				construct.append(constructPath());
			}
		}
		construct.append(constructComplex());
		construct.append(constructExpandSelect());
		construct.append("}\n");
		return construct;
	}

	private StringBuilder targetEntityIdentifier(RdfEntityType rdfEntityType, String key, String indent)
			throws EdmException {
		StringBuilder targetEntityIdentifier = new StringBuilder();
		if (DEBUG)
			targetEntityIdentifier.append(indent).append("#targetEntityIdentifier\n");
		String type = rdfEntityType.getURL();
		targetEntityIdentifier.append(indent)
				.append("?" + key + "_s <" + RdfConstants.TARGETENTITY + "> <" + type + "> .\n");
		//Fixes #178
		targetEntityIdentifier.append(indent)
				.append("?" + key + "_s <" + RdfConstants.RDF_SUBJECT + "> ?" + key + "_s .\n");
		return targetEntityIdentifier;
	}

	private StringBuilder constructType(RdfEntityType rdfEntityType, String key, String indent) throws EdmException {
		StringBuilder constructType = new StringBuilder();
		if (DEBUG)
			constructType.append(indent).append("#constructType\n");
		String type = rdfEntityType.getURL();
		constructType.append(indent).append("?" + key + "_s <" + RdfConstants.ASSERTEDTYPE + "> <" + type + "> .\n");
		constructType.append(indent).append("?" + key + "_s <" + RdfConstants.RDF_SUBJECT + "> ?" + key + "_s .\n");
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
			constructOperation.append("<" + RdfConstants.TARGETENTITY + "> <" + type + ">; <"
					+ RdfConstants.ASSERTEDTYPE + "> <" + type + "> ;\n");
		for (RdfProperty property : rdfOperationType.getProperties()) {
			constructOperation.append(indent + "\t\t")
					.append(" <" + property.getPropertyURI() + "> ?" + property.getVarName() + " ;\n");
		}
		constructOperation.replace(constructOperation.length() - 2, constructOperation.length() - 1, ".");
		return constructOperation;
	}

	private StringBuilder constructNodeShapePath() throws EdmException {
		StringBuilder constructNodeShapePath = new StringBuilder();
		if (DEBUG)
			constructNodeShapePath.append("\t#constructNodeShapePath\n");
		String key = edmTargetEntitySet.getEntityType().getName();
		for (RdfProperty property : rdfTargetEntityType.getInheritedProperties()) {
			if (property.getIsComplex()) {
				//Complex properties
				constructNodeShapePath.append(constructComplexType(key, property.getComplexType()));
			} else {
				constructNodeShapePath.append("\t").append("?" + key + "_s <" + property.getPropertyURI() + "> ?" + key
						+ "_" + property.getEDMPropertyName() + ".\n");
			}
		}
		return constructNodeShapePath;
	}

	private StringBuilder constructComplexType(String entityTypeName, RdfComplexType complexType) {
		StringBuilder constructNodeShapePath = new StringBuilder();
		for (RdfProperty complexProperty : complexType.getProperties().values()) {
			constructNodeShapePath.append("\t?" + entityTypeName + "_s <" + complexProperty.getPropertyURI() + "> ?"
					+ entityTypeName + "_" + complexProperty.getEDMPropertyName() + " .\n");
		}
		for (RdfComplexProperty complexProperty : complexType.getComplexProperties().values()) {
			RdfNavigationProperty complexNavigationProperty = complexProperty.getRdfObjectPropertyShape().getPath();

			if (complexNavigationProperty.IsInverse()) {
				constructNodeShapePath
						.append("\t?" + entityTypeName + "_s <" + complexNavigationProperty.getInversePropertyOfURI()
								+ "> ?" + entityTypeName + complexProperty.getComplexPropertyName() + "_s" + " .\n");
			} else {
				constructNodeShapePath
						.append("\t?" + entityTypeName + "_s <" + complexNavigationProperty.getNavigationPropertyIRI()
								+ "> ?" + entityTypeName + "_s" + complexProperty.getComplexPropertyName() + " .\n");
			}
			constructNodeShapePath.append(constructNodeShapeType(entityTypeName, complexProperty));
			constructNodeShapePath.append(constructComplexType(
					entityTypeName + complexProperty.getComplexPropertyName(), complexProperty.getComplexType()));

		}
		for (RdfShapedNavigationProperty complexShapedNavigationProperty : complexType.getShapedNavigationProperties()
				.values()) {

			constructNodeShapePath.append("\t?" + entityTypeName + "_s <"
					+ complexShapedNavigationProperty.getRdfNavigationProperty().getNavigationPropertyIRI() + "> ?"
					+ entityTypeName + "_"
					+ complexShapedNavigationProperty.getRdfNavigationProperty().getEDMNavigationPropertyName()
					+ " .\n");
		}
		return constructNodeShapePath;
	}

	private StringBuilder constructNodeShapeType(String entityTypeName, RdfComplexProperty complexProperty) {
		StringBuilder constructNodeShapeType = new StringBuilder();
		if (DEBUG)
			constructNodeShapeType.append("\t#constructNodeShapeType\n");
		constructNodeShapeType.append("\t?" + entityTypeName + complexProperty.getComplexPropertyName() + "_s <"
				+ RdfConstants.ASSERTEDSHAPE + "> <" + complexProperty.getComplexType().getIRI() + "> .\n");
		return constructNodeShapeType;
	}

	private StringBuilder constructPath() throws EdmException {
		StringBuilder constructPath = new StringBuilder();
		if (DEBUG)
			constructPath.append("\t#constructPath\n");
		String key = edmTargetEntitySet.getEntityType().getName();
		constructPath.append("\t").append("?" + key + "_s ?" + key + "_p ?" + key + "_o .\n");
		if (key.equals("Term")) {
			constructPath.append("\t").append("?" + key
					+ "_s  <http://www.w3.org/1999/02/22-rdf-syntax-ns#rdf_literal> ?" + key + "_literal .\n");
			constructPath.append("\t").append(
					"?" + key + "_s  <http://www.w3.org/1999/02/22-rdf-syntax-ns#resource> ?" + key + "_resource .\n");
			constructPath.append("\t").append(
					"?" + key + "_s  <http://www.w3.org/1999/02/22-rdf-syntax-ns#rdf_object> ?" + key + "_o .\n");
		}
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

	private StringBuilder constructExpandSelect() throws EdmException, OData2SparqlException {
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
		} else if (this.rdfTargetEntityType.isNodeShape()) {
			where.append(
					clausesNodeShapeProperties(this.rdfTargetEntityType.getEntityTypeName(), this.rdfTargetEntityType));
		} else if (isImplicitEntityType(edmTargetEntitySet.getEntityType())) {
			//??????????????????
			where.append(clausesPathProperties());
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

	private Object clausesNodeShapeProperties(String entityTypeName, RdfEntityType rdfTargetEntityType) {
		StringBuilder clausesNodeShapeProperties = new StringBuilder();
		if (DEBUG)
			clausesNodeShapeProperties.append("\t#clausesNodeShapeProperties\n");
		clausesNodeShapeProperties.append("\t{\n");
		for (RdfProperty property : rdfTargetEntityType.getInheritedProperties()) {
			if (property.getIsComplex()) {
				clausesComplexType(entityTypeName, clausesNodeShapeProperties, property.getComplexType());
			} else {
				if (property.isOptional()) {
					clausesNodeShapeProperties
							.append("\t\tOPTIONAL{\t\t?" + entityTypeName + "_s <" + property.getPropertyURI() + "> ?"
									+ entityTypeName + "_" + property.getEDMPropertyName() + " }\n");
				} else {
					clausesNodeShapeProperties.append("\t\t?" + entityTypeName + "_s <" + property.getPropertyURI()
							+ "> ?" + entityTypeName + "_" + property.getEDMPropertyName() + ".\n");
				}
			}
		}
		clausesNodeShapeProperties.append("\t}\n");
		return clausesNodeShapeProperties;
	}

	private void clausesComplexType(String entityTypeName, StringBuilder clausesNodeShapeProperties,
			RdfComplexType complexType) {
		for (RdfProperty complexProperty : complexType.getProperties().values()) {
			if (complexProperty.isOptional())
				clausesNodeShapeProperties.append("\t\tOPTIONAL{");
			clausesNodeShapeProperties.append("\t\t?" + entityTypeName + "_s <" + complexProperty.getPropertyURI()
					+ "> ?" + entityTypeName + "_" + complexProperty.getEDMPropertyName());
			if (complexProperty.isOptional())
				clausesNodeShapeProperties.append(" }\n");
			else
				clausesNodeShapeProperties.append(" .\n");
		}
		for (RdfComplexProperty complexProperty : complexType.getComplexProperties().values()) {
			RdfNavigationProperty complexNavigationProperty = complexProperty.getRdfObjectPropertyShape().getPath();
			if (complexProperty.isOptional())
				clausesNodeShapeProperties.append("\t\tOPTIONAL{");
			if (complexNavigationProperty.IsInverse()) {
				clausesNodeShapeProperties.append("\t\t{\n");
				clausesNodeShapeProperties.append(
						"\t\t\t?" + entityTypeName + "_s <" + complexNavigationProperty.getInversePropertyOfURI()
								+ "> ?" + entityTypeName + complexProperty.getComplexPropertyName() + "_s" + "\n");
				clausesNodeShapeProperties.append("\t\t}UNION{\n");
				clausesNodeShapeProperties
						.append("\t\t\t?" + entityTypeName + complexProperty.getComplexPropertyName() + "_s" + " <"
								+ complexNavigationProperty.getNavigationPropertyIRI() + "> ?" + entityTypeName + "_s");
				clausesNodeShapeProperties.append("\n\t\t}\n");
			} else {
				clausesNodeShapeProperties
						.append("\t\t?" + entityTypeName + "_s <" + complexNavigationProperty.getNavigationPropertyIRI()
								+ "> ?" + entityTypeName + "_s" + complexProperty.getComplexPropertyName());
			}
			clausesComplexType(entityTypeName + complexProperty.getComplexPropertyName(), clausesNodeShapeProperties,
					complexProperty.getComplexType());
			if (complexProperty.isOptional())
				clausesNodeShapeProperties.append(" }\n");
			//else
			//	clausesNodeShapeProperties.append(" .\n");
		}
		for (RdfShapedNavigationProperty complexShapedNavigationProperty : complexType.getShapedNavigationProperties()
				.values()) {

			if (complexShapedNavigationProperty.isOptional())
				clausesNodeShapeProperties.append("\t\tOPTIONAL{");
			//if(complexShapedNavigationProperty.getRdfNavigationProperty().IsInverse() ) {
			//	clausesNodeShapeProperties
			//	.append("\t?" + entityTypeName + "_" + complexShapedNavigationProperty.getRdfNavigationProperty().getEDMNavigationPropertyName() + " <" + complexShapedNavigationProperty.getRdfNavigationProperty().getInversePropertyOfURI()
			//			+ "> ?" + entityTypeName + "_s");
			//}else 
			{
				clausesNodeShapeProperties.append("\t\t?" + entityTypeName + "_s <"
						+ complexShapedNavigationProperty.getRdfNavigationProperty().getNavigationPropertyIRI() + "> ?"
						+ entityTypeName + "_"
						+ complexShapedNavigationProperty.getRdfNavigationProperty().getEDMNavigationPropertyName());
			}
			if (complexShapedNavigationProperty.isOptional())
				clausesNodeShapeProperties.append(" }\n");
			else
				clausesNodeShapeProperties.append(" .\n");
		}
	}

	private StringBuilder clausesPathProperties() throws EdmException {
		StringBuilder clausesPathProperties = new StringBuilder();
		if (DEBUG)
			clausesPathProperties.append("\t#clausesPathProperties\n");
		StringBuilder clausesSelect = clausesSelect(this.selectPropertyMap,
				edmTargetEntitySet.getEntityType().getName(), edmTargetEntitySet.getEntityType().getName(),
				rdfTargetEntityType, "\t", !isImplicitEntityType(edmTargetEntitySet.getEntityType()));
		if (clausesSelect.length() > 0) {
			//#167 Add optional to ensure that properties without attributes are included
			if (isImplicitEntityType(edmTargetEntitySet.getEntityType()))
				clausesPathProperties.append("\tOPTIONAL");
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
		clausesOperationProperties.append("BIND(").append(operationUUID(rdfOperationType))
				.append(" AS ?" + nextTargetKey + "_s)\n");
		clausesOperationProperties.append("\t}\n");
		return clausesOperationProperties;
	}

	private StringBuilder operationUUID(RdfEntityType rdfOperationType) {
		StringBuilder operationUUID = new StringBuilder();
		operationUUID.append("IRI(CONCAT(\"" + RdfConstants.URN_NS + "\",MD5(CONCAT(");
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

	private String getQueryOptionText(RdfRepository datasetRepository, List<CustomQueryOption> queryOptions,
			FunctionImportParameter functionImportParameter) throws OData2SparqlException {

		for (CustomQueryOption queryOption : queryOptions) {
			if (queryOption.getName().equals(functionImportParameter.getName()))
				switch (functionImportParameter.getType()) {
				//Fixes #86
				case "http://www.w3.org/2000/01/rdf-schema#Class":
				case "http://www.w3.org/2000/01/rdf-schema#Resource":
					return encodeIRI(datasetRepository, queryOption);
				default:
					return queryOption.getText();
				}
		}
		return null;
	}

	private String encodeIRI(RdfRepository datasetRepository, CustomQueryOption queryOption)
			throws OData2SparqlException {
		String resource = queryOption.getText().substring(1, queryOption.getText().length() - 1);
		if (resource.startsWith(RdfConstants.BLANKNODE)) {
			return "<" + resource.replace(RdfConstants.BLANKNODE, RdfConstants.BLANKNODE_RDF) + ">";
		} else {
			resource = UriUtils.odataToRdfQname(queryOption.getText());

			String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(queryOption.getText());
			resource = "<" + expandedKey + ">";
			return resource;
		}
	}

	private String getParameterValues(List<UriParameter> keyPredicates,
			FunctionImportParameter functionImportParameter) {
		for (UriParameter queryOption : keyPredicates) {
			if (queryOption.getName().equals(functionImportParameter.getName()))
				switch (functionImportParameter.getType()) {
				//Fixes #86
				case "http://www.w3.org/2000/01/rdf-schema#Class":
				case "http://www.w3.org/2000/01/rdf-schema#Resource":
					String resource = UriUtils.odataToRdfQname(queryOption.getText());
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
		String dataset = "";
		//RdfRepository proxyDatasetRepository =null;
		if (rdfOperationType.isProxy()) {
			// Locate the service for which this function is a proxy
			for (Entry<String, com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter> functionImportParameterEntry : rdfOperationType
					.getFunctionImportParameters().entrySet()) {
				com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter functionImportParameter = functionImportParameterEntry
						.getValue();
				if (functionImportParameter.isDataset()) {
					dataset = getQueryOptionText(this.rdfModel.getRdfRepository(), queryOptions,
							functionImportParameter);
					if (dataset == null) {
						throw new OData2SparqlException(
								"FunctionImport (" + rdfOperationType.getEntityTypeName() + ") requires dataset");
					} else {
						dataset = dataset.substring(1, dataset.length() - 1);
						//RdfEdmProvider proxiedRdfEdmProvider = this.rdfEdmProvider.getRdfEdmProviders().getRdfEdmProvider(dataset);
						this.rdfModel.addProxy(dataset);
						//this.addProxiedRdfEdmProvider(dataset,proxiedRdfEdmProvider);
						proxyDatasetRepository = this.rdfModel.getRdfRepository().getRepositories()
								.getRdfRepository(dataset);
						if (proxyDatasetRepository == null)
							throw new OData2SparqlException("FunctionImport (" + rdfOperationType.getEntityTypeName()
									+ ") refers to dataset " + dataset + " that is not recognized");
						break;
					}
				}
			}
			//Find the repository details of this service: endpoint and prefixes

		}
		for (Entry<String, com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter> functionImportParameterEntry : rdfOperationType
				.getFunctionImportParameters().entrySet()) {
			com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter functionImportParameter = functionImportParameterEntry
					.getValue();
			String parameterValue = "";
			switch (rdfResourceParts.getUriType()) {
			case URI11:
				parameterValue = getParameterValues(rdfResourceParts.getEntitySet().getKeyPredicates(),
						functionImportParameter);
				break;
			default:
				parameterValue = getQueryOptionText(proxyDatasetRepository, queryOptions, functionImportParameter);
			}
			if (functionImportParameter.isPropertyPath())
				parameterValue = preprocessPropertyPath(proxyDatasetRepository, parameterValue);
			if (functionImportParameter.isDataset())
				parameterValue = preprocessDataset(proxyDatasetRepository, parameterValue);
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

	private String preprocessPropertyPath(RdfRepository datasetRepository, String propertyPath) {
		String translatedPropertyPath = propertyPath.replaceAll("\\s", "");
		final Matcher propertyPathMatcher = propertyPathPattern.matcher(translatedPropertyPath);
		while (propertyPathMatcher.find()) {
			propertyPathMatcher.group(2);
			Matcher propertyMatcher = propertyPattern.matcher(propertyPathMatcher.group(2));
			while (propertyMatcher.find()) {
				String prefix = propertyMatcher.group(1).trim();
				String name = propertyMatcher.group(2).trim();
				if (prefix != null && name != null) { // qname to be converted to URI
					Namespace namespace = this.rdfModel.getRdfRepository().getNamespaces().get(prefix);
					if (namespace == null)
						namespace = datasetRepository.getNamespaces().get(prefix);
					if (namespace != null) {
						String uri = "<" + namespace.getName() + name + ">";
						translatedPropertyPath = translatedPropertyPath.replaceFirst(prefix + "~" + name, uri);
					}
				}
			}
		}
		return translatedPropertyPath;
	}

	private String preprocessDataset(RdfRepository datasetRepository, String parameterValue) {
		//return "\"" + datasetRepository.getDataRepository().getServiceUrl() + "\"";
		if (datasetRepository != null && datasetRepository.getDataRepository().getServiceUrl() != null) {
			return "<" + datasetRepository.getDataRepository().getServiceUrl() + ">";
		} else {
			throw new EdmException(
					"Invalid datasetRepository without SPARQL endpoint for proxy operation: " + parameterValue);
		}
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
			if(!rdfTargetEntityType.isProxy() && !isImplicitEntityType(edmTargetEntitySet.getEntityType())) clausesExpandSelect.append("\tUNION\n");
			clausesExpandSelect.append(expandItemsWhere(rdfTargetEntityType,
					rdfTargetEntityType.entityTypeName, this.expandOption.getExpandItems(), "\t", false));
		}
		return clausesExpandSelect;
	}

	private StringBuilder selectOperation() throws EdmException, OData2SparqlException {
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
		if (isImplicitEntityType(edmTargetEntitySet.getEntityType())) {
			selectExpand.append("\t").append("{\tSELECT ");
			selectExpand.append("?" + edmTargetEntitySet.getEntityType().getName() + "_s ");
			selectExpand.append("?" + edmTargetEntitySet.getEntityType().getName() + "_ap ");
			selectExpand.append("?" + edmTargetEntitySet.getEntityType().getName() + "_ao ");
			selectExpand.append("?" + edmTargetEntitySet.getEntityType().getName() + "_as\n");
		} else if ((uriType.equals(UriType.URI6B)) && limitClause().length() > 0) {
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
		selectExpand.append(selectExpandWhereLimit("\t\t"));
		selectExpand.append("\t").append("}\n");
		return selectExpand;
	}

	private StringBuilder selectExpandWhere(String indent) throws EdmException, OData2SparqlException {
		StringBuilder selectExpandWhere = new StringBuilder();
		if (DEBUG)
			selectExpandWhere.append(indent).append("#selectExpandWhere\n");
		selectExpandWhere.append(indent).append("{\n");
		//		selectExpandWhere.append(filter(indent + "\t"));
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
		case URI11:
		case URI15:
		case URI16:
			selectExpandWhere.append(clausesPath(indent + "\t"));
			break;
		default:
			selectExpandWhere.append("#Unhandled URIType: " + this.uriType + "\n");
		}
		StringBuilder searchAndFilter = new StringBuilder();
		searchAndFilter.append(search(indent + "\t\t\t"));
		searchAndFilter.append(clausesFilter(indent + "\t\t\t"));
		searchAndFilter.append(clausesExpandFilter(indent + "\t\t\t"));
		searchAndFilter.append(filter(indent + "\t\t\t"));
		//searchAndFilter.append(lambdaAllFilter(indent + "\t\t\t"));
		switch (uriType) {
		case URI1:
			selectExpandWhere.append(selectPath(searchAndFilter));
			selectExpandWhere.append(indent).append("}\n");
			break;
		case URI2:
		case URI3:
		case URI4:
		case URI5:
		case URI6A:
		case URI11:
		case URI15:
		case URI16:
			selectExpandWhere.append(searchAndFilter);
			selectExpandWhere.append(indent).append("}\n");
			break;
		case URI6B:
			//selectExpandWhere.append(indent).append("} ").append(limitClause()).append("\n");
			selectExpandWhere.append(searchAndFilter);
			selectExpandWhere.append(indent).append("} ").append("\n");
			break;
		default:
			selectExpandWhere.append("#Unhandled URIType: " + this.uriType + "\n");
		}
		return selectExpandWhere;
	}

	private StringBuilder selectExpandWhereLimit(String indent) throws EdmException, OData2SparqlException {
		StringBuilder selectExpandWhereLimit = new StringBuilder();

		switch (uriType) {
		case URI1:
		case URI2:
		case URI3:
		case URI4:
		case URI5:
		case URI6A:
		case URI11:
		case URI15:
		case URI16:
			break;
		case URI6B:
			selectExpandWhereLimit.append(indent).append(limitClause()).append("\n");
			break;
		default:
			selectExpandWhereLimit.append("#Unhandled URIType: " + this.uriType + "\n");
		}
		return selectExpandWhereLimit;
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
		case URI11:
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
		valuesSubClassOf.append("VALUES(?class){");
		RdfEntityType actualRdfEntityType = rdfEntityType;
		if (rdfEntityType.isNodeShape()) {
			actualRdfEntityType = rdfEntityType.getNodeShape().getTargetClass();
		}
		valuesSubClassOf.append("(<" + actualRdfEntityType.getURL() + ">)");
		for (RdfEntityType subType : actualRdfEntityType.getAllSubTypes()) {
			valuesSubClassOf.append("(<" + subType.getURL() + ">)");
		}
		return valuesSubClassOf;
	}

	private StringBuilder clausesPath_URI1(String indent) throws EdmException, OData2SparqlException {
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

	private StringBuilder clausesPath_URI2(String indent) throws EdmException, OData2SparqlException {
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

	private StringBuilder clausesPath_URI3(String indent) throws EdmException, OData2SparqlException {
		StringBuilder clausesPath = new StringBuilder();
		if (DEBUG)
			clausesPath.append("#clausesPath_URI3\n");
		if (uriInfo.getUriResourceParts().size() > 2) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getUriResourceParts(),
					((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates()));
			//	clausesPath.append(clausesPath_KeyPredicateValues(indent));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI4(String indent) throws EdmException, OData2SparqlException {
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

	private StringBuilder clausesPath_URI5(String indent) throws EdmException, OData2SparqlException {
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

	private StringBuilder clausesPath_URI6A(String indent) throws EdmException, OData2SparqlException {
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

	private StringBuilder clausesPath_URI6B(String indent) throws EdmException, OData2SparqlException {
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

	private StringBuilder clausesPath_URI16(String indent) throws EdmException, OData2SparqlException {
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

	private StringBuilder clausesPath_KeyPredicateValues(String indent) throws EdmException, OData2SparqlException {
		StringBuilder clausesPath_KeyPredicateValues = new StringBuilder();
		String key = "";
		if (uriType == UriType.URI11)
			return clausesPath_KeyPredicateValues;
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
					RdfNavigationProperty navProperty = rdfEntityType.findNavigationPropertyByEDMNavigationPropertyName(
							uriInfo.getUriResourceParts().get(1).getSegmentValue());
					key = "?" + rdfTargetEntityType.entityTypeName;
					clausesPath_KeyPredicateValues.append(indent).append("VALUES(" + key + "_s");// #116
					if (this.rdfModel.getRdfRepository().isWithMatching()) {
						clausesPath_KeyPredicateValues.append("m)");
					}else {
						clausesPath_KeyPredicateValues.append(")");
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
				RdfNavigationProperty navProperty = rdfEntityType.findNavigationPropertyByEDMNavigationPropertyName(
						uriInfo.getUriResourceParts().get(1).getSegmentValue());
				if (navProperty != null) {
					//key = "?" + rdfTargetEntityType
					//		.findNavigationPropertyByEDMAssociationName(navProperty.getInversePropertyOf().getLocalName()).getVarName();
					//Fixes #85
					key = "?" + rdfTargetEntityType
							.findNavigationPropertyByEDMNavigationPropertyName(
									navProperty.getInverseNavigationProperty().getNavigationPropertyName())
							.getVarName();
				} else {
					log.error("Failed to locate operation navigation property:"
							+ uriInfo.getUriResourceParts().get(1).getSegmentValue());
				}
			}
		} else {
			key = primaryKey_Variables(rdfEntityType).toString();
		}
		if (rdfResourceParts.getEntitySet().getKeyPredicates().size() != 0) {
			//if (((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getKeyPredicates().size() != 0) {
			//need to sort the values into same order as keys (treemap order)
			if (this.rdfModel.getRdfRepository().isWithMatching() && !rdfEntityType.isOperation()) {
				clausesPath_KeyPredicateValues.append(indent).append("VALUES(" + key + "m)");
			} else {
				clausesPath_KeyPredicateValues.append(indent).append("VALUES(" + key + ")");
			}
			TreeMap<String, String> keyValues = new TreeMap<String, String>();
			for (UriParameter entityKey : rdfResourceParts.getEntitySet().getKeyPredicates()) {
				//	for (UriParameter entityKey : ((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0))
				//			.getKeyPredicates()) {
				// Strip leading and trailing single quote from key
				String expandedKey = rdfModel.getRdfPrefixes()
						.expandPrefix(entityKey.getText().substring(1, entityKey.getText().length() - 1));

				keyValues.put(entityKey.getName(), expandKey(expandedKey));
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

	private String expandKey(String expandedKey) {
		String pathVariable = "";
		expandedKey = SparqlEntity.URLDecodeEntityKey(expandedKey);
		if (expandedKey.equals(RdfConstants.SPARQL_UNDEF)) {
			pathVariable = " " + RdfConstants.SPARQL_UNDEF + " ";
		} else if (expandedKey.startsWith(RdfConstants.BLANKNODE)) {
			pathVariable = "<" + expandedKey.replace(RdfConstants.BLANKNODE, RdfConstants.BLANKNODE_RDF) + ">";
		} else {
			pathVariable = "<" + expandedKey + ">";
		}
		return pathVariable;
	}

	private StringBuilder clausesPathNavigation(String indent, List<UriResource> navigationSegments,
			List<UriParameter> entityKeys) throws EdmException, OData2SparqlException {
		StringBuilder clausesPathNavigation = new StringBuilder();

		String path = "";//edmEntitySet.getEntityType().getName();//edmTargetEntitySet.getEntityType().getName();
		boolean isFirstSegment = true;
		Integer index = 0;
		String keyVariable = "";
		String pathVariable = "";
		String entityKeyVariable = "";
		String targetVariable = "";
		String sourceVariable = "";
		String matchTargetVariable;
		String keyBindString = "";
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
			clausesPathNavigation.append(indent).append("#pathSegment").append(index).append("\n");
			UriResource navigationSegment = navigationSegments.get(index);
			if (!navigationSegment.getKind().equals(UriResourceKind.complexProperty)) {
				UriResourceNavigation uriResourceNavigation = ((UriResourceNavigation) navigationSegment);
				EdmNavigationProperty predicate = uriResourceNavigation.getProperty();
				RdfNavigationProperty navProperty = rdfModelToMetadata.getMappedNavigationProperty(
						new FullQualifiedName(predicate.getType().getNamespace(), predicate.getName()));
				// Not possible to have more than one key field is it?
				UriResource currentSegment = navigationSegments.get(index-1);
				switch (currentSegment.getClass().toString()) {
				
				case "class org.apache.olingo.server.core.uri.UriResourceNavigationPropertyImpl":
					entityKeys=	((UriResourceNavigation) currentSegment).getKeyPredicates(); break;
				case "class org.apache.olingo.server.core.uri.UriResourceEntitySetImpl":
					entityKeys=	((UriResourceEntitySet) currentSegment).getKeyPredicates(); break;
				case "class org.apache.olingo.server.core.uri.UriResourceComplexPropertyImpl":
					entityKeys=	null; break;
				}
				if(entityKeys!=null) {
					for (UriParameter entityKey : entityKeys) {
						String expandedKey = rdfModel.getRdfPrefixes()
								.expandPrefix(entityKey.getText().substring(1, entityKey.getText().length() - 1));
						pathVariable = expandKey(expandedKey);
						keyBindString = keyBindString + indent + "BIND(" +expandKey(expandedKey) +" AS ?" + path + "_s)\n";
						pathVariable = "?" + path + "_s";
					}
				}
				if (this.rdfModel.getRdfRepository().isWithMatching()) {
					keyVariable = "?key_s";
					clausesPathNavigation.append(clausesMatch(keyVariable, pathVariable, indent));
					pathVariable = keyVariable;
				}
				entityKeyVariable = pathVariable;
				pathVariable = "?" + path + "_s";

				if (index.equals(lastIndex - 1)) {
					targetVariable = "?" + edmTargetEntitySet.getEntityType().getName() + "_s";
					sourceVariable = pathVariable;
				} else {
					targetVariable = "?" + path + navProperty.getNavigationPropertyName() + "_s";
					sourceVariable = pathVariable;
				}
				if (this.rdfModel.getRdfRepository().isWithMatching()) {
					matchTargetVariable = "?" + path + navProperty.getNavigationPropertyName() + "_sm";
				} else {
					matchTargetVariable = targetVariable;
				}
				if (isImplicitNavigationProperty(navProperty)) {
					String target = edmTargetEntitySet.getEntityType().getName();

					if (index == segmentSize - 1) {
						switch (navProperty.getEDMNavigationPropertyName()) {
						case RdfConstants.RDF_HASFACTS_LABEL:
							clausesPathNavigation.append(indent).append(entityKeyVariable).append(" ?").append(target)
									.append("_ap ?").append(target).append("_ao .\n");
							clausesPathNavigation.append(indent).append("BIND(?").append(target).append("_ap as ?")
									.append(target).append("_s)\n");
							break;
						case RdfConstants.RDF_HASVALUES_LABEL:
							clausesPathNavigation.append(indent).append(entityKeyVariable).append(" ?").append(target)
									.append("_ap ?").append(target).append("_ao .\n");
							clausesPathNavigation.append(indent).append("BIND(?").append(target).append("_ao as ?")
									.append(target).append("_s)\n");
							break;
						case RdfConstants.RDF_ISOBJECTIVE_LABEL:
							clausesPathNavigation.append(indent).append("?").append(target).append("_as ?")
									.append(target).append("_ap ").append(entityKeyVariable).append(".\n");
							clausesPathNavigation.append(indent).append("BIND(?").append(target).append("_ap as ?")
									.append(target).append("_s)\n");
							break;
						case RdfConstants.RDF_HASSUBJECTS_LABEL:
							clausesPathNavigation.append(indent).append("?").append(target).append("_ao").append(" ?")
									.append(target).append("_ap ").append(entityKeyVariable).append(" .\n");
							clausesPathNavigation.append(indent).append("BIND(?").append(target).append("_ao as ?")
									.append(target).append("_s)\n");
							break;
						default:
							break;
						}

					}
					if (uriResourceNavigation.getKeyPredicates().size() > 0) {
						String navigationKey = "";
						for (UriParameter entityKey : uriResourceNavigation.getKeyPredicates()) {

							String expandedKey = rdfModel.getRdfPrefixes()
									.expandPrefix(entityKey.getText().substring(1, entityKey.getText().length() - 1));
							navigationKey = expandKey(expandedKey);
						}
						switch (navProperty.getEDMNavigationPropertyName()) {
						case RdfConstants.RDF_HASFACTS_LABEL:
						case RdfConstants.RDF_ISOBJECTIVE_LABEL:
							clausesPathNavigation.append(indent).append("FILTER(?").append(target).append("_ap = ")
									.append(navigationKey).append(")\n");
							break;
						case RdfConstants.RDF_HASVALUES_LABEL:
						case RdfConstants.RDF_HASSUBJECTS_LABEL:
							clausesPathNavigation.append(indent).append("FILTER(?").append(target).append("_ao = ")
									.append(navigationKey).append(")\n");
							break;
						default:
							if (this.rdfModel.getRdfRepository().isWithMatching()) {
								clausesPathNavigation
										.append(clausesMatchNavigationKey(targetVariable, navigationKey, indent));
							}
							break;
						}
					} else {

					}

				} else {
					if (navProperty.IsInverse()) {
						clausesPathNavigation.append(indent).append("{\n");
						clausesPathNavigation.append(indent).append("\t").append("\t" + matchTargetVariable + " <"
								+ navProperty.getInversePropertyOf().getIRI() + "> " + sourceVariable + " .\n");
						clausesPathNavigation.append(indent).append("} UNION {\n");
						clausesPathNavigation.append(indent).append("\t").append("\t" + sourceVariable + " <"
								+ navProperty.getNavigationPropertyIRI() + "> " + matchTargetVariable + " .\n");
						clausesPathNavigation.append(indent).append("}\n");
					} else {
						clausesPathNavigation.append(indent).append("\t").append(sourceVariable + " <"
								+ navProperty.getNavigationPropertyIRI() + "> " + matchTargetVariable + " .\n");
					}
					if (this.rdfModel.getRdfRepository().isWithMatching()) {
						clausesPathNavigation.append(clausesMatch(matchTargetVariable, targetVariable, indent));
						if (uriResourceNavigation.getKeyPredicates().size() > 0) {
							String navigationKey = "";
							for (UriParameter entityKey : uriResourceNavigation.getKeyPredicates()) {

								String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(
										entityKey.getText().substring(1, entityKey.getText().length() - 1));
								navigationKey = expandKey(expandedKey);
							}
							clausesPathNavigation
									.append(clausesMatchNavigationKey(targetVariable, navigationKey, indent));
						}
					}
				}
				path += navProperty.getNavigationPropertyName();
				isFirstSegment = false;
			} else {
				if (isFirstSegment) {
					// Not possible to have more than one key field is it?
					for (UriParameter entityKey : entityKeys) {
						String expandedKey = rdfModel.getRdfPrefixes()
								.expandPrefix(entityKey.getText().substring(1, entityKey.getText().length() - 1));
						pathVariable = expandKey(expandedKey);
					}
					keyVariable = "?" + edmEntitySet.getEntityType().getName()+ "_s";
					if (this.rdfModel.getRdfRepository().isWithMatching()) {
						clausesPathNavigation.append(clausesMatch(keyVariable, pathVariable, indent));
						pathVariable = keyVariable;
					}else {
						clausesPathNavigation.append(indent).append("BIND("+pathVariable + " AS " + keyVariable + ")\n");
						
					}
				}
			}
		}
		return clausesPathNavigation.insert(0, keyBindString );
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

	private StringBuilder selectPath(StringBuilder searchAndFilter) throws EdmException, OData2SparqlException {
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
		//		selectPath.append(filter(indent + "\t"));
		//selectPath.append(search(indent + "\t"));
		selectPath.append(clausesPath(indent + "\t"));
		selectPath.append(searchAndFilter);
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
		//selectPath.append(lambdaAllFilter("\t\t\t"));
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
//	private StringBuilder lambdaAllFilter(String indent) {
//		StringBuilder lambdaAllFilter = new StringBuilder().append(indent);
//		if (DEBUG)
//			lambdaAllFilter.append("#lambdaAllFilter\n");
////		return lambdaAllFilter;
//		if (this.filterClause != null
//				&& this.filterClause.getNavPropertyPropertyFilters().get(rdfTargetEntityType.entityTypeName) != null) {
//			lambdaAllFilter.append(indent).append("MINUS{\n");
//			lambdaAllFilter.append(filterClauses.getClausesFilter(indent + "\t"));
//			lambdaAllFilter.append(indent + "\t").append(filterClauses.getFilter());
//			lambdaAllFilter.append(indent).append("}\n");
//		} else {
//			// clausesFilter.append(clausesFilter(null,
//			// rdfEntityType.entityTypeName, indent,null));
//		}
//		return lambdaAllFilter;
//	}
	private StringBuilder expandComplexConstruct(RdfEntityType targetEntityType, String targetKey, String indent)
			throws EdmException {
		StringBuilder expandItemsConstruct = new StringBuilder();

		for (String navigationPropertyName : this.edmPathComplexType.getNavigationPropertyNames()) {
			RdfNavigationProperty navigationProperty = rdfModelToMetadata.getMappedNavigationProperty(
					new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(), navigationPropertyName));
			expandItemsConstruct.append(expandComplexPropertyConstruct(targetEntityType, targetKey, indent,
					navigationProperty, targetKey + navigationProperty.getNavigationPropertyName(),
					navigationProperty.getRangeClass()));

		}
		return expandItemsConstruct;
	}

	private StringBuilder expandItemsConstruct(RdfEntityType targetEntityType, String targetKey,
			List<ExpandItem> expandItems, String indent) throws EdmException, OData2SparqlException {
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
						for (RdfNavigationProperty navigationProperty : rdfComplexProperty.getComplexType()
								.getNavigationProperties().values()) {
							expandItemsConstruct.append(expandItemConstruct(targetEntityType, targetKey, indent,
									expandItem, navigationProperty,
									targetKey + navigationProperty.getNavigationPropertyName(),
									navigationProperty.getRangeClass()));
						}
					}
				} else {
					for (RdfNavigationProperty navigationProperty : targetEntityType
							.getInheritedNavigationProperties()) {
						if (validateOperationCallable(navigationProperty.getRangeClass())) {
							expandItemsConstruct.append(expandItemConstruct(targetEntityType, targetKey, indent,
									expandItem, navigationProperty,
									targetKey + navigationProperty.getNavigationPropertyName(),
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

				RdfNavigationProperty navProperty = rdfModelToMetadata.getMappedNavigationProperty(
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

	private Boolean validateOperationCallable(RdfEntityType rdfEntityType) throws OData2SparqlException {
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
						String parameterValue = getQueryOptionText(this.rdfModel.getRdfRepository(),
								uriInfo.getCustomQueryOptions(), functionImportParameter);
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
			String indent, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType) {
		StringBuilder expandComplexPropertyConstruct = new StringBuilder();

		if (navProperty.getRangeClass().isOperation()) {
			expandComplexPropertyConstruct.append(indent + "\t").append("?" + targetKey + "_s <"
					+ navProperty.getNavigationPropertyIRI() + "> ?" + nextTargetKey + "_s .\n");
			expandComplexPropertyConstruct.append(indent)
					.append(constructOperation(nextTargetKey, navProperty.getRangeClass(), indent, true));
		} else if (navProperty.getDomainClass().isOperation()) {

		} else {
			expandComplexPropertyConstruct.append(indent + "\t").append("?" + targetKey + "_s <"
					+ navProperty.getNavigationPropertyIRI() + "> ?" + nextTargetKey + "_s .\n");
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
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType) throws EdmException, OData2SparqlException {
		StringBuilder expandItemConstruct = new StringBuilder();

		if (navProperty.getRangeClass().isOperation()) {
			expandItemConstruct.append(indent + "\t").append("?" + targetKey + "_s <"
					+ navProperty.getNavigationPropertyIRI() + "> ?" + nextTargetKey + "_s .\n");
			expandItemConstruct.append(indent)
					.append(constructOperation(nextTargetKey, navProperty.getRangeClass(), indent, true));
		} else if (navProperty.getDomainClass().isOperation()) {

		} else {
			expandItemConstruct.append(indent + "\t").append("?" + targetKey + "_s <"
					+ navProperty.getNavigationPropertyIRI() + "> ?" + nextTargetKey + "_s .\n");
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
						+ navProperty.getNavigationPropertyName() + "> ?" + nextTargetKey + "_count.\n");
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

				RdfNavigationProperty navigationProperty = rdfModelToMetadata.getMappedNavigationProperty(
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
			String indent, RdfNavigationProperty navProperty, String nextTargetKey, RdfEntityType nextTargetEntityType)
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
				expandComplexNavigationProperty.append(indent).append("\t\t\t\t\t").append("?" + targetKey + "_s <"
						+ navProperty.getNavigationPropertyIRI() + "> " + matchingTargetKey + " .\n");
				expandComplexNavigationProperty.append(indent).append("\t\t\t\t").append("}\n");

			} else {
				expandComplexNavigationProperty.append(indent).append("\t\t\t\t").append("?" + targetKey + "_s <"
						+ navProperty.getNavigationPropertyIRI() + "> " + matchingTargetKey + " .\n");
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
			List<ExpandItem> expandItems, String indent, Boolean withinService)
			throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {
		StringBuilder expandItemsWhere = new StringBuilder();
		boolean firstExpandItem = true;
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
						for (RdfNavigationProperty navigationProperty : rdfComplexProperty.getComplexType()
								.getNavigationProperties().values()) {
							if (validateOperationCallable(navigationProperty.getRangeClass())) {
								expandItemsWhere.append(expandItemWhere(targetEntityType, targetKey, indent, expandItem,
										navigationProperty, targetKey + navigationProperty.getNavigationPropertyName(),
										navigationProperty.getRangeClass(), firstExpandItem,withinService));
								firstExpandItem = false;
							}
						}
					}
				} else {
					for (RdfNavigationProperty navigationProperty : targetEntityType
							.getInheritedNavigationProperties()) {
						if (validateOperationCallable(navigationProperty.getRangeClass())) {
							expandItemsWhere.append(expandItemWhere(targetEntityType, targetKey, indent, expandItem,
									navigationProperty, targetKey + navigationProperty.getNavigationPropertyName(),
									navigationProperty.getRangeClass(), firstExpandItem,withinService));
							firstExpandItem = false;
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

				RdfNavigationProperty navProperty = rdfModelToMetadata.getMappedNavigationProperty(
						new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(),
								resourceNavigation.getProperty().getName()));

				String nextTargetKey = targetKey + resourceNavigation.getProperty().getName();
				RdfEntityType nextTargetEntityType = navProperty.getRangeClass();

				expandItemsWhere.append(expandItemWhere(targetEntityType, targetKey, indent, expandItem, navProperty,
						nextTargetKey, nextTargetEntityType, firstExpandItem,withinService));
				firstExpandItem = false;
			}
		}
		return expandItemsWhere;
	}

	private StringBuilder expandItemWhere(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType, Boolean firstExpandItem, Boolean withinService)
			throws OData2SparqlException, ODataApplicationException, ExpressionVisitException {

		StringBuilder expandItemWhere = new StringBuilder();
		//expandItemWhere.append(indent).append("#expandItemWhere\n");
		if (navProperty.getDomainClass().isOperation()) {
			expandItemWhere.append(operationExpandItemWhere(targetEntityType, targetKey, indent, expandItem,
					navProperty, nextTargetKey, nextTargetEntityType, firstExpandItem,withinService));
		} else if (isImplicitNavigationProperty(navProperty)) {
			expandItemWhere.append(implicitExpandItemWhere(targetEntityType, targetKey, indent, expandItem, navProperty,
					nextTargetKey, nextTargetEntityType, firstExpandItem,withinService));
		} else {
			expandItemWhere.append(standardExpandItemWhere(targetEntityType, targetKey, indent, expandItem, navProperty,
					nextTargetKey, nextTargetEntityType, firstExpandItem,withinService));
		}
		return expandItemWhere;
	}

	private StringBuilder operationExpandItemWhere(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType, Boolean firstExpandItem ,Boolean withinService) throws EdmException, ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		StringBuilder operationExpandItemWhere = new StringBuilder();
		operationExpandItemWhere.append(indent).append("#operationExpandItemWhere\n");
		
		
		operationExpandItemWhere.append(indent).append("{\n");
		operationExpandItemWhere.append(indent).append("\tBIND(?" + navProperty.getRelatedKey() + " AS ?" + nextTargetKey + "_s");
		if (this.rdfModel.getRdfRepository().isWithMatching()) {
			operationExpandItemWhere.append("m");
			}
		operationExpandItemWhere.append(")\n");
		if (navProperty.getDomainClass().isProxy()
				&& this.proxyDatasetRepository != this.rdfModel.getRdfRepository()) {
			operationExpandItemWhere.append(indent)	.append("\tSERVICE<" + this.proxyDatasetRepository.getDataRepository().getServiceUrl()+ "?distinct=true&infer=false>{\n"); 
			withinService=true;
		}
		operationExpandItemWhere.append(indent).append("\t\t{\n");		
		operationExpandItemWhere.append(indent)	.append(selectObjectPropertyValues(targetKey, indent+"\t", expandItem, navProperty, nextTargetKey));
		operationExpandItemWhere.append(indent).append("\t\t}\n");
		if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
			operationExpandItemWhere.append(expandItemsWhere(nextTargetEntityType, nextTargetKey,
					expandItem.getExpandOption().getExpandItems(), indent + "\t\t",true));
		}	
		if (withinService) {
			operationExpandItemWhere.append(indent).append("\t}\n");	
			withinService=false;
		}
		operationExpandItemWhere.append(indent).append("}\n");
		return operationExpandItemWhere;
	}

	private StringBuilder implicitExpandItemWhere(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType, boolean firstExpandItem,Boolean withinService) throws ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		StringBuilder implicitExpandItemWhere = new StringBuilder();
		implicitExpandItemWhere.append(indent).append("#implicitExpandItemWhere\n");
		implicitExpandItemWhere.append(indent).append("OPTIONAL{\n");

		
		StringBuilder selectImplicitExpandProperties = selectImplicitExpandProperties(targetEntityType, targetKey, indent, expandItem,
				navProperty, nextTargetKey, nextTargetEntityType, firstExpandItem);
		
		if (selectImplicitExpandProperties.length() > 0) {
			implicitExpandItemWhere.append(indent).append(selectImplicitExpandProperties);
			if ((expandItem.getCountOption() != null) && expandItem.getCountOption().getValue()) {
				implicitExpandItemWhere.append(indent).append("\tUNION\n");
			}
		}
		
		if ((expandItem.getCountOption() != null) && expandItem.getCountOption().getValue()) {
			implicitExpandItemWhere.append(expandItemWhereCount(targetEntityType, targetKey, indent, expandItem,
					navProperty, nextTargetKey, nextTargetEntityType));
		}
		//[UNION?
		//ExpandItemWhere]?
		if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
			//implicitExpandItemWhere.append(indent).append("\tUNION\n");
			implicitExpandItemWhere.append(expandItemsWhere(nextTargetEntityType, nextTargetKey,
					expandItem.getExpandOption().getExpandItems(), indent + "\t", withinService));
		}
		implicitExpandItemWhere.append(indent).append("}\n");
		return implicitExpandItemWhere;
	}

	private StringBuilder standardExpandItemWhere(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType, boolean firstExpandItem,Boolean withinService)
			throws ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		StringBuilder standardExpandItemWhere = new StringBuilder();
		standardExpandItemWhere.append(indent).append("#standardExpandItemWhere\n");
		if(firstExpandItem) {
			standardExpandItemWhere.append(indent).append("{\n");
		}else {
			standardExpandItemWhere.append(indent).append("UNION{\n");
		}

		StringBuilder selectExpandProperties = selectExpandProperties(targetEntityType, targetKey, indent, expandItem,
				navProperty, nextTargetKey, nextTargetEntityType, firstExpandItem,withinService);
		if (selectExpandProperties.length() > 0) {
			standardExpandItemWhere.append(selectExpandProperties);
			if ((expandItem.getCountOption() != null) && expandItem.getCountOption().getValue()) {
				standardExpandItemWhere.append(indent).append("\tUNION\n");
			}
		}
		if ((expandItem.getCountOption() != null) && expandItem.getCountOption().getValue()) {
			standardExpandItemWhere.append(expandItemWhereCount(targetEntityType, targetKey, indent, expandItem,
					navProperty, nextTargetKey, nextTargetEntityType));
		}
		//Nested within expand
		//[UNION?
		//ExpandItemWhere]?
//		if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
//
//			standardExpandItemWhere.append(expandItemsWhere(nextTargetEntityType, nextTargetKey,
//					expandItem.getExpandOption().getExpandItems(), indent + "\t",withinService));
//		}
		standardExpandItemWhere.append(indent).append("}\n");
		return standardExpandItemWhere;
	}
	private StringBuilder selectImplicitExpandProperties(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType, boolean firstExpandItem) {
		StringBuilder selectImplicitExpandProperties = new StringBuilder();
		selectImplicitExpandProperties.append(indent).append("#selectImplicitExpandProperties\n");
		selectImplicitExpandProperties.append(indent).append("\t{\n");
		if (navProperty.getDomainCardinality().equals(Cardinality.ONE)
				|| navProperty.getDomainCardinality().equals(Cardinality.MULTIPLE)) {
			selectImplicitExpandProperties.append(indent).append("\t{\n");
		} else {
			selectImplicitExpandProperties.append(indent).append("\t\t{\n");
		}
		selectImplicitExpandProperties.append(expandImplicit(targetKey, navProperty, indent + "\t\t", selectObjectPropertyValues(targetKey, indent, expandItem, navProperty, nextTargetKey)));
		selectImplicitExpandProperties.append(indent).append("\t\t}\n");
		selectImplicitExpandProperties.append(indent).append("\t}\n");
		return selectImplicitExpandProperties;
	}
	private StringBuilder selectExpandProperties(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType, boolean firstExpandItem, Boolean withinService) throws EdmException, ODataApplicationException, ExpressionVisitException, OData2SparqlException {
		StringBuilder selectExpandProperties = new StringBuilder();
		selectExpandProperties.append(indent).append("\t#selectExpandProperties\n");
		selectExpandProperties.append(indent).append("\t{\n");
		if (navProperty.getDomainCardinality().equals(Cardinality.ONE)
				|| navProperty.getDomainCardinality().equals(Cardinality.MULTIPLE)) {
			selectExpandProperties.append(indent).append("\t\t{\n");
		} else {
			selectExpandProperties.append(indent).append("\t\tOPTIONAL{\n");
		}
		selectExpandProperties.append(selectObjectProperties(targetKey, indent, expandItem, navProperty, nextTargetKey,withinService));
		selectExpandProperties.append(indent).append("\t\t}\n");
		selectExpandProperties.append(selectObjectPropertyValues(targetKey, indent, expandItem, navProperty, nextTargetKey));
		//Nest the expand items within query to improve query execution performance ... deepest first
		if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
			selectExpandProperties.append(expandItemsWhere(nextTargetEntityType, nextTargetKey,
				expandItem.getExpandOption().getExpandItems(), indent + "\t\t",withinService));
		}	

		selectExpandProperties.append(indent).append("\t}\n");
		return selectExpandProperties;
	}

	private StringBuilder selectObjectProperties(String targetKey, String indent, ExpandItem expandItem,
			RdfNavigationProperty navProperty, String nextTargetKey, Boolean withinService) {

		StringBuilder selectObjectProperties = new StringBuilder();		
		//Should not use a subselect within a service call
		if (withinService) {
			selectObjectProperties.append(indent).append("{\n");
		}else {
			selectObjectProperties.append(indent).append("\t\t\t").append("SELECT ?" + targetKey + "_s ?" + nextTargetKey + "_s {\n");
		}
		String matchingTargetKey = "?" + nextTargetKey + "_s";
		if (this.rdfModel.getRdfRepository().isWithMatching()) {
			matchingTargetKey = matchingTargetKey + "m";
		}
		if (navProperty.getDomainClass().isOperation()) {
			// Nothing to add as BIND assumed to be created
		} else if (navProperty.IsInverse()) {

			selectObjectProperties.append(indent).append("\t\t\t\t{\n");
			selectObjectProperties.append(indent).append("\t\t\t\t\t").append(matchingTargetKey + " <"
					+ navProperty.getInversePropertyOf().getIRI() + "> ?" + targetKey + "_s .\n");
			selectObjectProperties.append(indent).append("\t\t\t\t").append("}UNION{\n");
			selectObjectProperties.append(indent).append("\t\t\t\t\t").append("?" + targetKey + "_s <"
					+ navProperty.getNavigationPropertyIRI() + "> " + matchingTargetKey + " .\n");
			selectObjectProperties.append(indent).append("\t\t\t\t").append("}\n");

		} else {
			selectObjectProperties.append(indent).append("\t\t\t\t").append("?" + targetKey + "_s <"
					+ navProperty.getNavigationPropertyIRI() + "> " + matchingTargetKey + " .\n");
		}
		if (this.rdfModel.getRdfRepository().isWithMatching())
			selectObjectProperties.append(clausesMatch("?" + nextTargetKey + "_s", indent + "\t\t\t\t"));
		selectObjectProperties.append(indent).append("\t\t\t}");

		if ((expandItem.getTopOption() != null)) {
			selectObjectProperties.append(" LIMIT " + expandItem.getTopOption().getValue());
		} else if ((expandItem.getSelectOption() != null) && (expandItem.getTopOption() == null)) {
			// Fixes #176 
			//						if(this.rdfModel.getRdfRepository().getExpandOrderbyDefault()) {
			//							expandItemWhere.append(" ORDER BY ?"+  nextTargetKey + "_s" );
			//						}
			//						if(this.rdfModel.getRdfRepository().getExpandTopDefault()!= null) {
			//							expandItemWhere.append(" LIMIT "+ this.rdfModel.getRdfRepository().getExpandTopDefault());
			//						}
			//						if(this.rdfModel.getRdfRepository().getExpandSkipDefault()!= null) {
			//							expandItemWhere.append(" OFFSET "+ this.rdfModel.getRdfRepository().getExpandSkipDefault());
			//						}

		} else if ((expandItem.getSelectOption() == null) && (expandItem.getCountOption() != null)
				&& (expandItem.getCountOption().getValue())) {
			// Fixes #78 by setting limit even if $top not specified, as it cannot be in OpenUI5.
			selectObjectProperties.append(" LIMIT 0");
		}
		if ((expandItem.getSkipOption() != null)) {
			selectObjectProperties.append(" OFFSET " + expandItem.getSkipOption().getValue());
		}
		selectObjectProperties.append("\n");
		return selectObjectProperties;
	}

	private StringBuilder selectObjectPropertyValues(String targetKey, String indent, ExpandItem expandItem,
			RdfNavigationProperty navProperty, String nextTargetKey) {
		TreeSet<String> selectedProperties = createSelectPropertyMap(navProperty.getRangeClass(),
				expandItem.getSelectOption());
		if (selectedProperties != null && selectedProperties.isEmpty())
			return new StringBuilder();
		StringBuilder clausesSelect = clausesSelect(selectedProperties, nextTargetKey, nextTargetKey,
				navProperty.getRangeClass(), indent + "\t", isPrimitiveValue);
		return clausesSelect;
	}

	@SuppressWarnings("unused")
	private StringBuilder expandItemWhere_old(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType, boolean firstExpandItem)
			throws OData2SparqlException, ODataApplicationException, ExpressionVisitException {

		StringBuilder expandItemWhere = new StringBuilder();

		// Not optional if filter imposed on path but should really be equality like filters, not negated filters
		//SparqlExpressionVisitor expandFilterClause;

		expandItemWhere.append(indent).append("#expandItemWhere\n");
		boolean isImplicitNavigationProperty = isImplicitNavigationProperty(navProperty);
		if (navProperty.getDomainClass().isOperation()) {//Fixes #103 || limitSet()) {
			expandItemWhere.append(indent)
					.append("{BIND(?" + navProperty.getRelatedKey() + " AS ?" + nextTargetKey + "_s");
			if (this.rdfModel.getRdfRepository().isWithMatching()) { //Fix #117
				expandItemWhere.append("m");
			}
			expandItemWhere.append(")\n");
			if (navProperty.getDomainClass().isProxy()
					&& this.proxyDatasetRepository != this.rdfModel.getRdfRepository()) {
				expandItemWhere.append(indent)
						.append("{SERVICE<" + this.proxyDatasetRepository.getDataRepository().getServiceUrl()
								+ "?distinct=true&infer=false>{\n"); //timeout=5&
				//No optional with service call because of performance 
				//expandItemWhere.append(indent).append("OPTIONAL");
				expandItemWhere.append("\t{\n");
			} else {
				expandItemWhere.append(indent).append("UNION");
				if (expandItem.getSelectOption() != null)
					expandItemWhere.append("\t{OPTIONAL\n");
			}
		} else if (isImplicitNavigationProperty) {
			if (!firstExpandItem) {
				expandItemWhere.append(indent).append("UNION{");//.append("OPTIONAL");
				if (expandItem.getSelectOption() != null)
					expandItemWhere.append("\t{OPTIONAL\n");
			} else {
				expandItemWhere.append(indent).append("UNION{");//.append("OPTIONAL");
				if (expandItem.getSelectOption() != null)
					expandItemWhere.append("\t{OPTIONAL\n");
			}
		} else {//if( !firstExpandItem){
			expandItemWhere.append(indent).append("UNION{");
			if (navProperty.getDomainCardinality().equals(Cardinality.ONE)
					|| navProperty.getDomainCardinality().equals(Cardinality.MULTIPLE)) {
				expandItemWhere.append("\t{\n");
			} else {
				if (expandItem.getSelectOption() != null)
					expandItemWhere.append("\t{OPTIONAL\n");
			}
		}
		//expandItemWhere.append("\t{\n");
		//expandItemWhere.append(indent);

		if (navProperty.getRangeClass().isOperation()) {
			expandItemWhere.append(indent).append("\t{\n");
			expandItemWhere.append(clausesOperationProperties(nextTargetKey, navProperty.getRangeClass()));
			expandItemWhere.append(indent).append("\t}\n");
		} else {
			if ((expandItem.getSelectOption() == null) && (expandItem.getCountOption() != null)
					&& (expandItem.getCountOption().getValue())) {

			} else {
				TreeSet<String> selectedProperties = createSelectPropertyMap(navProperty.getRangeClass(),
						expandItem.getSelectOption());
				StringBuilder clausesSelect = clausesSelect(selectedProperties, nextTargetKey, nextTargetKey,
						navProperty.getRangeClass(), indent + "\t\t",
						!isImplicitNavigationProperty && !navProperty.getDomainClass().isProxy());
				expandItemWhere.append(indent).append("\t{\n");
				if (isImplicitNavigationProperty) {
					expandItemWhere.append(expandImplicit(targetKey, navProperty, indent + "\t\t", clausesSelect));
				} else {
					if (selectedProperties != null && selectedProperties.isEmpty())
						return new StringBuilder();
					if (navProperty.getDomainClass().isOperation()) {
						expandItemWhere.append(indent).append("\t\t{").append(" {\n");
					} else {
						expandItemWhere.append(indent).append("\t\t{")
								.append("SELECT ?" + targetKey + "_s ?" + nextTargetKey + "_s {\n");
					}
					//expandItemWhere.append(selectExpandWhere("\t\t\t\t"));
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
						expandItemWhere.append(indent).append("\t\t\t\t\t").append("?" + targetKey + "_s <"
								+ navProperty.getNavigationPropertyIRI() + "> " + matchingTargetKey + " .\n");
						expandItemWhere.append(indent).append("\t\t\t\t").append("}\n");

					} else {
						expandItemWhere.append(indent).append("\t\t\t\t").append("?" + targetKey + "_s <"
								+ navProperty.getNavigationPropertyIRI() + "> " + matchingTargetKey + " .\n");
					}
					if (this.rdfModel.getRdfRepository().isWithMatching())
						expandItemWhere.append(clausesMatch("?" + nextTargetKey + "_s", indent + "\t\t\t\t"));
					expandItemWhere.append(indent).append("\t\t\t}");

					if ((expandItem.getTopOption() != null)) {
						expandItemWhere.append(" LIMIT " + expandItem.getTopOption().getValue());
					} else if ((expandItem.getSelectOption() != null) && (expandItem.getTopOption() == null)) {
						// Fixes #176 
						//						if(this.rdfModel.getRdfRepository().getExpandOrderbyDefault()) {
						//							expandItemWhere.append(" ORDER BY ?"+  nextTargetKey + "_s" );
						//						}
						//						if(this.rdfModel.getRdfRepository().getExpandTopDefault()!= null) {
						//							expandItemWhere.append(" LIMIT "+ this.rdfModel.getRdfRepository().getExpandTopDefault());
						//						}
						//						if(this.rdfModel.getRdfRepository().getExpandSkipDefault()!= null) {
						//							expandItemWhere.append(" OFFSET "+ this.rdfModel.getRdfRepository().getExpandSkipDefault());
						//						}

					} else if ((expandItem.getSelectOption() == null) && (expandItem.getCountOption() != null)
							&& (expandItem.getCountOption().getValue())) {
						// Fixes #78 by setting limit even if $top not specified, as it cannot be in OpenUI5.
						expandItemWhere.append(" LIMIT 0");
					}
					if ((expandItem.getSkipOption() != null)) {
						expandItemWhere.append(" OFFSET " + expandItem.getSkipOption().getValue());
					}
					expandItemWhere.append("\n" + indent).append("\t\t}\n");
					expandItemWhere.append(clausesSelect);
				}
				expandItemWhere.append(indent).append("\t}\n");
			}
		}

		if ((expandItem.getCountOption() != null) && expandItem.getCountOption().getValue()) {
			if (expandItem.getSelectOption() != null)
				expandItemWhere.append(indent).append("\tUNION\n");
			expandItemWhere.append(expandItemWhereCount(targetEntityType, targetKey, indent, expandItem, navProperty,
					nextTargetKey, nextTargetEntityType));
			expandItemWhere.append(indent).append("}\n");
		} else {
			expandItemWhere.append(indent).append("}\n");
		}
		if ((expandItem.getExpandOption() != null) && (expandItem.getExpandOption().getExpandItems().size() > 0)) {
			expandItemWhere.append(expandItemsWhere(nextTargetEntityType, nextTargetKey,
					expandItem.getExpandOption().getExpandItems(), indent + "\t",false));
			expandItemWhere.append(indent).append("}\n");
		}
		if (navProperty.getDomainClass().isProxy()) {
			expandItemWhere.append(indent).append("}}\n");
		}
			expandItemWhere.append(indent).append("}\n");
		return expandItemWhere;
	}

	public static boolean isImplicitNavigationProperty(RdfNavigationProperty navProperty) {
		String[] implicitNavigationProperties = new String[] { RdfConstants.RDF_HASFACTS_LABEL,
				RdfConstants.RDF_HASPREDICATE_LABEL, RdfConstants.RDF_HASVALUES_LABEL,
				RdfConstants.RDF_HASOBJECTVALUE_LABEL, RdfConstants.RDF_ISOBJECTIVE_LABEL,
				RdfConstants.RDF_ISPREDICATEOF_LABEL, RdfConstants.RDF_HASSUBJECTS_LABEL };
		return Arrays.asList(implicitNavigationProperties).contains(navProperty.getEDMNavigationPropertyName());
	}

	private static boolean isImplicitEntityType(EdmEntityType edmEntityType) {
		String[] implicitEntityTypes = new String[] { RdfConstants.RDF_OBJECTPREDICATE_LABEL,
				RdfConstants.RDF_SUBJECTPREDICATE_LABEL, RdfConstants.RDF_VALUE_LABEL, RdfConstants.RDFS_RESOURCE_LABEL };
		return Arrays.asList(implicitEntityTypes).contains(edmEntityType.getName());
	}

	public static boolean isImplicitEntityType(RdfEntityType rdfEntityType) {
		String[] implicitEntityTypes = new String[] { RdfConstants.RDF_OBJECTPREDICATE_LABEL,
				RdfConstants.RDF_SUBJECTPREDICATE_LABEL, RdfConstants.RDF_VALUE_LABEL, RdfConstants.RDFS_RESOURCE_LABEL  };
		return Arrays.asList(implicitEntityTypes).contains(rdfEntityType.getEntityTypeName());
	}

	private StringBuilder expandImplicit(String targetKey, RdfNavigationProperty navProperty, String indent,
			StringBuilder clausesSelect) {
		StringBuilder expandImplicit = null;
		switch (navProperty.getEDMNavigationPropertyName()) {
		case RdfConstants.RDF_HASFACTS_LABEL:
			return expandItemWhereHasFacts(targetKey, indent);
		case RdfConstants.RDF_HASPREDICATE_LABEL:
			return expandItemWhereHasPredicate(targetKey, indent, clausesSelect);
		case RdfConstants.RDF_HASVALUES_LABEL:
			return expandItemWhereHasValues(targetKey, indent);
		case RdfConstants.RDF_HASOBJECTVALUE_LABEL:
			return expandItemWhereHasObjectValue(targetKey, indent, clausesSelect);
		case RdfConstants.RDF_ISOBJECTIVE_LABEL:
			return expandItemWhereIsObjective(targetKey, indent);
		case RdfConstants.RDF_ISPREDICATEOF_LABEL:
			return expandItemWhereIsPredicateOf(targetKey, indent, clausesSelect);
		case RdfConstants.RDF_HASSUBJECTS_LABEL:
			return expandItemWhereHasSubjects(targetKey, indent, clausesSelect);
		default:
			break;
		}
		return expandImplicit;
	}

	private StringBuilder expandItemWhereHasFacts(String targetKey, String indent) {
		StringBuilder expandItemWhereHasFacts = new StringBuilder();
		//  	?Customer_s ?Customerrdf_hasFacts_ap ?Customerrdf_hasFacts_ao . 
		//  	BIND( IRI(CONCAT(STR(?Customer_s),"-",STR(?Customerrdf_hasFacts_ap))) as ?Customerrdf_hasFacts_s )
		expandItemWhereHasFacts.append(indent).append("?").append(targetKey).append("_s ?").append(targetKey)
				.append(RdfConstants.RDF_HASFACTS_LABEL).append("_ap ?").append(targetKey)
				.append(RdfConstants.RDF_HASFACTS_LABEL).append("_ao .\n");
		//		expandItemWhereHasFacts.append(indent).append("BIND( IRI(CONCAT(\"").append(RdfConstants.ANONNODE)
		//				.append("\",MD5(STR(?").append(targetKey).append("_s)),\"-\",MD5(STR(?").append(targetKey)
		//				.append(RdfConstants.RDF_HASFACTS_LABEL).append("_ap)))) as ?").append(targetKey)
		//				.append(RdfConstants.RDF_HASFACTS_LABEL).append("_s )\n");

		expandItemWhereHasFacts.append(indent).append("BIND( IRI(CONCAT(STR(?").append(targetKey)
				.append("_s),\"-\",MD5(STR(?").append(targetKey).append(RdfConstants.RDF_HASFACTS_LABEL)
				.append("_ap)))) as ?").append(targetKey).append(RdfConstants.RDF_HASFACTS_LABEL).append("_s )\n");
		return expandItemWhereHasFacts;
	}

	private StringBuilder expandItemWhereHasPredicate(String targetKey, String indent, StringBuilder clausesSelect) {
		StringBuilder expandItemWhereHasPredicate = new StringBuilder();
		//		BIND( ?Customerrdf_hasFacts_ap    as ?Customerrdf_hasFactsrdf_hasPredicate_s  )
		//		VALUES(?Customerrdf_hasFactsrdf_hasPredicate_p){(<http://www.w3.org/2000/01/rdf-schema#label>)(<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)}
		//		OPTIONAL{ ?Customerrdf_hasFactsrdf_hasPredicate_s ?Customerrdf_hasFactsrdf_hasPredicate_p ?Customerrdf_hasFactsrdf_hasPredicate_ao .}
		//		BIND(COALESCE(?Factrdf_property_ao,  IF(?Factrdf_property_p = <http://www.w3.org/2000/01/rdf-schema#label>, STRAFTER(STR(?Factrdf_property_s),(replace(str(?Factrdf_property_s), "(.+[/#])(.+)$", "$1"))),IF(?Factrdf_property_p = <http://www.w3.org/2000/01/rdf-schema#type>,<http://www.w3.org/2000/01/rdf-schema#Property>,"" )    )) as ?Factrdf_property_o)
		expandItemWhereHasPredicate.append(indent).append("BIND( ?").append(targetKey).append("_ap as ?")
				.append(targetKey).append(RdfConstants.RDF_HASPREDICATE_LABEL).append("_s  )\n");
		expandItemWhereHasPredicate.append(clausesSelect.substring(0, clausesSelect.indexOf("}") + 1)).append("\n");
		expandItemWhereHasPredicate.append(indent).append("\tOPTIONAL{?").append(targetKey)
				.append(RdfConstants.RDF_HASPREDICATE_LABEL).append("_s ?").append(targetKey)
				.append(RdfConstants.RDF_HASPREDICATE_LABEL).append("_p ?").append(targetKey)
				.append(RdfConstants.RDF_HASPREDICATE_LABEL).append("_ao }\n");
		//Special case to ensure, even when RDF and RDFS are not included, identities of properties are available.
		expandItemWhereHasPredicate.append(indent).append("\tBIND(COALESCE(?").append(targetKey)
				.append(RdfConstants.RDF_HASPREDICATE_LABEL).append("_ao,IF(?").append(targetKey)
				.append(RdfConstants.RDF_HASPREDICATE_LABEL)
				.append("_p = <http://www.w3.org/2000/01/rdf-schema#label>,STRAFTER(STR(?").append(targetKey)
				.append(RdfConstants.RDF_HASPREDICATE_LABEL).append("_s),(REPLACE(STR(?").append(targetKey)
				.append(RdfConstants.RDF_HASPREDICATE_LABEL).append("_s), \"(.+[/#])(.+)$\", \"$1\"))),IF(?")
				.append(targetKey).append(RdfConstants.RDF_HASPREDICATE_LABEL)
				.append("_p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>,<http://www.w3.org/1999/02/22-rdf-syntax-ns#Property>,\"\" ))) as ?")
				.append(targetKey).append(RdfConstants.RDF_HASPREDICATE_LABEL).append("_o)\n");
		//expandItemWhereHasPredicate.append(indent).append("\t}\n");
		return expandItemWhereHasPredicate;
	}

	private StringBuilder expandItemWhereHasValues(String targetKey, String indent) {
		StringBuilder expandItemWhereHasValues = new StringBuilder();
		//      BIND( IRI(CONCAT(STR(?Customerrdf_hasFacts_s),"-", MD5(STR(?Customerrdf_hasFacts_ao) ))) as ?Customerrdf_hasFactsrdf_hasValues_s)  
		//		BIND( ?Customerrdf_hasFacts_ao  as ?Customerrdf_hasFactsrdf_hasValues_o )
		//  	BIND( if(IsLiteral(?Customerrdf_hasFacts_ao), <http://www.w3.org/1999/02/22-rdf-syntax-ns#rdf_hasDataValue>,<http://www.w3.org/1999/02/22-rdf-syntax-ns#hasObjectValue>) as ?Customerrdf_hasFactsrdf_hasValues_p )	
		expandItemWhereHasValues.append(indent).append("BIND( IRI(CONCAT(STR(?").append(targetKey)
				.append("_s),\"-\", MD5(STR(?").append(targetKey).append("_ao) ))) as ?").append(targetKey)
				.append(RdfConstants.RDF_HASVALUES_LABEL).append("_s)\n");
		expandItemWhereHasValues.append(indent).append("BIND( ?").append(targetKey).append("_ao  as ?")
				.append(targetKey).append(RdfConstants.RDF_HASVALUES_LABEL).append("_o )\n");
		expandItemWhereHasValues.append(indent).append("{\n");
		expandItemWhereHasValues.append(indent).append("\tBIND( if(IsLiteral(?").append(targetKey).append("_ao), <")
				.append(RdfConstants.RDF_HASDATAVALUE).append(">,<").append(RdfConstants.RDF_HASOBJECTVALUE)
				.append(">) as ?").append(targetKey).append(RdfConstants.RDF_HASVALUES_LABEL).append("_p )\n");
		expandItemWhereHasValues.append(indent).append("}UNION{\n");
		expandItemWhereHasValues.append(indent).append("\tBIND( <").append(RdfConstants.RDF_OBJECTVALUE)
				.append("> as ?").append(targetKey).append(RdfConstants.RDF_HASVALUES_LABEL).append("_p )\n");
		expandItemWhereHasValues.append(indent).append("}\n");
		return expandItemWhereHasValues;
	}

	private StringBuilder expandItemWhereHasObjectValue(String targetKey, String indent, StringBuilder clausesSelect) {
		StringBuilder expandItemWhereHasObjectValue = new StringBuilder();
		//		BIND(?Customerrdf_hasFactsrdf_hasValues_o  as ?Customerrdf_hasFactsrdf_hasValuesrdf_hasObjectValue_s  )
		//		VALUES(?Customerrdf_hasFactsrdf_hasValuesrdf_hasObjectValue_p){(<http://www.w3.org/2000/01/rdf-schema#label>)(<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)}
		//		?Customerrdf_hasFactsrdf_hasValuesrdf_hasObjectValue_s ?Customerrdf_hasFactsrdf_hasValuesrdf_hasObjectValue_p ?Customerrdf_hasFactsrdf_hasValuesrdf_hasObjectValue_o .	
		expandItemWhereHasObjectValue.append(indent).append("BIND( ?").append(targetKey).append("_o  as ?")
				.append(targetKey).append(RdfConstants.RDF_HASOBJECTVALUE_LABEL).append("_s  )\n");
		expandItemWhereHasObjectValue.append("\t").append(clausesSelect);
		return expandItemWhereHasObjectValue;
	}

	private StringBuilder expandItemWhereIsObjective(String targetKey, String indent) {
		StringBuilder expandItemWhereIsObjective = new StringBuilder();
		//		?Customerrdf_isObjective_as ?Customerrdf_isObjective_ap  ?Customer_s   .
		//		BIND( IRI(CONCAT(STR(?Customerrdf_isObjective_ap ),"-",STR(?Customer_s))) as ?Customerrdf_isObjective_s )
		expandItemWhereIsObjective.append(indent).append("?").append(targetKey)
				.append(RdfConstants.RDF_ISOBJECTIVE_LABEL).append("_as ?").append(targetKey)
				.append(RdfConstants.RDF_ISOBJECTIVE_LABEL).append("_ap  ?").append(targetKey).append("_s .\n");
		expandItemWhereIsObjective.append(indent).append("BIND( IRI(CONCAT(STR(?").append(targetKey)
				.append(RdfConstants.RDF_ISOBJECTIVE_LABEL).append("_ap ),\"-\",MD5(STR(?").append(targetKey)
				.append("_s)))) as ?").append(targetKey).append(RdfConstants.RDF_ISOBJECTIVE_LABEL).append("_s )\n");
		return expandItemWhereIsObjective;
	}

	private StringBuilder expandItemWhereIsPredicateOf(String targetKey, String indent, StringBuilder clausesSelect) {
		StringBuilder expandItemWhereIsPredicateOf = new StringBuilder();
		//		BIND(?Customerrdf_isObjective_ap  as    ?Customerrdf_isObjectiverdf_isPredicateOf_s)    
		//		VALUES(?Customerrdf_isObjectiverdf_isPredicateOf_p){(<http://www.w3.org/2000/01/rdf-schema#comment>)(<http://www.w3.org/1999/02/22-rdf-syntax-ns#subjectId>)(<http://www.w3.org/2000/01/rdf-schema#label>)}
		//		?Customerrdf_isObjectiverdf_isPredicateOf_s ?Customerrdf_isObjectiverdf_isPredicateOf_p ?Customerrdf_isObjectiverdf_isPredicateOf_o .	
		expandItemWhereIsPredicateOf.append(indent).append("BIND(?").append(targetKey).append("_ap  as ?")
				.append(targetKey).append(RdfConstants.RDF_ISPREDICATEOF_LABEL).append("_s)\n");
		expandItemWhereIsPredicateOf.append(clausesSelect);
		return expandItemWhereIsPredicateOf;
	}

	private StringBuilder expandItemWhereHasSubjects(String targetKey, String indent, StringBuilder clausesSelect) {
		StringBuilder expandItemWhereHasSubjects = new StringBuilder();
		//  	BIND(?Customerrdf_isObjective_as  as   ?Customerrdf_isObjectiverdf_hasSubjects_s )
		//		VALUES(?Customerrdf_isObjectiverdf_hasSubjects_p){(<http://www.w3.org/2000/01/rdf-schema#comment>)(<http://www.w3.org/1999/02/22-rdf-syntax-ns#subjectId>)(<http://www.w3.org/2000/01/rdf-schema#label>)}
		//		?Customerrdf_isObjectiverdf_hasSubjects_s ?Customerrdf_isObjectiverdf_hasSubjects_p ?Customerrdf_isObjectiverdf_hasSubjects_o .
		expandItemWhereHasSubjects.append(indent).append("BIND(?").append(targetKey).append("_as  as ?")
				.append(targetKey).append(RdfConstants.RDF_HASSUBJECTS_LABEL).append("_s )\n");
		expandItemWhereHasSubjects.append(clausesSelect);
		return expandItemWhereHasSubjects;
	}

	private StringBuilder expandItemWhereCount(RdfEntityType targetEntityType, String targetKey, String indent,
			ExpandItem expandItem, RdfNavigationProperty navProperty, String nextTargetKey,
			RdfEntityType nextTargetEntityType)
			throws OData2SparqlException, ODataApplicationException, ExpressionVisitException {

		StringBuilder expandItemWhereCount = new StringBuilder();
		expandItemWhereCount.append(indent).append("\t#expandItemWhereCount\n");
		//TODO UNION or OPTIONAL, currently OPTIONAL improves performance #174 suggest neither!!
		//		expandItemWhereCount.append(indent).append("\tUNION");
		expandItemWhereCount.append(indent).append("\t{ SELECT ?").append(targetKey)
				.append("_s (COUNT(DISTINCT ?" + nextTargetKey + "_s) as ?" + nextTargetKey + "_count)\n")
				.append(indent).append("\t\t{\n");
		// Not optional if filter imposed on path but should really be equality like filters, not negated filters
		//SparqlExpressionVisitor expandFilterClause;

		if (navProperty.getDomainClass().isOperation()) {
			for (RdfProperty property : navProperty.getDomainClass().getProperties()) {
				if (property.getPropertyTypeName().equals(navProperty.getRangeClass().getURL()))
					expandItemWhereCount.append(indent)
							.append("\t\tBIND(?" + property.getVarName() + " AS ?" + nextTargetKey + "_s)\n");
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
				expandItemWhereCount.append(indent).append("\t\t\t{\n");
				expandItemWhereCount.append(indent).append("\t\t\t\t").append(
						nextKeyVar + " <" + navProperty.getInversePropertyOf().getIRI() + "> " + keyvar + " .\n");
				expandItemWhereCount.append(indent).append("\t\t\t").append("}UNION{\n");
				expandItemWhereCount.append(indent).append("\t\t\t\t")
						.append(keyvar + " <" + navProperty.getNavigationPropertyIRI() + "> " + nextKeyVar + " .\n");
				expandItemWhereCount.append(indent).append("\t\t\t").append("}\n");

			} else {
				expandItemWhereCount.append(indent).append("\t\t")
						.append(keyvar + " <" + navProperty.getNavigationPropertyIRI() + "> " + nextKeyVar + " .\n");
			}
			if (this.rdfModel.getRdfRepository().isWithMatching())
				expandItemWhereCount.append(clausesMatch("?" + nextTargetKey + "_s", nextKeyVar, indent + "\t\t"));
		}
		expandItemWhereCount.append(indent).append("\t\t} GROUP BY ?").append(targetKey).append("_s\n").append(indent)
				.append("\t}\n");
		return expandItemWhereCount;
	}

	private StringBuilder clausesSelect(TreeSet<String> selectPropertyMap, String nextTargetKey, String navPath,
			RdfEntityType targetEntityType, String indent, boolean includeSubjectid) {
		StringBuilder clausesSelect = new StringBuilder();
		Boolean hasProperties = false;
		// Case URI5 need to fetch only one property as given in resourceParts
		if (navPath.equals(nextTargetKey) || this.filterClause.getNavPropertyPropertyFilters().containsKey(navPath)) {
		} else {
			clausesSelect.append("OPTIONAL");
		}
		//Fixes #178
		//clausesSelect.append(indent).append("{\n");
		if (selectPropertyMap != null && !selectPropertyMap.isEmpty()) {
			clausesSelect.append(indent).append("\tVALUES(?" + nextTargetKey + "_p){");
			selectPropertyMap.add(RdfConstants.RDF_TYPE);
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
		if (targetEntityType.getEntityTypeName().equals(RdfConstants.RDF_VALUE_LABEL)) {
			clausesSelect.append(indent).append("\tBIND( if(IsLiteral(?").append(nextTargetKey).append("_o), ?")
					.append(nextTargetKey).append("_o,\"\") as ?").append(nextTargetKey).append("_literal)\n");
			clausesSelect.append(indent).append("\tBIND( if(!IsLiteral(?").append(nextTargetKey).append("_o), ?")
					.append(nextTargetKey).append("_o,\"\") as ?").append(nextTargetKey).append("_resource)\n");
		}
		//Fixes #178
		//		if(includeSubjectid ){//!isImplicitEntityType(edmTargetEntitySet.getEntityType())) {
		//			clausesSelect.append(indent).append("} UNION {\n");
		//			clausesSelect.append(indent).append("\tBIND(<" + RdfConstants.RDF_SUBJECT + ">  as ?" + nextTargetKey + "_p )\n");
		//			clausesSelect.append(indent).append("\tBIND(?" + nextTargetKey + "_s as ?" + nextTargetKey + "_o )\n");
		//		}
		//clausesSelect.append(indent).append("}\n");
		if (hasProperties)
			return clausesSelect;
		else
			return new StringBuilder();
	}

	private StringBuilder complexProperties(RdfModel.RdfProperty selectProperty) {
		StringBuilder complexProperties = new StringBuilder();
		if (selectProperty != null) {
			for (RdfProperty complexProperty : selectProperty.getComplexType().getProperties().values()) {
				if (complexProperty.getIsComplex()) {
					complexProperties.append(complexProperties(complexProperty));
				} else {
					complexProperties.append("(<" + complexProperty.getPropertyURI() + ">)");
				}
			}
			for (RdfNavigationProperty complexNavigationProperty : selectProperty.getComplexType()
					.getNavigationProperties().values()) {
				complexProperties.append("(<" + complexNavigationProperty.getNavigationPropertyIRI() + ">)");
			}
			for (RdfShapedNavigationProperty complexShapedNavigationProperty : selectProperty.getComplexType()
					.getShapedNavigationProperties().values()) {
				complexProperties.append("(<"
						+ complexShapedNavigationProperty.getRdfNavigationProperty().getNavigationPropertyIRI() + ">)");
			}
		}
		return complexProperties;
	}

	private TreeSet<String> complexPropertiesSet(RdfModel.RdfProperty selectProperty) {
		TreeSet<String> complexProperties = new TreeSet<String>();
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
		if (defaultLimit > 0)
			defaultLimitClause.append(" LIMIT ").append(defaultLimit);
		return defaultLimitClause;
	}

	private TreeSet<String> createSelectPropertyMap(RdfEntityType entityType, SelectOption selectOption)
			throws EdmException {
		// Align variables
		//RdfEntityType entityType = rdfTargetEntityType;
		//String key = entityType.entityTypeName;
		TreeSet<String> valueProperties = new TreeSet<String>();
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
						//Need to include this to ensure that if the entity has no actual properties whatsoever then something is returned.
						valueProperties.add(RdfConstants.RDF_TYPE);
						for (RdfProperty rdfProperty : segmentEntityType.getInheritedProperties()) {//getProperties()) {
							if (rdfProperty.getPropertyURI() != null) {
								valueProperties.add(rdfProperty.getPropertyURI());
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
											.findNavigationPropertyByEDMNavigationPropertyName(segmentName) == null) {
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
			} else {
				valueProperties.add(RdfConstants.RDF_TYPE);
				return null;// valueProperties;
			}
		}
		//return null;
	}

	public Boolean isPrimitiveValue() {
		return isPrimitiveValue;
	}

	public void setIsPrimitiveValue(Boolean isPrimitiveValue) {
		this.isPrimitiveValue = isPrimitiveValue;
	}

	public SparqlStatement prepareEntityLinksSparql()
			throws EdmException, ODataApplicationException, OData2SparqlException {
		String expandedKey = rdfResourceParts.getValidatedSubjectIdUrl();
		String expandedProperty;
		StringBuilder sparql = new StringBuilder();

		if (!rdfResourceParts.getUriType().equals(UriType.URI2)) {
			String key = rdfEntityType.entityTypeName;
			String targetKey;
			if (!rdfResourceParts.getUriType().equals(UriType.URI7B)) {
				if (rdfResourceParts.getValidatedTargetSubjectIdUrl() != null) {
					targetKey = "<" + rdfResourceParts.getValidatedTargetSubjectIdUrl() + ">";
				} else {
					targetKey = "?" + key + "_o";
				}
			} else {
				targetKey = "?" + key + "_o";
			}
			sparql.append("CONSTRUCT { " + targetKey + "  <" + RdfConstants.TARGETENTITY + "><"
					+ rdfResourceParts.getResponseRdfEntityType().getURL() + "> ;");
			RdfNavigationProperty rdfNavigationProperty = rdfResourceParts.getLastNavProperty();
			expandedProperty = rdfNavigationProperty.getNavigationPropertyIRI();

			sparql.append("<" + RdfConstants.ASSERTEDTYPE + "> <" + rdfResourceParts.getResponseRdfEntityType().getURL()
					+ "> .}\n");
			sparql.append("WHERE { { <" + expandedKey + ">  <" + expandedProperty + "> " + targetKey + " .}");
			if (rdfNavigationProperty.IsInverse()) {
				String expandedInverseProperty = rdfNavigationProperty.getInversePropertyOfURI().toString();
				sparql.append(" \nUNION { " + targetKey + " <" + expandedInverseProperty + "> <" + expandedKey + ">.}");
			}
			sparql.append("}");
		} else {
			sparql.append("CONSTRUCT { <" + expandedKey + ">  <" + RdfConstants.TARGETENTITY + "><"
					+ rdfResourceParts.getResponseRdfEntityType().getURL() + "> ;");
			sparql.append("<" + RdfConstants.ASSERTEDTYPE + "> <" + rdfResourceParts.getResponseRdfEntityType().getURL()
					+ "> .}WHERE{}\n");
		}

		return new SparqlStatement(sparql.toString());
	}

	public void addProxiedRdfEdmProvider(String proxyDataset, RdfEdmProvider proxiedRdfEdmProvider) {
		if (!this.proxiedRdfEdmProviders.containsKey(proxyDataset)) {
			this.proxiedRdfEdmProviders.put(proxyDataset, proxiedRdfEdmProvider);
		}
	}

	public StringBuilder sparqlPrefixes() {
		StringBuilder sparqlPrefixes = new StringBuilder();

		TreeMap<String, String> modelPrefixes = (TreeMap<String, String>) this.rdfModel.getRdfPrefixes().getPrefixes();
		@SuppressWarnings("unchecked")
		TreeMap<String, String> corePrefixes = (TreeMap<String, String>) modelPrefixes.clone();

		for (Map.Entry<String, String> prefixEntry : corePrefixes.entrySet()) {

			String prefix = prefixEntry.getKey();
			String url = prefixEntry.getValue();
			sparqlPrefixes.append("PREFIX ").append(prefix).append(": <").append(url).append(">\n");
		}
		return sparqlPrefixes;
	}
}