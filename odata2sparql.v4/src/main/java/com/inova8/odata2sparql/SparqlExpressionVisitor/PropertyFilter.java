package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.HashSet;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;

public class PropertyFilter {
	private final RdfProperty property;
	private final HashSet<String> filters;

	PropertyFilter(RdfProperty property) {
		this.property = property;
		filters = new HashSet<String>();
	}

	public RdfProperty getProperty() {
		return property;
	}

	public HashSet<String> getFilters() {
		return filters;
	}
}
