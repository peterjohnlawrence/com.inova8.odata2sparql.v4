/*
 * inova8 2020
 */
package com.inova8.odata2sparql.Constants;

import com.inova8.odata2sparql.Constants.TextSearchType;

/**
 * The Enum TextSearchType.
 */
public enum TextSearchType {

	/** The default. */
	DEFAULT("http://inova8.com/odata4sparql#DEFAULT"), /** The halyard es. */
 HALYARD_ES("http://inova8.com/odata4sparql#Halyard_ElasticSearch"), /** The rdf4j lucene. */
 RDF4J_LUCENE(
			"http://inova8.com/odata4sparql#RDF4J_Lucene");

	/** The code. */
	private final String code;

	/**
	 * Instantiates a new text search type.
	 *
	 * @param code the code
	 */
	private TextSearchType(String code) {
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
	 * @return the text search type
	 */
	public static TextSearchType get(String code) {
		if (code == null)
			return TextSearchType.DEFAULT;
		for (TextSearchType s : values()) {
			if (s.code.equals(code))
				return s;
		}
		return TextSearchType.DEFAULT;
	}
}
