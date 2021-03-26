/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.ReferenceCollectionProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.ReferenceCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;

/**
 * The Class SparqlReferenceCollectionProcessor.
 */
public class SparqlReferenceCollectionProcessor implements ReferenceCollectionProcessor{
	
	/** The rdf edm provider. */
	private final RdfEdmProvider rdfEdmProvider;
	
	/** The odata. */
	private OData odata;
	
	/** The service metadata. */
	private ServiceMetadata serviceMetadata;
	
	/**
	 * Instantiates a new sparql reference collection processor.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 */
	public SparqlReferenceCollectionProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	/**
	 * Inits the.
	 *
	 * @param odata the odata
	 * @param serviceMetadata the service metadata
	 */
	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}
	
	
	/**
	 * Read reference collection.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param responseFormat the response format
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
	@Override 
	public void readReferenceCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve info from URI
		// 1.1. retrieve the info about the requested entity set
		RdfResourceParts rdfResourceParts = null;
		try {
			rdfResourceParts = new RdfResourceParts(this.rdfEdmProvider, uriInfo);
		} catch (EdmException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		} catch (ODataException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		} catch (OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		}	

		// 2. retrieve data from backend
		// 2.1. retrieve the entityCollection data
		EntityCollection entityCollection;
		try {
			entityCollection = SparqlBaseCommand.readReferenceCollection(rdfEdmProvider, uriInfo, UriType.URI7B);
		} catch (EdmException | OData2SparqlException | ODataException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		}
		if (entityCollection == null) {
			throw new ODataApplicationException("References not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}
		// 3. serialize
		ODataSerializer serializer = odata.createSerializer(responseFormat);
		ContextURL contextUrl = rdfResourceParts.contextUrl(request,odata) ;
		ReferenceCollectionSerializerOptions opts = ReferenceCollectionSerializerOptions.with().contextURL(contextUrl).build();
		SerializerResult serializerResult = serializer.referenceCollection(serviceMetadata, rdfResourceParts.getResponseEntitySet(),entityCollection, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());	
	}
}
