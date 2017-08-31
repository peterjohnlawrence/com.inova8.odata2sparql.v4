package com.inova8.odata2sparql.SparqlStatement;

import java.util.List;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.commons.api.data.AbstractEntityCollection;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfLiteral;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfQuerySolution;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.SparqlBuilder.SparqlQueryBuilder;
import com.inova8.odata2sparql.uri.UriType;

public class SparqlBaseCommand {
	private final static Log log = LogFactory.getLog(SparqlBaseCommand.class);

	//	private HashMap<String, RdfAssociation> buildLinksMap(RdfEdmProvider edmProvider,
	//			List<NavigationSegment> navigationSegments) throws EdmException {
	//
	//		HashMap<String, RdfAssociation> navPropertiesMap = new HashMap<String, RdfAssociation>();
	//
	//		if (navigationSegments != null) {
	//
	//			for (NavigationSegment navigationSegment : navigationSegments) {
	//				RdfAssociation rdfAssociation = edmProvider.getMappedNavigationProperty(new FullQualifiedName(
	//						navigationSegment.getNavigationProperty().getRelationship().getNamespace(), navigationSegment
	//								.getNavigationProperty().getRelationship().getName()));
	//				navPropertiesMap.put(rdfAssociation.getAssociationNodeIRI(), rdfAssociation);
	//			}
	//
	//		}
	//		return navPropertiesMap;
	//	}

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
			throws ODataException, OData2SparqlException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),
				rdfEdmProvider.getEdmMetadata(), uriInfo, uriType);

		//prepareQuery
		SparqlStatement sparqlStatement = null;
		UriResourceEntitySet uriResourceEntitySet;

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
	static public AbstractEntityCollection readReferenceCollection(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo, UriType uriType)
			throws ODataException, OData2SparqlException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		SparqlQueryBuilder sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),
				rdfEdmProvider.getEdmMetadata(), uriInfo, uriType);

		//prepareQuery
		SparqlStatement sparqlStatement = null;


		sparqlStatement = sparqlBuilder.prepareEntityLinksSparql();
		SparqlEntityCollection rdfResults = sparqlStatement.executeConstruct(rdfEdmProvider, rdfEntityType,null,null);

		if (rdfResults == null) {
			throw new ODataApplicationException("No results", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH);
		} else {
			return rdfResults;
		}

	}
//	public static EdmEntitySet getNavigationTargetEntitySet(final UriInfoResource uriInfo)
//			throws ODataApplicationException {
//
//		EdmEntitySet entitySet;
//		final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
//
//		// First must be entity set (hence function imports are not supported here).
//		if (resourcePaths.get(0) instanceof UriResourceEntitySet) {
//			entitySet = ((UriResourceEntitySet) resourcePaths.get(0)).getEntitySet();
//		} else {
//			throw new ODataApplicationException("Invalid resource type.",
//					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
//		}
//
//		int navigationCount = 0;
//		while (entitySet != null && ++navigationCount < resourcePaths.size()
//				&& resourcePaths.get(navigationCount) instanceof UriResourceNavigation) {
//			final UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths
//					.get(navigationCount);
//			final EdmBindingTarget target = entitySet
//					.getRelatedBindingTarget(uriResourceNavigation.getProperty().getName());
//			if (target instanceof EdmEntitySet) {
//				entitySet = (EdmEntitySet) target;
//			} else {
//				throw new ODataApplicationException("Singletons not supported",
//						HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
//			}
//		}
//
//		return entitySet;
//	}
}
