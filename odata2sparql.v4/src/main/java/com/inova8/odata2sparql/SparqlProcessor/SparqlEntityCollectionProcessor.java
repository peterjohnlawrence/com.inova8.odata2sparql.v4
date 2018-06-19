package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
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
import org.apache.olingo.server.api.processor.CountEntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.FixedFormatSerializer;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfLiteral;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.uri.UriType;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;

public class SparqlEntityCollectionProcessor implements CountEntityCollectionProcessor {
	private final Logger log = LoggerFactory.getLogger(SparqlEntityCollectionProcessor.class);
	private final RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;

	public SparqlEntityCollectionProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		// 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourceParts.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		EdmEntityType responseEdmEntityType = null;
		EdmEntitySet responseEdmEntitySet = null;
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();
		CountOption countOption = uriInfo.getCountOption();
		// 2nd: fetch the data from backend for this requested EntitySetName
		// it has to be delivered as EntitySet object
		EntityCollection entitySet = null;
		try {
			entitySet = SparqlBaseCommand.readEntitySet(this.rdfEdmProvider, uriInfo,
					(uriInfo.getUriResourceParts().size() > 1) ? UriType.URI6B : UriType.URI1);
		} catch (ODataException | OData2SparqlException e) {
			log.info("No data found");
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		}

		// 3rd apply $orderby
	    OrderByOption orderByOption = uriInfo.getOrderByOption();
	    if (orderByOption != null) {
	    	sortEntityCollection(entitySet, orderByOption);
	    }		
		// 4th: create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);
		// Analyze the URI segments
		int segmentCount = resourceParts.size();
		if (segmentCount == 1) { // no navigation
			responseEdmEntityType = edmEntitySet.getEntityType();
			responseEdmEntitySet = edmEntitySet; // since we have only one segment
		} else if (segmentCount == 2) { //navigation
			UriResource navSegment = resourceParts.get(1);
			if (navSegment instanceof UriResourceNavigation) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) navSegment;
				EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
				responseEdmEntityType = edmNavigationProperty.getType();
				responseEdmEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet, edmNavigationProperty);//SparqlBaseCommand.getNavigationTargetEntitySet(uriInfo);
			}
		} else {
			// this would be the case for e.g. Products(1)/Category/Products(1)/Category
			throw new ODataApplicationException("Not supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
					Locale.ROOT);
		}
		// 5th: Now serialize the content: transform from the EntitySet object to InputStream

		ContextURL contextUrl = null;
		try {
			//Need absolute URI for PowewrQuery and Linqpad (and probably other MS based OData clients)
			String selectList = odata.createUriHelper().buildContextURLSelectList(responseEdmEntityType, expandOption,
					selectOption);
			contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).selectList(selectList)
					.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
		} catch (URISyntaxException e) {
			throw new ODataApplicationException("Inavlid RawBaseURI " + request.getRawBaseUri(),
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
		}

		final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
		EntityCollectionSerializerOptions opts=null;

		if ((countOption != null) && countOption.getValue()) {
			opts = EntityCollectionSerializerOptions.with().select(selectOption).expand(expandOption).id(id).count(countOption)
					.contextURL(contextUrl).build();
		} else {
			opts = EntityCollectionSerializerOptions.with().select(selectOption).expand(expandOption).id(id)
					.contextURL(contextUrl).build();
		}

		SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, responseEdmEntityType,
				entitySet, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

	}

	private void sortEntityCollection(EntityCollection entitySet, OrderByOption orderByOption) {
		List<Entity> entityList = entitySet.getEntities();
		List<OrderByItem> orderItemList = orderByOption.getOrders();
		final OrderByItem orderByItem = orderItemList.get(0); // we support only one
		Expression expression = orderByItem.getExpression();
		if(expression instanceof Member){
		    UriInfoResource resourcePath = ((Member)expression).getResourcePath();
		    UriResource uriResource = resourcePath.getUriResourceParts().get(0);
		    if (uriResource instanceof UriResourcePrimitiveProperty) {
		        EdmProperty edmProperty = ((UriResourcePrimitiveProperty)uriResource).getProperty();
		        final String sortPropertyName = edmProperty.getName();
		        final String sortPropertyType = edmProperty.getType().getName();

		        // do the sorting for the list of entities  
		        Collections.sort(entityList, new Comparator<Entity>() {

		            // delegate the sorting to native sorter of Integer and String
		            public int compare(Entity entity1, Entity entity2) {
		                int compareResult = 0;

		                if(sortPropertyType.equals("Integer")){
		                    Integer integer1 = (Integer) entity1.getProperty(sortPropertyName).getValue();
		                    Integer integer2 = (Integer) entity2.getProperty(sortPropertyName).getValue();

		                    compareResult = integer1.compareTo(integer2);
		                }else if(sortPropertyType.equals("Double")){
		                	Float float1 = (Float) entity1.getProperty(sortPropertyName).getValue();
		                	Float float2 = (Float) entity2.getProperty(sortPropertyName).getValue();

		                    compareResult = float1.compareTo(float2);
		                }else{
		                    String propertyValue1 = (String) entity1.getProperty(sortPropertyName).getValue();
		                    String propertyValue2 = (String) entity2.getProperty(sortPropertyName).getValue();

		                    compareResult = propertyValue1.compareTo(propertyValue2);
		                }

		                // if 'desc' is specified in the URI, change the order
		                if(orderByItem.isDescending()){
		                    return - compareResult; // just reverse order
		                }

		                return compareResult;
		            }
		        });
		    }
		}
	}

	@Override
	public void countEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {

		// 2. retrieve data from backend
		// 2.1. retrieve the entity data, for which the property has to be read
		RdfLiteral count = null;
		try {
			count = SparqlBaseCommand.countEntitySet(rdfEdmProvider, uriInfo, UriType.URI15);
		} catch (EdmException | OData2SparqlException | ExpressionVisitException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		}
		// 3. serialize
		if (count != null) {
			// 3.1. configure the serializer
			FixedFormatSerializer serializer = odata.createFixedFormatSerializer();
			// 3.2. serialize
			InputStream countStream = serializer.count(Integer.parseInt(count.getLexicalForm().toString()));

			//4. configure the response object
			response.setContent(countStream);
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
		} else {
			// in case there's no value for the property, we can skip the serialization
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		}

	}

}
