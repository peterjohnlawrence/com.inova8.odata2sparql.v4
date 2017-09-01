package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.batch.BatchFacade;
import org.apache.olingo.server.api.deserializer.batch.BatchOptions;
import org.apache.olingo.server.api.deserializer.batch.BatchRequestPart;
import org.apache.olingo.server.api.deserializer.batch.ODataResponsePart;
import org.apache.olingo.server.api.processor.BatchProcessor;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;

public class SparqlBatchProcessor implements BatchProcessor {
	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;

	public SparqlBatchProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void processBatch(BatchFacade facade, ODataRequest request, ODataResponse response)
			throws ODataApplicationException, ODataLibraryException {

		// 1. Extract the boundary
		final String boundary = facade.extractBoundaryFromContentType(request.getHeader(HttpHeader.CONTENT_TYPE));
		// 2. Prepare the batch options
		final BatchOptions options = BatchOptions.with().rawBaseUri(request.getRawBaseUri())
				.rawServiceResolutionUri(request.getRawServiceResolutionUri()).build();

		// 3. Deserialize the batch request
		final List<BatchRequestPart> requestParts = odata.createFixedFormatDeserializer()
				.parseBatchRequest(request.getBody(), boundary, options);

		// 4. Execute the batch request parts
		final List<ODataResponsePart> responseParts = new ArrayList<ODataResponsePart>();
		for (final BatchRequestPart part : requestParts) {
			responseParts.add(facade.handleBatchRequest(part));
		}

		// 5. Create a new boundary for the response
		final String responseBoundary = "batch_" + UUID.randomUUID().toString();

		// 6. Serialize the response content
		final InputStream responseContent = odata.createFixedFormatSerializer().batchResponse(responseParts,
				responseBoundary);

		// 7. Setup response
		response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.MULTIPART_MIXED + ";boundary=" + responseBoundary);
		response.setContent(responseContent);
		response.setStatusCode(HttpStatusCode.ACCEPTED.getStatusCode());

	}

	@Override
	public ODataResponsePart processChangeSet(BatchFacade facade, List<ODataRequest> requests)
			throws ODataApplicationException, ODataLibraryException {
	    final List<ODataResponse> responses = new ArrayList<ODataResponse>();
	    try {
	        //TODO storage.beginTransaction();

	        for(final ODataRequest request : requests) {
	            // Actual request dispatching to the other processor interfaces.
	            final ODataResponse response = facade.handleODataRequest(request);

	            // Determine if an error occurred while executing the request.
	            // Exceptions thrown by the processors get caught and result in a proper OData response.
	            final int statusCode = response.getStatusCode();
	            if(statusCode < 400) {
	                // The request has been executed successfully. Return the response as a part of the change set
	                responses.add(response);
	            } else {
	                // Something went wrong. Undo all previous requests in this Change Set
	            	//TODO storage.rollbackTranscation();

	               /*
	                * In addition the response must be provided as follows:
	                * 
	                * OData Version 4.0 Part 1: Protocol Plus Errata 02
	                *     11.7.4 Responding to a Batch Request
	                *
	                *     When a request within a change set fails, the change set response is not represented using
	                *     the multipart/mixed media type. Instead, a single response, using the application/http media type
	                *     and a Content-Transfer-Encoding header with a value of binary, is returned that
	                *     applies to all requests in the change set and MUST be formatted according to the Error Handling
	                *     defined for the particular response format.
	                *     
	                * This can be simply done by passing the response of the failed ODataRequest to a new instance of 
	                * ODataResponsePart and setting the second parameter "isChangeSet" to false.
	                */
	                return new ODataResponsePart(response, false);
	            }
	        }

	        // Everything went well, so commit the changes.
	        //TODO  storage.commitTransaction();
	        return new ODataResponsePart(responses, true);

	    } catch(ODataApplicationException e) {
	        // See below
	    	//TODO storage.rollbackTranscation();
	        throw e;
	    } catch(ODataLibraryException e) {
	        // The batch request is malformed or the processor implementation is not correct.
	        // Throwing an exception will stop the whole batch request not only the Change Set!
	    	//TODO storage.rollbackTranscation();
	        throw e;
	    }
	}

}
