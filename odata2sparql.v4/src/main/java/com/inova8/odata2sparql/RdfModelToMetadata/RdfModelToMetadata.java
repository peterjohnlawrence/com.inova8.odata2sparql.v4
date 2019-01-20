package com.inova8.odata2sparql.RdfModelToMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.apache.olingo.commons.api.edm.provider.CsdlTerm;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfSchema;
import com.inova8.odata2sparql.Utils.FullQualifiedNameComparator;

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

	private final TreeMap<String, CsdlSchema> rdfEdm = new TreeMap<String, CsdlSchema>();

	public List<CsdlSchema> getSchemas() {
		return new ArrayList<CsdlSchema>(rdfEdm.values());
	}

	public CsdlSchema getSchema(String nameSpace) {
		return rdfEdm.get(nameSpace);
	}
	public CsdlTerm getTerm(FullQualifiedName termName) {
		return null;
	}

	private final Map<FullQualifiedName, RdfEntityType> entitySetMapping = new TreeMap<FullQualifiedName, RdfEntityType>(
			new FullQualifiedNameComparator());
	private final Map<FullQualifiedName, RdfProperty> propertyMapping = new TreeMap<FullQualifiedName, RdfProperty>(
			new FullQualifiedNameComparator());
	private final Map<FullQualifiedName, RdfAssociation> navigationPropertyMapping = new TreeMap<FullQualifiedName, RdfAssociation>(
			new FullQualifiedNameComparator());

	private void addToAnnotations(List<CsdlAnnotation> annotations, String fqn, String text) {
		if (text == null || text.isEmpty()) {
		} else {
			annotations.add(new CsdlAnnotation().setTerm(fqn).setExpression(
					new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.String, text)));
		}
	}

	public RdfModelToMetadata(RdfModel rdfModel, boolean withRdfAnnotations, boolean withSapAnnotations,
			boolean useBaseType, boolean withFKProperties) {
		Map<String, CsdlEntityType> globalEntityTypes = new TreeMap<String, CsdlEntityType>();

		Map<String, RdfAssociation> navigationPropertyLookup = new TreeMap<String, RdfAssociation>();
		Map<String, CsdlEntitySet> entitySetsMapping = new TreeMap<String, CsdlEntitySet>();

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
		TreeMap<String, CsdlEntitySet> entitySets = new TreeMap<String, CsdlEntitySet>();

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
						entityType.setBaseType(RdfFullQualifiedName.getFullQualifiedName(rdfClass.getBaseType()));
					}
				}
				List<CsdlAnnotation> entityTypeAnnotations = new ArrayList<CsdlAnnotation>();
				if (withRdfAnnotations)
					addToAnnotations(entityTypeAnnotations, RdfConstants.RDFS_CLASS_FQN, rdfClass.getIRI());
				if (withSapAnnotations) {
					addToAnnotations(entityTypeAnnotations, RdfConstants.SAP_LABEL_FQN, rdfClass.getEntityTypeLabel());
					if (rdfClass.getBaseType() != null)
						addToAnnotations(entityTypeAnnotations, RdfConstants.ODATA_BASETYPE_FQN,
								rdfClass.getBaseType().getEntityTypeName());
				}
				entityType.setAnnotations(entityTypeAnnotations);
				//TODO testing openTypes
				//entityType.setOpenType(true);
			}
		}

		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Second pass to add properties, navigation properties, and entitysets, and create the schema
			Map<String, CsdlEntityType> entityTypes = new TreeMap<String, CsdlEntityType>();
			Map<String, CsdlComplexType> complexTypes = new TreeMap<String, CsdlComplexType>();
			Map<String, CsdlEntityType> entityTypeMapping = new TreeMap<String, CsdlEntityType>();

			String modelNamespace = rdfModel.getModelNamespace(rdfGraph);

			for (RdfEntityType rdfClass : rdfGraph.classes) {
				String entityTypeName = rdfClass.getEDMEntityTypeName();
				CsdlEntityType entityType = globalEntityTypes.get(rdfClass.getIRI());
				entityTypes.put(entityTypeName, entityType);
				entityType.setAbstract(false);
				entityTypeMapping.put(entityTypeName, entityType);
				TreeMap<String, CsdlNavigationProperty> navigationProperties = new TreeMap<String, CsdlNavigationProperty>();
				TreeMap<String, CsdlProperty> entityTypeProperties = new TreeMap<String, CsdlProperty>();

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
						if ((withFKProperties && rdfProperty.isFK()) || !rdfProperty.isFK() || rdfProperty.getIsKey()) {
							String propertyName = rdfProperty.getEDMPropertyName();
							CsdlProperty property = null;
							if (rdfProperty.getIsComplex()) {
								if (currentRdfClass == rdfClass) {
									//use complexType but do not inherit down
									property = new CsdlProperty().setName(propertyName)
											.setType(rdfProperty.getComplexType().getFullQualifiedName());
								}
							} else {
								EdmPrimitiveTypeKind propertyType = RdfEdmType.getEdmType(rdfProperty.propertyTypeName);
								property = new CsdlProperty().setName(propertyName)
										.setType(propertyType.getFullQualifiedName());
								if (propertyType == EdmPrimitiveTypeKind.DateTimeOffset)
									property.setPrecision(RdfConstants.DATE_PRECISION);
								if (propertyType == EdmPrimitiveTypeKind.Decimal)
									property.setScale(RdfConstants.DECIMAL_SCALE);

							}
							if (property != null) {
								//Only build if not null
								buildProperty(withRdfAnnotations, withSapAnnotations, rdfProperty, property);
								entityTypeProperties.put(property.getName(), property);
								propertyMapping.put(new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(),
										property.getName()), rdfProperty);
							}
						}

					}
					entityType.setProperties(new ArrayList<CsdlProperty>(entityTypeProperties.values()));
					entityType.setNavigationProperties(
							new ArrayList<CsdlNavigationProperty>(navigationProperties.values()));

					String entitySetName = rdfClass.getEDMEntitySetName();

					CsdlEntitySet entitySet = new CsdlEntitySet().setName(entitySetName)
							.setType(new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), entityTypeName));

					List<CsdlAnnotation> entitySetAnnotations = new ArrayList<CsdlAnnotation>();
					if (withSapAnnotations)
						addToAnnotations(entitySetAnnotations, RdfConstants.SAP_LABEL_FQN,
								rdfClass.getEntitySetLabel());
					entitySet.setAnnotations(entitySetAnnotations);

					entitySets.put(entitySet.getName(), entitySet);

					entitySetMapping.put(entitySet.getTypeFQN(), rdfClass);
					entitySetsMapping.put(entitySetName, entitySet);
					if (useBaseType)
						currentRdfClass = null;
					else if (currentRdfClass.getBaseType() != currentRdfClass) {
						currentRdfClass = currentRdfClass.getBaseType();
					} else {
						currentRdfClass = null;
					}
				} while (currentRdfClass != null);
			}

			for (RdfAssociation rdfAssociation : rdfGraph.associations) {
				// if (!rdfAssociation.isInverse)
				{
					String associationName = rdfAssociation.getEDMAssociationName();

					//TODO Do we need a new navigation property or extend an existing one?
					CsdlNavigationProperty navigationProperty = new CsdlNavigationProperty().setName(associationName)
							.setType(new FullQualifiedName(rdfAssociation.getRangeClass().getSchema().getSchemaPrefix(),
									rdfAssociation.getRangeName()))
							.setCollection((rdfAssociation.getDomainCardinality() == Cardinality.MANY)
									|| (rdfAssociation.getDomainCardinality() == Cardinality.MULTIPLE));

					List<CsdlAnnotation> navigationPropertyAnnotations = new ArrayList<CsdlAnnotation>();
					if (withRdfAnnotations)
						addToAnnotations(navigationPropertyAnnotations, RdfConstants.PROPERTY_FQN,
								rdfAssociation.getAssociationIRI().toString());
					if (withSapAnnotations)
						addToAnnotations(navigationPropertyAnnotations, RdfConstants.SAP_LABEL_FQN,
								rdfAssociation.getAssociationLabel());
					if (withSapAnnotations)
						addToAnnotations(navigationPropertyAnnotations, RdfConstants.SAP_HEADING_FQN,
								rdfAssociation.getAssociationLabel());
					if (withSapAnnotations)
						addToAnnotations(navigationPropertyAnnotations, RdfConstants.SAP_QUICKINFO_FQN,
								rdfAssociation.getAssociationLabel());
					if (rdfAssociation.IsInverse()) {
						if (withRdfAnnotations)
							addToAnnotations(navigationPropertyAnnotations, RdfConstants.INVERSEOF_FQN,
									rdfAssociation.getInversePropertyOfURI().toString());
					}
					navigationProperty.setAnnotations(navigationPropertyAnnotations);
					//TODO should not add duplicates to the same entity, even though Olingo accepts them							
					globalEntityTypes.get(rdfAssociation.getDomainNodeURI()).getNavigationProperties()
							.add(navigationProperty);
					navigationPropertyLookup.put(navigationProperty.getName(), rdfAssociation);
					navigationPropertyMapping.put(rdfAssociation.getFullQualifiedName(), rdfAssociation);
				}
			}
			for (RdfComplexType rdfComplexType : rdfGraph.getComplexTypes()) {
				CsdlComplexType csdlComplexType = new CsdlComplexType().setName(rdfComplexType.getComplexTypeName());
				for (RdfProperty rdfProperty : rdfComplexType.getProperties().values()) {
					List<CsdlAnnotation> propertyAnnotations = new ArrayList<CsdlAnnotation>();
					addToAnnotations(propertyAnnotations, RdfConstants.ODATA_SUBTYPE_FQN,
							rdfProperty.getOfClass().getEntityTypeName());
					csdlComplexType.getProperties()
							.add(new CsdlProperty().setName(rdfProperty.getEDMPropertyName())
									.setType(RdfEdmType.getEdmType(rdfProperty.propertyTypeName).getFullQualifiedName())
									.setAnnotations(propertyAnnotations));
				}
				for (RdfAssociation rdfNavigationProperty : rdfComplexType.getNavigationProperties().values()) {
					List<CsdlAnnotation> navigationPropertyAnnotations = new ArrayList<CsdlAnnotation>();
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.ODATA_SUBTYPE_FQN,
							rdfNavigationProperty.getDomainName());
					csdlComplexType.getNavigationProperties()
							.add(new CsdlNavigationProperty().setName(rdfNavigationProperty.getEDMAssociationName())
									.setAnnotations(navigationPropertyAnnotations)
									.setType(new FullQualifiedName(
											rdfNavigationProperty.getRangeClass().getSchema().getSchemaPrefix(),
											rdfNavigationProperty.getRangeName()))
									.setCollection((rdfNavigationProperty.getDomainCardinality() == Cardinality.MANY)
											|| (rdfNavigationProperty.getDomainCardinality() == Cardinality.MULTIPLE)));
				}
				complexTypes.put(rdfComplexType.getComplexTypeName(), csdlComplexType);
			}
			//Only add if  schema is not empty of entityTypes
			if (!entityTypes.isEmpty()) {
				List<CsdlAnnotation> schemaAnnotations = new ArrayList<CsdlAnnotation>();
				addToAnnotations(schemaAnnotations, RdfConstants.ONTOLOGY_FQN, rdfGraph.getSchemaName());

				CsdlSchema modelSchema = new CsdlSchema().setNamespace(modelNamespace)
						.setEntityTypes(new ArrayList<CsdlEntityType>(entityTypes.values()))
						.setComplexTypes(new ArrayList<CsdlComplexType>(complexTypes.values()));
				//TODO MS does not support annotations to the schema
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
					//TODO Not supported in Olingo					
					//					List<CsdlAnnotation> navigationPropertyAnnotations = new ArrayList<CsdlAnnotation>();
					//					if (withRdfAnnotations)
					//						navigationPropertyAnnotations.add(buildCsdlAnnotation(RdfConstants.PROPERTY_FQN,
					//								rdfAssociation.getAssociationIRI().toString()));
					//					if (withSapAnnotations)
					//						navigationPropertyAnnotations.add(
					//								buildCsdlAnnotation(RdfConstants.SAP_LABEL_FQN, rdfAssociation.getAssociationLabel()));
					//					if (withSapAnnotations)
					//						navigationPropertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_HEADING_FQN,
					//								rdfAssociation.getAssociationLabel()));
					//					if (withSapAnnotations)
					//						navigationPropertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_QUICKINFO_FQN,
					//								rdfAssociation.getAssociationLabel()));
					//					if (rdfAssociation.IsInverse()) {
					//						if (withRdfAnnotations)
					//							navigationPropertyAnnotations.add(buildCsdlAnnotation(RdfConstants.INVERSEOF_FQN,
					//									rdfAssociation.getInversePropertyOfURI().toString()));
					//					}
					//					navigationPropertyBinding.setAnnotations(navigationPropertyAnnotations);
					entitySets.get(rdfAssociation.getDomainClass().getEDMEntitySetName())
							.getNavigationPropertyBindings().add(navigationPropertyBinding);
				}
			}
		}
		if (!useBaseType) {
			// Fourth pass to flatten navigationPropertyBinding if baseTypes not supported
			for (RdfSchema rdfGraph : rdfModel.graphs) {
				for (RdfEntityType rdfClass : rdfGraph.classes) {
					inheritBasetypeNavigationProperties(globalEntityTypes, entitySets, rdfClass);
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
		entityContainer.setEntitySets(new ArrayList<CsdlEntitySet>(entitySets.values()));

		//Finally, add terms and schemas to which they belong if they do not exist that have been used	
		addTermsToSchemas();
	}

	private void buildProperty(boolean withRdfAnnotations, boolean withSapAnnotations, RdfProperty rdfProperty,
			CsdlProperty property) {
		List<CsdlAnnotation> propertyAnnotations = new ArrayList<CsdlAnnotation>();
		if (!rdfProperty.propertyName.equals(RdfConstants.SUBJECT)) {
			if (rdfProperty.isFK()) {
				addToAnnotations(propertyAnnotations, RdfConstants.ODATA_FK_FQN,
						rdfProperty.getFkProperty().getAssociationName());
				//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.ODATA_FK_FQN, rdfProperty.getFkProperty().getAssociationName()));
			}
			if (withRdfAnnotations) {
				addToAnnotations(propertyAnnotations, RdfConstants.PROPERTY_FQN,
						rdfProperty.getPropertyURI().toString());
				//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.PROPERTY_FQN,					rdfProperty.getPropertyURI().toString()));
				addToAnnotations(propertyAnnotations, RdfConstants.DATATYPE_FQN, rdfProperty.propertyTypeName);
				//propertyAnnotations.add(			buildCsdlAnnotation(RdfConstants.DATATYPE_FQN, rdfProperty.propertyTypeName));
				if (rdfProperty.getEquivalentProperty() != null) {
					addToAnnotations(propertyAnnotations, RdfConstants.OWL_EQUIVALENTPROPERTY_FQN,
							rdfProperty.getEquivalentProperty());
					//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.OWL_EQUIVALENTPROPERTY_FQN,					rdfProperty.getEquivalentProperty()));
				}
			}
			if (withSapAnnotations) {
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_LABEL_FQN, rdfProperty.getPropertyLabel());
				//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_LABEL_FQN,						rdfProperty.getPropertyLabel()));
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_HEADING_FQN, rdfProperty.getPropertyLabel());
				//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_HEADING_FQN,						rdfProperty.getPropertyLabel()));
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_QUICKINFO_FQN, rdfProperty.getDescription());
				//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_QUICKINFO_FQN,						rdfProperty.getDescription()));
			}
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
				addToAnnotations(propertyAnnotations, RdfConstants.DATATYPE_FQN, RdfConstants.RDFS_RESOURCE);
			//propertyAnnotations.add(						buildCsdlAnnotation(RdfConstants.DATATYPE_FQN, RdfConstants.RDFS_RESOURCE));
			if (withSapAnnotations) {
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_LABEL_FQN, rdfProperty.getPropertyLabel());
				//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_LABEL_FQN,						rdfProperty.getPropertyLabel()));
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_HEADING_FQN, rdfProperty.getPropertyLabel());
				//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_HEADING_FQN,						rdfProperty.getPropertyLabel()));
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_QUICKINFO_FQN, rdfProperty.getDescription());
				//propertyAnnotations.add(buildCsdlAnnotation(RdfConstants.SAP_QUICKINFO_FQN,						rdfProperty.getDescription()));
			}
			property.setAnnotations(propertyAnnotations);

		}
	}

	private void addTermsToSchemas() {
		CsdlSchema sapSchema = rdfEdm.get("sap");
		if (sapSchema == null) {
			rdfEdm.put("sap", new CsdlSchema().setNamespace("sap"));
		}
		rdfEdm.get("sap").setTerms(RdfConstants.SAPTERMS);

		CsdlSchema odataSchema = rdfEdm.get("odata");
		if (odataSchema == null) {
			rdfEdm.put("odata", new CsdlSchema().setNamespace("odata"));
		}
		rdfEdm.get("odata").setTerms(RdfConstants.ODATATERMS);

		CsdlSchema rdfSchema = rdfEdm.get("rdf");
		if (rdfSchema == null) {
			rdfEdm.put("rdf", new CsdlSchema().setNamespace("rdf"));
		}
		rdfEdm.get("rdf").setTerms(RdfConstants.RDFTERMS);

		CsdlSchema rdfsSchema = rdfEdm.get("rdfs");
		if (rdfsSchema == null) {
			rdfEdm.put("rdfs", new CsdlSchema().setNamespace("rdfs"));
		}
		rdfEdm.get("rdfs").setTerms(RdfConstants.RDFSTERMS);

		CsdlSchema owlSchema = rdfEdm.get("owl");
		if (owlSchema == null) {
			rdfEdm.put("owl", new CsdlSchema().setNamespace("owl"));
		}
		rdfEdm.get("owl").setTerms(RdfConstants.OWLTERMS);
	}

	private void inheritBasetypeNavigationProperties(Map<String, CsdlEntityType> globalEntityTypes,
			TreeMap<String, CsdlEntitySet> entitySets, RdfEntityType rdfClass) {
		RdfEntityType baseType = rdfClass.getBaseType();
		if (baseType != null) {
			List<CsdlNavigationPropertyBinding> inheritedNavigationPropertyBindings = entitySets
					.get(baseType.getEDMEntitySetName()).getNavigationPropertyBindings();
			for (CsdlNavigationPropertyBinding inheritedNavigationPropertyBinding : inheritedNavigationPropertyBindings) {
				List<CsdlNavigationPropertyBinding> navigationPropertyBindings = entitySets
						.get(rdfClass.getEDMEntitySetName()).getNavigationPropertyBindings();
				if (!navigationPropertyBindings.contains(inheritedNavigationPropertyBinding))
					navigationPropertyBindings.add(inheritedNavigationPropertyBinding);
			}
			for (CsdlNavigationProperty inheritedNavigationProperty : globalEntityTypes.get(baseType.getIRI())
					.getNavigationProperties()) {
				List<CsdlNavigationProperty> navigationProperties = globalEntityTypes.get(rdfClass.getIRI())
						.getNavigationProperties();
				if (!navigationProperties.contains(inheritedNavigationProperty))
					navigationProperties.add(inheritedNavigationProperty);
				//TODO do these need to be added?
				//navigationPropertyLookup.put(navigationProperty.getName(), rdfAssociation);
				//navigationPropertyMapping.put(rdfAssociation.getFullQualifiedName(), rdfAssociation);
			}
			inheritBasetypeNavigationProperties(globalEntityTypes, entitySets, baseType);
		}
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
