package com.inova8.odata2sparql.uri;

import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceKind;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;

public class RdfResourceComplexProperty extends RdfResourcePart {
	RdfEdmProvider rdfEdmProvider;
	EdmComplexType complexType;
	UriResourceComplexProperty uriResourceComplexProperty;
	public RdfResourceComplexProperty(RdfEdmProvider rdfEdmProvider, UriResourceComplexProperty uriResourceComplexProperty) {
		super.setUriResourceKind(UriResourceKind.complexProperty);
		this.rdfEdmProvider = rdfEdmProvider;
		this.uriResourceComplexProperty =uriResourceComplexProperty;
		EdmComplexType complexType = uriResourceComplexProperty.getComplexType();
		this.complexType =complexType;
	}
	public EdmComplexType getComplexType() {
		return complexType;
	}
	public String getNavPath() {
		return uriResourceComplexProperty.toString();
	}
}
