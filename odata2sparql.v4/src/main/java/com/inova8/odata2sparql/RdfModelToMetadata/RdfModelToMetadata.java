package com.inova8.odata2sparql.RdfModelToMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;

import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfSchema;

public class RdfModelToMetadata {
	private class PrefixedNamespace {
		private final String uri;
		private final String prefix;

		private PrefixedNamespace(String uri, String prefix) {
			this.uri = uri;
			this.prefix = prefix;
		}

		@Override
		public String toString() {
			return uri + " " + prefix;
		}
	}

	private final HashMap<String, CsdlSchema> rdfEdm = new HashMap<String, CsdlSchema>();

	public List<CsdlSchema> getSchemas() {
		return new ArrayList<CsdlSchema>(rdfEdm.values());
	}

	public CsdlSchema getSchema(String nameSpace) {
		return rdfEdm.get(nameSpace);
	}

	private final Map<FullQualifiedName, RdfEntityType> entitySetMapping = new HashMap<FullQualifiedName, RdfEntityType>();
	private final Map<FullQualifiedName, RdfProperty> propertyMapping = new HashMap<FullQualifiedName, RdfProperty>();
	private final Map<FullQualifiedName, RdfAssociation> navigationPropertyMapping = new HashMap<FullQualifiedName, RdfAssociation>();

	private CsdlAnnotation buildCsdlAnnotation(String fqn, String text) {
		return new CsdlAnnotation().setTerm(fqn)
				.setExpression(new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.String, text));
	}

	public RdfModelToMetadata(RdfModel rdfModel, boolean withRdfAnnotations, boolean withSapAnnotations,
			boolean useBaseType) {
		Map<String, CsdlEntityType> globalEntityTypes = new HashMap<String, CsdlEntityType>();

		Map<String, RdfAssociation> navigationPropertyLookup = new HashMap<String, RdfAssociation>();
		//Map<String, RdfAssociation> associationLookup = new HashMap<String, RdfAssociation>();
		Map<String, CsdlEntitySet> entitySetsMapping = new HashMap<String, CsdlEntitySet>();

		ArrayList<PrefixedNamespace> nameSpaces = new ArrayList<PrefixedNamespace>();
		nameSpaces.add(new PrefixedNamespace(RdfConstants.RDF_SCHEMA, RdfConstants.RDF));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.RDFS_SCHEMA, RdfConstants.RDFS));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.OWL_SCHEMA, RdfConstants.OWL));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.XSD_SCHEMA, RdfConstants.XSD));

		String entityContainerName = RdfConstants.ENTITYCONTAINER;
		CsdlEntityContainer entityContainer = new CsdlEntityContainer().setName(entityContainerName);

		CsdlSchema instanceSchema = new CsdlSchema().setNamespace(RdfConstants.ENTITYCONTAINERNAMESPACE)
				.setEntityContainer(entityContainer);
		rdfEdm.put(RdfConstants.ENTITYCONTAINERNAMESPACE, instanceSchema);

		HashMap<String, CsdlEntitySet> entitySets = new HashMap<String, CsdlEntitySet>();

		//		HashMap<String, AssociationSet> associationSets = new HashMap<String, AssociationSet>();

		//Custom types langString

		ArrayList<CsdlProperty> langStringProperties = new ArrayList<CsdlProperty>();
		langStringProperties.add(new CsdlProperty().setName(RdfConstants.LANG)
				.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(true));
		langStringProperties.add(new CsdlProperty().setName(RdfConstants.VALUE)
				.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
		CsdlComplexType langLiteralType = new CsdlComplexType().setName(RdfConstants.LANGSTRING);
		langLiteralType.setProperties(langStringProperties);
		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// First pass to locate all classes (entitytypes) and datatypes (typedefinitions)

			for (RdfEntityType rdfClass : rdfGraph.classes) {
				String entityTypeName = rdfClass.getEDMEntityTypeName();
				CsdlEntityType entityType = globalEntityTypes.get(rdfClass.getIRI());
				if (entityType == null) {
					entityType = new CsdlEntityType().setName(entityTypeName);
					globalEntityTypes.put(rdfClass.getIRI(), entityType);
				}
				if (useBaseType) {
					if (rdfClass.getBaseType() != null) {
						String baseTypeName = rdfClass.getBaseType().getEDMEntityTypeName();
						CsdlEntityType baseType = globalEntityTypes.get(rdfClass.getBaseType().getIRI());
						if (baseType == null) {
							baseType = new CsdlEntityType().setName(baseTypeName);
							globalEntityTypes.put(rdfClass.getBaseType().getIRI(), baseType);
						}
						//entityType.setBaseType(rdfClass.getBaseType().getFullQualifiedName());
						entityType.setBaseType(RdfFullQualifiedName.getFullQualifiedName(rdfClass.getBaseType()));
					}
				}
				List<CsdlAnnotation> entityTypeAnnotations = new ArrayList<CsdlAnnotation>();
				if (withRdfAnnotations)
					entityTypeAnnotations.add(buildCsdlAnnotation(RdfConstants.RDFS_CLASS_FQN, rdfClass.getIRI()));
				if (withSapAnnotations)
					entityTypeAnnotations
							.add(buildCsdlAnnotation(RdfConstants.SAP_LABEL_FQN, rdfClass.getEntityTypeLabel()));
				entityType.setAnnotations(entityTypeAnnotations);
			}
		}

		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Second pass to add properties, navigation properties, and entitysets, and create the schema
			Map<String, CsdlEntityType> entityTypes = new HashMap<String, CsdlEntityType>();
			//			HashMap<String, Association> associations = new HashMap<String, Association>();
			Map<String, CsdlEntityType> entityTypeMapping = new HashMap<String, CsdlEntityType>();

			String modelNamespace = rdfModel.getModelNamespace(rdfGraph);

			for (RdfEntityType rdfClass : rdfGraph.classes) {
				String entityTypeName = rdfClass.getEDMEntityTypeName();
				CsdlEntityType entityType = globalEntityTypes.get(rdfClass.getIRI());
				entityTypes.put(entityTypeName, entityType);
				entityType.setAbstract(false);
				entityTypeMapping.put(entityTypeName, entityType);
				HashMap<String, CsdlNavigationProperty> navigationProperties = new HashMap<String, CsdlNavigationProperty>();
				HashMap<String, CsdlProperty> entityTypeProperties = new HashMap<String, CsdlProperty>();

				//Iterate through baseType if flattened metadata required
				RdfEntityType currentRdfClass = rdfClass;
				do {

					ArrayList<CsdlPropertyRef> keys = new ArrayList<CsdlPropertyRef>();
					for (RdfPrimaryKey primaryKey : currentRdfClass.getPrimaryKeys()) {
						String propertyName = primaryKey.getEDMPropertyName();
						keys.add(new CsdlPropertyRef().setName(propertyName));
						entityType.setKey(keys);
					}

					for (RdfProperty rdfProperty : currentRdfClass.getProperties()) {
						String propertyName = rdfProperty.getEDMPropertyName();
						EdmPrimitiveTypeKind propertyType = RdfEdmType.getEdmType(rdfProperty.propertyTypeName);

						//TODO langString
						//					if (propertyType.equals(EdmPrimitiveTypeKind.String)
						//							&& !rdfProperty.propertyName
						//									.equals(RdfConstants.ID))
						//						propertyType = langLiteralType;
						CsdlProperty property = new CsdlProperty().setName(propertyName)
								.setType(propertyType.getFullQualifiedName());
						if (propertyType == EdmPrimitiveTypeKind.DateTimeOffset)
							property.setPrecision(3);

						List<CsdlAnnotation> propertyAnnotations = new ArrayList<CsdlAnnotation>();
						if (!rdfProperty.propertyName.equals(RdfConstants.SUBJECT)) {

							if (withRdfAnnotations)
								propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.PROPERTY_FQN,
										rdfProperty.getPropertyURI().toString()));

							if (withRdfAnnotations)
								propertyAnnotations.add(
										buildCsdlAnnotation(RdfConstants.DATATYPE_FQN, rdfProperty.propertyTypeName));
							if (rdfProperty.getEquivalentProperty() != null) {
								if (withRdfAnnotations)
									propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.OWL_EQUIVALENTPROPERTY_FQN,
											rdfProperty.getEquivalentProperty()));
							}
							if (withSapAnnotations)
								propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_LABEL_FQN,
										rdfProperty.getPropertyLabel()));
							if (withSapAnnotations)
								propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_HEADING_FQN,
										rdfProperty.getPropertyLabel()));
							if (withSapAnnotations)
								propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_QUICKINFO_FQN,
										rdfProperty.getDescription()));
							property.setAnnotations(propertyAnnotations);

							if (rdfProperty.getIsKey()) {
								property.setNullable(false);
							} else if (rdfProperty.getCardinality() == RdfConstants.Cardinality.ZERO_TO_ONE
									|| rdfProperty.getCardinality() == RdfConstants.Cardinality.MANY) {
								property.setNullable(true);
							} else {
								//TODO need to handle case when data violates nullablility, in the meantime allow all to be nullable
								property.setNullable(true);
							}

							if (rdfProperty.getCardinality() == RdfConstants.Cardinality.MANY
									|| rdfProperty.getCardinality() == RdfConstants.Cardinality.MULTIPLE) {
								//TODO property.setCollectionKind(CollectionKind.List);
							} else {
								//TODO property.setCollectionKind(CollectionKind.NONE);
							}

						} else {

							property.setNullable(false);
							if (withRdfAnnotations)
								propertyAnnotations.add(
										buildCsdlAnnotation(RdfConstants.DATATYPE_FQN, RdfConstants.RDFS_RESOURCE));
							if (withSapAnnotations)
								propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_LABEL_FQN,
										rdfProperty.getPropertyLabel()));
							if (withSapAnnotations)
								propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_HEADING_FQN,
										rdfProperty.getPropertyLabel()));
							if (withSapAnnotations)
								propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_QUICKINFO_FQN,
										rdfProperty.getDescription()));
							property.setAnnotations(propertyAnnotations);

						}
						entityTypeProperties.put(property.getName(), property);
						propertyMapping.put(
								new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), property.getName()),
								rdfProperty);

					}
					entityType.setProperties(new ArrayList<CsdlProperty>(entityTypeProperties.values()));
					entityType.setNavigationProperties(
							new ArrayList<CsdlNavigationProperty>(navigationProperties.values()));

					String entitySetName = rdfClass.getEDMEntitySetName();

					CsdlEntitySet entitySet = new CsdlEntitySet().setName(entitySetName)
							.setType(new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), entityTypeName));

					List<CsdlAnnotation> entitySetAnnotations = new ArrayList<CsdlAnnotation>();
					if (withSapAnnotations)
						entitySetAnnotations
								.add(buildCsdlAnnotation(RdfConstants.SAP_LABEL_FQN, rdfClass.getEntityTypeLabel()));
					entitySet.setAnnotations(entitySetAnnotations);

					entitySets.put(entitySet.getName(), entitySet);//.add(entitySet);

					entitySetMapping.put(entitySet.getTypeFQN(), rdfClass);
					entitySetsMapping.put(entitySetName, entitySet);
					if (useBaseType)
						currentRdfClass = null;
					else
						currentRdfClass = currentRdfClass.getBaseType();
				} while (currentRdfClass != null);
			}

			for (RdfAssociation rdfAssociation : rdfGraph.associations) {
				// if (!rdfAssociation.isInverse)
				{
					String associationName = rdfAssociation.getEDMAssociationName();

					//					//TODO if (!rdfAssociation.isInverse)
					//					associationLookup.put(association.getName(), rdfAssociation);

					//TODO Do we need a new navigation property or extend an existing one?
					CsdlNavigationProperty navigationProperty = new CsdlNavigationProperty().setName(associationName)
							.setType(new FullQualifiedName(rdfAssociation.getRangeClass().getSchema().getSchemaPrefix(),
									rdfAssociation.getRangeName()))
							.setCollection((rdfAssociation.getDomainCardinality() == Cardinality.MANY)
									|| (rdfAssociation.getDomainCardinality() == Cardinality.MULTIPLE));

					List<CsdlAnnotation> navigationPropertyAnnotations = new ArrayList<CsdlAnnotation>();
					if (withRdfAnnotations)
						navigationPropertyAnnotations.add(buildCsdlAnnotation(RdfConstants.PROPERTY_FQN,
								rdfAssociation.getAssociationIRI().toString()));
					if (withSapAnnotations)
						navigationPropertyAnnotations.add(
								buildCsdlAnnotation(RdfConstants.SAP_LABEL_FQN, rdfAssociation.getAssociationLabel()));
					if (withSapAnnotations)
						navigationPropertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_HEADING_FQN,
								rdfAssociation.getAssociationLabel()));
					if (withSapAnnotations)
						navigationPropertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_QUICKINFO_FQN,
								rdfAssociation.getAssociationLabel()));
					if (rdfAssociation.IsInverse()) {
						if (withRdfAnnotations)
							navigationPropertyAnnotations.add(buildCsdlAnnotation(RdfConstants.INVERSEOF_FQN,
									rdfAssociation.getInversePropertyOfURI().toString()));
					}
					navigationProperty.setAnnotations(navigationPropertyAnnotations);
					//TODO should not add duplicates to the same entity, even though Olingo accepts them							
					globalEntityTypes.get(rdfAssociation.getDomainNodeURI()).getNavigationProperties()
							.add(navigationProperty);

					navigationPropertyLookup.put(navigationProperty.getName(), rdfAssociation);

					//navigationPropertyMapping.put(new FullQualifiedName( rdfAssociation.getDomainClass().getSchema().getSchemaPrefix(), navigationProperty.getName()), rdfAssociation);
					navigationPropertyMapping.put(rdfAssociation.getFullQualifiedName(), rdfAssociation);

				}
			}
			//Only add if  schema is not empty of entityTypes
			if (!entityTypes.isEmpty()) {
				List<CsdlAnnotation> schemaAnnotations = new ArrayList<CsdlAnnotation>();
				schemaAnnotations.add(buildCsdlAnnotation(RdfConstants.ONTOLOGY_FQN, rdfGraph.getSchemaName()));

				CsdlSchema modelSchema = new CsdlSchema().setNamespace(modelNamespace)
						.setEntityTypes(new ArrayList<CsdlEntityType>(entityTypes.values()))
						.setComplexTypes(new ArrayList<CsdlComplexType>());
				//TODO MS does not support annotations to the schema
				//	modelSchema.setAnnotations(schemaAnnotations);
				if (modelNamespace.equals(RdfConstants.RDF)) {
					modelSchema.getComplexTypes().add(langLiteralType);
				}
				rdfEdm.put(modelNamespace, modelSchema);
			}
		}

		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Third pass to add navigationPropertyBinding
			for (RdfAssociation rdfAssociation : rdfGraph.associations) {
				{
					String path = rdfAssociation.getEDMAssociationName();
					String target = rdfAssociation.getRangeClass().getEDMEntitySetName();//.getRangeName();
					CsdlNavigationPropertyBinding navigationPropertyBinding = new CsdlNavigationPropertyBinding()
							.setPath(path).setTarget(target);
					entitySets.get(rdfAssociation.getDomainClass().getEDMEntitySetName())
							.getNavigationPropertyBindings().add(navigationPropertyBinding);
				}
			}
		}

		List<CsdlFunctionImport> functionImports = new ArrayList<CsdlFunctionImport>();
		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Final pass to add any functionImports
			CsdlSchema modelSchema = this.getSchema(rdfModel.getModelNamespace(rdfGraph));
			for (RdfEntityType rdfEntityType : rdfGraph.classes) {
				if (rdfEntityType.isFunctionImport()) {
					final CsdlFunctionImport functionImport = new CsdlFunctionImport();
					final CsdlFunction function = new CsdlFunction();
					List<CsdlParameter> functionParameters = new ArrayList<CsdlParameter>(0);
					for (com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter functionImportParameter : rdfEntityType
							.getFunctionImportParameters().values()) {

						CsdlParameter edmFunctionParameter = new CsdlParameter();
						edmFunctionParameter.setName(functionImportParameter.getName())
								.setType(
										RdfEdmType.getEdmType(functionImportParameter.getType()).getFullQualifiedName())
								.setNullable(false);
						functionParameters.add(edmFunctionParameter);
					}
					final CsdlReturnType functionImportReturnType = (new CsdlReturnType())
							.setType(RdfFullQualifiedName.getFullQualifiedName(rdfEntityType)).setCollection(true);
					//List<CsdlAnnotation> functionImportAnnotations = new ArrayList<CsdlAnnotation>();
					//functionImportAnnotations.add(new AnnotationAttribute().setName("IsBindable").setText("true"));
					function.setComposable(true);
					functionImport.setName(rdfEntityType.getEDMEntityTypeName() + "_fn")
							.setEntitySet(rdfEntityType.getEDMEntitySetName())
							.setFunction(RdfFullQualifiedName.getFullQualifiedName(rdfEntityType) + "_fn")
							.setIncludeInServiceDocument(true);//.getEDMEntityTypeName())

					function.setName(rdfEntityType.getEDMEntityTypeName() + "_fn").setParameters(functionParameters);
					function.setReturnType(functionImportReturnType);
					functionImports.add(functionImport);
					modelSchema.getFunctions().add(function);
				}
			}
		}
		entityContainer.setFunctionImports(functionImports);
		entityContainer.setEntitySets(new ArrayList<CsdlEntitySet>(entitySets.values()));//.setAssociationSets(new ArrayList<AssociationSet>(associationSets.values()));
		//addCoreFunctionImports(entityContainer, globalEntityTypes, entitySetsMapping);
	}

	public RdfEntityType getMappedEntityType(FullQualifiedName fqnEntityType) {
		return entitySetMapping.get(fqnEntityType);
	}

	public RdfEntityType getRdfEntityTypefromEdmEntitySet(EdmEntitySet edmEntitySet) throws EdmException {
		return this.getMappedEntityType(new FullQualifiedName(edmEntitySet.getEntityType().getNamespace(),
				edmEntitySet.getEntityType().getName()));
	}

	public RdfProperty getMappedProperty(FullQualifiedName fqnProperty) {
		return propertyMapping.get(fqnProperty);
	}

	public RdfAssociation getMappedNavigationProperty(FullQualifiedName edmNavigationProperty) {
		return navigationPropertyMapping.get(edmNavigationProperty);
	}

}
