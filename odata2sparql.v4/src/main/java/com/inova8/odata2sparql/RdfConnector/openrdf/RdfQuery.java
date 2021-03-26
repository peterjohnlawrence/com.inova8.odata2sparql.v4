/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

/**
 * The Class RdfQuery.
 */
abstract class RdfQuery {
	
	/** The rdf role repository. */
	RdfRoleRepository rdfRoleRepository;
	
	/** The query. */
	protected String query;
	
	/** The connection. */
	public RepositoryConnection connection;
	
	/**
	 * Instantiates a new rdf query.
	 */
	public RdfQuery() {
	}


}
