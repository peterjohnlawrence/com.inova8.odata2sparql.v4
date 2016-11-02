package com.inova8.odata2sparql.SparqlProcessor;

import java.util.Locale;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.uri.UriInfo;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlBuilder.SparqlQueryBuilder;

public class SparqlPrimitiveProcessor implements PrimitiveProcessor {
	public SparqlPrimitiveProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}
	private RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;
	private SparqlQueryBuilder sparqlBuilder;
	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		// TODO Auto-generated method stub
		throw new ODataApplicationException(new Object(){}.getClass().getEnclosingMethod().getName() + " not yet implemented",
                HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);

	}

	@Override
	public void updatePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException,
			ODataLibraryException {
		// TODO Auto-generated method stub
		throw new ODataApplicationException(new Object(){}.getClass().getEnclosingMethod().getName() + " not yet implemented",
                HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);

	}

	@Override
	public void deletePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		// TODO Auto-generated method stub
		throw new ODataApplicationException(new Object(){}.getClass().getEnclosingMethod().getName() + " not yet implemented",
                HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
	}

}
