/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;


/**
 * The Class RdfResultSet. Acts as a wrapper class for the results of a SPARQL query
 */
public class RdfResultSet {
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfTripleSet.class);
    /** The result set. */
    private final TupleQueryResult resultSet;
    
    /** The connection. */
    private final RepositoryConnection connection;
    
    /**
     * Instantiates a new rdf result set.
     *
     * @param repository the repository
     * @param query the query
     */
	 RdfResultSet(Repository repository, String query) {
		 this.connection =  repository.getConnection();
		 this.resultSet =  this.connection.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();
	}
	/**
	 * Checks for next.
	 *
	 * @return true, if successful
	 */
	public boolean hasNext() {
		try {
			return resultSet.hasNext();
		} catch (QueryEvaluationException e) {
			return false;
		}
	}
	
	/**
	 * Next solution.
	 *
	 * @return the rdf query solution
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfQuerySolution nextSolution() throws OData2SparqlException {
		BindingSet querySolution;
		try {
			querySolution = resultSet.next();
		} catch (QueryEvaluationException e) {
			throw new OData2SparqlException("RdfResultSet nextSolution failure",e);
		}
		return new RdfQuerySolution(querySolution);
	}
	
	/**
	 * Next.
	 *
	 * @return the rdf query solution
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfQuerySolution next() throws OData2SparqlException {
		BindingSet querySolution;
		try {
			querySolution = resultSet.next();
		} catch (QueryEvaluationException e) {
			throw new OData2SparqlException("RdfResultSet next failure",e);
		}
		return new RdfQuerySolution(querySolution);
	}

	/**
	 * Close.
	 */
	public void close()  {
		try {
			if (connection.isOpen() ){
				connection.close();
			}
		} catch (QueryEvaluationException | RepositoryException e) {
			log.warn("Failed to close RdfResultSet");
		}
	}
	
	/**
	 * Checks if is closed.
	 *
	 * @return true, if is closed
	 */
	public boolean isClosed(){
		return !connection.isOpen();
		
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
