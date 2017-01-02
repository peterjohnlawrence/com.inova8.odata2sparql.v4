package com.inova8.odata2sparql.RdfModel;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfSelectQuery;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;


class RdfMetamodelProvider {
	@SuppressWarnings("unused")
	private final Log log = LogFactory.getLog(RdfMetamodelProvider.class);
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
}
