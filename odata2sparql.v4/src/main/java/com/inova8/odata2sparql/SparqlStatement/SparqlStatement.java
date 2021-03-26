/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlStatement;

import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfConstructQuery;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfSelectQuery;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfTripleSet;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfUpdate;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;

/**
 * The Class SparqlStatement.
 */
public class SparqlStatement {
	
	/** The Constant log. */
	private final static Logger log = LoggerFactory.getLogger(SparqlStatement.class);
	
	/** The sparql. */
	private static String sparql;

	/**
	 * Instantiates a new sparql statement.
	 *
	 * @param sparql the sparql
	 */
	public SparqlStatement(String sparql) {
		SparqlStatement.sparql = sparql;
	}

	/**
	 * Gets the sparql.
	 *
	 * @return the sparql
	 */
	public String getSparql() {
		return sparql;
	}

	/**
	 * Execute construct.
	 *
	 * @param sparqlEdmProvider the sparql edm provider
	 * @param entityType the entity type
	 * @param expand the expand
	 * @param select the select
	 * @return the sparql entity collection
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	SparqlEntityCollection executeConstruct(RdfEdmProvider sparqlEdmProvider, RdfEntityType entityType,
			ExpandOption expand, SelectOption select) throws OData2SparqlException {
		RdfConstructQuery rdfQuery = new RdfConstructQuery(sparqlEdmProvider.getRdfRepository().getDataRepository(),
				sparql);

		RdfTripleSet results;
		try {
			results = rdfQuery.execConstruct();
			log.info("Processing results");
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataRuntimeException(e.getMessage(), null);
		}
		return new SparqlEntityCollection(sparqlEdmProvider, entityType, results, expand, select);
	}

	/**
	 * Execute select.
	 *
	 * @param sparqlEdmProvider the sparql edm provider
	 * @return the rdf result set
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfResultSet executeSelect(RdfEdmProvider sparqlEdmProvider) throws OData2SparqlException {
		RdfSelectQuery rdfQuery = new RdfSelectQuery(sparqlEdmProvider.getRdfRepository().getDataRepository(), sparql);
		RdfResultSet results = null;
		try {
			results = rdfQuery.execSelect();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataRuntimeException(e.getMessage(), null);
		}
		return results;
	}

	/**
	 * Execute insert.
	 *
	 * @param sparqlEdmProvider the sparql edm provider
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	void executeInsert(RdfEdmProvider sparqlEdmProvider)
			throws OData2SparqlException {

		RdfUpdate rdfInsert = new RdfUpdate(sparqlEdmProvider.getRdfRepository().getDataRepository(),
				sparql);
		try {
			rdfInsert.execUpdate();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataRuntimeException(e.getMessage(), null);
		} finally {
			rdfInsert.close();
		}
	}

	/**
	 * Execute delete.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 */
	public void executeDelete(RdfEdmProvider rdfEdmProvider) {
		
		RdfUpdate rdfDelete = new RdfUpdate(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparql);
		try {
			rdfDelete.execUpdate();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataRuntimeException(e.getMessage(), null);
		} finally {
			rdfDelete.close();
		}	
	}
	
	/**
	 * Execute update.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 */
	public void executeUpdate(RdfEdmProvider rdfEdmProvider) {
		
		RdfUpdate rdfUpdate = new RdfUpdate(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparql);
		try {
			rdfUpdate.execUpdate();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataRuntimeException(e.getMessage(), null);
		} finally {
			rdfUpdate.close();
		}	
	}
}