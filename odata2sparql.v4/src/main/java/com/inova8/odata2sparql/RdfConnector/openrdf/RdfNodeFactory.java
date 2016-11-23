package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;


public class RdfNodeFactory {
	private final static ValueFactory valueFactory =  SimpleValueFactory.getInstance();
	public static RdfNode createURI(String uri) {
		RdfNode rdfNode = new RdfNode(valueFactory.createIRI( uri));
		return rdfNode;
	}

	public static RdfNode createLiteral(String literal) {
		RdfNode rdfNode = new RdfNode(valueFactory.createLiteral( literal));
		return rdfNode;
	}

}
