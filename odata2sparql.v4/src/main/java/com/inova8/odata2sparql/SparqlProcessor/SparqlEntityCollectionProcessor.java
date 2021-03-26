package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
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
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.FixedFormatSerializer;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
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
import com.inova8.odata2sparql.uri.RdfResourceNavigationProperty;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;
import com.inova8.odata2sparql.SparqlStatement.SparqlBaseCommand;

public class SparqlEntityCollectionProcessor{// implements CountEntityCollectionProcessor {
	private final static Logger log = LoggerFactory.getLogger(SparqlEntityCollectionProcessor.class);
	public SparqlEntityCollectionProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
	}
//
//	@Override
//	public void init(OData odata, ServiceMetadata serviceMetadata) {
//		this.odata = odata;
//		this.serviceMetadata = serviceMetadata;
//	}
//
//	@Override
//	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
//			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
//		// 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
//
//		RdfResourceParts rdfResourceParts = null;
//		try {
//			rdfResourceParts = new RdfResourceParts(this.rdfEdmProvider, uriInfo);
//		} catch (EdmException e) {
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
//					Locale.ENGLISH);
//		} catch (ODataException e) {
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
//					Locale.ENGLISH);
//		} catch (OData2SparqlException e) {
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
//					Locale.ENGLISH);
//		}
//		if (rdfResourceParts.isFunction()) {
//			PathQLEntityCollectionProcessor.processEntityCollectionFunction( serviceMetadata,odata,rdfEdmProvider, request, response, uriInfo, responseFormat, rdfResourceParts);
//
//		} else {
//			processEntityCollectionQuery( serviceMetadata, odata,rdfEdmProvider,request, response, uriInfo, responseFormat, rdfResourceParts);
//		}
//
//	}


	public static void processEntityCollectionQuery(ServiceMetadata serviceMetadata,OData odata,RdfEdmProvider rdfEdmProvider,ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat, RdfResourceParts rdfResourceParts)
			throws ODataApplicationException, SerializerException {
		EdmEntitySet responseEdmEntitySet = rdfResourceParts.getResponseEntitySet();
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();
		CountOption countOption = uriInfo.getCountOption();
		// 2nd: fetch the data from backend for this requested EntitySetName
		// it has to be delivered as EntitySet object
		EntityCollection entitySet = null;
		try {
			entitySet = SparqlBaseCommand.readEntitySet(rdfEdmProvider, uriInfo, rdfResourceParts.getUriType(),
					rdfResourceParts);
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
		//If deltas requested then a delta serializer should be used
		//EdmDeltaSerializer edmDeltaSerializer = odata.createEdmDeltaSerializer(responseFormat, null);
		ODataSerializer serializer = odata.createSerializer(responseFormat);
		// Analyze the URI segments
		int segmentCount = rdfResourceParts.size();
		if (segmentCount == 3) { //navigation via complextype
			if (rdfResourceParts.getResourceKind(2).equals(UriResourceKind.navigationProperty)) {
				RdfResourceNavigationProperty rdfNavigationProperty = rdfResourceParts.getAsNavigationProperty(2);
				if (!rdfNavigationProperty.getEdmNavigationProperty().isCollection()) {

					//TODO ***********************************************************
					//Need to get the actual value of the complex property
					//First find the entity, then its complexproperty value which will be a collection of entities
					String entityString = rdfResourceParts.getEntityString();
					Entity resultsEntity = null;
					for (Entity entity : entitySet.getEntities()) {
						if (entity.getId().toString().equals(entityString)) {
							resultsEntity = entity;
							break;
						}
					}
					//Now its complexproperty value

					ComplexValue result = (ComplexValue) resultsEntity
							.getProperty(rdfResourceParts.getLastComplexType().getName())//rdfResourceParts.getAsComplexProperty(segmentCount - 2).getComplexType().getName())
							.getValue();
					entitySet = new EntityCollection();
					//Now its property value 
					String navigationPropertyName = rdfResourceParts.getAsNavigationProperty(segmentCount - 1)
							.getEdmNavigationProperty().getName();
					for (Property property : result.getValue()) {
						if (property.getName().equals(navigationPropertyName)) {
							entitySet = (EntityCollection) property.getValue();
						}
					}
				}
			}
		}

		// 5th: Now serialize the content: transform from the EntitySet object to InputStream	
		ContextURL contextUrl = rdfResourceParts.contextUrl(request, odata); //null;
		final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
		EntityCollectionSerializerOptions opts = null;

		if ((countOption != null) && countOption.getValue()) {
			opts = EntityCollectionSerializerOptions.with().select(selectOption).expand(expandOption).id(id)
					.count(countOption).contextURL(contextUrl).build();
		} else {
			opts = EntityCollectionSerializerOptions.with().select(selectOption).expand(expandOption).id(id)
					.contextURL(contextUrl).build();
		}

		SerializerResult serializerResult = serializer.entityCollection(serviceMetadata,
				responseEdmEntitySet.getEntityType(), entitySet, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

	private static void sortEntityCollection(EntityCollection entitySet, OrderByOption orderByOption) {
		List<Entity> entityList = entitySet.getEntities();
		List<OrderByItem> orderItemList = orderByOption.getOrders();
		final OrderByItem orderByItem = orderItemList.get(0); // we support only one
		Expression expression = orderByItem.getExpression();
		if (expression instanceof Member) {
			UriInfoResource resourcePath = ((Member) expression).getResourcePath();
			UriResource uriResource = resourcePath.getUriResourceParts().get(0);
			if (uriResource instanceof UriResourcePrimitiveProperty) {
				EdmProperty edmProperty = ((UriResourcePrimitiveProperty) uriResource).getProperty();
				final String sortPropertyName = edmProperty.getName();
				final String sortPropertyType = edmProperty.getType().getName();

				// do the sorting for the list of entities  
				Collections.sort(entityList, new Comparator<Entity>() {

					// delegate the sorting to native sorter of Integer and String
					public int compare(Entity entity1, Entity entity2) {
						Property property1 = entity1.getProperty(sortPropertyName);
						Property property2 = entity2.getProperty(sortPropertyName);
						if (property1 == null) {
							return (property2 == null) ? 0 : -1;
						}
						if (property2 == null) {
							return 1;
						}
						Object value1 = property1.getValue();
						Object value2 = property2.getValue();
						if (value1 == null) {
							return (value2 == null) ? 0 : -1;
						}
						if (value2 == null) {
							return 1;
						}
						int compareResult = 0;
						switch (sortPropertyType) {

						case "Integer":
						case "Int32":
							Integer integer1 = (Integer) value1;
							Integer integer2 = (Integer) entity2.getProperty(sortPropertyName).getValue();
							compareResult = integer1.compareTo(integer2);
							break;
						case "Decimal":
							BigDecimal decimal1 = (BigDecimal) value1;
							BigDecimal decimal2 = (BigDecimal) entity2.getProperty(sortPropertyName).getValue();
							compareResult = decimal1.compareTo(decimal2);
							break;
						case "Double":
							Double double1 = (Double) value1;
							Double double2 = (Double) entity2.getProperty(sortPropertyName).getValue();
							compareResult = double1.compareTo(double2);
							break;
						case "Boolean":
							Boolean boolean1 = (Boolean) value1;
							Boolean boolean2 = (Boolean) entity2.getProperty(sortPropertyName).getValue();
							compareResult = boolean1.compareTo(boolean2);
							break;
						case "DateTimeOffset":
							Timestamp offsetDateTime1 = (Timestamp) value1;
							Timestamp offsetDateTime2 = (Timestamp) entity2.getProperty(sortPropertyName).getValue();
							compareResult = offsetDateTime1.compareTo(offsetDateTime2);
							break;
						default:
							compareResult = ((String) value1).compareTo((String) value2);
							break;
						}
						if (orderByItem.isDescending()) {
							return -compareResult; // just reverse order
						}
						return compareResult;
					}
				});
			}
		}
	}

//	@Override
//	public void countEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo)
//			throws ODataApplicationException, ODataLibraryException {
//
//		// 2. retrieve data from backend
//		// 2.1. retrieve the entity data, for which the property has to be read
//		RdfLiteral count = null;
//		try {
//			count = SparqlBaseCommand.countEntitySet(rdfEdmProvider, uriInfo, UriType.URI15);
//		} catch (EdmException | OData2SparqlException | ExpressionVisitException e) {
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
//					Locale.ENGLISH);
//		} catch (ODataException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		// 3. serialize
//		if (count != null) {
//			// 3.1. configure the serializer
//			FixedFormatSerializer serializer = odata.createFixedFormatSerializer();
//			// 3.2. serialize
//			InputStream countStream = serializer.count(Integer.parseInt(count.getLexicalForm().toString()));
//
//			//4. configure the response object
//			response.setContent(countStream);
//			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
//			response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
//		} else {
//			// in case there's no value for the property, we can skip the serialization
//			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
//		}
//
//	}
	public static void countEntityCollection(ServiceMetadata serviceMetadata,OData odata,RdfEdmProvider rdfEdmProvider,ODataRequest request, ODataResponse response, UriInfo uriInfo, RdfResourceParts rdfResourceParts)
			throws ODataApplicationException, ODataLibraryException {

		// 2. retrieve data from backend
		// 2.1. retrieve the entity data, for which the property has to be read
		RdfLiteral count = null;
		try {
			count = SparqlBaseCommand.countEntitySet(rdfEdmProvider, uriInfo, UriType.URI15);
		} catch (EdmException | OData2SparqlException | ExpressionVisitException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		} catch (ODataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
