package com.inova8.odata2sparql.SparqlProcessor;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.CountEntityCollectionProcessor;
import org.apache.olingo.server.api.uri.UriInfo;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;

public class SparqlCountEntityCollectionProcessor implements CountEntityCollectionProcessor {
	private final Log log = LogFactory.getLog(SparqlCountEntityCollectionProcessor.class);
	public SparqlCountEntityCollectionProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException(new Object(){}.getClass().getEnclosingMethod().getName() + " not yet implemented",
                HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public void countEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException(new Object(){}.getClass().getEnclosingMethod().getName() + " not yet implemented",
                HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);

	}

}
