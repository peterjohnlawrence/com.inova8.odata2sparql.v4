package com.inova8.odata2sparql.SparqlStatement;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfLiteral;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfQuerySolution;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.SparqlBuilder.SparqlCreateUpdateDeleteBuilder;
import com.inova8.odata2sparql.SparqlBuilder.SparqlQueryBuilder;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlBaseCommand {
	private final static Log log = LogFactory.getLog(SparqlBaseCommand.class);

	static public EntityCollection readEntitySet(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, UriType uriType)
			throws ODataException, OData2SparqlException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),
				rdfEdmProvider.getEdmMetadata(), uriInfo, uriType);

		//prepareQuery
		SparqlStatement sparqlStatement = null;
		UriResourceEntitySet uriResourceEntitySet;
		switch (uriType) {
		case URI1:
			uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
			edmEntitySet = uriResourceEntitySet.getEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			break;
		case URI6B:
			UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
			FullQualifiedName edmEntityTypeFQN = uriResourceNavigation.getProperty().getType().getFullQualifiedName();
			rdfEntityType = rdfEdmProvider.getMappedEntityType(edmEntityTypeFQN);
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
		return rdfResults.getEntityCollection();
	}

	static public Entity readEntity(RdfEdmProvider rdfEdmProvider, final UriInfo uriInfo, UriType uriType)
			throws EdmException, OData2SparqlException, ODataException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),
				rdfEdmProvider.getEdmMetadata(), uriInfo, uriType);

		//prepareQuery
		SparqlStatement sparqlStatement = null;
		UriResourceEntitySet uriResourceEntitySet = null;
		switch (uriType) {
		case URI5:
			UriResource lastResourcePart = resourcePaths.get(resourcePaths.size() - 1);
			int minSize = 2;
			if (lastResourcePart.getSegmentValue().equals("$value")) {
				minSize++;
			}
			if (resourcePaths.size() > minSize) {
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
				FullQualifiedName edmEntityTypeFQN = uriResourceNavigation.getProperty().getType()
						.getFullQualifiedName();
				rdfEntityType = rdfEdmProvider.getMappedEntityType(edmEntityTypeFQN);
			} else {
				uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
				edmEntitySet = uriResourceEntitySet.getEntitySet();
				rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			}
			break;
		case URI2:
			uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
			edmEntitySet = uriResourceEntitySet.getEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			break;
		case URI6A:
			UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
			FullQualifiedName edmEntityTypeFQN = uriResourceNavigation.getProperty().getType().getFullQualifiedName();
			rdfEntityType = rdfEdmProvider.getMappedEntityType(edmEntityTypeFQN);
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

		return rdfResults.getFirstEntity();

	}

	static public RdfLiteral countEntitySet(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, UriType uriType)
			throws  OData2SparqlException, EdmException, ODataApplicationException, ExpressionVisitException {
		//List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		//RdfEntityType rdfEntityType = null;
		//EdmEntitySet edmEntitySet = null;
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),
				rdfEdmProvider.getEdmMetadata(), uriInfo, uriType);

		//prepareQuery
		SparqlStatement sparqlStatement = null;
		//UriResourceEntitySet uriResourceEntitySet;

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
			UriType uriType) throws OData2SparqlException, EdmException, ODataApplicationException, ExpressionVisitException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;

		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		edmEntitySet = uriResourceEntitySet.getEntitySet();
		rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),
				rdfEdmProvider.getEdmMetadata(), uriInfo, uriType);

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
			List<URI> requestEntityReferences) throws  OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
		RdfAssociation navigationProperty = entityType
				.findNavigationProperty(uriResourceNavigation.getProperty().getName());

		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateInsertLinkQuery( entityType, keyPredicates,navigationProperty,
					requestEntityReferences);
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
		RdfAssociation navigationProperty = entityType
				.findNavigationProperty(uriResourceNavigation.getProperty().getName());
		List<UriParameter> navigationKeyPredicates = uriResourceNavigation.getKeyPredicates();
		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateUpdateLinkQuery( entityType, keyPredicates,navigationProperty,navigationKeyPredicates,
					requestEntityReferences);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeInsert(rdfEdmProvider);	
	}

	public static void deleteEntityReference(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		RdfEntityType entityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		List<UriParameter> entityKeyPredicates = uriResourceEntitySet.getKeyPredicates();
		UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
		RdfAssociation navigationProperty = entityType
				.findNavigationProperty(uriResourceNavigation.getProperty().getName());
		List<UriParameter> navigationKeyPredicates = uriResourceNavigation.getKeyPredicates();
		SparqlCreateUpdateDeleteBuilder sparqlCreateUpdateDeleteBuilder = new SparqlCreateUpdateDeleteBuilder(
				rdfEdmProvider);
		try {
			sparqlStatement = sparqlCreateUpdateDeleteBuilder.generateDeleteLinkQuery( entityType, entityKeyPredicates,navigationProperty,navigationKeyPredicates);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeInsert(rdfEdmProvider);
	}
}
