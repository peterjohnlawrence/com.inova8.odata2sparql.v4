package com.inova8.odata2sparql.RdfRepository;

import org.eclipse.rdf4j.repository.Repository;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.SPARQLProfile;

public /*static*/ class RdfRoleRepository {

	private final Repository repository;
	private final int defaultQueryLimit;
	private SPARQLProfile profile = SPARQLProfile.DEFAULT; // NO_UCD (use final)
	private final String queryEndpointUrl;

	private final String updateEndpointUrl;
	private final String httpURL;

	private String insertGraphUrl;
	private String removeGraphUrl;
	private String changeGraphUrl;

	RdfRoleRepository(Repository repository, int defaultQueryLimit, SPARQLProfile profile, String queryEndpointUrl,
			String updateEndpointUrl) {
		super();
		this.repository = repository;
		this.defaultQueryLimit = defaultQueryLimit;
		this.profile = profile;
		this.queryEndpointUrl = queryEndpointUrl;
		this.updateEndpointUrl = updateEndpointUrl;
		this.httpURL = null;
	}

	RdfRoleRepository(Repository repository, int defaultQueryLimit, SPARQLProfile profile, String httpURL) {
		super();
		this.repository = repository;
		this.defaultQueryLimit = defaultQueryLimit;
		this.profile = profile;
		this.queryEndpointUrl = null;
		this.updateEndpointUrl = null;
		this.httpURL = httpURL;

	}

	RdfRoleRepository(Repository repository, int defaultQueryLimit, SPARQLProfile profile) {
		super();
		this.repository = repository;
		this.defaultQueryLimit = defaultQueryLimit;
		this.profile = profile;
		this.queryEndpointUrl = null;
		this.updateEndpointUrl = null;
		this.httpURL = null;

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

	public String getHttpURL() {
		return httpURL == null ? "" : httpURL;
	}

	public String getQueryEndpointUrl() {
		return queryEndpointUrl == null ? "" : queryEndpointUrl;
	}

	public String getUpdateEndpointUrl() {
		return updateEndpointUrl == null ? "" : updateEndpointUrl;
	}

	public String getServiceUrl() {
		return queryEndpointUrl == null ? httpURL : queryEndpointUrl;
	}

	public String getInsertGraphUrl() {
		return insertGraphUrl == null ? RdfConstants.DEFAULTINSERTGRAPH : insertGraphUrl;
	}

	public void setInsertGraphUrl(String insertGraphUrl) {
		this.insertGraphUrl = insertGraphUrl;
	}

	public String getRemoveGraphUrl() {
		return removeGraphUrl == null ? "" : removeGraphUrl;
	}

	public void setRemoveGraphUrl(String removeGraphUrl) {
		this.removeGraphUrl = removeGraphUrl;
	}

	public String getChangeGraphUrl() {
		return changeGraphUrl == null ? "" : changeGraphUrl;
	}

	public Boolean isChangeGraphUrl() {
		if (changeGraphUrl != null)
			return true;
		else
			return false;
	}

	public void setChangeGraphUrl(String changeGraphUrl) {
		this.changeGraphUrl = changeGraphUrl;
	}
}
