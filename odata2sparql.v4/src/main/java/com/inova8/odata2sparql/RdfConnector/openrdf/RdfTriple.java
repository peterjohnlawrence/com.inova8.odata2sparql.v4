package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.model.Statement;

public class RdfTriple {

	private Statement statement;
	RdfTriple(Statement statement) {
		this.statement = statement;
	}
	public Statement getStatement() {
		return statement;
	}
	public void setStatement(Statement statement) {
		this.statement = statement;
	}
	public RdfNode getSubject() {
		return new RdfNode(statement.getSubject());
	}
	public RdfNode getPredicate() {
		return new RdfNode(statement.getPredicate());
	}
	public RdfNode getObject() {
		return new RdfNode(statement.getObject());
	}
}
