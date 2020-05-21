/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
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

public class SparqlStatement {
	private final static Logger log = LoggerFactory.getLogger(SparqlStatement.class);
	private static String sparql;

	public SparqlStatement(String sparql) {
		SparqlStatement.sparql = sparql;
	}

	public String getSparql() {
		return sparql;
	}

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