package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRepositories;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

public class RdfEdmProviders {
	private final Logger log = LoggerFactory.getLogger(RdfEdmProviders.class);
	private static  Map<String, RdfEdmProvider> rdfEdmProviders = new TreeMap<String, RdfEdmProvider>();
	private final RdfRepositories rdfRepositories;

	public RdfEdmProviders(String configFolder,String repositoryFolder,String repositoryUrl, String repositoryDir ) {
		super();
		rdfRepositories = new RdfRepositories(configFolder, repositoryFolder,repositoryUrl,repositoryDir);
	}

	public  void reset(String rdfRepositoryID) {
		if (rdfRepositoryID.equals(RdfConstants.WILDCARD)) {
			rdfEdmProviders = new TreeMap<String, RdfEdmProvider>();
		} else {
			rdfEdmProviders.remove(rdfRepositoryID.toUpperCase());
		}
	}
	public  void reload() {
		rdfRepositories.reload();
		reset(RdfConstants.WILDCARD);
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
	public RdfRepositories getRepositories() {
		
		return rdfRepositories;
	}
}
