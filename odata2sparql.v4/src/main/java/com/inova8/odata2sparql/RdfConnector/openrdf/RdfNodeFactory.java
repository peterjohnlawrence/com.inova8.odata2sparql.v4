/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;


/**
 * A factory for creating RdfNode objects.
 */
public class RdfNodeFactory {
	
	/** The Constant valueFactory. */
	private final static ValueFactory valueFactory =  SimpleValueFactory.getInstance();
	
	/**
	 * Creates a new RdfNode object.
	 *
	 * @param uri the uri
	 * @return the rdf node
	 */
	public static RdfNode createURI(String uri) {
		RdfNode rdfNode = new RdfNode(valueFactory.createIRI( uri));
		return rdfNode;
	}

	/**
	 * Creates a new RdfNode object.
	 *
	 * @param literal the literal
	 * @return the rdf node
	 */
	public static RdfNode createLiteral(String literal) {
		RdfNode rdfNode = new RdfNode(valueFactory.createLiteral( literal));
		return rdfNode;
	}

}
