/*
 * inova8 2020
 */
package com.inova8.odata2sparql.Constants;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang.SystemUtils;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class RdfConstants.
 */
public class RdfConstants {

	/** The Constant log. */
	private final static Logger log = LoggerFactory.getLogger(RdfConstants.class);

	/**
	 * The Enum Cardinality.
	 */
	public enum Cardinality {
		// Cardinality corresponding to
		/** The zero to one. */
		// 0..1, 1..1, 0..*, 1..*
		ZERO_TO_ONE, 
 /** The one. */
 ONE, 
 /** The many. */
 MANY, 
 /** The multiple. */
 MULTIPLE
	}

	/** The Constant DEFAULTINSERTGRAPH. */
	public static final String DEFAULTINSERTGRAPH = "http://insertGraph";
	
	/** The Constant SERVICE. */
	public static final String SERVICE = "service";	
	
	/** The Constant DATE_PRECISION. */
	public static final int DATE_PRECISION = 3;
	
	/** The Constant DECIMAL_SCALE. */
	public static final int DECIMAL_SCALE = 27;//10; Fixes #119
	
	/** The Constant DEFAULTCONFIG. */
	public static final String DEFAULTCONFIG = "/var/opt/inova8/odata2sparql/";
	
	/** The Constant DEFAULTFOLDER. */
	public static final String DEFAULTFOLDER = ".default";

	/** The Constant RESET. */
	public static final String RESET = "$reset";
	
	/** The Constant RELOAD. */
	public static final String RELOAD = "$reload";
	
	/** The Constant LOGS. */
	public static final String LOGS = "$logs";
	
	/** The Constant CHANGES. */
	public static final String CHANGES = "$changes";
	
	/** The Constant WILDCARD. */
	public static final String WILDCARD = "*";
	
	/** The Constant UNDEFVALUE. */
	public static final String UNDEFVALUE = "UNDEF";
	
	/** The Constant PREFIXSEPARATOR. */
	public static final String PREFIXSEPARATOR = ".";
	
	/** The Constant valueFactoryImpl. */
	private final static ValueFactory valueFactoryImpl = SimpleValueFactory.getInstance();
	
	/** The meta queries. */
	static private Hashtable<Value, String> metaQueries;// = new Hashtable<Value, String>();
	
	/** The Constant metaModels. */
	static private final Hashtable<Value, Hashtable<Value, String>> metaModels = new Hashtable<Value, Hashtable<Value, String>>();

	/** The Constant CONFIG_PROPERTIES. */
	public static final String CONFIG_PROPERTIES = "\\config.properties";
	
	/** The Constant repositoryUrl. */
	public final static String repositoryUrl = "repositoryUrl";
	
	/** The Constant systemId. */
	public final static String systemId = "ODATA2SPARQL";
	
	/** The Constant systemIRI. */
	public final static String systemIRI = "HTTP://ODATA2SPARQL";
	
	/** The Constant bootStrapQuery. */
	public final static String bootStrapQuery = "SELECT  ?Metadata ?Query  ?QueryString WHERE { ?Metadata ?Query  ?querys . ?querys <http://spinrdf.org/sp#text> ?QueryString . ?Query  <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://inova8.com/odata4sparql#metadataQuery> .}";
	
	/** The Constant URI_DEFAULTMETAMODEL. */
	public final static Value URI_DEFAULTMETAMODEL = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#RDFSModel");
	
	/** The repository working directory. */
	public static String repositoryWorkingDirectory;
	
	/** The repository manager dir. */
	public static File repositoryManagerDir;
	
	/** The odata 4 sparql file. */
	public static String odata4sparqlFile;
	
	/** The rdf file. */
	public static String rdfFile;
	
	/** The rdfs file. */
	public static String rdfsFile;
	
	/** The sail file. */
	public static String sailFile;
	
	/** The sp file. */
	public static String spFile;
	
	/** The contextmenu file. */
	public static String contextmenuFile;
	
	/** The search file. */
	public static String searchFile;
	
	/** The olgap file. */
	public static String olgapFile;
	
	/** The change file. */
	public static String changeFile;
	
	/** The script file. */
	public static String scriptFile;
	
	/** The Constant RDFSModel. */
	public final static Value RDFSModel = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#RDFSModel");

	/** The Constant TARGETENTITY. */
	public static final String TARGETENTITY = "http://targetEntity";
	
	/** The Constant ASSERTEDTYPE. */
	public static final String ASSERTEDTYPE = "http://assertedType";
	
	/** The Constant ASSERTEDSHAPE. */
	public static final String ASSERTEDSHAPE = "http://assertedShape";
	
	/** The Constant MATCHING. */
	public static final String MATCHING = "http://matching";
	
	/** The Constant COUNT. */
	public static final String COUNT = "http://count";
	
	/** The Constant PREFIX. */
	public static final String PREFIX = "j";
	
	/** The Constant PROPERTY_POSTFIX. */
	public static final String PROPERTY_POSTFIX = "_value";
	
	/** The Constant PLURAL. */
	public static final String PLURAL = "s";
	
	/** The Constant SHAPE_POSTFIX. */
	public static final String SHAPE_POSTFIX = "_shape";//"";//

	/** The Constant DEFAULTMATCH. */
	public static final String DEFAULTMATCH = "{ key1 (<http://www.w3.org/2004/02/skos/core#exactMatch> | ^ <http://www.w3.org/2004/02/skos/core#exactMatch>)* key2 }";

	/** The Constant SAP_ANNOTATION_SCHEMA. */
	public static final String SAP_ANNOTATION_SCHEMA = "http://www.sap.com/Protocols/SAPData";
	
	/** The Constant SAP_ANNOTATION_NS. */
	public static final String SAP_ANNOTATION_NS = "sap";
	
	/** The Constant SAP_LABEL. */
	private static final String SAP_LABEL = "label";
	
	/** The Constant SAP_HEADING. */
	private static final String SAP_HEADING = "heading";
	
	/** The Constant SAP_QUICKINFO. */
	private static final String SAP_QUICKINFO = "quickinfo";
	
	/** The Constant ODATA_NS. */
	public static final String  ODATA_NS ="OData";
	
	/** The Constant ODATA_DEFAULTNAMESPACE. */
	private static final String  ODATA_DEFAULTNAMESPACE ="defaultNamespace";
	
	/** The Constant ODATA_SUPPORTSCRIPTING. */
	private static final String  ODATA_SUPPORTSCRIPTING="supportScripting";
	
	/** The Constant ODATA_NAMESPACES. */
	private static final String  ODATA_NAMESPACES ="namespaces";
	
	/** The Constant ODATA_BASETYPE. */
	private static final String  ODATA_BASETYPE ="baseType";
	
	/** The Constant ODATA_FK. */
	private static final String  ODATA_FK ="FK";
	
	/** The Constant ODATA_SUBTYPE. */
	private static final String  ODATA_SUBTYPE ="subType";
	
	/** The Constant ODATA_ISPROXY. */
	private static final String  ODATA_ISPROXY ="isProxy";
	
	/** The Constant ODATA_ISDATASET. */
	private static final String  ODATA_ISDATASET ="isDataset";
	
	/** The Constant ODATA_ISPROPERTYPATH. */
	private static final String  ODATA_ISPROPERTYPATH ="isPropertyPath";
	
	/** The Constant ODATA_RDFTYPE. */
	private static final String  ODATA_RDFTYPE ="rdfType";
	
	/** The Constant ODATA_ISREIFIEDSTATEMENT. */
	private static final String  ODATA_ISREIFIEDSTATEMENT ="isReifiedStatement";
	
	/** The Constant ODATA_ISREIFIEDPREDICATE. */
	private static final String  ODATA_ISREIFIEDPREDICATE ="isReifiedPredicate";
	
	/** The Constant ODATA_ISREIFIEDSUBJECTPREDICATE. */
	private static final String  ODATA_ISREIFIEDSUBJECTPREDICATE ="isReifiedSubject";
	
	/** The Constant ODATA_ISREIFIEDOBJECTPREDICATE. */
	private static final String  ODATA_ISREIFIEDOBJECTPREDICATE ="isReifiedObject";
	
	/** The Constant ODATA_INVERSEOF. */
	private static final String  ODATA_INVERSEOF ="inverseOf";
	
	/** The Constant INOVA8_SCHEMA. */
	public static final String INOVA8_SCHEMA = "http://inova8.com/";
	
	/** The Constant INOVA8_NS. */
	public static final String INOVA8_NS = "http://inova8.com/";
	
	/** The Constant INOVA8. */
	public static final String INOVA8 = "inova8";
	
	/** The Constant RDF_SCHEMA. */
	public static final String RDF_SCHEMA = "http://www.w3.org/1999/02/22-rdf-syntax-ns";
	
	/** The Constant RDF_NS. */
	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	
	/** The Constant RDF. */
	public static final String RDF = "rdf";
	
	/** The Constant RDFS_SCHEMA. */
	public static final String RDFS_SCHEMA = "http://www.w3.org/2000/01/rdf-schema";
	
	/** The Constant RDFS_NS. */
	public static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
	
	/** The Constant RDFS. */
	public static final String RDFS = "rdfs";
	
	/** The Constant OWL_SCHEMA. */
	public static final String OWL_SCHEMA = "http://www.w3.org/2002/07/owl";
	
	/** The Constant OWL_NS. */
	public static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
	
	/** The Constant OWL. */
	public static final String OWL = "owl";
	
	/** The Constant XSD_SCHEMA. */
	public static final String XSD_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	
	/** The Constant XSD_NS. */
	public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
	
	/** The Constant XSD. */
	public static final String XSD = "xsd";
	
	/** The Constant URN_NS. */
	public static final String URN_NS = "urn:";
	
	/** The Constant URN. */
	public static final String URN = "urn";
	
	/** The Constant DC_NS. */
	public static final String DC_NS = "http://purl.org/dc/elements/1.1/";
	
	/** The Constant DC. */
	public static final String DC = "dc";

	/** The Constant XSD_STRING. */
	public static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";
	
	/** The Constant XSD_BOOLEAN. */
	public static final String XSD_BOOLEAN = "http://www.w3.org/2001/XMLSchema#boolean";
	
	/** The Constant XSD_DATE. */
	public static final String XSD_DATE = "http://www.w3.org/2001/XMLSchema#date";
	
	/** The Constant XSD_DATETIME. */
	public static final String XSD_DATETIME = "http://www.w3.org/2001/XMLSchema#dateTime";
	
	/** The Constant RDF_PLAIN_LITERAL. */
	public static final String RDF_PLAIN_LITERAL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral";
	
	/** The Constant LITERAL. */
	public static final String LITERAL = "Literal";
	
	/** The Constant RDF_LITERAL. */
	public static final String RDF_LITERAL = "http://www.w3.org/2000/01/rdf-schema#Literal";

	/** The Constant RDFS_RESOURCE. */
	public static final String RDFS_RESOURCE = "http://www.w3.org/2000/01/rdf-schema#Resource";
	
	/** The Constant RDFS_RESOURCE_NAME. */
	public static final String RDFS_RESOURCE_NAME = "Resource";
	
	/** The Constant RDFS_RESOURCE_FQN. */
	public static final FullQualifiedName  RDFS_RESOURCE_FQN= new FullQualifiedName(RDFS, RDFS_RESOURCE_NAME);	
	
	/** The Constant RDFS_RESOURCE_LABEL. */
	public static final String RDFS_RESOURCE_LABEL = "Resource";
	
	/** The Constant RDFS_CLASS. */
	public static final String RDFS_CLASS = "http://www.w3.org/2000/01/rdf-schema#Class";
	
	/** The Constant RDFS_CLASS_LABEL. */
	public static final String RDFS_CLASS_LABEL = "Class";
	
	/** The Constant RDFS_CLASS_TERM. */
	public static final String RDFS_CLASS_TERM = "class";
	
	/** The Constant OWL_THING. */
	public static final String OWL_THING = "http://www.w3.org/2002/07/owl#Thing";
	
	/** The Constant OWL_THING_NAME. */
	public static final String OWL_THING_NAME = "Thing";
	
	/** The Constant OWL_THING_FQN. */
	public static final FullQualifiedName  OWL_THING_FQN= new FullQualifiedName(OWL, OWL_THING_NAME);
	
	/** The Constant OWL_THING_LABEL. */
	public static final String OWL_THING_LABEL = "Thing";
	
	/** The Constant OWL_CLASS. */
	public static final String OWL_CLASS = "http://www.w3.org/2002/07/owl#Class";
	
	/** The Constant OWL_CLASS_LABEL. */
	public static final String OWL_CLASS_LABEL = "Class";
	
	/** The Constant RDF_PROPERTY. */
	public static final String RDF_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
	
	/** The Constant RDF_PROPERTY_LABEL. */
	public static final String RDF_PROPERTY_LABEL = "Property";
	
	/** The Constant RDF_PROPERTY_TERM. */
	public static final String RDF_PROPERTY_TERM = "property";
	
	/** The Constant OWL_OBJECTPROPERTY. */
	public static final String OWL_OBJECTPROPERTY = "http://www.w3.org/2002/07/owl#ObjectProperty";
	
	/** The Constant OWL_OBJECTPROPERTY_LABEL. */
	public static final String OWL_OBJECTPROPERTY_LABEL = "ObjectProperty";
	
	/** The Constant OWL_DATATYPEPROPERTY. */
	public static final String OWL_DATATYPEPROPERTY = "http://www.w3.org/2002/07/owl#DatatypeProperty";
	
	/** The Constant OWL_DATATYPEPROPERTY_LABEL. */
	public static final String OWL_DATATYPEPROPERTY_LABEL = "DatatypeProperty";
	
	/** The Constant OWL_ONTOLOGY. */
	public static final String OWL_ONTOLOGY = "http://www.w3.org/2002/07/owl#Ontology";
	
	/** The Constant OWL_ONTOLOGY_LABEL. */
	public static final String OWL_ONTOLOGY_LABEL = "Ontology";

	/** The Constant RDF_STATEMENT. */
	public static final String RDF_STATEMENT = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement";
	
	/** The Constant RDF_STATEMENT_LABEL. */
	public static final String RDF_STATEMENT_LABEL = "Statement";
	
	/** The Constant SUBJECT. */
	public static final String SUBJECT = "subjectId";
	
	/** The Constant RDF_SUBJECT. */
	public static final String RDF_SUBJECT = "http://www.w3.org/1999/02/22-rdf-syntax-ns#subjectId";
	
	/** The Constant RDF_SUBJECT_LABEL. */
	public static final String RDF_SUBJECT_LABEL = "subject";
	
	/** The Constant RDF_TYPE. */
	public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	
	/** The Constant RDF_TYPE_EDMNAME. */
	public static final String RDF_TYPE_EDMNAME = "rdf_type";
	
	/** The Constant RDF_TYPE_LABEL. */
	public static final String RDF_TYPE_LABEL = "has Type";
	
	/** The Constant RDF_INVERSE_TYPE. */
	public static final String RDF_INVERSE_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#hasInstance";
	
	/** The Constant RDF_INVERSE_TYPE_LABEL. */
	public static final String RDF_INVERSE_TYPE_LABEL = "has Instance";

	/** The Constant RDFS_DOMAIN. */
	public static final String RDFS_DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
	
	/** The Constant RDFS_DOMAIN_LABEL. */
	public static final String RDFS_DOMAIN_LABEL = "domain";
	
	/** The Constant RDFS_SUBCLASSOF. */
	public static final String RDFS_SUBCLASSOF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
	
	/** The Constant RDFS_SUBCLASSOF_LABEL. */
	public static final String RDFS_SUBCLASSOF_LABEL = "subClassOf";
	
	/** The Constant RDFS_RANGE. */
	public static final String RDFS_RANGE = "http://www.w3.org/2000/01/rdf-schema#range";
	
	/** The Constant RDFS_RANGE_LABEL. */
	public static final String RDFS_RANGE_LABEL = "range";

	/** The Constant RDFS_LABEL. */
	public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
	
	/** The Constant RDFS_LABEL_LABEL. */
	public static final String RDFS_LABEL_LABEL = "label";

	/** The Constant RDFS_COMMENT. */
	public static final String RDFS_COMMENT = "http://www.w3.org/2000/01/rdf-schema#comment";
	
	/** The Constant RDFS_COMMENT_LABEL. */
	public static final String RDFS_COMMENT_LABEL = "comment";

	/** The Constant OWL_EQUIVALENTPROPERTY_LABEL. */
	private static final String OWL_EQUIVALENTPROPERTY_LABEL = "equivalentProperty";
	
	/** The Constant OWL_IMPORTS. */
	public static final String OWL_IMPORTS = "http://www.w3.org/2002/07/owl#imports";
	
	/** The Constant OWL_IMPORTS_LABEL. */
	public static final String OWL_IMPORTS_LABEL = "imports";
		
	/** The Constant RDF_SUBJECTPREDICATE. */
	public static final String RDF_SUBJECTPREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#SubjectPredicate";
	
	/** The Constant RDF_OBJECTPREDICATE. */
	public static final String RDF_OBJECTPREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Fact";	
	
	/** The Constant RDF_VALUE. */
	public static final String RDF_VALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Term";	
	
	/** The Constant RDF_HASSUBJECTS. */
	public static final String RDF_HASSUBJECTS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#subjects";
	
	/** The Constant RDF_ISPREDICATEOF. */
	public static final String RDF_ISPREDICATEOF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#isPropertyOf";
	
	/** The Constant RDF_ISOBJECTIVE. */
	public static final String RDF_ISOBJECTIVE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#isObjectOf";
	
	/** The Constant RDF_HASDATAVALUE. */
	public static final String RDF_HASDATAVALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#rdf_literal";
	
	/** The Constant RDF_OBJECTVALUE. */
	public static final String RDF_OBJECTVALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#rdf_object";
	
	/** The Constant RDF_HASOBJECTVALUE. */
	public static final String RDF_HASOBJECTVALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#resource";
	
	/** The Constant RDF_HASVALUES. */
	public static final String RDF_HASVALUES = "http://www.w3.org/1999/02/22-rdf-syntax-ns#terms";
	
	/** The Constant RDF_HASPREDICATE. */
	public static final String RDF_HASPREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#property";
	
	/** The Constant RDF_HASFACTS. */
	public static final String RDF_HASFACTS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#facts";
	
	/** The Constant RDF_FACTVALUE. */
	public static final String RDF_FACTVALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#factValue";	
	
	/** The Constant RDF_FACTVALUE_VALUE. */
	public static final String RDF_FACTVALUE_VALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#value";	
	
	/** The Constant RDF_FACTVALUE_SCRIPT. */
	public static final String RDF_FACTVALUE_SCRIPT = "http://www.w3.org/1999/02/22-rdf-syntax-ns#script";	
	
	/** The Constant RDF_FACTVALUE_TRACE. */
	public static final String RDF_FACTVALUE_TRACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#trace";	
	
	/** The Constant RDF_SUBJECTPREDICATE_LABEL. */
	public static final String RDF_SUBJECTPREDICATE_LABEL = "SubjectPredicate";
	
	/** The Constant RDF_OBJECTPREDICATE_LABEL. */
	public static final String RDF_OBJECTPREDICATE_LABEL = "Fact";	
	
	/** The Constant RDF_VALUE_LABEL. */
	public static final String RDF_VALUE_LABEL = "Term";	
	
	/** The Constant RDF_HASSUBJECTS_LABEL. */
	public static final String RDF_HASSUBJECTS_LABEL = "rdf_subjects";
	
	/** The Constant RDF_ISPREDICATEOF_LABEL. */
	public static final String RDF_ISPREDICATEOF_LABEL = "rdf_isPropertyOf";
	
	/** The Constant RDF_ISOBJECTIVE_LABEL. */
	public static final String RDF_ISOBJECTIVE_LABEL = "rdf_isObjectOf";
	
	/** The Constant RDF_HASDATAVALUE_LABEL. */
	public static final String RDF_HASDATAVALUE_LABEL = "rdf_literal";
	
	/** The Constant RDF_OBJECTVALUE_LABEL. */
	public static final String RDF_OBJECTVALUE_LABEL = "rdf_object";
	
	/** The Constant RDF_HASOBJECTVALUE_LABEL. */
	public static final String RDF_HASOBJECTVALUE_LABEL = "rdf_resource";
	
	/** The Constant RDF_HASVALUES_LABEL. */
	public static final String RDF_HASVALUES_LABEL = "rdf_terms";
	
	/** The Constant RDF_HASPREDICATE_LABEL. */
	public static final String RDF_HASPREDICATE_LABEL = "rdf_property";
	
	/** The Constant RDF_HASFACTS_LABEL. */
	public static final String RDF_HASFACTS_LABEL = "rdf_facts";

	/** The Constant RDF_FACTVALUE_LABEL. */
	public static final String RDF_FACTVALUE_LABEL = "FactValue";	
	
	/** The Constant RDF_FACTVALUE_VALUE_LABEL. */
	public static final String RDF_FACTVALUE_VALUE_LABEL = "value";	
//	public static final String RDF_FACTVALUE_LABEL = "FactValue";	
/** The Constant RDF_FACTVALUE_SCRIPT_LABEL. */
//	public static final String RDF_FACTVALUE_VALUE_LABEL = "value";	
	public static final String RDF_FACTVALUE_SCRIPT_LABEL = "script";	
	
	/** The Constant RDF_FACTVALUE_TRACE_LABEL. */
	public static final String RDF_FACTVALUE_TRACE_LABEL = "trace";	
	
	/** The Constant SPARQL_UNDEF. */
	public static final String SPARQL_UNDEF = "UNDEF";

	/** The Constant ONTOLOGY. */
	private static final String ONTOLOGY = "Ontology";
	
	/** The Constant DATATYPE. */
	private static final String DATATYPE = "Datatype";
	
	/** The Constant PROPERTY. */
	public static final String PROPERTY = "Property";

	/** The Constant LANGSTRING. */
	public static final String LANGSTRING = "langString";
	
	/** The Constant LANG. */
	public static final String LANG = "lang";
	
	/** The Constant VALUE. */
	public static final String VALUE = "value";

	/** The Constant FACT. */
	public static final String FACT = "fact";
	
	/** The Constant FACTS. */
	public static final String FACTS = "facts";
	
	/** The Constant DATAVALUES. */
	public static final String DATAVALUES = "dataValues";
	
	/** The Constant OBJECTVALUES. */
	public static final String OBJECTVALUES = "objectValues";

	
	/** The Constant INVERSEOF. */
	private static final String INVERSEOF = "inverseOf ";
	
	/** The Constant SPARQL_MODEL. */
	public static final String SPARQL_MODEL = "SparqlModel";

	/** The Constant ENTITYCONTAINER. */
	public static final String ENTITYCONTAINER = "Container";
	
	/** The Constant ENTITYCONTAINERNAMESPACE. */
	public static final String ENTITYCONTAINERNAMESPACE = "Instances";

	/*
	 * Modifiers for classes and properties as they appear in $metadata
	*/
	
	/*	The following is from the odata BNF revealing that only '_' non-alpha-numeric characters are allowed. Thus the ~ cannot be used as a prefix separator for the odata names as it is used in the results
 
 	; Note: this pattern is overly restrictive, the normative definition is type TSimpleIdentifier in OData EDM XML Schema
	odataIdentifier             = identifierLeadingCharacter *127identifierCharacter
	identifierLeadingCharacter  = ALPHA / "_"         ; plus Unicode characters from the categories L or Nl
	identifierCharacter         = ALPHA / "_" / DIGIT ; plus Unicode characters from the categories L, Nl, Nd, Mn, Mc, Pc, or Cf
	*/
	
	/** The Constant PREDICATE_SEPARATOR. */
	public static final String PREDICATE_SEPARATOR = "_";
	
	/** The Constant CLASS_SEPARATOR. */
	public static final String CLASS_SEPARATOR ="_";

	/** The Constant CLASS_LABEL_PREFIX. */
	public static final String CLASS_LABEL_PREFIX = "";
	
	/** The Constant PROPERTY_LABEL_PREFIX. */
	public static final String PROPERTY_LABEL_PREFIX = "";
	
	/** The Constant NAVIGATIONPROPERTY_LABEL_PREFIX. */
	/*	
	 * Although it is legal for FunctionImports to share names with EntitySets, Openui5 has a problem
	 */
	public static final String NAVIGATIONPROPERTY_LABEL_PREFIX = "";
	
	/** The Constant FUNCTION_POSTFIX. */
	public static final String FUNCTION_POSTFIX ="_fn";//  "";//
	
	/** The Constant BLANKNODE. */
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
	public static final String BLANKNODE = "_~";
	
	/** The Constant BLANKNODE_RDF. */
	public static final String BLANKNODE_RDF = "_:";
	
	/** The Constant QNAME_SEPARATOR. */
	public static final String QNAME_SEPARATOR = "~";
	
	/** The Constant QNAME_SEPARATOR_ENCODED. */
	public static final String QNAME_SEPARATOR_ENCODED = "~";
	
	/** The Constant QNAME_SEPARATOR_RDF. */
	public static final String QNAME_SEPARATOR_RDF = ":";
	
	/** The Constant RDF_LANG_STRING. */
	public static final Object RDF_LANG_STRING = RDF + "." + LANGSTRING;

	/** The Constant URI_ASSOCIATIONQUERY. */
	public static final Value URI_ASSOCIATIONQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#associationQuery");
	
	/** The Constant URI_CLASSQUERY. */
	public static final Value URI_CLASSQUERY = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#classQuery");

	/** The Constant URI_DATATYPEQUERY. */
	public static final Value URI_DATATYPEQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#datatypeQuery");
	
	/** The Constant URI_GRAPHQUERY. */
	public static final Value URI_GRAPHQUERY = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#graphQuery");
	
	/** The Constant URI_INVERSEASSOCIATIONQUERY. */
	public static final Value URI_INVERSEASSOCIATIONQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#inverseAssociationQuery");
	
	/** The Constant URI_OPERATIONARGUMENTQUERY. */
	public static final Value URI_OPERATIONARGUMENTQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#operationArgumentQuery");
	
	/** The Constant URI_OPERATIONASSOCIATIONRESULTQUERY. */
	public static final Value URI_OPERATIONASSOCIATIONRESULTQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#operationAssociationResultQuery");
	
	/** The Constant URI_OPERATIONPROPERTYRESULTQUERY. */
	public static final Value URI_OPERATIONPROPERTYRESULTQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#operationPropertyResultQuery");
	
	/** The Constant URI_OPERATIONQUERY. */
	public static final Value URI_OPERATIONQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#operationQuery");
	
	/** The Constant URI_PREFIXQUERY. */
	public static final Value URI_PREFIXQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#prefixQuery");
	
	/** The Constant URI_PROPERTYQUERY. */
	public static final Value URI_PROPERTYQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyQuery");
	
	/** The Constant URI_PROPERTY_DOMAINS_QUERY. */
	public static final Value URI_PROPERTY_DOMAINS_QUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyQuery_Domains");
	
	/** The Constant URI_PROPERTY_RANGES_QUERY. */
	public static final Value URI_PROPERTY_RANGES_QUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyQuery_Ranges");
	
	/** The Constant URI_PROPERTY_CARDINALITY_QUERY. */
	public static final Value URI_PROPERTY_CARDINALITY_QUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyQuery_Cardinality");
	
	/** The Constant URI_REPOSITORYQUERY. */
	public static final Value URI_REPOSITORYQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#repositoryQuery");
	
	/** The Constant URI_NODESHAPESQUERY. */
	public static final Value URI_NODESHAPESQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#nodeShapesQuery");
	
	/** The Constant URI_PROPERTYSHAPESQUERY. */
	public static final Value URI_PROPERTYSHAPESQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyShapesQuery");
	
	/** The Constant URI_REIFIEDSTATEMENTQUERY. */
	public static final Value URI_REIFIEDSTATEMENTQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#reifiedStatementQuery");
	
	/** The Constant URI_HALYARD_SEARCH. */
	public static final Value URI_HALYARD_SEARCH = valueFactoryImpl
			.createIRI("http://merck.github.io/Halyard/ns#search");
	
	/** The Constant URI_LUCENE_MATCHES. */
	public static final Value URI_LUCENE_MATCHES = valueFactoryImpl
			.createIRI("http://www.openrdf.org/contrib/lucenesail#matches");
	
	/** The Constant URI_LUCENE_QUERY. */
	public static final Value URI_LUCENE_QUERY = valueFactoryImpl
			.createIRI("http://www.openrdf.org/contrib/lucenesail#query");

	/** The Constant SAP_LABEL_FQN. */
	public final static String SAP_LABEL_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_LABEL;
	
	/** The Constant SAP_HEADING_FQN. */
	public final static String SAP_HEADING_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_HEADING;
	
	/** The Constant SAP_QUICKINFO_FQN. */
	public final static String SAP_QUICKINFO_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_QUICKINFO;
	
	/** The Constant ODATA_DEFAULTNAMESPACE_FQN. */
	public final static String ODATA_DEFAULTNAMESPACE_FQN= RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_DEFAULTNAMESPACE;
	
	/** The Constant ODATA_SUPPORTSCRIPTING_FQN. */
	public final static String ODATA_SUPPORTSCRIPTING_FQN= RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_SUPPORTSCRIPTING;
	
	/** The Constant ODATA_NAMESPACES_FQN. */
	public final static String ODATA_NAMESPACES_FQN= RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_NAMESPACES;
	
	/** The Constant ODATA_BASETYPE_FQN. */
	public static final String ODATA_BASETYPE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_BASETYPE;
	
	/** The Constant ODATA_FK_FQN. */
	public static final String ODATA_FK_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_FK;
	
	/** The Constant ODATA_SUBTYPE_FQN. */
	public static final String ODATA_SUBTYPE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_SUBTYPE;
	
	/** The Constant ODATA_ISPROXY_FQN. */
	public static final String ODATA_ISPROXY_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISPROXY;
	
	/** The Constant ODATA_ISDATASET_FQN. */
	public static final String ODATA_ISDATASET_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISDATASET;
	
	/** The Constant ODATA_ISPROPERTYPATH_FQN. */
	public static final String ODATA_ISPROPERTYPATH_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISPROPERTYPATH;
	
	/** The Constant ODATA_RDFTYPE_FQN. */
	public static final String ODATA_RDFTYPE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_RDFTYPE;
	
	/** The Constant ODATA_ISREIFIEDSTATEMENT_FQN. */
	public static final String ODATA_ISREIFIEDSTATEMENT_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISREIFIEDSTATEMENT;
	
	/** The Constant ODATA_ISREIFIEDPREDICATE_FQN. */
	public static final String ODATA_ISREIFIEDPREDICATE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISREIFIEDPREDICATE;
	
	/** The Constant ODATA_ISREIFIEDSUBJECTPREDICATE_FQN. */
	public static final String ODATA_ISREIFIEDSUBJECTPREDICATE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISREIFIEDSUBJECTPREDICATE;
	
	/** The Constant ODATA_ISREIFIEDOBJECTPREDICATE_FQN. */
	public static final String ODATA_ISREIFIEDOBJECTPREDICATE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISREIFIEDOBJECTPREDICATE;
	
	/** The Constant ODATA_INVERSEOF_FQN. */
	public static final String ODATA_INVERSEOF_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_INVERSEOF;
	
	/** The Constant RDFS_CLASS_FQN. */
	//public static final String ODATA_SCRIPT_FQN = RdfConstants.RDF + "." + RdfConstants.ODATA_SCRIPT;
	public final static String RDFS_CLASS_FQN = RdfConstants.RDFS + "." + RdfConstants.RDFS_CLASS_TERM;
	
	/** The Constant PROPERTY_FQN. */
	public final static String PROPERTY_FQN = RdfConstants.RDF + "." + RdfConstants.RDF_PROPERTY_TERM;
	
	/** The Constant DATATYPE_FQN. */
	public final static String DATATYPE_FQN = RdfConstants.RDFS + "." + RdfConstants.DATATYPE;
	
	/** The Constant INVERSEOF_FQN. */
	public final static String INVERSEOF_FQN = RdfConstants.OWL + "." + RdfConstants.INVERSEOF;
	
	/** The Constant OWL_EQUIVALENTPROPERTY_FQN. */
	public final static String OWL_EQUIVALENTPROPERTY_FQN = RdfConstants.OWL + "."
			+ RdfConstants.OWL_EQUIVALENTPROPERTY_LABEL;
	
	/** The Constant ONTOLOGY_FQN. */
	public final static String ONTOLOGY_FQN = RdfConstants.OWL + "." + RdfConstants.ONTOLOGY;

	/** The Constant sapLabelTerm. */
	private final static CsdlTerm sapLabelTerm = new CsdlTerm().setName(RdfConstants.SAP_LABEL).setType("Edm.String");
	
	/** The Constant sapheadingTerm. */
	private final static CsdlTerm sapheadingTerm = new CsdlTerm().setName(RdfConstants.SAP_HEADING).setType("Edm.String");
	
	/** The Constant sapquickinfoTerm. */
	private final static CsdlTerm sapquickinfoTerm = new CsdlTerm().setName(RdfConstants.SAP_QUICKINFO).setType("Edm.String");
	
	/** The Constant odataDefaultNamespace. */
	private final static CsdlTerm odataDefaultNamespace = new CsdlTerm().setName(RdfConstants.ODATA_DEFAULTNAMESPACE).setType("Edm.String");
	
	/** The Constant odataSupportScripting. */
	private final static CsdlTerm odataSupportScripting = new CsdlTerm().setName(RdfConstants.ODATA_SUPPORTSCRIPTING).setType("Edm.Boolean");
	
	/** The Constant odataNamespaces. */
	private final static CsdlTerm odataNamespaces = new CsdlTerm().setName(RdfConstants.ODATA_NAMESPACES).setType("Edm.String");
	
	/** The Constant odatabaseType. */
	private final static CsdlTerm odatabaseType = new CsdlTerm().setName(RdfConstants.ODATA_BASETYPE).setType("Edm.String");
	
	/** The Constant odataFK. */
	private final static CsdlTerm odataFK = new CsdlTerm().setName(RdfConstants.ODATA_FK).setType("Edm.String");
	
	/** The Constant odataSubType. */
	private final static CsdlTerm odataSubType = new CsdlTerm().setName(RdfConstants.ODATA_SUBTYPE).setType("Edm.String");
	
	/** The Constant odataIsProxy. */
	private final static CsdlTerm odataIsProxy = new CsdlTerm().setName(RdfConstants.ODATA_ISPROXY).setType("Edm.String");
	
	/** The Constant odataIsDataset. */
	private final static CsdlTerm odataIsDataset = new CsdlTerm().setName(RdfConstants.ODATA_ISDATASET).setType("Edm.String");
	
	/** The Constant odataIsPropertyPath. */
	private final static CsdlTerm odataIsPropertyPath = new CsdlTerm().setName(RdfConstants.ODATA_ISPROPERTYPATH).setType("Edm.String");
	
	/** The Constant odataRdfType. */
	private final static CsdlTerm odataRdfType = new CsdlTerm().setName(RdfConstants.ODATA_RDFTYPE).setType("Edm.String");
	
	/** The Constant odataIsReifiedStatement. */
	private final static CsdlTerm odataIsReifiedStatement = new CsdlTerm().setName(RdfConstants.ODATA_ISREIFIEDSTATEMENT).setType("Edm.String");
	
	/** The Constant odataIsReifiedPredicate. */
	private final static CsdlTerm odataIsReifiedPredicate = new CsdlTerm().setName(RdfConstants.ODATA_ISREIFIEDPREDICATE).setType("Edm.String");
	
	/** The Constant odataIsReifiedSubjectPredicate. */
	private final static CsdlTerm odataIsReifiedSubjectPredicate = new CsdlTerm().setName(RdfConstants.ODATA_ISREIFIEDSUBJECTPREDICATE).setType("Edm.String");
	
	/** The Constant odataIsReifiedObjectPredicate. */
	private final static CsdlTerm odataIsReifiedObjectPredicate = new CsdlTerm().setName(RdfConstants.ODATA_ISREIFIEDOBJECTPREDICATE).setType("Edm.String");
	
	/** The Constant odataInverseOf. */
	private final static CsdlTerm odataInverseOf = new CsdlTerm().setName(RdfConstants.ODATA_INVERSEOF).setType("Edm.String");
	
	/** The Constant rdfsClassTerm. */
	//private final static CsdlTerm odataScript = new CsdlTerm().setName(RdfConstants.ODATA_SCRIPT).setType("Edm.String");
	private final static CsdlTerm rdfsClassTerm = new CsdlTerm().setName(RdfConstants.RDFS_CLASS_TERM).setType("Edm.String");
	
	/** The Constant rdfPropertyTerm. */
	private final static CsdlTerm rdfPropertyTerm = new CsdlTerm().setName(RdfConstants.RDF_PROPERTY_TERM).setType("Edm.String");
	
	/** The Constant rdfsDatatypeTerm. */
	private final static CsdlTerm rdfsDatatypeTerm = new CsdlTerm().setName(RdfConstants.DATATYPE).setType("Edm.String");
	
	/** The Constant owlInverseOfTerm. */
	private final static CsdlTerm owlInverseOfTerm = new CsdlTerm().setName(RdfConstants.INVERSEOF).setType("Edm.String");
	
	/** The Constant owlOntologyTerm. */
	private final static CsdlTerm owlOntologyTerm = new CsdlTerm().setName(RdfConstants.ONTOLOGY).setType("Edm.String");

	/** The Constant RDF_DATATYPES. */
	public final static List<String> RDF_DATATYPES = new ArrayList<String>();

	/** The Constant TERMS. */
	public static final TreeMap<String, CsdlTerm> TERMS = new TreeMap<String, CsdlTerm>();
	
	/** The Constant SAPTERMS. */
	public static final ArrayList< CsdlTerm> SAPTERMS = new ArrayList<CsdlTerm>();
	
	/** The Constant ODATATERMS. */
	public static final ArrayList< CsdlTerm> ODATATERMS = new ArrayList<CsdlTerm>();
	
	/** The Constant RDFTERMS. */
	public static final ArrayList< CsdlTerm> RDFTERMS = new ArrayList<CsdlTerm>();
	
	/** The Constant RDFSTERMS. */
	public static final ArrayList< CsdlTerm> RDFSTERMS = new ArrayList<CsdlTerm>();
	
	/** The Constant OWLTERMS. */
	public static final ArrayList< CsdlTerm> OWLTERMS = new ArrayList<CsdlTerm>();


	/**
	 * Gets the meta queries.
	 *
	 * @return the meta queries
	 */
	public static Hashtable<Value, String> getMetaQueries() {
		return metaQueries;
	}

	/**
	 * Sets the meta queries.
	 *
	 * @param newMetaQueries the new meta queries
	 */
	public static void setMetaQueries(Hashtable<Value, String> newMetaQueries) {
		metaQueries = newMetaQueries;
	}

	/**
	 * Gets the meta models.
	 *
	 * @return the meta models
	 */
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

			odata4sparqlFile = repositoryManagerDirPath + "ontologies/odata4sparql.v2.7.8.rdf";
			rdfFile = repositoryManagerDirPath + "ontologies/22-rdf-syntax-ns.ttl";
			rdfsFile = repositoryManagerDirPath + "ontologies/rdf-schema.ttl";
			sailFile = repositoryManagerDirPath + "ontologies/sail.rdf";
			spFile = repositoryManagerDirPath + "ontologies/sp.ttl";
			contextmenuFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.contextmenu.v2.0.2.rdf";
			olgapFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.olgap.v2.0.0.rdf";
			searchFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.lucenesearch.v3.0.0.rdf";
			changeFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.change.v1.0.0.rdf";
			scriptFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.script.v1.0.1.rdf";


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
		TERMS.put(RdfConstants.ODATA_DEFAULTNAMESPACE, odataDefaultNamespace);
		TERMS.put(RdfConstants.ODATA_SUPPORTSCRIPTING, odataSupportScripting);
		TERMS.put(RdfConstants.ODATA_NAMESPACES, odataNamespaces);
		TERMS.put(RdfConstants.ODATA_BASETYPE, odatabaseType);
		TERMS.put(RdfConstants.ODATA_FK, odataFK);
		TERMS.put(RdfConstants.ODATA_SUBTYPE, odataSubType);
		TERMS.put(RdfConstants.ODATA_ISPROXY, odataIsProxy);
		TERMS.put(RdfConstants.ODATA_ISDATASET, odataIsDataset);
		TERMS.put(RdfConstants.ODATA_ISPROPERTYPATH, odataIsPropertyPath);
		TERMS.put(RdfConstants.ODATA_RDFTYPE, odataRdfType);
		TERMS.put(RdfConstants.ODATA_ISREIFIEDSTATEMENT, odataIsReifiedStatement);
		TERMS.put(RdfConstants.ODATA_ISREIFIEDPREDICATE, odataIsReifiedPredicate);
		TERMS.put(RdfConstants.ODATA_ISREIFIEDSUBJECTPREDICATE, odataIsReifiedSubjectPredicate);
		TERMS.put(RdfConstants.ODATA_ISREIFIEDOBJECTPREDICATE, odataIsReifiedObjectPredicate);
		TERMS.put(RdfConstants.ODATA_INVERSEOF, odataInverseOf);
		TERMS.put(RdfConstants.RDFS_CLASS_TERM, rdfsClassTerm);
		TERMS.put(RdfConstants.RDF_PROPERTY_TERM, rdfPropertyTerm);
		TERMS.put(RdfConstants.DATATYPE, rdfsDatatypeTerm);
		TERMS.put(RdfConstants.INVERSEOF, owlInverseOfTerm);
		TERMS.put(RdfConstants.ONTOLOGY, owlOntologyTerm);
		
		SAPTERMS.add(sapLabelTerm);
		SAPTERMS.add(sapheadingTerm);
		SAPTERMS.add(sapquickinfoTerm);
		ODATATERMS.add(odataDefaultNamespace);
		ODATATERMS.add(odataSupportScripting);
		ODATATERMS.add(odataNamespaces);
		ODATATERMS.add(odatabaseType);
		ODATATERMS.add(odataFK);
		ODATATERMS.add(odataSubType);
		ODATATERMS.add(odataIsProxy);
		ODATATERMS.add(odataIsDataset);
		ODATATERMS.add(odataIsPropertyPath);
		ODATATERMS.add(odataRdfType);
		ODATATERMS.add(odataIsReifiedStatement);
		ODATATERMS.add(odataIsReifiedPredicate);
		ODATATERMS.add(odataIsReifiedSubjectPredicate);
		ODATATERMS.add(odataIsReifiedObjectPredicate);
		ODATATERMS.add(odataInverseOf);
		RDFTERMS.add(rdfPropertyTerm);
		RDFSTERMS.add(rdfsClassTerm);
		RDFSTERMS.add(rdfsDatatypeTerm);
		OWLTERMS.add(owlInverseOfTerm);
		OWLTERMS.add(owlOntologyTerm);
		
		

	}
}
