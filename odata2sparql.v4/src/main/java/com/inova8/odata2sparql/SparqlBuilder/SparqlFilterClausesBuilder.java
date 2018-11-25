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
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
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
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.SparqlExpressionVisitor.PropertyFilter;
import com.inova8.odata2sparql.SparqlExpressionVisitor.SparqlExpressionVisitor;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlFilterClausesBuilder {

	private StringBuilder expandItemVariables = new StringBuilder("");
	private StringBuilder clausesExpandFilter = new StringBuilder("");
	private StringBuilder filter = new StringBuilder("");
	private SparqlExpressionVisitor filterClause;

	private final UriType uriType;
	private final RdfModelToMetadata rdfModelToMetadata;
	private final RdfModel rdfModel;
	private EdmEntitySet edmEntitySet;
	private RdfEntityType rdfEntityType;
	private EdmEntitySet edmTargetEntitySet;
	private RdfEntityType rdfTargetEntityType;

	public SparqlFilterClausesBuilder(RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, UriInfo uriInfo,
			UriType uriType)
			throws ODataApplicationException, ExpressionVisitException, EdmException, OData2SparqlException {
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.uriType = uriType;
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourceParts.get(0);
		this.edmEntitySet = uriResourceEntitySet.getEntitySet();
		this.rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		// By default
		this.edmTargetEntitySet = edmEntitySet;
		this.rdfTargetEntityType = rdfEntityType;
		
		UriResource lastSegment;
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
		filter = filterClause != null ? new StringBuilder(filterClause.getFilterClause()) : filter;
		if (uriInfo.getExpandOption() != null)
			expandItems(rdfTargetEntityType, rdfTargetEntityType.entityTypeName,
					uriInfo.getExpandOption().getExpandItems(), "#indent");
	}

	public StringBuilder getExpandItemVariables() {
		return expandItemVariables;
	}

	public StringBuilder getFilter() {
		return filter;
	}

	public StringBuilder getClausesFilter(String indent) {
		return clausesFilter(null, rdfTargetEntityType.entityTypeName, indent, this.filterClause
				.getNavPropertyPropertyFilters().get(rdfTargetEntityType.entityTypeName).getPropertyFilters());
	}

	public StringBuilder getClausesExpandFilter(String indent) {
		return new StringBuilder(clausesExpandFilter.toString().replaceAll("#indent", indent));
	}

	public SparqlExpressionVisitor getFilterClause() {
		return filterClause;
	}

	private SparqlExpressionVisitor filterClause(FilterOption filter, RdfEntityType entityType, String nextTargetKey)
			throws ODataApplicationException, ExpressionVisitException {
		SparqlExpressionVisitor sparqlExpressionVisitor = new SparqlExpressionVisitor(rdfModel, rdfModelToMetadata,
				entityType, nextTargetKey);
		if (filter != null) {
			Expression filterExpression = filter.getExpression();
			final Object visitorResult;
			final String result;
			visitorResult = filterExpression.accept(sparqlExpressionVisitor);
			result = new String((String) visitorResult);
			sparqlExpressionVisitor.setConditionString(result);
		}
		return sparqlExpressionVisitor;
	}

	private StringBuilder clausesFilter(Entry<String, ExpandOption> expandSelectTreeNodeLinksEntry,
			String nextTargetKey, String indent, TreeMap<String, PropertyFilter> propertyFilters) {
		StringBuilder clausesFilter = new StringBuilder();
		clausesFilter.append(indent).append("{\n");
		// Repeat for each filtered property associated with this navProperty
		for (Entry<String, PropertyFilter> propertyFilterEntry : propertyFilters.entrySet()) {
			PropertyFilter propertyFilter = propertyFilterEntry.getValue();
			clausesFilter.append(indent).append("\t")
					.append("?" + nextTargetKey + "_s <" + propertyFilter.getProperty().getPropertyURI() + "> ?"
							+ nextTargetKey + propertyFilter.getProperty().getEDMPropertyName() + "_value .\n");
			for (String filter : propertyFilter.getFilters()) {
				clausesFilter.append(indent).append("\t").append("FILTER((?" + filter + "_value))\n");
			}
		}
		clausesFilter.append(indent).append("}\n");
		return clausesFilter;
	}

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
				RdfAssociation navProperty = rdfModelToMetadata.getMappedNavigationProperty(
						new FullQualifiedName(targetEntityType.getSchema().getSchemaPrefix(), 
								resourceNavigation.getProperty().getName()));
				String nextTargetKey = targetKey + resourceNavigation.getProperty().getName();
				RdfEntityType nextTargetEntityType = navProperty.getRangeClass();
				//Now do the work
				if (expandItem.getFilterOption() != null) {
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

	private void expandItem(String targetKey, String indent, ExpandItem expandItem, RdfAssociation navProperty,
			String nextTargetKey, RdfEntityType nextTargetEntityType)
			throws ODataApplicationException, ExpressionVisitException {
		SparqlExpressionVisitor filterClause = filterClause(expandItem.getFilterOption(), nextTargetEntityType,
				nextTargetKey);
		filter.append(filterClause.getFilterClause());

		clausesExpandFilter.append(indent).append("{\n");

		if (navProperty.IsInverse()) {
//			clausesExpandFilter.append(indent).append("\t").append("?" + nextTargetKey + "_s <"	+ navProperty.getInversePropertyOfURI() + "> ?" + targetKey + "_s .\n");
			
			clausesExpandFilter.append(indent).append("\t").append("{\n");
			clausesExpandFilter.append(indent).append("\t").append("\t").append("?" + nextTargetKey + "_s <"
					+ navProperty.getInversePropertyOf().getIRI() + "> ?" + targetKey + "_s .\n");
			clausesExpandFilter.append(indent).append("\t").append("}UNION{\n");
			clausesExpandFilter.append(indent).append("\t").append("\t").append(
					"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
			clausesExpandFilter.append(indent).append("\t").append("}\n");
		} else {
			clausesExpandFilter.append(indent).append("\t").append(
					"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
		}
		//Check if there are any relevant filters before proceeding
		if (!filterClause.getNavPropertyPropertyFilters().isEmpty()) {
			TreeMap<String, PropertyFilter> propertyFilters = filterClause.getNavPropertyPropertyFilters()
					.get(nextTargetEntityType.entityTypeName).getPropertyFilters();
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

		clausesExpandFilter.append(indent).append("}\n");

		expandItemVariables.append(" ?" + nextTargetKey + "_s");
	}

}
