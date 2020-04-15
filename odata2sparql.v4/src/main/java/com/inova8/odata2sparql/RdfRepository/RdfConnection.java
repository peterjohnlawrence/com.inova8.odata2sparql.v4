/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.RdfRepository;

import com.inova8.odata2sparql.Constants.SPARQLProfile;

class RdfConnection {

	public SPARQLProfile getProfile() {

		return this.repository.getProfile() != null ? this.repository.getProfile() : SPARQLProfile.DEFAULT;
	}
	@SuppressWarnings("unused")
	private final  String url;
	@SuppressWarnings("unused")
	private final  String user;
	@SuppressWarnings("unused")
	private final  String password;
	private final RdfRoleRepository repository;

	public RdfConnection(RdfRoleRepository repository) {
		this.repository = repository;
		this.url = null;
		this.user = null;
		this.password = null;

	}
}
