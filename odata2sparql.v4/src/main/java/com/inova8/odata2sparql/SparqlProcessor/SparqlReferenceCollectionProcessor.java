package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.AbstractEntityCollection;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
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
import org.apache.olingo.server.api.processor.ReferenceCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.ReferenceCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourceProperty;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlReferenceCollectionProcessor implements ReferenceCollectionProcessor{
	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;
	public SparqlReferenceCollectionProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}
	
	
	@Override
	public void readReferenceCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		// 1. Retrieve info from URI
		// 1.1. retrieve the info about the requested entity set
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriEntityset = (UriResourceEntitySet) resourceParts.get(0);
		EdmEntitySet edmEntitySet = uriEntityset.getEntitySet();

		// 1.2. retrieve the requested (Edm) property
		// the second to last segment is the Property, if the last is $value
//		UriResource lastResourcePart = resourceParts.get(resourceParts.size() - 1);
//		int minSize = 1;
//		if (lastResourcePart.getSegmentValue().equals("$ref")) {
//			minSize++;
//		}
//		UriResourceNavigation uriNavigation = (UriResourceNavigation) resourceParts.get(resourceParts.size() - minSize);
//		EdmNavigationProperty edmNavigationProperty = uriNavigation.getProperty();
//		String edmPropertyName = edmNavigationProperty.getName();

		// 2. retrieve data from backend
		// 2.1. retrieve the entityCollection data
	
		AbstractEntityCollection entityCollection;
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
//			 String selectList = odata.createUriHelper().buildContextURLSelectList(responseEdmEntityType,
//					 expandOption, selectOption);
			contextUrl = ContextURL.with().serviceRoot(new URI(request.getRawBaseUri()+"/")).build();
		} catch (URISyntaxException e) {
			throw new ODataApplicationException("Inavlid RawBaseURI "+ request.getRawBaseUri(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
		}

		ReferenceCollectionSerializerOptions opts = ReferenceCollectionSerializerOptions.with().contextURL(contextUrl)
				.build();
		SerializerResult serializerResult = serializer
				.referenceCollection(serviceMetadata, edmEntitySet,entityCollection, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		
	}

}
