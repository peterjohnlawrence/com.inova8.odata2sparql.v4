package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

abstract class RdfQuery {
	RdfRoleRepository rdfRoleRepository;
	protected String query;
	public RepositoryConnection connection;
	public RdfQuery() {
	}


}
