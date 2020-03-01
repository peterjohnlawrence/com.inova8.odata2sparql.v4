package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRepositories;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;
import com.inova8.odata2sparql.SparqlBuilder.SparqlChangeManager;

public class RdfEdmProviders {
	private final Logger log = LoggerFactory.getLogger(RdfEdmProviders.class);
	private static  TreeMap<String, RdfEdmProvider> rdfEdmProviders = new TreeMap<String, RdfEdmProvider>();
	private final RdfRepositories rdfRepositories;

	public RdfEdmProviders(String configFolder,String repositoryFolder,String repositoryUrl, String repositoryDir ) {
		super();
		rdfRepositories = new RdfRepositories(configFolder, repositoryFolder,repositoryUrl,repositoryDir);
	}

	public  void reset(String rdfRepositoryID) {
		if (rdfRepositoryID.equals(RdfConstants.WILDCARD)) {
			rdfEdmProviders = new TreeMap<String, RdfEdmProvider>();
		} else {
			rdfEdmProviders.remove(rdfRepositoryID);
		}
	}
	public  void reload() {
		rdfRepositories.reload();
		reset(RdfConstants.WILDCARD);
	}
	public  void changes(String rdfRepositoryID, String option) throws OData2SparqlException {
		switch (option){
		case "clear": 
			SparqlChangeManager.clear(rdfRepositoryID, getRdfEdmProvider(rdfRepositoryID));	
			break;
		case "rollback": 
			SparqlChangeManager.rollback(rdfRepositoryID, getRdfEdmProvider(rdfRepositoryID));	
			break;
		case "archive": 
			SparqlChangeManager.archive(rdfRepositoryID, getRdfEdmProvider(rdfRepositoryID));	
			break;
		default: break;
		}
	}
	public RdfEdmProvider getRdfEdmProvider(String rdfRepositoryID) throws OData2SparqlException {

		RdfRepository rdfRepository = rdfRepositories.getRdfRepository(rdfRepositoryID);
		if (rdfRepository == null) {
			log.error("Unsupported model: " + rdfRepositoryID);
			throw new OData2SparqlException("Unsupported model: " + rdfRepositoryID);
		}
		RdfEdmProvider rdfEdmProvider = rdfEdmProviders.get( rdfRepositoryID);

		if (rdfEdmProvider == null) {
			rdfEdmProvider = new RdfEdmProvider(this,rdfRepository);
	//		rdfEdmProviders.put(rdfRepositoryID, rdfEdmProvider);
		}
		return rdfEdmProvider;
	}
	public RdfRepositories getRepositories() {
		
		return rdfRepositories;
	}
	public TreeMap<String, RdfEdmProvider> getRdfEdmProviders () {
		
		return rdfEdmProviders;
	}
}
