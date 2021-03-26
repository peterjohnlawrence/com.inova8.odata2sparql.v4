/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRepositories;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;
import com.inova8.odata2sparql.SparqlBuilder.SparqlChangeManager;

/**
 * The Class RdfEdmProviders.
 */
public class RdfEdmProviders {
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfEdmProviders.class);
	
	/** The rdf edm providers. */
	private static  TreeMap<String, RdfEdmProvider> rdfEdmProviders = new TreeMap<String, RdfEdmProvider>();
	
	/** The rdf repositories. */
	private final RdfRepositories rdfRepositories;

	/**
	 * Instantiates a new rdf edm providers.
	 *
	 * @param configFolder the config folder
	 * @param repositoryFolder the repository folder
	 * @param repositoryUrl the repository url
	 * @param repositoryDir the repository dir
	 */
	public RdfEdmProviders(String configFolder,String repositoryFolder,String repositoryUrl, String repositoryDir ) {
		super();
		rdfRepositories = new RdfRepositories(configFolder, repositoryFolder,repositoryUrl,repositoryDir);
	}

	/**
	 * Reset.
	 *
	 * @param rdfRepositoryID the rdf repository ID
	 */
	public  void reset(String rdfRepositoryID) {
		if (rdfRepositoryID.equals(RdfConstants.WILDCARD)) {
			rdfEdmProviders = new TreeMap<String, RdfEdmProvider>();
		} else {
			rdfEdmProviders.remove(rdfRepositoryID);
			System.gc();
		}
	}
	
	/**
	 * Reload.
	 */
	public  void reload() {
		rdfRepositories.reload();
		reset(RdfConstants.WILDCARD);
	}
	
	/**
	 * Changes.
	 *
	 * @param rdfRepositoryID the rdf repository ID
	 * @param option the option
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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
	
	/**
	 * Gets the rdf edm provider.
	 *
	 * @param rdfRepositoryID the rdf repository ID
	 * @return the rdf edm provider
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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
	
	/**
	 * Gets the repositories.
	 *
	 * @return the repositories
	 */
	public RdfRepositories getRepositories() {
		
		return rdfRepositories;
	}
	
	/**
	 * Gets the rdf edm providers.
	 *
	 * @return the rdf edm providers
	 */
	public TreeMap<String, RdfEdmProvider> getRdfEdmProviders () {
		
		return rdfEdmProviders;
	}
}
