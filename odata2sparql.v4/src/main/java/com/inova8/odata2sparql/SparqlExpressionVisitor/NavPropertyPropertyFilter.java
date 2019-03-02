package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.TreeMap;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;

public class NavPropertyPropertyFilter {
	private final RdfNavigationProperty navProperty;

	private final TreeMap<String, PropertyFilter> propertyFilters;
//	@Deprecated
//	public NavPropertyPropertyFilter(RdfAssociation navProperty) {
//		this.navProperty = navProperty;
//		propertyFilters = new TreeMap<String, PropertyFilter>();
//	}
	public NavPropertyPropertyFilter() {
		propertyFilters = new TreeMap<String, PropertyFilter>();
		this.navProperty = null;
	}
	public RdfNavigationProperty getNavProperty() {
		return navProperty;
	}

	public TreeMap<String, PropertyFilter> getPropertyFilters() {
		return propertyFilters;
	}

}
