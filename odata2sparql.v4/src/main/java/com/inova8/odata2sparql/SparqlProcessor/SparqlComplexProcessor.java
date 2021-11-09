/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlProcessor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ComplexValue;
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

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;
import com.inova8.odata2sparql.uri.RdfResourceComplexProperty;
import com.inova8.odata2sparql.uri.RdfResourcePart;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;

/**
 * The Class SparqlComplexProcessor.
 */
public class SparqlComplexProcessor implements ComplexProcessor, ComplexCollectionProcessor {
	
	/** The rdf edm provider. */
	private final RdfEdmProvider rdfEdmProvider;
	
	/** The odata. */
	private OData odata;
	
	/** The service metadata. */
	private ServiceMetadata serviceMetadata;

	/**
	 * Instantiates a new sparql complex processor.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 */
	public SparqlComplexProcessor(RdfEdmProvider rdfEdmProvider) {
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
	 * Read complex.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param responseFormat the response format
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
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
		} catch (OData2SparqlException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		}

	}

	/**
	 * Read complex collection.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param responseFormat the response format
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
	@Override
	public void readComplexCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve info from URI
		// 1.1. retrieve the info about the requested entity set
		RdfResourceParts rdfResourceParts = null;
		try {
			rdfResourceParts = new RdfResourceParts(this.rdfEdmProvider, uriInfo);
		} catch (EdmException | ODataException | OData2SparqlException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		EdmEntitySet edmEntitySet = rdfResourceParts.getResponseEntitySet();

		// 1.2. retrieve the requested (Edm) property
		EdmComplexType edmComplexType = rdfResourceParts.getLastComplexType();
		String edmPropertyName = edmComplexType.getName().replace(RdfConstants.SHAPE_POSTFIX, "");

		// 2. retrieve data from backend
		// 2.1. retrieve the entity data, for which the property has to be read

		Entity entity = null;
		try {
			entity = SparqlBaseCommand.readEntity(rdfEdmProvider, uriInfo, UriType.URI3, rdfResourceParts);
		} catch (EdmException | OData2SparqlException | ODataException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		}
		if (entity == null) {
			throw new ODataApplicationException("Property not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}
		// 2.2. retrieve the property data from the entity
		//rdfResourceParts.getLastComplexProperty()
		Property property = navigateToComplexProperty(entity, rdfResourceParts);//entity.getProperty(edmPropertyName);
		if (property == null) {
			throw new ODataApplicationException("Property not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}

		// 3. serialize

		writeCollection(request, response, responseFormat, edmEntitySet, edmPropertyName, edmComplexType, property,
				uriInfo, rdfResourceParts);

	}
	
	/**
	 * Write collection.
	 *
	 * @param request the request
	 * @param response the response
	 * @param responseFormat the response format
	 * @param edmEntitySet the edm entity set
	 * @param edmPropertyName the edm property name
	 * @param edmComplexType the edm complex type
	 * @param property the property
	 * @param uriInfo the uri info
	 * @param rdfResourceParts the rdf resource parts
	 * @throws SerializerException the serializer exception
	 * @throws ODataApplicationException the o data application exception
	 */
	private void writeCollection(ODataRequest request, ODataResponse response, ContentType responseFormat,
			EdmEntitySet edmEntitySet, String edmPropertyName, EdmComplexType edmComplexType, Property property,
			UriInfo uriInfo, RdfResourceParts rdfResourceParts) throws SerializerException, ODataApplicationException {
		Object value = property.getValue();
		if (value != null) {

			// 3.1. configure the serializer
			ODataSerializer serializer = odata.createSerializer(responseFormat);
			ComplexSerializerOptions options = ComplexSerializerOptions.with().select(uriInfo.getSelectOption())
					.expand(uriInfo.getExpandOption()).contextURL(rdfResourceParts.contextUrl(request, odata)).build();
			// 3.2. serialize
			SerializerResult serializerResult = serializer.complexCollection(serviceMetadata, edmComplexType, property, options);
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
	
	/**
	 * Navigate to complex property.
	 *
	 * @param entity the entity
	 * @param rdfResourceParts the rdf resource parts
	 * @return the property
	 */
	private Property navigateToComplexProperty(Entity entity, RdfResourceParts rdfResourceParts) {
		ArrayList<RdfResourcePart> navPath = rdfResourceParts.getNavPath();
		if (navPath == null) {
			return null;
		} else {
			Property property = entity.getProperty(navPath.get(0).getNavPath());
			for (RdfResourcePart path : navPath.subList(1, navPath.size())) {
				RdfResourceComplexProperty complexProperty = (RdfResourceComplexProperty) path;
				List<Property> values = ((ComplexValue) property.getValue()).getValue();
				property = values.stream().filter(value -> complexProperty.getNavPath().equals(value.getName()))
						.findAny().orElse(null);
			}
			return property;
		}
	}

	/**
	 * Read complex value.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param responseFormat the response format
	 * @param isValue the is value
	 * @throws EdmException the edm exception
	 * @throws ODataException the o data exception
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private void readComplexValue(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat, Boolean isValue) throws EdmException, ODataException, OData2SparqlException {
		// 1. Retrieve info from URI
		// 1.1. retrieve the info about the requested entity set
		RdfResourceParts rdfResourceParts = new RdfResourceParts(this.rdfEdmProvider, uriInfo);
		EdmEntitySet edmEntitySet = rdfResourceParts.getResponseEntitySet();

		// 1.2. retrieve the requested (Edm) property
		EdmComplexType edmComplexType = rdfResourceParts.getLastComplexType();
		String edmPropertyName = edmComplexType.getName().replace(RdfConstants.SHAPE_POSTFIX, "");

		// 2. retrieve data from backend
		// 2.1. retrieve the entity data, for which the property has to be read

		Entity entity = null;
		try {
			entity = SparqlBaseCommand.readEntity(rdfEdmProvider, uriInfo, UriType.URI3, rdfResourceParts);
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

	/**
	 * Write complex value.
	 *
	 * @param response the response
	 * @param property the property
	 * @throws ODataApplicationException the o data application exception
	 */
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

	/**
	 * Write property.
	 *
	 * @param request the request
	 * @param response the response
	 * @param responseFormat the response format
	 * @param edmEntitySet the edm entity set
	 * @param edmPropertyName the edm property name
	 * @param edmComplexType the edm complex type
	 * @param property the property
	 * @param uriInfo the uri info
	 * @param rdfResourceParts the rdf resource parts
	 * @throws SerializerException the serializer exception
	 * @throws ODataApplicationException the o data application exception
	 */
	private void writeProperty(ODataRequest request, ODataResponse response, ContentType responseFormat,
			EdmEntitySet edmEntitySet, String edmPropertyName, EdmComplexType edmComplexType, Property property,
			UriInfo uriInfo, RdfResourceParts rdfResourceParts) throws SerializerException, ODataApplicationException {
		Object value = property.getValue();
		if (value != null) {

			// 3.1. configure the serializer
			ODataSerializer serializer = odata.createSerializer(responseFormat);
			//			ContextURL contextUrl = null;
			//			try {
			//				//Need absolute URI for PowerQuery and Linqpad (and probably other MS based OData clients)
			//				contextUrl = ContextURL.with().entitySet(edmEntitySet).keyPath(rdfResourceParts.getLocalKey())
			//						.navOrPropertyPath(rdfResourceParts.getNavPath())
			//						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
			//			} catch (URISyntaxException e) {
			//				throw new ODataApplicationException("Invalid RawBaseURI " + request.getRawBaseUri(),
			//						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			//			}
			ComplexSerializerOptions options = ComplexSerializerOptions.with().select(uriInfo.getSelectOption())
					.expand(uriInfo.getExpandOption()).contextURL(rdfResourceParts.contextUrl(request, odata)).build();
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

	/**
	 * Update complex.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param requestFormat the request format
	 * @param responseFormat the response format
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
	@Override
	public void updateComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);

	}

	/**
	 * Delete complex.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
	@Override
	public void deleteComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);

	}

	/**
	 * Update complex collection.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param requestFormat the request format
	 * @param responseFormat the response format
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
	@Override
	public void updateComplexCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);

	}

	/**
	 * Delete complex collection.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
	@Override
	public void deleteComplexCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		throw new ODataApplicationException("Not Implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
				Locale.ENGLISH);

	}
}
