package com.inova8.odata2sparql.RdfRepository;

import org.openrdf.repository.Repository;

import com.inova8.odata2sparql.Constants.SPARQLProfile;

public /*static*/ class RdfRoleRepository{
	  
	private final Repository repository;
	private final int defaultQueryLimit;	
	private  SPARQLProfile profile = SPARQLProfile.DEFAULT; // NO_UCD (use final)
	RdfRoleRepository(Repository repository,int defaultQueryLimit, SPARQLProfile profile) {
		super();
		this.repository=repository;
		this.defaultQueryLimit = defaultQueryLimit;
		this.profile = profile;
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
}
