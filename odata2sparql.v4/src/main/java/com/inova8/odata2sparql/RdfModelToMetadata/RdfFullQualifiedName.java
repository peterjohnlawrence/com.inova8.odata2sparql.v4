package com.inova8.odata2sparql.RdfModelToMetadata;

import org.apache.olingo.commons.api.edm.FullQualifiedName;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;

class RdfFullQualifiedName {
	public static FullQualifiedName getFullQualifiedName(RdfNavigationProperty rdfAssociation) {
		return new FullQualifiedName(rdfAssociation.getDomainClass().getSchema().getSchemaPrefix(), rdfAssociation.getEDMNavigationPropertyName());//associationName);
	}
	public static FullQualifiedName getFullQualifiedName(RdfEntityType rdfEntityType) {
		return new  FullQualifiedName(rdfEntityType.getSchema().getSchemaPrefix(), rdfEntityType.getEntityTypeName());
	}
}
