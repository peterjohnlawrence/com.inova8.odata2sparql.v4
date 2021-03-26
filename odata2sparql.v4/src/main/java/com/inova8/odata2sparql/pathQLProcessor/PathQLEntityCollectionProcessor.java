/*
 * inova8 2020
 */
package com.inova8.odata2sparql.pathQLProcessor;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.antlr.v4.runtime.RecognitionException;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.intelligentgraph.intelligentGraphRepository.IntelligentGraphRepository;
import com.inova8.intelligentgraph.pathQLModel.Fact;
import com.inova8.intelligentgraph.pathQLModel.MatchFact;
import com.inova8.intelligentgraph.pathQLModel.Resource;
import com.inova8.intelligentgraph.pathQLModel.Thing;
import com.inova8.intelligentgraph.pathQLResults.FactResults;
import com.inova8.intelligentgraph.pathQLResults.MatchResults;
import com.inova8.intelligentgraph.pathQLResults.ResourceResults;
import com.inova8.odata2sparql.Constants.PathQLConstants;
import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.uri.RdfResourceEntitySet;
import com.inova8.odata2sparql.uri.RdfResourceFunction;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriUtils;
import com.inova8.pathql.parser.Match;
import com.inova8.pathql.parser.PathQLEvaluator;
import com.inova8.pathql.processor.PathPatternException;

/**
 * The Class PathQLEntityCollectionProcessor.
 */
public class PathQLEntityCollectionProcessor {
	
	/** The Constant log. */
	private final static Logger log = LoggerFactory.getLogger(PathQLEntityCollectionProcessor.class);
	
	/** The rdf edm provider. */
	private static RdfEdmProvider rdfEdmProvider;

	/**
	 * Process entity collection function.
	 *
	 * @param serviceMetadata the service metadata
	 * @param odata the odata
	 * @param rdfEdmProvider the rdf edm provider
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param responseFormat the response format
	 * @param rdfResourceParts the rdf resource parts
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws PathPatternException the path pattern exception
	 * @throws ODataException the o data exception
	 * @throws URISyntaxException the URI syntax exception
	 */
	public static void processEntityCollectionFunction(ServiceMetadata serviceMetadata, OData odata,
			RdfEdmProvider rdfEdmProvider, ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat, RdfResourceParts rdfResourceParts) throws OData2SparqlException, PathPatternException, ODataException, URISyntaxException {

		PathQLEntityCollectionProcessor.rdfEdmProvider = rdfEdmProvider;
		RdfResourceFunction function = (RdfResourceFunction) (rdfResourceParts.getLastResourcePart());
		EdmEntitySet responseEntitySet = rdfResourceParts.getResponseEntitySet();
		Repository repository = rdfEdmProvider.getRdfRepository().getDataRepository().getRepository();
		IntelligentGraphRepository source = new IntelligentGraphRepository(repository);
		String defaultPrefix = rdfEdmProvider.getRdfRepository().getDefaultPrefix();
		//PathQLRepository.setPrefixes(rdfEdmProvider.getRdfModel().getRdfPrefixes().getIRIPrefixes());
		//PathQLRepository.getPrefixes().put("", rdfEdmProvider.getRdfModel().getRdfPrefixes().getIRIPrefixes().get(defaultPrefix));
		EntityCollection entityCollection = null;
		
		switch (function.getFunction().getFunction().getName()) {
		case PathQLConstants.PATHQL_PATHQL_FNAME: 
			entityCollection = processPathQLQuery(rdfEdmProvider, rdfResourceParts, source);
			break;
//		case PathQLConstants.PATHQL_FACTFUNCTION_FNAME: 
//			entityCollection = processFactQuery(rdfEdmProvider, rdfResourceParts, source);
//			break;
		case PathQLConstants.PATHQL_FACTSFUNCTION_FNAME: 
			entityCollection = processFactsQuery(rdfEdmProvider, rdfResourceParts, source);
			break;


		default:
			log.error("Unsupported Function");
			throw new ODataApplicationException("Unsupported Function", HttpStatusCode.BAD_REQUEST.getStatusCode(),
					Locale.ENGLISH);
		}
		try {
			ODataSerializer serializer;

			serializer = odata.createSerializer(responseFormat);

			EdmEntityType edmEntityType = responseEntitySet.getEntityType();

			ExpandOption expandOption = uriInfo.getExpandOption();
			SelectOption selectOption = uriInfo.getSelectOption();
			//String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, expandOption,selectOption);

			ContextURL contextUrl = rdfResourceParts.contextUrl(request, odata);
			final String id = request.getRawBaseUri() + "/" + responseEntitySet.getName();
			EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id)
					.contextURL(contextUrl).select(selectOption).expand(expandOption).build();
			SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType,
					entityCollection, opts);
			InputStream serializedContent = serializerResult.getContent();
			response.setContent(serializedContent);
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		} catch (SerializerException e) {
			log.error("SerializerException");
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
					Locale.ENGLISH);
		}
	}

	/**
	 * Process fact querys.
	 *
	 * @param rdfEdmProvider2 the rdf edm provider 2
	 * @param rdfResourceParts the rdf resource parts
	 * @param source the source
	 * @return the entity collection
	 * @throws ODataApplicationException the o data application exception
	 */
//	private static EntityCollection processFactQuerys(RdfEdmProvider rdfEdmProvider2, RdfResourceParts rdfResourceParts,
//			IntelligentGraphRepository source) throws ODataApplicationException {
//		try {
//			EntityCollection entityCollection = null;
//			RdfResourceEntitySet penultimateResourcePart = (RdfResourceEntitySet) rdfResourceParts
//					.getPenultimateResourcePart();
//			String searchString = penultimateResourcePart.getKeyPredicates().get(0).getText();
//
//			@SuppressWarnings("deprecation")
//			MatchResults matchResultsIterator = Match.entityMatch(searchString);
//			
//			while(matchResultsIterator.hasNext() ) {
//				MatchFact nextMatch = matchResultsIterator.nextResource();
//				String thisKey = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(nextMatch.getSubject().stringValue(), RdfConstants.QNAME_SEPARATOR);
//				RdfResourceFunction lastResourcePart = (RdfResourceFunction) rdfResourceParts.getLastResourcePart();
//				String pathQL = ODataArgumentToString(lastResourcePart.getKeyPredicates().get(0).getText());
//				IRI thisIRI = UriUtils.keyToIri(thisKey, rdfEdmProvider.getRdfModel().getRdfPrefixes());
//				Thing $this =  Thing.create(source, thisIRI, null, rdfEdmProvider.getRdfModel().getRdfPrefixes().getIRIPrefixes());
//				String defaultPrefix = rdfEdmProvider.getRdfModel().getRdfPrefixes().getPrefixes().get(rdfEdmProvider.getRdfRepository().getDefaultPrefix());
//				$this.prefix(defaultPrefix);
//				FactResults facts = (FactResults) $this.getFacts(pathQL);
//				entityCollection = resourcesToEntitySet(facts);	
//			}
//
//			return entityCollection;
//		} catch (ODataException e) {
//			log.info("No data found");
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
//					Locale.ENGLISH);
//		} catch (PathPatternException e) {
//			log.error("Path exception");
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
//					Locale.ENGLISH);
//		} catch (OData2SparqlException e) {
//			log.error("URISyntaxException");
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
//					Locale.ENGLISH);
//		}
//	}
	
	/**
	 * Process fact query.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param rdfResourceParts the rdf resource parts
	 * @param source the source
	 * @return the entity collection
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws PathPatternException the path pattern exception
	 * @throws ODataException the o data exception
	 */
//	private static EntityCollection processFactQuery(RdfEdmProvider rdfEdmProvider, RdfResourceParts rdfResourceParts,
//			IntelligentGraphRepository source) throws OData2SparqlException, PathPatternException, ODataException {
//		try {
//			EntityCollection entityCollection;
//			RdfResourceEntitySet penultimateResourcePart = (RdfResourceEntitySet) rdfResourceParts
//					.getPenultimateResourcePart();
//			String thisKey = penultimateResourcePart.getKeyPredicates().get(0).getText();
//			RdfResourceFunction lastResourcePart = (RdfResourceFunction) rdfResourceParts.getLastResourcePart();
//			String pathQL = ODataArgumentToString(lastResourcePart.getKeyPredicates().get(0).getText());
//			IRI thisIRI = UriUtils.keyToIri(thisKey, rdfEdmProvider.getRdfModel().getRdfPrefixes());
//			Thing $this =Thing.create(source, thisIRI, null, rdfEdmProvider.getRdfModel().getRdfPrefixes().getIRIPrefixes());
//			String defaultPrefix = rdfEdmProvider.getRdfModel().getRdfPrefixes().getPrefixes().get(rdfEdmProvider.getRdfRepository().getDefaultPrefix());
//			$this.prefix(defaultPrefix);
//			FactResults facts = (FactResults) $this.getFacts(pathQL);
//			entityCollection = resourcesToEntitySet(facts);
//			return entityCollection;
//		} catch (ODataException e) {
//			log.info("No data found");
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
//					Locale.ENGLISH);
//		} catch (PathPatternException e) {
//			log.error("Path exception");
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
//					Locale.ENGLISH);
//		} catch (OData2SparqlException e) {
//			log.error("URISyntaxException");
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
//					Locale.ENGLISH);
//		}
//	}
	
	/**
	 * Process facts query.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param rdfResourceParts the rdf resource parts
	 * @param source the source
	 * @return the entity collection
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws PathPatternException the path pattern exception
	 * @throws ODataException the o data exception
	 * @throws URISyntaxException the URI syntax exception
	 */
	private static EntityCollection processFactsQuery(RdfEdmProvider rdfEdmProvider, RdfResourceParts rdfResourceParts,
			IntelligentGraphRepository source) throws OData2SparqlException, PathPatternException, ODataException, URISyntaxException {
		try {
			EntityCollection entityCollection;
			RdfResourceEntitySet penultimateResourcePart = (RdfResourceEntitySet) rdfResourceParts
					.getPenultimateResourcePart();
			RdfResourceFunction lastResourcePart = (RdfResourceFunction) rdfResourceParts.getLastResourcePart();
			String pathQL = ODataArgumentToString(lastResourcePart.getKeyPredicates().get(0).getText());
			ResourceResults resources = PathQLEvaluator.evaluate(source,"[ a :" + penultimateResourcePart.getEntitySetName() + "]/" + pathQL);
			entityCollection = resourcesToEntitySet(resources);
			return entityCollection;
		} catch (ODataException e) {
			log.info("No data found");
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		} catch (PathPatternException e) {
			log.error("Path exception");
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
					Locale.ENGLISH);
		}
	}
	
	/**
	 * Process path QL query.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param rdfResourceParts the rdf resource parts
	 * @param source the source
	 * @return the entity collection
	 * @throws ODataException the o data exception
	 * @throws URISyntaxException the URI syntax exception
	 * @throws RecognitionException the recognition exception
	 * @throws PathPatternException the path pattern exception
	 */
	private static EntityCollection processPathQLQuery(RdfEdmProvider rdfEdmProvider, RdfResourceParts rdfResourceParts,IntelligentGraphRepository source)
			throws ODataException, URISyntaxException, RecognitionException, PathPatternException {
		try {
			EntityCollection entityCollection;
			RdfResourceFunction lastResourcePart = (RdfResourceFunction) rdfResourceParts.getLastResourcePart();
			String pathQL = ODataArgumentToString(RdfResourceParts.getParameter(lastResourcePart.getKeyPredicates().get(0)));
			ResourceResults resources = PathQLEvaluator.evaluate(source,pathQL);
			entityCollection = resourcesToEntitySet(resources);
			return entityCollection;
		} catch (ODataException e) {
			log.info("No data found");
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
					Locale.ENGLISH);
		} catch (URISyntaxException e) {
			log.info("URISyntaxException");
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
					Locale.ENGLISH);
		}
	}
	
	/**
	 * Process match query.
	 *
	 * @param rdfResourceParts the rdf resource parts
	 * @return the entity collection
	 * @throws ODataException the o data exception
	 * @throws URISyntaxException the URI syntax exception
	 */
//	@Deprecated
//	private static EntityCollection processMatchQuery(RdfResourceParts rdfResourceParts)
//			throws ODataException, URISyntaxException {
//		try {
//			EntityCollection entityCollection;
//			RdfResourceFunction lastResourcePart = (RdfResourceFunction) rdfResourceParts.getLastResourcePart();
//			String searchString = ODataArgumentToString(RdfResourceParts.getParameter(lastResourcePart.getKeyPredicates().get(0)));
//			MatchResults matchResultsIterator = Match.entityMatch(searchString);
//			entityCollection = resourcesToEntitySet(matchResultsIterator);
//			return entityCollection;
//		} catch (ODataException e) {
//			log.info("No data found");
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.NOT_FOUND.getStatusCode(),
//					Locale.ENGLISH);
//		} catch (URISyntaxException e) {
//			log.info("URISyntaxException");
//			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(),
//					Locale.ENGLISH);
//		}
//	}

	/**
	 * O data argument to string.
	 *
	 * @param agrgument the agrgument
	 * @return the string
	 */
	public static String ODataArgumentToString(String agrgument) {

		if (agrgument.endsWith("'")) {
			agrgument = agrgument.substring(1, agrgument.length() - 1);
			return agrgument;
		} else {
			return agrgument;
		}
	}

	/**
	 * Resources to entity set.
	 *
	 * @param facts the facts
	 * @return the entity collection
	 * @throws ODataException the o data exception
	 */
	static EntityCollection resourcesToEntitySet(FactResults facts) throws ODataException {
		EntityCollection entityCollection = new EntityCollection();
		List<Entity> entityList = entityCollection.getEntities();
		while ((facts!=null) &&  facts.hasNext()) {
			Fact nextFact = (Fact) facts.nextResource();
			Entity entity = resourceToEntity(nextFact);
			entityList.add(entity);
		}
		return entityCollection;
	}
	
	/**
	 * Resources to entity set.
	 *
	 * @param matches the matches
	 * @return the entity collection
	 * @throws ODataException the o data exception
	 * @throws URISyntaxException the URI syntax exception
	 */
	@Deprecated
	static EntityCollection resourcesToEntitySet(MatchResults matches)
			throws ODataException, URISyntaxException {
		EntityCollection entityCollection = new EntityCollection();
		List<Entity> entityList = entityCollection.getEntities();
		while ((matches!=null) &&  matches.hasNext()) {
			MatchFact nextMatch = (MatchFact) matches.nextResource();
			Entity entity = resourceToEntity( nextMatch);
			entityList.add(entity);
		}
		return entityCollection;
	}

	/**
	 * Resources to entity set.
	 *
	 * @param resources the resources
	 * @return the entity collection
	 * @throws ODataException the o data exception
	 * @throws URISyntaxException the URI syntax exception
	 */
	static EntityCollection resourcesToEntitySet(ResourceResults resources)
			throws ODataException, URISyntaxException {
		EntityCollection entityCollection = new EntityCollection();
		List<Entity> entityList = entityCollection.getEntities();
		
		while ((resources!=null) && resources.hasNext()) {	
			Resource nextResource= resources.nextResource();
			Entity entity = resourceToEntity(nextResource);
			entityList.add(entity);
		}
		return entityCollection;

	}
	
	/**
	 * Resource to entity.
	 *
	 * @param resource the resource
	 * @return the entity
	 */
	static Entity resourceToEntity(Resource resource) {

		String className = resource.getClass().getSimpleName();
		switch(className ) {
			case "Fact": return resourceToEntity((Fact) resource) ;
			case "MatchFact": return resourceToEntity((MatchFact) resource) ;
			case "Resource": 
			default:break;
		}
		Entity entity = new Entity();
		String uri = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(resource.getValue().stringValue(),
				RdfConstants.QNAME_SEPARATOR);
		try {
			entity.setId(new URI(uri));
			entity.addProperty(new Property(null, RdfConstants.SUBJECT, ValueType.PRIMITIVE, uri));
			entity.addProperty(new Property(null, RdfConstants.RDFS_LABEL_LABEL, ValueType.PRIMITIVE, uri));
		} catch (URISyntaxException e) {
			return null;
		}

		return entity;

	}

	/**
	 * Resource to entity.
	 *
	 * @param nextMatch the next match
	 * @return the entity
	 */
	private static Entity resourceToEntity(MatchFact nextMatch) {
		Entity entity = new Entity();
		entity.addProperty(
				new Property(null, RdfConstants.RDFS_LABEL_LABEL, ValueType.PRIMITIVE, nextMatch.toString()));

		String subjectId = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(nextMatch.getSubject().stringValue(), RdfConstants.QNAME_SEPARATOR);
		entity.addProperty(new Property(null,  RdfConstants.SUBJECT, ValueType.PRIMITIVE, subjectId));	
		if(nextMatch.getPredicate().stringValue()!=null) {
			String predicateId = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(nextMatch.getPredicate().stringValue(), RdfConstants.QNAME_SEPARATOR);
			entity.addProperty(new Property(null,  PathQLConstants.PATHQL_FACT_PROPERTYID_NAME, ValueType.PRIMITIVE, predicateId));
		}
		entity.addProperty(new Property(null, PathQLConstants.PATHQL_FACT_SNIPPET_NAME, ValueType.PRIMITIVE,
				nextMatch.getSnippet()));
		entity.addProperty(new Property(null, PathQLConstants.PATHQL_FACT_SCORE_NAME, ValueType.PRIMITIVE,
				nextMatch.getScore()));
		entity.setId(nextMatch.getId());

		addLinkedEntity(nextMatch.getSubject(), entity, PathQLConstants.PATHQL_FACT_SUBJECT_NAME);
		if(nextMatch.getPredicate().stringValue()!=null)  addLinkedEntity(nextMatch.getPredicate(), entity, PathQLConstants.PATHQL_FACT_PROPERTY_NAME);
		//addValueEntity(nextMatch.getValue(), entity);
		return entity;
	}


	/**
	 * Resource to entity.
	 *
	 * @param nextFact the next fact
	 * @return the entity
	 */
	private static Entity resourceToEntity(Fact nextFact) {
		Entity entity = new Entity();
		entity.addProperty(
				new Property(null, RdfConstants.RDFS_LABEL_LABEL, ValueType.PRIMITIVE, nextFact.toString()));

		String subjectId = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(nextFact.getSubject().stringValue(), RdfConstants.QNAME_SEPARATOR);
		entity.addProperty(new Property(null,  RdfConstants.SUBJECT, ValueType.PRIMITIVE, subjectId));		
		String predicateId = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(nextFact.getPredicate().stringValue(), RdfConstants.QNAME_SEPARATOR);
		entity.addProperty(new Property(null,  PathQLConstants.PATHQL_FACT_PROPERTYID_NAME, ValueType.PRIMITIVE, predicateId));

		entity.setId(nextFact.getId());

		addLinkedEntity(nextFact.getSubject(), entity, PathQLConstants.PATHQL_FACT_SUBJECT_NAME);
		addLinkedEntity(nextFact.getPredicate(), entity, PathQLConstants.PATHQL_FACT_PROPERTY_NAME);
		addValueEntity(nextFact.getValue(), entity);
		return entity;
	}


	/**
	 * Iri to entity.
	 *
	 * @param iri the iri
	 * @return the entity
	 */
	static Entity iriToEntity(IRI iri) {
		Entity entity = new Entity();
		String uri = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(iri.stringValue(),
				RdfConstants.QNAME_SEPARATOR);
		try {
			entity.setId(new URI(uri));
			entity.addProperty(new Property(null, RdfConstants.SUBJECT, ValueType.PRIMITIVE, uri));
			entity.addProperty(new Property(null, RdfConstants.RDFS_LABEL_LABEL, ValueType.PRIMITIVE, uri));
		} catch (URISyntaxException e) {
			return null;
		}
		return entity;
	}
	
	/**
	 * Adds the linked entity.
	 *
	 * @param resource the resource
	 * @param entity the entity
	 * @param navigationProperty the navigation property
	 */
	private static void addLinkedEntity(Resource resource, Entity entity, String navigationProperty) {
		Entity linkedEntity = resourceToEntity(resource);
		Link link = new Link();
		link.setTitle(navigationProperty);
		link.setInlineEntity(linkedEntity);
		entity.getNavigationLinks().add(link);
	}
	
	/**
	 * Adds the value entity.
	 *
	 * @param value the value
	 * @param entity the entity
	 */
	private static void addValueEntity(Value value, Entity entity) {
		if (value.isResource()) {
			Entity valueEntity = iriToEntity((IRI) value);
			Link link = new Link();
			link.setTitle(PathQLConstants.PATHQL_FACT_OBJECT_NAME);
			link.setInlineEntity(valueEntity);
			entity.getNavigationLinks().add(link);
			String objectId = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(value.stringValue(), RdfConstants.QNAME_SEPARATOR);
			entity.addProperty(new Property(null, PathQLConstants.PATHQL_FACT_OBJECTID_NAME, ValueType.PRIMITIVE,
					objectId));
		} else {
			entity.addProperty(new Property(null, PathQLConstants.PATHQL_FACT_LITERAL_NAME, ValueType.PRIMITIVE,
					value.stringValue()));
		}
	}

	/**
	 * Count entity collection.
	 *
	 * @param serviceMetadata the service metadata
	 * @param odata the odata
	 * @param rdfEdmProvider the rdf edm provider
	 * @param request the request
	 * @param response the response
	 * @param uriInfo the uri info
	 * @param rdfResourceParts the rdf resource parts
	 */
	public static void countEntityCollection(ServiceMetadata serviceMetadata, OData odata,
			RdfEdmProvider rdfEdmProvider, ODataRequest request, ODataResponse response, UriInfo uriInfo,
			RdfResourceParts rdfResourceParts) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Fact to entity.
	 *
	 * @param nextResource the next resource
	 * @return the entity
	 */
	@SuppressWarnings("unused")
	private static Entity factToEntity(Resource nextResource) {
		Entity entity = new Entity();
		entity.addProperty(
				new Property(null, RdfConstants.RDFS_LABEL_LABEL, ValueType.PRIMITIVE, nextResource.toString()));

		String subjectId = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(nextResource.getSubject().stringValue(), RdfConstants.QNAME_SEPARATOR);
		entity.addProperty(new Property(null,  RdfConstants.SUBJECT, ValueType.PRIMITIVE, subjectId));	
		String predicateId = rdfEdmProvider.getRdfModel().getRdfPrefixes().toQName(nextResource.getPredicate().stringValue(), RdfConstants.QNAME_SEPARATOR);
		entity.addProperty(new Property(null,  PathQLConstants.PATHQL_FACT_PROPERTYID_NAME, ValueType.PRIMITIVE, predicateId));
//		entity.addProperty(new Property(null, PathQLConstants.PATHQL_FACT_SNIPPET_NAME, ValueType.PRIMITIVE,
//				nextResource.getSnippet()));
//		entity.addProperty(new Property(null, PathQLConstants.PATHQL_FACT_SCORE_NAME, ValueType.PRIMITIVE,
//				nextResource.getScore()));
		//entity.setId(nextResource.getId());

		addLinkedEntity(nextResource.getSubject(), entity, PathQLConstants.PATHQL_FACT_SUBJECT_NAME);
		addLinkedEntity(nextResource.getPredicate(), entity, PathQLConstants.PATHQL_FACT_PROPERTY_NAME);
		addValueEntity(nextResource.getValue(), entity);
		return entity;
	}
}
