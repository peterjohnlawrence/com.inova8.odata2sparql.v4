/*
 * inova8 2020
 */
package com.inova8.odata2sparql.uri;

import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;

/**
 * The Class RdfResourceProperty.
 */
public class RdfResourceProperty extends RdfResourcePart {
	
	/** The rdf edm provider. */
	RdfEdmProvider rdfEdmProvider;
	
	/** The property. */
	EdmProperty property;
	
	/**
	 * Instantiates a new rdf resource property.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param uriResourcePrimitiveProperty the uri resource primitive property
	 */
	public RdfResourceProperty(RdfEdmProvider rdfEdmProvider, UriResourcePrimitiveProperty uriResourcePrimitiveProperty) {
		super.setUriResourceKind(UriResourceKind.primitiveProperty);
		this.rdfEdmProvider = rdfEdmProvider;
		EdmProperty property = uriResourcePrimitiveProperty.getProperty();
		this.property = property;
	}
	
	/**
	 * Gets the nav path.
	 *
	 * @return the nav path
	 */
	public String getNavPath() {
		return property.getName();
	}
	
	/**
	 * Gets the edm property.
	 *
	 * @return the edm property
	 */
	public EdmProperty getEdmProperty() {
		return property;
	}
}
