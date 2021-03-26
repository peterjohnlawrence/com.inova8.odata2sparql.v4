package com.inova8.odata2sparql.Processor;

import java.net.URISyntaxException;
import java.util.Locale;

import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.ex.ODataException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlProcessor.SparqlEntityCollectionProcessor;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import pathPatternProcessor.PathPatternException;

import com.inova8.odata2sparql.pathQLProcessor.PathQLEntityCollectionProcessor;

public class EntityCollectionProcessor implements CountEntityCollectionProcessor {
	private final Logger log = LoggerFactory.getLogger(EntityCollectionProcessor.class);
	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;

	public EntityCollectionProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		// 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)

		RdfResourceParts rdfResourceParts = null;
		try {
			rdfResourceParts = new RdfResourceParts(this.rdfEdmProvider, uriInfo);
		} catch (EdmException|ODataException|OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
					Locale.ENGLISH);
		} 
		if (rdfResourceParts.isFunction()) {
			try {
				PathQLEntityCollectionProcessor.processEntityCollectionFunction(serviceMetadata,odata,rdfEdmProvider, request, response, uriInfo, responseFormat, rdfResourceParts);
			} catch (OData2SparqlException|PathPatternException|ODataException|URISyntaxException e) {
				throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
						Locale.ENGLISH);
			} 

		} else {
			SparqlEntityCollectionProcessor.processEntityCollectionQuery(serviceMetadata,odata,rdfEdmProvider,request, response, uriInfo, responseFormat, rdfResourceParts);
		}

	}

	@Override
	public void countEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {

		RdfResourceParts rdfResourceParts = null;
		try {
			rdfResourceParts = new RdfResourceParts(this.rdfEdmProvider, uriInfo);
		} catch (EdmException |ODataException|OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
					Locale.ENGLISH);
		} 
		if (rdfResourceParts.isFunction()) {
			PathQLEntityCollectionProcessor.countEntityCollection( serviceMetadata, odata,rdfEdmProvider, request,  response,  uriInfo, rdfResourceParts);

		} else {
			SparqlEntityCollectionProcessor.countEntityCollection(serviceMetadata, odata,rdfEdmProvider, request,  response,  uriInfo, rdfResourceParts);
		}
		


	}

}
