package com.inova8.odata2sparql.Constants;

import com.inova8.odata2sparql.Constants.SPARQLProfile;


public enum SPARQLProfile {
	// Cardinality corresponding to
	// 0..1, 1..1, 0..*, 1..*
	DEFAULT("http://inova8.com/odata4sparql#DEFAULT"), SPARQL10("http://inova8.com/odata4sparql#SPARQL10"), SPARQL11(
			"http://inova8.com/odata4sparql#SPARQL11"), AG("http://inova8.com/odata4sparql#ALLEGROGRAPH"), TB(
			"http://inova8.com/odata4sparql#TOPQUADRANT"), JENA("http://inova8.com/odata4sparql#JENA"), VIRT("http://inova8.com/odata4sparql#VIRTUOSO"), RDF4J("http://inova8.com/odata4sparql#RDF4J");

	private final String code;

	private SPARQLProfile(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static SPARQLProfile get(String code) {
		if (code == null)
			return SPARQLProfile.DEFAULT;
		for (SPARQLProfile s : values()) {
			if (s.code.equals(code))
				return s;
		}
		return SPARQLProfile.DEFAULT;
	}
}
