package com.inova8.odata2sparql.RdfModelToMetadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;

import org.apache.commons.lang3.StringEscapeUtils;
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
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlCollection;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlExpression;

import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfSchema;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfShapedNavigationProperty;
import com.inova8.odata2sparql.Utils.FullQualifiedNameComparator;
import com.inova8.odata2sparql.Utils.NavigationPropertyComparator;
import com.inova8.odata2sparql.Utils.PropertyComparator;

public class RdfModelToMetadata {
	public class PrefixedNamespace {
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
			new PropertyComparator());
	private final Map<FullQualifiedName, RdfNavigationProperty> navigationPropertyMapping = new TreeMap<FullQualifiedName, RdfNavigationProperty>(
			new NavigationPropertyComparator());

	private void addToAnnotations(List<CsdlAnnotation> annotations, String fqn, String text) {
		if (text == null || text.isEmpty()) {
		} else {
			annotations.add(new CsdlAnnotation().setTerm(fqn)
					.setExpression(new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.String,
							StringEscapeUtils.escapeXml11(text.replaceAll("\"", "\\\"")))));//StringEscapeUtils.escapeXml11(text) )));"
		}
	}
	private void addToAnnotations(List<CsdlAnnotation> annotations, String fqn, Boolean text) {

			annotations.add(new CsdlAnnotation().setTerm(fqn)
					.setExpression(new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.Bool, text.toString())));
	}
	private void addToAnnotations(List<CsdlAnnotation> annotations, String fqn, List<String> textList) {

		if (textList == null || textList.isEmpty()) {
		} else {
			CsdlAnnotation annotation = new CsdlAnnotation();
			annotation.setTerm(fqn);
			//CsdlConstantExpression expression = new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.String, StringEscapeUtils.escapeXml11(text));
			ArrayList<CsdlExpression> expressionList = new ArrayList<CsdlExpression>();
			for (String text : textList) {
				expressionList.add(new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.String,
						StringEscapeUtils.escapeXml11(text.replaceAll("\"", "\\\""))));//StringEscapeUtils.escapeXml11(text)));
			}
			CsdlCollection collection = new CsdlCollection().setItems(expressionList);
			annotation.setExpression(collection);
			annotations.add(annotation);
		}
	}

	public RdfModelToMetadata(RdfModel rdfModel, boolean withRdfAnnotations, boolean withSapAnnotations,
			boolean useBaseType, boolean withFKProperties) {
		Map<String, CsdlEntityType> globalEntityTypes = new TreeMap<String, CsdlEntityType>();

		Map<String, RdfNavigationProperty> navigationPropertyLookup = new TreeMap<String, RdfNavigationProperty>();
		Map<String, CsdlEntitySet> entitySetsMapping = new TreeMap<String, CsdlEntitySet>();
		TreeMap<String, CsdlEntitySet> entitySets = new TreeMap<String, CsdlEntitySet>();

		CsdlEntityContainer entityContainer = initializeMetadata(rdfModel);
		CsdlComplexType langLiteralType = createLangType();
		
		createRdfMetadata(rdfModel);
	//	CsdlComplexType factType = createFactType();

		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// First pass to locate all classes (entitytypes) and datatypes (typedefinitions)
			locateEntityTypes(withRdfAnnotations, withSapAnnotations, useBaseType, globalEntityTypes, rdfGraph);
		}

		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Second pass to add properties, navigation properties, and entitysets, and create the schema
			Map<String, CsdlEntityType> entityTypes = new TreeMap<String, CsdlEntityType>();
			Map<String, CsdlComplexType> complexTypes = new TreeMap<String, CsdlComplexType>();
			Map<String, CsdlEntityType> entityTypeMapping = new TreeMap<String, CsdlEntityType>();

			String modelNamespace = rdfModel.getModelNamespace(rdfGraph);

			locateEntityTypes(withRdfAnnotations, withSapAnnotations, useBaseType, withFKProperties, globalEntityTypes,
					entitySetsMapping, entitySets, rdfGraph, entityTypes, entityTypeMapping);

			locateNavigationProperties(withRdfAnnotations, withSapAnnotations, globalEntityTypes,
					navigationPropertyLookup, rdfGraph);
			locateComplexTypes(rdfGraph, complexTypes);
			//			locateNodeShapes(withRdfAnnotations, withSapAnnotations, useBaseType, withFKProperties, globalEntityTypes,
			//					entitySetsMapping, entitySets, rdfGraph, entityTypes, entityTypeMapping, complexTypes);
			//Only add if  schema is not empty of entityTypes or complexTypes
			if (!entityTypes.isEmpty() || !complexTypes.isEmpty()) {
				List<CsdlAnnotation> schemaAnnotations = new ArrayList<CsdlAnnotation>();
				addToAnnotations(schemaAnnotations, RdfConstants.ONTOLOGY_FQN, rdfGraph.getSchemaName());

				CsdlSchema modelSchema = new CsdlSchema().setNamespace(modelNamespace)
						.setEntityTypes(new ArrayList<CsdlEntityType>(entityTypes.values()))
						.setComplexTypes(new ArrayList<CsdlComplexType>(complexTypes.values()));
				//TODO MS does not support annotations to the schema
				if (modelNamespace.equals(RdfConstants.RDF)) {
					modelSchema.getComplexTypes().add(langLiteralType);
			//		modelSchema.getComplexTypes().add(factType);
				}
				if (modelNamespace.equals(RdfConstants.RDFS)) {					
				//	entityTypes.get(RdfConstants.RDFS_RESOURCE_LABEL).getProperties().add(createFactsProperty());
				}
				rdfEdm.put(modelNamespace, modelSchema);
			}
		}

		
		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Third pass to add navigationPropertyBinding
			locateNavigationPropertyBinding(entitySets, rdfGraph);
		}
		if (!useBaseType) {
			// Fourth pass to flatten navigationPropertyBinding if baseTypes not supported
			for (RdfSchema rdfGraph : rdfModel.graphs) {
				for (RdfEntityType rdfClass : rdfGraph.getClasses()) {
					if (!rdfClass.isNodeShape()) {
						HashSet<RdfEntityType> visited = new HashSet<RdfEntityType>();
						inheritBasetypeNavigationProperties(globalEntityTypes, entitySets, rdfClass, visited);
					}
				}
			}
		}
		locateFunctionImports(rdfModel, entityContainer);
		entityContainer.setEntitySets(new ArrayList<CsdlEntitySet>(entitySets.values()));

		//Finally, add terms and schemas to which they belong if they do not exist that have been used	
		addTermsToSchemas();
	}

	private CsdlEntityContainer initializeMetadata(RdfModel rdfModel) {
		String entityContainerName = RdfConstants.ENTITYCONTAINER;
		CsdlEntityContainer entityContainer = new CsdlEntityContainer().setName(entityContainerName);
		ArrayList<PrefixedNamespace> nameSpaces = new ArrayList<PrefixedNamespace>();
		nameSpaces.add(new PrefixedNamespace(RdfConstants.RDF_SCHEMA, RdfConstants.RDF));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.RDFS_SCHEMA, RdfConstants.RDFS));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.OWL_SCHEMA, RdfConstants.OWL));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.XSD_SCHEMA, RdfConstants.XSD));
		CsdlSchema instanceSchema = new CsdlSchema().setNamespace(RdfConstants.ENTITYCONTAINERNAMESPACE)
				.setEntityContainer(entityContainer);

		rdfEdm.put(RdfConstants.ENTITYCONTAINERNAMESPACE, instanceSchema);

		List<CsdlAnnotation> instanceSchemaAnnotations = new ArrayList<CsdlAnnotation>();
		addToAnnotations(instanceSchemaAnnotations, RdfConstants.ODATA_DEFAULTNAMESPACE_FQN,
				rdfModel.getRdfRepository().getDefaultPrefix());
		instanceSchema.setAnnotations(instanceSchemaAnnotations);
		return entityContainer;
	}

	private CsdlComplexType createLangType() {
		ArrayList<CsdlProperty> langStringProperties = new ArrayList<CsdlProperty>();
		langStringProperties.add(new CsdlProperty().setName(RdfConstants.LANG)
				.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(true));
		langStringProperties.add(new CsdlProperty().setName(RdfConstants.VALUE)
				.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));

		CsdlComplexType langLiteralType = new CsdlComplexType().setName(RdfConstants.LANGSTRING);
		langLiteralType.setProperties(langStringProperties);
		return langLiteralType;
	}
	private void createRdfMetadata(RdfModel rdfModel) {
		
		
	}
	private CsdlComplexType createFactType() {
		ArrayList<CsdlProperty> factProperties = new ArrayList<CsdlProperty>();
		factProperties.add(new CsdlProperty().setName(RdfConstants.PROPERTY)
				.setType( new FullQualifiedName(RdfConstants.RDF, RdfConstants.PROPERTY)    // EdmPrimitiveTypeKind.String.getFullQualifiedName()
						).setNullable(false));
		factProperties.add(new CsdlProperty().setName(RdfConstants.DATAVALUES)
				.setType(new FullQualifiedName(RdfConstants.RDF, RdfConstants.LANGSTRING) 							//EdmPrimitiveTypeKind.String.getFullQualifiedName()
						).setCollection(true).setNullable(true));
		factProperties.add(new CsdlProperty().setName(RdfConstants.OBJECTVALUES)
				.setType(new FullQualifiedName(RdfConstants.RDFS, RdfConstants.RDFS_RESOURCE_LABEL) 							//EdmPrimitiveTypeKind.String.getFullQualifiedName()
						).setCollection(true).setNullable(true));
		CsdlComplexType factType = new CsdlComplexType().setName(RdfConstants.FACT);
		factType.setProperties(factProperties);
		return factType;
	}
	private CsdlProperty createFactsProperty() {
		CsdlProperty factsProperty = new CsdlProperty().setName(RdfConstants.FACTS)
				.setType( new FullQualifiedName(RdfConstants.RDF, RdfConstants.FACT)    // EdmPrimitiveTypeKind.String.getFullQualifiedName()
						).setNullable(true);

		return factsProperty;
	}
	private void locateFunctionImports(RdfModel rdfModel, CsdlEntityContainer entityContainer) {
		List<CsdlFunctionImport> functionImports = new ArrayList<CsdlFunctionImport>();
		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Final pass to add any functionImports
			CsdlSchema modelSchema = this.getSchema(rdfModel.getModelNamespace(rdfGraph));
			for (RdfEntityType rdfEntityType : rdfGraph.getClasses()) {
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
						
						List<CsdlAnnotation> functionParameterAnnotations = new ArrayList<CsdlAnnotation>();
						addToAnnotations(functionParameterAnnotations, RdfConstants.ODATA_ISDATASET_FQN,functionImportParameter.isDataset());
						addToAnnotations(functionParameterAnnotations, RdfConstants.ODATA_ISPROPERTYPATH_FQN,functionImportParameter.isPropertyPath());
						edmFunctionParameter.setAnnotations(functionParameterAnnotations);
						functionParameters.add(edmFunctionParameter);
					}
					final CsdlReturnType functionImportReturnType = (new CsdlReturnType())
							.setType(RdfFullQualifiedName.getFullQualifiedName(rdfEntityType)).setCollection(true);
					List<CsdlAnnotation> functionAnnotations = new ArrayList<CsdlAnnotation>();
					addToAnnotations(functionAnnotations, RdfConstants.ODATA_ISPROXY_FQN,rdfEntityType.isProxy());
					//functionImportAnnotations.add(new AnnotationAttribute().setName("IsBindable").setText("true"));
					function.setAnnotations(functionAnnotations);
					function.setComposable(true);
					functionImport.setName(rdfEntityType.getEDMEntityTypeName() + RdfConstants.FUNCTION_POSTFIX)
							.setEntitySet(rdfEntityType.getEDMEntitySetName())
							.setFunction(RdfFullQualifiedName.getFullQualifiedName(rdfEntityType)
									+ RdfConstants.FUNCTION_POSTFIX)
							.setIncludeInServiceDocument(true);//.getEDMEntityTypeName())

					function.setName(rdfEntityType.getEDMEntityTypeName() + RdfConstants.FUNCTION_POSTFIX)
							.setParameters(functionParameters);
					function.setReturnType(functionImportReturnType);
					functionImports.add(functionImport);
					modelSchema.getFunctions().add(function);
				}
			}
		}
		entityContainer.setFunctionImports(functionImports);
	}

	private void locateNavigationPropertyBinding(TreeMap<String, CsdlEntitySet> entitySets, RdfSchema rdfGraph) {
		for (RdfNavigationProperty rdfNavigationProperty : rdfGraph.getNavigationProperties()) {
			{
				String path = rdfNavigationProperty.getEDMNavigationPropertyName();
				String target = rdfNavigationProperty.getRangeClass().getEDMEntitySetName();//.getRangeName();
				CsdlNavigationPropertyBinding navigationPropertyBinding = new CsdlNavigationPropertyBinding()
						.setPath(path).setTarget(target);
				entitySets.get(rdfNavigationProperty.getDomainClass().getEDMEntitySetName())
						.getNavigationPropertyBindings().add(navigationPropertyBinding);
			}
		}
	}

	private void locateEntityTypes(boolean withRdfAnnotations, boolean withSapAnnotations, boolean useBaseType,
			boolean withFKProperties, Map<String, CsdlEntityType> globalEntityTypes,
			Map<String, CsdlEntitySet> entitySetsMapping, TreeMap<String, CsdlEntitySet> entitySets, RdfSchema rdfGraph,
			Map<String, CsdlEntityType> entityTypes, Map<String, CsdlEntityType> entityTypeMapping) {
		for (RdfEntityType rdfClass : rdfGraph.getClasses()) {
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
										.setType(rdfProperty.getComplexType().getFullQualifiedName())
										.setCollection(rdfProperty.isCollection());
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
							propertyMapping.put(
									new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), property.getName()),
									rdfProperty);
						}
					}
				}
				entityType.setProperties(new ArrayList<CsdlProperty>(entityTypeProperties.values()));
				entityType
						.setNavigationProperties(new ArrayList<CsdlNavigationProperty>(navigationProperties.values()));

				addEntityTypeToEntitySets(withSapAnnotations, entitySetsMapping, entitySets, rdfClass);

				if (useBaseType)
					currentRdfClass = null;
				else if (currentRdfClass.getBaseType() != currentRdfClass) {
					currentRdfClass = currentRdfClass.getBaseType();
				} else {
					currentRdfClass = null;
				}
			} while (currentRdfClass != null);
		}
	}

	private void addEntityTypeToEntitySets(boolean withSapAnnotations, Map<String, CsdlEntitySet> entitySetsMapping,
			TreeMap<String, CsdlEntitySet> entitySets, RdfEntityType rdfClass) {
		String entitySetName = rdfClass.getEDMEntitySetName();

		CsdlEntitySet entitySet = new CsdlEntitySet().setName(entitySetName).setType(
				new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), rdfClass.getEDMEntityTypeName()));

		List<CsdlAnnotation> entitySetAnnotations = new ArrayList<CsdlAnnotation>();
		if (withSapAnnotations) {
			addToAnnotations(entitySetAnnotations, RdfConstants.SAP_LABEL_FQN, rdfClass.getEntitySetLabel());
			//TODO Causes error with Excel
			//addToAnnotations(entitySetAnnotations, RdfConstants.SAP_HEADING_FQN, rdfClass.getEntitySetLabel());
			//addToAnnotations(entitySetAnnotations, RdfConstants.SAP_QUICKINFO_FQN, rdfClass.getDescription());
		}
		entitySet.setAnnotations(entitySetAnnotations);

		entitySets.put(entitySet.getName(), entitySet);

		entitySetMapping.put(entitySet.getTypeFQN(), rdfClass);
		entitySetsMapping.put(entitySetName, entitySet);
	}

	private void locateNavigationProperties(boolean withRdfAnnotations, boolean withSapAnnotations,
			Map<String, CsdlEntityType> globalEntityTypes, Map<String, RdfNavigationProperty> navigationPropertyLookup,
			RdfSchema rdfGraph) {
		for (RdfNavigationProperty rdfNavigationProperty : rdfGraph.getNavigationProperties()) {
			// if (!rdfAssociation.isInverse)
			{
				String navigationPropertyName = rdfNavigationProperty.getEDMNavigationPropertyName();
				//TODO Do we need a new navigation property or extend an existing one?
				CsdlNavigationProperty navigationProperty = new CsdlNavigationProperty().setName(navigationPropertyName)
						.setType(new FullQualifiedName(
								rdfNavigationProperty.getRangeClass().getSchema().getSchemaPrefix(),
								rdfNavigationProperty.getRangeName()))
						.setCollection((rdfNavigationProperty.getDomainCardinality() == Cardinality.MANY)
								|| (rdfNavigationProperty.getDomainCardinality() == Cardinality.MULTIPLE));

				List<CsdlAnnotation> navigationPropertyAnnotations = new ArrayList<CsdlAnnotation>();
				if (withRdfAnnotations)
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.PROPERTY_FQN,
							rdfNavigationProperty.getNavigationPropertyIRI().toString());
				if (withSapAnnotations)
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.SAP_LABEL_FQN,
							rdfNavigationProperty.getNavigationPropertyLabel());
				if (withSapAnnotations)
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.SAP_HEADING_FQN,
							rdfNavigationProperty.getNavigationPropertyLabel());
				if (withSapAnnotations)
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.SAP_QUICKINFO_FQN,
							rdfNavigationProperty.getNavigationPropertyLabel());
				if (rdfNavigationProperty.IsInverse()) {
					if (withRdfAnnotations)
						addToAnnotations(navigationPropertyAnnotations, RdfConstants.INVERSEOF_FQN,
								rdfNavigationProperty.getInversePropertyOfURI().toString());
				}
				navigationProperty.setAnnotations(navigationPropertyAnnotations);
				//TODO should not add duplicates to the same entity, even though Olingo accepts them							
				globalEntityTypes.get(rdfNavigationProperty.getDomainNodeURI()).getNavigationProperties()
						.add(navigationProperty);
				navigationPropertyLookup.put(navigationProperty.getName(), rdfNavigationProperty);
				navigationPropertyMapping.put(rdfNavigationProperty.getFullQualifiedName(), rdfNavigationProperty);
			}
		}
	}

	private void locateComplexTypes(RdfSchema rdfGraph, Map<String, CsdlComplexType> complexTypes) {
		for (RdfComplexType rdfComplexType : rdfGraph.getComplexTypes()) {
			CsdlComplexType csdlComplexType = new CsdlComplexType().setName(rdfComplexType.getComplexTypeName());
			for (RdfProperty rdfProperty : rdfComplexType.getProperties().values()) {
				List<CsdlAnnotation> propertyAnnotations = new ArrayList<CsdlAnnotation>();
				if (rdfComplexType.isProvenanceType())
					addToAnnotations(propertyAnnotations, RdfConstants.ODATA_SUBTYPE_FQN,
							rdfProperty.getOfClass().getEntityTypeName());
				CsdlProperty complexProperty = new CsdlProperty().setName(rdfProperty.getEDMPropertyName())
						//.setType(RdfEdmType.getEdmType(rdfProperty.propertyTypeName).getFullQualifiedName())
						.setAnnotations(propertyAnnotations);
				if (rdfProperty.getIsComplex()) {
					complexProperty.setType(rdfProperty.getComplexType().getFullQualifiedName())
							.setCollection((rdfProperty.getCardinality() == Cardinality.MANY)
									|| (rdfProperty.getCardinality() == Cardinality.MULTIPLE));
				} else {
					complexProperty.setType(RdfEdmType.getEdmType(rdfProperty.propertyTypeName).getFullQualifiedName());
				}
				csdlComplexType.getProperties().add(complexProperty);

			}
			for (RdfComplexProperty rdfComplexProperty : rdfComplexType.getComplexProperties().values()) {
				List<CsdlAnnotation> complexPropertyAnnotations = new ArrayList<CsdlAnnotation>();
				CsdlProperty complexProperty = new CsdlProperty().setName(rdfComplexProperty.getComplexPropertyName())
						.setAnnotations(complexPropertyAnnotations);
				complexProperty.setType(rdfComplexProperty.getComplexType().getFullQualifiedName())
						.setCollection((rdfComplexProperty.getCardinality() == Cardinality.MANY)
								|| (rdfComplexProperty.getCardinality() == Cardinality.MULTIPLE));

				csdlComplexType.getProperties().add(complexProperty);

			}
			for (RdfNavigationProperty rdfNavigationProperty : rdfComplexType.getNavigationProperties().values()) {
				List<CsdlAnnotation> navigationPropertyAnnotations = new ArrayList<CsdlAnnotation>();
				if (rdfComplexType.isProvenanceType())
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.ODATA_SUBTYPE_FQN,
							rdfNavigationProperty.getDomainName());

				csdlComplexType.getNavigationProperties()
						.add(new CsdlNavigationProperty().setName(rdfNavigationProperty.getEDMNavigationPropertyName())
								.setAnnotations(navigationPropertyAnnotations)
								.setType(new FullQualifiedName(
										rdfNavigationProperty.getRangeClass().getSchema().getSchemaPrefix(),
										rdfNavigationProperty.getRangeName()))
								.setCollection((rdfNavigationProperty.getDomainCardinality() == Cardinality.MANY)
										|| (rdfNavigationProperty.getDomainCardinality() == Cardinality.MULTIPLE)));
			}
			for (RdfShapedNavigationProperty rdfShapedNavigationProperty : rdfComplexType
					.getShapedNavigationProperties().values()) {
				List<CsdlAnnotation> navigationPropertyAnnotations = new ArrayList<CsdlAnnotation>();
				RdfNavigationProperty rdfNavigationProperty = rdfShapedNavigationProperty.getRdfNavigationProperty();
				if (rdfComplexType.isProvenanceType())
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.ODATA_SUBTYPE_FQN,
							rdfNavigationProperty.getDomainName());

				csdlComplexType.getNavigationProperties()
						.add(new CsdlNavigationProperty().setName(rdfNavigationProperty.getEDMNavigationPropertyName())
								.setAnnotations(navigationPropertyAnnotations)
								.setType(new FullQualifiedName(
										rdfNavigationProperty.getRangeClass().getSchema().getSchemaPrefix(),
										rdfNavigationProperty.getRangeName()))
								.setCollection((rdfShapedNavigationProperty.getCardinality() == Cardinality.MANY)
										|| (rdfShapedNavigationProperty.getCardinality() == Cardinality.MULTIPLE)));
			}
			complexTypes.put(rdfComplexType.getComplexTypeName(), csdlComplexType);
		}
	}

	private void locateEntityTypes(boolean withRdfAnnotations, boolean withSapAnnotations, boolean useBaseType,
			Map<String, CsdlEntityType> globalEntityTypes, RdfSchema rdfGraph) {
		for (RdfEntityType rdfClass : rdfGraph.getClasses()) {
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
			}
			if (rdfClass.getBaseType() != null) {
				ArrayList<String> superTypes = new ArrayList<String>();
				for (RdfEntityType superType : rdfClass.getSuperTypes()) {
					superTypes.add(superType.getODataEntitySetName());
				}
				addToAnnotations(entityTypeAnnotations, RdfConstants.ODATA_BASETYPE_FQN, superTypes);
			}
			entityType.setAnnotations(entityTypeAnnotations);
			//TODO testing openTypes
			//entityType.setOpenType(true);
		}
	}

	private void buildProperty(boolean withRdfAnnotations, boolean withSapAnnotations, RdfProperty rdfProperty,
			CsdlProperty property) {
		List<CsdlAnnotation> propertyAnnotations = new ArrayList<CsdlAnnotation>();
		if (!rdfProperty.propertyName.equals(RdfConstants.SUBJECT)) {
			if (rdfProperty.isFK()) {
				addToAnnotations(propertyAnnotations, RdfConstants.ODATA_FK_FQN,
						rdfProperty.getFkProperty().getNavigationPropertyName());
			}
			if (withRdfAnnotations) {
				addToAnnotations(propertyAnnotations, RdfConstants.PROPERTY_FQN,
						rdfProperty.getPropertyURI().toString());
				addToAnnotations(propertyAnnotations, RdfConstants.DATATYPE_FQN, rdfProperty.propertyTypeName);

				if (rdfProperty.getEquivalentProperty() != null) {
					addToAnnotations(propertyAnnotations, RdfConstants.OWL_EQUIVALENTPROPERTY_FQN,
							rdfProperty.getEquivalentProperty());
				}
			}
			if (withSapAnnotations) {
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_LABEL_FQN, rdfProperty.getPropertyLabel());
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_HEADING_FQN, rdfProperty.getPropertyLabel());
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_QUICKINFO_FQN, rdfProperty.getDescription());
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
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_HEADING_FQN, rdfProperty.getPropertyLabel());
				addToAnnotations(propertyAnnotations, RdfConstants.SAP_QUICKINFO_FQN, rdfProperty.getDescription());
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
			TreeMap<String, CsdlEntitySet> entitySets, RdfEntityType rdfClass, HashSet<RdfEntityType> visited) {
		//		RdfEntityType baseType = rdfClass.getBaseType();
		//		if (baseType != null) {
		if (!rdfClass.getSuperTypes().isEmpty()) {
			for (RdfEntityType baseType : rdfClass.getSuperTypes()) {
				if (!visited.contains(baseType)) {
					visited.add(baseType);
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
					inheritBasetypeNavigationProperties(globalEntityTypes, entitySets, baseType, visited);
				}
			}
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

	public RdfNavigationProperty getMappedNavigationProperty(FullQualifiedName edmNavigationProperty) {
		return navigationPropertyMapping.get(edmNavigationProperty);
	}

}
