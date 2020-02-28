package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

public class RdfSelectQuery extends RdfQuery{
	private final Logger log = LoggerFactory.getLogger(RdfConstructQuery.class);
	public RdfSelectQuery(RdfRoleRepository rdfRoleRepository, String query) {
		super.rdfRoleRepository = rdfRoleRepository;
		super.query = query;
	}
	public RdfResultSet execSelect() throws OData2SparqlException {
		return execSelect(true);
	}
	public RdfResultSet execSelect(Boolean logQuery) throws OData2SparqlException {
		RdfResultSet rdfResultSet = null;
		try {
			if( logQuery)log.info( "\n"+ super.query);
			rdfResultSet = new RdfResultSet(rdfRoleRepository.getRepository(), super.query );
		} catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			log.error( super.query);
			log.error(e.toString());
			switch(e.getCause().getClass().toString()) {
			case "class.org.eclipse.rdf4j.repository.RepositoryException" :
				throw new OData2SparqlException("Connection failure: Make sure SPARQL service is available",e);
			case "class.org.eclipse.rdf4j.repository.MalformedQueryException" :
				throw new OData2SparqlException("Issued query malformed",e);				
				 
			default:
				throw new OData2SparqlException("RdfSelectQuery execSelect failure with message:\n"+ e.getMessage(),e);
			}

		}
		return rdfResultSet;
	}	
}
