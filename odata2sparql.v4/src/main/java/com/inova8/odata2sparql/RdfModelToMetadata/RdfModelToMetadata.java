/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfModelToMetadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.inova8.odata2sparql.Constants.PathQLConstants;
import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;

import org.apache.commons.text.StringEscapeUtils;
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
import org.apache.olingo.commons.api.edm.provider.CsdlTypeDefinition;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlCollection;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlExpression;

import com.inova8.odata2sparql.RdfModel.PathQLProvider;
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

/**
 * The Class RdfModelToMetadata.
 */
public class RdfModelToMetadata {
	
	/**
	 * The Class PrefixedNamespace.
	 */
	public class PrefixedNamespace {
		
		/** The uri. */
		private final String uri;
		
		/** The prefix. */
		private final String prefix;

		/**
		 * Instantiates a new prefixed namespace.
		 *
		 * @param uri the uri
		 * @param prefix the prefix
		 */
		private PrefixedNamespace(String uri, String prefix) {
			this.uri = uri;
			this.prefix = prefix;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return uri + " " + prefix;
		}
	}

	/** The rdf edm. */
	private final TreeMap<String, CsdlSchema> rdfEdm = new TreeMap<String, CsdlSchema>();

	/**
	 * Gets the schemas.
	 *
	 * @return the schemas
	 */
	public List<CsdlSchema> getSchemas() {
		return new ArrayList<CsdlSchema>(rdfEdm.values());
	}

	/**
	 * Gets the schema.
	 *
	 * @param nameSpace the name space
	 * @return the schema
	 */
	public CsdlSchema getSchema(String nameSpace) {
		return rdfEdm.get(nameSpace);
	}

	/**
	 * Gets the term.
	 *
	 * @param termName the term name
	 * @return the term
	 */
	public CsdlTerm getTerm(FullQualifiedName termName) {
		return null;
	}

	/** The entity set mapping. */
	private final Map<FullQualifiedName, RdfEntityType> entitySetMapping = new TreeMap<FullQualifiedName, RdfEntityType>(
			new FullQualifiedNameComparator());
	
	/** The property mapping. */
	private final Map<FullQualifiedName, RdfProperty> propertyMapping = new TreeMap<FullQualifiedName, RdfProperty>(
			new PropertyComparator());
	
	/** The navigation property mapping. */
	private final Map<FullQualifiedName, RdfNavigationProperty> navigationPropertyMapping = new TreeMap<FullQualifiedName, RdfNavigationProperty>(
			new NavigationPropertyComparator());

	/**
	 * Adds the to annotations.
	 *
	 * @param annotations the annotations
	 * @param fqn the fqn
	 * @param text the text
	 */
	private void addToAnnotations(List<CsdlAnnotation> annotations, String fqn, String text) {
		if (text == null || text.isEmpty()) {
		} else {
			annotations.add(new CsdlAnnotation().setTerm(fqn)
					.setExpression(new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.String,
							StringEscapeUtils.escapeXml11(text.replaceAll("\"", "\\\"")))));
		}
	}
	
	/**
	 * Adds the to annotations.
	 *
	 * @param annotations the annotations
	 * @param fqn the fqn
	 * @param text the text
	 */
	public void addToAnnotations(List<CsdlAnnotation> annotations, String fqn, Boolean text) {

			annotations.add(new CsdlAnnotation().setTerm(fqn)
					.setExpression(new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.Bool, text.toString())));
	}
	
	/**
	 * Adds the to annotations.
	 *
	 * @param annotations the annotations
	 * @param fqn the fqn
	 * @param textList the text list
	 */
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

	/**
	 * Instantiates a new rdf model to metadata.
	 *
	 * @param rdfModel the rdf model
	 * @param withRdfAnnotations the with rdf annotations
	 * @param withSapAnnotations the with sap annotations
	 * @param useBaseType the use base type
	 * @param withFKProperties the with FK properties
	 * @param supportScripting the support scripting
	 */
	public RdfModelToMetadata(RdfModel rdfModel, boolean withRdfAnnotations, boolean withSapAnnotations,
			boolean useBaseType, boolean withFKProperties, boolean supportScripting) {
		Map<String, CsdlEntityType> globalEntityTypes = new TreeMap<String, CsdlEntityType>();

		Map<String, RdfNavigationProperty> navigationPropertyLookup = new TreeMap<String, RdfNavigationProperty>();
		Map<String, CsdlEntitySet> entitySetsMapping = new TreeMap<String, CsdlEntitySet>();
		TreeMap<String, CsdlEntitySet> entitySets = new TreeMap<String, CsdlEntitySet>();

		CsdlEntityContainer entityContainer = initializeMetadata(rdfModel);
		CsdlComplexType langLiteralType = createLangType();

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

			locateEntityTypes(withRdfAnnotations, withSapAnnotations,  useBaseType, withFKProperties, supportScripting, globalEntityTypes,
					entitySetsMapping, entitySets, rdfGraph, entityTypes, entityTypeMapping);

			locateNavigationProperties(withRdfAnnotations, withSapAnnotations, supportScripting, globalEntityTypes,
					navigationPropertyLookup, rdfGraph);
			locateComplexTypes(rdfGraph, complexTypes);

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

				}
				if (modelNamespace.equals(RdfConstants.RDFS)) {					

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

		CsdlSchema modelSchema = this.getSchema(PathQLConstants.PATHQL);
		modelSchema.getFunctions().addAll(	PathQLProvider.locateFunctionImports(rdfModel, entityContainer));
//		CsdlSchema instancesSchema = this.getSchema(RdfConstants.ENTITYCONTAINERNAMESPACE);
//		instancesSchema.getFunctions().addAll(	PathQLProvider.locateFunctionImports(rdfModel, entityContainer));
		entityContainer.setEntitySets(new ArrayList<CsdlEntitySet>(entitySets.values()));

		//Finally, add terms and schemas to which they belong if they do not exist that have been used	
		addTermsToSchemas();
		addTypeDefinitionsToSchemas(rdfModel);
	}
	
	/**
	 * Adds the type definitions to schemas.
	 *
	 * @param rdfModel the rdf model
	 */
	private void addTypeDefinitionsToSchemas(RdfModel rdfModel) {

		for( RdfModel.RdfSchema rdfGraph: rdfModel.graphs) {
			CsdlSchema schema = getSchema(rdfGraph.getSchemaPrefix());
			if(schema!=null) {
				List<CsdlTypeDefinition> csdlTypeDefinitions = new ArrayList<CsdlTypeDefinition>();
				for( RdfModel.RdfDatatype rdfDatatype  : rdfGraph.getDatatypes()) {
					CsdlTypeDefinition csdlTypeDefinition = new CsdlTypeDefinition();
					csdlTypeDefinition.setName(rdfDatatype.getDatatypeName()).setUnderlyingType(rdfDatatype.getEDMBasetypeName().getFullQualifiedName());
					csdlTypeDefinitions.add(csdlTypeDefinition);
				}
				if (!csdlTypeDefinitions.isEmpty()) {
					schema.setTypeDefinitions(csdlTypeDefinitions);
				}
			}
		}
	}
	
	/**
	 * Initialize metadata.
	 *
	 * @param rdfModel the rdf model
	 * @return the csdl entity container
	 */
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
		addToAnnotations(instanceSchemaAnnotations, RdfConstants.ODATA_SUPPORTSCRIPTING_FQN,
				rdfModel.getRdfRepository().isSupportScripting());		
		addNamespacesAnnotation(rdfModel, instanceSchemaAnnotations);

		instanceSchema.setAnnotations(instanceSchemaAnnotations);
		
		return entityContainer;
	}

	/**
	 * Adds the namespaces annotation.
	 *
	 * @param rdfModel the rdf model
	 * @param instanceSchemaAnnotations the instance schema annotations
	 */
	private void addNamespacesAnnotation(RdfModel rdfModel, List<CsdlAnnotation> instanceSchemaAnnotations) {
		CsdlCollection prefixes = new CsdlCollection();
		ArrayList<CsdlExpression> prefixAnnotations = new ArrayList<CsdlExpression>();
		
		for(  Entry<String, String> rdfPrefixEntry: rdfModel.getRdfPrefixes().getPrefixes().entrySet()) {
			prefixAnnotations.add(
					new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.String,
					(rdfPrefixEntry.getKey() + ": <"+rdfPrefixEntry.getValue()+">")
					//StringEscapeUtils.escapeXml11(("@prefix " + RdfConstants.XSD + ": <"+RdfConstants.XSD_SCHEMA+">").replaceAll("\"", "\\\""))
					)
					)
			;
		}
		prefixes.setItems(prefixAnnotations);
		
		instanceSchemaAnnotations.add(new CsdlAnnotation().setTerm(RdfConstants.ODATA_NAMESPACES_FQN).setExpression(prefixes));
	}

	/**
	 * Creates the lang type.
	 *
	 * @return the csdl complex type
	 */
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
	
	/**
	 * Locate function imports.
	 *
	 * @param rdfModel the rdf model
	 * @param entityContainer the entity container
	 */
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

	/**
	 * Locate navigation property binding.
	 *
	 * @param entitySets the entity sets
	 * @param rdfGraph the rdf graph
	 */
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

	/**
	 * Locate entity types.
	 *
	 * @param withRdfAnnotations the with rdf annotations
	 * @param withSapAnnotations the with sap annotations
	 * @param useBaseType the use base type
	 * @param withFKProperties the with FK properties
	 * @param supportScripting the support scripting
	 * @param globalEntityTypes the global entity types
	 * @param entitySetsMapping the entity sets mapping
	 * @param entitySets the entity sets
	 * @param rdfGraph the rdf graph
	 * @param entityTypes the entity types
	 * @param entityTypeMapping the entity type mapping
	 */
	private void locateEntityTypes(boolean withRdfAnnotations, boolean withSapAnnotations, boolean useBaseType,
			boolean withFKProperties,boolean supportScripting, Map<String, CsdlEntityType> globalEntityTypes,
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
					//	CsdlProperty annotationProperty= null;
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
							
						//	annotationProperty = new CsdlProperty().setName(propertyName+"@odata.annotation")
						//			.setType(propertyType.getFullQualifiedName());
						}
						if (property != null) {
							//Only build if not null
							buildProperty(withRdfAnnotations, withSapAnnotations, supportScripting, rdfProperty, property);
							entityTypeProperties.put(property.getName(), property);
						//	entityTypeProperties.put(annotationProperty.getName(), annotationProperty);
							propertyMapping.put(
									new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), property.getName()),
									rdfProperty);
						//	propertyMapping.put(
						//			new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), annotationProperty.getName()),
						//			rdfProperty);

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

	/**
	 * Adds the entity type to entity sets.
	 *
	 * @param withSapAnnotations the with sap annotations
	 * @param entitySetsMapping the entity sets mapping
	 * @param entitySets the entity sets
	 * @param rdfClass the rdf class
	 */
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
		if (rdfClass.isReified()) {
			addToAnnotations(entitySetAnnotations, RdfConstants.ODATA_ISREIFIEDSTATEMENT_FQN, rdfClass.isReified().toString());
		}
		entitySet.setAnnotations(entitySetAnnotations);

		entitySets.put(entitySet.getName(), entitySet);

		entitySetMapping.put(entitySet.getTypeFQN(), rdfClass);
		entitySetsMapping.put(entitySetName, entitySet);
	}

	/**
	 * Locate navigation properties.
	 *
	 * @param withRdfAnnotations the with rdf annotations
	 * @param withSapAnnotations the with sap annotations
	 * @param supportScripting the support scripting
	 * @param globalEntityTypes the global entity types
	 * @param navigationPropertyLookup the navigation property lookup
	 * @param rdfGraph the rdf graph
	 */
	private void locateNavigationProperties(boolean withRdfAnnotations, boolean withSapAnnotations,boolean supportScripting,
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
								|| (rdfNavigationProperty.getDomainCardinality() == Cardinality.MULTIPLE))
						.setNullable(rdfNavigationProperty.getDomainCardinality() == Cardinality.ZERO_TO_ONE ||rdfNavigationProperty.getDomainCardinality() == Cardinality.MANY  );

				List<CsdlAnnotation> navigationPropertyAnnotations = new ArrayList<CsdlAnnotation>();
				if(supportScripting) {
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.PROPERTY_FQN,
							rdfNavigationProperty.getNavigationPropertyIRI().toString());
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.DATATYPE_FQN, navigationPropertyName);				
					
				}
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
					if (withRdfAnnotations) {
						addToAnnotations(navigationPropertyAnnotations, RdfConstants.INVERSEOF_FQN,
								rdfNavigationProperty.getInversePropertyOfURI().toString());
					}
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.ODATA_INVERSEOF_FQN,
							rdfNavigationProperty.getInverseNavigationProperty().getDomainName().toString() +"/"+
					rdfNavigationProperty.getInverseNavigationProperty().getEDMNavigationPropertyName().toString());
				}
				if (rdfNavigationProperty.isReifiedSubjectPredicate()) {
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.ODATA_ISREIFIEDSUBJECTPREDICATE_FQN,
							rdfNavigationProperty.isReifiedSubjectPredicate().toString());
				}
				if (rdfNavigationProperty.isReifiedObjectPredicate()) {
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.ODATA_ISREIFIEDOBJECTPREDICATE_FQN,
							rdfNavigationProperty.isReifiedObjectPredicate().toString());
				}
				if (rdfNavigationProperty.isReifiedPredicate()) {
					addToAnnotations(navigationPropertyAnnotations, RdfConstants.ODATA_ISREIFIEDPREDICATE_FQN,
							rdfNavigationProperty.isReifiedPredicate().toString());
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

	/**
	 * Locate complex types.
	 *
	 * @param rdfGraph the rdf graph
	 * @param complexTypes the complex types
	 */
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
									|| (rdfProperty.getCardinality() == Cardinality.MULTIPLE))
							.setNullable(rdfProperty.getCardinality() == Cardinality.ZERO_TO_ONE||rdfProperty.getCardinality() == Cardinality.MANY  );
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
								|| (rdfComplexProperty.getCardinality() == Cardinality.MULTIPLE))
						.setNullable(rdfComplexProperty.getCardinality() == Cardinality.ZERO_TO_ONE||rdfComplexProperty.getCardinality() == Cardinality.MANY);

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
										|| (rdfNavigationProperty.getDomainCardinality() == Cardinality.MULTIPLE))
								.setNullable(rdfNavigationProperty.getDomainCardinality() == Cardinality.ZERO_TO_ONE||rdfNavigationProperty.getDomainCardinality() == Cardinality.MANY));
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
										|| (rdfShapedNavigationProperty.getCardinality() == Cardinality.MULTIPLE))
								.setNullable(rdfShapedNavigationProperty.getCardinality() == Cardinality.ZERO_TO_ONE||rdfShapedNavigationProperty.getCardinality()== Cardinality.MANY));
			}
			complexTypes.put(rdfComplexType.getComplexTypeName(), csdlComplexType);
		}
	}

	/**
	 * Locate entity types.
	 *
	 * @param withRdfAnnotations the with rdf annotations
	 * @param withSapAnnotations the with sap annotations
	 * @param useBaseType the use base type
	 * @param globalEntityTypes the global entity types
	 * @param rdfGraph the rdf graph
	 */
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
			if(rdfClass.isReified()) {
				addToAnnotations(entityTypeAnnotations, RdfConstants.ODATA_ISREIFIEDSTATEMENT_FQN, rdfClass.isReified().toString());
			}
			entityType.setAnnotations(entityTypeAnnotations);
			//TODO testing openTypes
			entityType.setOpenType(true);
		}
	}

	/**
	 * Builds the property.
	 *
	 * @param withRdfAnnotations the with rdf annotations
	 * @param withSapAnnotations the with sap annotations
	 * @param supportScripting the support scripting
	 * @param rdfProperty the rdf property
	 * @param property the property
	 */
	private void buildProperty(boolean withRdfAnnotations, boolean withSapAnnotations, boolean supportScripting,RdfProperty rdfProperty,
			CsdlProperty property) {
		List<CsdlAnnotation> propertyAnnotations = new ArrayList<CsdlAnnotation>();
		if (!rdfProperty.propertyName.equals(RdfConstants.SUBJECT)) {
			if (rdfProperty.isFK()) {
				addToAnnotations(propertyAnnotations, RdfConstants.ODATA_FK_FQN,
						rdfProperty.getFkProperty().getNavigationPropertyName());
			}
			if(RdfEdmType.isAnyUri(rdfProperty.getPropertyTypeName())) {
				addToAnnotations(propertyAnnotations, RdfConstants.ODATA_RDFTYPE_FQN,
						RdfEdmType.anyUri());
			}
			if (rdfProperty.isReifiedObjectPredicate()) {
				addToAnnotations(propertyAnnotations, RdfConstants.ODATA_ISREIFIEDOBJECTPREDICATE_FQN,
						rdfProperty.isReifiedObjectPredicate().toString());
			}
			if(supportScripting) {
				addToAnnotations(propertyAnnotations, RdfConstants.PROPERTY_FQN,
						rdfProperty.getPropertyURI().toString());
				addToAnnotations(propertyAnnotations, RdfConstants.DATATYPE_FQN, rdfProperty.propertyTypeName);				
				
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

	/**
	 * Adds the terms to schemas.
	 */
	private void addTermsToSchemas() {
		CsdlSchema sapSchema = rdfEdm.get(RdfConstants.SAP_ANNOTATION_NS);
		if (sapSchema == null) {
			rdfEdm.put(RdfConstants.SAP_ANNOTATION_NS, new CsdlSchema().setNamespace(RdfConstants.SAP_ANNOTATION_NS));
		}
		rdfEdm.get(RdfConstants.SAP_ANNOTATION_NS).setTerms(RdfConstants.SAPTERMS);

		CsdlSchema odataSchema = rdfEdm.get(RdfConstants.ODATA_NS);
		if (odataSchema == null) {
			rdfEdm.put(RdfConstants.ODATA_NS, new CsdlSchema().setNamespace(RdfConstants.ODATA_NS));
		}
		rdfEdm.get(RdfConstants.ODATA_NS).setTerms(RdfConstants.ODATATERMS);

		CsdlSchema rdfSchema = rdfEdm.get(RdfConstants.RDF_NS);
		if (rdfSchema == null) {
			rdfEdm.put(RdfConstants.RDF_NS, new CsdlSchema().setNamespace(RdfConstants.RDF_NS));
		}
		rdfEdm.get(RdfConstants.RDF_NS).setTerms(RdfConstants.RDFTERMS);

		CsdlSchema rdfsSchema = rdfEdm.get(RdfConstants.RDFS_NS);
		if (rdfsSchema == null) {
			rdfEdm.put(RdfConstants.RDFS_NS, new CsdlSchema().setNamespace(RdfConstants.RDFS_NS));
		}
		rdfEdm.get(RdfConstants.RDFS_NS).setTerms(RdfConstants.RDFSTERMS);

		CsdlSchema owlSchema = rdfEdm.get(RdfConstants.OWL_NS);
		if (owlSchema == null) {
			rdfEdm.put(RdfConstants.OWL_NS, new CsdlSchema().setNamespace(RdfConstants.OWL_NS));
		}
		rdfEdm.get(RdfConstants.OWL_NS).setTerms(RdfConstants.OWLTERMS);
	}

	/**
	 * Inherit basetype navigation properties.
	 *
	 * @param globalEntityTypes the global entity types
	 * @param entitySets the entity sets
	 * @param rdfClass the rdf class
	 * @param visited the visited
	 */
	private void inheritBasetypeNavigationProperties(Map<String, CsdlEntityType> globalEntityTypes,
			TreeMap<String, CsdlEntitySet> entitySets, RdfEntityType rdfClass, HashSet<RdfEntityType> visited) {
		//		RdfEntityType baseType = rdfClass.getBaseType();
		//		if (baseType != null) {
		if (!rdfClass.getSuperTypes().isEmpty()) {
			for (RdfEntityType baseType : rdfClass.getSuperTypes()) {
				if (!visited.contains(baseType)) {
					//Fixes #205 by reordering
					inheritBasetypeNavigationProperties(globalEntityTypes, entitySets, baseType, visited);
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
//Fixes #205					inheritBasetypeNavigationProperties(globalEntityTypes, entitySets, baseType, visited);
				}
			}
		}
	}

	/**
	 * Gets the mapped entity type.
	 *
	 * @param fqnEntityType the fqn entity type
	 * @return the mapped entity type
	 */
	public RdfEntityType getMappedEntityType(FullQualifiedName fqnEntityType) {
		return entitySetMapping.get(fqnEntityType);
	}

	/**
	 * Gets the rdf entity typefrom edm entity set.
	 *
	 * @param edmEntitySet the edm entity set
	 * @return the rdf entity typefrom edm entity set
	 * @throws EdmException the edm exception
	 */
	public RdfEntityType getRdfEntityTypefromEdmEntitySet(EdmEntitySet edmEntitySet) throws EdmException {
		return this.getMappedEntityType(new FullQualifiedName(edmEntitySet.getEntityType().getNamespace(),
				edmEntitySet.getEntityType().getName()));
	}

	/**
	 * Gets the mapped property.
	 *
	 * @param fqnProperty the fqn property
	 * @return the mapped property
	 */
	public RdfProperty getMappedProperty(FullQualifiedName fqnProperty) {
		return propertyMapping.get(fqnProperty);
	}

	/**
	 * Gets the mapped navigation property.
	 *
	 * @param edmNavigationProperty the edm navigation property
	 * @return the mapped navigation property
	 */
	public RdfNavigationProperty getMappedNavigationProperty(FullQualifiedName edmNavigationProperty) {
		return navigationPropertyMapping.get(edmNavigationProperty);
	}

}
