package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;


public class RdfTripleSet {
	private final Logger log = LoggerFactory.getLogger(RdfTripleSet.class);
	private final GraphQueryResult tripleSet;
	private final RepositoryConnection connection;

	RdfTripleSet(RepositoryConnection connection, GraphQueryResult tripleSet) {
		this.tripleSet = tripleSet;
		this.connection = connection;
	}

	public boolean hasNext() {
		try {
			return tripleSet.hasNext();
		} catch (QueryEvaluationException e) {
			return false;
		}
	}

	public RdfTriple next() throws OData2SparqlException {
		RdfTriple rdfTriple;
		try {
			rdfTriple = new RdfTriple(tripleSet.next());
		} catch (QueryEvaluationException e) {
			throw new OData2SparqlException("RdfTripleSet next failure",e);
		}
		return rdfTriple;
	}

	private void close() {
		try {
			tripleSet.close();
			connection.close();
		} catch (QueryEvaluationException | RepositoryException e) {
			log.warn("failed to close RdfTripleSet");
		}
	}

	protected void finalize() throws Throwable {
		super.finalize();
		this.close();
	}
}
