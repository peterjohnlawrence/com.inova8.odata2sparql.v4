package com.inova8.odata2sparql.SparqlStatement;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfLiteral;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfQuerySolution;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.SparqlBuilder.SparqlCreateUpdateDeleteBuilder;
import com.inova8.odata2sparql.SparqlBuilder.SparqlQueryBuilder;
import com.inova8.odata2sparql.uri.RdfResourceParts;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlBaseCommand {
	private final static Logger log = LoggerFactory.getLogger(SparqlBaseCommand.class);

	static public EntityCollection readEntitySet(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, UriType uriType,
			RdfResourceParts rdfResourceParts) throws ODataException, OData2SparqlException {
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider, uriInfo, uriType, rdfResourceParts);

		//prepareQuery
		SparqlStatement sparqlStatement = null;

		switch (uriType) {
		case URI1:
			break;
		case URI4:
			break;
		case URI6B:
			break;
		case URI11:
			break;
		default:
			throw new ODataApplicationException("Unhandled URIType " + uriType,
					HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
		}
		try {
			sparqlStatement = sparqlBuilder.prepareConstructSparql();
		} catch (OData2SparqlException e) {
			throw new ODataRuntimeException(e.getMessage());
		}
		SparqlEntityCollection rdfResults = sparqlStatement.executeConstruct(rdfEdmProvider,
				rdfResourceParts.getResponseRdfEntityType(), 
				uriInfo.getExpandOption(), uriInfo.getSelectOption());
		return rdfResults.getEntityCollection();
	}

	static public Entity readEntity(RdfEdmProvider rdfEdmProvider, final UriInfo uriInfo, UriType uriType,
			RdfResourceParts rdfResourceParts) throws EdmException, OData2SparqlException, ODataException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		String key = null;
		Boolean knownKey = false; 
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider, uriInfo, uriType, rdfResourceParts);

		//prepareQuery
		SparqlStatement sparqlStatement = null;
		UriResourceEntitySet uriResourceEntitySet = null;
		switch (uriType) {
		case URI3:
		case URI4:
		case URI5:
			UriResource lastResourcePart = resourcePaths.get(resourcePaths.size() - 1);
			int minSize = 2;
			if (lastResourcePart.getSegmentValue().equals("$value")) {
				minSize++;
			}
			if (resourcePaths.size() > minSize) {
				UriResource penultimateSegment = resourcePaths.get(1);
				if (penultimateSegment.getKind().equals(UriResourceKind.complexProperty)) {
					//Complextype
					uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
					edmEntitySet = uriResourceEntitySet.getEntitySet();
					rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

				} else {
					UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
					FullQualifiedName edmEntityTypeFQN = uriResourceNavigation.getProperty().getType()
							.getFullQualifiedName();
					rdfEntityType = rdfEdmProvider.getMappedEntityType(edmEntityTypeFQN);
				}
			} else {
				uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
				edmEntitySet = uriResourceEntitySet.getEntitySet();
				rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			}
			key = rdfResourceParts.getTargetSubjectId();
			//key = uriResourceEntitySet.getKeyPredicates().get(0).getText();
			//key = key.substring(1, key.length() - 1);
			knownKey = (key != null);
			break;
		case URI2:
			uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
			edmEntitySet = uriResourceEntitySet.getEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			key = rdfResourceParts.getTargetSubjectId();
			knownKey = (key != null);
			break;
		case URI6A:
			UriResource lastSegment = resourcePaths.get(resourcePaths.size() - 1);
			UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;//resourcePaths.get(1);
			FullQualifiedName edmEntityTypeFQN = uriResourceNavigation.getProperty().getType().getFullQualifiedName();
			rdfEntityType = rdfEdmProvider.getMappedEntityType(edmEntityTypeFQN);
			if (!uriResourceNavigation.getKeyPredicates().isEmpty()) {
				key = uriResourceNavigation.getKeyPredicates().get(0).getText();
				key = key.substring(1, key.length() - 1);
				knownKey = true;
			} else {
				knownKey = false;
			}

			break;
		default:
			throw new ODataApplicationException("Unhandled URIType " + uriType,
					HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
		}
		try {
			sparqlStatement = sparqlBuilder.prepareConstructSparql();
		} catch (OData2SparqlException e) {
			throw new ODataRuntimeException(e.getMessage());
		}

		SparqlEntityCollection rdfResults = sparqlStatement.executeConstruct(rdfEdmProvider, rdfEntityType,
				uriInfo.getExpandOption(), uriInfo.getSelectOption());

		return knownKey ? rdfResults.findEntity(key) : rdfResults.getFirstEntity();

	}

	static public RdfLiteral countEntitySet(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, UriType uriType)
			throws OData2SparqlException, EdmException, ODataException {
		RdfResourceParts rdfResourceParts = new RdfResourceParts(rdfEdmProvider, uriInfo);
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider, uriInfo, uriType, rdfResourceParts);

		//prepareQuery
		SparqlStatement sparqlStatement = null;
		sparqlStatement = sparqlBuilder.prepareCountEntitySetSparql();
		RdfResultSet rdfResults = sparqlStatement.executeSelect(rdfEdmProvider);

		RdfLiteral countLiteral = null;
		while (rdfResults.hasNext()) {
			RdfQuerySolution solution = rdfResults.next();
			countLiteral = solution.getRdfLiteral("COUNT");
			break;
		}
		if (countLiteral == null) {
			throw new ODataApplicationException("No results", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		} else {
			return countLiteral;
		}
	}

	static public EntityCollection readReferenceCollection(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo,
			UriType uriType)
			throws OData2SparqlException, EdmException, ODataException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfResourceParts rdfResourceParts = new RdfResourceParts(rdfEdmProvider, uriInfo);
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;

		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		edmEntitySet = uriResourceEntitySet.getEntitySet();
		rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider, uriInfo, uriType, rdfResourceParts);

		//prepareQuery
		SparqlStatement sparqlStatement = sparqlBuilder.prepareEntityLinksSparql();
		SparqlEntityCollection rdfResults = sparqlStatement.executeConstruct(rdfEdmProvider, rdfEntityType, null, null);

		if (rdfResults == null) {
			throw new ODataApplicationException("No results", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		} else {
			return rdfResults;
		}
	}

	public static Entity writeEntity(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, Entity requestEntity)
			throws OData2SparqlException, ODataApplicationException {
		SparqlStatement sparqlStatement = null;
		EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateInsertEntity(entityType, requestEntity);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeInsert(rdfEdmProvider);
		return requestEntity;
	}

	public static void deleteEntity(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		// 2. delete the data in backend
		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateDeleteEntity(entityType, keyPredicates);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);
	}

	public static void updateEntity(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, Entity requestEntity,
			HttpMethod httpMethod) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
		//EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		// Note that this updateEntity()-method is invoked for both PUT or PATCH operations  
		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateUpdateEntity(entityType, keyPredicates,
					requestEntity);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);
	}

	public static void updatePrimitiveValue(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, Object entry)
			throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		UriResourcePrimitiveProperty uriResourcePrimitiveProperty = (UriResourcePrimitiveProperty) resourcePaths.get(1);
		EdmProperty edmProperty = uriResourcePrimitiveProperty.getProperty();

		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateUpdateEntitySimplePropertyValue(entityType,
					keyPredicates, edmProperty.getName(), entry);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeUpdate(rdfEdmProvider);
	}

	public static void deletePrimitiveValue(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo)
			throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		UriResourcePrimitiveProperty uriResourcePrimitiveProperty = (UriResourcePrimitiveProperty) resourcePaths.get(1);
		EdmProperty edmProperty = uriResourcePrimitiveProperty.getProperty();

		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateDeleteEntitySimplePropertyValue(entityType,
					keyPredicates, edmProperty.getName());
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);

	}

	public static void writeEntityReference(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo,
			List<URI> requestEntityReferences) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
		RdfNavigationProperty navigationProperty = entityType
				.findNavigationPropertyByEDMNavigationPropertyName(uriResourceNavigation.getProperty().getName());

		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateInsertLinkQuery(entityType, keyPredicates,
					navigationProperty, requestEntityReferences);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeInsert(rdfEdmProvider);

	}

	public static void updateEntityReference(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo,
			List<URI> requestEntityReferences) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
		RdfNavigationProperty navigationProperty = entityType
				.findNavigationPropertyByEDMNavigationPropertyName(uriResourceNavigation.getProperty().getName());
		List<UriParameter> navigationKeyPredicates = uriResourceNavigation.getKeyPredicates();
		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateUpdateLinkQuery(entityType, keyPredicates,
					navigationProperty, navigationKeyPredicates, requestEntityReferences);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeInsert(rdfEdmProvider);
	}

	public static void deleteEntityReference(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo)
			throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		List<UriParameter> entityKeyPredicates = uriResourceEntitySet.getKeyPredicates();
		UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
		RdfNavigationProperty navigationProperty = entityType
				.findNavigationPropertyByEDMNavigationPropertyName(uriResourceNavigation.getProperty().getName());
		List<UriParameter> navigationKeyPredicates = uriResourceNavigation.getKeyPredicates();
		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateDeleteLinkQuery(entityType, entityKeyPredicates,
					navigationProperty, navigationKeyPredicates);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeInsert(rdfEdmProvider);
	}
}
