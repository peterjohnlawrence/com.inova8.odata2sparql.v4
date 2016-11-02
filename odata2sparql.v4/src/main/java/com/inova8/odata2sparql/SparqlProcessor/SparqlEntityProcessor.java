package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmException;
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
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.SparqlBuilder.SparqlQueryBuilder;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlEntityProcessor implements EntityProcessor {
	public SparqlEntityProcessor(RdfEdmProvider rdfEdmProvider) {
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
	public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
	    // 1. retrieve the Entity Type
	    List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
	    // Note: only in our example we can assume that the first segment is the EntitySet
	    UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
	    EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

	    // 2. retrieve the data from backend
	    List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
	    Entity entity = null;
		try {
			entity = readEntity(uriInfo);
		} catch (EdmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OData2SparqlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ODataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//storage.readEntityData(edmEntitySet, keyPredicates);

	    // 3. serialize
	    EdmEntityType entityType = edmEntitySet.getEntityType();

	    ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
	    // expand and select currently not supported
	    EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

	    ODataSerializer serializer = odata.createSerializer(responseFormat);
	    SerializerResult serializerResult = serializer.entity(serviceMetadata, entityType, entity, options);
	    InputStream entityStream = serializerResult.getContent();

	    //4. configure the response object
	    response.setContent(entityStream);
	    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
	    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

	@Override
	public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		// TODO Auto-generated method stub

	}
	@Override 
	public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException,
			ODataLibraryException {
		// TODO Auto-generated method stub

	}

	public Entity readEntity(final UriInfo uriInfo) throws EdmException, OData2SparqlException, ODataException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		UriType uriType;
		if (uriInfo.getUriResourceParts().size() > 1) {
			uriType = UriType.URI6A;
		} else {
			uriType = UriType.URI2;
		}		
		
		this.sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),rdfEdmProvider.getEdmMetadata(),uriInfo, uriType);
		
		//prepareQuery
		SparqlStatement sparqlStatement = null;
		if (resourcePaths.size() == 1) {
			UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
			edmEntitySet = uriResourceEntitySet.getEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		} else if (resourcePaths.size() == 2) {
			//TODO surely not limited to just 2?
			UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(1);
			edmEntitySet = uriResourceEntitySet.getEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		}
		try {
			sparqlStatement = this.sparqlBuilder.prepareConstructSparql();
		} catch (OData2SparqlException e) {
			throw new ODataRuntimeException( e.getMessage());
		}
		SparqlResults rdfResults = null;
		rdfResults = SparqlBaseCommand.executeQuery(rdfEdmProvider,rdfEntityType, sparqlStatement,uriInfo.getExpandOption(), uriInfo.getSelectOption());
		return rdfResults.getEntity();
		
//		Map<String, Object> data = rdfResults.getEntityResults();
//		if (data == null) {
//			throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
//		} else {
//			ExpandSelectTreeNode expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
//			Map<String, ODataCallback> callbacks = locateCallbacks(expandSelectTreeNode, rdfResults);
//			return EntityProvider.writeEntry(contentType, edmEntitySet, data, buildEntityProperties(expandSelectTreeNode, callbacks)
//					.build());
//		}
	}
}
