/*
 * inova8 2020
 */
package com.inova8.odata2sparql.uri;

import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceKind;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;

/**
 * The Class RdfResourceComplexProperty.
 */
public class RdfResourceComplexProperty extends RdfResourcePart {
	
	/** The rdf edm provider. */
	RdfEdmProvider rdfEdmProvider;
	
	/** The complex type. */
	EdmComplexType complexType;
	
	/** The uri resource complex property. */
	UriResourceComplexProperty uriResourceComplexProperty;
	
	/**
	 * Instantiates a new rdf resource complex property.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param uriResourceComplexProperty the uri resource complex property
	 */
	public RdfResourceComplexProperty(RdfEdmProvider rdfEdmProvider, UriResourceComplexProperty uriResourceComplexProperty) {
		super.setUriResourceKind(UriResourceKind.complexProperty);
		this.rdfEdmProvider = rdfEdmProvider;
		this.uriResourceComplexProperty =uriResourceComplexProperty;
		EdmComplexType complexType = uriResourceComplexProperty.getComplexType();
		this.complexType =complexType;
	}
	
	/**
	 * Gets the complex type.
	 *
	 * @return the complex type
	 */
	public EdmComplexType getComplexType() {
		return complexType;
	}
	
	/**
	 * Gets the nav path.
	 *
	 * @return the nav path
	 */
	public String getNavPath() {
		return uriResourceComplexProperty.toString();
	}
}
