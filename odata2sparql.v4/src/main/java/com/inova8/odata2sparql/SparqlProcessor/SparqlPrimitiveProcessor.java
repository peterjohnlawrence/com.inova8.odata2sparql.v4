package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
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
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourceProperty;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.uri.UriType;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;

public class SparqlPrimitiveProcessor implements PrimitiveProcessor {
	public SparqlPrimitiveProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve info from URI
		// 1.1. retrieve the info about the requested entity set
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriEntityset = (UriResourceEntitySet) resourceParts.get(0);
		EdmEntitySet edmEntitySet = uriEntityset.getEntitySet();

		// 1.2. retrieve the requested (Edm) property
		// the last segment is the Property
		UriResourceProperty uriProperty = (UriResourceProperty) resourceParts.get(resourceParts.size() - 1);
		EdmProperty edmProperty = uriProperty.getProperty();
		String edmPropertyName = edmProperty.getName();
		EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();

		// 2. retrieve data from backend
		// 2.1. retrieve the entity data, for which the property has to be read
		Entity entity = null;
		try {
			entity = SparqlBaseCommand.readEntity(rdfEdmProvider, uriInfo, UriType.URI5);
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
		Object value = property.getValue();
		if (value != null) {

			// 3.1. configure the serializer
			ODataSerializer serializer = odata.createSerializer(responseFormat);
			ContextURL contextUrl = null;
			try {
				//Need absolute URI for PowewrQuery and Linqpad (and probably other MS based OData clients)
				contextUrl = ContextURL.with().entitySet(edmEntitySet).navOrPropertyPath(edmPropertyName)
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
			} catch (URISyntaxException e) {
				throw new ODataApplicationException("Inavlid RawBaseURI " + request.getRawBaseUri(),
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			}

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

	@Override
	public void updatePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException,
			ODataLibraryException {
		// TODO Auto-generated method stub
		throw new ODataApplicationException(new Object() {
		}.getClass().getEnclosingMethod().getName() + " not yet implemented", HttpStatusCode.NOT_FOUND.getStatusCode(),
				Locale.ENGLISH);

	}

	@Override
	public void deletePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		// TODO Auto-generated method stub
		throw new ODataApplicationException(new Object() {
		}.getClass().getEnclosingMethod().getName() + " not yet implemented", HttpStatusCode.NOT_FOUND.getStatusCode(),
				Locale.ENGLISH);
	}

}
