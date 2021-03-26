package com.inova8.odata2sparql.uri;

import java.io.UnsupportedEncodingException;
import static org.eclipse.rdf4j.model.util.Values.iri;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.IRI;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrefixes;

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
public static String objectToSubjectId(String objectId ) {
	String subjectId = objectId.split("\\(",0)[1].split("\\)",0)[0];
	return subjectId.substring(1,subjectId.length()-1);
}
public static String objectToSubjectUri(String objectId, RdfPrefixes rdfPrefixes  ) throws OData2SparqlException {
	return rdfPrefixes.expandPredicate(objectToSubjectId(objectId));
}
public static IRI subjectToIri(String subjectId, RdfPrefixes rdfPrefixes  ) throws OData2SparqlException {
	return iri(rdfPrefixes.expandPredicate(subjectId));
}
public static IRI keyToIri(String subjectId, RdfPrefixes rdfPrefixes  ) throws OData2SparqlException {
	return iri(rdfPrefixes.expandPredicateKey(subjectId));
}
}
