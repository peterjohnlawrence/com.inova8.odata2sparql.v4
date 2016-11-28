package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

public class RdfConstructQuery extends RdfQuery{
	private final Log log = LogFactory.getLog(RdfConstructQuery.class);
	private GraphQuery graphQuery;
	public RdfConstructQuery(RdfRoleRepository rdfRoleRepository, String query) {
		super.rdfRoleRepository = rdfRoleRepository;
		super.query = query;
	}
	public RdfTripleSet execConstruct() throws  OData2SparqlException {
		RdfTripleSet rdfTripleSet = null;
		try {
			super.connection = rdfRoleRepository.getRepository().getConnection();
			graphQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, super.query);
			rdfTripleSet = new RdfTripleSet(connection, graphQuery.evaluate());
			log.info( super.query);
		} catch (Exception e) {		
			log.error( " RdfTripleSet execConstruct() failure with exception" + e.getMessage());
			throw new OData2SparqlException(" RdfTripleSet execConstruct() failure with message:\n"+ e.getMessage(),e);
		}
		return  rdfTripleSet;
	}
}
