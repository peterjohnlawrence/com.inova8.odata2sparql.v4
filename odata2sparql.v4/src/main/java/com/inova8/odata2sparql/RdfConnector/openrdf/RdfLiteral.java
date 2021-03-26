/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.model.Literal;

/**
 * The Class RdfLiteral.
 */
public class RdfLiteral {
	
	/** The node literal. */
	private final Literal node_literal;

	/**
	 * Instantiates a new rdf literal.
	 *
	 * @param node_literal the node literal
	 */
	RdfLiteral(Literal node_literal) {
		this.node_literal = node_literal;
	}

	/**
	 * Gets the lexical form.
	 *
	 * @return the lexical form
	 */
	public String getLexicalForm() {
		return node_literal.stringValue();
	}

	/**
	 * Gets the string.
	 *
	 * @return the string
	 */
	public String getString() {
		return node_literal.toString();
	}
}
