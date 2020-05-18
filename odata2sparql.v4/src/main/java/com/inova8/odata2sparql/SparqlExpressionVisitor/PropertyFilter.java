package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.HashSet;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;

public class PropertyFilter {
	private final RdfProperty property;
	private final RdfNavigationProperty navigationProperty;
	private final String  targetPrefix;
	private final HashSet<String> filters;

	PropertyFilter(String  targetPrefix,RdfProperty property) {
		this.navigationProperty = null;
		this.property = property;
		this.targetPrefix = targetPrefix;
		filters = new HashSet<String>();
	}
	PropertyFilter(String  targetPrefix,RdfNavigationProperty navigationProperty) {
		this.navigationProperty = navigationProperty;
		this.property = null;
		this.targetPrefix = targetPrefix;
		filters = new HashSet<String>();
	}
	public RdfProperty getProperty() {
		return property;
	}
	public RdfNavigationProperty getNavigationProperty() {
		return navigationProperty;
	}
	public HashSet<String> getFilters() {
		return filters;
	}
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
