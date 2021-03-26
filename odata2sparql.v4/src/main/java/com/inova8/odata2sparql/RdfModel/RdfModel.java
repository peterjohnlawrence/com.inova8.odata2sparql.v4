/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfModel;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import static org.eclipse.rdf4j.model.util.Values.iri;

import org.core4j.Enumerable;
import org.core4j.Predicate1;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;

import com.inova8.odata2sparql.Constants.PathQLConstants;
import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.RdfConstants.Cardinality;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfEdmType;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;
import com.inova8.odata2sparql.uri.UriUtils;

/**
 * The Class RdfModel.
 */
public class RdfModel {
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfModel.class);

	/**
	 * Key.
	 *
	 * @param clazz the clazz
	 * @return the string
	 */
	static String KEY(RdfEntityType clazz) {
		return RdfConstants.SUBJECT;
	};

	/** The graphs. */
	public final List<RdfModel.RdfSchema> graphs = new ArrayList<RdfModel.RdfSchema>();
	
	/** The pending node shapes. */
	private TreeMap<String, RdfModel.RdfNodeShape> pendingNodeShapes = new TreeMap<String, RdfModel.RdfNodeShape>();

	/** The rdf prefixes. */
	private final RdfPrefixes rdfPrefixes = new RdfPrefixes();
	
	/** The rdf repository. */
	protected final RdfRepository rdfRepository;
	
	/** The proxies. */
	private TreeSet<String> proxies = new TreeSet<String>();

	/**
	 * Instantiates a new rdf model.
	 *
	 * @param rdfRepository the rdf repository
	 */
	public RdfModel(RdfRepository rdfRepository) {

		rdfPrefixes.setStandardNsPrefixes();
		this.rdfRepository = rdfRepository;
	}

	/**
	 * The Class RdfPrefixes.
	 */
	public class RdfPrefixes {
		
		/** The prefix to IRI. */
		private final HashMap<String, IRI> prefixToIRI = new HashMap<String, IRI>();
		
		/** The prefix to URI. */
		private final Map<String, String> prefixToURI = new TreeMap<String, String>();
		
		/** The UR ito prefix. */
		private final Map<String, String> URItoPrefix = new TreeMap<String, String>();

		/**
		 * Sets the standard ns prefixes.
		 */
		private void setStandardNsPrefixes() {
			set(RdfConstants.INOVA8, RdfConstants.INOVA8_NS);
			set(RdfConstants.RDF, RdfConstants.RDF_NS);
			set(RdfConstants.RDFS, RdfConstants.RDFS_NS);
			set(RdfConstants.DC, RdfConstants.DC_NS);
			set(RdfConstants.OWL, RdfConstants.OWL_NS);
			set(RdfConstants.XSD, RdfConstants.XSD_NS);
			set(RdfConstants.URN, RdfConstants.URN_NS);
		}

		/**
		 * Log.
		 */
		void log() {
			log.info("Deduced prefixes: " + prefixToURI.toString());
		}

		/**
		 * Gets the prefixes.
		 *
		 * @return the prefixes
		 */
		public TreeMap<String, String> getPrefixes() {
			return (TreeMap<String, String>) prefixToURI;
		}
		
		/**
		 * Gets the IRI prefixes.
		 *
		 * @return the IRI prefixes
		 */
		public HashMap<String, IRI> getIRIPrefixes() {
			return  prefixToIRI;
		}
		
		/**
		 * Sparql prefixes.
		 *
		 * @return the string builder
		 */
		public StringBuilder sparqlPrefixes() {
			StringBuilder sparqlPrefixes = new StringBuilder();
			for (Map.Entry<String, String> prefixEntry : prefixToURI.entrySet()) {
				String prefix = prefixEntry.getKey();
				String url = prefixEntry.getValue();
				sparqlPrefixes.append("PREFIX ").append(prefix).append(": <").append(url).append(">\n");
			}
			return sparqlPrefixes;
		}

		/**
		 * Sets the ns prefix.
		 *
		 * @param graphPrefix the graph prefix
		 * @param graphName the graph name
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		private void setNsPrefix(String graphPrefix, String graphName) throws OData2SparqlException {

			checkLegal(graphPrefix);
			if (graphName == null)
				throw new NullPointerException("null URIs are prohibited as arguments to setNsPrefix");
			set(graphPrefix, graphName);

		}

		/**
		 * Gets the ns URI prefix.
		 *
		 * @param schemaName the schema name
		 * @return the ns URI prefix
		 */
		private String getNsURIPrefix(String schemaName) {
			return URItoPrefix.get(schemaName);
		}

		/**
		 * Gets the ns prefix URI.
		 *
		 * @param sprefix the sprefix
		 * @return the ns prefix URI
		 */
		private String getNsPrefixURI(String sprefix) {
			return prefixToURI.get(sprefix);
		}

		/**
		 * Expand prefix.
		 *
		 * @param decodedEntityKey the decoded entity key
		 * @return the string
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		public String expandPrefix(String decodedEntityKey) throws OData2SparqlException {
			String encodeEntityKey = UriUtils.encodeQName(decodedEntityKey);
			int colon = encodeEntityKey.indexOf(RdfConstants.QNAME_SEPARATOR);
			if (colon < 0)
				return decodedEntityKey;
			else {
				String uri = get(encodeEntityKey.substring(0, colon));
				if (uri == null) {
					//Try and see if initial part of entityKey is prefix to which /ext has been added
					String prefix = encodeEntityKey.substring(0, colon);
					int priorSeparator = prefix.indexOf(RdfConstants.PREFIXSEPARATOR);
					if (priorSeparator < 0) {

					} else {
						uri = get(prefix.substring(0, priorSeparator));
						if (uri != null) {
							uri = uri + prefix.substring(priorSeparator + 1).replace(RdfConstants.PREFIXSEPARATOR, "/")
									+ "/";
						}
					}
				}
				try {
					return uri == null ? encodeEntityKey
							: uri + UriUtils.encodeUri(decodedEntityKey.substring(colon + 1));
				} catch (UnsupportedEncodingException e) {
					throw new OData2SparqlException("Unencodable key: " + decodedEntityKey, null);
				}
			}
		}

		/**
		 * Expand predicate.
		 *
		 * @param entityKey the entity key
		 * @return the string
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		public String expandPredicate(String entityKey) throws OData2SparqlException {
			UrlValidator urlValidator = new UrlValidator();
			String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey);
			String expandedEntityKey = expandPrefix(decodedEntityKey);//decodedEntityKey
			if (urlValidator.isValid(expandedEntityKey)) {
				return expandedEntityKey;
			} else {
				throw new OData2SparqlException("Invalid key: " + entityKey, null);
			}
		}

		/**
		 * Expand predicate key.
		 *
		 * @param predicateKey the predicate key
		 * @return the string
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		public String expandPredicateKey(String predicateKey) throws OData2SparqlException {

			if (predicateKey.endsWith("'")) {
				String entityKey = predicateKey.substring(1, predicateKey.length() - 1);
				return expandPredicate(entityKey);
			} else {
				return expandPredicate(predicateKey);
			}
		}

		/**
		 * Check legal.
		 *
		 * @param prefix the prefix
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		private void checkLegal(String prefix) throws OData2SparqlException {
			if (prefix.length() > 0 && !(Pattern.matches("[a-zA-Z_][a-zA-Z0-9_.]*",prefix)))
				throw new OData2SparqlException("RdfPrefixes checkLegal failure");
		}

		/**
		 * Gets the.
		 *
		 * @param prefix the prefix
		 * @return the string
		 */
		public String get(String prefix) {
			return prefixToURI.get(prefix);
		}

		/**
		 * Sets the.
		 *
		 * @param prefix the prefix
		 * @param uri the uri
		 */
		private void set(String prefix, String uri) {
			prefixToIRI.put(prefix, iri(uri));
			prefixToURI.put(prefix, uri);
			URItoPrefix.put(uri, prefix);
		}

		/**
		 * Gets the or create prefix.
		 *
		 * @param prefix the prefix
		 * @param uri the uri
		 * @return the or create prefix
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		public String getOrCreatePrefix(String prefix, String uri) throws OData2SparqlException {
			if (prefix == null || prefix.equals("")) {
				if (!uri.substring(uri.length() - 1).equals("#")) {
					String sprefix = this.getNsURIPrefix(uri);
					if (sprefix == null || sprefix.equals("")) {
						return extendExistingPrefix(uri);
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

		/**
		 * Extend existing prefix.
		 *
		 * @param uri the uri
		 * @return the string
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		protected String extendExistingPrefix(String uri) throws OData2SparqlException {
			String sprefix;
			//If the URI is of the form:    http://rootpath/xxxx/yyyyyy
			//where http://rootpath/ is already a prefix 'root'
			//then we should create a prefix with name rootprefix_xxxx

			String[] parts = uri.split("/");
			if (parts.length > 3) {
				for (int j = 1; j < parts.length - 2; j++) {
					String rootUri = "";
					for (int i = 0; i < parts.length - j; i++) {
						rootUri += parts[i] + "/";
					}
					sprefix = this.getNsURIPrefix(rootUri);
					if (sprefix != null) {
						String sNewPrefix = sprefix;
						for (int k = j; k >= 1; k--) {
							sNewPrefix += RdfConstants.PREFIXSEPARATOR + parts[parts.length - k];
						}
						this.setNsPrefix(sNewPrefix, uri);
						log.info("New prefix added to " + RdfModel.this.rdfRepository.getModelName() + ':' + sNewPrefix
								+ "~ for URI " + uri);
						return sNewPrefix;
					}
				}
				//String rootUri = "";
				//for (int i = 0; i < parts.length - 1; i++) {
				//	rootUri += parts[i] + "/";
				//}
				//sprefix = this.getNsURIPrefix(rootUri);
				//if (sprefix == null || sprefix.equals("")) {
				return generateNextPrefix(uri);
				//} else {
				//	String sNewPrefix = sprefix + RdfConstants.PREFIXSEPARATOR + parts[parts.length - 1];
				//	this.setNsPrefix(sNewPrefix, uri);
				//	return sNewPrefix;
				//}
			} else {
				//Create a new prefix of the form jn
				return generateNextPrefix(uri);
			}
		}

		/**
		 * Generate next prefix.
		 *
		 * @param uri the uri
		 * @return the string
		 */
		private String generateNextPrefix(String uri) {
			int i = 0;
			String sprefix = "";
			while (true) {
				sprefix = RdfConstants.PREFIX + i;
				if (this.getNsPrefixURI(sprefix) == null) {
					log.info("New prefix added to " + RdfModel.this.rdfRepository.getModelName() + ':' + sprefix
							+ "~ for URI " + uri);
					this.set(sprefix, uri);
					return sprefix;
				}
				i++;
			}
		}

		/**
		 * To Q name.
		 *
		 * @param node the node
		 * @param qNameSeparator the q name separator
		 * @return the string
		 */
		public String toQName(RdfNode node, String qNameSeparator) {
			String qname = null;
			if (node.isBlank()) {
				return (((BNode) node.getNode()).toString()).replace(RdfConstants.QNAME_SEPARATOR_RDF,
						RdfConstants.QNAME_SEPARATOR_ENCODED);
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

		/**
		 * To Q name.
		 *
		 * @param uri the uri
		 * @param qNameSeparator the q name separator
		 * @return the string
		 */
		public String toQName(String uri, String qNameSeparator) {
			String qname = null;
			try {
				URI URI = new URI(uri);
				String path = URI.getPath();
				String lname ;
				if(URI.getFragment()!=null ) {
					lname = URI.getFragment();
				}else{
					int lastSlash = path.lastIndexOf("/")+1;
					lname	= path.substring(lastSlash, path.length());
				//	lname	= path.substring(1, path.length());
				}
				qname = rdfPrefixes.getOrCreatePrefix(null, uri.substring(0, uri.length() - lname.length()))
						+ qNameSeparator + lname;
			} catch (OData2SparqlException | URISyntaxException e) {
				log.error("RdfNode toQName failure. Node:" + uri + " with exception " + e.toString());
			}
			return qname;
		}
	}

	/**
	 * The Class RdfSchema.
	 */
	public static class RdfSchema {
		
		/** The schema name. */
		private String schemaName;
		
		/** The schema prefix. */
		private String schemaPrefix;
		
		/** The is default. */
		public boolean isDefault = false;
		
		/**
		 * Checks if is default.
		 *
		 * @return true, if is default
		 */
		public boolean isDefault() {
			return isDefault;
		}

		/** The classes. */
		private final List<RdfModel.RdfEntityType> classes = new ArrayList<RdfModel.RdfEntityType>();
		
		/** The navigation properties. */
		private final List<RdfModel.RdfNavigationProperty> navigationProperties = new ArrayList<RdfModel.RdfNavigationProperty>();
		
		/** The datatypes. */
		private final List<RdfModel.RdfDatatype> datatypes = new ArrayList<RdfModel.RdfDatatype>();

		/** The complex types. */
		private final TreeSet<RdfModel.RdfComplexType> complexTypes = new TreeSet<RdfModel.RdfComplexType>();
		
		/** The node shapes. */
		private final TreeSet<RdfModel.RdfNodeShape> nodeShapes = new TreeSet<RdfModel.RdfNodeShape>();

		/**
		 * Instantiates a new rdf schema.
		 *
		 * @param graphName the graph name
		 * @param graphPrefix the graph prefix
		 */
		public RdfSchema( String graphName, String graphPrefix) {
		//	super();
			initialiseDatatypes();

			schemaName=graphName;
			schemaPrefix=graphPrefix;
		}

		/**
		 * Initialise datatypes.
		 */
		private void initialiseDatatypes() {
		}

		/**
		 * Gets the datatypes.
		 *
		 * @return the datatypes
		 */
		public List<RdfModel.RdfDatatype> getDatatypes() {
			return datatypes;
		}

		/**
		 * Gets the schema name.
		 *
		 * @return the schema name
		 */
		public String getSchemaName() {
			return schemaName;
		}

		/**
		 * Sets the schema name.
		 *
		 * @param schemaName the new schema name
		 */
		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}

		/**
		 * Gets the schema prefix.
		 *
		 * @return the schema prefix
		 */
		public String getSchemaPrefix() {
			return schemaPrefix;
		}

		/**
		 * Sets the schema prefix.
		 *
		 * @param schemaPrefix the new schema prefix
		 */
		public void setSchemaPrefix(String schemaPrefix) {
			this.schemaPrefix = schemaPrefix;
		}

		/**
		 * Gets the complex types.
		 *
		 * @return the complex types
		 */
		public TreeSet<RdfModel.RdfComplexType> getComplexTypes() {
			return complexTypes;
		}

		/**
		 * Gets the node shapes.
		 *
		 * @return the node shapes
		 */
		public TreeSet<RdfModel.RdfNodeShape> getNodeShapes() {
			return nodeShapes;
		}

		/**
		 * Gets the classes.
		 *
		 * @return the classes
		 */
		public List<RdfModel.RdfEntityType> getClasses() {
			return classes;
		}

		/**
		 * Gets the navigation properties.
		 *
		 * @return the navigation properties
		 */
		public List<RdfModel.RdfNavigationProperty> getNavigationProperties() {
			return navigationProperties;
		}
	}

	/**
	 * The Class RdfEntityType.
	 */
	public static class RdfEntityType implements Comparable<RdfEntityType> {

		/** The entity type name. */
		public String entityTypeName;
		
		/** The entity type label. */
		private String entityTypeLabel;
		
		/** The entity set label. */
		private String entitySetLabel;
		
		/** The schema. */
		private RdfSchema schema;
		
		/** The entity type node. */
		private RdfNode entityTypeNode;
		
		/** The base type. */
		private RdfEntityType baseType;
		
		/** The node shape. */
		private RdfNodeShape nodeShape;
		
		/** The super types. */
		private TreeSet<RdfEntityType> superTypes = new TreeSet<RdfEntityType>();
		
		/** The root class. */
		private boolean rootClass = false;
		
		/** The is operation. */
		private boolean isOperation = false;
		
		/** The is entity. */
		private boolean isEntity = false;
		
		/** The function import. */
		private boolean functionImport = false;
		
		/** The description. */
		private String description;
		
		/** The sub types. */
		private TreeSet<RdfEntityType> subTypes = new TreeSet<RdfEntityType>();
		
		/** The query text. */
		public String queryText;
		
		/** The delete text. */
		private String deleteText;
		
		/** The insert text. */
		private String insertText;
		
		/** The update text. */
		private String updateText;
		
		/** The update property text. */
		private String updatePropertyText;
		
		/** The function import parameters. */
		private final TreeMap<String, FunctionImportParameter> functionImportParameters = new TreeMap<String, FunctionImportParameter>();
		
		/** The properties. */
		private final TreeMap<String, RdfModel.RdfProperty> properties = new TreeMap<String, RdfModel.RdfProperty>();
		
		/** The navigation properties. */
		private final TreeMap<String, RdfModel.RdfNavigationProperty> navigationProperties = new TreeMap<String, RdfModel.RdfNavigationProperty>();
		
		/** The incoming navigation properties. */
		//		private final TreeMap<String, RdfModel.RdfComplexType> complexTypes = new TreeMap<String, RdfModel.RdfComplexType>();
		private final TreeMap<String, RdfModel.RdfNavigationProperty> incomingNavigationProperties = new TreeMap<String, RdfModel.RdfNavigationProperty>();
		
		/** The primary keys. */
		final TreeMap<String, RdfModel.RdfPrimaryKey> primaryKeys = new TreeMap<String, RdfModel.RdfPrimaryKey>();
		
		/** The is proxy. */
		private boolean isProxy = false;
		
		/** The is reified. */
		private Boolean isReified = false;
        
        /**
         * Gets the reified subject navigation property.
         *
         * @return the reified subject navigation property
         */
        public RdfModel.RdfNavigationProperty getReifiedSubjectNavigationProperty(){

			for(RdfModel.RdfNavigationProperty rdfNavigationProperty : getInheritedNavigationProperties()) {//navigationProperties.values()) {
        		if(rdfNavigationProperty.reifiedSubjectPredicate)
        			return rdfNavigationProperty;
        	}
        	return null;
        }
		
		/**
		 * Gets the delete text.
		 *
		 * @return the delete text
		 */
		public String getDeleteText() {
			return deleteText;
		}

		/**
		 * Sets the delete text.
		 *
		 * @param deleteText the new delete text
		 */
		public void setDeleteText(String deleteText) {
			this.deleteText = deleteText;
		}

		/**
		 * Gets the insert text.
		 *
		 * @return the insert text
		 */
		public String getInsertText() {
			return insertText;
		}

		/**
		 * Sets the insert text.
		 *
		 * @param insertText the new insert text
		 */
		public void setInsertText(String insertText) {
			this.insertText = insertText;
		}

		/**
		 * Checks if is proxy.
		 *
		 * @return true, if is proxy
		 */
		public boolean isProxy() {
			return isProxy;
		}

		/**
		 * Gets the update text.
		 *
		 * @return the update text
		 */
		public String getUpdateText() {
			return updateText;
		}

		/**
		 * Sets the update text.
		 *
		 * @param updateText the new update text
		 */
		public void setUpdateText(String updateText) {
			this.updateText = updateText;
		}

		/**
		 * Gets the update property text.
		 *
		 * @return the update property text
		 */
		public String getUpdatePropertyText() {
			return updatePropertyText;
		}

		/**
		 * Sets the update property text.
		 *
		 * @param updatePropertyText the new update property text
		 */
		public void setUpdatePropertyText(String updatePropertyText) {
			this.updatePropertyText = updatePropertyText;
		}

		/**
		 * Gets the entity type name.
		 *
		 * @return the entity type name
		 */
		public String getEntityTypeName() {
			return entityTypeName;
		}

		/**
		 * Sets the entity type name.
		 *
		 * @param entityTypeName the new entity type name
		 */
		public void setEntityTypeName(String entityTypeName) {
			this.entityTypeName = entityTypeName;
		}

		/**
		 * Gets the entity type node.
		 *
		 * @return the entity type node
		 */
		public RdfNode getEntityTypeNode() {
			return entityTypeNode;
		}

		/**
		 * Sets the entity type node.
		 *
		 * @param entityTypeNode the new entity type node
		 */
		public void setEntityTypeNode(RdfNode entityTypeNode) {
			this.entityTypeNode = entityTypeNode;
		}

		/**
		 * Gets the base type.
		 *
		 * @return the base type
		 */
		public RdfEntityType getBaseType() {
			return baseType;
		}

		/**
		 * Gets the super types.
		 *
		 * @return the super types
		 */
		public TreeSet<RdfEntityType> getSuperTypes() {
			return superTypes;
		}

		/**
		 * Adds the sub type.
		 *
		 * @param subType the sub type
		 */
		protected void addSubType(RdfEntityType subType) {
			subTypes.add(subType);
		}

		/**
		 * Gets the sub types.
		 *
		 * @return the sub types
		 */
		public TreeSet<RdfEntityType> getSubTypes() {
			return subTypes;
		}

		/**
		 * Checks if is node shape.
		 *
		 * @return true, if is node shape
		 */
		public boolean isNodeShape() {
			return (nodeShape != null);
		}

		/**
		 * Gets the node shape.
		 *
		 * @return the node shape
		 */
		public RdfNodeShape getNodeShape() {
			return nodeShape;
		}

		/**
		 * Sets the node shape.
		 *
		 * @param nodeShape the new node shape
		 */
		public void setNodeShape(RdfNodeShape nodeShape) {
			this.nodeShape = nodeShape;
		}

		/**
		 * Gets the all sub types.
		 *
		 * @return the all sub types
		 */
		public TreeSet<RdfEntityType> getAllSubTypes() {
			TreeSet<RdfEntityType> allSubTypes = new TreeSet<RdfEntityType>();
			allSubTypes.addAll(subTypes);
			for (RdfEntityType subType : subTypes) {
				allSubTypes.addAll(subType.getAllSubTypes());
			}
			return allSubTypes;
		}

		/**
		 * Sets the base type.
		 *
		 * @param baseType the new base type
		 */
		public void setBaseType(RdfEntityType baseType) {

			this.baseType = baseType;
			if (baseType != null) {
				baseType.addSubType(this);
				this.superTypes.add(baseType);
			}
		}

		/**
		 * Gets the schema.
		 *
		 * @return the schema
		 */
		public RdfSchema getSchema() {
			return schema;
		}

		/**
		 * Sets the schema.
		 *
		 * @param schema the new schema
		 */
		public void setSchema(RdfSchema schema) {
			this.schema = schema;
		}

		/**
		 * Gets the entity set label.
		 *
		 * @return the entity set label
		 */
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

		/**
		 * Sets the entity set label.
		 *
		 * @param entitySetLabel the new entity set label
		 */
		public void setEntitySetLabel(String entitySetLabel) {
			this.entitySetLabel = entitySetLabel.trim();
		}

		/**
		 * Gets the entity type label.
		 *
		 * @return the entity type label
		 */
		public String getEntityTypeLabel() {
			if (entityTypeLabel == null) {
				return this.entityTypeName;
			} else {
				return this.entityTypeLabel;
			}
		}

		/**
		 * Sets the entity type label.
		 *
		 * @param entityTypeLabel the new entity type label
		 */
		public void setEntityTypeLabel(String entityTypeLabel) {
			this.entityTypeLabel = entityTypeLabel.trim();
		}

		/**
		 * Gets the description.
		 *
		 * @return the description
		 */
		public String getDescription() {
			if (this.description == null || this.description.isEmpty()) {
				return entityTypeLabel;
			} else {
				return description;
			}
		}

		/**
		 * Sets the description.
		 *
		 * @param description the new description
		 */
		public void setDescription(String description) {
			this.description = description.trim();
		}

		/**
		 * Checks if is function import.
		 *
		 * @return true, if is function import
		 */
		public boolean isFunctionImport() {
			return functionImport;
		}

		/**
		 * Gets the function import parameters.
		 *
		 * @return the function import parameters
		 */
		public TreeMap<String, FunctionImportParameter> getFunctionImportParameters() {
			return functionImportParameters;
		}

		/**
		 * Checks if is operation.
		 *
		 * @return true, if is operation
		 */
		public boolean isOperation() {
			return isOperation;
		}

		/**
		 * Sets the operation.
		 *
		 * @param isOperation the new operation
		 */
		public void setOperation(boolean isOperation) {
			this.isOperation = isOperation;
			this.isEntity = !isOperation;
		}

		/**
		 * Checks if is entity.
		 *
		 * @return true, if is entity
		 */
		public boolean isEntity() {
			return isEntity;
		}

		/**
		 * Sets the entity.
		 *
		 * @param isEntity the new entity
		 */
		public void setEntity(boolean isEntity) {
			this.isEntity = isEntity;
			this.isOperation = !isEntity;
		}

		/**
		 * Gets the iri.
		 *
		 * @return the iri
		 */
		public String getIRI() {
			return entityTypeNode.getIRI().toString();
		}

		/**
		 * Gets the url.
		 *
		 * @return the url
		 */
		public String getURL() {
			//Gts the IRI that should be used in SPARQL
			if (isNodeShape()) {
				//get the target emtityType rather than the ndoeShape
				return getNodeShape().getIRI();//.getTargetClass().getIRI().toString();
			} else {
				return entityTypeNode.getIRI().toString();
			}
		}

		/**
		 * Gets the navigation properties.
		 *
		 * @return the navigation properties
		 */
		public HashSet<RdfModel.RdfNavigationProperty> getNavigationProperties() {
			return new HashSet<RdfModel.RdfNavigationProperty>(navigationProperties.values());
		}

		/**
		 * Gets the inherited navigation properties.
		 *
		 * @return the inherited navigation properties
		 */
		public HashSet<RdfModel.RdfNavigationProperty> getInheritedNavigationProperties() {
			HashSet<RdfModel.RdfNavigationProperty> inheritedNavigationProperties = new HashSet<RdfModel.RdfNavigationProperty>();
			inheritedNavigationProperties.addAll(navigationProperties.values());
			if (!this.getSuperTypes().isEmpty()) {
				TreeSet<RdfEntityType> visited = new TreeSet<RdfEntityType>();
				inheritedNavigationProperties.addAll(this.getSuperTypeNavigationProperties(visited));
			}
			return inheritedNavigationProperties;
		}

		/**
		 * Gets the inherited navigation properties map.
		 *
		 * @return the inherited navigation properties map
		 */
		public TreeMap<String, RdfModel.RdfNavigationProperty> getInheritedNavigationPropertiesMap() {
			TreeMap<String, RdfModel.RdfNavigationProperty> inheritedNavigationProperties = new TreeMap<String, RdfModel.RdfNavigationProperty>();
			for (RdfNavigationProperty navigationProperty : this.navigationProperties.values()) {
				inheritedNavigationProperties.put(navigationProperty.navigationPropertyName, navigationProperty);
			}
			if (!this.getSuperTypes().isEmpty()) {
				TreeSet<RdfEntityType> visited = new TreeSet<RdfEntityType>();
				for (RdfNavigationProperty navigationProperty : this.getSuperTypeNavigationProperties(visited)) {
					inheritedNavigationProperties.put(navigationProperty.navigationPropertyName, navigationProperty);
				}
			}
			return inheritedNavigationProperties;
		}

		/**
		 * Gets the super type navigation properties.
		 *
		 * @param visited the visited
		 * @return the super type navigation properties
		 */
		public Collection<RdfModel.RdfNavigationProperty> getSuperTypeNavigationProperties(
				TreeSet<RdfEntityType> visited) {
			HashSet<RdfModel.RdfNavigationProperty> inheritedNavigationProperties = new HashSet<RdfModel.RdfNavigationProperty>();
			inheritedNavigationProperties.addAll(navigationProperties.values());
			if (!this.getSuperTypes().isEmpty()) {
				for (RdfEntityType superType : this.getSuperTypes()) {
					if (!visited.contains(superType)) {
						visited.add(superType);
						inheritedNavigationProperties.addAll(superType.getSuperTypeNavigationProperties(visited));
					}
				}
			}
			return inheritedNavigationProperties;
		}

		/**
		 * Gets the properties.
		 *
		 * @return the properties
		 */
		public Collection<RdfModel.RdfProperty> getProperties() {
			return properties.values();
		}

		/**
		 * Gets the inherited properties.
		 *
		 * @return the inherited properties
		 */
		public TreeSet<RdfModel.RdfProperty> getInheritedProperties() {
			TreeSet<RdfModel.RdfProperty> inheritedProperties = new TreeSet<RdfModel.RdfProperty>();
			inheritedProperties.addAll(properties.values());
			if (!this.getSuperTypes().isEmpty()) {
				TreeSet<RdfEntityType> visited = new TreeSet<RdfEntityType>();
				inheritedProperties.addAll(this.getSuperTypeProperties(visited));
			}
			return inheritedProperties;
		}

		/**
		 * Gets the super type properties.
		 *
		 * @param visited the visited
		 * @return the super type properties
		 */
		public TreeSet<RdfModel.RdfProperty> getSuperTypeProperties(TreeSet<RdfEntityType> visited) {
			TreeSet<RdfModel.RdfProperty> inheritedProperties = new TreeSet<RdfModel.RdfProperty>();
			inheritedProperties.addAll(properties.values());
			if (!this.getSuperTypes().isEmpty()) {
				for (RdfEntityType superType : this.getSuperTypes()) {
					if (!visited.contains(superType)) {
						visited.add(superType);
						inheritedProperties.addAll(superType.getSuperTypeProperties(visited));
					}
				}
			}
			return inheritedProperties;
		}

		/**
		 * Find navigation property.
		 *
		 * @param navigationPropertyName the navigation property name
		 * @return the rdf navigation property
		 */
		public RdfNavigationProperty findNavigationProperty(String navigationPropertyName) {
			//return navigationProperties.get(navigationPropertyName);
			return getInheritedNavigationPropertiesMap().get(navigationPropertyName);
		}

		/**
		 * Find inverse navigation property.
		 *
		 * @param navigationPropertyName the navigation property name
		 * @return the rdf navigation property
		 */
		public RdfNavigationProperty findInverseNavigationProperty(String navigationPropertyName) {
			return incomingNavigationProperties.get(navigationPropertyName);
		}

		/**
		 * Find navigation property by EDM navigation property name.
		 *
		 * @param edmNavigationPropertyName the edm navigation property name
		 * @return the rdf navigation property
		 */
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

		/**
		 * Find navigation property.
		 *
		 * @param navigationPropertyNode the navigation property node
		 * @return the rdf navigation property
		 */
		public RdfNavigationProperty findNavigationProperty(RdfNode navigationPropertyNode) {
			if (navigationPropertyNode != null) {
				for (RdfNavigationProperty navigationProperty : this.getInheritedNavigationProperties()) {
					if (navigationProperty.navigationPropertyNode.getIRI().toString()
							.equals(navigationPropertyNode.getIRI().toString()))
						return navigationProperty;
				}
			}
			return null;
		}

		/**
		 * Find property.
		 *
		 * @param propertyNode the property node
		 * @return the rdf property
		 */
		public RdfProperty findProperty(RdfNode propertyNode) {
			return findProperty(rdfToOdata(propertyNode.getLocalName()));
		}
		
		/**
		 * Find property URI.
		 *
		 * @param propertyURI the property URI
		 * @return the rdf property
		 */
		public RdfProperty findPropertyURI(String propertyURI) {
			return findProperty(rdfToOdata(propertyURI));
		}
		
		/**
		 * Find property.
		 *
		 * @param propertyName the property name
		 * @return the rdf property
		 */
		public RdfProperty findProperty(String propertyName) {
			if (properties.containsKey(propertyName))
				return properties.get(propertyName);
			if (this.getBaseType() != null) {
				return this.getBaseType().findProperty(propertyName);
			} else {
				return null;
			}
		}

		/**
		 * Find complex property.
		 *
		 * @param propertyNode the property node
		 * @return the rdf complex type property pair
		 */
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

		/**
		 * Find complex property.
		 *
		 * @param propertyName the property name
		 * @return the rdf complex type property pair
		 */
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

		/**
		 * Gets the EDM entity type name.
		 *
		 * @return the EDM entity type name
		 */
		public String getEDMEntityTypeName() {
			return this.entityTypeName;
		}

		/**
		 * Gets the EDM entity set name.
		 *
		 * @return the EDM entity set name
		 */
		public String getEDMEntitySetName() {
			if (this.schema.isDefault) {
				return this.entityTypeName;
			} else {
				return this.schema.schemaPrefix + RdfConstants.CLASS_SEPARATOR + this.entityTypeName;
			}
		}

		/**
		 * Gets the o data entity set name.
		 *
		 * @return the o data entity set name
		 */
		public String getODataEntitySetName() {
			if (this.schema.isDefault) {
				return this.entityTypeName;
			} else {
				return this.schema.schemaPrefix + RdfConstants.QNAME_SEPARATOR + this.entityTypeName;
			}
		}

		/**
		 * Gets the primary keys.
		 *
		 * @return the primary keys
		 */
		public Collection<RdfPrimaryKey> getPrimaryKeys() {
			return getPrimaryKeys(true);
		}

		/**
		 * Gets the primary keys.
		 *
		 * @param withBaseType the with base type
		 * @return the primary keys
		 */
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

		/**
		 * Sets the function import.
		 *
		 * @param b the new function import
		 */
		public void setFunctionImport(boolean b) {
			this.functionImport = true;
		}

		/**
		 * Checks if is root class.
		 *
		 * @return true, if is root class
		 */
		public boolean isRootClass() {
			return rootClass;
		}

		/**
		 * Sets the root class.
		 *
		 * @param rootClass the new root class
		 */
		public void setRootClass(boolean rootClass) {
			this.rootClass = rootClass;
		}

		/**
		 * Sets the proxy.
		 *
		 * @param isProxy the new proxy
		 */
		public void setProxy(boolean isProxy) {
			this.isProxy = isProxy;
		}

		/**
		 * Compare to.
		 *
		 * @param rdfEntityType the rdf entity type
		 * @return the int
		 */
		@Override
		public int compareTo(RdfEntityType rdfEntityType) {
			return getURL().compareTo(rdfEntityType.getURL());
		}

		/**
		 * Checks if is reified.
		 *
		 * @return the boolean
		 */
		public Boolean isReified() {
			return isReified;
		}

		/**
		 * Sets the reified.
		 *
		 * @param isReified the new reified
		 */
		public void setReified(Boolean isReified) {
			this.isReified = isReified;
		}

	}

	/**
	 * The Class FunctionImportParameter.
	 */
	public class FunctionImportParameter {
		
		/** The name. */
		private String name;
		
		/** The type. */
		private String type;
		
		/** The nullable. */
		private boolean nullable;
		
		/** The is dataset. */
		private Boolean isDataset = false;
		
		/** The is property path. */
		private Boolean isPropertyPath = false;

		/**
		 * Instantiates a new function import parameter.
		 *
		 * @param name the name
		 * @param type the type
		 * @param nullable the nullable
		 * @param isDataset the is dataset
		 * @param isPropertyPath the is property path
		 */
		private FunctionImportParameter(String name, String type, boolean nullable, boolean isDataset,
				boolean isPropertyPath) {
			super();
			this.name = name;
			this.type = type;
			this.nullable = nullable;
			this.isDataset = isDataset;
			this.isPropertyPath = isPropertyPath;
		}

		/**
		 * Checks if is dataset.
		 *
		 * @return the boolean
		 */
		public Boolean isDataset() {
			return isDataset;
		}

		/**
		 * Checks if is property path.
		 *
		 * @return the boolean
		 */
		public Boolean isPropertyPath() {
			return isPropertyPath;
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Sets the name.
		 *
		 * @param name the new name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * Sets the type.
		 *
		 * @param type the new type
		 */
		public void setType(String type) {
			this.type = type;
		}

		/**
		 * Checks if is nullable.
		 *
		 * @return true, if is nullable
		 */
		public boolean isNullable() {
			return nullable;
		}

		/**
		 * Sets the nullable.
		 *
		 * @param nullable the new nullable
		 */
		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}
	}

	/**
	 * A primary key property.
	 */
	public static class RdfPrimaryKey {
		
		/**
		 * Instantiates a new rdf primary key.
		 *
		 * @param propertyName the property name
		 * @param primaryKeyName the primary key name
		 */
		RdfPrimaryKey(String propertyName, String primaryKeyName) {
			super();
			this.propertyName = propertyName;
			this.primaryKeyName = primaryKeyName;
		}

		/** The property name. */
		private final String propertyName;

		/** The primary key name. */
		private final String primaryKeyName;

		/**
		 * Gets the EDM property name.
		 *
		 * @return the EDM property name
		 */
		public String getEDMPropertyName() {
			return this.propertyName;
		}

		/**
		 * Gets the primary key name.
		 *
		 * @return the primary key name
		 */
		public String getPrimaryKeyName() {
			return primaryKeyName;
		}
	}

	/**
	 * The Class RdfDatatype.
	 */
	public static class RdfDatatype {
		
		/**
		 * Instantiates a new rdf datatype.
		 *
		 * @param datatypeName the datatype name
		 * @param basetypeName the basetype name
		 */
		private RdfDatatype(String datatypeName, String basetypeName) {
			super();
			this.datatypeName = datatypeName;
			this.basetypeName = basetypeName;
		}

		/** The datatype name. */
		private final String datatypeName;
		
		/** The basetype name. */
		private final String basetypeName;

		/**
		 * Gets the datatype name.
		 *
		 * @return the datatype name
		 */
		public String getDatatypeName() {
			return datatypeName;
		}

		/**
		 * Gets the basetype name.
		 *
		 * @return the basetype name
		 */
		public String getBasetypeName() {
			return basetypeName;
		}

		/**
		 * Gets the EDM basetype name.
		 *
		 * @return the EDM basetype name
		 */
		public EdmPrimitiveTypeKind getEDMBasetypeName() {
			return RdfEdmType.getEdmType(basetypeName);
		}

	}

	/**
	 * Represents a property of a complexType that references a complexType.
	 * 
	 */
	public static class RdfComplexProperty {
		
		/** The complex property name. */
		private String complexPropertyName;
		
		/** The complex property label. */
		private String complexPropertyLabel;
		
		/** The complex property type name. */
		private String complexPropertyTypeName;
		
		/** The rdf object property shape. */
		private RdfObjectPropertyShape rdfObjectPropertyShape;
		
		/** The complex type. */
		private RdfComplexType complexType;
		
		/** The cardinality. */
		private Cardinality cardinality;

		/**
		 * Gets the complex property name.
		 *
		 * @return the complex property name
		 */
		public String getComplexPropertyName() {
			return complexPropertyName;
		}

		/**
		 * Sets the complex property name.
		 *
		 * @param complexPropertyName the new complex property name
		 */
		public void setComplexPropertyName(String complexPropertyName) {
			this.complexPropertyName = complexPropertyName;
		}

		/**
		 * Gets the complex property label.
		 *
		 * @return the complex property label
		 */
		public String getComplexPropertyLabel() {
			return complexPropertyLabel;
		}

		/**
		 * Sets the complex property label.
		 *
		 * @param complexPropertyLabel the new complex property label
		 */
		public void setComplexPropertyLabel(String complexPropertyLabel) {
			this.complexPropertyLabel = complexPropertyLabel;
		}

		/**
		 * Gets the complex property type name.
		 *
		 * @return the complex property type name
		 */
		public String getComplexPropertyTypeName() {
			return complexPropertyTypeName;
		}

		/**
		 * Sets the complex property type name.
		 *
		 * @param complexPropertyTypeName the new complex property type name
		 */
		public void setComplexPropertyTypeName(String complexPropertyTypeName) {
			this.complexPropertyTypeName = complexPropertyTypeName;
		}

		/**
		 * Gets the rdf object property shape.
		 *
		 * @return the rdf object property shape
		 */
		public RdfObjectPropertyShape getRdfObjectPropertyShape() {
			return rdfObjectPropertyShape;
		}

		/**
		 * Sets the rdf object property shape.
		 *
		 * @param rdfObjectPropertyShape the new rdf object property shape
		 */
		public void setRdfObjectPropertyShape(RdfObjectPropertyShape rdfObjectPropertyShape) {
			this.rdfObjectPropertyShape = rdfObjectPropertyShape;
		}

		/**
		 * Sets the complex type.
		 *
		 * @param complexType the new complex type
		 */
		public void setComplexType(RdfComplexType complexType) {
			this.complexType = complexType;

		}

		/**
		 * Sets the cardinality.
		 *
		 * @param cardinality the new cardinality
		 */
		public void setCardinality(Cardinality cardinality) {
			this.cardinality = cardinality;
		}

		/**
		 * Gets the complex type.
		 *
		 * @return the complex type
		 */
		public RdfComplexType getComplexType() {
			return complexType;
		}

		/**
		 * Gets the cardinality.
		 *
		 * @return the cardinality
		 */
		public Cardinality getCardinality() {
			return cardinality;
		}

		/**
		 * Checks if is optional.
		 *
		 * @return true, if is optional
		 */
		public boolean isOptional() {
			if (rdfObjectPropertyShape.getMinCount() == 0)
				return true;
			else
				return false;
		}

		/**
		 * Gets the complex property IRI.
		 *
		 * @return the complex property IRI
		 */
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
	public static class RdfProperty implements Comparable<RdfProperty> {

		/** The property name. */
		public String propertyName;
		
		/** The property label. */
		private String propertyLabel;
		
		/** The property type name. */
		public String propertyTypeName;
		
		/** The var name. */
		private String varName;
		
		/** The property node. */
		protected RdfNode propertyNode;
		
		/** The super property. */
		private RdfProperty superProperty;
		
		/** The property uri. */
		private String propertyUri;
		
		/** The is key. */
		private Boolean isKey = false;
		
		/** The is complex. */
		private Boolean isComplex = false;
		
		/** The complex type. */
		private RdfComplexType complexType;
		
		/** The cardinality. */
		private RdfConstants.Cardinality cardinality = RdfConstants.Cardinality.ZERO_TO_ONE;
		
		/** The equivalent property. */
		private String equivalentProperty;
		
		/** The of class. */
		private RdfEntityType ofClass;
		
		/** The description. */
		private String description;
		
		/** The fk property. */
		private RdfNavigationProperty fkProperty = null;
		
		/** The rdf object property shape. */
		private RdfObjectPropertyShape rdfObjectPropertyShape = null;
		
		/** The reified object predicate. */
		private Boolean reifiedObjectPredicate = false;

		/**
		 * Gets the rdf object property shape.
		 *
		 * @return the rdf object property shape
		 */
		public RdfObjectPropertyShape getRdfObjectPropertyShape() {
			return rdfObjectPropertyShape;
		}

		/**
		 * Sets the rdf object property shape.
		 *
		 * @param rdfObjectPropertyShape the new rdf object property shape
		 */
		public void setRdfObjectPropertyShape(RdfObjectPropertyShape rdfObjectPropertyShape) {
			this.rdfObjectPropertyShape = rdfObjectPropertyShape;
		}

		/**
		 * Gets the cardinality.
		 *
		 * @return the cardinality
		 */
		public RdfConstants.Cardinality getCardinality() {
			return cardinality;
		}

		/**
		 * Checks if is optional.
		 *
		 * @return true, if is optional
		 */
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

		/**
		 * Checks if is collection.
		 *
		 * @return true, if is collection
		 */
		public boolean isCollection() {
			switch (cardinality) {
			case MANY:
			case MULTIPLE:
				return true;
			default:
				return false;
			}
		}

		/**
		 * Sets the cardinality.
		 *
		 * @param cardinality the new cardinality
		 */
		public void setCardinality(RdfConstants.Cardinality cardinality) {
			this.cardinality = cardinality;
		}

		/**
		 * Gets the property label.
		 *
		 * @return the property label
		 */
		public String getPropertyLabel() {
			return propertyLabel;
		}

		/**
		 * Sets the property label.
		 *
		 * @param propertyLabel the new property label
		 */
		public void setPropertyLabel(String propertyLabel) {
			this.propertyLabel = propertyLabel.trim();
		}

		/**
		 * Gets the equivalent property.
		 *
		 * @return the equivalent property
		 */
		public String getEquivalentProperty() {
			return equivalentProperty;
		}

		/**
		 * Sets the equivalent property.
		 *
		 * @param equivalentProperty the new equivalent property
		 */
		public void setEquivalentProperty(String equivalentProperty) {
			this.equivalentProperty = equivalentProperty;
		}

		/**
		 * Gets the description.
		 *
		 * @return the description
		 */
		public String getDescription() {
			if (this.description == null || this.description.isEmpty()) {
				return propertyLabel;
			} else {
				return description;
			}
		}

		/**
		 * Sets the description.
		 *
		 * @param description the new description
		 */
		public void setDescription(String description) {

			this.description = description.trim();
		}

		/**
		 * Gets the EDM property name.
		 *
		 * @return the EDM property name
		 */
		public String getEDMPropertyName() {
			return this.propertyName;
		}

		/**
		 * Gets the property URI.
		 *
		 * @return the property URI
		 */
		public String getPropertyURI() {
			if (propertyNode != null) {
				return propertyNode.getIRI().toString();
			} else if (propertyUri != null) {
				return propertyUri;
			} else {
				return null;
			}
		}

		/**
		 * Gets the property type name.
		 *
		 * @return the property type name
		 */
		public String getPropertyTypeName() {
			return propertyTypeName;
		}

		/**
		 * Gets the super property.
		 *
		 * @return the super property
		 */
		public RdfProperty getSuperProperty() {
			return superProperty;
		}

		/**
		 * Sets the super property.
		 *
		 * @param superProperty the new super property
		 */
		public void setSuperProperty(RdfProperty superProperty) {
			this.superProperty = superProperty;
		}

		/**
		 * Gets the checks if is key.
		 *
		 * @return the checks if is key
		 */
		public Boolean getIsKey() {
			return isKey;
		}

		/**
		 * Sets the checks if is key.
		 *
		 * @param isKey the new checks if is key
		 */
		public void setIsKey(Boolean isKey) {
			this.isKey = isKey;
		}

		/**
		 * Gets the checks if is complex.
		 *
		 * @return the checks if is complex
		 */
		public Boolean getIsComplex() {
			return isComplex;
		}

		/**
		 * Sets the checks if is complex.
		 *
		 * @param isComplex the new checks if is complex
		 */
		public void setIsComplex(Boolean isComplex) {
			this.isComplex = isComplex;
		}

		/**
		 * Gets the var name.
		 *
		 * @return the var name
		 */
		public String getVarName() {
			return varName;
		}

		/**
		 * Sets the var name.
		 *
		 * @param varName the new var name
		 */
		public void setVarName(String varName) {
			this.varName = varName;
		}

		/**
		 * Gets the complex type.
		 *
		 * @return the complex type
		 */
		public RdfComplexType getComplexType() {
			return this.complexType;
		}

		/**
		 * Sets the complex type.
		 *
		 * @param complexType the new complex type
		 */
		public void setComplexType(RdfComplexType complexType) {
			this.complexType = complexType;
		}

		/**
		 * Checks if is fk.
		 *
		 * @return true, if is fk
		 */
		public boolean isFK() {
			return !(fkProperty == null);
		}

		/**
		 * Gets the fk property.
		 *
		 * @return the fk property
		 */
		public RdfNavigationProperty getFkProperty() {
			return fkProperty;
		}

		/**
		 * Sets the fk property.
		 *
		 * @param fkProperty the new fk property
		 */
		public void setFkProperty(RdfNavigationProperty fkProperty) {
			this.fkProperty = fkProperty;
		}

		/**
		 * Gets the of class.
		 *
		 * @return the of class
		 */
		public RdfEntityType getOfClass() {
			return ofClass;
		}

		/**
		 * Sets the of class.
		 *
		 * @param ofClass the new of class
		 */
		public void setOfClass(RdfEntityType ofClass) {
			this.ofClass = ofClass;
		}

		/**
		 * Compare to.
		 *
		 * @param rdfProperty the rdf property
		 * @return the int
		 */
		@Override
		public int compareTo(RdfProperty rdfProperty) {
			return propertyName.compareTo(rdfProperty.propertyName);
		}

		/**
		 * Gets the graph pattern variable.
		 *
		 * @param path the path
		 * @return the graph pattern variable
		 */
		public String getGraphPatternVariable(String path) {
			return "?" + path + getEDMPropertyName() + "_s";
		}

		/**
		 * Gets the graph pattern value.
		 *
		 * @param path the path
		 * @return the graph pattern value
		 */
		public String getGraphPatternValue(String path) {
			return "?" + path + getEDMPropertyName() + RdfConstants.PROPERTY_POSTFIX;
		}

		/**
		 * Gets the graph pattern.
		 *
		 * @param path the path
		 * @return the graph pattern
		 */
		public String getGraphPattern(String path) {
			String graphPattern = new String("");
			graphPattern = "?".concat(path).concat("_s <").concat(getPropertyURI()).concat("> ")
					.concat(getGraphPatternValue(path)).concat(" .");
			return graphPattern;
		}

		/**
		 * Sets the reified object predicate.
		 *
		 * @param reifiedObjectpredicate the new reified object predicate
		 */
		public void setReifiedObjectPredicate(Boolean reifiedObjectpredicate) {
			reifiedObjectPredicate = reifiedObjectpredicate;
		}

		/**
		 * Checks if is reified object predicate.
		 *
		 * @return the boolean
		 */
		public Boolean isReifiedObjectPredicate() {
			return reifiedObjectPredicate;
		}
	}

	/**
	 * The Class RdfComplexTypePropertyPair.
	 */
	public static class RdfComplexTypePropertyPair {
		
		/** The rdf complex type. */
		private RdfComplexType rdfComplexType;
		
		/** The rdf property. */
		private RdfProperty rdfProperty;
		
		/** The rdf navigation property. */
		private RdfNavigationProperty rdfNavigationProperty;
		
		/** The rdf complex property. */
		private RdfComplexProperty rdfComplexProperty;
		
		/** The is navigation property. */
		private boolean isNavigationProperty;
		
		/** The is property. */
		private boolean isProperty;
		
		/** The is complex property. */
		private boolean isComplexProperty;

		/**
		 * Instantiates a new rdf complex type property pair.
		 *
		 * @param rdfComplexType the rdf complex type
		 * @param rdfNavigationProperty the rdf navigation property
		 */
		public RdfComplexTypePropertyPair(RdfComplexType rdfComplexType, RdfNavigationProperty rdfNavigationProperty) {
			super();
			this.rdfComplexType = rdfComplexType;
			this.setRdfNavigationProperty(rdfNavigationProperty);
			this.isNavigationProperty = true;
		}

		/**
		 * Instantiates a new rdf complex type property pair.
		 *
		 * @param rdfComplexType the rdf complex type
		 * @param rdfProperty the rdf property
		 */
		public RdfComplexTypePropertyPair(RdfComplexType rdfComplexType, RdfProperty rdfProperty) {
			super();
			this.rdfComplexType = rdfComplexType;
			this.rdfProperty = rdfProperty;
			this.isProperty = true;
		}

		/**
		 * Instantiates a new rdf complex type property pair.
		 *
		 * @param rdfComplexType the rdf complex type
		 * @param rdfComplexProperty the rdf complex property
		 */
		public RdfComplexTypePropertyPair(RdfComplexType rdfComplexType, RdfComplexProperty rdfComplexProperty) {
			super();
			this.rdfComplexType = rdfComplexType;
			this.rdfComplexProperty = rdfComplexProperty;
			this.isComplexProperty = true;
		}

		/**
		 * Gets the equivalent complex property name.
		 *
		 * @return the equivalent complex property name
		 */
		public String getEquivalentComplexPropertyName() {
			return getRdfComplexType().getEquivalentComplexPropertyName();

		}

		/**
		 * Gets the rdf complex type.
		 *
		 * @return the rdf complex type
		 */
		public RdfComplexType getRdfComplexType() {
			return rdfComplexType;
		}

		/**
		 * Gets the rdf property.
		 *
		 * @return the rdf property
		 */
		public RdfProperty getRdfProperty() {
			return rdfProperty;
		}

		/**
		 * Gets the rdf complex property.
		 *
		 * @return the rdf complex property
		 */
		public RdfComplexProperty getRdfComplexProperty() {
			return rdfComplexProperty;
		}

		/**
		 * Gets the rdf navigation property.
		 *
		 * @return the rdf navigation property
		 */
		public RdfNavigationProperty getRdfNavigationProperty() {
			return rdfNavigationProperty;
		}

		/**
		 * Sets the rdf navigation property.
		 *
		 * @param rdfNavigationProperty the new rdf navigation property
		 */
		public void setRdfNavigationProperty(RdfNavigationProperty rdfNavigationProperty) {
			this.rdfNavigationProperty = rdfNavigationProperty;
		}

		/**
		 * Checks if is navigation property.
		 *
		 * @return true, if is navigation property
		 */
		public boolean isNavigationProperty() {
			return isNavigationProperty;
		}

		/**
		 * Checks if is property.
		 *
		 * @return true, if is property
		 */
		public boolean isProperty() {
			return isProperty;
		}

		/**
		 * Checks if is complex property.
		 *
		 * @return true, if is complex property
		 */
		public boolean isComplexProperty() {
			return isComplexProperty;
		}

	}

	/**
	 * The Class RdfComplexType.
	 */
	public static class RdfComplexType implements Comparable<RdfComplexType> {
		
		/** The complex type node. */
		private RdfNode complexTypeNode;
		
		/** The complex type name. */
		private String complexTypeName;
		
		/** The complex type label. */
		private String complexTypeLabel;
		
		/** The domain node. */
		private RdfNode domainNode;
		
		/** The domain name. */
		private String domainName;
		
		/** The schema. */
		private RdfSchema schema;
		
		/** The properties. */
		private TreeMap<String, RdfProperty> properties = new TreeMap<String, RdfProperty>();
		
		/** The complex properties. */
		private TreeMap<String, RdfComplexProperty> complexProperties = new TreeMap<String, RdfComplexProperty>();
		
		/** The navigation properties. */
		private TreeMap<String, RdfNavigationProperty> navigationProperties = new TreeMap<String, RdfNavigationProperty>();
		
		/** The shaped navigation properties. */
		private TreeMap<String, RdfShapedNavigationProperty> shapedNavigationProperties = new TreeMap<String, RdfShapedNavigationProperty>();
		
		/** The domain class. */
		public RdfEntityType domainClass;
		
		/** The provenance type. */
		private boolean provenanceType = false;

		/**
		 * Adds the property.
		 *
		 * @param rdfProperty the rdf property
		 */
		public void addProperty(RdfProperty rdfProperty) {
			properties.put(RdfModel.rdfToOdata(rdfProperty.propertyName), rdfProperty);
		}

		/**
		 * Adds the complex property.
		 *
		 * @param rdfComplexProperty the rdf complex property
		 */
		public void addComplexProperty(RdfComplexProperty rdfComplexProperty) {
			complexProperties.put(RdfModel.rdfToOdata(rdfComplexProperty.getComplexPropertyName()), rdfComplexProperty);
		}

		/**
		 * Adds the navigation property.
		 *
		 * @param rdfNavigationProperty the rdf navigation property
		 */
		public void addNavigationProperty(RdfNavigationProperty rdfNavigationProperty) {
			navigationProperties.put(RdfModel.rdfToOdata(rdfNavigationProperty.navigationPropertyName),
					rdfNavigationProperty);
		}

		/**
		 * Adds the shaped navigation property.
		 *
		 * @param shapedNavigationProperty the shaped navigation property
		 */
		public void addShapedNavigationProperty(RdfShapedNavigationProperty shapedNavigationProperty) {
			shapedNavigationProperties.put(
					RdfModel.rdfToOdata(shapedNavigationProperty.getRdfNavigationProperty().navigationPropertyName),
					shapedNavigationProperty);
		}

		/**
		 * Gets the complex type node.
		 *
		 * @return the complex type node
		 */
		public RdfNode getComplexTypeNode() {
			return complexTypeNode;
		}

		/**
		 * Gets the domain node.
		 *
		 * @return the domain node
		 */
		public RdfNode getDomainNode() {
			return domainNode;
		}

		/**
		 * Gets the complex type name.
		 *
		 * @return the complex type name
		 */
		public String getComplexTypeName() {
			return complexTypeName;
		}

		/**
		 * Gets the equivalent complex property name.
		 *
		 * @return the equivalent complex property name
		 */
		public String getEquivalentComplexPropertyName() {
			return getComplexTypeName().replace(RdfConstants.SHAPE_POSTFIX, "");

		}

		/**
		 * Gets the complex type label.
		 *
		 * @return the complex type label
		 */
		public String getComplexTypeLabel() {
			return complexTypeLabel;
		}

		/**
		 * Gets the domain name.
		 *
		 * @return the domain name
		 */
		public String getDomainName() {
			return domainName;
		}

		/**
		 * Gets the properties.
		 *
		 * @return the properties
		 */
		public TreeMap<String, RdfProperty> getProperties() {
			return properties;
		}

		/**
		 * Gets the complex properties.
		 *
		 * @return the complex properties
		 */
		public TreeMap<String, RdfComplexProperty> getComplexProperties() {
			return complexProperties;
		}

		/**
		 * Gets the navigation properties.
		 *
		 * @return the navigation properties
		 */
		public TreeMap<String, RdfNavigationProperty> getNavigationProperties() {
			return navigationProperties;
		}

		/**
		 * Gets the shaped navigation properties.
		 *
		 * @return the shaped navigation properties
		 */
		public TreeMap<String, RdfShapedNavigationProperty> getShapedNavigationProperties() {
			return shapedNavigationProperties;
		}

		/**
		 * Gets the full qualified name.
		 *
		 * @return the full qualified name
		 */
		public FullQualifiedName getFullQualifiedName() {
			if (domainClass != null) {
				return new FullQualifiedName(domainClass.schema.schemaPrefix, this.getComplexTypeName());
			} else {
				return new FullQualifiedName(this.schema.schemaPrefix, this.getComplexTypeName());
			}
		}

		/**
		 * Sets the schema.
		 *
		 * @param schema the new schema
		 */
		public void setSchema(RdfSchema schema) {
			this.schema = schema;

		}

		/**
		 * Sets the provenance type.
		 *
		 * @param provenanceType the new provenance type
		 */
		public void setProvenanceType(boolean provenanceType) {
			this.provenanceType = provenanceType;
		}

		/**
		 * Checks if is provenance type.
		 *
		 * @return true, if is provenance type
		 */
		public boolean isProvenanceType() {
			return this.provenanceType;
		}

		/**
		 * Gets the iri.
		 *
		 * @return the iri
		 */
		public String getIRI() {
			return schema.schemaName + complexTypeName;
		}

		/**
		 * Compare to.
		 *
		 * @param rdfComplexType the rdf complex type
		 * @return the int
		 */
		@Override
		public int compareTo(RdfComplexType rdfComplexType) {
			return getIRI().compareTo(rdfComplexType.getIRI());
		}

	}

	/**
	 * The Class RdfShapedNavigationProperty.
	 */
	public static class RdfShapedNavigationProperty {
		
		/** The rdf navigation property. */
		private RdfNavigationProperty rdfNavigationProperty;
		
		/** The shaped navigation property name. */
		private String shapedNavigationPropertyName;
		
		/** The shaped navigation property label. */
		private String shapedNavigationPropertyLabel;
		
		/** The cardinality. */
		private Cardinality cardinality;

		/**
		 * Instantiates a new rdf shaped navigation property.
		 *
		 * @param rdfNavigationProperty the rdf navigation property
		 * @param shapedNavigationPropertyName the shaped navigation property name
		 * @param shapedNavigationPropertyLabel the shaped navigation property label
		 * @param cardinality the cardinality
		 */
		public RdfShapedNavigationProperty(RdfNavigationProperty rdfNavigationProperty,
				String shapedNavigationPropertyName, String shapedNavigationPropertyLabel, Cardinality cardinality) {
			super();
			this.rdfNavigationProperty = rdfNavigationProperty;
			this.shapedNavigationPropertyName = shapedNavigationPropertyName;
			this.shapedNavigationPropertyLabel = shapedNavigationPropertyLabel;
			this.cardinality = cardinality;
		}

		/**
		 * Gets the rdf navigation property.
		 *
		 * @return the rdf navigation property
		 */
		public RdfNavigationProperty getRdfNavigationProperty() {
			return rdfNavigationProperty;
		}

		/**
		 * Gets the shaped navigation property name.
		 *
		 * @return the shaped navigation property name
		 */
		public String getShapedNavigationPropertyName() {
			return shapedNavigationPropertyName;
		}

		/**
		 * Gets the shaped navigation property label.
		 *
		 * @return the shaped navigation property label
		 */
		public String getShapedNavigationPropertyLabel() {
			return shapedNavigationPropertyLabel;
		}

		/**
		 * Gets the cardinality.
		 *
		 * @return the cardinality
		 */
		public Cardinality getCardinality() {
			return cardinality;
		}

		/**
		 * Checks if is optional.
		 *
		 * @return true, if is optional
		 */
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

	/**
	 * The Class RdfNavigationProperty.
	 */
	public static class RdfNavigationProperty {
		
		/** The navigation property name. */
		private String navigationPropertyName;
		
		/** The navigation property label. */
		private String navigationPropertyLabel;
		
		/** The var name. */
		private String varName;
		
		/** The related key. */
		private String relatedKey;
		
		/** The domain node. */
		private RdfNode domainNode;

		/** The domain name. */
		private String domainName;
		
		/** The domain class. */
		private RdfEntityType domainClass;
		
		/** The super property. */
		private RdfProperty superProperty;
		
		/** The range node. */
		@SuppressWarnings("unused")
		private RdfNode rangeNode;
		
		/** The range name. */
		private String rangeName;
		
		/** The navigation property node. */
		private RdfNode navigationPropertyNode;
		
		/** The is inverse. */
		private Boolean isInverse = false;
		
		/** The inverse property of. */
		private RdfNode inversePropertyOf;
		
		/** The inverse navigation property. */
		private RdfNavigationProperty inverseNavigationProperty;
		
		/** The description. */
		private String description;
		
		/** The range class. */
		private RdfEntityType rangeClass;
		
		/** The range cardinality. */
		private Cardinality rangeCardinality;
		
		/** The domain cardinality. */
		private Cardinality domainCardinality;
		
		/** The fk property. */
		private RdfProperty fkProperty = null;
		
		/** The navigation property schema. */
		private RdfSchema navigationPropertySchema;
		
		/** The reified subject predicate. */
		private Boolean reifiedSubjectPredicate = false;
		
		/** The reified object predicate. */
		private Boolean reifiedObjectPredicate = false;
		
		/** The reified predicate. */
		private Boolean reifiedPredicate = false;

		/**
		 * Gets the navigation property name.
		 *
		 * @return the navigation property name
		 */
		public String getNavigationPropertyName() {
			return navigationPropertyName;
		}

		/**
		 * Checks if is reified subject predicate.
		 *
		 * @return the boolean
		 */
		public Boolean isReifiedSubjectPredicate() {
			return reifiedSubjectPredicate;
		}

		/**
		 * Sets the reified subject predicate.
		 *
		 * @param reifiedSubjectPredicate the new reified subject predicate
		 */
		public void setReifiedSubjectPredicate(Boolean reifiedSubjectPredicate) {
			this.reifiedSubjectPredicate = reifiedSubjectPredicate;
		}

		/**
		 * Checks if is reified object predicate.
		 *
		 * @return the boolean
		 */
		public Boolean isReifiedObjectPredicate() {
			return reifiedObjectPredicate;
		}

		/**
		 * Sets the reified object predicate.
		 *
		 * @param reifiedObjectPredicate the new reified object predicate
		 */
		public void setReifiedObjectPredicate(Boolean reifiedObjectPredicate) {
			this.reifiedObjectPredicate = reifiedObjectPredicate;
		}

		/**
		 * Checks if is reified predicate.
		 *
		 * @return the boolean
		 */
		public Boolean isReifiedPredicate() {
			return reifiedPredicate;
		}

		/**
		 * Sets the reified predicate.
		 *
		 * @param reifiedPredicate the new reified predicate
		 */
		public void setReifiedPredicate(Boolean reifiedPredicate) {
			this.reifiedPredicate = reifiedPredicate;
		}

		/**
		 * Gets the navigation property node.
		 *
		 * @return the navigation property node
		 */
		public RdfNode getNavigationPropertyNode() {
			return navigationPropertyNode;
		}

		/**
		 * Gets the navigation property node IRI.
		 *
		 * @return the navigation property node IRI
		 */
		public String getNavigationPropertyNodeIRI() {
			return navigationPropertyNode.getIRI().toString();
		}

		/**
		 * Sets the navigation property node.
		 *
		 * @param navigationPropertyNode the new navigation property node
		 */
		public void setNavigationPropertyNode(RdfNode navigationPropertyNode) {
			this.navigationPropertyNode = navigationPropertyNode;
		}

		/**
		 * Gets the navigation property label.
		 *
		 * @return the navigation property label
		 */
		public String getNavigationPropertyLabel() {
			return navigationPropertyLabel;
		}

		/**
		 * Sets the navigation property label.
		 *
		 * @param navigationPropertyLabel the new navigation property label
		 */
		public void setNavigationPropertyLabel(String navigationPropertyLabel) {
			this.navigationPropertyLabel = navigationPropertyLabel.trim();
		}

		/**
		 * Gets the domain name.
		 *
		 * @return the domain name
		 */
		public String getDomainName() {
			return domainName;
		}

		/**
		 * Sets the domain name.
		 *
		 * @param domainName the new domain name
		 */
		public void setDomainName(String domainName) {
			this.domainName = domainName;
		}

		/**
		 * Gets the range name.
		 *
		 * @return the range name
		 */
		public String getRangeName() {
			return rangeName;
		}

		/**
		 * Sets the range name.
		 *
		 * @param rangeName the new range name
		 */
		public void setRangeName(String rangeName) {
			this.rangeName = rangeName;
		}

		/**
		 * Gets the description.
		 *
		 * @return the description
		 */
		public String getDescription() {
			if (this.description == null || this.description.isEmpty()) {
				return navigationPropertyLabel;
			} else {
				return description;
			}
		}

		/**
		 * Sets the description.
		 *
		 * @param description the new description
		 */
		public void setDescription(String description) {
			this.description = description;
		}

		/**
		 * Gets the inverse property of.
		 *
		 * @return the inverse property of
		 */
		public RdfNode getInversePropertyOf() {
			return inversePropertyOf;
		}

		/**
		 * Gets the inverse property of URI.
		 *
		 * @return the inverse property of URI
		 */
		public String getInversePropertyOfURI() {
			if(inversePropertyOf!=null)
				return inversePropertyOf.getIRI().toString();
			else
				return null;
		}

		/**
		 * Gets the domain node.
		 *
		 * @return the domain node
		 */
		public RdfNode getDomainNode() {
			return domainNode;
		}

		/**
		 * Gets the domain class.
		 *
		 * @return the domain class
		 */
		public RdfEntityType getDomainClass() {
			return domainClass;
		}

		/**
		 * Gets the domain node URI.
		 *
		 * @return the domain node URI
		 */
		public String getDomainNodeURI() {
			return this.domainNode.getIRI().toString();
		}

		/**
		 * Sets the domain node.
		 *
		 * @param domainNode the new domain node
		 */
		public void setDomainNode(RdfNode domainNode) {
			this.domainNode = domainNode;
		}

		/**
		 * Gets the range cardinality.
		 *
		 * @return the range cardinality
		 */
		public Cardinality getRangeCardinality() {
			return rangeCardinality;
		}

		/**
		 * Sets the range cardinality.
		 *
		 * @param rangeCardinality the new range cardinality
		 */
		public void setRangeCardinality(Cardinality rangeCardinality) {
			//Fixes #95 this.rangeCardinality = rangeCardinality;
			if (this.rangeCardinality == null || this.rangeCardinality.equals(Cardinality.MANY)
					|| this.rangeCardinality.equals(Cardinality.MULTIPLE)) {
				this.rangeCardinality = rangeCardinality;
			}
		}

		/**
		 * Gets the domain cardinality.
		 *
		 * @return the domain cardinality
		 */
		public Cardinality getDomainCardinality() {
			return domainCardinality;
		}

		/**
		 * Sets the domain cardinality.
		 *
		 * @param domainCardinality the new domain cardinality
		 */
		public void setDomainCardinality(Cardinality domainCardinality) {
			//Fixes #95 this.domainCardinality = domainCardinality;
			if (this.domainCardinality == null || this.domainCardinality.equals(Cardinality.MANY)
					|| this.domainCardinality.equals(Cardinality.MULTIPLE)) {
				this.domainCardinality = domainCardinality;
			}
		}

		/**
		 * Gets the navigation property IRI.
		 *
		 * @return the navigation property IRI
		 */
		public String getNavigationPropertyIRI() {
			return navigationPropertyNode.getIRI().toString();
		}

		/**
		 * Gets the full qualified name.
		 *
		 * @return the full qualified name
		 */
		public FullQualifiedName getFullQualifiedName() {
			return new FullQualifiedName(domainClass.schema.schemaPrefix, this.getEDMNavigationPropertyName());
		}

		/**
		 * Gets the EDM navigation property name.
		 *
		 * @return the EDM navigation property name
		 */
		public String getEDMNavigationPropertyName() {
			if (this.navigationPropertySchema.isDefault  ||this.navigationPropertySchema.getSchemaPrefix().equals(PathQLConstants.PATHQL) ) {
				return this.navigationPropertyName;
			} else {
				return this.navigationPropertySchema.schemaPrefix + RdfConstants.CLASS_SEPARATOR
						+ this.navigationPropertyName;
			}
		}

		/**
		 * Gets the navigation property schema.
		 *
		 * @return the navigation property schema
		 */
		public RdfSchema getNavigationPropertySchema() {
			return this.navigationPropertySchema;
		}

		/**
		 * Gets the navigation property name from EDM.
		 *
		 * @param edmNavigationPropertyName the edm navigation property name
		 * @return the navigation property name from EDM
		 */
		public String getNavigationPropertyNameFromEDM(String edmNavigationPropertyName) {

			if (this.navigationPropertySchema.isDefault) {
				return edmNavigationPropertyName;
			} else {
				return edmNavigationPropertyName
						.replace(this.navigationPropertySchema.schemaPrefix + RdfConstants.CLASS_SEPARATOR, "");
			}
		}

		/**
		 * Gets the range class.
		 *
		 * @return the rangeClass
		 */
		public RdfEntityType getRangeClass() {
			return rangeClass;
		}

		/**
		 * Gets the var name.
		 *
		 * @return the var name
		 */
		public String getVarName() {
			return varName;
		}

		/**
		 * Sets the var name.
		 *
		 * @param varName the new var name
		 */
		public void setVarName(String varName) {
			this.varName = varName;
		}

		/**
		 * Gets the related key.
		 *
		 * @return the related key
		 */
		public String getRelatedKey() {
			return relatedKey;
		}

		/**
		 * Sets the related key.
		 *
		 * @param relatedKey the new related key
		 */
		public void setRelatedKey(String relatedKey) {
			this.relatedKey = relatedKey;
		}

		/**
		 * Checks if is inverse.
		 *
		 * @return the boolean
		 */
		public Boolean IsInverse() {
			return isInverse;
		}

		/**
		 * Sets the checks if is inverse.
		 *
		 * @param isInverse the new checks if is inverse
		 */
		public void setIsInverse(Boolean isInverse) {
			this.isInverse = isInverse;
		}

		//		public void setHasInverse(Boolean hasInverse) {
		//			this.hasInverse = hasInverse;
		/**
		 * Gets the inverse navigation property.
		 *
		 * @return the inverse navigation property
		 */
		//		}
		public RdfNavigationProperty getInverseNavigationProperty() {
			return inverseNavigationProperty;
		}

		/**
		 * Sets the inverse navigation property.
		 *
		 * @param inverseNavigationProperty the new inverse navigation property
		 */
		public void setInverseNavigationProperty(RdfNavigationProperty inverseNavigationProperty) {
			this.inverseNavigationProperty = inverseNavigationProperty;
		}

		/**
		 * Sets the super property.
		 *
		 * @param superProperty the new super property
		 */
		public void setSuperProperty(RdfProperty superProperty) {
			this.superProperty = superProperty;

		}

		/**
		 * Gets the super property.
		 *
		 * @return the super property
		 */
		public RdfProperty getSuperProperty() {
			return superProperty;
		}

		/**
		 * Gets the fk property.
		 *
		 * @return the fk property
		 */
		public RdfProperty getFkProperty() {
			return fkProperty;
		}

		/**
		 * Sets the fk property.
		 *
		 * @param property the new fk property
		 */
		public void setFkProperty(RdfProperty property) {
			this.fkProperty = property;
		}

		/**
		 * Checks for fk property.
		 *
		 * @return the boolean
		 */
		public Boolean hasFkProperty() {
			return !(fkProperty == null);
		}

		/**
		 * Checks if is optional.
		 *
		 * @return true, if is optional
		 */
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

		/**
		 * Gets the graph pattern variable.
		 *
		 * @param path the path
		 * @return the graph pattern variable
		 */
		public String getGraphPatternVariable(String path) {
			return "?" + path + getNavigationPropertyName() + "_s";
		}

		/**
		 * Gets the graph pattern.
		 *
		 * @param path the path
		 * @return the graph pattern
		 */
		public String getGraphPattern(String path) {
			String graphPattern = new String();
			if (IsInverse()) {
				graphPattern = "{?".concat(path).concat("_s <").concat(getNavigationPropertyIRI()).concat("> ")
						.concat(getGraphPatternVariable(path)).concat(" .} UNION {");
				graphPattern = graphPattern.concat(getGraphPatternVariable(path)).concat(" <")
						.concat(getInversePropertyOfURI()).concat("> ?").concat(path).concat("_s .}");
			} else {
				graphPattern = "?".concat(path).concat("_s <").concat(getNavigationPropertyIRI()).concat("> ")
						.concat(getGraphPatternVariable(path)).concat(" .");
			}
			return graphPattern;
		}
	}

	/**
	 * The Class RdfURI.
	 */
	private class RdfURI {
		
		/** The local name. */
		public String localName;

		/** The graph name. */
		private String graphName;
		
		/** The graph prefix. */
		private String graphPrefix;
		
		/** The graph. */
		private RdfSchema graph;

		/**
		 * Instantiates a new rdf URI.
		 *
		 * @param node the node
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		RdfURI(RdfNode node) throws OData2SparqlException {
			//	this.node = node;
			if (node.isBlank()) {
				log.error("Unexpected blank node" + node.getIRI().toString());
			} else {
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
					if (graphPrefix.equals("_"))
						return;
				}
				graph = getOrCreateGraph(graphName, graphPrefix);
			}
		}

		/**
		 * Instantiates a new rdf URI.
		 *
		 * @param graph the graph
		 * @param localName the local name
		 */
		RdfURI(RdfSchema graph, String localName) {
			this.localName = localName;
			this.graph = graph;
			this.graphName = graph.getSchemaName();
			this.graphPrefix = graph.getSchemaPrefix();
		}

		/**
		 * Gets the schema.
		 *
		 * @return the schema
		 */
		public RdfSchema getSchema() {
			return graph;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return graphName + localName;
		}
	}

	/**
	 * The Class RdfNodeShape.
	 */
	public class RdfNodeShape implements Comparable<RdfNodeShape> {
		
		/** The node shape node. */
		private RdfNode nodeShapeNode;
		
		/** The entity type. */
		private RdfEntityType entityType;
		
		/** The base node shape. */
		private RdfNodeShape baseNodeShape;
		
		/** The node shape name. */
		private String nodeShapeName;
		
		/** The node shape label. */
		private String nodeShapeLabel;
		
		/** The node shape description. */
		private String nodeShapeDescription;
		
		/** The schema. */
		private RdfSchema schema;
		
		/** The target class. */
		private RdfEntityType targetClass;
		
		/** The complex type. */
		private RdfComplexType complexType;
		
		/** The deactivated. */
		private boolean deactivated;
		
		/** The data property shapes. */
		private TreeMap<String, RdfDataPropertyShape> dataPropertyShapes = new TreeMap<String, RdfDataPropertyShape>();
		
		/** The object property shapes. */
		private TreeMap<String, RdfObjectPropertyShape> objectPropertyShapes = new TreeMap<String, RdfObjectPropertyShape>();

		/**
		 * Instantiates a new rdf node shape.
		 *
		 * @param nodeShapeNode the node shape node
		 * @throws OData2SparqlException the o data 2 sparql exception
		 */
		public RdfNodeShape(RdfNode nodeShapeNode) throws OData2SparqlException {
			this.nodeShapeNode = nodeShapeNode;
			RdfURI nodeShapeURI = new RdfURI(nodeShapeNode);
			this.schema = nodeShapeURI.graph;
		}

		/**
		 * Gets the node shape node.
		 *
		 * @return the node shape node
		 */
		public RdfNode getNodeShapeNode() {
			return nodeShapeNode;
		}

		/**
		 * Gets the entity type.
		 *
		 * @return the entity type
		 */
		public RdfEntityType getEntityType() {
			return entityType;
		}

		/**
		 * Sets the entity type.
		 *
		 * @param entityType the new entity type
		 */
		public void setEntityType(RdfEntityType entityType) {
			this.entityType = entityType;
		}

		/**
		 * Gets the iri.
		 *
		 * @return the iri
		 */
		public String getIRI() {
			//			return nodeShapeNode.getIRI().toString();
			return schema.getSchemaName() + nodeShapeName;
		}

		/**
		 * Gets the base node shape.
		 *
		 * @return the base node shape
		 */
		public RdfNodeShape getBaseNodeShape() {
			return baseNodeShape;
		}

		/**
		 * Sets the base node shape.
		 *
		 * @param baseNodeShape the new base node shape
		 */
		public void setBaseNodeShape(RdfNodeShape baseNodeShape) {
			this.baseNodeShape = baseNodeShape;
		}

		/**
		 * Gets the node shape name.
		 *
		 * @return the node shape name
		 */
		public String getNodeShapeName() {
			return nodeShapeName;
		}

		/**
		 * Gets the node shape complex type name.
		 *
		 * @return the node shape complex type name
		 */
		public String getNodeShapeComplexTypeName() {
			return nodeShapeName + RdfConstants.SHAPE_POSTFIX;
		}

		/**
		 * Sets the node shape name.
		 *
		 * @param nodeShapeName the new node shape name
		 */
		public void setNodeShapeName(String nodeShapeName) {
			this.nodeShapeName = nodeShapeName;
		}

		/**
		 * Gets the node shape label.
		 *
		 * @return the node shape label
		 */
		public String getNodeShapeLabel() {
			return nodeShapeLabel;
		}

		/**
		 * Sets the node shape label.
		 *
		 * @param nodeShapeLabel the new node shape label
		 */
		public void setNodeShapeLabel(String nodeShapeLabel) {
			this.nodeShapeLabel = nodeShapeLabel;
		}

		/**
		 * Gets the node shape description.
		 *
		 * @return the node shape description
		 */
		public String getNodeShapeDescription() {
			return nodeShapeDescription;
		}

		/**
		 * Sets the node shape description.
		 *
		 * @param nodeShapeDescription the new node shape description
		 */
		public void setNodeShapeDescription(String nodeShapeDescription) {
			this.nodeShapeDescription = nodeShapeDescription;
		}

		/**
		 * Gets the schema.
		 *
		 * @return the schema
		 */
		public RdfSchema getSchema() {
			return schema;
		}

		/**
		 * Sets the schema.
		 *
		 * @param schema the new schema
		 */
		public void setSchema(RdfSchema schema) {
			this.schema = schema;
		}

		/**
		 * Gets the target class.
		 *
		 * @return the target class
		 */
		public RdfEntityType getTargetClass() {
			if (targetClass != null) {
				return targetClass;
			} else if (baseNodeShape != null) {
				return baseNodeShape.getTargetClass();
			} else {
				return null;
			}
		}

		/**
		 * Sets the target class.
		 *
		 * @param targetClass the new target class
		 */
		public void setTargetClass(RdfEntityType targetClass) {
			this.targetClass = targetClass;
		}

		/**
		 * Checks if is deactivated.
		 *
		 * @return true, if is deactivated
		 */
		public boolean isDeactivated() {
			return deactivated;
		}

		/**
		 * Sets the deactivated.
		 *
		 * @param deactivated the new deactivated
		 */
		public void setDeactivated(boolean deactivated) {
			this.deactivated = deactivated;
		}

		/**
		 * Gets the data property shapes.
		 *
		 * @return the data property shapes
		 */
		public TreeMap<String, RdfDataPropertyShape> getDataPropertyShapes() {
			return dataPropertyShapes;
		}

		/**
		 * Gets the object property shapes.
		 *
		 * @return the object property shapes
		 */
		public TreeMap<String, RdfObjectPropertyShape> getObjectPropertyShapes() {
			return objectPropertyShapes;
		}

		/**
		 * Adds the data property shape.
		 *
		 * @param dataPropertyShape the data property shape
		 */
		public void addDataPropertyShape(RdfDataPropertyShape dataPropertyShape) {
			dataPropertyShapes.put(dataPropertyShape.propertyShapeName, dataPropertyShape);
		}

		/**
		 * Adds the object property shape.
		 *
		 * @param objectPropertyShape the object property shape
		 */
		public void addObjectPropertyShape(RdfObjectPropertyShape objectPropertyShape) {
			objectPropertyShapes.put(objectPropertyShape.propertyShapeName, objectPropertyShape);
		}

		/**
		 * Derive graph.
		 *
		 * @return the rdf schema
		 */
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

		/**
		 * Gets the full qualified name.
		 *
		 * @return the full qualified name
		 */
		public FullQualifiedName getFullQualifiedName() {
			return new FullQualifiedName(this.getSchema().getSchemaPrefix(), this.getNodeShapeName());
		}

		/**
		 * Sets the complex type.
		 *
		 * @param complexType the new complex type
		 */
		public void setComplexType(RdfComplexType complexType) {
			this.complexType = complexType;
		}

		/**
		 * Gets the complex type.
		 *
		 * @return the complex type
		 */
		public RdfComplexType getComplexType() {
			return complexType;
		}

		/**
		 * Compare to.
		 *
		 * @param rdfNodeShape the rdf node shape
		 * @return the int
		 */
		@Override
		public int compareTo(RdfNodeShape rdfNodeShape) {
			return getIRI().compareTo(rdfNodeShape.getIRI());
		}

	}

	/**
	 * The Class RdfDataPropertyShape.
	 */
	public class RdfDataPropertyShape {
		
		/** The node shape. */
		private RdfNodeShape nodeShape;
		
		/** The property shape node. */
		private RdfNode propertyShapeNode;
		
		/** The property shape name. */
		private String propertyShapeName;
		
		/** The property shape label. */
		private String propertyShapeLabel;
		
		/** The property shape description. */
		private String propertyShapeDescription;
		
		/** The path. */
		private RdfProperty path;
		
		/** The min count. */
		private int minCount;
		
		/** The max count. */
		private int maxCount;

		/**
		 * Instantiates a new rdf data property shape.
		 *
		 * @param nodeShape the node shape
		 * @param propertyShapeNode the property shape node
		 */
		public RdfDataPropertyShape(RdfNodeShape nodeShape, RdfNode propertyShapeNode) {
			this.nodeShape = nodeShape;
			this.propertyShapeNode = propertyShapeNode;
		}

		/**
		 * Gets the property shape name.
		 *
		 * @return the property shape name
		 */
		public String getPropertyShapeName() {
			return propertyShapeName;
		}

		/**
		 * Gets the node shape.
		 *
		 * @return the node shape
		 */
		public RdfNodeShape getNodeShape() {
			return nodeShape;
		}

		/**
		 * Gets the iri.
		 *
		 * @return the iri
		 */
		public String getIRI() {
			return propertyShapeNode.getIRI().toString();
		}

		/**
		 * Sets the property shape name.
		 *
		 * @param propertyShapeName the new property shape name
		 */
		public void setPropertyShapeName(String propertyShapeName) {
			this.propertyShapeName = propertyShapeName;
		}

		/**
		 * Gets the property shape label.
		 *
		 * @return the property shape label
		 */
		public String getPropertyShapeLabel() {
			return propertyShapeLabel;
		}

		/**
		 * Sets the property shape label.
		 *
		 * @param propertyShapeLabel the new property shape label
		 */
		public void setPropertyShapeLabel(String propertyShapeLabel) {
			this.propertyShapeLabel = propertyShapeLabel;
		}

		/**
		 * Gets the property shape description.
		 *
		 * @return the property shape description
		 */
		public String getPropertyShapeDescription() {
			return propertyShapeDescription;
		}

		/**
		 * Sets the property shape description.
		 *
		 * @param propertyShapeDescription the new property shape description
		 */
		public void setPropertyShapeDescription(String propertyShapeDescription) {
			this.propertyShapeDescription = propertyShapeDescription;
		}

		/**
		 * Gets the path.
		 *
		 * @return the path
		 */
		public RdfProperty getPath() {
			return path;
		}

		/**
		 * Sets the path.
		 *
		 * @param path the new path
		 */
		public void setPath(RdfProperty path) {
			this.path = path;
		}

		/**
		 * Gets the min count.
		 *
		 * @return the min count
		 */
		public int getMinCount() {
			return minCount;
		}

		/**
		 * Sets the min count.
		 *
		 * @param minCount the new min count
		 */
		public void setMinCount(int minCount) {
			this.minCount = minCount;
		}

		/**
		 * Gets the max count.
		 *
		 * @return the max count
		 */
		public int getMaxCount() {
			return maxCount;
		}

		/**
		 * Sets the max count.
		 *
		 * @param maxCount the new max count
		 */
		public void setMaxCount(int maxCount) {
			this.maxCount = maxCount;
		}
	}

	/**
	 * The Class RdfObjectPropertyShape.
	 */
	public class RdfObjectPropertyShape {
		
		/** The node shape. */
		private RdfNodeShape nodeShape;
		
		/** The property shape name. */
		private String propertyShapeName;
		
		/** The property shape label. */
		private String propertyShapeLabel;
		
		/** The property shape description. */
		private String propertyShapeDescription;
		
		/** The path. */
		private RdfNavigationProperty path;
		
		/** The property node. */
		private RdfNodeShape propertyNode;
		
		/** The min count. */
		private Integer minCount;
		
		/** The max count. */
		private Integer maxCount;
		
		/** The is inverse path. */
		private boolean isInversePath = false;

		/**
		 * Instantiates a new rdf object property shape.
		 *
		 * @param nodeShape the node shape
		 * @param propertyShapeNode the property shape node
		 */
		public RdfObjectPropertyShape(RdfNodeShape nodeShape, RdfNode propertyShapeNode) {
			this.nodeShape = nodeShape;
		}

		/**
		 * Gets the property shape name.
		 *
		 * @return the property shape name
		 */
		public String getPropertyShapeName() {
			return propertyShapeName;
		}

		/**
		 * Gets the node shape.
		 *
		 * @return the node shape
		 */
		public RdfNodeShape getNodeShape() {
			return nodeShape;
		}

		/**
		 * Sets the property shape name.
		 *
		 * @param propertyShapeName the new property shape name
		 */
		public void setPropertyShapeName(String propertyShapeName) {
			this.propertyShapeName = propertyShapeName;
		}

		/**
		 * Gets the property shape label.
		 *
		 * @return the property shape label
		 */
		public String getPropertyShapeLabel() {
			return propertyShapeLabel;
		}

		/**
		 * Sets the property shape label.
		 *
		 * @param propertyShapeLabel the new property shape label
		 */
		public void setPropertyShapeLabel(String propertyShapeLabel) {
			this.propertyShapeLabel = propertyShapeLabel;
		}

		/**
		 * Gets the property shape description.
		 *
		 * @return the property shape description
		 */
		public String getPropertyShapeDescription() {
			return propertyShapeDescription;
		}

		/**
		 * Sets the property shape description.
		 *
		 * @param propertyShapeDescription the new property shape description
		 */
		public void setPropertyShapeDescription(String propertyShapeDescription) {
			this.propertyShapeDescription = propertyShapeDescription;
		}

		/**
		 * Gets the path.
		 *
		 * @return the path
		 */
		public RdfNavigationProperty getPath() {
			return path;
		}

		/**
		 * Sets the path.
		 *
		 * @param path the new path
		 */
		public void setPath(RdfNavigationProperty path) {
			this.path = path;
		}

		/**
		 * Gets the property node.
		 *
		 * @return the property node
		 */
		public RdfNodeShape getPropertyNode() {
			return propertyNode;
		}

		/**
		 * Sets the property node.
		 *
		 * @param propertyNode the new property node
		 */
		public void setPropertyNode(RdfNodeShape propertyNode) {
			this.propertyNode = propertyNode;
		}

		/**
		 * Gets the min count.
		 *
		 * @return the min count
		 */
		public Integer getMinCount() {
			return minCount;
		}

		/**
		 * Sets the min count.
		 *
		 * @param minCount the new min count
		 */
		public void setMinCount(Integer minCount) {
			this.minCount = minCount;
		}

		/**
		 * Gets the max count.
		 *
		 * @return the max count
		 */
		public Integer getMaxCount() {
			return maxCount;
		}

		/**
		 * Sets the max count.
		 *
		 * @param maxCount the new max count
		 */
		public void setMaxCount(Integer maxCount) {
			this.maxCount = maxCount;
		}

		/**
		 * Sets the inverse path.
		 *
		 * @param isInversePath the new inverse path
		 */
		public void setInversePath(boolean isInversePath) {
			this.isInversePath = isInversePath;
		}

		/**
		 * Checks if is inverse path.
		 *
		 * @return true, if is inverse path
		 */
		public boolean isInversePath() {
			return isInversePath;
		}
	}

	/**
	 * Gets the rdf repository.
	 *
	 * @return the rdf repository
	 */
	public RdfRepository getRdfRepository() {
		return rdfRepository;
	}

	/**
	 * Gets the rdf prefixes.
	 *
	 * @return the rdf prefixes
	 */
	public RdfPrefixes getRdfPrefixes() {
		return rdfPrefixes;
	}

	/**
	 * Gets the or create entity type from shape.
	 *
	 * @param shapeNode the shape node
	 * @return the or create entity type from shape
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfEntityType getOrCreateEntityTypeFromShape(RdfNode shapeNode) throws OData2SparqlException {
		RdfURI classURI = new RdfURI(shapeNode);
		String equivalentClassName = classURI.localName.replace(RdfConstants.SHAPE_POSTFIX, "");
		RdfEntityType clazz = Enumerable.create(classURI.graph.getClasses())
				.firstOrNull(classNameEquals(rdfToOdata(equivalentClassName)));
		if (clazz == null) {
			//Should never get here
			clazz = new RdfEntityType();
			clazz.schema = classURI.graph;
			clazz.entityTypeName = rdfToOdata(equivalentClassName);

			//clazz.entityTypeNode = entityTypeNode;
			classURI.graph.getClasses().add(clazz);
		}
		return clazz;
	}

	/**
	 * Gets the or create entity type.
	 *
	 * @param classNode the class node
	 * @return the or create entity type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfEntityType getOrCreateEntityType(RdfNode classNode) throws OData2SparqlException {
		return getOrCreateEntityType(classNode, null);
	}

	/**
	 * Gets the or create entity type.
	 *
	 * @param entityTypeNode the entity type node
	 * @param entityTypeLabelNode the entity type label node
	 * @return the or create entity type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfEntityType getOrCreateEntityType(RdfNode entityTypeNode, RdfNode entityTypeLabelNode)
			throws OData2SparqlException {

		RdfURI classURI = new RdfURI(entityTypeNode);
		RdfEntityType clazz = Enumerable.create(classURI.graph.getClasses())
				.firstOrNull(classNameEquals(rdfToOdata(classURI.localName)));
		if (clazz == null) {
			clazz = new RdfEntityType();
			clazz.schema = classURI.graph;
			clazz.entityTypeName = rdfToOdata(classURI.localName);

			clazz.entityTypeNode = entityTypeNode;
			classURI.graph.getClasses().add(clazz);
		}
		//Fixes #90
		if (entityTypeLabelNode != null) {
			clazz.entityTypeLabel = entityTypeLabelNode.getLiteralObject().toString().trim();
		}
		return clazz;
	}

	/**
	 * Gets the or create entity type.
	 *
	 * @param nodeShape the node shape
	 * @return the or create entity type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfEntityType getOrCreateEntityType(RdfNodeShape nodeShape) throws OData2SparqlException {
		RdfURI classURI = new RdfURI(nodeShape.getSchema(), nodeShape.getNodeShapeName().toString());
		RdfEntityType clazz = Enumerable.create(nodeShape.getSchema().getClasses())
				.firstOrNull(classNameEquals(rdfToOdata(classURI.localName)));
		if (clazz == null) {
			clazz = new RdfEntityType();
			clazz.schema = nodeShape.getSchema();
			clazz.entityTypeName = rdfToOdata(nodeShape.nodeShapeName);

			clazz.entityTypeNode = nodeShape.nodeShapeNode;
			nodeShape.getSchema().getClasses().add(clazz);

			clazz.setNodeShape(nodeShape);
		}
		return clazz;
	}

	/**
	 * Find entity type.
	 *
	 * @param entityTypeNode the entity type node
	 * @return the rdf entity type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfEntityType findEntityType(RdfNode entityTypeNode) throws OData2SparqlException {
		RdfURI classURI = new RdfURI(entityTypeNode);
		RdfEntityType clazz = Enumerable.create(classURI.getSchema().getClasses())
				.firstOrNull(classNameEquals(rdfToOdata(classURI.localName)));
		return clazz;
	}

	/**
	 * Gets the or create entity type.
	 *
	 * @param entityTypeNode the entity type node
	 * @param entityTypeLabelNode the entity type label node
	 * @param baseType the base type
	 * @return the or create entity type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfEntityType getOrCreateEntityType(RdfNode entityTypeNode, RdfNode entityTypeLabelNode, RdfEntityType baseType)
			throws OData2SparqlException {
		RdfEntityType clazz = getOrCreateEntityType(entityTypeNode, entityTypeLabelNode);
		clazz.setBaseType(baseType);
		return clazz;
	}

	/**
	 * Gets the or create datatype.
	 *
	 * @param datatypeNode the datatype node
	 * @param basetypeNode the basetype node
	 * @return the or create datatype
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfDatatype getOrCreateDatatype(RdfNode datatypeNode, RdfNode basetypeNode) throws OData2SparqlException {
		RdfURI datatypeURI = new RdfURI(datatypeNode);
		RdfURI basetypeURI = new RdfURI(basetypeNode);
		RdfDatatype datatype = Enumerable.create(datatypeURI.graph.datatypes)
				.firstOrNull(datatypeNameEquals(datatypeURI.localName));
		if (datatype == null) {
			datatype = new RdfDatatype(rdfToOdata(datatypeURI.localName), rdfToOdata(basetypeURI.localName));
			datatypeURI.graph.datatypes.add(datatype);
		}
		return datatype;
	}

	/**
	 * Gets the or create operation entity type.
	 *
	 * @param queryNode the query node
	 * @return the or create operation entity type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfEntityType getOrCreateOperationEntityType(RdfNode queryNode) throws OData2SparqlException {
		return getOrCreateOperationEntityType(queryNode, null, null, null, null, null, null, null);
	}

	/**
	 * Gets the or create operation entity type.
	 *
	 * @param queryNode the query node
	 * @param queryLabel the query label
	 * @param queryText the query text
	 * @param deleteText the delete text
	 * @param insertText the insert text
	 * @param updateText the update text
	 * @param updatePropertyText the update property text
	 * @param isProxy the is proxy
	 * @return the or create operation entity type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfEntityType getOrCreateOperationEntityType(RdfNode queryNode, RdfNode queryLabel, RdfNode queryText,
			RdfNode deleteText, RdfNode insertText, RdfNode updateText, RdfNode updatePropertyText, RdfNode isProxy)
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
		if (isProxy != null)
			operationEntityType.setProxy(isProxy.getLiteralValue().booleanValue());
		operationEntityType.setBaseType(null);

		return operationEntityType;
	}

	/**
	 * Gets the or create operation arguments.
	 *
	 * @param queryNode the query node
	 * @param queryPropertyNode the query property node
	 * @param varName the var name
	 * @param rangeNode the range node
	 * @param isDatasetNode the is dataset node
	 * @param isPropertyPathNode the is property path node
	 * @return the or create operation arguments
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	void getOrCreateOperationArguments(RdfNode queryNode, RdfNode queryPropertyNode, RdfNode varName, RdfNode rangeNode,
			RdfNode isDatasetNode, RdfNode isPropertyPathNode) throws OData2SparqlException {
		RdfEntityType operationEntityType = this.getOrCreateOperationEntityType(queryNode);
		if (operationEntityType.isOperation()) {
			operationEntityType.setFunctionImport(true);

			String propertyTypeName = rangeNode.getIRI().toString();
			Boolean isDataset = (isDatasetNode != null) ? isDatasetNode.getLiteralValue().booleanValue() : false;
			boolean isPropertyPath = (isPropertyPathNode != null) ? isPropertyPathNode.getLiteralValue().booleanValue()
					: false;
			FunctionImportParameter functionImportParameter = new FunctionImportParameter(
					varName.getLiteralValue().getLabel(), propertyTypeName, false, isDataset, isPropertyPath);
			operationEntityType.getFunctionImportParameters().put(varName.getLiteralValue().getLabel(),
					functionImportParameter);
		} else {
		}
	}

	/**
	 * Gets the or create operation property.
	 *
	 * @param queryNode the query node
	 * @param propertyNode the property node
	 * @param propertyLabelNode the property label node
	 * @param rangeNode the range node
	 * @param varName the var name
	 * @return the or create operation property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Gets the or create operation navigation property.
	 *
	 * @param queryNode the query node
	 * @param propertyNode the property node
	 * @param propertyLabelNode the property label node
	 * @param rangeNode the range node
	 * @param varName the var name
	 * @param optionalNode the optional node
	 * @return the or create operation navigation property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfNavigationProperty getOrCreateOperationNavigationProperty(RdfNode queryNode, RdfNode propertyNode,
			RdfNode propertyLabelNode, RdfNode rangeNode, RdfNode varName, RdfNode optionalNode)
			throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI operationURI = new RdfURI(queryNode);
		RdfURI rangeURI = new RdfURI(rangeNode);
		Boolean optional = optionalNode.getLiteralValue().booleanValue();

		RdfEntityType operationEntityType = this.getOrCreateOperationEntityType(queryNode);
		if (!operationEntityType.isEntity()) {
			String navigationPropertyName = rdfToOdata(propertyURI.localName);
			RdfNavigationProperty navigationProperty = operationEntityType.findNavigationProperty(propertyNode);
			if (navigationProperty == null) {

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
				if (!optional) {
					inverseNavigationProperty.rangeCardinality = RdfConstants.Cardinality.ONE;
				} else {
					inverseNavigationProperty.rangeCardinality = RdfConstants.Cardinality.ZERO_TO_ONE;
				}
				inverseNavigationProperty.domainCardinality = RdfConstants.Cardinality.MANY;

			} else {
				navigationProperty.setIsInverse(false);
			}

			navigationProperty.rangeCardinality = RdfConstants.Cardinality.MANY;
			if (!optional) {
				navigationProperty.domainCardinality = RdfConstants.Cardinality.ONE;
			} else {
				navigationProperty.rangeCardinality = RdfConstants.Cardinality.ZERO_TO_ONE;
			}
			//Since this is not a primary entity we need to add the keys of the navigation properties as properties, as well as adding them as primarykeys.
			RdfProperty property = getOrCreateOperationProperty(queryNode, propertyNode, propertyLabelNode, rangeNode,
					varName);
			if (!optional) {
				property.isKey = true;
				operationEntityType.primaryKeys.put(property.propertyName,
						new RdfPrimaryKey(property.propertyName, property.propertyName));
			}
			navigationProperty.setRelatedKey(property.propertyName);
			return navigationProperty;
		} else
			return null;
	}
	
	/**
	 * Gets the or create property.
	 *
	 * @param propertyName the property name
	 * @param propertyNode the property node
	 * @param equivalentPropertyNode the equivalent property node
	 * @param propertyLabelNode the property label node
	 * @param domainNode the domain node
	 * @param rangeNode the range node
	 * @param cardinality the cardinality
	 * @return the or create property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfProperty getOrCreateProperty(String propertyName, RdfNode propertyNode, RdfNode equivalentPropertyNode, RdfNode propertyLabelNode,
			RdfNode domainNode, RdfNode rangeNode, Cardinality cardinality) throws OData2SparqlException {
		RdfURI propertyURI = new RdfURI(propertyNode);

		String propertyTypeName = rangeNode.getIRI().toString();

		RdfEntityType clazz = this.getOrCreateEntityType(domainNode);

		RdfProperty property = Enumerable.create(clazz.getProperties())
				.firstOrNull(propertyNameEquals(propertyName));	
		if (property == null) {
			property = new RdfProperty();
			if (equivalentPropertyNode != null) {
				property.setEquivalentProperty(equivalentPropertyNode.getIRI().toString());
			}
			property.propertyName = propertyName;
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
	
	/**
	 * Gets the or create property.
	 *
	 * @param propertyNode the property node
	 * @param equivalentPropertyNode the equivalent property node
	 * @param propertyLabelNode the property label node
	 * @param domainNode the domain node
	 * @param rangeNode the range node
	 * @param cardinality the cardinality
	 * @return the or create property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfProperty getOrCreateProperty(RdfNode propertyNode, RdfNode equivalentPropertyNode, RdfNode propertyLabelNode,
			RdfNode domainNode, RdfNode rangeNode, Cardinality cardinality) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		String propertyName = rdfToOdata(propertyURI.localName);
		return getOrCreateProperty( propertyName, propertyNode,  equivalentPropertyNode,  propertyLabelNode,
				 domainNode,  rangeNode,  cardinality );
	}

	/**
	 * Gets the or create complex property.
	 *
	 * @param nodeShape the node shape
	 * @param propertyNodeShape the property node shape
	 * @return the or create complex property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Gets the or create complex property.
	 *
	 * @param nodeShape the node shape
	 * @param objectPropertyNodeShape the object property node shape
	 * @return the or create complex property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Gets the or create property.
	 *
	 * @param propertyNode the property node
	 * @param propertyLabelNode the property label node
	 * @param domainNode the domain node
	 * @return the or create property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Gets the or create FK property.
	 *
	 * @param rdfNavigationProperty the rdf navigation property
	 * @return the or create FK property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Gets the or create complex type.
	 *
	 * @param nodeShape the node shape
	 * @return the or create complex type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Gets the or create complex type.
	 *
	 * @param complexTypeNode the complex type node
	 * @param complexTypeLabelNode the complex type label node
	 * @param superdomainNode the superdomain node
	 * @return the or create complex type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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
			complexType.setSchema(complexTypeURI.getSchema());
			//clazz.complexTypes.put(complexType.complexTypeName, complexType);
			clazz.getSchema().getComplexTypes().add(complexType);
		}
		complexType.domainClass = this.getOrCreateEntityType(superdomainNode);
		return complexType;
	}

	/**
	 * Sets the property range.
	 *
	 * @param propertyNode the property node
	 * @param classes the classes
	 * @param rangeNode the range node
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	void setPropertyRange(RdfNode propertyNode, TreeSet<RdfEntityType> classes, RdfNode rangeNode)
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

	/**
	 * Sets the property cardinality.
	 *
	 * @param propertyNode the property node
	 * @param classes the classes
	 * @param cardinality the cardinality
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	void setPropertyCardinality(RdfNode propertyNode, TreeSet<RdfEntityType> classes, Cardinality cardinality)
			throws OData2SparqlException {
		if (classes!=null && classes.size() > 0) {
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
	
	/**
	 * Gets the or create navigation property.
	 *
	 * @param navigationPropertyName the navigation property name
	 * @param propertyNode the property node
	 * @param propertyLabelNode the property label node
	 * @param domainNode the domain node
	 * @param rangeNode the range node
	 * @param multipleDomainNode the multiple domain node
	 * @param multipleRangeNode the multiple range node
	 * @param domainCardinality the domain cardinality
	 * @param rangeCardinality the range cardinality
	 * @return the or create navigation property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfNavigationProperty getOrCreateNavigationProperty(String navigationPropertyName, RdfNode propertyNode, RdfNode propertyLabelNode,
			RdfNode domainNode, RdfNode rangeNode, RdfNode multipleDomainNode, RdfNode multipleRangeNode,
			Cardinality domainCardinality, Cardinality rangeCardinality) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI domainURI = new RdfURI(domainNode);
		RdfURI rangeURI = new RdfURI(rangeNode);

		RdfNavigationProperty navigationProperty = Enumerable.create(domainURI.graph.getNavigationProperties())
				.firstOrNull(navigationPropertyEquals(navigationPropertyName, domainURI.toString()));
		if (navigationProperty == null) {
			navigationProperty = buildNavigationProperty(navigationPropertyName, propertyNode, propertyURI, domainNode,
					domainURI, rangeNode, rangeURI);
		} else {
			//Duplicate navigationproperty with different range?
			if (navigationProperty.getDomainName().equals(domainNode.getLocalName())
					&& !navigationProperty.getRangeName().equals(rangeNode.getLocalName())) {
				log.error("Model contains navigationProperty (" + navigationProperty.getNavigationPropertyName()
						+ ") with multiple ranges for the same domain:" + rangeNode.getLocalName() + " vs "
						+ navigationProperty.getRangeName()
						+ " Consider creating a super class restriction instead, or check consistency between property and inverse property ranges");
			}
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
	
	/**
	 * Gets the or create navigation property.
	 *
	 * @param propertyNode the property node
	 * @param propertyLabelNode the property label node
	 * @param domainNode the domain node
	 * @param rangeNode the range node
	 * @param multipleDomainNode the multiple domain node
	 * @param multipleRangeNode the multiple range node
	 * @param domainCardinality the domain cardinality
	 * @param rangeCardinality the range cardinality
	 * @return the or create navigation property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfNavigationProperty getOrCreateNavigationProperty(RdfNode propertyNode, RdfNode propertyLabelNode,
			RdfNode domainNode, RdfNode rangeNode, RdfNode multipleDomainNode, RdfNode multipleRangeNode,
			Cardinality domainCardinality, Cardinality rangeCardinality) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI domainURI = new RdfURI(domainNode);
		RdfURI rangeURI = new RdfURI(rangeNode);
		String navigationPropertyName = createNavigationPropertyName(multipleDomainNode, multipleRangeNode, domainURI,
				propertyURI, rangeURI);
		return getOrCreateNavigationProperty(navigationPropertyName,  propertyNode,  propertyLabelNode,
			 domainNode,  rangeNode,  multipleDomainNode,  multipleRangeNode,
			 domainCardinality,  rangeCardinality);

	}

	/**
	 * Gets the or create inverse navigation property.
	 *
	 * @param inversePropertyNode the inverse property node
	 * @param inversePropertyLabelNode the inverse property label node
	 * @param propertyNode the property node
	 * @param domainNode the domain node
	 * @param rangeNode the range node
	 * @param multipleDomainNode the multiple domain node
	 * @param multipleRangeNode the multiple range node
	 * @param domainCardinality the domain cardinality
	 * @param rangeCardinality the range cardinality
	 * @return the or create inverse navigation property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Checks if is navigation property.
	 *
	 * @param propertyNode the property node
	 * @param domainNode the domain node
	 * @param rangeNode the range node
	 * @param multipleDomainNode the multiple domain node
	 * @param multipleRangeNode the multiple range node
	 * @return the boolean
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	Boolean isNavigationProperty(RdfNode propertyNode, RdfNode domainNode, RdfNode rangeNode,
			RdfNode multipleDomainNode, RdfNode multipleRangeNode) throws OData2SparqlException {

		RdfURI propertyURI = new RdfURI(propertyNode);
		RdfURI domainURI = new RdfURI(domainNode);
		RdfURI rangeURI = new RdfURI(rangeNode);
		String navigationPropertyName = createNavigationPropertyName(multipleDomainNode, multipleRangeNode, domainURI,
				propertyURI, rangeURI);

		RdfNavigationProperty navigationProperty = Enumerable.create(domainURI.graph.getNavigationProperties())
				.firstOrNull(navigationPropertyEquals(navigationPropertyName, domainNode.getIRIString()));
		if (navigationProperty == null) {
			return false;
		}
		return true;
	}

	/**
	 * Creates the navigation property name.
	 *
	 * @param multipleDomainNode the multiple domain node
	 * @param multipleRangeNode the multiple range node
	 * @param domainURI the domain URI
	 * @param propertyURI the property URI
	 * @param rangeURI the range URI
	 * @return the string
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private String createNavigationPropertyName(RdfNode multipleDomainNode, RdfNode multipleRangeNode, RdfURI domainURI,
			RdfURI propertyURI, RdfURI rangeURI) throws OData2SparqlException {
		return rdfToOdata(propertyURI.localName);
	}

	/**
	 * Builds the navigation property.
	 *
	 * @param navigationPropertyName the navigation property name
	 * @param propertyNode the property node
	 * @param propertyURI the property URI
	 * @param domainNode the domain node
	 * @param domainURI the domain URI
	 * @param rangeNode the range node
	 * @param rangeURI the range URI
	 * @return the rdf navigation property
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private RdfNavigationProperty buildNavigationProperty(String navigationPropertyName, RdfNode propertyNode,
			RdfURI propertyURI, RdfNode domainNode, RdfURI domainURI, RdfNode rangeNode, RdfURI rangeURI)
			throws OData2SparqlException {
		RdfNavigationProperty navigationProperty = new RdfNavigationProperty();
		navigationProperty.navigationPropertyName = navigationPropertyName;
		navigationProperty.navigationPropertyNode = propertyNode;
		navigationProperty.navigationPropertySchema = (new RdfURI(propertyNode)).graph;
		navigationProperty.rangeName = rdfToOdata(rangeURI.localName);
		navigationProperty.rangeNode = rangeNode;
		navigationProperty.domainName = rdfToOdata(domainURI.localName);
		navigationProperty.domainNode = domainNode;

		navigationProperty.domainClass = this.getOrCreateEntityType(domainNode);
		navigationProperty.rangeClass = this.getOrCreateEntityType(rangeNode);

		navigationProperty.domainClass.navigationProperties.put(navigationPropertyName, navigationProperty);
		navigationProperty.rangeClass.incomingNavigationProperties.put(navigationPropertyName, navigationProperty);
		domainURI.graph.getNavigationProperties().add(navigationProperty);
		return navigationProperty;
	}

	/**
	 * Gets the or create graph.
	 *
	 * @param graphName the graph name
	 * @param graphPrefix the graph prefix
	 * @return the or create graph
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfSchema getOrCreateGraph(String graphName, String graphPrefix) throws OData2SparqlException {
		RdfSchema graph = Enumerable.create(graphs).firstOrNull(graphNameEquals(graphName));
		if (graph == null) {
			graph = new RdfSchema(graphName, graphPrefix);
//			graph.schemaName = graphName;
//			graph.schemaPrefix = graphPrefix;
			rdfPrefixes.setNsPrefix(graphPrefix, graphName);
			graphs.add(graph);
		}
		return graph;
	}

	/**
	 * Gets the or create node shape.
	 *
	 * @param nodeShapeNode the node shape node
	 * @param baseNodeShapeNode the base node shape node
	 * @param nodeShapeLabelNode the node shape label node
	 * @param nodeShapeNameNode the node shape name node
	 * @param nodeShapeDescriptionNode the node shape description node
	 * @param nodeShapeTargetClassNode the node shape target class node
	 * @param nodeShapeDeactivatedNode the node shape deactivated node
	 * @return the or create node shape
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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
			rdfEntityType = Enumerable.create(nodeShapeTargetClassURI.graph.getClasses())
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

	/**
	 * Gets the or create node shape entity complex type.
	 *
	 * @param nodeShape the node shape
	 * @return the or create node shape entity complex type
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Gets the or create property shape.
	 *
	 * @param nodeShapeNode the node shape node
	 * @param propertyShapeNode the property shape node
	 * @param propertyShapeLabelNode the property shape label node
	 * @param propertyShapeNameNode the property shape name node
	 * @param propertyShapeDescriptionNode the property shape description node
	 * @param pathNode the path node
	 * @param inversePathNode the inverse path node
	 * @param propertyNode the property node
	 * @param minCountNode the min count node
	 * @param maxCountNode the max count node
	 * @return the or create property shape
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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
			if (nodeShape.getTargetClass() != null) {
				RdfNavigationProperty navigationProperty = nodeShape.getTargetClass().findNavigationProperty(pathNode);
				if (navigationProperty != null) {
					RdfObjectPropertyShape objectPropertyShape = new RdfObjectPropertyShape(nodeShape,
							propertyShapeNode);
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
					RdfObjectPropertyShape objectPropertyShape = new RdfObjectPropertyShape(nodeShape,
							propertyShapeNode);
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
					log.error("Cannot find path in targetClass for property:" + pathNode.getLocalName() +" Check structure of constraints");
				}
			} else {
				log.error("Nodeshape " + nodeShape.nodeShapeLabel + " has no targetClass defined. Check structure of constraints");
			}
		} else {
			log.error("No path defined");
		}
	}

	/**
	 * Gets the model namespace.
	 *
	 * @param rdfGraph the rdf graph
	 * @return the model namespace
	 */
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

	/**
	 * Gets the or create prefix.
	 *
	 * @param prefix the prefix
	 * @param uri the uri
	 * @return the or create prefix
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public String getOrCreatePrefix(String prefix, String uri) throws OData2SparqlException {
		return rdfPrefixes.getOrCreatePrefix(prefix, uri);
	}

	/**
	 * Rdf to odata.
	 *
	 * @param rdfName the rdf name
	 * @return the string
	 */
	public static String rdfToOdata(String rdfName) {
		return rdfName.replace("-", "_").replace("/", "_").replace(".", "_");
	}

	/**
	 * Graph name equals.
	 *
	 * @param graphName the graph name
	 * @return the predicate 1
	 */
	private static final Predicate1<RdfSchema> graphNameEquals(final String graphName) {
		return new Predicate1<RdfSchema>() {
			public boolean apply(RdfSchema graph) {
				return graph.schemaName.equals(graphName);
			}
		};
	}

	/**
	 * Class name equals.
	 *
	 * @param className the class name
	 * @return the predicate 1
	 */
	private static final Predicate1<RdfEntityType> classNameEquals(final String className) {
		return new Predicate1<RdfEntityType>() {
			public boolean apply(RdfEntityType clazz) {
				return clazz.entityTypeName.equals(className);
			}
		};
	}

	/**
	 * Property name equals.
	 *
	 * @param propertyName the property name
	 * @return the predicate 1
	 */
	private static final Predicate1<RdfProperty> propertyNameEquals(final String propertyName) {
		return new Predicate1<RdfProperty>() {
			public boolean apply(RdfProperty property) {
				return property.propertyName.equals(propertyName);
			}
		};
	}

	/**
	 * Complex type name equals.
	 *
	 * @param complexTypeName the complex type name
	 * @return the predicate 1
	 */
	private static final Predicate1<RdfComplexType> complexTypeNameEquals(final String complexTypeName) {
		return new Predicate1<RdfComplexType>() {
			public boolean apply(RdfComplexType complexType) {
				return complexType.complexTypeName.equals(complexTypeName);
			}
		};
	}

	/**
	 * Navigation property equals.
	 *
	 * @param navigationPropertyName the navigation property name
	 * @param navigationPropertyDomainURI the navigation property domain URI
	 * @return the predicate 1
	 */
	private static final Predicate1<RdfNavigationProperty> navigationPropertyEquals(final String navigationPropertyName,
			final String navigationPropertyDomainURI) {
		return new Predicate1<RdfNavigationProperty>() {
			public boolean apply(RdfNavigationProperty navigationProperty) {
				return navigationProperty.navigationPropertyName.equals(navigationPropertyName)
						&& navigationProperty.domainNode.getIRIString().equals(navigationPropertyDomainURI);
			}
		};
	}

	/**
	 * Datatype name equals.
	 *
	 * @param datatypeName the datatype name
	 * @return the predicate 1
	 */
	private static final Predicate1<RdfDatatype> datatypeNameEquals(final String datatypeName) {
		return new Predicate1<RdfDatatype>() {
			public boolean apply(RdfDatatype datatype) {
				return datatype.datatypeName.equals(datatypeName);
			}
		};
	}

	/**
	 * Allocate node shapes to graphs.
	 *
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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
			if(nodeShape.getSchema()==null) {
				log.error("Cannot identify schema of nodeshape: "+ nodeShape.getNodeShapeName()+" Nodeshape will be ignored, assuming an orphan");
			}else {
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
	}

	/**
	 * Allocate object property shape.
	 *
	 * @param nodeShape the node shape
	 * @param objectPropertyShape the object property shape
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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
			if(nodeShape.getComplexType()!=null) {
				nodeShape.getComplexType().addShapedNavigationProperty(shapedNavigationProperty);
			}else {
				log.error("Nodeshape without allocated complexType:" +nodeShape.getNodeShapeName() );
			}
		}
	}

	/**
	 * Deduce object property shape cardinality.
	 *
	 * @param objectPropertyShape the object property shape
	 * @return the cardinality
	 */
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

	/**
	 * Inherit base node properties.
	 *
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private void inheritBaseNodeProperties() throws OData2SparqlException {
		for (RdfNodeShape nodeShape : pendingNodeShapes.values()) {
			if (nodeShape.getBaseNodeShape() != null) {
				inheritProperties(nodeShape, nodeShape.getBaseNodeShape());
			}
		}
	}

	/**
	 * Inherit properties.
	 *
	 * @param nodeShape the node shape
	 * @param baseNodeShape the base node shape
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Adds the proxy.
	 *
	 * @param proxyDataset the proxy dataset
	 */
	public void addProxy(String proxyDataset) {
		if (!this.proxies.contains(proxyDataset)) {
			this.proxies.add(proxyDataset);
			//TODO This should not be called when the proxy is added because more namespace prefixes could be added to the proxied dataset after it is added
			RdfRepository proxyRepository = this.getRdfRepository().getRepositories().getRdfRepository(proxyDataset);
			for (Namespace namespace : proxyRepository.getNamespaces().values()) {
				try {
					this.getRdfPrefixes().getOrCreatePrefix(namespace.getPrefix(), namespace.getName());
				} catch (OData2SparqlException e) {
					log.warn("Prefix " + namespace.getPrefix() + " cannot be created " + namespace.getName());
				}
			}
		}
	}

}