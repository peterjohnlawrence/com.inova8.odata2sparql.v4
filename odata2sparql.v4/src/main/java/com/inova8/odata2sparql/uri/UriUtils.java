package com.inova8.odata2sparql.uri;

import com.inova8.odata2sparql.Constants.RdfConstants;

public class UriUtils {
public static String encodeUri(String uri) {
	return uri.replaceAll("\\(", "%28").replaceAll("\\)","%29").replaceAll("\\/", "%2F");
}
public static String odataToRdfQname(String odataQname ) {
	return odataQname.replace("'", "")
			.replaceAll(RdfConstants.QNAME_SEPARATOR_ENCODED, RdfConstants.QNAME_SEPARATOR_RDF);
}
}
