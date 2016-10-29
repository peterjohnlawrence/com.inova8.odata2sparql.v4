package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.RepositoryException;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

public class RdfSelectQuery extends RdfQuery{
	private final Log log = LogFactory.getLog(RdfConstructQuery.class);
	private TupleQuery tupleQuery;
	public RdfSelectQuery(RdfRoleRepository rdfRoleRepository, String query) {
		super.rdfRoleRepository = rdfRoleRepository;
		super.query = query;
	}
	public RdfResultSet execSelect() throws OData2SparqlException {
		RdfResultSet rdfResultSet = null;
		try {
			super.connection = rdfRoleRepository.getRepository().getConnection();
			tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, super.query);
			log.info( super.query);
			rdfResultSet = new RdfResultSet(connection, tupleQuery.evaluate());
		} catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			log.error( super.query);
			throw new OData2SparqlException("RdfSelectQuery execSelect failure",e);
		}
		return rdfResultSet;
	}	
}
