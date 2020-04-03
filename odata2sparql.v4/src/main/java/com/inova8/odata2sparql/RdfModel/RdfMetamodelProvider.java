package com.inova8.odata2sparql.RdfModel;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfSelectQuery;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;


class RdfMetamodelProvider {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(RdfMetamodelProvider.class);
	private final RdfRepository rdfRepository;
	
	public RdfRepository getRdfRepository() {
		return rdfRepository;
	}

	RdfMetamodelProvider(RdfRepository rdfRepository) {
		super();
		this.rdfRepository = rdfRepository;
	}
//	public RdfModelProvider getRdfModelProvider() {
//		return new RdfModelProvider(this);
//	}
	public RdfResultSet getGraphs() throws OData2SparqlException {
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_GRAPHQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	};

	public RdfResultSet getClasses() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_CLASSQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}

	public RdfResultSet getDatatypes() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_DATATYPEQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}

	public RdfResultSet getProperties() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTYQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	public RdfResultSet getProperty_Domains() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTY_DOMAINS_QUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	public RdfResultSet getProperty_Ranges() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTY_RANGES_QUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	public RdfResultSet getProperty_Cardinality() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTY_CARDINALITY_QUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	public RdfResultSet getAssociations() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_ASSOCIATIONQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	public RdfResultSet getInverseAssociations() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_INVERSEASSOCIATIONQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	public RdfResultSet getOperations() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_OPERATIONQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
		}
	
	public RdfResultSet getOperationPropertyResults() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_OPERATIONPROPERTYRESULTQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	public RdfResultSet getOperationAssociationResults() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_OPERATIONASSOCIATIONRESULTQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	public RdfResultSet getOperationArguments() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_OPERATIONARGUMENTQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	
	public RdfResultSet getNodeShapes() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_NODESHAPESQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	public RdfResultSet getPropertyShapes() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_PROPERTYSHAPESQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	public RdfResultSet getReifiedPredicates() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_REIFIEDPREDICATEQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	public RdfResultSet getReifiedSubjectPredicates() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_REIFIEDSUBJECTPREDICATEQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
	public RdfResultSet getReifiedObjectPredicates() throws OData2SparqlException{
		String query = RdfConstants.getMetaQueries().get(RdfConstants.URI_REIFIEDOBJECTPREDICATEQUERY);
		RdfSelectQuery rdfQuery= new RdfSelectQuery(rdfRepository.getModelRepository(),query);
		return  rdfQuery.execSelect(false);
	}
}
