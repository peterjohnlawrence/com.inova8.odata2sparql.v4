/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlProcessor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmProperty;
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
import org.apache.olingo.server.api.deserializer.FixedFormatDeserializer;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.PrimitiveValueProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.UriResourceProperty;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;

/**
 * The Class SparqlPrimitiveValueProcessor.
 */
public class SparqlPrimitiveValueProcessor implements PrimitiveValueProcessor {
	
	/** The rdf edm provider. */
	private final RdfEdmProvider rdfEdmProvider;
	
	/** The odata. */
	private OData odata;
	
	/** The service metadata. */
	private ServiceMetadata serviceMetadata;

	/**
	 * Instantiates a new sparql primitive value processor.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 */
	public SparqlPrimitiveValueProcessor(RdfEdmProvider rdfEdmProvider) {
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
	 * Read primitive value.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param responseFormat the response format
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
	@Override
	public void readPrimitiveValue(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		try {
			this.readPrimitiveOrValue(request, response, uriInfo, responseFormat, true);
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
	 * Read primitive.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param responseFormat the response format
	 * @throws ODataApplicationException the o data application exception
	 * @throws ODataLibraryException the o data library exception
	 */
	@Override
	public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		try {
			this.readPrimitiveOrValue(request, response, uriInfo, responseFormat, false);
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
	 * Read primitive or value.
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
	private void readPrimitiveOrValue(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat, Boolean isValue) throws EdmException, ODataException, OData2SparqlException {
		// 1. Retrieve info from URI
		// 1.1. retrieve the info about the requested entity set
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		RdfResourceParts rdfResourceParts  = new RdfResourceParts(this.rdfEdmProvider,uriInfo);
		UriType uriType = null;

		// 1.2. retrieve the requested (Edm) property
		// the second to last segment is the Property, if the last is $value
		int minSize = 1;
		if (rdfResourceParts.isValueRequest()) {
			minSize++;
		}	
		
		UriResourceProperty uriProperty = (UriResourceProperty) resourceParts.get(resourceParts.size() - minSize);
		EdmProperty edmProperty = uriProperty.getProperty();
		String edmPropertyName = edmProperty.getName();
		EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();
		
		// 2. retrieve data from backend
		// 2.1. retrieve the entity data, for which the property has to be read
		Entity entity = null;
		try {
			uriType = rdfResourceParts.getUriType();
			entity = SparqlBaseCommand.readEntity(rdfEdmProvider, uriInfo, uriType,rdfResourceParts);
		} catch (EdmException | OData2SparqlException | ODataException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		}
		if (entity == null) {
			throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}
		// 2.2. retrieve the property data from the entity
		Property property = null;
		if(uriType.equals(UriType.URI3) || uriType.equals(UriType.URI4)) {
			property = entity.getProperty(rdfResourceParts.getLastComplexType().getName());
			if(property!=null) {
				ComplexValue complexValue = (ComplexValue )(property.getValue());
				for( Property propertyValue: complexValue.getValue()) {
					if(propertyValue.getName().equals(edmPropertyName)) {
						property = propertyValue;
						break;
					}
				}	
			}
		}else {
			property = entity.getProperty(edmPropertyName);
		}
		
		if (property == null && !isValue) {
			throw new ODataApplicationException("Property " + edmPropertyName + " not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}

		// 3. serialize
		if (isValue) {
			writePropertyValue(rdfResourceParts, response, property);
		} else {
			writeProperty(rdfResourceParts, request, response, responseFormat,  edmPropertyName, edmPropertyType, property);
		}

	}

	/**
	 * Write property value.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param response the response
	 * @param property the property
	 * @throws ODataApplicationException the o data application exception
	 */
	private void writePropertyValue(RdfResourceParts rdfResourceParts, ODataResponse response, Property property) throws ODataApplicationException {
		if (property == null) {
			//ByteArrayInputStream serializerContent = new ByteArrayInputStream(null);//Charset.forName("UTF-8")));
			//response.setContent(serializerContent);
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
			//throw new ODataApplicationException("No property found", HttpStatusCode.NOT_FOUND.getStatusCode(),	Locale.ENGLISH);
		} else {
			if (property.getValue() == null) {
				response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
			} else {
				String value = String.valueOf(property.getValue());
				ByteArrayInputStream serializerContent = new ByteArrayInputStream(value.getBytes());//Charset.forName("UTF-8")));
				response.setContent(serializerContent);
				response.setStatusCode(HttpStatusCode.OK.getStatusCode());
				response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
			}
		}
	}

	/**
	 * Write property.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @param request the request
	 * @param response the response
	 * @param responseFormat the response format
	 * @param edmPropertyName the edm property name
	 * @param edmPropertyType the edm property type
	 * @param property the property
	 * @throws SerializerException the serializer exception
	 * @throws ODataApplicationException the o data application exception
	 */
	private void writeProperty(RdfResourceParts rdfResourceParts, ODataRequest request, ODataResponse response, ContentType responseFormat,
			String edmPropertyName, EdmPrimitiveType edmPropertyType, Property property)
			throws SerializerException, ODataApplicationException {
		Object value = property.getValue();
		if (value != null) {

			// 3.1. configure the serializer
			ODataSerializer serializer = odata.createSerializer(responseFormat);
			ContextURL contextUrl = rdfResourceParts.contextUrl(request,odata) ;
			PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().contextURL(contextUrl).build();
			// 3.2. serialize
			SerializerResult serializerResult = serializer.primitive(serviceMetadata, edmPropertyType, property,
					options);
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
	 * Update primitive.
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
	public void updatePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourcePrimitiveProperty uriResourcePrimitiveProperty = (UriResourcePrimitiveProperty) resourcePaths.get(resourcePaths.size()-1);
		EdmProperty edmProperty = uriResourcePrimitiveProperty.getProperty();

		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.property(requestInputStream, edmProperty);
		//TODO we need to better handle different formats of the object provided
		this.updatePrimitiveOrValue(request, response, uriInfo, result.getProperty().getValue().toString(), false);
	}

	/**
	 * Update primitive value.
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
	public void updatePrimitiveValue(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourcePrimitiveProperty uriResourcePrimitiveProperty = (UriResourcePrimitiveProperty) resourcePaths.get(resourcePaths.size()-1);
		EdmProperty edmProperty = uriResourcePrimitiveProperty.getProperty();

		InputStream requestInputStream = request.getBody();
		FixedFormatDeserializer deserializer = this.odata.createFixedFormatDeserializer();
		Object result = deserializer.primitiveValue(requestInputStream, edmProperty);

		this.updatePrimitiveOrValue(request, response, uriInfo, result, true);
	}

	/**
	 * Update primitive or value.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param entry the entry
	 * @param isValue the is value
	 * @throws ODataApplicationException the o data application exception
	 */
	private void updatePrimitiveOrValue(ODataRequest request, ODataResponse response, UriInfo uriInfo, Object entry,
			Boolean isValue) throws ODataApplicationException {
		try {
			SparqlBaseCommand.updatePrimitiveValue(rdfEdmProvider, uriInfo, entry);
		} catch (Exception e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NO_CONTENT.getStatusCode(),
					Locale.ENGLISH);
		}
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	/**
	 * Delete primitive value.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public void deletePrimitiveValue(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException {
		this.deletePrimitiveOrValue(request, response, uriInfo, false);
	}

	/**
	 * Delete primitive.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public void deletePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException{
		this.deletePrimitiveOrValue(request, response, uriInfo, true);
	}

	/**
	 * Delete primitive or value.
	 *
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param isValue the is value
	 * @throws ODataApplicationException the o data application exception
	 */
	private void deletePrimitiveOrValue(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			Boolean isValue) throws ODataApplicationException {
		try {
			SparqlBaseCommand.deletePrimitiveValue(rdfEdmProvider, uriInfo);
		} catch (Exception e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NO_CONTENT.getStatusCode(),
					Locale.ENGLISH);
		}
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}
}
