package com.inova8.odata2sparql.RdfModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNodeFactory;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfQuerySolution;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfComplexType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfDatatype;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfSchema;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

public class RdfModelProvider {
	private final Logger log = LoggerFactory.getLogger(RdfModelProvider.class);
	private final RdfModel model;
	private TreeSet<RdfComplexType> complexTypes = new TreeSet<RdfComplexType>();
	private final RdfMetamodelProvider rdfMetamodelProvider;

	public RdfModelProvider(RdfRepository rdfRepository) {
		super();
		this.rdfMetamodelProvider = new RdfMetamodelProvider(rdfRepository);
		model = new RdfModel(rdfRepository);
	}

	public RdfModel getRdfModel() throws Exception {
		log.info("Loading model " + model.getRdfRepository().getModelName() + " from endpoint: "
				+ model.getRdfRepository().getModelRepository().getRepository().toString());
		RdfEntityType rdfsResource = initializeCore();
		getClasses();
		getDatatypes();
		getDataTypeProperties();
		getObjectProperties();
		getInverseProperties();
		getOperations();
		getOperationAssociationResults();
		getOperationPropertyResults();
		getOperationArguments();
		getNodeShapes();
		getPropertyShapes();
		getReifiedStatements();
		cleanupReifiedPredicates();
		cleanupOrphanClasses(rdfsResource);
		if (rdfMetamodelProvider.getRdfRepository().getWithFKProperties())
			createFKProperties();
		model.getRdfPrefixes().log();
		return model;
	}

	private RdfEntityType initializeCore() throws OData2SparqlException {
		RdfSchema defaultGraph = model.getOrCreateGraph(rdfMetamodelProvider.getRdfRepository().defaultNamespace(),
				rdfMetamodelProvider.getRdfRepository().getDefaultPrefix());
		defaultGraph.isDefault = true;
		model.getOrCreatePrefix(rdfMetamodelProvider.getRdfRepository().getDefaultPrefix(),
				rdfMetamodelProvider.getRdfRepository().defaultNamespace());

		for (Entry<String, Namespace> namespaceEntry : rdfMetamodelProvider.getRdfRepository().getNamespaces()
				.entrySet()) {
			model.getOrCreatePrefix(namespaceEntry.getKey(), namespaceEntry.getValue().getName());
		}

		RdfNode rdfStringNode = RdfNodeFactory.createURI(RdfConstants.XSD_STRING);

		RdfNode rdfsResourceNode = RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE);
		RdfNode rdfsResourceLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_RESOURCE_LABEL);
		RdfNode owlThingNode = RdfNodeFactory.createURI(RdfConstants.OWL_THING);
		RdfNode owlThingLabelNode = RdfNodeFactory.createLiteral(RdfConstants.OWL_THING_LABEL);
		RdfNode rdfsClassNode = RdfNodeFactory.createURI(RdfConstants.RDFS_CLASS);
		RdfNode rdfsClassLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_CLASS_LABEL);
		RdfNode owlClassNode = RdfNodeFactory.createURI(RdfConstants.OWL_CLASS);
		RdfNode owlClassLabelNode = RdfNodeFactory.createLiteral(RdfConstants.OWL_CLASS_LABEL);
		RdfNode rdfPropertyNode = RdfNodeFactory.createURI(RdfConstants.RDF_PROPERTY);
		RdfNode rdfPropertyLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_PROPERTY_LABEL);
		RdfNode owlObjectPropertyNode = RdfNodeFactory.createURI(RdfConstants.OWL_OBJECTPROPERTY);
		RdfNode owlObjectPropertyLabelNode = RdfNodeFactory.createLiteral(RdfConstants.OWL_OBJECTPROPERTY_LABEL);
		RdfNode owlDatatypePropertyNode = RdfNodeFactory.createURI(RdfConstants.OWL_DATATYPEPROPERTY);
		RdfNode owlDatatypePropertyLabelNode = RdfNodeFactory.createLiteral(RdfConstants.OWL_DATATYPEPROPERTY_LABEL);
		RdfNode owlOntologyNode = RdfNodeFactory.createURI(RdfConstants.OWL_ONTOLOGY);
		RdfNode owlOntologyLabelNode = RdfNodeFactory.createLiteral(RdfConstants.OWL_ONTOLOGY_LABEL);

		RdfNode rdfSubjectNode = RdfNodeFactory.createURI(RdfConstants.RDF_SUBJECT);
		RdfNode rdfSubjectLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_SUBJECT_LABEL);
		RdfNodeFactory.createURI(RdfConstants.RDF_TYPE);
		RdfNodeFactory.createLiteral(RdfConstants.RDF_TYPE_LABEL);
		RdfNodeFactory.createURI(RdfConstants.RDF_INVERSE_TYPE);
		RdfNodeFactory.createLiteral(RdfConstants.RDF_INVERSE_TYPE_LABEL);
		RdfNode rdfsSubClassOfNode = RdfNodeFactory.createURI(RdfConstants.RDFS_SUBCLASSOF);
		RdfNode rdfsSubClassOfLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_SUBCLASSOF_LABEL);
		RdfNodeFactory.createURI(RdfConstants.RDFS_DOMAIN);
		RdfNodeFactory.createLiteral(RdfConstants.RDFS_DOMAIN_LABEL);
		RdfNodeFactory.createURI(RdfConstants.RDFS_RANGE);
		RdfNodeFactory.createLiteral(RdfConstants.RDFS_RANGE_LABEL);
		RdfNode owlImportsNode = RdfNodeFactory.createURI(RdfConstants.OWL_IMPORTS);
		RdfNode owlImportsLabelNode = RdfNodeFactory.createLiteral(RdfConstants.OWL_IMPORTS_LABEL);

		RdfNode unityNode = RdfNodeFactory.createLiteral("1");

		RdfEntityType rdfsResource = model.getOrCreateEntityType(rdfsResourceNode, rdfsResourceLabelNode);
		rdfsResource.setRootClass(true);
		RdfProperty resourceSubjectProperty = model.getOrCreateProperty(rdfSubjectNode, null, rdfSubjectLabelNode,
				rdfsResourceNode, rdfStringNode, RdfConstants.Cardinality.MANY);
		resourceSubjectProperty.setIsKey(true);
		RdfPrimaryKey resourcePrimaryKey = new RdfPrimaryKey(RdfModel.KEY(rdfsResource), RdfModel.KEY(rdfsResource));
		rdfsResource.primaryKeys.put(RdfModel.KEY(rdfsResource), resourcePrimaryKey);

		RdfNode rdfsLabelNode = RdfNodeFactory.createURI(RdfConstants.RDFS_LABEL);
		RdfNode rdfsLabelLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_LABEL_LABEL);
		model.getOrCreateProperty(rdfsLabelNode, null, rdfsLabelLabelNode, rdfsResourceNode, rdfStringNode,
				RdfConstants.Cardinality.ZERO_TO_ONE);

		RdfNode rdfsCommentNode = RdfNodeFactory.createURI(RdfConstants.RDFS_COMMENT);
		RdfNode rdfsCommentLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_COMMENT_LABEL);
		model.getOrCreateProperty(rdfsCommentNode, null, rdfsCommentLabelNode, rdfsResourceNode, rdfStringNode,
				RdfConstants.Cardinality.ZERO_TO_ONE);

		model.getOrCreateEntityType(owlThingNode, owlThingLabelNode, rdfsResource);
		RdfEntityType rdfsClass = model.getOrCreateEntityType(rdfsClassNode, rdfsClassLabelNode, rdfsResource);
		model.getOrCreateEntityType(owlClassNode, owlClassLabelNode, rdfsClass);
		model.getOrCreateEntityType(owlOntologyNode, owlOntologyLabelNode, rdfsClass);
		RdfEntityType rdfProperty = model.getOrCreateEntityType(rdfPropertyNode, rdfPropertyLabelNode, rdfsResource);
		model.getOrCreateEntityType(owlObjectPropertyNode, owlObjectPropertyLabelNode, rdfProperty);
		model.getOrCreateEntityType(owlDatatypePropertyNode, owlDatatypePropertyLabelNode, rdfProperty);
		model.getOrCreateNavigationProperty(rdfsSubClassOfNode, rdfsSubClassOfLabelNode, rdfsClassNode, rdfsClassNode,
				unityNode, unityNode, RdfConstants.Cardinality.ZERO_TO_ONE, RdfConstants.Cardinality.MANY);
		model.getOrCreateNavigationProperty(owlImportsNode, owlImportsLabelNode, owlOntologyNode, owlOntologyNode,
				unityNode, unityNode, RdfConstants.Cardinality.ZERO_TO_ONE, RdfConstants.Cardinality.MANY);

		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_TYPE),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_TYPE_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE), RdfNodeFactory.createURI(RdfConstants.RDFS_CLASS),
				unityNode, unityNode, RdfConstants.Cardinality.MANY, RdfConstants.Cardinality.MANY);
		
		if (this.rdfMetamodelProvider.getRdfRepository().isIncludeImplicitRDF()) {
			includeImplicitRDF(rdfStringNode, unityNode, rdfsResource);
		}
//		if (this.rdfMetamodelProvider.getRdfRepository().isSupportScripting()) {
//			addSupportScripting(unityNode);
//		}
		return rdfsResource;
	}

	protected void includeImplicitRDF(RdfNode rdfStringNode, RdfNode unityNode, RdfEntityType rdfsResource)
			throws OData2SparqlException {
		//RDF MetaModel
		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_STATEMENT),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_STATEMENT_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE),
				RdfNodeFactory.createURI(RdfConstants.RDF_STATEMENT), unityNode, unityNode,
				RdfConstants.Cardinality.MANY, RdfConstants.Cardinality.ONE);
		RdfNode rdfSubjectPredicateNode = RdfNodeFactory.createURI(RdfConstants.RDF_SUBJECTPREDICATE);
		RdfNode rdfSubjectPredicateLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_SUBJECTPREDICATE_LABEL);
		model.getOrCreateEntityType(rdfSubjectPredicateNode, rdfSubjectPredicateLabelNode, rdfsResource);
		RdfNode rdfObjectPredicateNode = RdfNodeFactory.createURI(RdfConstants.RDF_OBJECTPREDICATE);
		RdfNode rdfObjectPredicateLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_OBJECTPREDICATE_LABEL);
		model.getOrCreateEntityType(rdfObjectPredicateNode, rdfObjectPredicateLabelNode, rdfsResource);
		RdfNode rdfValueNode = RdfNodeFactory.createURI(RdfConstants.RDF_VALUE);
		RdfNode rdfValueLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_VALUE_LABEL);
		model.getOrCreateEntityType(rdfValueNode, rdfValueLabelNode, rdfsResource);



		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_HASFACTS),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_HASFACTS_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE),
				RdfNodeFactory.createURI(RdfConstants.RDF_OBJECTPREDICATE), unityNode, unityNode,
				RdfConstants.Cardinality.MANY, RdfConstants.Cardinality.MANY);
		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_HASPREDICATE),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_HASPREDICATE_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDF_OBJECTPREDICATE),
				RdfNodeFactory.createURI(RdfConstants.RDF_PROPERTY), unityNode, unityNode, RdfConstants.Cardinality.ONE,
				RdfConstants.Cardinality.ONE);
		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_HASVALUES),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_HASVALUES_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDF_OBJECTPREDICATE),
				RdfNodeFactory.createURI(RdfConstants.RDF_VALUE), unityNode, unityNode, RdfConstants.Cardinality.MANY,
				RdfConstants.Cardinality.MANY);
		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_HASOBJECTVALUE),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_HASOBJECTVALUE_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDF_VALUE), RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE),
				unityNode, unityNode, RdfConstants.Cardinality.ONE, RdfConstants.Cardinality.ONE);
		model.getOrCreateProperty(RdfNodeFactory.createURI(RdfConstants.RDF_HASDATAVALUE), null,
				RdfNodeFactory.createLiteral(RdfConstants.RDF_HASDATAVALUE_LABEL), rdfValueNode, rdfStringNode,
				RdfConstants.Cardinality.ZERO_TO_ONE);
		model.getOrCreateProperty(RdfNodeFactory.createURI(RdfConstants.RDF_OBJECTVALUE), null,
				RdfNodeFactory.createLiteral(RdfConstants.RDF_OBJECTVALUE_LABEL), rdfValueNode, rdfStringNode,
				RdfConstants.Cardinality.ONE);
		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_ISOBJECTIVE),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_ISOBJECTIVE_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE),
				RdfNodeFactory.createURI(RdfConstants.RDF_SUBJECTPREDICATE), unityNode, unityNode,
				RdfConstants.Cardinality.MANY, RdfConstants.Cardinality.MANY);
		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_ISPREDICATEOF),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_ISPREDICATEOF_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDF_SUBJECTPREDICATE),
				RdfNodeFactory.createURI(RdfConstants.RDF_PROPERTY), unityNode, unityNode, RdfConstants.Cardinality.ONE,
				RdfConstants.Cardinality.ONE);
		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_HASSUBJECTS),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_HASSUBJECTS_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDF_SUBJECTPREDICATE),
				RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE), unityNode, unityNode,
				RdfConstants.Cardinality.MANY, RdfConstants.Cardinality.MANY);
	}

	private void addSupportScripting(RdfNode unityNode) throws OData2SparqlException {
		
		RdfNode rdfFactValueNode = RdfNodeFactory.createURI(RdfConstants.RDF_FACTVALUE);
		RdfNode rdfFactValueLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_FACTVALUE_LABEL);
		RdfEntityType rdfFactValueEntityType = model.getOrCreateEntityType(rdfFactValueNode, rdfFactValueLabelNode);
		rdfFactValueEntityType.setBaseType(null);
		RdfNode rdfFactValueValueNode = RdfNodeFactory.createURI(RdfConstants.RDF_FACTVALUE_VALUE);
		RdfNode rdfFactValueValueLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_FACTVALUE_VALUE_LABEL);
		model.getOrCreateProperty(rdfFactValueValueNode, rdfFactValueValueLabelNode, rdfFactValueNode);
		RdfNode rdfFactValueScriptNode = RdfNodeFactory.createURI(RdfConstants.RDF_FACTVALUE_SCRIPT);
		RdfNode rdfFactValueScriptLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_FACTVALUE_SCRIPT_LABEL);
		model.getOrCreateProperty(rdfFactValueScriptNode, rdfFactValueScriptLabelNode, rdfFactValueNode);
		RdfNode rdfFactValueTraceNode = RdfNodeFactory.createURI(RdfConstants.RDF_FACTVALUE_TRACE);
		RdfNode rdfFactValueTraceLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_FACTVALUE_TRACE_LABEL);
		model.getOrCreateProperty(rdfFactValueTraceNode, rdfFactValueTraceLabelNode, rdfFactValueNode);
		model.getOrCreateNavigationProperty(RdfNodeFactory.createURI(RdfConstants.RDF_FACTVALUE),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_FACTVALUE_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE), RdfNodeFactory.createURI(RdfConstants.RDF_FACTVALUE),
				unityNode, unityNode, RdfConstants.Cardinality.MANY, RdfConstants.Cardinality.MANY);
	}

	private void getClasses() throws OData2SparqlException {

		// Classes
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet classes = rdfMetamodelProvider.getClasses();
			try {
				while (classes.hasNext()) {
					RdfNode classNode = null;
					try {
						RdfQuerySolution soln = classes.nextSolution();
						classNode = soln.getRdfNode("class");
						RdfNode classLabelNode = null;
						if (soln.getRdfNode("classLabel") != null) {
							classLabelNode = soln.getRdfNode("classLabel");
						}
						RdfNode baseTypeNode = soln.getRdfNode("baseType");
						if (!baseTypeNode.isBlank()) {
							RdfEntityType baseType = model.getOrCreateEntityType(baseTypeNode);
							if (!classNode.isBlank()) {
								RdfEntityType entityType;
								if (classNode.getIRI().toString().equals(RdfConstants.RDFS_RESOURCE)
								// make statement the same as all others|| classNode.getIRI().toString().equals(RdfConstants.RDF_STATEMENT)
								) {
									//Special cases where we do not want to define basetypes so that OData aligns with RDF/RDFS/OWL
									entityType = model.getOrCreateEntityType(classNode, classLabelNode);
								} else {
									entityType = model.getOrCreateEntityType(classNode, classLabelNode);
									if (entityType != baseType)
										entityType.setBaseType(baseType);
									count++;
									debug.append(classNode.getIRI().toString()).append(";");
								}
								entityType.setEntity(true);
								if (soln.getRdfNode("description") != null) {
									entityType.setDescription(
											soln.getRdfNode("description").getLiteralValue().getLabel());
								}
							}
						} else {
							if (!classNode.isBlank()) {
								model.getOrCreateEntityType(classNode, classLabelNode).setEntity(true);
								count++;
								debug.append(classNode.getIRI().toString()).append(";");
							}
						}
					} catch (Exception e) {
						log.info("Failed to create class:" + classNode.getIRI().toString() + " with exception "
								+ e.getMessage());
					}
				}
			} finally {
				log.info(count + " Classes found [" + debug + "]");
				classes.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Class query. Check availability of triple store. Exception " + e.getMessage());
			throw new OData2SparqlException("Classes query exception. Check availability of triple store.", e);
		}
	}

	private void getDatatypes() throws OData2SparqlException {
		// Datatypes
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet datatypes = rdfMetamodelProvider.getDatatypes();
			try {
				while (datatypes.hasNext()) {
					RdfNode datatypeNode = null;
					RdfNode basetypeNode = null;
					try {
						RdfQuerySolution soln = datatypes.nextSolution();
						datatypeNode = soln.getRdfNode("datatype");
						basetypeNode = soln.getRdfNode("basetype");
						@SuppressWarnings("unused")
						RdfDatatype datatype = model.getOrCreateDatatype(datatypeNode, basetypeNode);
						count++;
						debug.append(datatypeNode.getIRI().toString()).append(";");
					} catch (Exception e) {
						log.info("Failed to create datatype:" + datatypeNode.getIRI().toString() + " with exception "
								+ e.getMessage());
					}
				}
			} finally {
				log.info(count + " Datatypes found [" + debug + "]");
				datatypes.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Datatypes query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("Datatypes query exception ", e);
		}
	}

	private void getDataTypeProperties() throws OData2SparqlException {
		TreeMap<String, TreeSet<RdfEntityType>> propertyClasses = new TreeMap<>();
		getDataTypeProperties_Domains(propertyClasses);
		getDataTypeProperties_Ranges(propertyClasses);
		getDataTypeProperties_Cardinality(propertyClasses);
		removeIncompleteProperties(propertyClasses);
	}

	private void getDataTypeProperties_Domains(TreeMap<String, TreeSet<RdfEntityType>> propertyClasses)
			throws OData2SparqlException {
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet properties = rdfMetamodelProvider.getProperty_Domains();
			try {
				while (properties.hasNext()) {
					RdfNode propertyNode = null;
					try {
						RdfQuerySolution soln = properties.nextSolution();
						propertyNode = soln.getRdfNode("property");
						RdfNode propertyLabelNode = null;
						if (soln.getRdfNode("propertyLabel") != null) {
							propertyLabelNode = soln.getRdfNode("propertyLabel");
						}

						RdfNode domainNode = soln.getRdfNode("domain");
						if (domainNode.isBlank()) {
							log.error("Failed to create property:" + propertyNode.getIRI().toString()
									+ " because domain is a blank node. Probably domain is defined as an owl:Restriction which is not currently supported");
						} else {
							RdfProperty datatypeProperty = getOrCreateSuperProperty(propertyNode, soln,
									propertyLabelNode, domainNode);
							if (soln.getRdfNode("description") != null) {
								datatypeProperty
										.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
							}
							if (datatypeProperty.getOfClass() != null) {
								TreeSet<RdfEntityType> classes = propertyClasses.get(propertyNode.getIRI().toString());
								if (classes == null) {
									classes = new TreeSet<RdfEntityType>();
									classes.add(datatypeProperty.getOfClass());
									propertyClasses.put(propertyNode.getIRI().toString(), classes);
								} else {
									classes.add(datatypeProperty.getOfClass());
								}
							}
							count++;
							debug.append(propertyNode.getIRI().toString()).append(";");
						}
					} catch (Exception e) {
						log.info("Failed to create property:" + propertyNode.getIRI().toString() + " with exception "
								+ e.getMessage());
					}
				}
			} finally {
				//log.info(count + " DataTypeProperties_Domains found [" + debug + "]");
				log.info(count + " DataTypeProperties_Domains found");
				properties.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Datatype property query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("DataTypeProperties_Domains query exception ", e);
		}
	}

	private RdfProperty getOrCreateSuperProperty(RdfNode propertyNode, RdfQuerySolution soln, RdfNode propertyLabelNode,
			RdfNode domainNode) throws OData2SparqlException {
		RdfProperty datatypeProperty = null;

		datatypeProperty = model.getOrCreateProperty(propertyNode, propertyLabelNode, domainNode);

		RdfNode superPropertyNode = soln.getRdfNode("superProperty");
		if (superPropertyNode != null) {
			RdfNode superdomainNode = soln.getRdfNode("superDomain");
			if (superdomainNode != null) {
				RdfProperty superProperty = model.getOrCreateProperty(soln.getRdfNode("superProperty"), null,
						superdomainNode);
				if (!domainNode.getIRI().toString().equals(superdomainNode.getIRI().toString())) {
					datatypeProperty = model.getOrCreateProperty(propertyNode, propertyLabelNode, domainNode);
					datatypeProperty.setSuperProperty(superProperty);
					RdfComplexType complexType = model.getOrCreateComplexType(soln.getRdfNode("superProperty"), null,
							superdomainNode);
					complexType.addProperty(datatypeProperty);
					complexTypes.add(complexType);
					superProperty.setIsComplex(true);
					superProperty.setComplexType(complexType);
				}
			} else {
				log.warn(
						"Superproperty:" + superPropertyNode.getIRI().toString() + " declared without specific domain");
			}
		} else {
			datatypeProperty = model.getOrCreateProperty(propertyNode, propertyLabelNode, domainNode);
		}
		return datatypeProperty;
	}

	private void getDataTypeProperties_Ranges(TreeMap<String, TreeSet<RdfEntityType>> propertyClasses)
			throws OData2SparqlException {
		// DataType Properties
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet properties = rdfMetamodelProvider.getProperty_Ranges();
			try {
				while (properties.hasNext()) {
					RdfNode propertyNode = null;
					RdfQuerySolution soln = null;
					RdfNode rangeNode = null;
					try {
						soln = properties.nextSolution();
						propertyNode = soln.getRdfNode("property");
						rangeNode = soln.getRdfNode("range");
						TreeSet<RdfEntityType> classes = propertyClasses.get(propertyNode.getIRI().toString());
						if (classes != null) {
							model.setPropertyRange(propertyNode, classes, rangeNode);
							count++;
							debug.append(propertyNode.getIRI().toString()).append(";");
						} else {
							log.info("Failed to create property ranges:" + propertyNode.getIRI().toString()
									+ " since no corresponding domain classes");
						}
					} catch (Exception e) {
						log.info("Failed to create property ranges:" + propertyNode.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(count + " DataTypeProperties_Ranges found");
				properties.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Datatype property ranges query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("DataTypeProperties_Ranges query exception ", e);
		}
	}

	private void getDataTypeProperties_Cardinality(TreeMap<String, TreeSet<RdfEntityType>> propertyClasses)
			throws OData2SparqlException {
		// DataType Properties
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet properties = rdfMetamodelProvider.getProperty_Cardinality();
			try {
				while (properties.hasNext()) {
					RdfNode propertyNode = null;
					try {
						RdfQuerySolution soln = properties.nextSolution();
						propertyNode = soln.getRdfNode("property");

						RdfNode maxCardinalityNode = null;
						RdfNode minCardinalityNode = null;
						RdfNode cardinalityNode = null;
						if (soln.getRdfNode("maxCardinality") != null)
							maxCardinalityNode = soln.getRdfNode("maxCardinality");
						if (soln.getRdfNode("minCardinality") != null)
							minCardinalityNode = soln.getRdfNode("minCardinality");
						if (soln.getRdfNode("cardinality") != null)
							cardinalityNode = soln.getRdfNode("cardinality");
						if ((soln.getRdfNode("maxCardinality") != null) || (soln.getRdfNode("minCardinality") != null)
								|| (soln.getRdfNode("cardinality") != null)) {
							Cardinality cardinality = interpretCardinality(maxCardinalityNode, minCardinalityNode,
									cardinalityNode, RdfConstants.Cardinality.ZERO_TO_ONE);
							TreeSet<RdfEntityType> classes = propertyClasses.get(propertyNode.getIRI().toString());
							model.setPropertyCardinality(propertyNode, classes, cardinality);
						}
						count++;
						debug.append(propertyNode.getIRI().toString()).append(";");
					} catch (Exception e) {
						log.info("Failed to create property cardinality:" + propertyNode.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(count + " DataTypeProperties_Cardinality found");
				properties.close();
			}
		} catch (OData2SparqlException e) {
			log.error(
					"Failed to execute datatype property cardinality query. Check availability of triple store. Exception "
							+ e.getMessage());
			throw new OData2SparqlException("DataTypeProperties_Cardinality query exception ", e);
		}
	}

	private void removeIncompleteProperties(TreeMap<String, TreeSet<RdfEntityType>> propertyClasses) {
		for (TreeSet<RdfEntityType> classes : propertyClasses.values()) {
			if (!classes.isEmpty()) {
				for (RdfEntityType clazz : classes) {
					if (clazz != null) {
						Collection<RdfProperty> properties = clazz.getProperties();
						Iterator<RdfProperty> propertiesIterator = properties.iterator();
						while (propertiesIterator.hasNext()) {
							RdfProperty property = propertiesIterator.next();
							if (property.propertyTypeName == null) {
								log.error("Removing incomplete datatype property declaration for class.property: "
										+ clazz.getEntityTypeNode().getIRI().toString() + "."
										+ property.getPropertyURI().toString());
								propertiesIterator.remove();

							}
						}
					}
				}
			}
		}
	}

	private void getObjectProperties() throws OData2SparqlException {
		// Object Properties
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet associations = rdfMetamodelProvider.getAssociations();
			try {
				while (associations.hasNext()) {
					RdfNode propertyNode = null;
					try {
						RdfQuerySolution soln = associations.nextSolution();

						propertyNode = soln.getRdfNode("property");
						RdfNode propertyLabelNode = null;
						if (soln.getRdfNode("propertyLabel") != null) {
							propertyLabelNode = soln.getRdfNode("propertyLabel");
						}
						RdfNode domainNode = soln.getRdfNode("domain");
						RdfNode multipleDomainNode = soln.getRdfNode("multipleDomain");
						RdfNode rangeNode = soln.getRdfNode("range");
						RdfNode multipleRangeNode = soln.getRdfNode("multipleRange");

						RdfNode maxRangeCardinalityNode = null;
						RdfNode minRangeCardinalityNode = null;
						RdfNode rangeCardinalityNode = null;
						if (soln.getRdfNode("maxRangeCardinality") != null)
							maxRangeCardinalityNode = soln.getRdfNode("maxRangeCardinality");
						if (soln.getRdfNode("minRangeCardinality") != null)
							minRangeCardinalityNode = soln.getRdfNode("minRangeCardinality");
						if (soln.getRdfNode("rangeCardinality") != null)
							rangeCardinalityNode = soln.getRdfNode("rangeCardinality");

						RdfNode maxDomainCardinalityNode = null;
						RdfNode minDomainCardinalityNode = null;
						RdfNode domainCardinalityNode = null;
						if (soln.getRdfNode("maxDomainCardinality") != null)
							maxDomainCardinalityNode = soln.getRdfNode("maxDomainCardinality");
						if (soln.getRdfNode("minDomainCardinality") != null)
							minDomainCardinalityNode = soln.getRdfNode("minDomainCardinality");
						if (soln.getRdfNode("domainCardinality") != null)
							domainCardinalityNode = soln.getRdfNode("domainCardinality");

						createSuperObjectProperty(soln, propertyNode, propertyLabelNode, domainNode, multipleDomainNode,
								rangeNode, multipleRangeNode, maxRangeCardinalityNode, minRangeCardinalityNode,
								rangeCardinalityNode, maxDomainCardinalityNode, minDomainCardinalityNode,
								domainCardinalityNode);
						count++;
						debug.append(propertyNode.getIRI().toString()).append(";");
					} catch (Exception e) {
						log.info("Failed to create objectproperty:" + propertyNode.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(count + " ObjectProperties found [" + debug + "]");
				//log.info(count + " ObjectProperties found");
				associations.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute associations query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("ObjectProperties query exception ", e);
		}
	}
	//	private void getReifiedObjectPredicates() throws OData2SparqlException {
	//		// Reified objectPredicates
	//		try {
	//			int count = 0;
	//			StringBuilder debug = new StringBuilder();
	//			RdfResultSet reifiedObjectPredicates = rdfMetamodelProvider.getReifiedObjectPredicates();
	//			try {
	//				while (reifiedObjectPredicates.hasNext()) {
	//					RdfNode reifiedPredicateNode = null;
	//					RdfQuerySolution soln = null;
	//					try {
	//						soln = reifiedObjectPredicates.nextSolution();
	//						reifiedPredicateNode = soln.getRdfNode("reifiedPredicate");
	//						RdfEntityType rdfEntityType = model.findEntityType(reifiedPredicateNode);
	//
	//						if (rdfEntityType != null) {
	//							RdfNode reifiedObjectPredicateNode = soln.getRdfNode("reifiedObjectPredicate");
	//							RdfNavigationProperty reifiedObjectPredicate = rdfEntityType.findNavigationProperty(reifiedObjectPredicateNode);
	//							if(reifiedObjectPredicate!=null) {
	//									reifiedObjectPredicate.setReifiedObjectPredicate(true);
	//									count++;
	//									debug.append(reifiedObjectPredicateNode.getIRI().toString()).append(";");
	//							}
	//						} else {
	//							log.info("Failed to create ReifiedObjectPredicates:" + reifiedPredicateNode.getIRI().toString()
	//									+ " since no corresponding classes");
	//						}
	//					} catch (Exception e) {
	//						log.info("Failed to find ReifiedObjectPredicates:" + reifiedPredicateNode.getIRI().toString()
	//								+ " with exception " + e.getMessage());
	//					}
	//				}
	//			} finally {
	//				log.info(count + " ReifiedfObjectPredicate properties found [" + debug + "]");
	//				reifiedObjectPredicates.close();
	//			}
	//		} catch (OData2SparqlException e) {
	//			log.error("Failed to execute reified objectpredicates. Check availability of triple store. Exception "
	//					+ e.getMessage());
	//			throw new OData2SparqlException("ReifiedObjectfPredicatesquery exception ", e);
	//		}
	//		
	//	}
	//
	//	private void getReifiedSubjectPredicates() throws OData2SparqlException {
	//		// Reified subjectPredicates
	//		try {
	//			int count = 0;
	//			StringBuilder debug = new StringBuilder();
	//			RdfResultSet reifiedSubjectPredicates = rdfMetamodelProvider.getReifiedSubjectPredicates();
	//			try {
	//				while (reifiedSubjectPredicates.hasNext()) {
	//					RdfNode reifiedPredicateNode = null;
	//					RdfQuerySolution soln = null;
	//					try {
	//						soln = reifiedSubjectPredicates.nextSolution();
	//						reifiedPredicateNode = soln.getRdfNode("reifiedPredicate");
	//						RdfEntityType rdfEntityType = model.findEntityType(reifiedPredicateNode);
	//
	//						if (rdfEntityType != null) {
	//							RdfNode reifiedSubjectPredicateNode = soln.getRdfNode("reifiedSubjectPredicate");
	//							RdfNavigationProperty reifiedSubjectPredicate = rdfEntityType.findNavigationProperty(reifiedSubjectPredicateNode);
	//							if(reifiedSubjectPredicate!=null) {
	//								reifiedSubjectPredicate.setReifiedSubjectPredicate(true);
	//								count++;
	//								debug.append(reifiedSubjectPredicateNode.getIRI().toString()).append(";");
	//							}
	//							
	//						} else {
	//							log.info("Failed to create ReifiedSubjectPredicates:" + reifiedPredicateNode.getIRI().toString()
	//									+ " since no corresponding classes");
	//						}
	//					} catch (Exception e) {
	//						log.info("Failed to find ReifiedPredicate class:" + reifiedPredicateNode.getIRI().toString()
	//								+ " with exception " + e.getMessage());
	//					}
	//				}
	//			} finally {
	//				log.info(count + " ReifiedfSubjectPredicate properties found [" + debug + "]");
	//				reifiedSubjectPredicates.close();
	//			}
	//		} catch (OData2SparqlException e) {
	//			log.error("Failed to execute reified subjectpredicates. Check availability of triple store. Exception "
	//					+ e.getMessage());
	//			throw new OData2SparqlException("ReifiedSubjectfPredicatesquery exception ", e);
	//		}
	//	}

	private void getReifiedStatements() throws OData2SparqlException {
		// Reified Classes
		try {
			int statementCount = 0;
			int subjectCount = 0;
			int objectCount = 0;
			int predicateCount = 0;
			StringBuilder statementDebug = new StringBuilder();
			StringBuilder subjectDebug = new StringBuilder();
			StringBuilder objectDebug = new StringBuilder();
			StringBuilder predicateDebug = new StringBuilder();
			RdfResultSet reifiedStatements = rdfMetamodelProvider.getReifiedStatements();
			try {
				while (reifiedStatements.hasNext()) {
					RdfNode reifiedStatementNode = null;
					RdfQuerySolution soln = null;
					try {
						soln = reifiedStatements.nextSolution();
						reifiedStatementNode = soln.getRdfNode("reifiedStatement");
						RdfEntityType rdfEntityType = model.findEntityType(reifiedStatementNode);

						if (rdfEntityType != null) {
							rdfEntityType.setReified(true);
							statementCount++;
							statementDebug.append(reifiedStatementNode.getIRI().toString()).append(";");
							RdfNode reifiedObjectPredicateNode = soln.getRdfNode("reifiedObjectPredicate");
							RdfNavigationProperty reifiedObjectPredicate = rdfEntityType
									.findNavigationProperty(reifiedObjectPredicateNode);
							if (reifiedObjectPredicate != null) {
								reifiedObjectPredicate.setReifiedObjectPredicate(true);
								//reifiedObjectPredicate.setDomainCardinality(Cardinality.ONE);
								//if(reifiedObjectPredicate.getInverseNavigationProperty()!=null)reifiedObjectPredicate.getInverseNavigationProperty().setRangeCardinality(Cardinality.ONE);
								objectCount++;
								objectDebug.append(reifiedObjectPredicateNode.getIRI().toString()).append(";");
							}
							RdfNode reifiedSubjectPredicateNode = soln.getRdfNode("reifiedSubjectPredicate");
							RdfNavigationProperty reifiedSubjectPredicate = rdfEntityType
									.findNavigationProperty(reifiedSubjectPredicateNode);
							if (reifiedSubjectPredicate != null) {
								reifiedSubjectPredicate.setReifiedSubjectPredicate(true);
								//reifiedSubjectPredicate.setDomainCardinality(Cardinality.ONE);
								//if(reifiedSubjectPredicate.getInverseNavigationProperty()!=null)reifiedSubjectPredicate.getInverseNavigationProperty().setRangeCardinality(Cardinality.ONE);
								subjectCount++;
								subjectDebug.append(reifiedSubjectPredicateNode.getIRI().toString()).append(";");
							}
							RdfNode reifiedPredicateNode = soln.getRdfNode("reifiedPredicate");
							RdfNavigationProperty reifiedPredicate = rdfEntityType
									.findNavigationProperty(reifiedPredicateNode);
							if (reifiedPredicate != null) {
								reifiedPredicate.setReifiedPredicate(true);
								//reifiedPredicate.setDomainCardinality(Cardinality.ONE);
								//if(reifiedPredicate.getInverseNavigationProperty()!=null)reifiedPredicate.getInverseNavigationProperty().setRangeCardinality(Cardinality.ONE);
								predicateCount++;
								predicateDebug.append(reifiedPredicateNode.getIRI().toString()).append(";");

							}
						} else {
							log.info("Failed to create reifiedStatements:" + reifiedStatementNode.getIRI().toString()
									+ " since no corresponding classes");
						}
					} catch (Exception e) {
						log.info("Failed to find ReifiedStatement class:" + reifiedStatementNode.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(statementCount + " ReifiedStatement classes found [" + statementDebug + "]");
				log.info(subjectCount + " ReifiedSubjectPredicate properties found [" + subjectDebug + "]");
				log.info(objectCount + " ReifiedfObjectPredicate properties found [" + objectDebug + "]");
				log.info(predicateCount + " ReifiedPredicate properties found [" + predicateDebug + "]");
				reifiedStatements.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute reified statements. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("ReifiedfPredicatesquery exception ", e);
		}

	}

	private void createSuperObjectProperty(RdfQuerySolution soln, RdfNode propertyNode, RdfNode propertyLabelNode,
			RdfNode domainNode, RdfNode multipleDomainNode, RdfNode rangeNode, RdfNode multipleRangeNode,
			RdfNode maxRangeCardinalityNode, RdfNode minRangeCardinalityNode, RdfNode rangeCardinalityNode,
			RdfNode maxDomainCardinalityNode, RdfNode minDomainCardinalityNode, RdfNode domainCardinalityNode)
			throws OData2SparqlException {
		Cardinality rangeCardinality = interpretCardinality(maxRangeCardinalityNode, minRangeCardinalityNode,
				rangeCardinalityNode, RdfConstants.Cardinality.MANY);
		Cardinality domainCardinality = interpretCardinality(maxDomainCardinalityNode, minDomainCardinalityNode,
				domainCardinalityNode, RdfConstants.Cardinality.MANY);
		RdfNavigationProperty rdfNavigationProperty = null;
		if (soln.getRdfNode("superProperty") != null) {
			RdfNode superdomainNode = soln.getRdfNode("superDomain");
			if (superdomainNode != null) {
				RdfProperty superProperty = model.getOrCreateProperty(soln.getRdfNode("superProperty"), null,
						superdomainNode);
				if (!domainNode.getIRI().toString().equals(superdomainNode.getIRI().toString())) {
					RdfComplexType complexType = model.getOrCreateComplexType(soln.getRdfNode("superProperty"), null,
							superdomainNode);
					rdfNavigationProperty = model.getOrCreateNavigationProperty(propertyNode, propertyLabelNode,
							domainNode, rangeNode, multipleDomainNode, multipleRangeNode, domainCardinality,
							rangeCardinality);
					if (soln.getRdfNode("description") != null) {
						rdfNavigationProperty
								.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
					}
					rdfNavigationProperty.setSuperProperty(superProperty);
					complexType.addNavigationProperty(rdfNavigationProperty);
					complexTypes.add(complexType);
					superProperty.setIsComplex(true);
					superProperty.setComplexType(complexType);
				}
			}
		} else {
			rdfNavigationProperty = model.getOrCreateNavigationProperty(propertyNode, propertyLabelNode, domainNode,
					rangeNode, multipleDomainNode, multipleRangeNode, domainCardinality, rangeCardinality);
			if (soln.getRdfNode("description") != null) {
				rdfNavigationProperty.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
			}
		}

	}

	private void cleanupReifiedPredicates() {

		for (RdfSchema graph : model.graphs) {
			Iterator<RdfEntityType> entityTypeIterator = graph.getClasses().iterator();
			while (entityTypeIterator.hasNext()) {
				RdfEntityType entityType = entityTypeIterator.next();
				if (entityType.isReified()) {

					RdfNavigationProperty reifiedObjectPredicate = null;
					RdfNavigationProperty reifiedSubjectPredicate = null;
					RdfNavigationProperty reifiedPredicate = null;
					HashSet<RdfNavigationProperty> navigationProperties = entityType.getNavigationProperties();//.getInheritedNavigationProperties();
					for (RdfNavigationProperty navigationProperty : navigationProperties) {
						if (navigationProperty.isReifiedObjectPredicate()) {
							reifiedObjectPredicate = navigationProperty;
						}
						if (navigationProperty.isReifiedSubjectPredicate()) {
							reifiedSubjectPredicate = navigationProperty;
						}
						if (navigationProperty.isReifiedPredicate()) {
							reifiedPredicate = navigationProperty;
						}
						if (reifiedObjectPredicate != null && reifiedSubjectPredicate != null
								&& reifiedPredicate != null)
							break;
					}
					if (reifiedObjectPredicate != null && reifiedSubjectPredicate != null) {
						if (reifiedObjectPredicate.getDomainCardinality().equals(Cardinality.MANY)
								|| reifiedObjectPredicate.getDomainCardinality().equals(Cardinality.MULTIPLE)) {
							reifiedObjectPredicate.setDomainCardinality(Cardinality.ONE);
							if (reifiedObjectPredicate.getInverseNavigationProperty() != null)
								reifiedObjectPredicate.getInverseNavigationProperty()
										.setRangeCardinality(Cardinality.ONE);
							log.warn(reifiedObjectPredicate.getNavigationPropertyIRI().toString()
									+ " declared reifiedSubject but cardinality MANY, set to ONE");
						}
						if (reifiedSubjectPredicate.getDomainCardinality().equals(Cardinality.MANY)
								|| reifiedSubjectPredicate.getDomainCardinality().equals(Cardinality.MULTIPLE)) {
							reifiedSubjectPredicate.setDomainCardinality(Cardinality.ONE);
							if (reifiedSubjectPredicate.getInverseNavigationProperty() != null)
								reifiedSubjectPredicate.getInverseNavigationProperty()
										.setRangeCardinality(Cardinality.ONE);
							log.warn(reifiedSubjectPredicate.getNavigationPropertyIRI().toString()
									+ " declared reifiedSubject but cardinality MANY, set to ONE");
						}
						if (reifiedPredicate != null) {
							if (reifiedPredicate.getDomainCardinality().equals(Cardinality.MANY)
									|| reifiedPredicate.getDomainCardinality().equals(Cardinality.MULTIPLE)) {
								reifiedPredicate.setDomainCardinality(Cardinality.ONE);
								if (reifiedPredicate.getInverseNavigationProperty() != null)
									reifiedPredicate.getInverseNavigationProperty()
											.setRangeCardinality(Cardinality.ONE);
								log.warn(reifiedPredicate.getNavigationPropertyIRI().toString()
										+ " declared reifiedPredicate but cardinality MANY, set to ONE");
							}
						}
					} else {
						log.warn(entityType.getIRI().toString()
								+ " declared reified but no subject and object predicates");
						entityType.setReified(false);
					}
				}
			}
		}
	}

	private void getInverseProperties() throws OData2SparqlException {
		// Inverse Properties
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet inverseAssociations = rdfMetamodelProvider.getInverseAssociations();
			try {
				while (inverseAssociations.hasNext()) {
					RdfNode inversePropertyNode = null;
					try {
						RdfQuerySolution soln = inverseAssociations.nextSolution();

						inversePropertyNode = soln.getRdfNode("inverseProperty");
						RdfNode inversePropertyLabelNode = null;
						if (soln.getRdfNode("inversePropertyLabel") != null) {
							inversePropertyLabelNode = soln.getRdfNode("inversePropertyLabel");
						}
						RdfNode propertyNode = soln.getRdfNode("property");

						RdfNode domainNode = soln.getRdfNode("domain");
						RdfNode multipleDomainNode = soln.getRdfNode("multipleDomain");
						RdfNode rangeNode = soln.getRdfNode("range");
						RdfNode multipleRangeNode = soln.getRdfNode("multipleRange");

						RdfNode maxRangeCardinalityNode = null;
						RdfNode minRangeCardinalityNode = null;
						RdfNode rangeCardinalityNode = null;
						if (soln.getRdfNode("maxRangeCardinality") != null)
							maxRangeCardinalityNode = soln.getRdfNode("maxRangeCardinality");
						if (soln.getRdfNode("minRangeCardinality") != null)
							minRangeCardinalityNode = soln.getRdfNode("minRangeCardinality");
						if (soln.getRdfNode("rangeCardinality") != null)
							rangeCardinalityNode = soln.getRdfNode("rangeCardinality");

						RdfNode maxDomainCardinalityNode = null;
						RdfNode minDomainCardinalityNode = null;
						RdfNode domainCardinalityNode = null;
						if (soln.getRdfNode("maxDomainCardinality") != null)
							maxDomainCardinalityNode = soln.getRdfNode("maxDomainCardinality");
						if (soln.getRdfNode("minDomainCardinality") != null)
							minDomainCardinalityNode = soln.getRdfNode("minDomainCardinality");
						if (soln.getRdfNode("domainCardinality") != null)
							domainCardinalityNode = soln.getRdfNode("domainCardinality");
						createInverseSuperProperty(soln, inversePropertyNode, inversePropertyLabelNode, propertyNode,
								domainNode, multipleDomainNode, rangeNode, multipleRangeNode, maxRangeCardinalityNode,
								minRangeCardinalityNode, rangeCardinalityNode, maxDomainCardinalityNode,
								minDomainCardinalityNode, domainCardinalityNode);
						count++;
						debug.append(inversePropertyNode.getIRI().toString()).append(";");
					} catch (Exception e) {
						log.info("Failed to create inverseproperty:" + inversePropertyNode.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(count + " InverseProperties found [" + debug + "]");
				inverseAssociations.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Inverse Associations query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("InverseProperties query exception ", e);
		}
	}

	private void createInverseSuperProperty(RdfQuerySolution soln, RdfNode inversePropertyNode,
			RdfNode inversePropertyLabelNode, RdfNode propertyNode, RdfNode domainNode, RdfNode multipleDomainNode,
			RdfNode rangeNode, RdfNode multipleRangeNode, RdfNode maxRangeCardinalityNode,
			RdfNode minRangeCardinalityNode, RdfNode rangeCardinalityNode, RdfNode maxDomainCardinalityNode,
			RdfNode minDomainCardinalityNode, RdfNode domainCardinalityNode) throws OData2SparqlException {
		Cardinality rangeCardinality = interpretCardinality(maxRangeCardinalityNode, minRangeCardinalityNode,
				rangeCardinalityNode, RdfConstants.Cardinality.MANY);

		Cardinality domainCardinality = interpretCardinality(maxDomainCardinalityNode, minDomainCardinalityNode,
				domainCardinalityNode, RdfConstants.Cardinality.MANY);
		RdfNavigationProperty inverseRdfNavigationProperty = null;
		RdfNode superPropertyNode = soln.getRdfNode("superProperty");
		if (superPropertyNode != null) {
			RdfNode superdomainNode = soln.getRdfNode("superDomain");
			if (superdomainNode != null) {
				RdfProperty superProperty = model.getOrCreateProperty(superPropertyNode, null, superdomainNode);
				if (!domainNode.getIRI().toString().equals(superdomainNode.getIRI().toString())) {
					inverseRdfNavigationProperty = model.getOrCreateInverseNavigationProperty(inversePropertyNode,
							inversePropertyLabelNode, propertyNode, domainNode, rangeNode, multipleDomainNode,
							multipleRangeNode, domainCardinality, rangeCardinality);
					inverseRdfNavigationProperty.setSuperProperty(superProperty);
					RdfComplexType complexType = model.getOrCreateComplexType(soln.getRdfNode("superProperty"), null,
							superdomainNode);
					complexType.addNavigationProperty(inverseRdfNavigationProperty);
					complexTypes.add(complexType);
					superProperty.setIsComplex(true);
					superProperty.setComplexType(complexType);
				}
			} else {
				log.warn(
						"Superproperty:" + superPropertyNode.getIRI().toString() + " declared without specific domain");
			}
		} else {
			inverseRdfNavigationProperty = model.getOrCreateInverseNavigationProperty(inversePropertyNode,
					inversePropertyLabelNode, propertyNode, domainNode, rangeNode, multipleDomainNode,
					multipleRangeNode, domainCardinality, rangeCardinality);
		}
		if (inverseRdfNavigationProperty != null && soln.getRdfNode("description") != null) {
			inverseRdfNavigationProperty.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
		}
	}

	private void getOperations() throws OData2SparqlException {
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet operations = rdfMetamodelProvider.getOperations();
			try {
				while (operations.hasNext()) {
					RdfNode queryNode = null;
					try {
						RdfQuerySolution soln = operations.nextSolution();
						queryNode = soln.getRdfNode("query");
						RdfNode queryText = soln.getRdfNode("queryText");
						RdfNode queryLabel = soln.getRdfNode("queryLabel");
						RdfNode deleteText = soln.getRdfNode("deleteText");
						RdfNode insertText = soln.getRdfNode("insertText");
						RdfNode updateText = soln.getRdfNode("updateText");
						RdfNode updatePropertyText = soln.getRdfNode("updatePropertyText");
						RdfNode isProxy = soln.getRdfNode("isProxy");
						RdfEntityType operationEntityType = model.getOrCreateOperationEntityType(queryNode, queryLabel,
								queryText, deleteText, insertText, updateText, updatePropertyText, isProxy);
						if (soln.getRdfNode("description") != null) {
							operationEntityType
									.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
						}
					} catch (Exception e) {
						log.info("Failed to create operation:" + queryNode.getIRI().toString() + " with exception "
								+ e.getMessage());
					}
					count++;
					debug.append(queryNode.getIRI().toString()).append(";");
				}
			} finally {
				log.info(count + " Operations found [" + debug + "]");
				operations.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Operations query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("Operations query exception ", e);
		}
	}

	private void getOperationAssociationResults() throws OData2SparqlException {
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet operationAssociationResults = rdfMetamodelProvider.getOperationAssociationResults();
			//?query ?varName ?property ?propertyLabel ?range
			try {
				while (operationAssociationResults.hasNext()) {
					RdfNode query = null;
					try {
						RdfQuerySolution soln = operationAssociationResults.nextSolution();
						query = soln.getRdfNode("query");
						RdfNode varName = soln.getRdfNode("varName");
						RdfNode queryProperty = soln.getRdfNode("property");
						RdfNode queryPropertyLabel = null;
						if (soln.getRdfNode("propertyLabel") != null) {
							queryPropertyLabel = soln.getRdfNode("propertyLabel");
						}
						RdfNode queryPropertyRange = soln.getRdfNode("range");
						RdfNode queryPropertyOptional = soln.getRdfNode("optional");
						RdfNavigationProperty operationAssociation = model.getOrCreateOperationNavigationProperty(query,
								queryProperty, queryPropertyLabel, queryPropertyRange, varName, queryPropertyOptional);
						if (soln.getRdfNode("description") != null) {
							operationAssociation
									.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
						}
						count++;
						debug.append(query.getIRI().toString()).append("\\").append(queryProperty.getIRI().toString())
								.append(";");
					} catch (Exception e) {
						log.info("Failed to create operation association results:" + query.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(count + " OperationAssociationResults found [" + debug + "]");
				operationAssociationResults.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Operation Associations query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("OperationsAssociations query exception ", e);
		}
	}

	private void getOperationPropertyResults() throws OData2SparqlException {
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet operationPropertyResults = rdfMetamodelProvider.getOperationPropertyResults();
			//?query ?varName ?property ?propertyLabel ?range
			try {
				while (operationPropertyResults.hasNext()) {
					RdfNode query = null;
					try {
						RdfQuerySolution soln = operationPropertyResults.nextSolution();
						query = soln.getRdfNode("query");
						RdfNode varName = soln.getRdfNode("varName");
						RdfNode queryProperty = soln.getRdfNode("property");
						RdfNode queryPropertyLabel = null;
						if (soln.getRdfNode("propertyLabel") != null) {
							queryPropertyLabel = soln.getRdfNode("propertyLabel");
						}
						RdfNode queryPropertyRange = soln.getRdfNode("range");

						RdfProperty operationProperty = model.getOrCreateOperationProperty(query, queryProperty,
								queryPropertyLabel, queryPropertyRange, varName);
						if (soln.getRdfNode("description") != null) {
							operationProperty
									.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
						}
						count++;
						debug.append(query.getIRI().toString()).append("\\").append(queryProperty.getIRI().toString())
								.append(";");
					} catch (Exception e) {
						log.info("Failed to create operation property results:" + query.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(count + " OperationPropertyResults found [" + debug + "]");
				operationPropertyResults.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Operation results query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("OperationsAssociations query exception ", e);
		}
	}

	private void getOperationArguments() throws OData2SparqlException {
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet operationArguments = rdfMetamodelProvider.getOperationArguments();
			try {
				while (operationArguments.hasNext()) {
					RdfNode query = null;
					try {
						RdfQuerySolution soln = operationArguments.nextSolution();
						query = soln.getRdfNode("query");
						RdfNode varName = soln.getRdfNode("varName");
						RdfNode queryProperty = null;
						if (soln.getRdfNode("property") != null)
							queryProperty = soln.getRdfNode("property");
						RdfNode range = null;
						if (soln.getRdfNode("range") != null)
							range = soln.getRdfNode("range");
						RdfNode isDataset = null;
						if (soln.getRdfNode("isDataset") != null)
							isDataset = soln.getRdfNode("isDataset");
						RdfNode isPropertyPath = null;
						if (soln.getRdfNode("isPropertyPath") != null)
							isPropertyPath = soln.getRdfNode("isPropertyPath");
						model.getOrCreateOperationArguments(query, queryProperty, varName, range, isDataset,
								isPropertyPath);
						count++;
						debug.append(query.getIRI().toString()).append("\\")
								.append(varName.getLiteralValue().stringValue()).append(";");
					} catch (Exception e) {
						log.info("Failed to create operation arguments:" + query.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(count + " OperationArguments found [" + debug + "]");
				operationArguments.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Operation arguments query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("OperationsAssociations query exception ", e);
		}
	}

	private void getNodeShapes() throws OData2SparqlException {
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet NodeShapes = rdfMetamodelProvider.getNodeShapes();
			try {
				while (NodeShapes.hasNext()) {
					RdfNode nodeShape = null;
					RdfNode baseNodeShape = null;
					RdfNode nodeShapeLabel = null;
					RdfNode nodeShapeName = null;
					RdfNode nodeShapeDescription = null;
					RdfNode nodeShapeTargetClass = null;
					RdfNode nodeShapeDeactivated = null;
					try {
						//SELECT ?nodeShape  ?nodeShapeLabel ?nodeShapeName ?nodeShapeDescription  ?nodeShapeTargetClass ?nodeShapeDeactivated
						RdfQuerySolution soln = NodeShapes.nextSolution();
						nodeShape = soln.getRdfNode("nodeShape");
						baseNodeShape = soln.getRdfNode("baseNodeShape");
						nodeShapeLabel = soln.getRdfNode("nodeShapeLabel");
						nodeShapeName = soln.getRdfNode("nodeShapeName");
						nodeShapeDescription = soln.getRdfNode("nodeShapeDescription");
						nodeShapeTargetClass = soln.getRdfNode("nodeShapeTargetClass");
						nodeShapeDeactivated = soln.getRdfNode("nodeShapeDeactivated");
						model.getOrCreateNodeShape(nodeShape, baseNodeShape, nodeShapeLabel, nodeShapeName,
								nodeShapeDescription, nodeShapeTargetClass, nodeShapeDeactivated);
						count++;
						debug.append(nodeShape.getIRI().toString()).append(";");
					} catch (Exception e) {
						log.info("Failed to create nodeShape:" + nodeShape.getIRI().toString() + " with exception "
								+ e.getMessage());
					}
				}
			} finally {
				log.info(count + " NodeShapes found [" + debug + "]");
				NodeShapes.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute NodeShapes query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("NodeShapes query exception ", e);
		}
	}

	private void getPropertyShapes() throws OData2SparqlException {
		try {
			int count = 0;
			StringBuilder debug = new StringBuilder();
			RdfResultSet PropertyShapes = rdfMetamodelProvider.getPropertyShapes();
			try {
				while (PropertyShapes.hasNext()) {
					RdfNode nodeShape = null;
					RdfNode propertyShape = null;
					RdfNode propertyShapeLabel = null;
					RdfNode propertyShapeName = null;
					RdfNode propertyShapeDescription = null;
					RdfNode path = null;
					RdfNode inversePath = null;
					RdfNode propertyNode = null;
					RdfNode minCount = null;
					RdfNode maxCount = null;
					try {
						//SELECT ?nodeShape  ?propertyShape ?propertyShapeLabel ?propertyShapeName ?propertyShapeDescription  ?path ?inversePath  ?propertyNode ?minCount ?maxCount
						RdfQuerySolution soln = PropertyShapes.nextSolution();
						nodeShape = soln.getRdfNode("nodeShape");
						propertyShape = soln.getRdfNode("propertyShape");
						propertyShapeLabel = soln.getRdfNode("propertyShapeLabel");
						propertyShapeName = soln.getRdfNode("propertyShapeName");
						propertyShapeDescription = soln.getRdfNode("propertyShapeDescription");
						path = soln.getRdfNode("path");
						inversePath = soln.getRdfNode("inversePath");
						propertyNode = soln.getRdfNode("propertyNode");
						minCount = soln.getRdfNode("minCount");
						maxCount = soln.getRdfNode("maxCount");

						model.getOrCreatePropertyShape(nodeShape, propertyShape, propertyShapeLabel, propertyShapeName,
								propertyShapeDescription, path, inversePath, propertyNode, minCount, maxCount);
						count++;
						debug.append(nodeShape.getIRI().toString()).append("\\")
								.append(propertyShape.getIRI().toString()).append(";");
					} catch (Exception e) {
						log.info("Failed to create propertyShape:" + propertyShape.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				model.allocateNodeShapesToGraphs();
				log.info(count + " PropertyShapes found [" + debug + "]");
				PropertyShapes.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute NodeShapes query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("PropertyShapes query exception ", e);
		}
	}

	private void cleanupOrphanClasses(RdfEntityType rdfsResource) {

		// Clean up orphaned classes
		for (RdfSchema graph : model.graphs) {
			Iterator<RdfEntityType> clazzIterator = graph.getClasses().iterator();
			while (clazzIterator.hasNext()) {
				RdfEntityType clazz = clazzIterator.next();
				if (clazz.getBaseType() == null && !clazz.isRootClass() && !clazz.isOperation()) {
					clazz.setBaseType(rdfsResource);
				} else {
					//clazz.isOperation = clazz.isOperation();
					//Need to define a primary key for an operation
					if (clazz.primaryKeys.isEmpty() && clazz.isOperation() && clazz.getBaseType() == null) {
						log.warn(
								"Class and related associations removed because incomplete definition, possibly no objectproperty results: "
										+ clazz.getIRI());
						//Remove any association  that uses this class
						for (RdfSchema associationGraph : model.graphs) {
							Iterator<RdfNavigationProperty> associationsIterator = associationGraph
									.getNavigationProperties().iterator();
							while (associationsIterator.hasNext()) {
								RdfNavigationProperty association = associationsIterator.next();
								if ((association.getDomainClass() == clazz) || (association.getRangeClass() == clazz)) {
									associationsIterator.remove();
								}
							}
						}
						//Now remove this class
						clazzIterator.remove();
					}
				}
			}
		}
	}

	private void createFKProperties() throws OData2SparqlException {
		for (RdfSchema rdfGraph : model.graphs) {
			for (RdfEntityType rdfEntityType : rdfGraph.getClasses()) {
				if (!rdfEntityType.isOperation()) {
					for (RdfNavigationProperty rdfNavigationProperty : rdfEntityType.getNavigationProperties()) {
						if (!((rdfNavigationProperty.getDomainCardinality() == Cardinality.MANY)
								|| (rdfNavigationProperty.getDomainCardinality() == Cardinality.MULTIPLE))) {
							model.getOrCreateFKProperty(rdfNavigationProperty);
						}
					}
				}
			}
		}
	}

	private Cardinality interpretCardinality(RdfNode maxCardinalityNode, RdfNode minCardinalityNode,
			RdfNode cardinalityNode, Cardinality defaultCardinality) {
		if (cardinalityNode != null) {
			if (cardinalityNode.getLiteralValue().getLabel().toString().equals("1"))
				return RdfConstants.Cardinality.ONE;
		} else if (minCardinalityNode != null) {
			if (minCardinalityNode.getLiteralValue().getLabel().toString().equals("0")) {
				if (maxCardinalityNode != null) {
					if (maxCardinalityNode.getLiteralValue().getLabel().toString().equals("1")) {
						return RdfConstants.Cardinality.ZERO_TO_ONE;
					} else {
						return RdfConstants.Cardinality.MANY;
					}
				} else {
					return RdfConstants.Cardinality.MANY;
				}
			} else if (minCardinalityNode.getLiteralValue().getLabel().toString().equals("1")) {
				if (maxCardinalityNode != null) {
					if (maxCardinalityNode.getLiteralValue().getLabel().toString().equals("1")) {
						return RdfConstants.Cardinality.ONE;
					} else {
						return RdfConstants.Cardinality.MULTIPLE;
					}
				} else {
					return RdfConstants.Cardinality.MULTIPLE;
				}

			} else {
				return RdfConstants.Cardinality.MANY;
			}
		} else {
			if (maxCardinalityNode != null) {
				if (maxCardinalityNode.getLiteralValue().getLabel().toString().equals("1")) {
					return RdfConstants.Cardinality.ZERO_TO_ONE;
				} else {
					return RdfConstants.Cardinality.MANY;
				}
			} else {
				return defaultCardinality;
			}
		}
		return defaultCardinality;
	}

}