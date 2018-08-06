/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.Constants;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.olingo.commons.api.edm.provider.CsdlTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfConstants {

	private final static Logger log = LoggerFactory.getLogger(RdfConstants.class);

	public enum Cardinality {
		// Cardinality corresponding to
		// 0..1, 1..1, 0..*, 1..*
		ZERO_TO_ONE, ONE, MANY, MULTIPLE
	}

	public static final int DATE_PRECISION = 3;
	public static final int DECIMAL_SCALE = 10;
	public static final String DEFAULTCONFIG = "/var/opt/inova8/odata2sparql/";
	public static final String DEFAULTFOLDER = ".default";

	public static final String RESET = "$reset";
	public static final String RELOAD = "$reload";
	public static final String WILDCARD = "*";
	public static final String UNDEFVALUE = "UNDEF";
	private final static ValueFactory valueFactoryImpl = SimpleValueFactory.getInstance();
	static private Hashtable<Value, String> metaQueries;// = new Hashtable<Value, String>();
	static private final Hashtable<Value, Hashtable<Value, String>> metaModels = new Hashtable<Value, Hashtable<Value, String>>();

	public static final String CONFIG_PROPERTIES = "\\config.properties";
	public final static String repositoryUrl = "repositoryUrl";
	public final static String systemId = "ODATA2SPARQL";
	public final static String bootStrapQuery = "SELECT  ?Metadata ?Query  ?QueryString WHERE { ?Metadata ?Query  ?querys . ?querys <http://spinrdf.org/sp#text> ?QueryString . ?Query  <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://inova8.com/odata4sparql#metadataQuery> .}";
	public final static Value URI_DEFAULTMETAMODEL = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#RDFSModel");
	public static String repositoryWorkingDirectory;
	public static File repositoryManagerDir;
	public static String odata4sparqlFile;
	public static String rdfFile;
	public static String rdfsFile;
	public static String sailFile;
	public static String spFile;

	public final static Value RDFSModel = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#RDFSModel");

	public static final String TARGETENTITY = "http://targetEntity";
	public static final String ASSERTEDTYPE = "http://assertedType";
	public static final String COUNT = "http://count";
	public static final String PREFIX = "j";
	public static final String PROPERTY_POSTFIX = "_value";

	public static final String SAP_ANNOTATION_SCHEMA = "http://www.sap.com/Protocols/SAPData";
	private static final String SAP_ANNOTATION_NS = "sap";
	private static final String SAP_LABEL = "label";
	private static final String SAP_HEADING = "heading";
	private static final String SAP_QUICKINFO = "quickinfo";

	public static final String RDF_SCHEMA = "http://www.w3.org/1999/02/22-rdf-syntax-ns";
	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDF = "rdf";
	public static final String RDFS_SCHEMA = "http://www.w3.org/2000/01/rdf-schema";
	public static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String RDFS = "rdfs";
	public static final String OWL_SCHEMA = "http://www.w3.org/2002/07/owl";
	public static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
	public static final String OWL = "owl";
	public static final String XSD_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
	public static final String XSD = "xsd";

	public static final String DC_NS = "http://purl.org/dc/elements/1.1/";
	public static final String DC = "dc";

	public static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";
	public static final String XSD_DATE = "http://www.w3.org/2001/XMLSchema#date";
	public static final String XSD_DATETIME = "http://www.w3.org/2001/XMLSchema#dateTime";
	public static final String RDF_PLAIN_LITERAL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral";
	public static final String RDF_LITERAL = "http://www.w3.org/2000/01/rdf-schema#Literal";

	public static final String RDFS_RESOURCE = "http://www.w3.org/2000/01/rdf-schema#Resource";
	public static final String RDFS_RESOURCE_LABEL = "Resource";
	public static final String RDFS_CLASS = "http://www.w3.org/2000/01/rdf-schema#Class";
	public static final String RDFS_CLASS_LABEL = "Class";
	public static final String OWL_THING = "http://www.w3.org/2002/07/owl#Thing";
	public static final String OWL_THING_LABEL = "Thing";
	public static final String OWL_CLASS = "http://www.w3.org/2002/07/owl#Class";
	public static final String OWL_CLASS_LABEL = "Class";
	public static final String RDF_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
	public static final String RDF_PROPERTY_LABEL = "Property";
	public static final String OWL_OBJECTPROPERTY = "http://www.w3.org/2002/07/owl#ObjectProperty";
	public static final String OWL_OBJECTPROPERTY_LABEL = "ObjectProperty";
	public static final String OWL_DATATYPEPROPERTY = "http://www.w3.org/2002/07/owl#DatatypeProperty";
	public static final String OWL_DATATYPEPROPERTY_LABEL = "DatatypeProperty";
	public static final String OWL_ONTOLOGY = "http://www.w3.org/2002/07/owl#Ontology";
	public static final String OWL_ONTOLOGY_LABEL = "Ontology";

	public static final String RDF_STATEMENT = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement";
	public static final String RDF_STATEMENT_LABEL = "Statement";
	public static final String SUBJECT = "subjectId";
	public static final String RDF_SUBJECT = "http://www.w3.org/1999/02/22-rdf-syntax-ns#subjectId";
	public static final String RDF_SUBJECT_LABEL = "subject";
	public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	public static final String RDF_TYPE_LABEL = "has Type";
	public static final String RDF_INVERSE_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#hasInstance";
	public static final String RDF_INVERSE_TYPE_LABEL = "has Instance";

	public static final String RDFS_DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
	public static final String RDFS_DOMAIN_LABEL = "domain";
	public static final String RDFS_SUBCLASSOF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
	public static final String RDFS_SUBCLASSOF_LABEL = "subClassOf";
	public static final String RDFS_RANGE = "http://www.w3.org/2000/01/rdf-schema#range";
	public static final String RDFS_RANGE_LABEL = "range";

	public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
	public static final String RDFS_LABEL_LABEL = "label";

	private static final String OWL_EQUIVALENTPROPERTY_LABEL = "equivalentProperty";
	public static final String OWL_IMPORTS = "http://www.w3.org/2002/07/owl#imports";
	public static final String OWL_IMPORTS_LABEL = "imports";

	public static final String SPARQL_UNDEF = "UNDEF";

	private static final String ONTOLOGY = "Ontology";
	private static final String DATATYPE = "Datatype";
	private static final String PROPERTY = "Property";

	public static final String LANGSTRING = "langString";
	public static final String LANG = "lang";
	public static final String VALUE = "value";

	private static final String INVERSEOF = "inverseOf ";
	public static final String SPARQL_MODEL = "SparqlModel";

	public static final String ENTITYCONTAINER = "Container";
	public static final String ENTITYCONTAINERNAMESPACE = "Instances";

	/*
	 * Modifiers for classes and properties as they appear in $metadata
	*/

	public static final String PREDICATE_SEPARATOR = "_";
	public static final String CLASS_SEPARATOR = "_";

	public static final String CLASS_LABEL_PREFIX = "";
	public static final String PROPERTY_LABEL_PREFIX = "";
	public static final String NAVIGATIONPROPERTY_LABEL_PREFIX = "";

	/*	
	The following from http://www.ietf.org/ defining what is allowed in a URI
	Needed to match ':' of qname with embeded key in odata.
	 
	 2.3. Unreserved Characters
	
	   Data characters that are allowed in a URI but do not have a reserved
	   purpose are called unreserved.  These include upper and lower case
	   letters, decimal digits, and a limited set of punctuation marks and
	   symbols.
	
	      unreserved  = alphanum | mark
	
	      mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
	
	   Unreserved characters can be escaped without changing the semantics
	   of the URI, but this should not be done unless the URI is being used
	   in a context that does not allow the unescaped character to appear.
	*/

	public static final String QNAME_SEPARATOR = "~";// ":";
	public static final String QNAME_SEPARATOR_ENCODED = "~";// "%3A";
	public static final String QNAME_SEPARATOR_RDF = ":";
	public static final Object RDF_LANG_STRING = RDF + "." + LANGSTRING;

	public static final Value URI_ASSOCIATIONQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#associationQuery");
	public static final Value URI_CLASSQUERY = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#classQuery");

	public static final Value URI_DATATYPEQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#datatypeQuery");
	public static final Value URI_GRAPHQUERY = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#graphQuery");
	public static final Value URI_INVERSEASSOCIATIONQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#inverseAssociationQuery");
	public static final Value URI_OPERATIONARGUMENTQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#operationArgumentQuery");
	public static final Value URI_OPERATIONASSOCIATIONRESULTQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#operationAssociationResultQuery");
	public static final Value URI_OPERATIONPROPERTYRESULTQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#operationPropertyResultQuery");
	public static final Value URI_OPERATIONQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#operationQuery");
	public static final Value URI_PREFIXQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#prefixQuery");
	public static final Value URI_PROPERTYQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyQuery");
	public static final Value URI_PROPERTY_DOMAINS_QUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyQuery_Domains");
	public static final Value URI_PROPERTY_RANGES_QUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyQuery_Ranges");
	public static final Value URI_PROPERTY_CARDINALITY_QUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyQuery_Cardinality");
	public static final Value URI_REPOSITORYQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#repositoryQuery");
	public static final Value URI_HALYARD_SEARCH = valueFactoryImpl
			.createIRI("http://merck.github.io/Halyard/ns#search");

	public final static String SAP_LABEL_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_LABEL;
	public final static String SAP_HEADING_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_HEADING;
	public final static String SAP_QUICKINFO_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_QUICKINFO;
	public final static String RDFS_CLASS_FQN = RdfConstants.RDFS + "." + RdfConstants.RDFS_CLASS_LABEL;
	public final static String PROPERTY_FQN = RdfConstants.RDF + "." + RdfConstants.PROPERTY;
	public final static String DATATYPE_FQN = RdfConstants.RDF + "." + RdfConstants.DATATYPE;
	public final static String INVERSEOF_FQN = RdfConstants.OWL + "." + RdfConstants.INVERSEOF;
	public final static String OWL_EQUIVALENTPROPERTY_FQN = RdfConstants.OWL + "."
			+ RdfConstants.OWL_EQUIVALENTPROPERTY_LABEL;
	public final static String ONTOLOGY_FQN = RdfConstants.OWL + "." + RdfConstants.ONTOLOGY;

	private final static CsdlTerm sapLabelTerm = new CsdlTerm().setName(RdfConstants.SAP_LABEL);
	private final static CsdlTerm sapheadingTerm = new CsdlTerm().setName(RdfConstants.SAP_HEADING);
	private final static CsdlTerm sapquickinfoTerm = new CsdlTerm().setName(RdfConstants.SAP_QUICKINFO);
	private final static CsdlTerm rdfsClassTerm = new CsdlTerm().setName(RdfConstants.RDFS_CLASS_LABEL);
	private final static CsdlTerm rdfPropertyTerm = new CsdlTerm().setName(RdfConstants.PROPERTY);
	private final static CsdlTerm rdfsDatatypeTerm = new CsdlTerm().setName(RdfConstants.DATATYPE);
	private final static CsdlTerm owlInverseOfTerm = new CsdlTerm().setName(RdfConstants.INVERSEOF);

	private final static CsdlTerm owlOntologyTerm = new CsdlTerm().setName(RdfConstants.ONTOLOGY);

	public final static List<String> RDF_DATATYPES = new ArrayList<String>();

	public static final HashMap<String, CsdlTerm> TERMS = new HashMap<String, CsdlTerm>();

	public static Hashtable<Value, String> getMetaQueries() {
		return metaQueries;
	}

	public static void setMetaQueries(Hashtable<Value, String> newMetaQueries) {
		metaQueries = newMetaQueries;
	}

	public static Hashtable<Value, Hashtable<Value, String>> getMetaModels() {
		return metaModels;
	}

	static {
		//Initialize the file dependent constants	
		try {
			String workingDirectory = null;
			if (SystemUtils.IS_OS_WINDOWS) {
				workingDirectory = Paths.get(System.getenv("AppData"), "inova8", "odata2sparql").toString();
			} else if (SystemUtils.IS_OS_LINUX) {
				workingDirectory = Paths.get(DEFAULTCONFIG).toString();
			} else if (SystemUtils.IS_OS_MAC) {
				workingDirectory = Paths.get(System.getProperty("user.home") + "/Library/Preferences", "inova8", "odata2sparql").toString();
			} else {
				log.error("Unsupported OS: " + SystemUtils.OS_NAME);
				throw new RuntimeException("Unsupported OS: " + SystemUtils.OS_NAME, null);
			}

			String repositoryManagerDirPath = URLDecoder.decode(RdfConstants.class.getResource("/").getFile(), "UTF-8");

			repositoryManagerDir = new File(workingDirectory);
			repositoryWorkingDirectory = workingDirectory;

			odata4sparqlFile = repositoryManagerDirPath + "ontologies/odata4sparql.rdf";
			rdfFile = repositoryManagerDirPath + "ontologies/22-rdf-syntax-ns.ttl";
			rdfsFile = repositoryManagerDirPath + "ontologies/rdf-schema.ttl";
			sailFile = repositoryManagerDirPath + "ontologies/sail.rdf";
			spFile = repositoryManagerDirPath + "ontologies/sp.ttl";

		} catch (UnsupportedEncodingException e) {
			log.error("Cannot decode file directory to be used for repository: " + e.getMessage());
		}
		//Initialize the RDF datatypes
		RDF_DATATYPES.add((RdfConstants.RDF_PLAIN_LITERAL));
		RDF_DATATYPES.add((RdfConstants.RDF_LITERAL));
		RDF_DATATYPES.add(("http://www.w3.org/2000/01/rdf-schema#Literal"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#decimal"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#Literal"));
		RDF_DATATYPES.add((RdfConstants.XSD_STRING));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#boolean"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#float"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#double"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#duration"));
		RDF_DATATYPES.add((RdfConstants.XSD_DATETIME));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#time"));
		RDF_DATATYPES.add(RdfConstants.XSD_DATE);
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gYearMonth"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gYear"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gMonthDay"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gDay"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gMonth"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#hexBinary"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#base64Binary"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#anyURI"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#anyType"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#QName"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#NOTATION"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#normalizedString"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#token"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#language"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#IDREFS"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#ENTITIES"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#NMTOKEN"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#Name"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#NCName"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#ID"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#IDREF"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#ENTITY"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#integer"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#nonPositiveInteger"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#negativeInteger"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#long"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#int"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#short"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#byte"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#nonNegativeInteger"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#unsignedLong"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#unsignedInt"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#unsignedShort"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#unsignedByte"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#positiveInteger"));

		//Initialize the EDM standard terms
		TERMS.put(RdfConstants.SAP_LABEL, sapLabelTerm);
		TERMS.put(RdfConstants.SAP_HEADING, sapheadingTerm);
		TERMS.put(RdfConstants.SAP_QUICKINFO, sapquickinfoTerm);
		TERMS.put(RdfConstants.RDFS_CLASS_LABEL, rdfsClassTerm);
		TERMS.put(RdfConstants.PROPERTY, rdfPropertyTerm);
		TERMS.put(RdfConstants.DATATYPE, rdfsDatatypeTerm);
		TERMS.put(RdfConstants.INVERSEOF, owlInverseOfTerm);
		TERMS.put(RdfConstants.ONTOLOGY, owlOntologyTerm);

	}
}
