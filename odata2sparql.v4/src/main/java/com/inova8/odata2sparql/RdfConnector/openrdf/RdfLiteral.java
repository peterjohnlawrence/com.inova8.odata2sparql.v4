package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.model.Literal;

public class RdfLiteral {
	private final Literal node_literal;

	RdfLiteral(Literal node_literal) {
		this.node_literal = node_literal;
	}

	public String getLexicalForm() {
		return node_literal.stringValue();
	}

	public String getString() {
		return node_literal.toString();
	}
}
