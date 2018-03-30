package com.inova8.odata2sparql.RdfModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

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
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfDatatype;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfSchema;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

public class RdfModelProvider {
	private final Logger log = LoggerFactory.getLogger(RdfModelProvider.class);
	private final RdfModel model;

	private final RdfMetamodelProvider rdfMetamodelProvider;

	public RdfModelProvider(RdfRepository rdfRepository) {
		super();
		this.rdfMetamodelProvider = new RdfMetamodelProvider(rdfRepository);
		model = new RdfModel(rdfRepository);
	}

	public RdfModel getRdfModel() throws Exception {
		log.info("Loading model " + model.getRdfRepository().getModelName() + " from endpoint: " + model.getRdfRepository().getModelRepository().getRepository().toString());
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
		cleanupOrphanClasses(rdfsResource);
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
		@SuppressWarnings("unused")
		RdfNode rdfsTypeNode = RdfNodeFactory.createURI(RdfConstants.RDF_TYPE);
		@SuppressWarnings("unused")
		RdfNode rdfsTypeLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_TYPE_LABEL);
		@SuppressWarnings("unused")
		RdfNode rdfsInverseTypeNode = RdfNodeFactory.createURI(RdfConstants.RDF_INVERSE_TYPE);
		@SuppressWarnings("unused")
		RdfNode rdfsInverseTypeLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDF_INVERSE_TYPE_LABEL);
		RdfNode rdfsSubClassOfNode = RdfNodeFactory.createURI(RdfConstants.RDFS_SUBCLASSOF);
		RdfNode rdfsSubClassOfLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_SUBCLASSOF_LABEL);
		@SuppressWarnings("unused")
		RdfNode rdfsDomainNode = RdfNodeFactory.createURI(RdfConstants.RDFS_DOMAIN);
		@SuppressWarnings("unused")
		RdfNode rdfsDomainLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_DOMAIN_LABEL);
		@SuppressWarnings("unused")
		RdfNode rdfsRangeNode = RdfNodeFactory.createURI(RdfConstants.RDFS_RANGE);
		@SuppressWarnings("unused")
		RdfNode rdfsRangeLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_RANGE_LABEL);
		RdfNode owlImportsNode = RdfNodeFactory.createURI(RdfConstants.OWL_IMPORTS);
		RdfNode owlImportsLabelNode = RdfNodeFactory.createLiteral(RdfConstants.OWL_IMPORTS_LABEL);

		RdfNode unityNode = RdfNodeFactory.createLiteral("1");

		RdfEntityType rdfsResource = model.getOrCreateEntityType(rdfsResourceNode, rdfsResourceLabelNode);
		rdfsResource.rootClass = true;
		RdfProperty resourceSubjectProperty = model.getOrCreateProperty(rdfSubjectNode, null, rdfSubjectLabelNode,
				rdfsResourceNode, rdfStringNode, RdfConstants.Cardinality.MANY);
		resourceSubjectProperty.setIsKey(true);
		RdfPrimaryKey resourcePrimaryKey = new RdfPrimaryKey(RdfModel.KEY(rdfsResource), RdfModel.KEY(rdfsResource));
		rdfsResource.primaryKeys.put(RdfModel.KEY(rdfsResource), resourcePrimaryKey);

		RdfNode rdfsLabelNode = RdfNodeFactory.createURI(RdfConstants.RDFS_LABEL);
		RdfNode rdfsLabelLabelNode = RdfNodeFactory.createLiteral(RdfConstants.RDFS_LABEL_LABEL);
		model.getOrCreateProperty(rdfsLabelNode, null, rdfsLabelLabelNode,
				rdfsResourceNode, rdfStringNode, RdfConstants.Cardinality.ZERO_TO_ONE);
		
		model.getOrCreateAssociation(RdfNodeFactory.createURI(RdfConstants.RDF_STATEMENT),
				RdfNodeFactory.createLiteral(RdfConstants.RDF_STATEMENT_LABEL),
				RdfNodeFactory.createURI(RdfConstants.RDFS_RESOURCE),
				RdfNodeFactory.createURI(RdfConstants.RDF_STATEMENT), unityNode, unityNode,
				RdfConstants.Cardinality.MANY, RdfConstants.Cardinality.ONE);

		model.getOrCreateEntityType(owlThingNode, owlThingLabelNode, rdfsResource);
		RdfEntityType rdfsClass = model.getOrCreateEntityType(rdfsClassNode, rdfsClassLabelNode, rdfsResource);
		@SuppressWarnings("unused")
		RdfEntityType owlClass = model.getOrCreateEntityType(owlClassNode, owlClassLabelNode, rdfsClass);
		@SuppressWarnings("unused")
		RdfEntityType owlOntology = model.getOrCreateEntityType(owlOntologyNode, owlOntologyLabelNode, rdfsClass);
		RdfEntityType rdfProperty = model.getOrCreateEntityType(rdfPropertyNode, rdfPropertyLabelNode, rdfsResource);
		@SuppressWarnings("unused")
		RdfEntityType owlObjectProperty = model.getOrCreateEntityType(owlObjectPropertyNode, owlObjectPropertyLabelNode,
				rdfProperty);
		@SuppressWarnings("unused")
		RdfEntityType owlDatatypeProperty = model.getOrCreateEntityType(owlDatatypePropertyNode,
				owlDatatypePropertyLabelNode, rdfProperty);
		model.getOrCreateAssociation(rdfsSubClassOfNode, rdfsSubClassOfLabelNode, rdfsClassNode, rdfsClassNode,
				unityNode, unityNode, RdfConstants.Cardinality.ZERO_TO_ONE, RdfConstants.Cardinality.MANY);
		model.getOrCreateAssociation(owlImportsNode, owlImportsLabelNode, owlOntologyNode, owlOntologyNode, unityNode,
				unityNode, RdfConstants.Cardinality.ZERO_TO_ONE, RdfConstants.Cardinality.MANY);
		return rdfsResource;
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
									if(entityType!=baseType)entityType.setBaseType(baseType);
								}
								entityType.setEntity(true);
								if (soln.getRdfNode("description") != null) {
									entityType.setDescription(
											soln.getRdfNode("description").getLiteralValue().getLabel());
								}
							}
						} else {
							if (!classNode.isBlank())
								model.getOrCreateEntityType(classNode, classLabelNode).setEntity(true);
						}
						count++;
						debug.append(classNode.getIRI().toString()).append(";");
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
			throw new OData2SparqlException("Classes query exception", e);
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
					try {
						RdfQuerySolution soln = datatypes.nextSolution();
						datatypeNode = soln.getRdfNode("datatype");
						@SuppressWarnings("unused")
						RdfDatatype datatype = model.getOrCreateDatatype(datatypeNode);
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
		HashMap<String, HashSet<RdfEntityType>> propertyClasses = new HashMap<>();
		getDataTypeProperties_Domains(propertyClasses);
		getDataTypeProperties_Ranges(propertyClasses);
		getDataTypeProperties_Cardinality(propertyClasses);
		removeIncompleteProperties(propertyClasses);
	}

	private void getDataTypeProperties_Domains(HashMap<String, HashSet<RdfEntityType>> propertyClasses)
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
						RdfProperty datatypeProperty = model.getOrCreateProperty(propertyNode, propertyLabelNode,
								domainNode);
						if (soln.getRdfNode("description") != null) {
							datatypeProperty
									.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
						}
						HashSet<RdfEntityType> classes = propertyClasses.get(propertyNode.getIRI().toString());
						if (classes == null) {
							classes = new HashSet<RdfEntityType>();
							classes.add(datatypeProperty.ofClass);
							propertyClasses.put(propertyNode.getIRI().toString(), classes);
						} else {
							classes.add(datatypeProperty.ofClass);
						}
						count++;
						debug.append(propertyNode.getIRI().toString()).append(";");
					} catch (Exception e) {
						log.info("Failed to create property:" + propertyNode.getIRI().toString() + " with exception "
								+ e.getMessage());
					}
				}
			} finally {
				log.info(count + " DataTypeProperties_Domains found [" + debug + "]");
				properties.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Datatype property query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("DataTypeProperties_Domains query exception ", e);
		}
	}

	private void getDataTypeProperties_Ranges(HashMap<String, HashSet<RdfEntityType>> propertyClasses)
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
						HashSet<RdfEntityType> classes = propertyClasses.get(propertyNode.getIRI().toString());
						if (classes != null) {
							model.setPropertyRange(propertyNode, classes, rangeNode);
							count++;
							debug.append(propertyNode.getIRI().toString()).append(";");
						} else {
							log.info("Failed to create property ranges:" + propertyNode.getIRI().toString()
									+ " since no defined range classes");
						}
					} catch (Exception e) {
						log.info("Failed to create property ranges:" + propertyNode.getIRI().toString()
								+ " with exception " + e.getMessage());
					}
				}
			} finally {
				log.info(count + " DataTypeProperties_Ranges found [" + debug + "]");
				properties.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute Datatype property ranges query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("DataTypeProperties_Ranges query exception ", e);
		}
	}

	private void getDataTypeProperties_Cardinality(HashMap<String, HashSet<RdfEntityType>> propertyClasses)
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
							HashSet<RdfEntityType> classes = propertyClasses.get(propertyNode.getIRI().toString());
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
				log.info(count + " DataTypeProperties_Cardinality found [" + debug + "]");
				properties.close();
			}
		} catch (OData2SparqlException e) {
			log.error(
					"Failed to execute Datatype property cardinality query. Check availability of triple store. Exception "
							+ e.getMessage());
			throw new OData2SparqlException("DataTypeProperties_Cardinality query exception ", e);
		}
	}

	private void removeIncompleteProperties(HashMap<String, HashSet<RdfEntityType>> propertyClasses) {
		for (HashSet<RdfEntityType> classes : propertyClasses.values()) {
			for (RdfEntityType clazz : classes) {
				Collection<RdfProperty> properties = clazz.getProperties();
				Iterator<RdfProperty> propertiesIterator = properties.iterator();
				while (propertiesIterator.hasNext()) {
					RdfProperty property = propertiesIterator.next();
					if (property.propertyTypeName == null) {
						log.error("Removing incomplete property declartion for class.property: " + clazz.entityTypeName + "." + property.propertyName);
						propertiesIterator.remove();
						
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
						Cardinality rangeCardinality = interpretCardinality(maxRangeCardinalityNode,
								minRangeCardinalityNode, rangeCardinalityNode, RdfConstants.Cardinality.MANY);

						RdfNode maxDomainCardinalityNode = null;
						RdfNode minDomainCardinalityNode = null;
						RdfNode domainCardinalityNode = null;
						if (soln.getRdfNode("maxDomainCardinality") != null)
							maxDomainCardinalityNode = soln.getRdfNode("maxDomainCardinality");
						if (soln.getRdfNode("minDomainCardinality") != null)
							minDomainCardinalityNode = soln.getRdfNode("minDomainCardinality");
						if (soln.getRdfNode("domainCardinality") != null)
							domainCardinalityNode = soln.getRdfNode("domainCardinality");
						Cardinality domainCardinality = interpretCardinality(maxDomainCardinalityNode,
								minDomainCardinalityNode, domainCardinalityNode, RdfConstants.Cardinality.MANY);

						RdfAssociation association = model.getOrCreateAssociation(propertyNode, propertyLabelNode,
								domainNode, rangeNode, multipleDomainNode, multipleRangeNode, domainCardinality,
								rangeCardinality);

						if (soln.getRdfNode("description") != null) {
							association.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
						}
						count++;
						debug.append(propertyNode.getIRI().toString()).append(";");
					} catch (Exception e) {
						log.info("Failed to create objectproperty:" + propertyNode.getIRI().toString()
								+ " with exception " + e.getMessage());
					}

				}
			} finally {
				log.info(count + " ObjectProperties found [" + debug + "]");
				associations.close();
			}
		} catch (OData2SparqlException e) {
			log.error("Failed to execute associations query. Check availability of triple store. Exception "
					+ e.getMessage());
			throw new OData2SparqlException("ObjectProperties query exception ", e);
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
						Cardinality rangeCardinality = interpretCardinality(maxRangeCardinalityNode,
								minRangeCardinalityNode, rangeCardinalityNode, RdfConstants.Cardinality.MANY);

						RdfNode maxDomainCardinalityNode = null;
						RdfNode minDomainCardinalityNode = null;
						RdfNode domainCardinalityNode = null;
						if (soln.getRdfNode("maxDomainCardinality") != null)
							maxDomainCardinalityNode = soln.getRdfNode("maxDomainCardinality");
						if (soln.getRdfNode("minDomainCardinality") != null)
							minDomainCardinalityNode = soln.getRdfNode("minDomainCardinality");
						if (soln.getRdfNode("domainCardinality") != null)
							domainCardinalityNode = soln.getRdfNode("domainCardinality");
						Cardinality domainCardinality = interpretCardinality(maxDomainCardinalityNode,
								minDomainCardinalityNode, domainCardinalityNode, RdfConstants.Cardinality.MANY);

						RdfAssociation inverseAssociation = model.getOrCreateInverseAssociation(inversePropertyNode,
								inversePropertyLabelNode, propertyNode, rangeNode, domainNode, multipleDomainNode,
								multipleRangeNode, domainCardinality, rangeCardinality);
						if (soln.getRdfNode("description") != null) {
							inverseAssociation
									.setDescription(soln.getRdfNode("description").getLiteralValue().getLabel());
						}
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
						RdfEntityType operationEntityType = model.getOrCreateOperationEntityType(queryNode, queryLabel,
								queryText,deleteText,insertText,updateText,updatePropertyText );
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

						RdfAssociation operationAssociation = model.getOrCreateOperationAssociation(query,
								queryProperty, queryPropertyLabel, queryPropertyRange, varName);
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
						model.getOrCreateOperationArguments(query, queryProperty, varName, range);
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

	private void cleanupOrphanClasses(RdfEntityType rdfsResource) {

		// Clean up orphaned classes
		for (RdfSchema graph : model.graphs) {
			Iterator<RdfEntityType> clazzIterator = graph.classes.iterator();
			while (clazzIterator.hasNext()) {
				RdfEntityType clazz = clazzIterator.next();
				if (clazz.getBaseType() == null && !clazz.rootClass && !clazz.isOperation) {
					clazz.setBaseType(rdfsResource);
				} else {
					clazz.isOperation = clazz.isOperation;
					//Need to define a primary key for an operation
					if (clazz.primaryKeys.isEmpty() && clazz.isOperation && clazz.getBaseType() == null) {
						log.warn("Class and related associations removed because incomplete definition, possibly no objectproperty results: " + clazz.getIRI());
						//Remove any association  that uses this class
						for (RdfSchema associationGraph : model.graphs) {
							 Iterator<RdfAssociation> associationsIterator = associationGraph.associations.iterator();
							 while (associationsIterator.hasNext()) {
								  RdfAssociation association = associationsIterator.next();
								  if((association.getDomainClass()==clazz)||(association.getRangeClass()==clazz)){
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