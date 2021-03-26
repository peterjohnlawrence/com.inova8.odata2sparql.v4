/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.HashSet;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;

/**
 * The Class PropertyFilter.
 */
public class PropertyFilter {
	
	/** The property. */
	private final RdfProperty property;
	
	/** The navigation property. */
	private final RdfNavigationProperty navigationProperty;
	
	/** The target prefix. */
	private final String  targetPrefix;
	
	/** The filters. */
	private final HashSet<String> filters;

	/**
	 * Instantiates a new property filter.
	 *
	 * @param targetPrefix the target prefix
	 * @param property the property
	 */
	PropertyFilter(String  targetPrefix,RdfProperty property) {
		this.navigationProperty = null;
		this.property = property;
		this.targetPrefix = targetPrefix;
		filters = new HashSet<String>();
	}
	
	/**
	 * Instantiates a new property filter.
	 *
	 * @param targetPrefix the target prefix
	 * @param navigationProperty the navigation property
	 */
	PropertyFilter(String  targetPrefix,RdfNavigationProperty navigationProperty) {
		this.navigationProperty = navigationProperty;
		this.property = null;
		this.targetPrefix = targetPrefix;
		filters = new HashSet<String>();
	}
	
	/**
	 * Gets the property.
	 *
	 * @return the property
	 */
	public RdfProperty getProperty() {
		return property;
	}
	
	/**
	 * Gets the navigation property.
	 *
	 * @return the navigation property
	 */
	public RdfNavigationProperty getNavigationProperty() {
		return navigationProperty;
	}
	
	/**
	 * Gets the filters.
	 *
	 * @return the filters
	 */
	public HashSet<String> getFilters() {
		return filters;
	}
	
	/**
	 * Gets the clause filter.
	 *
	 * @param nextTargetKey the next target key
	 * @return the clause filter
	 */
	public StringBuilder getClauseFilter(String nextTargetKey) {
		StringBuilder clauseFilter= new StringBuilder();
		if(this.property != null)
			clauseFilter.append("?" +  targetPrefix + "_s <" + this.getProperty().getPropertyURI() + "> ?"
				+  targetPrefix + this.getProperty().getEDMPropertyName() + "_value .\n");
		else {
			if(this.getNavigationProperty().IsInverse() ) {
				clauseFilter.append("{");
				clauseFilter.append("?" +  targetPrefix + "_s <" + this.getNavigationProperty().getNavigationPropertyIRI() + "> ?"
						+  targetPrefix + this.getNavigationProperty().getNavigationPropertyName() + "_s .");
				clauseFilter.append("} UNION {");
				clauseFilter.append("?" +  targetPrefix + this.getNavigationProperty().getNavigationPropertyName() + "_s <" + this.getNavigationProperty().getInversePropertyOfURI() + "> ?"
						+  targetPrefix + "_s .");
				clauseFilter.append("}\n");
			}else {
				clauseFilter.append("?" +  targetPrefix + "_s <" + this.getNavigationProperty().getNavigationPropertyIRI() + "> ?"
						+  targetPrefix + this.getNavigationProperty().getNavigationPropertyName() + "_s .\n");
			}
		}
		for (String filter : this.getFilters()) {
			clauseFilter.append("\t").append("FILTER((?" + filter + "_value))\n");
		}		
		return 	clauseFilter;	
	}
}
