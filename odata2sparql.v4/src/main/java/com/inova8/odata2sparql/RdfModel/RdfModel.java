package com.inova8.odata2sparql.RdfModel;

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
import com.inova8.odata2sparql.uri.UriUtils;

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
			String encodeEntityKey = UriUtils.encodeUri(decodedEntityKey);//decodedEntityKey.replaceAll("\\(", "%28").replaceAll("\\)","%29").replaceAll("\\/", "%2F");
			int colon = encodeEntityKey.indexOf(RdfConstants.QNAME_SEPARATOR);
			if (colon < 0)
				return encodeEntityKey;
			else {
				String uri = get(encodeEntityKey.substring(0, colon));
				return uri == null ? encodeEntityKey : uri + encodeEntityKey.substring(colon + 1);
			}
		}
		public String expandPredicate(String entityKey) throws OData2SparqlException {
			UrlValidator urlValidator = new UrlValidator();
			String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey);
			String expandedEntityKey = expandPrefix(decodedEntityKey);
			if (urlValidator.isValid(expandedEntityKey)) {
				return expandedEntityKey;
			} else {
				throw new OData2SparqlException("Invalid key: " + entityKey, null);
			}
		}
		public String expandPredicateKey(String predicateKey) throws OData2SparqlException {
			String entityKey = predicateKey.substring(1, predicateKey.length() - 1);
			return expandPredicate(entityKey);
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
				return (((BNode) node.getNode()).toString()).replace(RdfConstants.QNAME_SEPARATOR_RDF, RdfConstants.QNAME_SEPARATOR_ENCODED);
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
		public final List<RdfModel.RdfNavigationProperty> navigationProperties = new ArrayList<RdfModel.RdfNavigationProperty>();
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
		private HashSet<RdfEntityType> superTypes = new HashSet<RdfEntityType>();
		private boolean rootClass = false;
		private boolean isOperation = false;
		private boolean isEntity = false;
		private boolean functionImport = false;
		private String description;
		private HashSet<RdfEntityType> subTypes = new HashSet<RdfEntityType>();
		public String queryText;
		private String deleteText;
		private String insertText;
		private String updateText;
		private String updatePropertyText;
		private final TreeMap<String, FunctionImportParameter> functionImportParameters = new TreeMap<String, FunctionImportParameter>();
		private final TreeMap<String, RdfModel.RdfProperty> properties = new TreeMap<String, RdfModel.RdfProperty>();
		private final TreeMap<String, RdfModel.RdfNavigationProperty> navigationProperties = new TreeMap<String, RdfModel.RdfNavigationProperty>();
		//		private final TreeMap<String, RdfModel.RdfComplexType> complexTypes = new TreeMap<String, RdfModel.RdfComplexType>();
		private final TreeMap<String, RdfModel.RdfNavigationProperty> incomingNavigationProperties = new TreeMap<String, RdfModel.RdfNavigationProperty>();
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

		public HashSet<RdfEntityType> getSuperTypes() {
			return superTypes;
		}

		protected void addSubType(RdfEntityType subType) {
			subTypes.add(subType);
		}

		public Set<RdfEntityType> getSubTypes() {
			return subTypes;
		}

		public boolean isNodeShape() {
			return (nodeShape != null);
		}

		public RdfNodeShape getNodeShape() {
			return nodeShape;
		}

		public void setNodeShape(RdfNodeShape nodeShape) {
			this.nodeShape = nodeShape;
		}

		public HashSet<RdfEntityType> getAllSubTypes() {
			HashSet<RdfEntityType> allSubTypes = new HashSet<RdfEntityType>();
			allSubTypes.addAll(subTypes);
			for (RdfEntityType subType : subTypes) {
				allSubTypes.addAll(subType.getAllSubTypes());
			}
			return allSubTypes;
		}

		public void setBaseType(RdfEntityType baseType) {

			this.baseType = baseType;
			if (baseType != null) {
				baseType.addSubType(this);
				this.superTypes.add(baseType);
			}
		}

		public RdfSchema getSchema() {
			return schema;
		}

		public void setSchema(RdfSchema schema) {
			this.schema = schema;
		}

		public String getEntitySetLabel() {
			if (this.entitySetLabel == null) {
				String derivedName = this.getEntityTypeLabel();
				if (derivedName.equals("")) {
					derivedName = this.getEntityTypeName();
				}
				if (!derivedName.substring(derivedName.length() - 1).equals(RdfConstants.PLURAL))
					return derivedName + RdfConstants.PLURAL;
				else
					return derivedName;
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

		public String getURL() {
			//Gts the IRI that should be used in SPARQL
			if (isNodeShape()) {
				//get the target emtityType rather than the ndoeShape
				return getNodeShape().getIRI();//.getTargetClass().getIRI().toString();
			} else {
				return entityTypeNode.getIRI().toString();
			}
		}

		public Collection<RdfModel.RdfNavigationProperty> getNavigationProperties() {
			return navigationProperties.values();
		}

		public Collection<RdfModel.RdfNavigationProperty> getInheritedNavigationProperties() {
			Collection<RdfModel.RdfNavigationProperty> inheritedNavigationProperties = new ArrayList<RdfModel.RdfNavigationProperty>();
			inheritedNavigationProperties.addAll(navigationProperties.values());
			if (!this.getSuperTypes().isEmpty()) {
				HashSet<RdfEntityType> visited = new HashSet<RdfEntityType>();
				inheritedNavigationProperties.addAll(this.getSuperTypeNavigationProperties(visited));				
//				for (RdfEntityType superType : this.getSuperTypes()) {
//					inheritedNavigationProperties.addAll(superType.getInheritedNavigationProperties());
//				}
			}
			return inheritedNavigationProperties;
		}
		public Collection<RdfModel.RdfNavigationProperty> getSuperTypeNavigationProperties(HashSet<RdfEntityType> visited) {
			Collection<RdfModel.RdfNavigationProperty> inheritedNavigationProperties = new ArrayList<RdfModel.RdfNavigationProperty>();
			inheritedNavigationProperties.addAll(navigationProperties.values());
			if (!this.getSuperTypes().isEmpty()) {
				for (RdfEntityType superType : this.getSuperTypes()) {
					if( !visited.contains(superType)) {
						visited.add(superType);
						inheritedNavigationProperties.addAll(superType.getSuperTypeNavigationProperties(visited));
					}
				}
			}
			return inheritedNavigationProperties;
		}
		public Collection<RdfModel.RdfProperty> getProperties() {
			return properties.values();
		}

		public HashSet<RdfModel.RdfProperty> getInheritedProperties() {
			HashSet <RdfModel.RdfProperty> inheritedProperties = new HashSet <RdfModel.RdfProperty>();
			inheritedProperties.addAll(properties.values());
			if (!this.getSuperTypes().isEmpty()) {
				HashSet<RdfEntityType> visited = new HashSet<RdfEntityType>();
				inheritedProperties.addAll(this.getSuperTypeProperties(visited));
			}
			return inheritedProperties;
		}

		public HashSet<RdfModel.RdfProperty> getSuperTypeProperties(HashSet<RdfEntityType> visited) {
			HashSet <RdfModel.RdfProperty> inheritedProperties = new HashSet <RdfModel.RdfProperty>();
			inheritedProperties.addAll(properties.values());
			if (!this.getSuperTypes().isEmpty()) {
				for (RdfEntityType superType : this.getSuperTypes()) {
					if( !visited.contains(superType)) {
						visited.add(superType);
						inheritedProperties.addAll(superType.getSuperTypeProperties(visited));
					}
				}
			}
			return inheritedProperties;
		}	
		
		public RdfNavigationProperty findNavigationProperty(String navigationPropertyName) {
			return navigationProperties.get(navigationPropertyName);
		}

		public RdfNavigationProperty findInverseNavigationProperty(String navigationPropertyName) {
			return incomingNavigationProperties.get(navigationPropertyName);
		}

		public RdfNavigationProperty findNavigationPropertyByEDMNavigationPropertyName(
				String edmNavigationPropertyName) {
			String navigationPropertyName = null;
			if (this.schema.isDefault) {
				navigationPropertyName = edmNavigationPropertyName;
			} else {
				navigationPropertyName = edmNavigationPropertyName
						.replace(this.schema.schemaPrefix + RdfConstants.CLASS_SEPARATOR, "");
			}
			return findNavigationProperty(navigationPropertyName);
		}

		public RdfNavigationProperty findNavigationProperty(RdfNode navigationPropertyNode) {
			for (RdfNavigationProperty navigationProperty : this.getInheritedNavigationProperties()) {
				if (navigationProperty.navigationPropertyNode.getIRI().toString()
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
							return new RdfComplexTypePropertyPair(property.getComplexType(), complexProperty);
						}
					}
					for (RdfNavigationProperty complexNavigationProperty : property.getComplexType()
							.getNavigationProperties().values()) {
						if (complexNavigationProperty.getNavigationPropertyIRI().toString()
								.equals(propertyNode.getIRI().toString())) {
							return new RdfComplexTypePropertyPair(property.getComplexType(), complexNavigationProperty);
						}
					}
					for (RdfComplexProperty complexProperty : property.getComplexType().getComplexProperties()
							.values()) {
						if (complexProperty.getComplexPropertyIRI().toString()
								.equals(propertyNode.getIRI().toString())) {
							return new RdfComplexTypePropertyPair(property.getComplexType(), complexProperty);
						}
					}
					for (RdfShapedNavigationProperty shapedNavigationProperty : property.getComplexType()
							.getShapedNavigationProperties().values()) {
						if (shapedNavigationProperty.getRdfNavigationProperty().getNavigationPropertyIRI().toString()
								.equals(propertyNode.getIRI().toString())) {
							return new RdfComplexTypePropertyPair(property.getComplexType(),
									shapedNavigationProperty.getRdfNavigationProperty());
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
								property.getComplexType().getProperties().get(propertyName));
					} else if (property.getComplexType().getNavigationProperties().containsKey(propertyName)) {
						return new RdfComplexTypePropertyPair(property.getComplexType(),
								property.getComplexType().getNavigationProperties().get(propertyName));
					} else if (property.getComplexType().getShapedNavigationProperties().containsKey(propertyName)) {
						return new RdfComplexTypePropertyPair(property.getComplexType(), property.getComplexType()
								.getShapedNavigationProperties().get(propertyName).getRdfNavigationProperty());
					} else if (property.getComplexType().getComplexProperties().containsKey(propertyName)) {
						return new RdfComplexTypePropertyPair(property.getComplexType(), property.getComplexType()
								.getComplexProperties().get(propertyName).getRdfObjectPropertyShape().getPath());
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

	/**
	 * 
	 * A primary key property
	 *
	 */
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

	/**
	 * Represents a property of a complexType that references a complexType.
	 * 
	 */
	public static class RdfComplexProperty {
		private String complexPropertyName;
		private String complexPropertyLabel;
		private String complexPropertyTypeName;
		private RdfObjectPropertyShape rdfObjectPropertyShape;
		private RdfComplexType complexType;
		private Cardinality cardinality;

		public String getComplexPropertyName() {
			return complexPropertyName;
		}

		public void setComplexPropertyName(String complexPropertyName) {
			this.complexPropertyName = complexPropertyName;
		}

		public String getComplexPropertyLabel() {
			return complexPropertyLabel;
		}

		public void setComplexPropertyLabel(String complexPropertyLabel) {
			this.complexPropertyLabel = complexPropertyLabel;
		}

		public String getComplexPropertyTypeName() {
			return complexPropertyTypeName;
		}

		public void setComplexPropertyTypeName(String complexPropertyTypeName) {
			this.complexPropertyTypeName = complexPropertyTypeName;
		}

		public RdfObjectPropertyShape getRdfObjectPropertyShape() {
			return rdfObjectPropertyShape;
		}

		public void setRdfObjectPropertyShape(RdfObjectPropertyShape rdfObjectPropertyShape) {
			this.rdfObjectPropertyShape = rdfObjectPropertyShape;
		}

		public void setComplexType(RdfComplexType complexType) {
			this.complexType = complexType;

		}

		public void setCardinality(Cardinality cardinality) {
			this.cardinality = cardinality;
		}

		public RdfComplexType getComplexType() {
			return complexType;
		}

		public Cardinality getCardinality() {
			return cardinality;
		}

		public boolean isOptional() {
			if (rdfObjectPropertyShape.getMinCount() == 0)
				return true;
			else
				return false;
		}

		public String getComplexPropertyIRI() {
			if (rdfObjectPropertyShape.isInversePath()) {
				return rdfObjectPropertyShape.getPath().getInversePropertyOfURI();
			} else {
				return rdfObjectPropertyShape.getPath().getNavigationPropertyIRI();
			}
		}
	}

	/**
	 * Represents an attribute (primitive or complexType) property of an
	 * entityType. Represents a primitive attribute of a complexType.
	 * 
	 */
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
		private RdfConstants.Cardinality cardinality = RdfConstants.Cardinality.ZERO_TO_ONE;
		private String equivalentProperty;
		private RdfEntityType ofClass;
		private String description;
		private RdfNavigationProperty fkProperty = null;
		private RdfObjectPropertyShape rdfObjectPropertyShape = null;

		public RdfObjectPropertyShape getRdfObjectPropertyShape() {
			return rdfObjectPropertyShape;
		}

		public void setRdfObjectPropertyShape(RdfObjectPropertyShape rdfObjectPropertyShape) {
			this.rdfObjectPropertyShape = rdfObjectPropertyShape;
		}

		public RdfConstants.Cardinality getCardinality() {
			return cardinality;
		}

		public boolean isOptional() {
			switch (cardinality) {
			case ZERO_TO_ONE:
			case MANY:
				return true;
			case MULTIPLE:
			case ONE:
				return false;
			default:
				return true;
			}
		}

		public boolean isCollection() {
			switch (cardinality) {
			case MANY:
			case MULTIPLE:
				return true;
			default:
				return false;
			}
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

		public RdfNavigationProperty getFkProperty() {
			return fkProperty;
		}

		public void setFkProperty(RdfNavigationProperty fkProperty) {
			this.fkProperty = fkProperty;
		}

		public RdfEntityType getOfClass() {
			return ofClass;
		}

		public void setOfClass(RdfEntityType ofClass) {
			this.ofClass = ofClass;
		}
	}

	public static class RdfComplexTypePropertyPair {
		private RdfComplexType rdfComplexType;
		private RdfProperty rdfProperty;
		private RdfNavigationProperty rdfNavigationProperty;
		private RdfComplexProperty rdfComplexProperty;
		private boolean isNavigationProperty;
		private boolean isProperty;
		private boolean isComplexProperty;

		public RdfComplexTypePropertyPair(RdfComplexType rdfComplexType, RdfNavigationProperty rdfNavigationProperty) {
			super();
			this.rdfComplexType = rdfComplexType;
			this.setRdfNavigationProperty(rdfNavigationProperty);
			this.isNavigationProperty = true;
		}

		public RdfComplexTypePropertyPair(RdfComplexType rdfComplexType, RdfProperty rdfProperty) {
			super();
			this.rdfComplexType = rdfComplexType;
			this.rdfProperty = rdfProperty;
			this.isProperty = true;
		}

		public RdfComplexTypePropertyPair(RdfComplexType rdfComplexType, RdfComplexProperty rdfComplexProperty) {
			super();
			this.rdfComplexType = rdfComplexType;
			this.rdfComplexProperty = rdfComplexProperty;
			this.isComplexProperty = true;
		}

		public String getEquivalentComplexPropertyName() {
			return getRdfComplexType().getEquivalentComplexPropertyName();

		}

		public RdfComplexType getRdfComplexType() {
			return rdfComplexType;
		}

		public RdfProperty getRdfProperty() {
			return rdfProperty;
		}

		public RdfComplexProperty getRdfComplexProperty() {
			return rdfComplexProperty;
		}

		public RdfNavigationProperty getRdfNavigationProperty() {
			return rdfNavigationProperty;
		}

		public void setRdfNavigationProperty(RdfNavigationProperty rdfNavigationProperty) {
			this.rdfNavigationProperty = rdfNavigationProperty;
		}

		public boolean isNavigationProperty() {
			return isNavigationProperty;
		}

		public boolean isProperty() {
			return isProperty;
		}

		public boolean isComplexProperty() {
			return isComplexProperty;
		}

	}

	public static class RdfComplexType {
		private RdfNode complexTypeNode;
		private String complexTypeName;
		private String complexTypeLabel;
		private RdfNode domainNode;
		private String domainName;
		private RdfSchema schema;
		private TreeMap<String, RdfProperty> properties = new TreeMap<String, RdfProperty>();
		private TreeMap<String, RdfComplexProperty> complexProperties = new TreeMap<String, RdfComplexProperty>();
		private TreeMap<String, RdfNavigationProperty> navigationProperties = new TreeMap<String, RdfNavigationProperty>();
		private TreeMap<String, RdfShapedNavigationProperty> shapedNavigationProperties = new TreeMap<String, RdfShapedNavigationProperty>();
		public RdfEntityType domainClass;
		private boolean provenanceType = false;

		public void addProperty(RdfProperty rdfProperty) {
			properties.put(RdfModel.rdfToOdata(rdfProperty.propertyName), rdfProperty);
		}

		public void addComplexProperty(RdfComplexProperty rdfComplexProperty) {
			complexProperties.put(RdfModel.rdfToOdata(rdfComplexProperty.getComplexPropertyName()), rdfComplexProperty);
		}

		public void addNavigationProperty(RdfNavigationProperty rdfNavigationProperty) {
			navigationProperties.put(RdfModel.rdfToOdata(rdfNavigationProperty.navigationPropertyName),
					rdfNavigationProperty);
		}

		public void addShapedNavigationProperty(RdfShapedNavigationProperty shapedNavigationProperty) {
			shapedNavigationProperties.put(
					RdfModel.rdfToOdata(shapedNavigationProperty.getRdfNavigationProperty().navigationPropertyName),
					shapedNavigationProperty);
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

		public String getEquivalentComplexPropertyName() {
			return getComplexTypeName().replace(RdfConstants.SHAPE_POSTFIX, "");

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

		public TreeMap<String, RdfComplexProperty> getComplexProperties() {
			return complexProperties;
		}

		public TreeMap<String, RdfNavigationProperty> getNavigationProperties() {
			return navigationProperties;
		}

		public TreeMap<String, RdfShapedNavigationProperty> getShapedNavigationProperties() {
			return shapedNavigationProperties;
		}

		public FullQualifiedName getFullQualifiedName() {
			if (domainClass != null) {
				return new FullQualifiedName(domainClass.schema.schemaPrefix, this.getComplexTypeName());
			} else {
				return new FullQualifiedName(this.schema.schemaPrefix, this.getComplexTypeName());
			}
		}

		public void setSchema(RdfSchema schema) {
			this.schema = schema;

		}

		public void setProvenanceType(boolean provenanceType) {
			this.provenanceType = provenanceType;
		}

		public boolean isProvenanceType() {
			return this.provenanceType;
		}

		public String getIRI() {
			return schema.schemaName + complexTypeName;
		}

	}

	public static class RdfShapedNavigationProperty {
		private RdfNavigationProperty rdfNavigationProperty;
		private String shapedNavigationPropertyName;
		private String shapedNavigationPropertyLabel;
		private Cardinality cardinality;

		public RdfShapedNavigationProperty(RdfNavigationProperty rdfNavigationProperty,
				String shapedNavigationPropertyName, String shapedNavigationPropertyLabel, Cardinality cardinality) {
			super();
			this.rdfNavigationProperty = rdfNavigationProperty;
			this.shapedNavigationPropertyName = shapedNavigationPropertyName;
			this.shapedNavigationPropertyLabel = shapedNavigationPropertyLabel;
			this.cardinality = cardinality;
		}

		public RdfNavigationProperty getRdfNavigationProperty() {
			return rdfNavigationProperty;
		}

		public String getShapedNavigationPropertyName() {
			return shapedNavigationPropertyName;
		}

		public String getShapedNavigationPropertyLabel() {
			return shapedNavigationPropertyLabel;
		}

		public Cardinality getCardinality() {
			return cardinality;
		}

		public boolean isOptional() {
			switch (cardinality) {
			case ZERO_TO_ONE:
			case MANY:
				return true;
			case MULTIPLE:
			case ONE:
				return false;
			default:
				return true;
			}
		}
	}

	public static class RdfNavigationProperty {
		private String navigationPropertyName;
		private String navigationPropertyLabel;
		private String varName;
		private String relatedKey;
		private RdfNode domainNode;

		private String domainName;
		public RdfEntityType domainClass;
		private RdfProperty superProperty;
		@SuppressWarnings("unused")
		private RdfNode rangeNode;
		private String rangeName;
		private RdfNode navigationPropertyNode;
		private Boolean isInverse = false;
		//		private Boolean hasInverse = false;
		private RdfNode inversePropertyOf;
		private RdfNavigationProperty inverseNavigationProperty;
		private String description;
		private RdfEntityType rangeClass;
		private Cardinality rangeCardinality;
		private Cardinality domainCardinality;
		private RdfProperty fkProperty = null;
		public RdfSchema navigationPropertySchema;

		public String getNavigationPropertyName() {
			return navigationPropertyName;
		}

		public RdfNode getNavigationPropertyNode() {
			return navigationPropertyNode;
		}

		public String getNavigationPropertyNodeIRI() {
			return navigationPropertyNode.getIRI().toString();
		}

		public void setNavigationPropertyNode(RdfNode navigationPropertyNode) {
			this.navigationPropertyNode = navigationPropertyNode;
		}

		public String getNavigationPropertyLabel() {
			return navigationPropertyLabel;
		}

		public void setNavigationPropertyLabel(String navigationPropertyLabel) {
			this.navigationPropertyLabel = navigationPropertyLabel.trim();
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
				return navigationPropertyLabel;
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
			//Fixes #95 this.rangeCardinality = rangeCardinality;
			if (this.rangeCardinality == null || this.rangeCardinality.equals(Cardinality.MANY)
					|| this.rangeCardinality.equals(Cardinality.MULTIPLE)) {
				this.rangeCardinality = rangeCardinality;
			}
		}

		public Cardinality getDomainCardinality() {
			return domainCardinality;
		}

		public void setDomainCardinality(Cardinality domainCardinality) {
			//Fixes #95 this.domainCardinality = domainCardinality;
			if (this.domainCardinality == null || this.domainCardinality.equals(Cardinality.MANY)
					|| this.domainCardinality.equals(Cardinality.MULTIPLE)) {
				this.domainCardinality = domainCardinality;
			}
		}

		public String getNavigationPropertyIRI() {
			return navigationPropertyNode.getIRI().toString();
		}

		public FullQualifiedName getFullQualifiedName() {
			return new FullQualifiedName(domainClass.schema.schemaPrefix, this.getEDMNavigationPropertyName());
		}

		public String getEDMNavigationPropertyName() {
			if (this.navigationPropertySchema.isDefault) {
				return this.navigationPropertyName;
			} else {
				return this.navigationPropertySchema.schemaPrefix + RdfConstants.CLASS_SEPARATOR
						+ this.navigationPropertyName;
			}
		}

		public RdfSchema getNavigationPropertySchema() {
			return this.navigationPropertySchema;
		}

		public String getNavigationPropertyNameFromEDM(String edmNavigationPropertyName) {
			
			if (this.navigationPropertySchema.isDefault) {
				return edmNavigationPropertyName;
			} else {
				return edmNavigationPropertyName
						.replace(this.navigationPropertySchema.schemaPrefix + RdfConstants.CLASS_SEPARATOR, "");
			}
		}

		/**
		 * @return the rangeClass
		 */
		public RdfEntityType getRangeClass() {
			return rangeClass;
		}

		//		public String getEDMAssociationSetName() {
		//			if (this.domainClass.schema.isDefault) {
		//				return this.navigationPropertyName;
		//			} else {
		//				return this.domainClass.schema.schemaPrefix + RdfConstants.CLASS_SEPARATOR + this.navigationPropertyName;
		//			}
		//		}

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
		public RdfNavigationProperty getInverseNavigationProperty() {
			return inverseNavigationProperty;
		}

		public void setInverseNavigationProperty(RdfNavigationProperty inverseNavigationProperty) {
			this.inverseNavigationProperty = inverseNavigationProperty;
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

		public boolean isOptional() {
			switch (rangeCardinality) {
			case ZERO_TO_ONE:
			case MANY:
				return true;
			case MULTIPLE:
			case ONE:
				return false;
			default:
				return true;
			}
		}
	}

	private class RdfURI {
		public String localName;

		private String graphName;
		private String graphPrefix;
		private RdfSchema graph;

		RdfURI(RdfNode node) throws OData2SparqlException {
			//	this.node = node;
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
				if(graphPrefix.equals("_")) return;
			}
			graph = getOrCreateGraph(graphName, graphPrefix);	
		}

		RdfURI(RdfSchema graph, String localName) {
			this.localName = localName;
			this.graph = graph;
			this.graphName = graph.getSchemaName();
			this.graphPrefix = graph.getSchemaPrefix();
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
		private RdfComplexType complexType;
		private boolean deactivated;
		private TreeMap<String, RdfDataPropertyShape> dataPropertyShapes = new TreeMap<String, RdfDataPropertyShape>();
		private TreeMap<String, RdfObjectPropertyShape> objectPropertyShapes = new TreeMap<String, RdfObjectPropertyShape>();

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
			//			return nodeShapeNode.getIRI().toString();
			return schema.getSchemaName() + nodeShapeName;
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

		public String getNodeShapeComplexTypeName() {
			return nodeShapeName + RdfConstants.SHAPE_POSTFIX;
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

		public void setComplexType(RdfComplexType complexType) {
			this.complexType = complexType;
		}

		public RdfComplexType getComplexType() {
			return complexType;
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
		private String propertyShapeName;
		private String propertyShapeLabel;
		private String propertyShapeDescription;
		private RdfNavigationProperty path;
		private RdfNodeShape propertyNode;
		private Integer minCount;
		private Integer maxCount;
		private boolean isInversePath = false;

		public RdfObjectPropertyShape(RdfNodeShape nodeShape, RdfNode propertyShapeNode) {
			this.nodeShape = nodeShape;
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

		public RdfNavigationProperty getPath() {
			return path;
		}

		public void setPath(RdfNavigationProperty path) {
			this.path = path;
		}

		public RdfNodeShape getPropertyNode() {
			return propertyNode;
		}

		public void setPropertyNode(RdfNodeShape propertyNode) {
			this.propertyNode = propertyNode;
		}

		public Integer getMinCount() {
			return minCount;
		}

		public void setMinCount(Integer minCount) {
			this.minCount = minCount;
		}

		public Integer getMaxCount() {
			return maxCount;
		}

		public void setMaxCount(Integer maxCount) {
			this.maxCount = maxCount;
		}

		public void setInversePath(boolean isInversePath) {
			this.isInversePath = isInversePath;
		}

		public boolean isInversePath() {
			return isInversePath;
		}
	}

	public RdfRepository getRdfRepository() {
		return rdfRepository;
	}

	public RdfPrefixes getRdfPrefixes() {
		return rdfPrefixes;
	}

	public RdfEntityType getOrCreateEntityTypeFromShape(RdfNode shapeNode) throws OData2SparqlException {
		RdfURI classURI = new RdfURI(shapeNode);
		String equivalentClassName = classURI.localName.replace(RdfConstants.SHAPE_POSTFIX, "");
		RdfEntityType clazz = Enumerable.create(classURI.graph.classes)
				.firstOrNull(classNameEquals(rdfToOdata(equivalentClassName)));
		if (clazz == null) {
			//Should never get here
			clazz = new RdfEntityType();
			clazz.schema = classURI.graph;
			clazz.entityTypeName = rdfToOdata(equivalentClassName);

			//clazz.entityTypeNode = entityTypeNode;
			classURI.graph.classes.add(clazz);
		}
		return clazz;
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
		RdfURI classURI = new RdfURI(nodeShape.getSchema(), nodeShape.getNodeShapeName().toString());
		RdfEntityType clazz = Enumerable.create(nodeShape.getSchema().classes)
				.firstOrNull(classNameEquals(rdfToOdata(classURI.localName)));
		if (clazz == null) {
			clazz = new RdfEntityType();
			clazz.schema = nodeShape.getSchema();
			clazz.entityTypeName = rdfToOdata(nodeShape.nodeShapeName);

			clazz.entityTypeNode = nodeShape.nodeShapeNode;
			nodeShape.getSchema().classes.add(clazz);

			clazz.setNodeShape(nodeShape);
		}
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

	RdfNavigationProperty getOrCreateOperationNavigationProperty(RdfNode queryNode, RdfNode propertyNode,
			RdfNode propertyLabelNode, RdfNode rangeNode, RdfNode varName) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI operationURI = new RdfURI(queryNode);
		RdfURI rangeURI = new RdfURI(rangeNode);

		RdfEntityType operationEntityType = this.getOrCreateOperationEntityType(queryNode);
		if (!operationEntityType.isEntity()) {
			String navigationPropertyName = rdfToOdata(propertyURI.localName);
			RdfNavigationProperty navigationProperty = operationEntityType.findNavigationProperty(propertyNode);
			if (navigationProperty == null) {
				//TODO should not even get here
				navigationProperty = buildNavigationProperty(navigationPropertyName, propertyNode, propertyURI,
						queryNode, operationURI, rangeNode, rangeURI);
			}
			if (propertyLabelNode == null) {
				navigationProperty.navigationPropertyLabel = RdfConstants.NAVIGATIONPROPERTY_LABEL_PREFIX
						+ navigationProperty.navigationPropertyName;
			} else {
				navigationProperty.navigationPropertyLabel = propertyLabelNode.getLiteralValue().getLabel();
			}

			navigationProperty.setVarName(varName.getLiteralValue().getLabel());

			if (navigationProperty.IsInverse()) { // is it the inverse of something?
				RdfNavigationProperty inverseNavigationProperty = navigationProperty.getInverseNavigationProperty();
				inverseNavigationProperty.rangeCardinality = RdfConstants.Cardinality.ONE;
				inverseNavigationProperty.domainCardinality = RdfConstants.Cardinality.MANY;

			} else {
				navigationProperty.setIsInverse(false);
			}

			navigationProperty.rangeCardinality = RdfConstants.Cardinality.MANY;
			navigationProperty.domainCardinality = RdfConstants.Cardinality.ONE;

			//Since this is not a primary entity we need to add the keys of the navigation properties as properties, as well as adding them as primarykeys.
			RdfProperty property = getOrCreateOperationProperty(queryNode, propertyNode, propertyLabelNode, rangeNode,
					varName);
			property.isKey = true;
			operationEntityType.primaryKeys.put(property.propertyName,
					new RdfPrimaryKey(property.propertyName, property.propertyName));
			navigationProperty.setRelatedKey(property.propertyName);
			return navigationProperty;
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
				property.setEquivalentProperty(equivalentPropertyNode.getIRI().toString());
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

	RdfProperty getOrCreateComplexProperty(RdfNodeShape nodeShape, RdfNodeShape propertyNodeShape)
			throws OData2SparqlException {

		RdfEntityType clazz = nodeShape.getEntityType();
		RdfProperty property = null;
		if (clazz != null) {
			property = Enumerable.create(clazz.getProperties())
					.firstOrNull(propertyNameEquals(rdfToOdata(propertyNodeShape.getNodeShapeName())));
		}
		if (property == null) {
			property = new RdfProperty();
			property.propertyName = rdfToOdata(propertyNodeShape.getNodeShapeName());
			property.propertyLabel = propertyNodeShape.getNodeShapeLabel();
			//property.propertyUri = propertyNodeShape.
			//	property.propertyNode = propertyNode;
			property.setOfClass(clazz);
			//	clazz.properties.put(property.propertyName, property);
		}
		return property;
	}

	//	RdfProperty getOrCreateComplexProperty(RdfNodeShape nodeShape, RdfObjectPropertyShape objectPropertyNodeShape)
	//			throws OData2SparqlException {
	//		RdfNodeShape propertyNodeShape = objectPropertyNodeShape.getPropertyNode();
	//		RdfEntityType clazz = nodeShape.getEntityType();
	//		RdfProperty property = null;
	//		if (clazz != null) {
	//			property = Enumerable.create(clazz.getProperties())
	//					.firstOrNull(propertyNameEquals(rdfToOdata(propertyNodeShape.getNodeShapeName())));
	//		}
	//		if (property == null) {
	//			property = new RdfProperty();
	//			property.propertyName = rdfToOdata(propertyNodeShape.getNodeShapeName());
	//			property.propertyLabel = propertyNodeShape.getNodeShapeLabel();
	//			property.propertyUri = objectPropertyNodeShape.getPath().getNavigationPropertyIRI();
	//			//	property.propertyNode = propertyNode;
	//			property.setOfClass(clazz);
	//			//	clazz.properties.put(property.propertyName, property);
	//		}
	//		return property;
	//	}

	RdfComplexProperty getOrCreateComplexProperty(RdfNodeShape nodeShape,
			RdfObjectPropertyShape objectPropertyNodeShape) throws OData2SparqlException {

		RdfNavigationProperty equivalentNavigationProperty;
		if (objectPropertyNodeShape.getPath().IsInverse()) {
			equivalentNavigationProperty = objectPropertyNodeShape.getPath().getInverseNavigationProperty();//.getNavigationPropertyName();
		} else {
			equivalentNavigationProperty = objectPropertyNodeShape.getPath();//.getNavigationPropertyName();
		}

		RdfNodeShape propertyNodeShape = objectPropertyNodeShape.getPropertyNode();

		RdfComplexType complexType = nodeShape.getComplexType();
		RdfComplexProperty complexProperty = null;
		if (complexType != null) {
			complexProperty = complexType.getComplexProperties().values().stream().filter(
					p -> p.getComplexPropertyName().equals(equivalentNavigationProperty.getNavigationPropertyName()))
					.findAny().orElse(null); //rdfToOdata(propertyNodeShape.getNodeShapeName()))).findAny().orElse(null);
		}
		if (complexProperty == null) { //propertyNodeShape.getObjectPropertyShapes()
			complexProperty = new RdfComplexProperty();

			complexProperty.setComplexPropertyName(equivalentNavigationProperty.getNavigationPropertyName());//rdfToOdata(propertyNodeShape.getNodeShapeName()));
			complexProperty.setComplexPropertyLabel(equivalentNavigationProperty.getNavigationPropertyLabel());//propertyNodeShape.getNodeShapeLabel());
			complexProperty.setRdfObjectPropertyShape(objectPropertyNodeShape);
			complexProperty.setComplexType(propertyNodeShape.getComplexType());
			complexType.addComplexProperty(complexProperty);
		}
		return complexProperty;
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
			property.setOfClass(clazz);
			clazz.properties.put(property.propertyName, property);
		}
		return property;
	}

	RdfProperty getOrCreateFKProperty(RdfNavigationProperty rdfNavigationProperty) throws OData2SparqlException {
		RdfNode propertyNode = rdfNavigationProperty.getNavigationPropertyNode();
		RdfNode domainNode = rdfNavigationProperty.getDomainNode();
		RdfEntityType clazz = this.getOrCreateEntityType(domainNode);
		RdfProperty property = getOrCreateProperty(propertyNode, null, domainNode);
		property.propertyName = property.propertyName + "Id";
		property.setFkProperty(rdfNavigationProperty);
		rdfNavigationProperty.setFkProperty(property);
		clazz.properties.put(property.propertyName, property);
		return property;
	}

	public RdfComplexType getOrCreateComplexType(RdfNodeShape nodeShape) throws OData2SparqlException {
		//Used for regular complexTypes such as NodeShapes
		RdfComplexType complexType = Enumerable.create(nodeShape.getSchema().getComplexTypes())
				.firstOrNull(complexTypeNameEquals(rdfToOdata(nodeShape.getNodeShapeComplexTypeName())));
		if (complexType == null) {
			complexType = new RdfComplexType();
			complexType.complexTypeName = rdfToOdata(nodeShape.getNodeShapeComplexTypeName());
			complexType.complexTypeLabel = nodeShape.getNodeShapeLabel();
			complexType.setSchema(nodeShape.getSchema());
			complexType.setProvenanceType(false);
			nodeShape.getSchema().getComplexTypes().add(complexType);
			nodeShape.setComplexType(complexType);
		}
		return complexType;
	}

	public RdfComplexType getOrCreateComplexType(RdfNode complexTypeNode, RdfNode complexTypeLabelNode,
			RdfNode superdomainNode) throws OData2SparqlException {
		//Used for complex types that contain the provenance of properties
		RdfURI complexTypeURI = new RdfURI(complexTypeNode);
		RdfEntityType clazz = this.getOrCreateEntityType(superdomainNode);
		RdfComplexType complexType = Enumerable.create(clazz.getSchema().getComplexTypes())
				.firstOrNull(complexTypeNameEquals(rdfToOdata(complexTypeURI.localName)));
		if (complexType == null) {
			complexType = new RdfComplexType();
			complexType.setProvenanceType(true);
			complexType.complexTypeName = rdfToOdata(complexTypeURI.localName);
			if (complexTypeLabelNode == null) {
				complexType.complexTypeLabel = RdfConstants.PROPERTY_LABEL_PREFIX + complexType.complexTypeName;
			} else {
				complexType.complexTypeLabel = complexTypeLabelNode.getLiteralObject().toString();
			}
			complexType.complexTypeNode = complexTypeNode;
			//clazz.complexTypes.put(complexType.complexTypeName, complexType);
			clazz.getSchema().getComplexTypes().add(complexType);
		}
		complexType.domainClass = this.getOrCreateEntityType(superdomainNode);
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

	RdfNavigationProperty getOrCreateNavigationProperty(RdfNode propertyNode, RdfNode propertyLabelNode,
			RdfNode domainNode, RdfNode rangeNode, RdfNode multipleDomainNode, RdfNode multipleRangeNode,
			Cardinality domainCardinality, Cardinality rangeCardinality) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI domainURI = new RdfURI(domainNode);
		RdfURI rangeURI = new RdfURI(rangeNode);
		String navigationPropertyName = createNavigationPropertyName(multipleDomainNode, multipleRangeNode, domainURI,
				propertyURI, rangeURI);

		RdfNavigationProperty navigationProperty = Enumerable.create(domainURI.graph.navigationProperties)
				.firstOrNull(navigationPropertyNameEquals(navigationPropertyName));
		if (navigationProperty == null) {
			navigationProperty = buildNavigationProperty(navigationPropertyName, propertyNode, propertyURI, domainNode,
					domainURI, rangeNode, rangeURI);
		}
		if (propertyLabelNode == null) {
			if (navigationProperty.navigationPropertyLabel == null
					|| navigationProperty.navigationPropertyLabel == "") {
				navigationProperty.navigationPropertyLabel = RdfConstants.NAVIGATIONPROPERTY_LABEL_PREFIX
						+ navigationProperty.navigationPropertyName;
			}
		} else {
			navigationProperty.navigationPropertyLabel = propertyLabelNode.getLiteralObject().toString();
		}
		navigationProperty.setIsInverse(false);
		// #95 Only use the supplied cardinality if existing is undefined or MANY/MULTIPLE
		navigationProperty.setDomainCardinality(domainCardinality);
		navigationProperty.setRangeCardinality(rangeCardinality);
		return navigationProperty;
	}

	RdfNavigationProperty getOrCreateInverseNavigationProperty(RdfNode inversePropertyNode,
			RdfNode inversePropertyLabelNode, RdfNode propertyNode, RdfNode domainNode, RdfNode rangeNode,
			RdfNode multipleDomainNode, RdfNode multipleRangeNode, Cardinality domainCardinality,
			Cardinality rangeCardinality) throws OData2SparqlException {

		RdfNavigationProperty navigationProperty = getOrCreateNavigationProperty(propertyNode, null, domainNode,
				rangeNode, multipleDomainNode, multipleRangeNode, domainCardinality, rangeCardinality);
		RdfNavigationProperty inverseNavigationProperty = getOrCreateNavigationProperty(inversePropertyNode,
				inversePropertyLabelNode, rangeNode, domainNode, multipleRangeNode, multipleDomainNode,
				rangeCardinality, domainCardinality);
		inverseNavigationProperty.setIsInverse(true);
		inverseNavigationProperty.inversePropertyOf = propertyNode;
		//Added because inverse is symmetrical
		inverseNavigationProperty.setInverseNavigationProperty(navigationProperty);
		navigationProperty.setIsInverse(true);
		navigationProperty.setInverseNavigationProperty(inverseNavigationProperty);
		navigationProperty.inversePropertyOf = inversePropertyNode;
		return inverseNavigationProperty;
	}

	Boolean isNavigationProperty(RdfNode propertyNode, RdfNode domainNode, RdfNode rangeNode,
			RdfNode multipleDomainNode, RdfNode multipleRangeNode) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI domainURI = new RdfURI(domainNode);
		RdfURI rangeURI = new RdfURI(rangeNode);
		String navigationPropertyName = createNavigationPropertyName(multipleDomainNode, multipleRangeNode, domainURI,
				propertyURI, rangeURI);

		RdfNavigationProperty navigationProperty = Enumerable.create(domainURI.graph.navigationProperties)
				.firstOrNull(navigationPropertyNameEquals(navigationPropertyName));
		if (navigationProperty == null) {
			return false;
		}
		return true;
	}

	private String createNavigationPropertyName(RdfNode multipleDomainNode, RdfNode multipleRangeNode, RdfURI domainURI,
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

	private RdfNavigationProperty buildNavigationProperty(String navigationPropertyName, RdfNode propertyNode,
			RdfURI propertyURI, RdfNode domainNode, RdfURI domainURI, RdfNode rangeNode, RdfURI rangeURI)
			throws OData2SparqlException {
		RdfNavigationProperty navigationProperty = new RdfNavigationProperty();
		navigationProperty.navigationPropertyName = navigationPropertyName;
		navigationProperty.navigationPropertyNode = propertyNode;
		navigationProperty.navigationPropertySchema  = (new RdfURI(propertyNode)).graph;
		navigationProperty.rangeName = rdfToOdata(rangeURI.localName);
		navigationProperty.rangeNode = rangeNode;
		navigationProperty.domainName = rdfToOdata(domainURI.localName);
		navigationProperty.domainNode = domainNode;

		navigationProperty.domainClass = this.getOrCreateEntityType(domainNode);
		navigationProperty.rangeClass = this.getOrCreateEntityType(rangeNode);

		navigationProperty.domainClass.navigationProperties.put(navigationPropertyName, navigationProperty);
		navigationProperty.rangeClass.incomingNavigationProperties.put(navigationPropertyName, navigationProperty);
		domainURI.graph.navigationProperties.add(navigationProperty);
		return navigationProperty;
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
		if (nodeShapeLabelNode != null)
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
		//		nodeShape.setEntityType(getOrCreateNodeShapeEntityComplexType(nodeShape, nodeShapeNode, nodeShapeNameNode, nodeShapeLabelNode));
		return nodeShape;
	}

	RdfEntityType getOrCreateNodeShapeEntityComplexType(RdfNodeShape nodeShape)//, RdfNode nodeShapeNode, RdfNode nodeShapeNameNode,	RdfNode nodeShapeLabelNode) 
			throws OData2SparqlException {
		RdfEntityType nodeShapeEntityType = getOrCreateEntityType(nodeShape);
		nodeShapeEntityType.setOperation(false);
		nodeShapeEntityType.setBaseType(null);
		nodeShapeEntityType.setNodeShape(nodeShape);

		RdfComplexType nodeShapeComplexType = getOrCreateComplexType(nodeShape);

		RdfProperty nodeShapeProperty = getOrCreateComplexProperty(nodeShape, nodeShape);
		nodeShapeProperty.setIsComplex(true);
		nodeShapeProperty.setComplexType(nodeShapeComplexType);
		nodeShapeProperty.setCardinality(RdfConstants.Cardinality.ZERO_TO_ONE);
		nodeShapeProperty.setIsKey(false);
		nodeShapeEntityType.properties.put(nodeShapeProperty.getEDMPropertyName(), nodeShapeProperty);

		return nodeShapeEntityType;
	}

	public void getOrCreatePropertyShape(RdfNode nodeShapeNode, RdfNode propertyShapeNode,
			RdfNode propertyShapeLabelNode, RdfNode propertyShapeNameNode, RdfNode propertyShapeDescriptionNode,
			RdfNode pathNode, RdfNode inversePathNode, RdfNode propertyNode, RdfNode minCountNode, RdfNode maxCountNode)
			throws OData2SparqlException {
		String propertyShapeName;
		String propertyShapeDescription = null;
		String propertyShapeLabel = null;
		Integer minCount = 0;
		Integer maxCount = null;

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

		if (pathNode != null) {
			RdfNavigationProperty navigationProperty = nodeShape.getTargetClass().findNavigationProperty(pathNode);
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
			RdfNavigationProperty navigationProperty = nodeShape.getTargetClass()
					.findInverseNavigationProperty(inversePathNode.getLocalName());
			if (navigationProperty != null) {
				RdfObjectPropertyShape objectPropertyShape = new RdfObjectPropertyShape(nodeShape, propertyShapeNode);
				objectPropertyShape.setPath(navigationProperty);
				objectPropertyShape.setPropertyShapeDescription(propertyShapeDescription);
				objectPropertyShape.setPropertyShapeName(propertyShapeName);
				objectPropertyShape.setPropertyShapeLabel(propertyShapeLabel);
				objectPropertyShape.setMinCount(minCount);
				objectPropertyShape.setMaxCount(maxCount);
				objectPropertyShape.setInversePath(true);
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

	private static final Predicate1<RdfNavigationProperty> navigationPropertyNameEquals(
			final String navigationPropertyName) {
		return new Predicate1<RdfNavigationProperty>() {
			public boolean apply(RdfNavigationProperty navigationProperty) {
				return navigationProperty.navigationPropertyName.equals(navigationPropertyName);
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
			//RdfEntityType nodeShapeEntityComplexType = getOrCreateNodeShapeEntityComplexType(nodeShape);
			nodeShape.setEntityType(getOrCreateNodeShapeEntityComplexType(nodeShape));
			for (RdfDataPropertyShape dataPropertyShape : nodeShape.getDataPropertyShapes().values()) {
				nodeShape.getComplexType().addProperty(dataPropertyShape.path);
			}
			for (RdfObjectPropertyShape objectPropertyShape : nodeShape.getObjectPropertyShapes().values()) {
				allocateObjectPropertyShape(nodeShape, objectPropertyShape);
			}
		}
		//second pass to add 'inherited' properties from baseNodeShape
		inheritBaseNodeProperties();

	}

	private void allocateObjectPropertyShape(RdfNodeShape nodeShape, RdfObjectPropertyShape objectPropertyShape)
			throws OData2SparqlException {
		Cardinality cardinality = deduceObjectPropertyShapeCardinality(objectPropertyShape);

		if (objectPropertyShape.getPropertyNode() != null) {

			RdfComplexProperty complexProperty = getOrCreateComplexProperty(nodeShape, objectPropertyShape); //objectPropertyShape.getPath().getInverseNavigationProperty().getNavigationPropertyName()
			complexProperty.setCardinality(cardinality);
			nodeShape.getComplexType().addComplexProperty(complexProperty);
		} else {
			RdfShapedNavigationProperty shapedNavigationProperty;
			if (objectPropertyShape.isInversePath()) {
				shapedNavigationProperty = new RdfShapedNavigationProperty(
						objectPropertyShape.getPath().getInverseNavigationProperty(),
						objectPropertyShape.getPropertyShapeName(), objectPropertyShape.getPropertyShapeDescription(),
						cardinality);
			} else {
				shapedNavigationProperty = new RdfShapedNavigationProperty(objectPropertyShape.getPath(),
						objectPropertyShape.getPropertyShapeName(), objectPropertyShape.getPropertyShapeDescription(),
						cardinality);
			}
			nodeShape.getComplexType().addShapedNavigationProperty(shapedNavigationProperty);
		}
	}

	private Cardinality deduceObjectPropertyShapeCardinality(RdfObjectPropertyShape objectPropertyShape) {
		if (objectPropertyShape.getMaxCount() == null || objectPropertyShape.getMaxCount() > 1) {
			return (RdfConstants.Cardinality.MANY);
		} else if (objectPropertyShape.getMaxCount() == 1 && objectPropertyShape.getMinCount() == 1) {
			return (RdfConstants.Cardinality.ONE);
		} else if (objectPropertyShape.getMaxCount() == 1 && objectPropertyShape.getMinCount() == 0) {
			return (RdfConstants.Cardinality.ZERO_TO_ONE);
		} else {
			return (RdfConstants.Cardinality.MANY);
		}
	}

	private void inheritBaseNodeProperties() throws OData2SparqlException {
		for (RdfNodeShape nodeShape : pendingNodeShapes.values()) {
			if (nodeShape.getBaseNodeShape() != null) {
				inheritProperties(nodeShape, nodeShape.getBaseNodeShape());
			}
		}
	}

	private void inheritProperties(RdfNodeShape nodeShape, RdfNodeShape baseNodeShape) throws OData2SparqlException {
		for (RdfDataPropertyShape dataPropertyShape : nodeShape.getBaseNodeShape().getDataPropertyShapes().values()) {
			nodeShape.getComplexType().addProperty(dataPropertyShape.path);
		}
		for (RdfObjectPropertyShape objectPropertyShape : nodeShape.getBaseNodeShape().getObjectPropertyShapes()
				.values()) {
			allocateObjectPropertyShape(nodeShape, objectPropertyShape);
		}
		if (baseNodeShape.getBaseNodeShape() != null) {
			inheritProperties(nodeShape, baseNodeShape.getBaseNodeShape());
		}
	}

}