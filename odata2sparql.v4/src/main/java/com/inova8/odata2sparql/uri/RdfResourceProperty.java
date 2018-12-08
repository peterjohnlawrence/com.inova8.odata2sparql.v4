package com.inova8.odata2sparql.uri;

import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;

public class RdfResourceProperty extends RdfResourcePart {
	RdfEdmProvider rdfEdmProvider;
	EdmProperty property;
	public RdfResourceProperty(RdfEdmProvider rdfEdmProvider, UriResourcePrimitiveProperty uriResourcePrimitiveProperty) {
		super.setUriResourceKind(UriResourceKind.primitiveProperty);
		this.rdfEdmProvider = rdfEdmProvider;
		EdmProperty property = uriResourcePrimitiveProperty.getProperty();
		this.property = property;
	}
	public String getNavPath() {
		return property.getName();
	}
	public EdmProperty getEdmProperty() {
		return property;
	}
}
