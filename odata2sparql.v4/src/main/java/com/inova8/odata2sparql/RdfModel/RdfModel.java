package com.inova8.odata2sparql.RdfModel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.core4j.Enumerable;
import org.core4j.Predicate1;
import org.eclipse.rdf4j.model.BNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.xerces.util.XMLChar;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;

public class RdfModel {
	private final Logger log = LoggerFactory.getLogger(RdfModel.class);

	static String KEY(RdfEntityType clazz) {
		return RdfConstants.SUBJECT;
	};

	public final List<RdfModel.RdfSchema> graphs = new ArrayList<RdfModel.RdfSchema>();
	private TreeMap<String, RdfModel.RdfNodeShape> pendingNodeShapes = new TreeMap<String, RdfModel.RdfNodeShape>();

	private final RdfPrefixes rdfPrefixes = new RdfPrefixes();
	private final RdfRepository rdfRepository;

	public RdfModel(RdfRepository rdfRepository) {

		rdfPrefixes.setStandardNsPrefixes();
		this.rdfRepository = rdfRepository;
	}

	public RdfRepository getRdfRepository() {
		return rdfRepository;
	}

	public RdfPrefixes getRdfPrefixes() {
		return rdfPrefixes;
	}

	public class RdfPrefixes {
		private final Map<String, String> prefixToURI = new TreeMap<String, String>();
		private final Map<String, String> URItoPrefix = new TreeMap<String, String>();

		private void setStandardNsPrefixes() {
			set(RdfConstants.RDF, RdfConstants.RDF_NS);
			set(RdfConstants.RDFS, RdfConstants.RDFS_NS);
			set(RdfConstants.DC, RdfConstants.DC_NS);
			set(RdfConstants.OWL, RdfConstants.OWL_NS);
			set(RdfConstants.XSD, RdfConstants.XSD_NS);
		}

		void log() {
			log.info("Deduced prefixes: " + prefixToURI.toString());
		}

		public StringBuilder sparqlPrefixes() {
			StringBuilder sparqlPrefixes = new StringBuilder();
			for (Map.Entry<String, String> prefixEntry : prefixToURI.entrySet()) {
				String prefix = prefixEntry.getKey();
				String url = prefixEntry.getValue();
				sparqlPrefixes.append("PREFIX ").append(prefix).append(": <").append(url).append(">\n");
			}
			return sparqlPrefixes;
		}

		private void setNsPrefix(String graphPrefix, String graphName) throws OData2SparqlException {

			checkLegal(graphPrefix);
			if (graphName == null)
				throw new NullPointerException("null URIs are prohibited as arguments to setNsPrefix");
			set(graphPrefix, graphName);

		}

		private String getNsURIPrefix(String schemaName) {
			return URItoPrefix.get(schemaName);
		}

		private String getNsPrefixURI(String sprefix) {
			return prefixToURI.get(sprefix);
		}

		public String expandPrefix(String decodedEntityKey) {

			int colon = decodedEntityKey.indexOf(RdfConstants.QNAME_SEPARATOR);
			if (colon < 0)
				return decodedEntityKey;
			else {
				String uri = get(decodedEntityKey.substring(0, colon));
				return uri == null ? decodedEntityKey : uri + decodedEntityKey.substring(colon + 1);
			}
		}

		public String expandPredicateKey(String predicateKey) throws OData2SparqlException {
			UrlValidator urlValidator = new UrlValidator();
			String entityKey = predicateKey.substring(1, predicateKey.length() - 1);
			String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey);
			String expandedEntityKey = expandPrefix(decodedEntityKey);
			if (urlValidator.isValid(expandedEntityKey)) {
				return expandedEntityKey;
			} else {
				throw new OData2SparqlException("Invalid key: " + predicateKey, null);
			}
		}

		private void checkLegal(String prefix) throws OData2SparqlException {
			if (prefix.length() > 0 && !XMLChar.isValidNCName(prefix))
				throw new OData2SparqlException("RdfPrefixes checkLegal failure");
		}

		private String get(String prefix) {
			return prefixToURI.get(prefix);
		}

		private void set(String prefix, String uri) {
			prefixToURI.put(prefix, uri);
			URItoPrefix.put(uri, prefix);
		}

		public String getOrCreatePrefix(String prefix, String uri) throws OData2SparqlException {
			if (prefix == null || prefix.equals("")) {
				if (!uri.substring(uri.length() - 1).equals("#")) {
					String sprefix = this.getNsURIPrefix(uri);
					if (sprefix == null || sprefix.equals("")) {

						//If the URI is of the form:    http://rootpath/xxxx/yyyyyy
						//where http://rootpath/ is already a prefix 'root'
						//then we should create a prefix with name rootprefix_xxxx

						String[] parts = uri.split("/");
						if (parts.length > 3) {
							String rootUri = "";
							for (int i = 0; i < parts.length - 1; i++) {
								rootUri += parts[i] + "/";
							}
							sprefix = this.getNsURIPrefix(rootUri);
							if (sprefix == null || sprefix.equals("")) {
								return generateNextPrefix(uri);
							} else {
								String sNewPrefix = sprefix + "_" + parts[parts.length - 1];
								this.setNsPrefix(sNewPrefix, uri);
								return sNewPrefix;
							}
						} else {
							//Create a new prefix of the form jn
							return generateNextPrefix(uri);
						}
					} else {
						return sprefix;
					}
				} else {
					String sprefix = this.getNsURIPrefix(uri);
					if (sprefix == null || sprefix.equals("")) {
						return generateNextPrefix(uri);
					} else {
						return sprefix;
					}
				}
			} else {
				this.set(prefix, uri);
				return prefix;
			}
		}

		private String generateNextPrefix(String uri) {
			int i = 0;
			String sprefix = "";
			while (true) {
				sprefix = RdfConstants.PREFIX + i;
				if (this.getNsPrefixURI(sprefix) == null) {
					log.info("New prefix added: " + sprefix + "~ for URI " + uri);
					this.set(sprefix, uri);
					return sprefix;
				}
				i++;
			}
		}

		public String toQName(RdfNode node, String qNameSeparator) {
			String qname = null;
			if (node.isBlank()) {
				return ((BNode) node.getNode()).toString();
			} else {
				try {
					qname = rdfPrefixes.getOrCreatePrefix(null, node.getNamespace()) + qNameSeparator
							+ node.getLocalName();
				} catch (OData2SparqlException e) {
					log.error("RdfNode toQName failure. Node:" + node.toString() + " with exception " + e.toString());
				}
			}
			return qname;
		}

		public String toQName(String uri, String qNameSeparator) {
			String qname = null;
			try {
				URI URI = new URI(uri);
				String path = URI.getPath();
				String lname = path.substring(1, path.length());
				//uri.substring(0,uri.length()-lname.length());
				qname = rdfPrefixes.getOrCreatePrefix(null, uri.substring(0, uri.length() - lname.length()))
						+ qNameSeparator + lname;
			} catch (OData2SparqlException | URISyntaxException e) {
				log.error("RdfNode toQName failure. Node:" + uri + " with exception " + e.toString());
			}
			return qname;
		}
	}

	public static class RdfSchema {
		private String schemaName;
		private String schemaPrefix;
		boolean isDefault = false;
		public final List<RdfModel.RdfEntityType> classes = new ArrayList<RdfModel.RdfEntityType>();
		public final List<RdfModel.RdfAssociation> associations = new ArrayList<RdfModel.RdfAssociation>();
		private final List<RdfModel.RdfDatatype> datatypes = new ArrayList<RdfModel.RdfDatatype>();
		private final HashSet<RdfModel.RdfComplexType> complexTypes = new HashSet<RdfModel.RdfComplexType>();
		private final HashSet<RdfModel.RdfNodeShape> nodeShapes = new HashSet<RdfModel.RdfNodeShape>();

		public RdfSchema() {
			super();
			initialiseDatatypes();
		}

		private void initialiseDatatypes() {
			for (String datatype : RdfConstants.RDF_DATATYPES) {
				datatypes.add(new RdfDatatype(datatype));
			}
		}

		public String getSchemaName() {
			return schemaName;
		}

		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}

		public String getSchemaPrefix() {
			return schemaPrefix;
		}

		public void setSchemaPrefix(String schemaPrefix) {
			this.schemaPrefix = schemaPrefix;
		}

		public HashSet<RdfModel.RdfComplexType> getComplexTypes() {
			return complexTypes;
		}

		public HashSet<RdfModel.RdfNodeShape> getNodeShapes() {
			return nodeShapes;
		}
	}

	public static class RdfEntityType {

		public String entityTypeName;
		private String entityTypeLabel;
		private String entitySetLabel;
		private RdfSchema schema;
		private RdfNode entityTypeNode;
		private RdfEntityType baseType;
		private RdfNodeShape nodeShape;
		private boolean rootClass = false;
		private boolean isOperation = false;
		private boolean isEntity = false;
		private boolean functionImport = false;
		private String description;
		private Set<RdfEntityType> subTypes = new HashSet<RdfEntityType>();
		public String queryText;
		private String deleteText;
		private String insertText;
		private String updateText;
		private String updatePropertyText;
		private final TreeMap<String, FunctionImportParameter> functionImportParameters = new TreeMap<String, FunctionImportParameter>();
		private final TreeMap<String, RdfModel.RdfProperty> properties = new TreeMap<String, RdfModel.RdfProperty>();
		private final TreeMap<String, RdfModel.RdfAssociation> navigationProperties = new TreeMap<String, RdfModel.RdfAssociation>();
		private final TreeMap<String, RdfModel.RdfComplexType> complexTypes = new TreeMap<String, RdfModel.RdfComplexType>();
		private final TreeMap<String, RdfModel.RdfAssociation> incomingAssociations = new TreeMap<String, RdfModel.RdfAssociation>();
		final TreeMap<String, RdfModel.RdfPrimaryKey> primaryKeys = new TreeMap<String, RdfModel.RdfPrimaryKey>();

		public String getDeleteText() {
			return deleteText;
		}

		public void setDeleteText(String deleteText) {
			this.deleteText = deleteText;
		}

		public String getInsertText() {
			return insertText;
		}

		public void setInsertText(String insertText) {
			this.insertText = insertText;
		}

		public String getUpdateText() {
			return updateText;
		}

		public void setUpdateText(String updateText) {
			this.updateText = updateText;
		}

		public String getUpdatePropertyText() {
			return updatePropertyText;
		}

		public void setUpdatePropertyText(String updatePropertyText) {
			this.updatePropertyText = updatePropertyText;
		}

		public String getEntityTypeName() {
			return entityTypeName;
		}

		public void setEntityTypeName(String entityTypeName) {
			this.entityTypeName = entityTypeName;
		}

		public RdfNode getEntityTypeNode() {
			return entityTypeNode;
		}

		public void setEntityTypeNode(RdfNode entityTypeNode) {
			this.entityTypeNode = entityTypeNode;
		}

		public RdfEntityType getBaseType() {
			return baseType;
		}

		protected void addSubType(RdfEntityType subType) {
			subTypes.add(subType);
		}

		public Set<RdfEntityType> getSubTypes() {
			return subTypes;
		}

		public RdfNodeShape getNodeShape() {
			return nodeShape;
		}

		public void setNodeShape(RdfNodeShape nodeShape) {
			this.nodeShape = nodeShape;
		}

		public Set<RdfEntityType> getAllSubTypes() {
			Set<RdfEntityType> allSubTypes = new HashSet<RdfEntityType>();
			allSubTypes.addAll(subTypes);
			for (RdfEntityType subType : subTypes) {
				allSubTypes.addAll(subType.getAllSubTypes());
			}
			return allSubTypes;
		}

		public void setBaseType(RdfEntityType baseType) {
			this.baseType = baseType;
			if (baseType != null)
				baseType.addSubType(this);
		}

		public RdfSchema getSchema() {
			return schema;
		}

		public void setSchema(RdfSchema schema) {
			this.schema = schema;
		}

		public String getEntitySetLabel() {
			if (this.entitySetLabel == null) {
				return this.getEntityTypeLabel() + RdfConstants.PLURAL;
			} else {
				return this.entitySetLabel;
			}
		}

		public void setEntitySetLabel(String entitySetLabel) {
			this.entitySetLabel = entitySetLabel.trim();
		}

		public String getEntityTypeLabel() {
			if (entityTypeLabel == null) {
				return this.entityTypeName;
			} else {
				return this.entityTypeLabel;
			}
		}

		public void setEntityTypeLabel(String entityTypeLabel) {
			this.entityTypeLabel = entityTypeLabel.trim();
		}

		public String getDescription() {
			if (this.description == null || this.description.isEmpty()) {
				return entityTypeLabel;
			} else {
				return description;
			}
		}

		public void setDescription(String description) {
			this.description = description.trim();
		}

		public boolean isFunctionImport() {
			return functionImport;
		}

		public TreeMap<String, FunctionImportParameter> getFunctionImportParameters() {
			return functionImportParameters;
		}

		public TreeMap<String, RdfModel.RdfComplexType> getComplexTypes() {
			return complexTypes;
		}

		public boolean isOperation() {
			return isOperation;
		}

		public void setOperation(boolean isOperation) {
			this.isOperation = isOperation;
			this.isEntity = !isOperation;
		}

		public boolean isEntity() {
			return isEntity;
		}

		public void setEntity(boolean isEntity) {
			this.isEntity = isEntity;
			this.isOperation = !isEntity;
		}

		public String getIRI() {
			return entityTypeNode.getIRI().toString();
		}

		public Collection<RdfModel.RdfAssociation> getNavigationProperties() {
			return navigationProperties.values();
		}

		public Collection<RdfModel.RdfAssociation> getInheritedNavigationProperties() {
			Collection<RdfModel.RdfAssociation> inheritedNavigationProperties = new ArrayList<RdfModel.RdfAssociation>();
			inheritedNavigationProperties.addAll(navigationProperties.values());
			if (this.getBaseType() != null) {
				inheritedNavigationProperties.addAll(this.getBaseType().getInheritedNavigationProperties());
			}
			return inheritedNavigationProperties;
		}

		public Collection<RdfModel.RdfProperty> getProperties() {
			return properties.values();
		}

		public Collection<RdfModel.RdfProperty> getInheritedProperties() {
			Collection<RdfModel.RdfProperty> inheritedProperties = new ArrayList<RdfModel.RdfProperty>();
			inheritedProperties.addAll(properties.values());
			if (this.getBaseType() != null) {
				inheritedProperties.addAll(this.getBaseType().getInheritedProperties());
			}
			return inheritedProperties;
		}

		public RdfAssociation findNavigationProperty(String navigationPropertyName) {
			//TODO do we not want to find inherited properties as well?
			return navigationProperties.get(navigationPropertyName);
		}

		public RdfAssociation findInverseNavigationProperty(String navigationPropertyName) {
			//TODO do we not want to find inherited properties as well?
			return incomingAssociations.get(navigationPropertyName);
		}

		public RdfAssociation findNavigationPropertyByEDMAssociationName(String edmAssociationName) {
			//TODO do we not want to find inherited properties as well?
			String navigationPropertyName = null;
			if (this.schema.isDefault) {
				navigationPropertyName = edmAssociationName;
			} else {
				navigationPropertyName = edmAssociationName
						.replace(this.schema.schemaPrefix + RdfConstants.CLASS_SEPARATOR, "");
			}
			return findNavigationProperty(navigationPropertyName);
		}

		public RdfAssociation findNavigationProperty(RdfNode navigationPropertyNode) {
			for (RdfAssociation navigationProperty : this.getInheritedNavigationProperties()) {
				if (navigationProperty.associationNode.getIRI().toString()
						.equals(navigationPropertyNode.getIRI().toString()))
					return navigationProperty;
			}
			return null;
		}

		public RdfProperty findProperty(String propertyName) {
			if (properties.containsKey(propertyName))
				return properties.get(propertyName);
			if (this.getBaseType() != null) {
				return this.getBaseType().findProperty(propertyName);
			} else {
				return null;
			}
		}

		public RdfComplexTypePropertyPair findComplexProperty(RdfNode propertyNode) {
			//Only IRI available not EDM property name so need to search on that
			for (RdfProperty property : properties.values()) {
				if (property.getIsComplex()) {
					for (RdfProperty complexProperty : property.getComplexType().getProperties().values()) {
						if (complexProperty.getPropertyURI().toString().equals(propertyNode.getIRI().toString())) {
							return new RdfComplexTypePropertyPair(property.getComplexType(), complexProperty, null);
						}
					}
					for (RdfAssociation complexNavigationProperty : property.getComplexType().getNavigationProperties()
							.values()) {
						if (complexNavigationProperty.getAssociationIRI().toString()
								.equals(propertyNode.getIRI().toString())) {
							return new RdfComplexTypePropertyPair(property.getComplexType(), null,
									complexNavigationProperty);
						}
					}
				}
			}
			return null;

		}

		public RdfComplexTypePropertyPair findComplexProperty(String propertyName) {
			//EDM property name available so can use TreeMap
			for (RdfProperty property : properties.values()) {
				if (property.getIsComplex()) {
					if (property.getComplexType().getProperties().containsKey(propertyName)) {
						return new RdfComplexTypePropertyPair(property.getComplexType(),
								property.getComplexType().getProperties().get(propertyName), null);
					} else if (property.getComplexType().getNavigationProperties().containsKey(propertyName)) {
						return new RdfComplexTypePropertyPair(property.getComplexType(), null,
								property.getComplexType().getNavigationProperties().get(propertyName));
					}
				}
			}
			return null;
		}

		public String getEDMEntityTypeName() {
			return this.entityTypeName;
		}

		public String getEDMEntitySetName() {
			if (this.schema.isDefault) {
				return this.entityTypeName;
			} else {
				return this.schema.schemaPrefix + RdfConstants.CLASS_SEPARATOR + this.entityTypeName;
			}
		}

		public Collection<RdfPrimaryKey> getPrimaryKeys() {
			return getPrimaryKeys(true);
		}

		public Collection<RdfPrimaryKey> getPrimaryKeys(boolean withBaseType) {
			Collection<RdfPrimaryKey> primaryKeyValues = new ArrayList<RdfPrimaryKey>();
			RdfEntityType currentEntityType = this;
			do {
				primaryKeyValues.addAll(currentEntityType.primaryKeys.values());
				currentEntityType = currentEntityType.getBaseType();
				if (withBaseType)
					currentEntityType = null;
			} while (currentEntityType != null);
			return primaryKeyValues;
		}

		public void setFunctionImport(boolean b) {
			this.functionImport = true;
		}

		public boolean isRootClass() {
			return rootClass;
		}

		public void setRootClass(boolean rootClass) {
			this.rootClass = rootClass;
		}
	}

	public class FunctionImportParameter {
		private String name;
		private String type;
		private boolean nullable;

		private FunctionImportParameter(String name, String type, boolean nullable) {
			super();
			this.name = name;
			this.type = type;
			this.nullable = nullable;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public boolean isNullable() {
			return nullable;
		}

		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}
	}

	public static class RdfPrimaryKey {
		RdfPrimaryKey(String propertyName, String primaryKeyName) {
			super();
			this.propertyName = propertyName;
			this.primaryKeyName = primaryKeyName;
		}

		private final String propertyName;

		private final String primaryKeyName;

		public String getEDMPropertyName() {
			return this.propertyName;
		}

		public String getPrimaryKeyName() {
			return primaryKeyName;
		}
	}

	static class RdfDatatype {
		private RdfDatatype(String datatypeName) {
			super();
			this.datatypeName = datatypeName;
		}

		private final String datatypeName;
	}

	public static class RdfProperty {

		public String propertyName;
		private String propertyLabel;
		public String propertyTypeName;
		private String varName;
		protected RdfNode propertyNode;
		private RdfProperty superProperty;
		private String propertyUri;
		private Boolean isKey = false;
		private Boolean isComplex = false;
		private RdfComplexType complexType;
		private RdfConstants.Cardinality cardinality = RdfConstants.Cardinality.MANY;
		private String equivalentProperty;

		RdfEntityType ofClass;
		private String description;
		private RdfAssociation fkProperty = null;

		public RdfConstants.Cardinality getCardinality() {
			return cardinality;
		}

		public void setCardinality(RdfConstants.Cardinality cardinality) {
			this.cardinality = cardinality;
		}

		public String getPropertyLabel() {
			return propertyLabel;
		}

		public void setPropertyLabel(String propertyLabel) {
			this.propertyLabel = propertyLabel.trim();
		}

		public String getEquivalentProperty() {
			return equivalentProperty;
		}

		public void setEquivalentProperty(String equivalentProperty) {
			this.equivalentProperty = equivalentProperty;
		}

		public String getDescription() {
			if (this.description == null || this.description.isEmpty()) {
				return propertyLabel;
			} else {
				return description;
			}
		}

		public void setDescription(String description) {

			this.description = description.trim();
		}

		public String getEDMPropertyName() {
			return this.propertyName;
		}

		//		public void setPropertyUri(String propertyUri) {
		//			this.propertyUri = propertyUri;
		//		}

		public String getPropertyURI() {
			if (propertyNode != null) {
				return propertyNode.getIRI().toString();
			} else if (propertyUri != null) {
				return propertyUri;
			} else {
				return null;
			}
		}

		public String getPropertyTypeName() {
			return propertyTypeName;
		}

		public RdfProperty getSuperProperty() {
			return superProperty;
		}

		public void setSuperProperty(RdfProperty superProperty) {
			this.superProperty = superProperty;
		}

		public Boolean getIsKey() {
			return isKey;
		}

		public void setIsKey(Boolean isKey) {
			this.isKey = isKey;
		}

		public Boolean getIsComplex() {
			return isComplex;
		}

		public void setIsComplex(Boolean isComplex) {
			this.isComplex = isComplex;
		}

		public String getVarName() {
			return varName;
		}

		public void setVarName(String varName) {
			this.varName = varName;
		}

		public RdfComplexType getComplexType() {
			return this.complexType;
		}

		public void setComplexType(RdfComplexType complexType) {
			this.complexType = complexType;
		}

		public boolean isFK() {
			return !(fkProperty == null);
		}

		public RdfAssociation getFkProperty() {
			return fkProperty;
		}

		public void setFkProperty(RdfAssociation fkProperty) {
			this.fkProperty = fkProperty;
		}

		public RdfEntityType getOfClass() {
			return ofClass;
		}
	}

	public static class RdfComplexTypePropertyPair {
		private RdfComplexType rdfComplexType;
		private RdfProperty rdfProperty;
		private RdfAssociation rdfNavigationProperty;

		public RdfComplexTypePropertyPair(RdfComplexType rdfComplexType, RdfProperty rdfProperty,
				RdfAssociation rdfNavigationProperty) {
			super();
			this.rdfComplexType = rdfComplexType;
			this.rdfProperty = rdfProperty;
			this.setRdfNavigationProperty(rdfNavigationProperty);
		}

		public RdfComplexType getRdfComplexType() {
			return rdfComplexType;
		}

		public RdfProperty getRdfProperty() {
			return rdfProperty;
		}

		public RdfAssociation getRdfNavigationProperty() {
			return rdfNavigationProperty;
		}

		public void setRdfNavigationProperty(RdfAssociation rdfNavigationProperty) {
			this.rdfNavigationProperty = rdfNavigationProperty;
		}
	}

	public static class RdfComplexTypeNavigationPropertyPair {
		private RdfComplexType rdfComplexType;
		private RdfAssociation rdfNavigationProperty;

		public RdfComplexTypeNavigationPropertyPair(RdfComplexType rdfComplexType,
				RdfAssociation rdfNavigationProperty) {
			super();
			this.rdfComplexType = rdfComplexType;
			this.rdfNavigationProperty = rdfNavigationProperty;
		}

		public RdfComplexType getRdfComplexType() {
			return rdfComplexType;
		}

		public RdfAssociation getRdfNavigationProperty() {
			return rdfNavigationProperty;
		}
	}

	public static class RdfComplexType {
		private RdfNode complexTypeNode;
		private String complexTypeName;
		private String complexTypeLabel;
		private RdfNode domainNode;
		private String domainName;
		private TreeMap<String, RdfProperty> properties = new TreeMap<String, RdfProperty>();
		private TreeMap<String, RdfAssociation> navigationProperties = new TreeMap<String, RdfAssociation>();
		public RdfEntityType domainClass;

		public void addProperty(RdfProperty rdfProperty) {
			properties.put(RdfModel.rdfToOdata(rdfProperty.propertyName), rdfProperty);
		}

		public void addNavigationProperty(RdfAssociation rdfNavigationProperty) {
			navigationProperties.put(RdfModel.rdfToOdata(rdfNavigationProperty.associationName), rdfNavigationProperty);
		}

		public RdfNode getComplexTypeNode() {
			return complexTypeNode;
		}

		public RdfNode getDomainNode() {
			return domainNode;
		}

		public String getComplexTypeName() {
			return complexTypeName;
		}

		public String getComplexTypeLabel() {
			return complexTypeLabel;
		}

		public String getDomainName() {
			return domainName;
		}

		public TreeMap<String, RdfProperty> getProperties() {
			return properties;
		}

		public TreeMap<String, RdfAssociation> getNavigationProperties() {
			return navigationProperties;
		}

		public FullQualifiedName getFullQualifiedName() {
			return new FullQualifiedName(domainClass.schema.schemaPrefix, this.getComplexTypeName());
		}

	}

	public static class RdfAssociation {
		private String associationName;
		private String associationLabel;
		private String varName;
		private String relatedKey;
		private RdfNode domainNode;

		private String domainName;
		public RdfEntityType domainClass;
		private RdfProperty superProperty;
		@SuppressWarnings("unused")
		private RdfNode rangeNode;
		private String rangeName;
		private RdfNode associationNode;
		private Boolean isInverse = false;
		//		private Boolean hasInverse = false;
		private RdfNode inversePropertyOf;
		private RdfAssociation inverseAssociation;
		private String description;
		private RdfEntityType rangeClass;
		private Cardinality rangeCardinality;
		private Cardinality domainCardinality;
		private RdfProperty fkProperty = null;

		public String getAssociationName() {
			return associationName;
		}

		public RdfNode getAssociationNode() {
			return associationNode;
		}

		public String getAssociationNodeIRI() {
			return associationNode.getIRI().toString();
		}

		public void setAssociationNode(RdfNode associationNode) {
			this.associationNode = associationNode;
		}

		public String getAssociationLabel() {
			return associationLabel;
		}

		public void setAssociationLabel(String associationLabel) {
			this.associationLabel = associationLabel.trim();
		}

		public String getDomainName() {
			return domainName;
		}

		public void setDomainName(String domainName) {
			this.domainName = domainName;
		}

		public String getRangeName() {
			return rangeName;
		}

		public void setRangeName(String rangeName) {
			this.rangeName = rangeName;
		}

		public String getDescription() {
			if (this.description == null || this.description.isEmpty()) {
				return associationLabel;
			} else {
				return description;
			}
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public RdfNode getInversePropertyOf() {
			return inversePropertyOf;
		}

		public String getInversePropertyOfURI() {
			return inversePropertyOf.getIRI().toString();
		}

		public RdfNode getDomainNode() {
			return domainNode;
		}

		public RdfEntityType getDomainClass() {
			return domainClass;
		}

		public String getDomainNodeURI() {
			return this.domainNode.getIRI().toString();
		}

		public void setDomainNode(RdfNode domainNode) {
			this.domainNode = domainNode;
		}

		public Cardinality getRangeCardinality() {
			return rangeCardinality;
		}

		public void setRangeCardinality(Cardinality rangeCardinality) {
			//TODO #95 this.rangeCardinality = rangeCardinality;
			if (this.rangeCardinality == null || this.rangeCardinality.equals(Cardinality.MANY)
					|| this.rangeCardinality.equals(Cardinality.MULTIPLE)) {
				this.rangeCardinality = rangeCardinality;
			}
		}

		public Cardinality getDomainCardinality() {
			return domainCardinality;
		}

		public void setDomainCardinality(Cardinality domainCardinality) {
			//TODO #95 this.domainCardinality = domainCardinality;
			if (this.domainCardinality == null || this.domainCardinality.equals(Cardinality.MANY)
					|| this.domainCardinality.equals(Cardinality.MULTIPLE)) {
				this.domainCardinality = domainCardinality;
			}

		}

		public String getAssociationIRI() {
			return associationNode.getIRI().toString();
		}

		public FullQualifiedName getFullQualifiedName() {
			return new FullQualifiedName(domainClass.schema.schemaPrefix, this.getEDMAssociationName());//associationName);

		}

		public String getEDMAssociationName() {
			if (this.domainClass.schema.isDefault) {
				return this.associationName;
			} else {
				return this.domainClass.schema.schemaPrefix + RdfConstants.CLASS_SEPARATOR + this.associationName;
			}
		}

		public String getAssociationNameFromEDM(String edmAssociationName) {
			if (this.domainClass.schema.isDefault) {
				return edmAssociationName;
			} else {
				return edmAssociationName.replace(this.domainClass.schema.schemaPrefix + RdfConstants.CLASS_SEPARATOR,
						"");
			}
		}

		/**
		 * @return the rangeClass
		 */
		public RdfEntityType getRangeClass() {
			return rangeClass;
		}

		public String getEDMAssociationSetName() {
			if (this.domainClass.schema.isDefault) {
				return this.associationName;
			} else {
				return this.domainClass.schema.schemaPrefix + RdfConstants.CLASS_SEPARATOR + this.associationName;
			}
		}

		public String getVarName() {
			return varName;
		}

		public void setVarName(String varName) {
			this.varName = varName;
		}

		public String getRelatedKey() {
			return relatedKey;
		}

		public void setRelatedKey(String relatedKey) {
			this.relatedKey = relatedKey;
		}

		public Boolean IsInverse() {
			return isInverse;
		}

		public void setIsInverse(Boolean isInverse) {
			this.isInverse = isInverse;
		}

		//		public void setHasInverse(Boolean hasInverse) {
		//			this.hasInverse = hasInverse;
		//		}
		public RdfAssociation getInverseAssociation() {
			return inverseAssociation;
		}

		public void setInverseAssociation(RdfAssociation inverseAssociation) {
			this.inverseAssociation = inverseAssociation;
		}

		public void setSuperProperty(RdfProperty superProperty) {
			this.superProperty = superProperty;

		}

		public RdfProperty getSuperProperty() {
			return superProperty;
		}

		public RdfProperty getFkProperty() {
			return fkProperty;
		}

		public void setFkProperty(RdfProperty property) {
			this.fkProperty = property;
		}

		public Boolean hasFkProperty() {
			return !(fkProperty == null);
		}
	}

	private class RdfURI {
		@SuppressWarnings("unused")
		@Deprecated
		private RdfNode node;
		public String localName;
		private String graphName;
		private String graphPrefix;
		private RdfSchema graph;

		RdfURI(RdfNode node) throws OData2SparqlException {
			this.node = node;
			String[] parts = rdfPrefixes.toQName(node, RdfConstants.QNAME_SEPARATOR)
					.split(RdfConstants.QNAME_SEPARATOR); //node.toQName(rdfPrefixes).split(":");
			if (parts[0].equals("http") || parts[0].equals("null")) {
				localName = node.getLocalName();
				graphName = node.getNamespace();
				graphPrefix = getOrCreatePrefix(null, graphName);
			} else if (parts.length == 1) {
				localName = parts[0];
				graphName = null;
				graphPrefix = null;
				return;
			} else {
				localName = parts[1];
				for (int i = 2; i < parts.length; i++) {
					localName += "_" + parts[i];
				}
				graphName = rdfPrefixes.getNsPrefixURI(parts[0]);
				graphPrefix = parts[0];
			}
			graph = getOrCreateGraph(graphName, graphPrefix);
		}

		@Override
		public String toString() {
			return graphName + localName;
		}
	}

	public class RdfNodeShape {
		private RdfNode nodeShapeNode;
		private RdfEntityType entityType;
		private RdfNodeShape baseNodeShape;
		private String nodeShapeName;
		private String nodeShapeLabel;
		private String nodeShapeDescription;
		private RdfSchema schema;
		private RdfEntityType targetClass;
		private boolean deactivated;
		private TreeMap<String, RdfDataPropertyShape> dataPropertyShapes = new TreeMap<String, RdfDataPropertyShape>();
		private TreeMap<String, RdfObjectPropertyShape> objectPropertyShapes = new TreeMap<String, RdfObjectPropertyShape>();

		//		public RdfNodeShape(RdfSchema schema) {
		//			this.schema = schema;
		//		}
		public RdfNodeShape(RdfNode nodeShapeNode) throws OData2SparqlException {
			this.nodeShapeNode = nodeShapeNode;
			RdfURI nodeShapeURI = new RdfURI(nodeShapeNode);
			this.schema = nodeShapeURI.graph;
		}

		public RdfNode getNodeShapeNode() {
			return nodeShapeNode;
		}

		public RdfEntityType getEntityType() {
			return entityType;
		}

		public void setEntityType(RdfEntityType entityType) {
			this.entityType = entityType;
		}

		public String getIRI() {
			return nodeShapeNode.getIRI().toString();
		}

		public RdfNodeShape getBaseNodeShape() {
			return baseNodeShape;
		}

		public void setBaseNodeShape(RdfNodeShape baseNodeShape) {
			this.baseNodeShape = baseNodeShape;
		}

		public String getNodeShapeName() {
			return nodeShapeName;
		}

		public void setNodeShapeName(String nodeShapeName) {
			this.nodeShapeName = nodeShapeName;
		}

		public String getNodeShapeLabel() {
			return nodeShapeLabel;
		}

		public void setNodeShapeLabel(String nodeShapeLabel) {
			this.nodeShapeLabel = nodeShapeLabel;
		}

		public String getNodeShapeDescription() {
			return nodeShapeDescription;
		}

		public void setNodeShapeDescription(String nodeShapeDescription) {
			this.nodeShapeDescription = nodeShapeDescription;
		}

		public RdfSchema getSchema() {
			return schema;
		}

		public void setSchema(RdfSchema schema) {
			this.schema = schema;
		}

		public RdfEntityType getTargetClass() {
			if (targetClass != null) {
				return targetClass;
			} else {
				return baseNodeShape.getTargetClass();
			}
		}

		public void setTargetClass(RdfEntityType targetClass) {
			this.targetClass = targetClass;
		}

		public boolean isDeactivated() {
			return deactivated;
		}

		public void setDeactivated(boolean deactivated) {
			this.deactivated = deactivated;
		}

		public TreeMap<String, RdfDataPropertyShape> getDataPropertyShapes() {
			return dataPropertyShapes;
		}

		public TreeMap<String, RdfObjectPropertyShape> getObjectPropertyShapes() {
			return objectPropertyShapes;
		}

		public void addDataPropertyShape(RdfDataPropertyShape dataPropertyShape) {
			dataPropertyShapes.put(dataPropertyShape.propertyShapeName, dataPropertyShape);
		}

		public void addObjectPropertyShape(RdfObjectPropertyShape objectPropertyShape) {
			objectPropertyShapes.put(objectPropertyShape.propertyShapeName, objectPropertyShape);
		}

		public RdfSchema deriveGraph() {
			RdfNodeShape baseNode = this.getBaseNodeShape();
			if (baseNode != null) {
				if (baseNode.getSchema() != null) {
					return baseNode.getSchema();
				} else {
					return baseNode.deriveGraph();
				}
			} else {
				//So now looks at where this is a propertyNode of a propertyShape

				return null;
			}
		}
		public FullQualifiedName getFullQualifiedName() {
			return new FullQualifiedName(this.getSchema().getSchemaPrefix(), this.getNodeShapeName());
		}
	}

	public class RdfDataPropertyShape {
		private RdfNodeShape nodeShape;
		private RdfNode propertyShapeNode;
		private String propertyShapeName;
		private String propertyShapeLabel;
		private String propertyShapeDescription;
		private RdfProperty path;
		private int minCount;
		private int maxCount;

		public RdfDataPropertyShape(RdfNodeShape nodeShape, RdfNode propertyShapeNode) {
			this.nodeShape = nodeShape;
			this.propertyShapeNode = propertyShapeNode;
		}

		public String getPropertyShapeName() {
			return propertyShapeName;
		}

		public RdfNodeShape getNodeShape() {
			return nodeShape;
		}

		public String getIRI() {
			return propertyShapeNode.getIRI().toString();
		}

		public void setPropertyShapeName(String propertyShapeName) {
			this.propertyShapeName = propertyShapeName;
		}

		public String getPropertyShapeLabel() {
			return propertyShapeLabel;
		}

		public void setPropertyShapeLabel(String propertyShapeLabel) {
			this.propertyShapeLabel = propertyShapeLabel;
		}

		public String getPropertyShapeDescription() {
			return propertyShapeDescription;
		}

		public void setPropertyShapeDescription(String propertyShapeDescription) {
			this.propertyShapeDescription = propertyShapeDescription;
		}

		public RdfProperty getPath() {
			return path;
		}

		public void setPath(RdfProperty path) {
			this.path = path;
		}

		public int getMinCount() {
			return minCount;
		}

		public void setMinCount(int minCount) {
			this.minCount = minCount;
		}

		public int getMaxCount() {
			return maxCount;
		}

		public void setMaxCount(int maxCount) {
			this.maxCount = maxCount;
		}
	}

	public class RdfObjectPropertyShape {
		private RdfNodeShape nodeShape;
		private RdfNode propertyShapeNode;
		private String propertyShapeName;
		private String propertyShapeLabel;
		private String propertyShapeDescription;
		private RdfAssociation path;
		private RdfNodeShape propertyNode;
		private int minCount;
		private int maxCount;

		public RdfObjectPropertyShape(RdfNodeShape nodeShape, RdfNode propertyShapeNode) {
			this.nodeShape = nodeShape;
			this.propertyShapeNode = propertyShapeNode;
		}

		public String getPropertyShapeName() {
			return propertyShapeName;
		}

		public RdfNodeShape getNodeShape() {
			return nodeShape;
		}

		public void setPropertyShapeName(String propertyShapeName) {
			this.propertyShapeName = propertyShapeName;
		}

		public String getPropertyShapeLabel() {
			return propertyShapeLabel;
		}

		public void setPropertyShapeLabel(String propertyShapeLabel) {
			this.propertyShapeLabel = propertyShapeLabel;
		}

		public String getPropertyShapeDescription() {
			return propertyShapeDescription;
		}

		public void setPropertyShapeDescription(String propertyShapeDescription) {
			this.propertyShapeDescription = propertyShapeDescription;
		}

		public RdfAssociation getPath() {
			return path;
		}

		public void setPath(RdfAssociation path) {
			this.path = path;
		}

		public RdfNodeShape getPropertyNode() {
			return propertyNode;
		}

		public void setPropertyNode(RdfNodeShape propertyNode) {
			this.propertyNode = propertyNode;
		}

		public int getMinCount() {
			return minCount;
		}

		public void setMinCount(int minCount) {
			this.minCount = minCount;
		}

		public int getMaxCount() {
			return maxCount;
		}

		public void setMaxCount(int maxCount) {
			this.maxCount = maxCount;
		}

	}

	public RdfEntityType getOrCreateEntityType(RdfNode classNode) throws OData2SparqlException {
		return getOrCreateEntityType(classNode, null);
	}

	RdfEntityType getOrCreateEntityType(RdfNode entityTypeNode, RdfNode entityTypeLabelNode)
			throws OData2SparqlException {

		RdfURI classURI = new RdfURI(entityTypeNode);
		RdfEntityType clazz = Enumerable.create(classURI.graph.classes)
				.firstOrNull(classNameEquals(rdfToOdata(classURI.localName)));
		if (clazz == null) {
			clazz = new RdfEntityType();
			clazz.schema = classURI.graph;
			clazz.entityTypeName = rdfToOdata(classURI.localName);

			clazz.entityTypeNode = entityTypeNode;
			classURI.graph.classes.add(clazz);
		}
		//Fixes #90
		if (entityTypeLabelNode != null) {
			clazz.entityTypeLabel = entityTypeLabelNode.getLiteralObject().toString().trim();
		}
		return clazz;
	}

	RdfEntityType getOrCreateEntityType(RdfNodeShape nodeShape) throws OData2SparqlException {

		RdfEntityType clazz = new RdfEntityType();
		clazz.schema = nodeShape.getSchema();
		clazz.entityTypeName = rdfToOdata(nodeShape.nodeShapeName);

		clazz.entityTypeNode = nodeShape.nodeShapeNode;
		nodeShape.getSchema().classes.add(clazz);

		clazz.setNodeShape(nodeShape);
		return clazz;
	}

	RdfEntityType getOrCreateEntityType(RdfNode entityTypeNode, RdfNode entityTypeLabelNode, RdfEntityType baseType)
			throws OData2SparqlException {
		RdfEntityType clazz = getOrCreateEntityType(entityTypeNode, entityTypeLabelNode);
		clazz.setBaseType(baseType);
		return clazz;
	}

	RdfDatatype getOrCreateDatatype(RdfNode datatypeNode) throws OData2SparqlException {
		RdfURI datatypeURI = new RdfURI(datatypeNode);
		RdfDatatype datatype = Enumerable.create(datatypeURI.graph.datatypes)
				.firstOrNull(datatypeNameEquals(datatypeURI.localName));
		if (datatype == null) {
			datatype = new RdfDatatype(rdfToOdata(datatypeURI.localName));
		}
		return datatype;
	}

	RdfEntityType getOrCreateOperationEntityType(RdfNode queryNode) throws OData2SparqlException {
		return getOrCreateOperationEntityType(queryNode, null, null, null, null, null, null);
	}

	RdfEntityType getOrCreateOperationEntityType(RdfNode queryNode, RdfNode queryLabel, RdfNode queryText,
			RdfNode deleteText, RdfNode insertText, RdfNode updateText, RdfNode updatePropertyText)
			throws OData2SparqlException {
		RdfEntityType operationEntityType = getOrCreateEntityType(queryNode, queryLabel);
		if (queryText != null) {
			operationEntityType.queryText = queryText.getLiteral().getLexicalForm();
		}
		if (deleteText != null) {
			operationEntityType.deleteText = deleteText.getLiteral().getLexicalForm();
		}
		if (insertText != null) {
			operationEntityType.insertText = insertText.getLiteral().getLexicalForm();
		}
		if (updateText != null) {
			operationEntityType.updateText = updateText.getLiteral().getLexicalForm();
		}
		if (updatePropertyText != null) {
			operationEntityType.updatePropertyText = updatePropertyText.getLiteral().getLexicalForm();
		}
		operationEntityType.setOperation(true);
		operationEntityType.setBaseType(null);

		return operationEntityType;
	}

	void getOrCreateOperationArguments(RdfNode queryNode, RdfNode queryPropertyNode, RdfNode varName, RdfNode rangeNode)
			throws OData2SparqlException {
		RdfEntityType operationEntityType = this.getOrCreateOperationEntityType(queryNode);
		if (operationEntityType.isOperation()) {
			operationEntityType.setFunctionImport(true);

			String propertyTypeName = rangeNode.getIRI().toString();

			FunctionImportParameter functionImportParameter = new FunctionImportParameter(
					varName.getLiteralValue().getLabel(), propertyTypeName, false);
			operationEntityType.getFunctionImportParameters().put(varName.getLiteralValue().getLabel(),
					functionImportParameter);
		} else {
		}
	}

	RdfProperty getOrCreateOperationProperty(RdfNode queryNode, RdfNode propertyNode, RdfNode propertyLabelNode,
			RdfNode rangeNode, RdfNode varName) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		String propertyTypeName = rangeNode.getIRI().toString();

		RdfEntityType operationEntityType = this.getOrCreateOperationEntityType(queryNode);
		if (!operationEntityType.isEntity()) {
			RdfProperty property = Enumerable.create(operationEntityType.getProperties())
					.firstOrNull(propertyNameEquals(propertyURI.localName));
			if (property == null) {
				property = new RdfProperty();

				property.propertyTypeName = propertyTypeName;

				property.propertyName = varName.getLiteralValue().getLabel();

				if (propertyLabelNode == null) {
					property.propertyLabel = RdfConstants.PROPERTY_LABEL_PREFIX + property.propertyName;
				} else {
					property.propertyLabel = propertyLabelNode.getLiteralValue().getLabel();
				}
				property.propertyNode = propertyNode;
			}
			property.setVarName(varName.getLiteralValue().getLabel());
			operationEntityType.properties.put(property.propertyName, property);
			return property;
		} else {
			return null;
		}
	}

	RdfAssociation getOrCreateOperationAssociation(RdfNode queryNode, RdfNode propertyNode, RdfNode propertyLabelNode,
			RdfNode rangeNode, RdfNode varName) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI operationURI = new RdfURI(queryNode);
		RdfURI rangeURI = new RdfURI(rangeNode);

		RdfEntityType operationEntityType = this.getOrCreateOperationEntityType(queryNode);
		if (!operationEntityType.isEntity()) {
			String associationName = rdfToOdata(propertyURI.localName);
			RdfAssociation association = operationEntityType.findNavigationProperty(propertyNode);// Enumerable.create(propertyURI.graph.associations).firstOrNull(associationNameEquals(associationName));
			if (association == null) {
				//TODO should not even get here
				association = buildAssociation(associationName, propertyNode, propertyURI, queryNode, operationURI,
						rangeNode, rangeURI);
			}
			if (propertyLabelNode == null) {
				association.associationLabel = RdfConstants.NAVIGATIONPROPERTY_LABEL_PREFIX
						+ association.associationName;
			} else {
				association.associationLabel = propertyLabelNode.getLiteralValue().getLabel();
			}

			association.setVarName(varName.getLiteralValue().getLabel());

			if (association.IsInverse()) { // is it the inverse of something?
				RdfAssociation inverseAssociation = association.getInverseAssociation();
				inverseAssociation.rangeCardinality = RdfConstants.Cardinality.ONE;
				inverseAssociation.domainCardinality = RdfConstants.Cardinality.MANY;

			} else {
				association.setIsInverse(false);
			}

			association.rangeCardinality = RdfConstants.Cardinality.MANY;
			association.domainCardinality = RdfConstants.Cardinality.ONE;

			//Since this is not a primary entity we need to add the keys of the navigation properties as properties, as well as adding them as primarykeys.
			RdfProperty property = getOrCreateOperationProperty(queryNode, propertyNode, propertyLabelNode, rangeNode,
					varName);
			property.isKey = true;
			operationEntityType.primaryKeys.put(property.propertyName,
					new RdfPrimaryKey(property.propertyName, property.propertyName));
			association.setRelatedKey(property.propertyName);
			return association;
		} else
			return null;
	}

	RdfProperty getOrCreateProperty(RdfNode propertyNode, RdfNode equivalentPropertyNode, RdfNode propertyLabelNode,
			RdfNode domainNode, RdfNode rangeNode, Cardinality cardinality) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);

		String propertyTypeName = rangeNode.getIRI().toString();

		RdfEntityType clazz = this.getOrCreateEntityType(domainNode);

		RdfProperty property = Enumerable.create(clazz.getProperties())
				.firstOrNull(propertyNameEquals(propertyURI.localName));
		if (property == null) {
			property = new RdfProperty();
			if (equivalentPropertyNode != null) {
				property.equivalentProperty = equivalentPropertyNode.getIRI().toString();
			}
			property.propertyName = rdfToOdata(propertyURI.localName);
			if (propertyLabelNode == null) {
				property.propertyLabel = RdfConstants.PROPERTY_LABEL_PREFIX + property.propertyName;
			} else {
				property.propertyLabel = propertyLabelNode.getLiteralObject().toString();
			}
			property.propertyTypeName = propertyTypeName;
			property.propertyNode = propertyNode;
			property.cardinality = cardinality;

			clazz.properties.put(property.propertyName, property);
		}
		return property;
	}

	RdfProperty getOrCreateProperty(RdfNode propertyNode, RdfNode propertyLabelNode, RdfNode domainNode)
			throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);

		RdfEntityType clazz = this.getOrCreateEntityType(domainNode);

		RdfProperty property = Enumerable.create(clazz.getProperties())
				.firstOrNull(propertyNameEquals(rdfToOdata(propertyURI.localName)));
		if (property == null) {
			property = new RdfProperty();
			property.propertyName = rdfToOdata(propertyURI.localName);
			if (propertyLabelNode == null) {
				property.propertyLabel = RdfConstants.PROPERTY_LABEL_PREFIX + property.propertyName;
			} else {
				property.propertyLabel = propertyLabelNode.getLiteralObject().toString();
			}
			property.propertyNode = propertyNode;
			property.ofClass = clazz;
			clazz.properties.put(property.propertyName, property);
		}
		return property;
	}

	RdfProperty getOrCreateFKProperty(RdfAssociation rdfNavigationProperty) throws OData2SparqlException {
		RdfNode propertyNode = rdfNavigationProperty.getAssociationNode();
		RdfNode domainNode = rdfNavigationProperty.getDomainNode();
		RdfEntityType clazz = this.getOrCreateEntityType(domainNode);
		RdfProperty property = getOrCreateProperty(propertyNode, null, domainNode);
		property.propertyName = property.propertyName + "Id";
		property.setFkProperty(rdfNavigationProperty);
		rdfNavigationProperty.setFkProperty(property);
		clazz.properties.put(property.propertyName, property);
		return property;
	}

	public RdfComplexType getOrCreateComplexType(RdfNode complexTypeNode, RdfNode complexTypeLabelNode,
			RdfNode superdomainNode) throws OData2SparqlException {
		RdfURI complexTypeURI = new RdfURI(complexTypeNode);

		RdfEntityType clazz = this.getOrCreateEntityType(superdomainNode);

		RdfComplexType complexType = Enumerable.create(clazz.getSchema().getComplexTypes())
				.firstOrNull(complexTypeNameEquals(rdfToOdata(complexTypeURI.localName)));
		if (complexType == null) {
			complexType = new RdfComplexType();
			complexType.complexTypeName = rdfToOdata(complexTypeURI.localName);
			if (complexTypeLabelNode == null) {
				complexType.complexTypeLabel = RdfConstants.PROPERTY_LABEL_PREFIX + complexType.complexTypeName;
			} else {
				complexType.complexTypeLabel = complexTypeLabelNode.getLiteralObject().toString();
			}
			complexType.complexTypeNode = complexTypeNode;
			complexType.domainClass = this.getOrCreateEntityType(superdomainNode);
			clazz.complexTypes.put(complexType.complexTypeName, complexType);
			clazz.getSchema().getComplexTypes().add(complexType);
		}
		return complexType;
	}

	void setPropertyRange(RdfNode propertyNode, HashSet<RdfEntityType> classes, RdfNode rangeNode)
			throws OData2SparqlException {
		if (classes.size() > 0) {
			RdfURI propertyURI = new RdfURI(propertyNode);
			for (RdfEntityType clazz : classes) {
				if (clazz != null) {
					RdfProperty property = Enumerable.create(clazz.getProperties())
							.firstOrNull(propertyNameEquals(rdfToOdata(propertyURI.localName)));
					String propertyTypeName = rangeNode.getIRI().toString();
					property.propertyTypeName = propertyTypeName;
				}
			}
		}
	}

	void setPropertyCardinality(RdfNode propertyNode, HashSet<RdfEntityType> classes, Cardinality cardinality)
			throws OData2SparqlException {
		if (classes.size() > 0) {
			RdfURI propertyURI = new RdfURI(propertyNode);
			for (RdfEntityType clazz : classes) {
				if (clazz != null) {
					RdfProperty property = Enumerable.create(clazz.getProperties())
							.firstOrNull(propertyNameEquals(rdfToOdata(propertyURI.localName)));
					property.cardinality = cardinality;
				}
			}
		}
	}

	RdfAssociation getOrCreateAssociation(RdfNode propertyNode, RdfNode propertyLabelNode, RdfNode domainNode,
			RdfNode rangeNode, RdfNode multipleDomainNode, RdfNode multipleRangeNode, Cardinality domainCardinality,
			Cardinality rangeCardinality) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI domainURI = new RdfURI(domainNode);
		RdfURI rangeURI = new RdfURI(rangeNode);
		String associationName = createAssociationName(multipleDomainNode, multipleRangeNode, domainURI, propertyURI,
				rangeURI);

		RdfAssociation association = Enumerable.create(domainURI.graph.associations)
				.firstOrNull(associationNameEquals(associationName));
		if (association == null) {
			association = buildAssociation(associationName, propertyNode, propertyURI, domainNode, domainURI, rangeNode,
					rangeURI);
		}
		if (propertyLabelNode == null) {
			if (association.associationLabel == null || association.associationLabel == "") {
				association.associationLabel = RdfConstants.NAVIGATIONPROPERTY_LABEL_PREFIX
						+ association.associationName;
			}
		} else {
			association.associationLabel = propertyLabelNode.getLiteralObject().toString();
		}
		association.setIsInverse(false);
		// #95 Only use the supplied cardinality if existing is undefined or MANY/MULTIPLE
		association.setDomainCardinality(domainCardinality);
		association.setRangeCardinality(rangeCardinality);
		return association;
	}

	RdfAssociation getOrCreateInverseAssociation(RdfNode inversePropertyNode, RdfNode inversePropertyLabelNode,
			RdfNode propertyNode, RdfNode domainNode, RdfNode rangeNode, RdfNode multipleDomainNode,
			RdfNode multipleRangeNode, Cardinality domainCardinality, Cardinality rangeCardinality)
			throws OData2SparqlException {

		RdfAssociation association = getOrCreateAssociation(propertyNode, null, domainNode, rangeNode,
				multipleDomainNode, multipleRangeNode, domainCardinality, rangeCardinality);
		RdfAssociation inverseAssociation = getOrCreateAssociation(inversePropertyNode, inversePropertyLabelNode,
				rangeNode, domainNode, multipleRangeNode, multipleDomainNode, rangeCardinality, domainCardinality);
		inverseAssociation.setIsInverse(true);
		inverseAssociation.inversePropertyOf = propertyNode;
		//Added because inverse is symmetrical
		inverseAssociation.setInverseAssociation(association);
		association.setIsInverse(true);
		association.setInverseAssociation(inverseAssociation);
		association.inversePropertyOf = inversePropertyNode;
		return inverseAssociation;
	}

	Boolean isAssociation(RdfNode propertyNode, RdfNode domainNode, RdfNode rangeNode, RdfNode multipleDomainNode,
			RdfNode multipleRangeNode) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI domainURI = new RdfURI(domainNode);
		RdfURI rangeURI = new RdfURI(rangeNode);
		String associationName = createAssociationName(multipleDomainNode, multipleRangeNode, domainURI, propertyURI,
				rangeURI);

		RdfAssociation association = Enumerable.create(domainURI.graph.associations)
				.firstOrNull(associationNameEquals(associationName));
		if (association == null) {
			return false;
		}
		return true;
	}

	private String createAssociationName(RdfNode multipleDomainNode, RdfNode multipleRangeNode, RdfURI domainURI,
			RdfURI propertyURI, RdfURI rangeURI) throws OData2SparqlException {
//Removed for V4 as multiple ranges and domains do not matter any more as navigationProperties are directly associated with EntityType and EntitySet
//		if (!(multipleDomainNode.getLiteralObject().equals(1) || multipleDomainNode.getLiteralObject().equals("1"))) {
//			if (!(multipleRangeNode.getLiteralObject().equals(1) || multipleRangeNode.getLiteralObject().equals("1"))) {
//				return rdfToOdata(domainURI.localName) + RdfConstants.PREDICATE_SEPARATOR
//						+ rdfToOdata(propertyURI.localName) + RdfConstants.PREDICATE_SEPARATOR
//						+ rdfToOdata(rangeURI.localName);
//			} else {
//				return rdfToOdata(domainURI.localName) + RdfConstants.PREDICATE_SEPARATOR
//						+ rdfToOdata(propertyURI.localName);
//			}
//		} else if (!(multipleRangeNode.getLiteralObject().equals(1)
//				|| multipleRangeNode.getLiteralObject().equals("1"))) {
//			return rdfToOdata(propertyURI.localName) + RdfConstants.PREDICATE_SEPARATOR
//					+ rdfToOdata(rangeURI.localName);
//		}
		return rdfToOdata(propertyURI.localName);
	}

	private RdfAssociation buildAssociation(String associationName, RdfNode propertyNode, RdfURI propertyURI,
			RdfNode domainNode, RdfURI domainURI, RdfNode rangeNode, RdfURI rangeURI) throws OData2SparqlException {
		RdfAssociation association = new RdfAssociation();
		association.associationName = associationName;
		association.associationNode = propertyNode;
		association.rangeName = rdfToOdata(rangeURI.localName);
		association.rangeNode = rangeNode;
		association.domainName = rdfToOdata(domainURI.localName);
		association.domainNode = domainNode;

		association.domainClass = this.getOrCreateEntityType(domainNode);
		association.rangeClass = this.getOrCreateEntityType(rangeNode);

		association.domainClass.navigationProperties.put(associationName, association);
		association.rangeClass.incomingAssociations.put(associationName, association);
		domainURI.graph.associations.add(association);
		return association;
	}

	RdfSchema getOrCreateGraph(String graphName, String graphPrefix) throws OData2SparqlException {
		RdfSchema graph = Enumerable.create(graphs).firstOrNull(graphNameEquals(graphName));
		if (graph == null) {
			graph = new RdfSchema();
			graph.schemaName = graphName;
			graph.schemaPrefix = graphPrefix;
			rdfPrefixes.setNsPrefix(graphPrefix, graphName);
			graphs.add(graph);
		}
		return graph;
	}

	public RdfNodeShape getOrCreateNodeShape(RdfNode nodeShapeNode, RdfNode baseNodeShapeNode,
			RdfNode nodeShapeLabelNode, RdfNode nodeShapeNameNode, RdfNode nodeShapeDescriptionNode,
			RdfNode nodeShapeTargetClassNode, RdfNode nodeShapeDeactivatedNode) throws OData2SparqlException {
		RdfNodeShape nodeShape = this.pendingNodeShapes.get(nodeShapeNode.getIRIString());
		if (nodeShape == null) {
			nodeShape = new RdfNodeShape(nodeShapeNode);

		}
		RdfNodeShape baseNodeShape = null;
		String nodeShapeName = null;
		String nodeShapeDescription = null;
		boolean nodeShapeDeactivated = false;
		RdfEntityType rdfEntityType = null;
		if (nodeShapeTargetClassNode != null) {
			RdfURI nodeShapeTargetClassURI = new RdfURI(nodeShapeTargetClassNode);
			rdfEntityType = Enumerable.create(nodeShapeTargetClassURI.graph.classes)
					.firstOrNull(classNameEquals(rdfToOdata(nodeShapeTargetClassURI.localName)));
			if (rdfEntityType != null)
				nodeShape.setTargetClass(rdfEntityType);
		}
		if (baseNodeShapeNode != null) {
			RdfURI baseNodeShapeURI = new RdfURI(baseNodeShapeNode);
			baseNodeShape = this.pendingNodeShapes.get(baseNodeShapeURI.toString());
			if (baseNodeShape == null)
				baseNodeShape = new RdfNodeShape(baseNodeShapeNode);
			nodeShape.setBaseNodeShape(baseNodeShape);
		}

		if (nodeShapeNameNode != null) {
			nodeShapeName = nodeShapeNameNode.getLiteralValue().stringValue();
		} else if (nodeShapeLabelNode != null) {
			nodeShapeName = nodeShapeLabelNode.getLiteralValue().stringValue();
		} else {
			nodeShapeName = nodeShapeNode.getLocalName();
		}
		nodeShape.setNodeShapeName(nodeShapeName);
		if(nodeShapeLabelNode!=null)
			nodeShape.setNodeShapeLabel(nodeShapeLabelNode.getLiteralValue().stringValue()); 
		if (nodeShapeDescriptionNode != null) {
			nodeShapeDescription = nodeShapeDescriptionNode.getLiteralValue().stringValue();
			nodeShape.setNodeShapeDescription(nodeShapeDescription);
		}

		if (nodeShapeDeactivatedNode != null) {
			nodeShapeDeactivated = nodeShapeDeactivatedNode.getLiteralValue().toString().equals("true");
			nodeShape.setDeactivated(nodeShapeDeactivated);
		}

		this.pendingNodeShapes.put(nodeShapeNode.getIRIString(), nodeShape);
		//	nodeShapeURI.graph.nodeShapes.add(nodeShape);

		return nodeShape;

	}

	public void getOrCreatePropertyShape(RdfNode nodeShapeNode, RdfNode propertyShapeNode,
			RdfNode propertyShapeLabelNode, RdfNode propertyShapeNameNode, RdfNode propertyShapeDescriptionNode,
			RdfNode pathNode, RdfNode inversePathNode, RdfNode propertyNode, RdfNode minCountNode, RdfNode maxCountNode)
			throws OData2SparqlException {
		String propertyShapeName;
		String propertyShapeDescription = null;
		String propertyShapeLabel = null;
		int minCount = 0;
		int maxCount = 1;

		RdfNodeShape nodeShape = this.pendingNodeShapes.get(nodeShapeNode.getIRIString());//nodeShapeURI.toString());
		if (nodeShape == null) {
			nodeShape = new RdfNodeShape(nodeShapeNode);
		}

		if (propertyShapeNameNode != null) {
			propertyShapeName = propertyShapeNameNode.getLiteralValue().stringValue();
		} else if (propertyShapeLabelNode != null) {
			propertyShapeName = propertyShapeLabelNode.getLiteralValue().stringValue();
		} else {
			propertyShapeName = propertyShapeNode.getLocalName();
		}

		if (propertyShapeDescriptionNode != null) {
			propertyShapeDescription = propertyShapeDescriptionNode.getLiteralValue().stringValue();
		}

		if (propertyShapeLabelNode != null) {
			propertyShapeLabel = propertyShapeLabelNode.getLiteralValue().stringValue();
		}

		if (minCountNode != null) {
			minCount = minCountNode.getLiteralValue().intValue();
		}

		if (maxCountNode != null) {
			maxCount = maxCountNode.getLiteralValue().intValue();
		}

		//nodeShape.getTargetClass();

		if (pathNode != null) {
			RdfAssociation navigationProperty = nodeShape.getTargetClass().findNavigationProperty(pathNode);
			if (navigationProperty != null) {
				RdfObjectPropertyShape objectPropertyShape = new RdfObjectPropertyShape(nodeShape, propertyShapeNode);
				objectPropertyShape.setPath(navigationProperty);
				objectPropertyShape.setPropertyShapeDescription(propertyShapeDescription);
				objectPropertyShape.setPropertyShapeName(propertyShapeName);
				objectPropertyShape.setPropertyShapeLabel(propertyShapeLabel);
				objectPropertyShape.setMinCount(minCount);
				objectPropertyShape.setMaxCount(maxCount);
				if (propertyNode != null) {
					RdfNodeShape propertyNodeShape = this.pendingNodeShapes.get(propertyNode.getIRIString());
					objectPropertyShape.setPropertyNode(propertyNodeShape);
				}
				nodeShape.addObjectPropertyShape(objectPropertyShape);

			} else {
				RdfProperty property = nodeShape.getTargetClass().findProperty(pathNode.getLocalName());
				if (property != null) {
					RdfDataPropertyShape dataPropertyShape = new RdfDataPropertyShape(nodeShape, propertyShapeNode);
					dataPropertyShape.setPath(property);
					dataPropertyShape.setPropertyShapeDescription(propertyShapeDescription);
					dataPropertyShape.setPropertyShapeName(propertyShapeName);
					dataPropertyShape.setPropertyShapeLabel(propertyShapeLabel);
					dataPropertyShape.setMinCount(minCount);
					dataPropertyShape.setMaxCount(maxCount);
					nodeShape.addDataPropertyShape(dataPropertyShape);

				} else {
					log.error("Cannot find path in targetClass for property");
				}
			}
		} else if (inversePathNode != null) {
			RdfAssociation navigationProperty = nodeShape.getTargetClass()
					.findInverseNavigationProperty(inversePathNode.getLocalName());
			if (navigationProperty != null) {
				RdfObjectPropertyShape objectPropertyShape = new RdfObjectPropertyShape(nodeShape, propertyShapeNode);
				objectPropertyShape.setPath(navigationProperty);
				objectPropertyShape.setPropertyShapeDescription(propertyShapeDescription);
				objectPropertyShape.setPropertyShapeName(propertyShapeName);
				objectPropertyShape.setPropertyShapeLabel(propertyShapeLabel);
				objectPropertyShape.setMinCount(minCount);
				objectPropertyShape.setMaxCount(maxCount);
				if (propertyNode != null) {
					RdfNodeShape propertyNodeShape = this.pendingNodeShapes.get(propertyNode.getIRIString());
					objectPropertyShape.setPropertyNode(propertyNodeShape);
				}
				nodeShape.addObjectPropertyShape(objectPropertyShape);

			} else {
				log.error("Cannot find path in targetClass for property");
			}
		} else {
			log.error("No path defined");
		}
	}

	public String getModelNamespace(RdfSchema rdfGraph) {
		if (rdfGraph.schemaPrefix != null) {
			return rdfGraph.schemaPrefix;
		} else {
			if (this.getRdfPrefixes().getNsURIPrefix(rdfGraph.schemaName) == null) {
				if (this.getRdfPrefixes().getNsURIPrefix(rdfGraph.schemaName + "#") == null) {
					if (this.getRdfPrefixes().getNsURIPrefix(rdfGraph.schemaName + "/") == null) {
						return RdfConstants.SPARQL_MODEL;
					} else {
						return this.getRdfPrefixes().getNsURIPrefix(rdfGraph.schemaName + "/");
					}
				} else {
					return this.getRdfPrefixes().getNsURIPrefix(rdfGraph.schemaName + "#");
				}
			} else {
				return this.getRdfPrefixes().getNsURIPrefix(rdfGraph.schemaName);
			}
		}
	}

	public String getOrCreatePrefix(String prefix, String uri) throws OData2SparqlException {
		return rdfPrefixes.getOrCreatePrefix(prefix, uri);
	}

	public static String rdfToOdata(String rdfName) {
		return rdfName.replace("-", "_").replace("/", "_").replace(".", "_");
	}

	private static final Predicate1<RdfSchema> graphNameEquals(final String graphName) {
		return new Predicate1<RdfSchema>() {
			public boolean apply(RdfSchema graph) {
				return graph.schemaName.equals(graphName);
			}
		};
	}

	private static final Predicate1<RdfEntityType> classNameEquals(final String className) {
		return new Predicate1<RdfEntityType>() {
			public boolean apply(RdfEntityType clazz) {
				return clazz.entityTypeName.equals(className);
			}
		};
	}

	private static final Predicate1<RdfProperty> propertyNameEquals(final String propertyName) {
		return new Predicate1<RdfProperty>() {
			public boolean apply(RdfProperty property) {
				return property.propertyName.equals(propertyName);
			}
		};
	}

	private static final Predicate1<RdfComplexType> complexTypeNameEquals(final String complexTypeName) {
		return new Predicate1<RdfComplexType>() {
			public boolean apply(RdfComplexType complexType) {
				return complexType.complexTypeName.equals(complexTypeName);
			}
		};
	}

	private static final Predicate1<RdfAssociation> associationNameEquals(final String associationName) {
		return new Predicate1<RdfAssociation>() {
			public boolean apply(RdfAssociation association) {
				return association.associationName.equals(associationName);
			}
		};
	}

	private static final Predicate1<RdfDatatype> datatypeNameEquals(final String datatypeName) {
		return new Predicate1<RdfDatatype>() {
			public boolean apply(RdfDatatype datatype) {
				return datatype.datatypeName.equals(datatypeName);
			}
		};
	}

	private static final Predicate1<RdfNodeShape> nodeShapeNameEquals(final String nodeShapeName) {
		return new Predicate1<RdfNodeShape>() {
			public boolean apply(RdfNodeShape rdfNodeShape) {
				return rdfNodeShape.nodeShapeName.equals(nodeShapeName);
			}
		};
	}

	public void allocateNodeShapesToGraphs() throws OData2SparqlException {
		for (RdfNodeShape nodeShape : pendingNodeShapes.values()) {
			if (nodeShape.getSchema() != null) {
				nodeShape.getSchema().nodeShapes.add(nodeShape);
			} else {
				RdfSchema schema = nodeShape.deriveGraph();
				if (schema != null) {
					nodeShape.setSchema(schema);
					schema.nodeShapes.add(nodeShape);
				} else {
					//TODO probably need to traverse the tree more than once
					for (RdfNodeShape nodeShape1 : pendingNodeShapes.values()) {
						for (RdfObjectPropertyShape propertyShape : nodeShape1.getObjectPropertyShapes().values()) {
							if (propertyShape.getPropertyNode() == null) {
							} else {
								if (propertyShape.getPropertyNode().equals(nodeShape)) {
									nodeShape.setSchema(propertyShape.getNodeShape().getSchema());
									propertyShape.getNodeShape().getSchema().nodeShapes.add(nodeShape);
									break;
								}
							}
						}
					}
				}
			}
			RdfEntityType nodeShapeEntityType = getOrCreateEntityType(nodeShape);
			nodeShape.setEntityType(nodeShapeEntityType);
		}
	}

}