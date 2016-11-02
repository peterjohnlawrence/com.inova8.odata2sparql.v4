package com.inova8.odata2sparql.SparqlProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfConstructQuery;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfTriple;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfTripleSet;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfEntity;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;

class SparqlBaseCommand {
	private final static Log log = LogFactory.getLog(SparqlBaseCommand.class);
	protected SparqlResults toOEntities(RdfEdmProvider sparqlEdmProvider, RdfEntityType rdfEntityType,
//TODO V2			RdfTripleSet results, List<ArrayList<NavigationPropertySegment>> expand, List<SelectItem> select)
			RdfTripleSet results, ExpandOption expand, SelectOption select)
					throws EdmException {

		SparqlResults sparqlResults = new SparqlResults( );
		Map<String, RdfEntity> rdfEntitiesMap = sparqlResults.getEntitySetResultsMap();

		HashMap<String, RdfAssociation> navPropertiesMap = buildNavPropertiesMap(sparqlEdmProvider, expand, select);

		try {
			while (results.hasNext()) {
				RdfTriple triple = results.next();
				RdfNode subjectNode = triple.getSubject();
				RdfNode propertyNode = triple.getPredicate();
				RdfNode objectNode = triple.getObject();

				RdfEntity rdfSubjectEntity = findOrCreateEntity(sparqlEdmProvider, rdfEntitiesMap, subjectNode,
						rdfEntityType);
				if ((expand == null || expand.getExpandItems().isEmpty()) && (select == null || select.getSelectItems().isEmpty())) {
					rdfSubjectEntity.setEntityType(rdfEntityType); 
				}
				if (objectNode.isIRI() || objectNode.isBlank()) {// Must be a navigation property pointing to an expanded entity
					if (propertyNode.getIRI().toString().equals(RdfConstants.RDF_TYPE)) {
						rdfSubjectEntity.setEntityType(sparqlEdmProvider.getRdfModel()
								.getOrCreateEntityType(objectNode));
					}
					RdfAssociation rdfAssociation = navPropertiesMap.get(propertyNode.getIRI().toString());
					if (rdfAssociation != null) {
						// Locate which of the $expand this is related to
						RdfEntity rdfObjectEntity = findOrCreateEntity(sparqlEdmProvider, rdfEntitiesMap, objectNode,
								rdfEntityType);
						rdfObjectEntity.setEntityType(rdfAssociation.getRangeClass());
						sparqlResults.addNavPropertyObjectValues(rdfSubjectEntity.getSubject(),
								rdfAssociation.getEDMAssociationName(), rdfObjectEntity);					
					}
					if (rdfSubjectEntity.getEntityType().isOperation()) {
						// An operation so need to use these as the primary key of the record.
						if (rdfSubjectEntity.getEntityType().findNavigationProperty(propertyNode.getLocalName()) != null) {
							rdfSubjectEntity.put(
									rdfSubjectEntity.getEntityType()
											.findNavigationProperty(propertyNode.getLocalName()).getRelatedKey(), RdfEntity
											//.URLEncodeEntityKey(objectNode.toQName(sparqlEdmProvider.getRdfModel().getRdfPrefixes())));
											.URLEncodeEntityKey(sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(objectNode)));						}
					}
				} else if (objectNode.isBlank()) {
					//Must be a navigation property pointing to an expanded entity, but they should really be eliminated from the query in the first place
				} else if(propertyNode.getIRI().toString().equals(RdfConstants.TARGETENTITY )){
					//Mark any targetEntity so that recursive queries can be executed
					rdfSubjectEntity.setTargetEntity(true);
				}
				else {// Must be a property with a value, so put it into a hashmap for processing the second time round when we know the property
					rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getLiteralObject());
				}
			}
		} catch (OData2SparqlException e) {
			e.printStackTrace();
		}
		return sparqlResults.build();
	}

	SparqlResults toOLinks(RdfEdmProvider edmProvider, RdfEntityType entityType, RdfTripleSet results,
			List<NavigationSegment> navigationSegments) throws EdmException, OData2SparqlException {
		SparqlResults sparqlResults = new SparqlResults();
		Map<String, RdfEntity> entitiesMap = sparqlResults.getEntitySetResultsMap();

		HashMap<String, RdfAssociation> navPropertiesMap = buildLinksMap(edmProvider, navigationSegments);

		while (results.hasNext()) {
			RdfTriple triple = results.next();
			RdfNode subjectNode = triple.getSubject();
			RdfNode propertyNode = triple.getPredicate();
			RdfNode objectNode = null;
			objectNode = triple.getObject();

			RdfEntity rdfSubjectEntity = findOrCreateEntity(edmProvider, entitiesMap, subjectNode, entityType);

			if (objectNode.isIRI()) {// Must be a navigation property pointing to an expanded entity

				RdfAssociation rdfAssociation = navPropertiesMap.get(propertyNode.getIRI().toString());

				// Locate which of the $expand this is related to
				RdfEntity rdfObjectEntity = findOrCreateEntity(edmProvider, entitiesMap, objectNode, null);
				rdfObjectEntity.setEntityType(rdfAssociation.getRangeClass());
				rdfSubjectEntity.setEntityType(rdfAssociation.getDomainClass());

				sparqlResults.addNavPropertyObjectValues(rdfSubjectEntity.getSubject(), rdfAssociation.getAssociationName(),
						rdfObjectEntity);

			} else if (objectNode.isBlank()) {
				//Must be a navigation property pointing to an expanded entity, but they should reallu be eliminated from the query in the first place
			} else {// Must be a property with a value, so put it into a hashmap for processing the second time round when we know the property
				rdfSubjectEntity.getDatatypeProperties().put(propertyNode, objectNode.getLiteralObject());
			}
		}

		return sparqlResults.build();
	}

	private HashMap<String, RdfAssociation> buildNavPropertiesMap(RdfEdmProvider edmProvider,
//TODO V2			List<ArrayList<NavigationPropertySegment>> expand, List<SelectItem> select) 
			ExpandOption expand, SelectOption select) 
					throws EdmException {

		HashMap<String, RdfAssociation> navPropertiesMap = new HashMap<String, RdfAssociation>();
		if (expand != null) {
		//Add selected navigation properties even if not expanded.
		for (SelectItem selectItem : select.getSelectItems()) {
//TODO V2
//			for (NavigationPropertySegment navigationPropertySegment : selectItem.getNavigationPropertySegments()) {
//				RdfAssociation rdfAssociation = edmProvider.getMappedNavigationProperty(new FullQualifiedName(
//						navigationPropertySegment.getNavigationProperty().getRelationship().getNamespace(),
//						navigationPropertySegment.getNavigationProperty().getRelationship().getName()));
//				navPropertiesMap.put(rdfAssociation.getAssociationNodeIRI(), rdfAssociation);
//			}
		}
		}
		if (expand != null) {
//TODO V2
//			for (ArrayList<NavigationPropertySegment> navigationPropertySegments : expand) {
//				for (NavigationPropertySegment navigationPropertySegment : navigationPropertySegments) {
//					RdfAssociation rdfAssociation = edmProvider.getMappedNavigationProperty(new FullQualifiedName(
//							navigationPropertySegment.getNavigationProperty().getRelationship().getNamespace(),
//							navigationPropertySegment.getNavigationProperty().getRelationship().getName()));
//					navPropertiesMap.put(rdfAssociation.getAssociationNodeIRI(), rdfAssociation);
//				}
//			}
		}
		return navPropertiesMap;
	}

	private HashMap<String, RdfAssociation> buildLinksMap(RdfEdmProvider edmProvider,
			List<NavigationSegment> navigationSegments) throws EdmException {

		HashMap<String, RdfAssociation> navPropertiesMap = new HashMap<String, RdfAssociation>();

		if (navigationSegments != null) {

			for (NavigationSegment navigationSegment : navigationSegments) {
				RdfAssociation rdfAssociation = edmProvider.getMappedNavigationProperty(new FullQualifiedName(
						navigationSegment.getNavigationProperty().getRelationship().getNamespace(), navigationSegment
								.getNavigationProperty().getRelationship().getName()));
				navPropertiesMap.put(rdfAssociation.getAssociationNodeIRI(), rdfAssociation);
			}

		}
		return navPropertiesMap;
	}

	private RdfEntity findOrCreateEntity(RdfEdmProvider sparqlEdmProvider, Map<String, RdfEntity> rdfEntitiesMap,
			RdfNode subjectNode, RdfEntityType rdfEntityType) {
		RdfEntity rdfEntity;
		rdfEntity = rdfEntitiesMap.get(sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode));
		if (rdfEntity == null) {
			rdfEntity = new RdfEntity(subjectNode,sparqlEdmProvider.getRdfModel().getRdfPrefixes());
			rdfEntitiesMap.put(sparqlEdmProvider.getRdfModel().getRdfPrefixes().toQName(subjectNode), rdfEntity);
		}
		return rdfEntity;
	}
	static SparqlResults executeQuery(RdfEdmProvider sparqlEdmProvider, RdfEntityType entityType, SparqlStatement sparqlStatement,
			//TODO V2			List<ArrayList<NavigationPropertySegment>> expand, List<SelectItem> select) throws OData2SparqlException {
					ExpandOption expand, SelectOption select) throws OData2SparqlException {
					RdfConstructQuery rdfQuery = new RdfConstructQuery(sparqlEdmProvider.getRdfRepository().getDataRepository(),
							sparqlStatement.getSparql());
					RdfTripleSet results;
					try {
						results = rdfQuery.execConstruct();
					} catch (OData2SparqlException e) {
						log.error(e.getMessage());
						throw new ODataRuntimeException(e.getMessage(), null);
					}
					SparqlBaseCommand rdfBaseCommand = new SparqlBaseCommand();
					return rdfBaseCommand.toOEntities(sparqlEdmProvider, entityType, results, expand,select);
				}
}
