/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

/**
 * The Class RdfUpdate.
 */
public class RdfUpdate extends RdfQuery{
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfUpdate.class);
	
	/** The update query. */
	private Update updateQuery;
	
	/**
	 * Instantiates a new rdf update.
	 *
	 * @param rdfRoleRepository the rdf role repository
	 * @param query the query
	 */
	public RdfUpdate(RdfRoleRepository rdfRoleRepository, String query) {
		super.rdfRoleRepository = rdfRoleRepository;
		super.query = query;
	}
	
	/**
	 * Exec update.
	 *
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public void execUpdate() throws OData2SparqlException {
		try {
			super.connection = rdfRoleRepository.getRepository().getConnection();
			log.info( "\n"+ super.query);
			super.connection.begin();
			updateQuery = connection.prepareUpdate(QueryLanguage.SPARQL, super.query);
			updateQuery.execute();
			//Should check if the update caused any changes or not, otherwise can given the appearance of being successful when not. Should raise an HTTP 422 error
			super.connection.commit();

		} catch (RepositoryException | MalformedQueryException |UpdateExecutionException e) {
			log.error( e.getMessage());
			super.connection.rollback();
			throw new OData2SparqlException("RdfUpdate execUpdate failure",e);
		} 
	}
	
	/**
	 * Close.
	 */
	public void close() {
		try {
			super.connection.close();
		} catch (RepositoryException e) {
			log.warn("Failed to close update connection",e);
		}	
	}	
}
