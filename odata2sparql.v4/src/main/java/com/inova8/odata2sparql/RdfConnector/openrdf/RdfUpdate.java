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

public class RdfUpdate extends RdfQuery{
	private final Logger log = LoggerFactory.getLogger(RdfUpdate.class);
	private Update updateQuery;
	public RdfUpdate(RdfRoleRepository rdfRoleRepository, String query) {
		super.rdfRoleRepository = rdfRoleRepository;
		super.query = query;
	}
	public void execUpdate() throws OData2SparqlException {
		try {
			super.connection = rdfRoleRepository.getRepository().getConnection();
			log.info( super.query);
			super.connection.begin();
			updateQuery = connection.prepareUpdate(QueryLanguage.SPARQL, super.query);
			updateQuery.execute();
			super.connection.commit();

		} catch (RepositoryException | MalformedQueryException |UpdateExecutionException e) {
			log.error( e.getMessage());
			super.connection.rollback();
			throw new OData2SparqlException("RdfUpdate execUpdate failure",e);
		} 
	}
	public void close() {
		try {
			super.connection.close();
		} catch (RepositoryException e) {
			log.warn("Failed to close update connection",e);
		}	
	}	
}
