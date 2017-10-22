package com.inova8.odata2sparql.SparqlProcessor;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.etag.ETagHelper;
import org.apache.olingo.server.api.etag.ServiceMetadataETagSupport;
import org.apache.olingo.server.api.processor.ErrorProcessor;
import org.apache.olingo.server.api.processor.MetadataProcessor;
import org.apache.olingo.server.api.processor.ServiceDocumentProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.uri.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparqlDefaultProcessor implements MetadataProcessor, ServiceDocumentProcessor, ErrorProcessor {
	private OData odata;
	private ServiceMetadata serviceMetadata;
	private final Logger log = LoggerFactory.getLogger(SparqlErrorProcessor.class);

	@Override
	public void init(final OData odata, final ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readServiceDocument(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
			final ContentType requestedContentType) throws ODataApplicationException, ODataLibraryException {
		boolean isNotModified = false;
		ServiceMetadataETagSupport eTagSupport = serviceMetadata.getServiceMetadataETagSupport();
		if (eTagSupport != null && eTagSupport.getServiceDocumentETag() != null) {
			// Set application etag at response
			response.setHeader(HttpHeader.ETAG, eTagSupport.getServiceDocumentETag());
			// Check if service document has been modified
			ETagHelper eTagHelper = odata.createETagHelper();
			isNotModified = eTagHelper.checkReadPreconditions(eTagSupport.getServiceDocumentETag(),
					request.getHeaders(HttpHeader.IF_MATCH), request.getHeaders(HttpHeader.IF_NONE_MATCH));
		}

		// Send the correct response req.getRequestURL().toString().replace(req.getServletPath(), "")
		if (isNotModified) {
			response.setStatusCode(HttpStatusCode.NOT_MODIFIED.getStatusCode());
		} else {
			ODataSerializer serializer = odata.createSerializer(requestedContentType);
			//Provide serviceRoot with rawBaseUri as Excel PowerQuery does not like relative URIs
			response.setContent(serializer.serviceDocument(serviceMetadata, request.getRawBaseUri()).getContent());
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, requestedContentType.toContentTypeString());
		}
	}

	@Override
	public void readMetadata(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
			final ContentType requestedContentType) throws ODataApplicationException, ODataLibraryException {
		boolean isNotModified = false;
		ServiceMetadataETagSupport eTagSupport = serviceMetadata.getServiceMetadataETagSupport();
		if (eTagSupport != null && eTagSupport.getMetadataETag() != null) {
			// Set application etag at response
			response.setHeader(HttpHeader.ETAG, eTagSupport.getMetadataETag());
			// Check if metadata document has been modified
			ETagHelper eTagHelper = odata.createETagHelper();
			isNotModified = eTagHelper.checkReadPreconditions(eTagSupport.getMetadataETag(),
					request.getHeaders(HttpHeader.IF_MATCH), request.getHeaders(HttpHeader.IF_NONE_MATCH));
		}

		// Send the correct response
		if (isNotModified) {
			response.setStatusCode(HttpStatusCode.NOT_MODIFIED.getStatusCode());
		} else {
			ODataSerializer serializer = odata.createSerializer(requestedContentType);
			response.setContent(serializer.metadataDocument(serviceMetadata).getContent());
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, requestedContentType.toContentTypeString());
		}
	}

	@Override
	public void processError(ODataRequest request, ODataResponse response, ODataServerError serverError,
			ContentType responseFormat) {
		try {
			ODataSerializer serializer = odata.createSerializer(responseFormat);
			response.setContent(serializer.error(serverError).getContent());
			response.setStatusCode(serverError.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
			switch (serverError.getStatusCode()) {
			case 404:
				break;
			default:
				//Only log severe errors, not 404 etc
				if (serverError.getException() != null) {
					log.error(request.getMethod() + ": " + request.getRawRequestUri() + " Error: "
							+ serverError.getException().getMessage() + "\nCause: "
							+ serverError.getException().getCause());
				} else {
					log.error(request.getMethod() + ": " + request.getRawRequestUri() + " Error: "
							+ serverError.getException().getMessage());
				}
			}

		} catch (Exception e) {
			// This should never happen but to be sure we have this catch here to prevent sending a stacktrace to a client.
			String responseContent = "{\"error\":{\"code\":null,\"message\":\"An unexpected exception occurred during error processing\"}}";
			response.setContent(new ByteArrayInputStream(responseContent.getBytes(Charset.forName("utf-8"))));
			response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_JSON.toContentTypeString());
		}
	}
}