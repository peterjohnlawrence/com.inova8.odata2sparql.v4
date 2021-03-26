/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.TreeMap;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;

/**
 * The Class NavPropertyPropertyFilter.
 */
public class NavPropertyPropertyFilter {
	
	/** The nav property. */
	private final RdfNavigationProperty navProperty;

	/** The property filters. */
	private final TreeMap<String, PropertyFilter> propertyFilters;
//	@Deprecated
//	public NavPropertyPropertyFilter(RdfAssociation navProperty) {
//		this.navProperty = navProperty;
//		propertyFilters = new TreeMap<String, PropertyFilter>();
/**
 * Instantiates a new nav property property filter.
 */
//	}
	public NavPropertyPropertyFilter() {
		propertyFilters = new TreeMap<String, PropertyFilter>();
		this.navProperty = null;
	}
	
	/**
	 * Gets the nav property.
	 *
	 * @return the nav property
	 */
	public RdfNavigationProperty getNavProperty() {
		return navProperty;
	}

	/**
	 * Gets the property filters.
	 *
	 * @return the property filters
	 */
	public TreeMap<String, PropertyFilter> getPropertyFilters() {
		return propertyFilters;
	}

}
