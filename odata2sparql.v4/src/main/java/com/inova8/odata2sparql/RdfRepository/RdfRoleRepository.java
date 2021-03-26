/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfRepository;

import org.eclipse.rdf4j.repository.Repository;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.SPARQLProfile;

/**
 * The Class RdfRoleRepository.
 */
public /*static*/ class RdfRoleRepository {

	/** The repository. */
	private final Repository repository;
	
	/** The default query limit. */
	private final int defaultQueryLimit;
	
	/** The profile. */
	private SPARQLProfile profile = SPARQLProfile.DEFAULT; // NO_UCD (use final)
	
	/** The query endpoint url. */
	private final String queryEndpointUrl;

	/** The update endpoint url. */
	private final String updateEndpointUrl;
	
	/** The http URL. */
	private final String httpURL;

	/** The insert graph url. */
	private String insertGraphUrl;
	
	/** The remove graph url. */
	private String removeGraphUrl;
	
	/** The change graph url. */
	private String changeGraphUrl;

	/**
	 * Instantiates a new rdf role repository.
	 *
	 * @param repository the repository
	 * @param defaultQueryLimit the default query limit
	 * @param profile the profile
	 * @param queryEndpointUrl the query endpoint url
	 * @param updateEndpointUrl the update endpoint url
	 */
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

	/**
	 * Instantiates a new rdf role repository.
	 *
	 * @param repository the repository
	 * @param defaultQueryLimit the default query limit
	 * @param profile the profile
	 * @param httpURL the http URL
	 */
	RdfRoleRepository(Repository repository, int defaultQueryLimit, SPARQLProfile profile, String httpURL) {
		super();
		this.repository = repository;
		this.defaultQueryLimit = defaultQueryLimit;
		this.profile = profile;
		this.queryEndpointUrl = null;
		this.updateEndpointUrl = null;
		this.httpURL = httpURL;

	}

	/**
	 * Instantiates a new rdf role repository.
	 *
	 * @param repository the repository
	 * @param defaultQueryLimit the default query limit
	 * @param profile the profile
	 */
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
	 * Gets the repository.
	 *
	 * @return the repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * Gets the default query limit.
	 *
	 * @return the defaultQueryLimit
	 */
	public int getDefaultQueryLimit() {
		return defaultQueryLimit;
	}

	/**
	 * Gets the profile.
	 *
	 * @return the profile
	 */
	public SPARQLProfile getProfile() {
		return profile;
	}

	/**
	 * Gets the http URL.
	 *
	 * @return the http URL
	 */
	public String getHttpURL() {
		return httpURL == null ? "" : httpURL;
	}

	/**
	 * Gets the query endpoint url.
	 *
	 * @return the query endpoint url
	 */
	public String getQueryEndpointUrl() {
		return queryEndpointUrl == null ? "" : queryEndpointUrl;
	}

	/**
	 * Gets the update endpoint url.
	 *
	 * @return the update endpoint url
	 */
	public String getUpdateEndpointUrl() {
		return updateEndpointUrl == null ? "" : updateEndpointUrl;
	}

	/**
	 * Gets the service url.
	 *
	 * @return the service url
	 */
	public String getServiceUrl() {
		return queryEndpointUrl == null ? httpURL : queryEndpointUrl;
	}

	/**
	 * Gets the insert graph url.
	 *
	 * @return the insert graph url
	 */
	public String getInsertGraphUrl() {
		return insertGraphUrl == null ? RdfConstants.DEFAULTINSERTGRAPH : insertGraphUrl;
	}

	/**
	 * Sets the insert graph url.
	 *
	 * @param insertGraphUrl the new insert graph url
	 */
	public void setInsertGraphUrl(String insertGraphUrl) {
		this.insertGraphUrl = insertGraphUrl;
	}

	/**
	 * Gets the removes the graph url.
	 *
	 * @return the removes the graph url
	 */
	public String getRemoveGraphUrl() {
		return removeGraphUrl == null ? "" : removeGraphUrl;
	}

	/**
	 * Sets the removes the graph url.
	 *
	 * @param removeGraphUrl the new removes the graph url
	 */
	public void setRemoveGraphUrl(String removeGraphUrl) {
		this.removeGraphUrl = removeGraphUrl;
	}

	/**
	 * Gets the change graph url.
	 *
	 * @return the change graph url
	 */
	public String getChangeGraphUrl() {
		return changeGraphUrl == null ? "" : changeGraphUrl;
	}

	/**
	 * Checks if is change graph url.
	 *
	 * @return the boolean
	 */
	public Boolean isChangeGraphUrl() {
		if (changeGraphUrl != null)
			return true;
		else
			return false;
	}

	/**
	 * Sets the change graph url.
	 *
	 * @param changeGraphUrl the new change graph url
	 */
	public void setChangeGraphUrl(String changeGraphUrl) {
		this.changeGraphUrl = changeGraphUrl;
	}
}
