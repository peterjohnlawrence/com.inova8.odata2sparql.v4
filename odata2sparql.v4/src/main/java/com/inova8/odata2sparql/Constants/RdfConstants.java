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
import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;

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

	public static final String DEFAULTINSERTGRAPH = "http://insertGraph";
	
	public static final int DATE_PRECISION = 3;
	public static final int DECIMAL_SCALE = 27;//10; Fixes #119
	public static final String DEFAULTCONFIG = "/var/opt/inova8/odata2sparql/";
	public static final String DEFAULTFOLDER = ".default";

	public static final String RESET = "$reset";
	public static final String RELOAD = "$reload";
	public static final String LOGS = "$logs";
	public static final String CHANGES = "$changes";
	public static final String WILDCARD = "*";
	public static final String UNDEFVALUE = "UNDEF";
	public static final String PREFIXSEPARATOR = ".";
	
	
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
	public static String contextmenuFile;
	public static String searchFile;
	public static String olgapFile;
	public static String changeFile;

	public final static Value RDFSModel = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#RDFSModel");

	public static final String TARGETENTITY = "http://targetEntity";
	public static final String ASSERTEDTYPE = "http://assertedType";
	public static final String ASSERTEDSHAPE = "http://assertedShape";
	public static final String MATCHING = "http://matching";
	public static final String COUNT = "http://count";
	public static final String PREFIX = "j";
	public static final String PROPERTY_POSTFIX = "_value";
	public static final String PLURAL = "s";
	public static final String SHAPE_POSTFIX = "_shape";//"";//

	
	public static final String DEFAULTMATCH = "{ key1 (<http://www.w3.org/2004/02/skos/core#exactMatch> | ^ <http://www.w3.org/2004/02/skos/core#exactMatch>)* key2 }";


	public static final String SAP_ANNOTATION_SCHEMA = "http://www.sap.com/Protocols/SAPData";
	private static final String SAP_ANNOTATION_NS = "sap";
	private static final String SAP_LABEL = "label";
	private static final String SAP_HEADING = "heading";
	private static final String SAP_QUICKINFO = "quickinfo";
	
	private static final String  ODATA_NS ="odata";
	private static final String  ODATA_DEFAULTNAMESPACE ="defaultNamespace";
	private static final String  ODATA_NAMESPACES ="namespaces";
	private static final String  ODATA_BASETYPE ="baseType";
	private static final String  ODATA_FK ="FK";
	private static final String  ODATA_SUBTYPE ="subType";
	private static final String  ODATA_ISPROXY ="isProxy";
	private static final String  ODATA_ISDATASET ="isDataset";
	private static final String  ODATA_ISPROPERTYPATH ="isPropertyPath";
	private static final String  ODATA_RDFTYPE ="rdfType";
	private static final String  ODATA_ISREIFIEDSTATEMENT ="isReifiedStatement";
	private static final String  ODATA_ISREIFIEDPREDICATE ="isReifiedPredicate";
	private static final String  ODATA_ISREIFIEDSUBJECTPREDICATE ="isReifiedSubject";
	private static final String  ODATA_ISREIFIEDOBJECTPREDICATE ="isReifiedObject";
	private static final String  ODATA_INVERSEOF ="inverseOf";
		
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
	public static final String URN_NS = "urn:";
	public static final String URN = "urn";
	
	public static final String DC_NS = "http://purl.org/dc/elements/1.1/";
	public static final String DC = "dc";

	public static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";
	public static final String XSD_DATE = "http://www.w3.org/2001/XMLSchema#date";
	public static final String XSD_DATETIME = "http://www.w3.org/2001/XMLSchema#dateTime";
	public static final String RDF_PLAIN_LITERAL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral";
	public static final String LITERAL = "Literal";
	public static final String RDF_LITERAL = "http://www.w3.org/2000/01/rdf-schema#Literal";

	public static final String RDFS_RESOURCE = "http://www.w3.org/2000/01/rdf-schema#Resource";
	public static final String RDFS_RESOURCE_LABEL = "Resource";
	public static final String RDFS_CLASS = "http://www.w3.org/2000/01/rdf-schema#Class";
	public static final String RDFS_CLASS_LABEL = "Class";
	public static final String RDFS_CLASS_TERM = "class";
	public static final String OWL_THING = "http://www.w3.org/2002/07/owl#Thing";
	public static final String OWL_THING_LABEL = "Thing";
	public static final String OWL_CLASS = "http://www.w3.org/2002/07/owl#Class";
	public static final String OWL_CLASS_LABEL = "Class";
	public static final String RDF_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
	public static final String RDF_PROPERTY_LABEL = "Property";
	public static final String RDF_PROPERTY_TERM = "property";
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
	public static final String RDF_TYPE_EDMNAME = "rdf_type";
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

	public static final String RDFS_COMMENT = "http://www.w3.org/2000/01/rdf-schema#comment";
	public static final String RDFS_COMMENT_LABEL = "comment";

	
	private static final String OWL_EQUIVALENTPROPERTY_LABEL = "equivalentProperty";
	public static final String OWL_IMPORTS = "http://www.w3.org/2002/07/owl#imports";
	public static final String OWL_IMPORTS_LABEL = "imports";
	
	
	public static final String RDF_SUBJECTPREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#SubjectPredicate";
	public static final String RDF_OBJECTPREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Fact";	
	public static final String RDF_VALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Term";	
	public static final String RDF_HASSUBJECTS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#subjects";
	public static final String RDF_ISPREDICATEOF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#isPropertyOf";
	public static final String RDF_ISOBJECTIVE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#isObjectOf";
	public static final String RDF_HASDATAVALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#rdf_literal";
	public static final String RDF_OBJECTVALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#rdf_object";
	public static final String RDF_HASOBJECTVALUE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#resource";
	public static final String RDF_HASVALUES = "http://www.w3.org/1999/02/22-rdf-syntax-ns#terms";
	public static final String RDF_HASPREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#property";
	public static final String RDF_HASFACTS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#facts";

	public static final String RDF_SUBJECTPREDICATE_LABEL = "SubjectPredicate";
	public static final String RDF_OBJECTPREDICATE_LABEL = "Fact";	
	public static final String RDF_VALUE_LABEL = "Term";	
	public static final String RDF_HASSUBJECTS_LABEL = "rdf_subjects";
	public static final String RDF_ISPREDICATEOF_LABEL = "rdf_isPropertyOf";
	public static final String RDF_ISOBJECTIVE_LABEL = "rdf_isObjectOf";
	public static final String RDF_HASDATAVALUE_LABEL = "rdf_literal";
	public static final String RDF_OBJECTVALUE_LABEL = "rdf_object";
	public static final String RDF_HASOBJECTVALUE_LABEL = "rdf_resource";
	public static final String RDF_HASVALUES_LABEL = "rdf_terms";
	public static final String RDF_HASPREDICATE_LABEL = "rdf_property";
	public static final String RDF_HASFACTS_LABEL = "rdf_facts";

	public static final String SPARQL_UNDEF = "UNDEF";

	private static final String ONTOLOGY = "Ontology";
	private static final String DATATYPE = "Datatype";
	public static final String PROPERTY = "Property";

	public static final String LANGSTRING = "langString";
	public static final String LANG = "lang";
	public static final String VALUE = "value";

	public static final String FACT = "fact";
	public static final String FACTS = "facts";
	public static final String DATAVALUES = "dataValues";
	public static final String OBJECTVALUES = "objectValues";

	
	private static final String INVERSEOF = "inverseOf ";
	public static final String SPARQL_MODEL = "SparqlModel";

	public static final String ENTITYCONTAINER = "Container";
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
	
	public static final String PREDICATE_SEPARATOR = "_";
	public static final String CLASS_SEPARATOR ="_";

	public static final String CLASS_LABEL_PREFIX = "";
	public static final String PROPERTY_LABEL_PREFIX = "";
	
	/*	
	 * Although it is legal for FunctionImports to share names with EntitySets, Openui5 has a problem
	 */
	public static final String NAVIGATIONPROPERTY_LABEL_PREFIX = "";
	public static final String FUNCTION_POSTFIX ="_fn";//  "";//
	
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
	public static final String BLANKNODE_RDF = "_:";
	public static final String QNAME_SEPARATOR = "~";
	public static final String QNAME_SEPARATOR_ENCODED = "~";
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
	public static final Value URI_NODESHAPESQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#nodeShapesQuery");
	public static final Value URI_PROPERTYSHAPESQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#propertyShapesQuery");
	public static final Value URI_REIFIEDSTATEMENTQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#reifiedStatementQuery");
	public static final Value URI_HALYARD_SEARCH = valueFactoryImpl
			.createIRI("http://merck.github.io/Halyard/ns#search");
	public static final Value URI_LUCENE_MATCHES = valueFactoryImpl
			.createIRI("http://www.openrdf.org/contrib/lucenesail#matches");
	public static final Value URI_LUCENE_QUERY = valueFactoryImpl
			.createIRI("http://www.openrdf.org/contrib/lucenesail#query");

	public final static String SAP_LABEL_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_LABEL;
	public final static String SAP_HEADING_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_HEADING;
	public final static String SAP_QUICKINFO_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_QUICKINFO;
	public final static String ODATA_DEFAULTNAMESPACE_FQN= RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_DEFAULTNAMESPACE;
	public final static String ODATA_NAMESPACES_FQN= RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_NAMESPACES;
	public static final String ODATA_BASETYPE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_BASETYPE;
	public static final String ODATA_FK_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_FK;
	public static final String ODATA_SUBTYPE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_SUBTYPE;
	public static final String ODATA_ISPROXY_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISPROXY;
	public static final String ODATA_ISDATASET_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISDATASET;
	public static final String ODATA_ISPROPERTYPATH_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISPROPERTYPATH;
	public static final String ODATA_RDFTYPE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_RDFTYPE;
	public static final String ODATA_ISREIFIEDSTATEMENT_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISREIFIEDSTATEMENT;
	public static final String ODATA_ISREIFIEDPREDICATE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISREIFIEDPREDICATE;
	public static final String ODATA_ISREIFIEDSUBJECTPREDICATE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISREIFIEDSUBJECTPREDICATE;
	public static final String ODATA_ISREIFIEDOBJECTPREDICATE_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_ISREIFIEDOBJECTPREDICATE;
	public static final String ODATA_INVERSEOF_FQN = RdfConstants.ODATA_NS + "." + RdfConstants.ODATA_INVERSEOF;
	public final static String RDFS_CLASS_FQN = RdfConstants.RDFS + "." + RdfConstants.RDFS_CLASS_TERM;
	public final static String PROPERTY_FQN = RdfConstants.RDF + "." + RdfConstants.RDF_PROPERTY_TERM;
	public final static String DATATYPE_FQN = RdfConstants.RDF + "." + RdfConstants.DATATYPE;
	public final static String INVERSEOF_FQN = RdfConstants.OWL + "." + RdfConstants.INVERSEOF;
	
	public final static String OWL_EQUIVALENTPROPERTY_FQN = RdfConstants.OWL + "."
			+ RdfConstants.OWL_EQUIVALENTPROPERTY_LABEL;
	public final static String ONTOLOGY_FQN = RdfConstants.OWL + "." + RdfConstants.ONTOLOGY;

	private final static CsdlTerm sapLabelTerm = new CsdlTerm().setName(RdfConstants.SAP_LABEL).setType("Edm.String");
	private final static CsdlTerm sapheadingTerm = new CsdlTerm().setName(RdfConstants.SAP_HEADING).setType("Edm.String");
	private final static CsdlTerm sapquickinfoTerm = new CsdlTerm().setName(RdfConstants.SAP_QUICKINFO).setType("Edm.String");
	private final static CsdlTerm odataDefaultNamespace = new CsdlTerm().setName(RdfConstants.ODATA_DEFAULTNAMESPACE).setType("Edm.String");
	private final static CsdlTerm odataNamespaces = new CsdlTerm().setName(RdfConstants.ODATA_NAMESPACES).setType("Edm.String");
	private final static CsdlTerm odatabaseType = new CsdlTerm().setName(RdfConstants.ODATA_BASETYPE).setType("Edm.String");
	private final static CsdlTerm odataFK = new CsdlTerm().setName(RdfConstants.ODATA_FK).setType("Edm.String");
	private final static CsdlTerm odataSubType = new CsdlTerm().setName(RdfConstants.ODATA_SUBTYPE).setType("Edm.String");
	private final static CsdlTerm odataIsProxy = new CsdlTerm().setName(RdfConstants.ODATA_ISPROXY).setType("Edm.String");
	private final static CsdlTerm odataIsDataset = new CsdlTerm().setName(RdfConstants.ODATA_ISDATASET).setType("Edm.String");
	private final static CsdlTerm odataIsPropertyPath = new CsdlTerm().setName(RdfConstants.ODATA_ISPROPERTYPATH).setType("Edm.String");
	private final static CsdlTerm odataRdfType = new CsdlTerm().setName(RdfConstants.ODATA_RDFTYPE).setType("Edm.String");
	private final static CsdlTerm odataIsReifiedStatement = new CsdlTerm().setName(RdfConstants.ODATA_ISREIFIEDSTATEMENT).setType("Edm.String");
	private final static CsdlTerm odataIsReifiedPredicate = new CsdlTerm().setName(RdfConstants.ODATA_ISREIFIEDPREDICATE).setType("Edm.String");
	private final static CsdlTerm odataIsReifiedSubjectPredicate = new CsdlTerm().setName(RdfConstants.ODATA_ISREIFIEDSUBJECTPREDICATE).setType("Edm.String");
	private final static CsdlTerm odataIsReifiedObjectPredicate = new CsdlTerm().setName(RdfConstants.ODATA_ISREIFIEDOBJECTPREDICATE).setType("Edm.String");
	private final static CsdlTerm odataInverseOf = new CsdlTerm().setName(RdfConstants.ODATA_INVERSEOF).setType("Edm.String");
	private final static CsdlTerm rdfsClassTerm = new CsdlTerm().setName(RdfConstants.RDFS_CLASS_TERM).setType("Edm.String");
	private final static CsdlTerm rdfPropertyTerm = new CsdlTerm().setName(RdfConstants.RDF_PROPERTY_TERM).setType("Edm.String");
	private final static CsdlTerm rdfsDatatypeTerm = new CsdlTerm().setName(RdfConstants.DATATYPE).setType("Edm.String");
	private final static CsdlTerm owlInverseOfTerm = new CsdlTerm().setName(RdfConstants.INVERSEOF).setType("Edm.String");
	private final static CsdlTerm owlOntologyTerm = new CsdlTerm().setName(RdfConstants.ONTOLOGY).setType("Edm.String");

	public final static List<String> RDF_DATATYPES = new ArrayList<String>();

	public static final TreeMap<String, CsdlTerm> TERMS = new TreeMap<String, CsdlTerm>();
	
	public static final ArrayList< CsdlTerm> SAPTERMS = new ArrayList<CsdlTerm>();
	public static final ArrayList< CsdlTerm> ODATATERMS = new ArrayList<CsdlTerm>();
	public static final ArrayList< CsdlTerm> RDFTERMS = new ArrayList<CsdlTerm>();
	public static final ArrayList< CsdlTerm> RDFSTERMS = new ArrayList<CsdlTerm>();
	public static final ArrayList< CsdlTerm> OWLTERMS = new ArrayList<CsdlTerm>();


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

			odata4sparqlFile = repositoryManagerDirPath + "ontologies/odata4sparql.v2.7.4.rdf";
			rdfFile = repositoryManagerDirPath + "ontologies/22-rdf-syntax-ns.ttl";
			rdfsFile = repositoryManagerDirPath + "ontologies/rdf-schema.ttl";
			sailFile = repositoryManagerDirPath + "ontologies/sail.rdf";
			spFile = repositoryManagerDirPath + "ontologies/sp.ttl";
			contextmenuFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.contextmenu.v2.0.1.rdf";
			olgapFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.olgap.v2.0.0.rdf";
			searchFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.lucenesearch.v3.0.0.rdf";
			changeFile = repositoryManagerDirPath + "ontologies/odata4sparql.proxy.change.v1.0.0.rdf";

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
