/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;


/**
 * The Class RdfTripleSet.
 */
public class RdfTripleSet {
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfTripleSet.class);
	
	/** The triple set. */
	private final GraphQueryResult tripleSet;
	
	/** The connection. */
	private final RepositoryConnection connection;

	/**
	 * Instantiates a new rdf triple set.
	 *
	 * @param connection the connection
	 * @param tripleSet the triple set
	 */
	RdfTripleSet(RepositoryConnection connection, GraphQueryResult tripleSet) {
		this.tripleSet = tripleSet;
		this.connection = connection;
	}

	/**
	 * Checks for next.
	 *
	 * @return true, if successful
	 */
	public boolean hasNext() {
		try {
			return tripleSet.hasNext();
		} catch (QueryEvaluationException e) {
			return false;
		}
	}

	/**
	 * Next.
	 *
	 * @return the rdf triple
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfTriple next() throws OData2SparqlException {
		RdfTriple rdfTriple;
		try {
			rdfTriple = new RdfTriple(tripleSet.next());
		} catch (QueryEvaluationException e) {
			throw new OData2SparqlException("RdfTripleSet next failure",e);
		}
		return rdfTriple;
	}

	/**
	 * Close.
	 */
	public void close() {
		try {
			if(tripleSet.hasNext()){
				tripleSet.close();
			}			
			if (connection.isOpen() ){
				connection.close();
			}
			
		} catch (QueryEvaluationException | RepositoryException e) {
			log.warn("failed to close RdfTripleSet");
		}
	}

	/**
	 * Finalize.
	 *
	 * @throws Throwable the throwable
	 */
	@SuppressWarnings("deprecation")
	protected void finalize() throws Throwable {
		super.finalize();
		this.close();
	}
}
