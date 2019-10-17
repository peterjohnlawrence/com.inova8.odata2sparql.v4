package com.inova8.odata2sparql.uri;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.inova8.odata2sparql.Constants.RdfConstants;

public class UriUtils {
public static String encodeQName(String uri) {
	return uri.replaceAll("\\(", "%28").replaceAll("\\)","%29").replaceAll("\\/", "%2F");
}
public static String encodeUri(String uri) throws UnsupportedEncodingException {
	return URLEncoder.encode(uri, StandardCharsets.UTF_8.toString()).replace("+", "%20").replace("%7E", "~");
}
public static String odataToRdfQname(String odataQname ) {
	return odataQname.replace("'", "")
			.replaceAll(RdfConstants.QNAME_SEPARATOR_ENCODED, RdfConstants.QNAME_SEPARATOR_RDF);
}
}
