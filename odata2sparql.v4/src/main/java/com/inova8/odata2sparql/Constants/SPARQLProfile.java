/*
 * inova8 2020
 */
package com.inova8.odata2sparql.Constants;

import com.inova8.odata2sparql.Constants.SPARQLProfile;


/**
 * The Enum SPARQLProfile.
 */
public enum SPARQLProfile {
	// Cardinality corresponding to
	/** The default. */
	// 0..1, 1..1, 0..*, 1..*
	DEFAULT("http://inova8.com/odata4sparql#DEFAULT"), 
 /** The sparql10. */
 SPARQL10("http://inova8.com/odata4sparql#SPARQL10"), 
 /** The sparql11. */
 SPARQL11(
			"http://inova8.com/odata4sparql#SPARQL11"), 
 /** The ag. */
 AG("http://inova8.com/odata4sparql#ALLEGROGRAPH"), 
 /** The tb. */
 TB(
			"http://inova8.com/odata4sparql#TOPQUADRANT"), 
 /** The jena. */
 JENA("http://inova8.com/odata4sparql#JENA"), 
 /** The virt. */
 VIRT("http://inova8.com/odata4sparql#VIRTUOSO"), 
 /** The rdf4j. */
 RDF4J("http://inova8.com/odata4sparql#RDF4J");

	/** The code. */
	private final String code;

	/**
	 * Instantiates a new SPARQL profile.
	 *
	 * @param code the code
	 */
	private SPARQLProfile(String code) {
		this.code = code;
	}

	/**
	 * Gets the code.
	 *
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Gets the.
	 *
	 * @param code the code
	 * @return the SPARQL profile
	 */
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
