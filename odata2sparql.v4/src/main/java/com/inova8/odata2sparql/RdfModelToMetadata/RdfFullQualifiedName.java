/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfModelToMetadata;

import org.apache.olingo.commons.api.edm.FullQualifiedName;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;

/**
 * The Class RdfFullQualifiedName.
 */
public class RdfFullQualifiedName {
	
	/**
	 * Gets the full qualified name.
	 *
	 * @param rdfAssociation the rdf association
	 * @return the full qualified name
	 */
	public static FullQualifiedName getFullQualifiedName(RdfNavigationProperty rdfAssociation) {
		return new FullQualifiedName(rdfAssociation.getDomainClass().getSchema().getSchemaPrefix(), rdfAssociation.getEDMNavigationPropertyName());//associationName);
	}
	
	/**
	 * Gets the full qualified name.
	 *
	 * @param rdfEntityType the rdf entity type
	 * @return the full qualified name
	 */
	public static FullQualifiedName getFullQualifiedName(RdfEntityType rdfEntityType) {
		return new  FullQualifiedName(rdfEntityType.getSchema().getSchemaPrefix(), rdfEntityType.getEntityTypeName());
	}
}
