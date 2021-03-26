/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.model.Statement;

/**
 * The Class RdfTriple.
 */
public class RdfTriple {

	/** The statement. */
	private Statement statement;
	
	/**
	 * Instantiates a new rdf triple.
	 *
	 * @param statement the statement
	 */
	RdfTriple(Statement statement) {
		this.statement = statement;
	}
	
	/**
	 * Gets the statement.
	 *
	 * @return the statement
	 */
	public Statement getStatement() {
		return statement;
	}
	
	/**
	 * Sets the statement.
	 *
	 * @param statement the new statement
	 */
	public void setStatement(Statement statement) {
		this.statement = statement;
	}
	
	/**
	 * Gets the subject.
	 *
	 * @return the subject
	 */
	public RdfNode getSubject() {
		return new RdfNode(statement.getSubject());
	}
	
	/**
	 * Gets the predicate.
	 *
	 * @return the predicate
	 */
	public RdfNode getPredicate() {
		return new RdfNode(statement.getPredicate());
	}
	
	/**
	 * Gets the object.
	 *
	 * @return the object
	 */
	public RdfNode getObject() {
		return new RdfNode(statement.getObject());
	}
}
