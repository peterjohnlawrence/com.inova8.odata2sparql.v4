/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.RdfEdmProvider;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModelProvider;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

class RdfEdmModelProvider {

	private final RdfRepository rdfRepository;
	private RdfModelToMetadata edmMetadata;
	private final RdfModel rdfModel;
		
	RdfEdmModelProvider(RdfRepository rdfRepository ) throws OData2SparqlException{
		this.rdfRepository = rdfRepository;

		RdfModelProvider rdfModelProvider = new RdfModelProvider(rdfRepository);
		try {
			rdfModel =rdfModelProvider.getRdfModel();
		} catch (Exception e) {
			throw new OData2SparqlException(e.getMessage()); 
		}
		this.setEdmMetadata(new RdfModelToMetadata(rdfModel,this.rdfRepository.getWithRdfAnnotations() ,this.rdfRepository.getWithSapAnnotations(),this.rdfRepository.getUseBaseType()));
		
	}
	
	public RdfModelToMetadata getEdmMetadata() {
		return edmMetadata;
	}
	private void setEdmMetadata(RdfModelToMetadata edmMetadata) {
		this.edmMetadata = edmMetadata;
	}
	public RdfRepository getRdfRepository() {
		return rdfRepository;
	}
	/**
	 * @return the rdfModel
	 */
	public RdfModel getRdfModel() {
		return rdfModel;
	}
}
