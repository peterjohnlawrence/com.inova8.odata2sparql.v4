package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfConstructQuery;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfTripleSet;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.SparqlBuilder.SparqlQueryBuilder;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlEntityCollectionProcessor implements EntityCollectionProcessor {
	private final Log log = LogFactory.getLog(SparqlEntityCollectionProcessor.class);
	public SparqlEntityCollectionProcessor(RdfEdmProvider rdfEdmProvider) {
		super();
		this.rdfEdmProvider = rdfEdmProvider;
	}

	private RdfEdmProvider rdfEdmProvider;
	private OData odata;
	private ServiceMetadata serviceMetadata;
	private SparqlQueryBuilder sparqlBuilder;

	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		// 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// 2nd: fetch the data from backend for this requested EntitySetName
		// it has to be delivered as EntitySet object
		EntityCollection entitySet = null;
		try {
			entitySet = readEntitySet(  uriInfo);
		} catch (OData2SparqlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ODataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 3rd: create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// 4th: Now serialize the content: transform from the EntitySet object to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl)
				.build();
		SerializerResult serializerResult = serializer
				.entityCollection(serviceMetadata, edmEntityType, entitySet, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

	}
//TODO V2
//	public EntityCollection readEntitySet(ODataRequest request, ODataResponse response, UriInfo uriInfo,
//			ContentType responseFormat) throws OData2SparqlException, ODataException {
	public EntityCollection readEntitySet( UriInfo uriInfo) throws ODataException, OData2SparqlException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		UriType uriType;
		if (uriInfo.getUriResourceParts().size() > 1) {
			uriType = UriType.URI6B;
		} else {
			uriType = UriType.URI1;
		}
		this.sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(), rdfEdmProvider.getEdmMetadata(),
				uriInfo, uriType);

		//prepareQuery
		SparqlStatement sparqlStatement = null;
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		edmEntitySet = uriResourceEntitySet.getEntitySet();
		rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		try {
			sparqlStatement = this.sparqlBuilder.prepareConstructSparql();
		} catch (OData2SparqlException e) {
			throw new ODataRuntimeException(e.getMessage());
		}
		SparqlResults rdfResults = null;
		rdfResults = SparqlBaseCommand.executeQuery(rdfEdmProvider, rdfEntityType, sparqlStatement, uriInfo.getExpandOption(), uriInfo.getSelectOption());
		return rdfResults.getEntityCollection();
//TODO needs to be included
//		ExpandSelectTreeNode expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelectOption(),
//				uriInfo.getExpandOption());
//
//		Map<String, ODataCallback> callbacks = locateCallbacks(expandSelectTreeNode, rdfResults);
//		if (data == null) {
//			//TODO correct exception
//			throw new ODataRuntimeException("No data found");
//		} else {
//			return EntityProvider.writeFeed(contentType, edmEntitySet, data,
//					buildEntitySetProperties(expandSelectTreeNode, callbacks).build());
//		}
	}
//	private SparqlResults executeQuery(RdfEdmProvider sparqlEdmProvider,RdfEntityType entityType, SparqlStatement sparqlStatement,
////TODO V2			List<ArrayList<NavigationPropertySegment>> expand, List<SelectItem> select) throws OData2SparqlException {
//		ExpandOption expand, SelectOption select) throws OData2SparqlException {
//		RdfConstructQuery rdfQuery = new RdfConstructQuery(sparqlEdmProvider.getRdfRepository().getDataRepository(),
//				sparqlStatement.getSparql());
//		RdfTripleSet results;
//		try {
//			results = rdfQuery.execConstruct();
//		} catch (OData2SparqlException e) {
//			log.error(e.getMessage());
//			throw new ODataRuntimeException(e.getMessage(), null);
//		}
//		SparqlBaseCommand rdfBaseCommand = new SparqlBaseCommand();
//		return rdfBaseCommand.toOEntities(sparqlEdmProvider, entityType, results, expand,select);
//	}
}
