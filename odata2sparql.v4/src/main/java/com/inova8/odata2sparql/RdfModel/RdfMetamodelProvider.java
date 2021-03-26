/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfModel;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfSelectQuery;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;


/**
 * The Class RdfMetamodelProvider.
 */
class RdfMetamodelProvider {
	
	/** The log. */
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(RdfMetamodelProvider.class);
	
	/** The rdf repository. */
	private final RdfRepository rdfRepository;
	
	/**
	 * Gets the rdf repository.
	 *
	 * @return the rdf repository
	 */
	public RdfRepository getRdfRepository() {
		return rdfRepository;
	}

	/**
	 * Instantiates a new rdf metamodel provider.
	 *
	 * @param rdfRepository the rdf repository
	 */
	RdfMetamodelProvider(RdfRepository rdfRepository) {
		super();
		this.rdfRepository = rdfRepository;
	}
//	public RdfModelProvider getRdfModelProvider() {
//		return new RdfModelProvider(this);
/**
 * Gets the graphs.
 *
 * @return the graphs
 * @throws OData2SparqlException the o data 2 sparql exception
 */
//	}
	public RdfResultSet getGraphs() throws OData2SparqlException {
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_GRAPHQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	};

	/**
	 * Gets the classes.
	 *
	 * @return the classes
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getClasses() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_CLASSQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}

	/**
	 * Gets the datatypes.
	 *
	 * @return the datatypes
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getDatatypes() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_DATATYPEQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}

	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getProperties() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTYQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the property domains.
	 *
	 * @return the property domains
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getProperty_Domains() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTY_DOMAINS_QUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the property ranges.
	 *
	 * @return the property ranges
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getProperty_Ranges() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTY_RANGES_QUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the property cardinality.
	 *
	 * @return the property cardinality
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getProperty_Cardinality() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTY_CARDINALITY_QUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the associations.
	 *
	 * @return the associations
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getAssociations() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_ASSOCIATIONQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the inverse associations.
	 *
	 * @return the inverse associations
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getInverseAssociations() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_INVERSEASSOCIATIONQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the operations.
	 *
	 * @return the operations
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getOperations() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_OPERATIONQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
		}
	
	/**
	 * Gets the operation property results.
	 *
	 * @return the operation property results
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getOperationPropertyResults() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_OPERATIONPROPERTYRESULTQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the operation association results.
	 *
	 * @return the operation association results
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getOperationAssociationResults() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_OPERATIONASSOCIATIONRESULTQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the operation arguments.
	 *
	 * @return the operation arguments
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getOperationArguments() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_OPERATIONARGUMENTQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the node shapes.
	 *
	 * @return the node shapes
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getNodeShapes() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_NODESHAPESQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the property shapes.
	 *
	 * @return the property shapes
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getPropertyShapes() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTYSHAPESQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	/**
	 * Gets the reified statements.
	 *
	 * @return the reified statements
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResultSet getReifiedStatements() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_REIFIEDSTATEMENTQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
}
