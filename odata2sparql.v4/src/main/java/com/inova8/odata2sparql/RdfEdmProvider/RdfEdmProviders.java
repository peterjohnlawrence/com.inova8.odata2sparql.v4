package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.inova8.odata2sparql.Constants.ODataServiceVersion;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRepositories;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

public class RdfEdmProviders {
	private final Log log = LogFactory.getLog(RdfEdmProviders.class);
	private final Map<String, RdfEdmProvider> rdfEdmProviders = new HashMap<String, RdfEdmProvider>();
	static private final RdfRepositories rdfRepositories = new RdfRepositories();

	public static RdfRepositories getRdfRepositories() {
		return rdfRepositories;
	}

	public RdfEdmProviders() {
		super();
	}

	public RdfEdmProvider getRdfEdmProvider(String rdfRepositoryID) throws OData2SparqlException {

		RdfRepository rdfRepository = rdfRepositories.getRdfRepository(rdfRepositoryID);
		if (rdfRepository == null) {
			log.error("Unsupported model: " + rdfRepositoryID);
			throw new OData2SparqlException("Unsupported model: " + rdfRepositoryID);
		}
		RdfEdmProvider rdfEdmProvider = rdfEdmProviders.get( rdfRepositoryID);

		if (rdfEdmProvider == null) {
			rdfEdmProvider = new RdfEdmProvider(rdfRepository);
			rdfEdmProviders.put(rdfRepositoryID, rdfEdmProvider);
		}
		return rdfEdmProvider;
	}
}
