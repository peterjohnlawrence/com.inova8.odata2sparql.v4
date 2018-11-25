package com.inova8.odata2sparql.RdfRepository;

import org.eclipse.rdf4j.repository.Repository;

import com.inova8.odata2sparql.Constants.SPARQLProfile;

public /*static*/ class RdfRoleRepository {

	private final Repository repository;
	private final int defaultQueryLimit;
	private SPARQLProfile profile = SPARQLProfile.DEFAULT; // NO_UCD (use final)
	private final String queryEndpointUrl;

	private final String updateEndpointUrl;

	RdfRoleRepository(Repository repository, int defaultQueryLimit, SPARQLProfile profile, String queryEndpointUrl,
			String updateEndpointUrl) {
		super();
		this.repository = repository;
		this.defaultQueryLimit = defaultQueryLimit;
		this.profile = profile;
		this.queryEndpointUrl = queryEndpointUrl;
		this.updateEndpointUrl = updateEndpointUrl;
	}

	/**
	 * @return the repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return the defaultQueryLimit
	 */
	public int getDefaultQueryLimit() {
		return defaultQueryLimit;
	}

	public SPARQLProfile getProfile() {
		return profile;
	}

	public String getQueryEndpointUrl() {
		return queryEndpointUrl;
	}

	public String getUpdateEndpointUrl() {
		return updateEndpointUrl;
	}
}
