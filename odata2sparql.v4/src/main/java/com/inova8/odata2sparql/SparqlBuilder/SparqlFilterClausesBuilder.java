/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlBuilder;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.SparqlExpressionVisitor.NavPropertyPropertyFilter;
import com.inova8.odata2sparql.SparqlExpressionVisitor.PropertyFilter;
import com.inova8.odata2sparql.SparqlExpressionVisitor.SparqlExpressionVisitor;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;

/**
 * The Class SparqlFilterClausesBuilder.
 */
public class SparqlFilterClausesBuilder {

	/** The expand item variables. */
	private StringBuilder expandItemVariables = new StringBuilder("");
	
	/** The clauses expand filter. */
	private StringBuilder clausesExpandFilter = new StringBuilder("");
	
	/** The clauses expand filter necessary. */
	private Boolean clausesExpandFilterNecessary = false;
	
	/** The filter. */
	private StringBuilder filter = new StringBuilder("");
	
	/** The filter clause. */
	private SparqlExpressionVisitor filterClause;

	/** The uri type. */
	private final UriType uriType;
	
	/** The rdf model to metadata. */
	private final RdfModelToMetadata rdfModelToMetadata;
	
	/** The rdf model. */
	private final RdfModel rdfModel;
	
	/** The edm entity set. */
	private EdmEntitySet edmEntitySet;
	
	/** The rdf entity type. */
	private RdfEntityType rdfEntityType;
	
	/** The edm target entity set. */
	private EdmEntitySet edmTargetEntitySet;
	
	/** The rdf target entity type. */
	private RdfEntityType rdfTargetEntityType;

	/**
	 * Instantiates a new sparql filter clauses builder.
	 *
	 * @param rdfModel the rdf model
	 * @param rdfModelToMetadata the rdf model to metadata
	 * @param uriInfo the uri info
	 * @param uriType the uri type
	 * @param rdfResourceParts the rdf resource parts
	 * @throws ODataApplicationException the o data application exception
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws EdmException the edm exception
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public SparqlFilterClausesBuilder(RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, UriInfo uriInfo,
			UriType uriType,RdfResourceParts  rdfResourceParts)
			throws ODataApplicationException, ExpressionVisitException, EdmException, OData2SparqlException {
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.uriType =rdfResourceParts.getUriType();// uriType;
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		this.edmEntitySet = rdfResourceParts.getEntitySet().getEdmEntitySet(); 
		this.rdfEntityType = rdfResourceParts.getEntitySet().getRdfEntityType();
		// By default
		this.edmTargetEntitySet = edmEntitySet;
		this.rdfTargetEntityType = rdfEntityType;
		
		UriResource lastSegment;
		if(uriInfo.getFilterOption()!=null) 
		{
		switch (this.uriType) {
		case URI1: {
			filterClause = filterClause(uriInfo.getFilterOption(), rdfEntityType, "");
		}
			break;
		case URI2: {
		}
			break;
		case URI5: {
		}
			break;
		case URI6A: {
		}
			break;
		case URI6B: {
			lastSegment = resourceParts.get(resourceParts.size() - 1);
			
			if (lastSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();			
				if(resourceParts.size()>2 ) {
					//could be a complexType
					 UriResource penultimateSegment = resourceParts.get(resourceParts.size() - 2);
					 if( penultimateSegment.getKind().equals(UriResourceKind.complexProperty)) {
						 //Complextype with navigation property
						 UriResourceComplexProperty complexProperty = ((UriResourceComplexProperty) penultimateSegment);
						 edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, complexProperty.getComplexType(), edmNavigationProperty);
					 }else {
						edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
					 }
				}else {
					edmTargetEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);
				}
				rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
				filterClause = filterClause(uriInfo.getFilterOption(), rdfTargetEntityType, "");
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
				filterClause = filterClause(uriInfo.getFilterOption(), rdfTargetEntityType, "");
			} else {
				filterClause = filterClause(uriInfo.getFilterOption(), rdfEntityType, "");
			}
		}
			break;
		case URI16: {
		}
		default:
		}
		}
		filter = uriInfo.getFilterOption() != null ? new StringBuilder(filterClause.getFilterClause()) : filter;
		if (uriInfo.getExpandOption() != null)
			expandItems(rdfTargetEntityType, rdfTargetEntityType.entityTypeName,
					uriInfo.getExpandOption().getExpandItems(), "#indent");
	}

	/**
	 * Gets the expand item variables.
	 *
	 * @return the expand item variables
	 */
	public StringBuilder getExpandItemVariables() {
		return expandItemVariables;
	}

	/**
	 * Gets the filter.
	 *
	 * @return the filter
	 */
	public StringBuilder getFilter() {
		return filter;
	}

	/**
	 * Gets the clauses filter.
	 *
	 * @param indent the indent
	 * @return the clauses filter
	 */
	public StringBuilder getClausesFilter(String indent) {
		//return clausesFilter(null, rdfTargetEntityType.entityTypeName, indent, this.filterClause.getNavPropertyPropertyFilters().get(rdfTargetEntityType.entityTypeName).getPropertyFilters());
		StringBuilder clausesFilter = new StringBuilder();
		clausesFilter.append(indent).append("{\n");
		TreeMap<String, NavPropertyPropertyFilter> navPropertyPropertyFilters = this.filterClause.getNavPropertyPropertyFilters();
		for(NavPropertyPropertyFilter navPropertyPropertyFilter:  navPropertyPropertyFilters.values()) {
			clausesFilter.append(clausesFilter(null, rdfTargetEntityType.entityTypeName, indent, navPropertyPropertyFilter.getPropertyFilters()));			
		}
		clausesFilter.append(indent).append("}\n");
		return clausesFilter;
	}

	/**
	 * Gets the clauses expand filter.
	 *
	 * @param indent the indent
	 * @return the clauses expand filter
	 */
	public StringBuilder getClausesExpandFilter(String indent) {
		if(clausesExpandFilterNecessary) {
		return new StringBuilder(clausesExpandFilter.toString().replaceAll("#indent", indent));
		}else {
			return new StringBuilder("");
		}
	}

	/**
	 * Gets the filter clause.
	 *
	 * @return the filter clause
	 */
	public SparqlExpressionVisitor getFilterClause() {
		return filterClause;
	}

	/**
	 * Filter clause.
	 *
	 * @param filter the filter
	 * @param entityType the entity type
	 * @param nextTargetKey the next target key
	 * @return the sparql expression visitor
	 * @throws ODataApplicationException the o data application exception
	 * @throws ExpressionVisitException the expression visit exception
	 */
	private SparqlExpressionVisitor filterClause(FilterOption filter, RdfEntityType entityType, String nextTargetKey)
			throws ODataApplicationException, ExpressionVisitException {
		SparqlExpressionVisitor sparqlExpressionVisitor = new SparqlExpressionVisitor(rdfModel, rdfModelToMetadata,
				entityType, nextTargetKey);
		if (filter != null) {
			Expression filterExpression = filter.getExpression();
			final Object visitorResult;
			final String result;
			visitorResult = filterExpression.accept(sparqlExpressionVisitor);
			if(visitorResult==null  ) {
				throw new ExpressionVisitException("No filter created");
			}else {
				result = new String((String) visitorResult);
				sparqlExpressionVisitor.setConditionString(result);				
			}
		}
		return sparqlExpressionVisitor;
	}

	/**
	 * Clauses filter.
	 *
	 * @param expandSelectTreeNodeLinksEntry the expand select tree node links entry
	 * @param nextTargetKey the next target key
	 * @param indent the indent
	 * @param propertyFilters the property filters
	 * @return the string builder
	 */
	private StringBuilder clausesFilter(Entry<String, ExpandOption> expandSelectTreeNodeLinksEntry,
			String nextTargetKey, String indent, TreeMap<String, PropertyFilter> propertyFilters) {
		StringBuilder clausesFilter = new StringBuilder();
		// Repeat for each filtered property associated with this navProperty
		for (Entry<String, PropertyFilter> propertyFilterEntry : propertyFilters.entrySet()) {
			PropertyFilter propertyFilter = propertyFilterEntry.getValue();
			clausesFilter.append(indent).append("\t").append(propertyFilter.getClauseFilter(nextTargetKey));
		}
		return clausesFilter;
	}

	/**
	 * Expand items.
	 *
	 * @param targetEntityType the target entity type
	 * @param targetKey the target key
	 * @param expandItems the expand items
	 * @param indent the indent
	 * @throws EdmException the edm exception
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws ODataApplicationException the o data application exception
	 * @throws ExpressionVisitException the expression visit exception
	 */
	private void expandItems(RdfEntityType targetEntityType, String targetKey, List<ExpandItem> expandItems,
			String indent)
			throws EdmException, OData2SparqlException, ODataApplicationException, ExpressionVisitException {

		for (ExpandItem expandItem : expandItems) {
			if (expandItem.isStar()) {
				//Iterate through all navigation properties of this targetEntityType
				//However cannot further expand or add filter to an anonymous expand so no need to add anything
				/*				
				for (RdfAssociation navigationProperty : targetEntityType.getNavigationProperties()) {
					expandItem(targetKey, indent, expandItem, navigationProperty,
							targetKey + navigationProperty.getAssociationName(), navigationProperty.getRangeClass());
				}
				*/
			} else {
				List<UriResource> resourceParts = expandItem.getResourcePath().getUriResourceParts();
				//Only navigation supported in RDFS+ is one level of complexProperty, hence code is not generic
				UriResource firstResourcePart = resourceParts.get(0);
				UriResourceNavigation resourceNavigation = null;
				if (firstResourcePart instanceof UriResourceNavigation) {
					 resourceNavigation = (UriResourceNavigation) resourceParts.get(0);
				}else if (firstResourcePart instanceof UriResourceComplexProperty ) {
					resourceNavigation = (UriResourceNavigation) resourceParts.get(1);
				}
				RdfNavigationProperty navProperty = rdfModelToMetadata.getMappedNavigationProperty(
						new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(), 
								resourceNavigation.getProperty().getName()));
				String nextTargetKey = targetKey + resourceNavigation.getProperty().getName();
				RdfEntityType nextTargetEntityType = navProperty.getRangeClass();
				//Now do the work
				 if (expandItem.getFilterOption() != null) {
					clausesExpandFilterNecessary = true;
					expandItem(targetKey, indent, expandItem, navProperty, nextTargetKey, nextTargetEntityType);
				 }
				if ((expandItem.getExpandOption() != null)
						&& (expandItem.getExpandOption().getExpandItems().size() > 0)) {
					expandItems(nextTargetEntityType, nextTargetKey, expandItem.getExpandOption().getExpandItems(),
							indent + "\t");
				}
			}
		}
	}

	/**
	 * Expand item.
	 *
	 * @param targetKey the target key
	 * @param indent the indent
	 * @param expandItem the expand item
	 * @param navProperty the nav property
	 * @param nextTargetKey the next target key
	 * @param nextTargetEntityType the next target entity type
	 * @throws ODataApplicationException the o data application exception
	 * @throws ExpressionVisitException the expression visit exception
	 */
	private void expandItem(String targetKey, String indent, ExpandItem expandItem, RdfNavigationProperty navProperty,
			String nextTargetKey, RdfEntityType nextTargetEntityType)
			throws ODataApplicationException, ExpressionVisitException {
		SparqlExpressionVisitor filterClause = filterClause(expandItem.getFilterOption(), nextTargetEntityType,
				nextTargetKey);
		filter.append(filterClause.getFilterClause());

		clausesExpandFilter.append(indent).append("{\n");
		if (SparqlQueryBuilder.isImplicitNavigationProperty(navProperty)) {
			switch (navProperty.getNavigationPropertyLabel()){
			case "rdf_resource":
				clausesExpandFilter.append(indent).append("\t").append("BIND(?Fact").append("_ao as ?").append(nextTargetKey).append("_s )\n");
				break;
			case "rdf_property":
				clausesExpandFilter.append(indent).append("\t").append("BIND(?").append(targetKey).append("_ap as ?").append(nextTargetKey).append("_s )\n");
				break;
			case "rdf_terms":
				clausesExpandFilter.append(indent).append("\t").append("BIND(?Fact").append("_ao as ?").append(nextTargetKey).append("rdf_literal_value )\n");
				break;
			default:
			
			}
			
		}else	if (navProperty.IsInverse()) {	
			clausesExpandFilter.append(indent).append("\t").append("{\n");
			clausesExpandFilter.append(indent).append("\t").append("\t").append("?" + nextTargetKey + "_s <"
					+ navProperty.getInversePropertyOf().getIRI() + "> ?" + targetKey + "_s .\n");
			clausesExpandFilter.append(indent).append("\t").append("}UNION{\n");
			clausesExpandFilter.append(indent).append("\t").append("\t").append(
					"?" + targetKey + "_s <" + navProperty.getNavigationPropertyIRI() + "> ?" + nextTargetKey + "_s .\n");
			clausesExpandFilter.append(indent).append("\t").append("}\n");
		} else {
			clausesExpandFilter.append(indent).append("\t").append(
					"?" + targetKey + "_s <" + navProperty.getNavigationPropertyIRI() + "> ?" + nextTargetKey + "_s .\n");
		}
		//Check if there are any relevant literal filters before proceeding
		if (!filterClause.getNavPropertyPropertyFilters().isEmpty()) {
			//If this is an implicit entityType (rdf core) then no need to add any clauses to the query to find the triples
			if(!SparqlQueryBuilder.isImplicitEntityType(nextTargetEntityType)) {
				TreeMap<String, PropertyFilter> propertyFilters = filterClause.getNavPropertyPropertyFilters()
						.get(nextTargetKey).getPropertyFilters();
				clausesExpandFilter.append(indent).append("\t").append("{\n");
				// Repeat for each filtered property associated with this navProperty
				for (Entry<String, PropertyFilter> propertyFilterEntry : propertyFilters.entrySet()) {
					PropertyFilter propertyFilter = propertyFilterEntry.getValue();
					clausesExpandFilter.append(indent).append("\t").append("\t")
							.append("?" + nextTargetKey + "_s <" + propertyFilter.getProperty().getPropertyURI() + "> ?"
									+ nextTargetKey + propertyFilter.getProperty().getEDMPropertyName() + "_value .\n");
					for (String filter : propertyFilter.getFilters()) {
						clausesExpandFilter.append(indent).append("\t").append("FILTER((?" + filter + "_value))\n");
					}
				}
				clausesExpandFilter.append(indent).append("\t").append("}\n");
			}
		}

		clausesExpandFilter.append(indent).append("}\n");

		expandItemVariables.append(" ?" + nextTargetKey + "_s");
	}

}
