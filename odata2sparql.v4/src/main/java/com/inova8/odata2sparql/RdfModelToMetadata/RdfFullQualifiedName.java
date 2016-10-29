package com.inova8.odata2sparql.RdfModelToMetadata;

import org.apache.olingo.commons.api.edm.FullQualifiedName;

import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;

class RdfFullQualifiedName {
	public static FullQualifiedName getFullQualifiedName(RdfAssociation rdfAssociation) {
		return new FullQualifiedName(rdfAssociation.getDomainClass().getSchema().getSchemaPrefix(), rdfAssociation.getEDMAssociationName());//associationName);
	}
	public static FullQualifiedName getFullQualifiedName(RdfEntityType rdfEntityType) {
		return new  FullQualifiedName(rdfEntityType.getSchema().getSchemaPrefix(), rdfEntityType.getEntityTypeName());
	}
}
