package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
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
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.ReferenceProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.ReferenceSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlReferenceProcessor implements ReferenceProcessor{
	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;
	public SparqlReferenceProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}
	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readReference(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve info from URI
		// 1.1. retrieve the info about the requested entity set
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriEntityset = (UriResourceEntitySet) resourceParts.get(0);
		EdmEntitySet edmEntitySet = uriEntityset.getEntitySet();

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
		ContextURL contextUrl = null;
		try {
			//Need absolute URI for PowewrQuery and Linqpad (and probably other MS based OData clients)
			contextUrl = ContextURL.with().serviceRoot(new URI(request.getRawBaseUri()+"/")).build();
		} catch (URISyntaxException e) {
			throw new ODataApplicationException("Inavlid RawBaseURI "+ request.getRawBaseUri(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
		}

		ReferenceSerializerOptions opts = ReferenceSerializerOptions.with().contextURL(contextUrl).build();
		SerializerResult serializerResult = serializer.reference(serviceMetadata, edmEntitySet,entityCollection.getEntities().get(0), opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());	
	}

	@Override
	public void createReference(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat) throws ODataApplicationException, ODataLibraryException {
		// 2. create the data in backend
		// 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.entityReferences(requestInputStream);
		List<URI> requestEntityReferences = result.getEntityReferences();
		// 2.2 do the creation in backend, 

		try {
			SparqlBaseCommand.writeEntityReference(rdfEdmProvider, uriInfo, requestEntityReferences);
		} catch (EdmException | OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NO_CONTENT.getStatusCode(),
					Locale.ENGLISH);
		}
		// 3. serialize the response
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	@Override
	public void updateReference(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat) throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve the entity type from the URI
//		EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
//		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		// 2. create the data in backend
		// 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.entityReferences(requestInputStream);
		List<URI> requestEntityReferences = result.getEntityReferences();
		// 2.2 do the creation in backend, 

		try {
			SparqlBaseCommand.updateEntityReference(rdfEdmProvider, uriInfo, requestEntityReferences);
		} catch (EdmException | OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NO_CONTENT.getStatusCode(),
					Locale.ENGLISH);
		}
		// 3. serialize the response
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	@Override
	public void deleteReference(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve the entity type from the URI
//		EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
//		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		try {
			SparqlBaseCommand.deleteEntityReference(rdfEdmProvider, uriInfo);
		} catch (EdmException | OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NO_CONTENT.getStatusCode(),
					Locale.ENGLISH);
		}
		// 3. serialize the response
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

}
