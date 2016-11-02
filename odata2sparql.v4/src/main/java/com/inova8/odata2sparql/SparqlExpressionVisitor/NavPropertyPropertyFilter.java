package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.HashMap;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;

public class NavPropertyPropertyFilter {
	private final RdfAssociation navProperty;

	private final HashMap<String, PropertyFilter> propertyFilters;
//	@Deprecated
//	public NavPropertyPropertyFilter(RdfAssociation navProperty) {
//		this.navProperty = navProperty;
//		propertyFilters = new HashMap<String, PropertyFilter>();
//	}
	public NavPropertyPropertyFilter() {
		propertyFilters = new HashMap<String, PropertyFilter>();
		this.navProperty = null;
	}
	public RdfAssociation getNavProperty() {
		return navProperty;
	}

	public HashMap<String, PropertyFilter> getPropertyFilters() {
		return propertyFilters;
	}

}
