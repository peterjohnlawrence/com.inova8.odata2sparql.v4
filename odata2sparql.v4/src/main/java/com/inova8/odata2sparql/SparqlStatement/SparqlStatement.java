/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.SparqlStatement;

public class SparqlStatement {
	private final String sparql;

	public SparqlStatement(String sparql) {
		this.sparql = sparql;
	}

	public String getSparql() {
		return sparql;
	}
	
}