package com.inova8.odata2sparql.RdfModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		private final Map<String, String> prefixToURI = new HashMap<String, String>();
		private final Map<String, String> URItoPrefix = new HashMap<String, String>();

		
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
				sparqlPrefixes.append("PREFIX ").append(prefix).append(": <").append(url).append("> ");
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
			String expandedEntityKey  = expandPrefix(decodedEntityKey);
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
					this.set(sprefix, uri);
					return sprefix;
				}
				i++;
			}
		}

		public String toQName(RdfNode node,String qNameSeparator) {
			String qname = null;
			if (node.isBlank()) {
				return ((BNode) node.getNode()).toString();
			} else {
				try {
					qname = rdfPrefixes.getOrCreatePrefix(null, node.getNamespace()) + qNameSeparator + node.getLocalName();
				} catch (OData2SparqlException e) {
					log.error("RdfNode toQName failure. Node:" + node.toString() + " with exception " + e.toString());
				}
			}
			return qname;
		}
	}

	public static class RdfSchema {
		private String schemaName;
		private String schemaPrefix;
		boolean isDefault = false;
		public RdfSchema() {
			super();
			initialiseDatatypes();
		}
		private void initialiseDatatypes() {		
			for (String datatype:  RdfConstants.RDF_DATATYPES){
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

		public final List<RdfModel.RdfEntityType> classes = new ArrayList<RdfModel.RdfEntityType>();
		public final List<RdfModel.RdfAssociation> associations = new ArrayList<RdfModel.RdfAssociation>();
		private final List<RdfModel.RdfDatatype> datatypes = new ArrayList<RdfModel.RdfDatatype>();
	}

	public static class RdfEntityType {
		public String entityTypeName;
		private String entityTypeLabel;

		private RdfSchema schema;
		private RdfNode entityTypeNode;
		private RdfEntityType baseType;
		boolean rootClass = false;
		boolean isOperation = false;
		private boolean isEntity = false;
		private boolean functionImport = false;
		private String description;
		private Set<RdfEntityType> subTypes = new HashSet<RdfEntityType>();
		private String deleteText;
		private String insertText;
		private String updateText;
		private String updatePropertyText;
		
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
		protected void addSubType(RdfEntityType subType){
			subTypes.add(subType);
		}
		public Set<RdfEntityType> getSubTypes(){
			return subTypes;
		}
		public Set<RdfEntityType> getAllSubTypes(){
			Set<RdfEntityType> allSubTypes = new HashSet<RdfEntityType>();
			allSubTypes.addAll(subTypes);
			for(RdfEntityType subType: subTypes){
				allSubTypes.addAll(subType.getAllSubTypes());
			}
			return allSubTypes;
		}
		public void setBaseType(RdfEntityType baseType) {
			this.baseType = baseType;
			if(baseType != null) baseType.addSubType(this);
		}

		public RdfSchema getSchema() {
			return schema;
		}

		public void setSchema(RdfSchema schema) {
			this.schema = schema;
		}

		public String getEntityTypeLabel() {
			return entityTypeLabel;
		}

		public void setEntityTypeLabel(String entityTypeLabel) {
			this.entityTypeLabel = entityTypeLabel;
		}

		public String getDescription() {
			if (this.description == null || this.description.isEmpty()) {
				return entityTypeLabel;
			} else {
				return description;
			}
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public boolean isFunctionImport() {
			return functionImport;
		}


		private final HashMap<String, FunctionImportParameter> functionImportParameters = new HashMap<String, FunctionImportParameter>();

		public HashMap<String, FunctionImportParameter> getFunctionImportParameters() {
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

		public String queryText;

		public String getIRI() {
			return entityTypeNode.getIRI().toString();
		}

		private final HashMap<String, RdfModel.RdfProperty> properties = new HashMap<String, RdfModel.RdfProperty>();
		private final HashMap<String, RdfModel.RdfAssociation> navigationProperties = new HashMap<String, RdfModel.RdfAssociation>();

		public Collection<RdfModel.RdfAssociation> getNavigationProperties() {
			return navigationProperties.values();
		}
		public Collection<RdfModel.RdfAssociation> getInheritedNavigationProperties() {
			 Collection<RdfModel.RdfAssociation> inheritedNavigationProperties= new ArrayList<RdfModel.RdfAssociation>();
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
		private final HashMap<String, RdfModel.RdfAssociation> incomingAssociations = new HashMap<String, RdfModel.RdfAssociation>();

		final HashMap<String, RdfModel.RdfPrimaryKey> primaryKeys = new HashMap<String, RdfModel.RdfPrimaryKey>();

		public RdfAssociation findNavigationProperty(String navigationPropertyName) {
			//TODO do we not want to find inherited properties as well?
			return navigationProperties.get(navigationPropertyName);
		}
		public RdfAssociation findNavigationProperty(RdfNode navigationPropertyNode) {
			for (RdfAssociation navigationProperty : this.getInheritedNavigationProperties() ){
				if( navigationProperty.associationNode.getIRI().toString().equals(navigationPropertyNode.getIRI().toString())) return navigationProperty;			
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
			Collection<RdfPrimaryKey> primaryKeyValues = new ArrayList<RdfPrimaryKey>();;
			RdfEntityType currentEntityType = this;
			do{
				primaryKeyValues.addAll(currentEntityType.primaryKeys.values());
				currentEntityType=currentEntityType.getBaseType();
				if(withBaseType ) currentEntityType=null;
			}while(currentEntityType!=null);
			return primaryKeyValues;
		}
		public void setFunctionImport(boolean b) {
			this.functionImport = true;
		}
	}
	public class FunctionImportParameter{
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
		public RdfNode propertyNode;
		private Boolean isKey = false;
		private RdfConstants.Cardinality cardinality = RdfConstants.Cardinality.MANY;
		private String equivalentProperty;

		RdfEntityType ofClass;
		private String description;

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
			this.propertyLabel = propertyLabel;
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

			this.description = description;
		}

		public String getEDMPropertyName() {
			return this.propertyName;
		}

		public String getPropertyURI() {
			return propertyNode.getIRI().toString();
		}

		public String getPropertyTypeName() {
			return propertyTypeName;
		}

		public Boolean getIsKey() {
			return isKey;
		}

		public void setIsKey(Boolean isKey) {
			this.isKey = isKey;
		}
		public String getVarName() {
			return varName;
		}

		public void setVarName(String varName) {
			this.varName = varName;
		}
	}

	public static class RdfAssociation {
		private String associationName;
		private String associationLabel;
		private String varName;
		private String relatedKey;
		private RdfNode domainNode;
		private String domainName;
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
			this.associationLabel = associationLabel;
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

		public RdfEntityType domainClass;

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
			this.rangeCardinality = rangeCardinality;
		}

		public Cardinality getDomainCardinality() {
			return domainCardinality;
		}

		public void setDomainCardinality(Cardinality domainCardinality) {
			this.domainCardinality = domainCardinality;
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
	}

	private class RdfURI {
		@SuppressWarnings("unused")
		@Deprecated
		public RdfNode node;
		public String localName;
		private String graphName;
		private String graphPrefix;
		private RdfSchema graph;

		RdfURI(RdfNode node) throws OData2SparqlException {
			this.node = node;
			String[] parts = rdfPrefixes.toQName(node,RdfConstants.QNAME_SEPARATOR).split(RdfConstants.QNAME_SEPARATOR); //node.toQName(rdfPrefixes).split(":");
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
	}

	public RdfEntityType getOrCreateEntityType(RdfNode classNode) throws OData2SparqlException {
		return getOrCreateEntityType(classNode, null);
	}

	RdfEntityType getOrCreateEntityType(RdfNode entityTypeNode, RdfNode entityTypeLabelNode)
			throws OData2SparqlException {

		RdfURI classURI = new RdfURI(entityTypeNode);
		RdfEntityType clazz = Enumerable.create(classURI.graph.classes)
				.firstOrNull(classNameEquals(classURI.localName));
		if (clazz == null) {
			clazz = new RdfEntityType();
			clazz.schema = classURI.graph;
			clazz.entityTypeName = rdfToOdata(classURI.localName);
			if (entityTypeLabelNode == null) {
				clazz.entityTypeLabel = RdfConstants.CLASS_LABEL_PREFIX + clazz.entityTypeName;
			} else {
				clazz.entityTypeLabel = entityTypeLabelNode.getLiteralObject().toString();
			}
			clazz.entityTypeNode = entityTypeNode;
			classURI.graph.classes.add(clazz);
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
		RdfDatatype datatype = Enumerable.create(datatypeURI.graph.datatypes).firstOrNull(
				datatypeNameEquals(datatypeURI.localName));
		if (datatype == null) {
			datatype = new RdfDatatype(rdfToOdata(datatypeURI.localName));
			//datatype.datatypeName = rdfToOdata(datatypeURI.localName);
		}
		return datatype;
	}
	RdfEntityType getOrCreateOperationEntityType(RdfNode queryNode) throws OData2SparqlException
	{
		return getOrCreateOperationEntityType(queryNode, null,null,null,null,null,null);
	}
	RdfEntityType getOrCreateOperationEntityType(RdfNode queryNode, RdfNode queryLabel, RdfNode queryText,RdfNode deleteText,RdfNode insertText,RdfNode updateText,RdfNode updatePropertyText)
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

			FunctionImportParameter functionImportParameter = new FunctionImportParameter(varName.getLiteralValue().getLabel(), propertyTypeName, false);
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
			RdfProperty property = Enumerable.create(operationEntityType.getProperties()).firstOrNull(
					propertyNameEquals(propertyURI.localName));
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
			//String associationName = rdfToOdata(operationURI.localName) + RdfConstants.PREDICATE_SEPARATOR	+ rdfToOdata(propertyURI.localName);
			String associationName = rdfToOdata(propertyURI.localName);
			//String associationName createAssociationName(null, null, queryNode, propertyURI,	rangeURI);
			RdfAssociation association = Enumerable.create(propertyURI.graph.associations).firstOrNull(
					associationNameEquals(associationName));
			if (association == null) {
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
			
			if(association.IsInverse()){  // is it the inverse of something?
				RdfAssociation inverseAssociation = association.getInverseAssociation();
				inverseAssociation.rangeCardinality = RdfConstants.Cardinality.ONE;
				inverseAssociation.domainCardinality = RdfConstants.Cardinality.MANY;				
				
			}else{
				association.setIsInverse(false);	
			}
	
			association.rangeCardinality = RdfConstants.Cardinality.MANY;
			association.domainCardinality = RdfConstants.Cardinality.ONE;
			
			//Since this is not a primary entity we need to add the keys of the navigation properties are properties, as well as adding them as primarykeys.
			RdfProperty property = getOrCreateOperationProperty(queryNode, propertyNode, propertyLabelNode, rangeNode,
					varName);
			property.isKey = true;
			operationEntityType.primaryKeys.put(property.propertyName, new RdfPrimaryKey(property.propertyName,
					property.propertyName));
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

		RdfProperty property = Enumerable.create(clazz.getProperties()).firstOrNull(
				propertyNameEquals(propertyURI.localName));
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

		RdfProperty property = Enumerable.create(clazz.getProperties()).firstOrNull(
				propertyNameEquals(propertyURI.localName));
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

	void setPropertyRange(RdfNode propertyNode, HashSet<RdfEntityType> classes, RdfNode rangeNode)
			throws OData2SparqlException {
		RdfURI propertyURI = new RdfURI(propertyNode);
		for (RdfEntityType clazz : classes) {
			RdfProperty property = Enumerable.create(clazz.getProperties()).firstOrNull(
					propertyNameEquals(propertyURI.localName));
			String propertyTypeName = rangeNode.getIRI().toString();
			property.propertyTypeName = propertyTypeName;
		}
	}

	void setPropertyCardinality(RdfNode propertyNode, HashSet<RdfEntityType> classes, Cardinality cardinality)
			throws OData2SparqlException {
		RdfURI propertyURI = new RdfURI(propertyNode);
		for (RdfEntityType clazz : classes) {
			RdfProperty property = Enumerable.create(clazz.getProperties()).firstOrNull(
					propertyNameEquals(propertyURI.localName));
			property.cardinality = cardinality;
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

		// TODO Incorrectly assigning to property graph instead of domain graph
		//		RdfAssociation association = Enumerable.create(propertyURI.graph.associations).firstOrNull(
		//				associationNameEquals(associationName));
		RdfAssociation association = Enumerable.create(domainURI.graph.associations).firstOrNull(
				associationNameEquals(associationName));
		if (association == null) {
			association = buildAssociation(associationName, propertyNode, propertyURI, domainNode, domainURI,
					rangeNode, rangeURI);
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
		association.domainCardinality = domainCardinality;
		association.rangeCardinality = rangeCardinality;

		return association;
	}
	RdfAssociation getOrCreateInverseAssociation(RdfNode inversePropertyNode, RdfNode inversePropertyLabelNode,
			RdfNode propertyNode, RdfNode domainNode, RdfNode rangeNode, RdfNode multipleDomainNode,
			RdfNode multipleRangeNode, Cardinality domainCardinality, Cardinality rangeCardinality)
			throws OData2SparqlException {

		RdfAssociation association = getOrCreateAssociation(propertyNode, null, domainNode, rangeNode,
				multipleDomainNode, multipleRangeNode, domainCardinality, rangeCardinality);
		RdfAssociation inverseAssociation = getOrCreateAssociation(inversePropertyNode, inversePropertyLabelNode,
				domainNode, rangeNode, multipleDomainNode, multipleRangeNode, rangeCardinality, domainCardinality); // Note cardinality only is reversed
		inverseAssociation.setIsInverse(true);
		inverseAssociation.inversePropertyOf = propertyNode;
		//Added because inverse is symmetrical
		inverseAssociation.setInverseAssociation(association);
		association.setIsInverse(true);
		association.setInverseAssociation(inverseAssociation);
		association.inversePropertyOf = inversePropertyNode;
		return inverseAssociation;
	}
	private String createAssociationName(RdfNode multipleDomainNode, RdfNode multipleRangeNode, RdfURI domainURI,
			RdfURI propertyURI, RdfURI rangeURI) throws OData2SparqlException {
		if (!(multipleDomainNode.getLiteralObject().equals(1) || multipleDomainNode.getLiteralObject().equals("1"))) {
			if (!(multipleRangeNode.getLiteralObject().equals(1) || multipleRangeNode.getLiteralObject().equals("1"))) {
				return rdfToOdata(domainURI.localName) + RdfConstants.PREDICATE_SEPARATOR
						+ rdfToOdata(propertyURI.localName) + RdfConstants.PREDICATE_SEPARATOR
						+ rdfToOdata(rangeURI.localName);
			} else {
				return rdfToOdata(domainURI.localName) + RdfConstants.PREDICATE_SEPARATOR
						+ rdfToOdata(propertyURI.localName);
			}
		} else if (!(multipleRangeNode.getLiteralObject().equals(1) || multipleRangeNode.getLiteralObject().equals("1"))) {
			return rdfToOdata(propertyURI.localName) + RdfConstants.PREDICATE_SEPARATOR
					+ rdfToOdata(rangeURI.localName);
		}
		return rdfToOdata(propertyURI.localName);
	}



	private RdfAssociation buildAssociation(String associationName, RdfNode propertyNode, RdfURI propertyURI,
			RdfNode domainNode, RdfURI domainURI, RdfNode rangeNode, RdfURI rangeURI) throws OData2SparqlException {
		RdfAssociation association = new RdfAssociation();
		association.associationName = associationName;
		association.associationNode = propertyNode;
		association.rangeName = rangeURI.localName;
		association.rangeNode = rangeNode;
		association.domainName = domainURI.localName;
		association.domainNode = domainNode;

		association.domainClass = this.getOrCreateEntityType(domainNode);
		association.rangeClass = this.getOrCreateEntityType(rangeNode);

		association.domainClass.navigationProperties.put(associationName, association);
		association.rangeClass.incomingAssociations.put(associationName, association);
		domainURI.graph.associations.add(association);
		// TODO change to use domain of navigation property
		// propertyURI.graph.associations.add(association);
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
		return rdfName.replace("-", "_").replace("/", "_");
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

}