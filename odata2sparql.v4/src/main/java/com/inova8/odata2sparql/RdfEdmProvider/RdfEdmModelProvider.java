/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfEdmProvider;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModelProvider;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

/**
 * The Class RdfEdmModelProvider.
 */
class RdfEdmModelProvider {

	/** The rdf repository. */
	private final RdfRepository rdfRepository;
	
	/** The edm metadata. */
	private RdfModelToMetadata edmMetadata;
	
	/** The rdf model. */
	private final RdfModel rdfModel;
		
	/**
	 * Instantiates a new rdf edm model provider.
	 *
	 * @param rdfRepository the rdf repository
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfEdmModelProvider(RdfRepository rdfRepository ) throws OData2SparqlException{
		this.rdfRepository = rdfRepository;

		RdfModelProvider rdfModelProvider = new RdfModelProvider(rdfRepository);
		try {
			rdfModel =rdfModelProvider.getRdfModel();
		} catch (Exception e) {
			throw new OData2SparqlException(e.getMessage()); 
		}
		this.setEdmMetadata(new RdfModelToMetadata(rdfModel,this.rdfRepository.getWithRdfAnnotations() ,this.rdfRepository.getWithSapAnnotations(),this.rdfRepository.getUseBaseType(),this.rdfRepository.getWithFKProperties(),this.rdfRepository.isSupportScripting()));
		
	}
	
	/**
	 * Gets the edm metadata.
	 *
	 * @return the edm metadata
	 */
	public RdfModelToMetadata getEdmMetadata() {
		return edmMetadata;
	}
	
	/**
	 * Sets the edm metadata.
	 *
	 * @param edmMetadata the new edm metadata
	 */
	private void setEdmMetadata(RdfModelToMetadata edmMetadata) {
		this.edmMetadata = edmMetadata;
	}
	
	/**
	 * Gets the rdf repository.
	 *
	 * @return the rdf repository
	 */
	public RdfRepository getRdfRepository() {
		return rdfRepository;
	}
	
	/**
	 * Gets the rdf model.
	 *
	 * @return the rdfModel
	 */
	public RdfModel getRdfModel() {
		return rdfModel;
	}
}
