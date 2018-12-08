package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;

public class SparqlEntityProcessor implements EntityProcessor {
	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;

	public SparqlEntityProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		// 1. retrieve the Entity Type
		RdfResourceParts rdfResourceParts  = new RdfResourceParts(this.rdfEdmProvider, uriInfo);		
		EdmEntitySet responseEdmEntitySet =  rdfResourceParts.getResponseEntitySet();;
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();
		// 2. retrieve the data from backend
		Entity entity = null;
		try {
			entity = SparqlBaseCommand.readEntity(rdfEdmProvider, uriInfo,
					rdfResourceParts.getUriType());
			if (entity == null)
				throw new OData2SparqlException("No data found");
		} catch (EdmException | OData2SparqlException | ODataException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}
		// 3. serialize
		ContextURL contextUrl = null;
		try {
			//Need absolute URI for PowerQuery and Linqpad (and probably other MS based OData clients)
			String selectList = odata.createUriHelper().buildContextURLSelectList(responseEdmEntitySet.getEntityType(), expandOption,
					selectOption);
			contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).keyPath(rdfResourceParts.getLocalKey()).navOrPropertyPath(rdfResourceParts.getNavPath()).selectList(selectList)
					.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
		} catch (URISyntaxException e) {
			throw new ODataApplicationException("Inavlid RawBaseURI " + request.getRawBaseUri(),
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
		}

		EntitySerializerOptions options = EntitySerializerOptions.with().select(selectOption).expand(expandOption)
				.contextURL(contextUrl).build();

		ODataSerializer serializer = odata.createSerializer(responseFormat);
		SerializerResult serializerResult = serializer.entity(serviceMetadata, responseEdmEntitySet.getEntityType(), entity, options);
		InputStream entityStream = serializerResult.getContent();
		//4. configure the response object
		response.setContent(entityStream);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

	@Override
	public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		// 2. update the data in backend
		// 2.1. retrieve the payload from the PUT request for the entity to be updated
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
		Entity requestEntity = result.getEntity();
		// Note that this updateEntity()-method is invoked for both PUT or PATCH operations
		HttpMethod httpMethod = request.getMethod();

		try {
			SparqlBaseCommand.updateEntity(rdfEdmProvider, uriInfo, requestEntity, httpMethod);
		} catch (EdmException | OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NO_CONTENT.getStatusCode(),
					Locale.ENGLISH);
		}

		//3. configure the response object
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	@Override
	public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve the entity type from the URI
		EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		// 2. create the data in backend
		// 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
		Entity requestEntity = result.getEntity();
		// 2.2 do the creation in backend, which returns the newly created entity

		Entity createdEntity = null;
		try {
			createdEntity = SparqlBaseCommand.writeEntity(rdfEdmProvider, uriInfo, requestEntity);
			if (createdEntity == null)
				throw new OData2SparqlException("Entity not created");
		} catch (EdmException | OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NO_CONTENT.getStatusCode(),
					Locale.ENGLISH);
		}

		// 3. serialize the response (we have to return the created entity)
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
		// expand and select currently not supported
		EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

		ODataSerializer serializer = this.odata.createSerializer(responseFormat);
		SerializerResult serializedResponse = serializer.entity(serviceMetadata, edmEntityType, createdEntity, options);

		//4. configure the response object
		response.setContent(serializedResponse.getContent());
		response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

	@Override
	public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {

		try {
			SparqlBaseCommand.deleteEntity(rdfEdmProvider, uriInfo);
		} catch (OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NO_CONTENT.getStatusCode(),
					Locale.ENGLISH);
		}

		//3. configure the response object
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());

	}
}
