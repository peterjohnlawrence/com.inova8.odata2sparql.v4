package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRepositories;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

public class RdfEdmProviders {
	private final Log log = LogFactory.getLog(RdfEdmProviders.class);
	private static  Map<String, RdfEdmProvider> rdfEdmProviders = new HashMap<String, RdfEdmProvider>();
	private final RdfRepositories rdfRepositories;// = new RdfRepositories();
//	public static RdfRepositories getRdfRepositories() {
//		return rdfRepositories;
//	}
	public RdfEdmProviders(String repositoryFolder,String repositoryUrl) {
		super();
		rdfRepositories = new RdfRepositories(repositoryFolder,repositoryUrl);
	}
//	public RdfEdmProviders() {
//		super();
//	}
	public  void reset(String rdfRepositoryID) {
		if (rdfRepositoryID.equals(RdfConstants.WILDCARD)) {
			rdfEdmProviders = new HashMap<String, RdfEdmProvider>();
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
}
