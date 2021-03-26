/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * The Class RdfQuerySolution.
 */
public class RdfQuerySolution {
	
	/** The query solution. */
	private final BindingSet querySolution;
	
	/**
	 * Instantiates a new rdf query solution.
	 *
	 * @param querySolution the query solution
	 */
	RdfQuerySolution(BindingSet querySolution) {
		this.querySolution =querySolution;
	}
	
	/**
	 * Gets the rdf node.
	 *
	 * @param string the string
	 * @return the rdf node
	 */
	public RdfNode getRdfNode(String string) {
		if(querySolution.getValue(string)!=null){
			return new RdfNode(querySolution.getValue(string));
		}else{
			return null;
		}
	}
	
	/**
	 * Gets the rdf literal.
	 *
	 * @param string the string
	 * @return the rdf literal
	 */
	public RdfLiteral getRdfLiteral(String string) {	
		return new RdfLiteral((Literal)querySolution.getValue(string));
	}
}

