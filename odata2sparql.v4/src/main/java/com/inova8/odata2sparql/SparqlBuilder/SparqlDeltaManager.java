package com.inova8.odata2sparql.SparqlBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;

public class SparqlDeltaManager {
	private final static Logger log = LoggerFactory.getLogger(SparqlDeltaManager.class);
	
	public static void clear(RdfEdmProvider rdfEdmProvider) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		StringBuilder clear = new StringBuilder();
		try {
			clear.append("DELETE {GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">{?s ?p ?o}	}WHERE { GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">{ ?s ?p ?o }}");
			sparqlStatement= new SparqlStatement(clear.toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);
	}
}
