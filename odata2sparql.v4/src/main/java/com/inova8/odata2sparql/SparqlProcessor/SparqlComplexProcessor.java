package com.inova8.odata2sparql.SparqlProcessor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmComplexType;
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
import org.apache.olingo.server.api.processor.ComplexCollectionProcessor;
import org.apache.olingo.server.api.processor.ComplexProcessor;
import org.apache.olingo.server.api.serializer.ComplexSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlComplexProcessor implements ComplexProcessor,ComplexCollectionProcessor {
	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;

	public SparqlComplexProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		try {
			this.readComplexValue(request, response, uriInfo, responseFormat, false);
		} catch (EdmException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		} catch (ODataException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		}

	}
	@Override
	public void readComplexCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);
		
	}

	private void readComplexValue(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat, Boolean isValue) throws EdmException, ODataException {
		// 1. Retrieve info from URI
		// 1.1. retrieve the info about the requested entity set
		RdfResourceParts rdfResourceParts = new RdfResourceParts(this.rdfEdmProvider, uriInfo);
		EdmEntitySet edmEntitySet = rdfResourceParts.getResponseEntitySet();

		// 1.2. retrieve the requested (Edm) property
		EdmComplexType edmComplexType = rdfResourceParts.getLastComplexType();
		String edmPropertyName = edmComplexType.getName();

		// 2. retrieve data from backend
		// 2.1. retrieve the entity data, for which the property has to be read

		Entity entity = null;
		try {
			entity = SparqlBaseCommand.readEntity(rdfEdmProvider, uriInfo, UriType.URI3,rdfResourceParts);
		} catch (EdmException | OData2SparqlException | ODataException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		}
		if (entity == null) {
			throw new ODataApplicationException("Property not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}
		// 2.2. retrieve the property data from the entity
		Property property = entity.getProperty(edmPropertyName);
		if (property == null) {
			throw new ODataApplicationException("Property not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}

		// 3. serialize
		if (isValue) {
			writeComplexValue(response, property);
		} else {
			writeProperty(request, response, responseFormat, edmEntitySet, edmPropertyName, edmComplexType, property,
					uriInfo, rdfResourceParts);
		}
	}

	private void writeComplexValue(ODataResponse response, Property property) throws ODataApplicationException {
		if (property == null) {
			throw new ODataApplicationException("No property found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		} else {
			if (property.getValue() == null) {
				response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
			} else {
				String value = String.valueOf(property.getValue());
				ByteArrayInputStream serializerContent = new ByteArrayInputStream(value.getBytes());
				response.setContent(serializerContent);
				response.setStatusCode(HttpStatusCode.OK.getStatusCode());
				response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
			}
		}
	}

	private void writeProperty(ODataRequest request, ODataResponse response, ContentType responseFormat,
			EdmEntitySet edmEntitySet, String edmPropertyName, EdmComplexType edmComplexType, Property property,
			UriInfo uriInfo, RdfResourceParts rdfResourceParts) throws SerializerException, ODataApplicationException {
		Object value = property.getValue();
		if (value != null) {

			// 3.1. configure the serializer
			ODataSerializer serializer = odata.createSerializer(responseFormat);
			ContextURL contextUrl = null;
			try {
				//Need absolute URI for PowerQuery and Linqpad (and probably other MS based OData clients)
				contextUrl = ContextURL.with().entitySet(edmEntitySet).keyPath(rdfResourceParts.getLocalKey())
						.navOrPropertyPath(rdfResourceParts.getNavPath())
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
			} catch (URISyntaxException e) {
				throw new ODataApplicationException("Invalid RawBaseURI " + request.getRawBaseUri(),
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			}
			ComplexSerializerOptions options = ComplexSerializerOptions.with().select(uriInfo.getSelectOption())
					.expand(uriInfo.getExpandOption()).contextURL(contextUrl).build();
			// 3.2. serialize
			SerializerResult serializerResult = serializer.complex(serviceMetadata, edmComplexType, property, options);
			InputStream propertyStream = serializerResult.getContent();

			//4. configure the response object
			response.setContent(propertyStream);
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		} else {
			// in case there's no value for the property, we can skip the serialization
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		}
	}

	@Override
	public void updateComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);

	}

	@Override
	public void deleteComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);

	}



	@Override
	public void updateComplexCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);
		
	}

	@Override
	public void deleteComplexCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);
		
	}
}
