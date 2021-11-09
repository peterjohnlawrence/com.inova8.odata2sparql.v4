/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

/**
 * The Class RdfConstructQuery.
 */
public class RdfConstructQuery extends RdfQuery{
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfConstructQuery.class);
	
	/** The graph query. */
	private GraphQuery graphQuery;
	
	/**
	 * Instantiates a new rdf construct query.
	 *
	 * @param rdfRoleRepository the rdf role repository
	 * @param query the query
	 */
	public RdfConstructQuery(RdfRoleRepository rdfRoleRepository, String query) {
		super.rdfRoleRepository = rdfRoleRepository;
		super.query = query;
	}
	
	/**
	 * Exec construct.
	 *
	 * @return the rdf triple set
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfTripleSet execConstruct() throws  OData2SparqlException {
		RdfTripleSet rdfTripleSet = null;
		try {
			super.connection = rdfRoleRepository.getRepository().getConnection();
			graphQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, super.query);
			log.info( "\n"+ super.query);
			rdfTripleSet = new RdfTripleSet(connection, QueryResults.distinctResults(graphQuery.evaluate()));
		} catch (Exception e) {		
			log.error( " RdfTripleSet execConstruct() failure with exception" + e.getMessage());
			log.error( " Query:" + super.query);
			throw new OData2SparqlException(" RdfTripleSet execConstruct() failure with message:\n"+ e.getMessage(),e);
		}
		return  rdfTripleSet;
	}
}
