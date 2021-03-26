/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfRepository;

import com.inova8.odata2sparql.Constants.SPARQLProfile;

/**
 * The Class RdfConnection.
 */
class RdfConnection {

	/**
	 * Gets the profile.
	 *
	 * @return the profile
	 */
	public SPARQLProfile getProfile() {

		return this.repository.getProfile() != null ? this.repository.getProfile() : SPARQLProfile.DEFAULT;
	}
	
	/** The url. */
	@SuppressWarnings("unused")
	private final  String url;
	
	/** The user. */
	@SuppressWarnings("unused")
	private final  String user;
	
	/** The password. */
	@SuppressWarnings("unused")
	private final  String password;
	
	/** The repository. */
	private final RdfRoleRepository repository;

	/**
	 * Instantiates a new rdf connection.
	 *
	 * @param repository the repository
	 */
	public RdfConnection(RdfRoleRepository repository) {
		this.repository = repository;
		this.url = null;
		this.user = null;
		this.password = null;

	}
}
