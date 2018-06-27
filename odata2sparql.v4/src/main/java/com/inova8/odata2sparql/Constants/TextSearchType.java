package com.inova8.odata2sparql.Constants;

import com.inova8.odata2sparql.Constants.TextSearchType;


public enum TextSearchType {
	// Cardinality corresponding to
	// 0..1, 1..1, 0..*, 1..*
	DEFAULT("http://inova8.com/odata4sparql#DEFAULT"), HALYARD_ES("http://inova8.com/odata4sparql#Halyard_ElasticSearch"), RDF4J_LUCENE(
			"http://inova8.com/odata4sparql#RDF4J_Lucene");

	private final String code;

	private TextSearchType(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

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
