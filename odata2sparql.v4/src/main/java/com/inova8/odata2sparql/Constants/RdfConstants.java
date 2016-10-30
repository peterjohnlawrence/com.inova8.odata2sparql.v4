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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.commons.api.edm.provider.CsdlTerm;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;

public class RdfConstants {
	private final static Log log = LogFactory.getLog(RdfConstants.class);

	public enum Cardinality {
		// Cardinality corresponding to
		// 0..1, 1..1, 0..*, 1..*
		ZERO_TO_ONE, ONE, MANY, MULTIPLE
	}

	public static final String RESET = "$reset";
	public static final String RELOAD = "$reload";
	public static final String WILDCARD = "*";
	private final static ValueFactory valueFactoryImpl = SimpleValueFactory.getInstance();
	static private final Hashtable<Value, String> metaQueries = new Hashtable<Value, String>();

	public final static String repositoryUrl = "repositoryUrl";
	public final static String systemId = "ODATA2SPARQL";
	public final static String bootStrapQuery = "SELECT   ?Query  ?QueryString WHERE { ?Metadata ?Query  ?querys . ?querys <http://spinrdf.org/sp#text> ?QueryString . ?Query  <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://inova8.com/odata4sparql#metadataQuery> .}";

	private static String repositoryManagerDirPath; // NO_UCD (use final)
	public static File repositoryManagerDir; // NO_UCD (use final)
	public static String odata4sparqlFile; // NO_UCD (use final)
	public static String rdfFile; // NO_UCD (use final)
	public static String rdfsFile; // NO_UCD (use final)
	public static String modelFile; // NO_UCD (use final)
	public static String sailFile; // NO_UCD (use final)
	public static String spFile; // NO_UCD (use final)

	public final static Value RDFSModel = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#RDFSModel");
	@Deprecated
	public final static Value repositoryQuery = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#repositoryQuery");
	@Deprecated
	public final static Value prefixQuery = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#prefixQuery");

	public static final String TARGETENTITY = "http://targetEntity";
	public static final String PREFIX = "j";
	public static final String PROPERTY_POSTFIX = "_value";

	public static final String SAP_ANNOTATION_SCHEMA = "http://www.sap.com/Protocols/SAPData";
	public static final String SAP_ANNOTATION_NS = "sap";
	public static final String SAP_LABEL = "label";
	public static final String SAP_HEADING = "heading";
	public static final String SAP_QUICKINFO = "quickinfo";

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
	public static final String DC_SCHEMA = "http://purl.org/dc/elements/1.1/";
	public static final String DC_NS = "http://purl.org/dc/elements/1.1/";
	public static final String DC = "dc";

	public static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";
	public static final String RDF_PLAIN_LITERAL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral";

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
	//TODO we should rely on the metadata queries to determine which properties even of Resource that should be included
	@Deprecated
	public static final String RDFS_COMMENT = "http://www.w3.org/2000/01/rdf-schema#comment";
	@Deprecated
	public static final String RDFS_COMMENT_LABEL = "comment";
	@Deprecated
	public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
	@Deprecated
	public static final String RDFS_LABEL_LABEL = "label";
	public static final String RDFS_DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
	public static final String RDFS_DOMAIN_LABEL = "domain";
	public static final String RDFS_SUBCLASSOF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
	public static final String RDFS_SUBCLASSOF_LABEL = "subClassOf";
	public static final String RDFS_RANGE = "http://www.w3.org/2000/01/rdf-schema#range";
	public static final String RDFS_RANGE_LABEL = "range";
	@Deprecated
	public static final String OWL_EQUIVALENTPROPERTY = "http://www.w3.org/2002/07/owl#equivalentProperty";
	public static final String OWL_EQUIVALENTPROPERTY_LABEL = "equivalentProperty";
	public static final String OWL_IMPORTS = "http://www.w3.org/2002/07/owl#imports";
	public static final String OWL_IMPORTS_LABEL = "imports";

	public static final String ONTOLOGY = "Ontology";
	public static final String DATATYPE = "Datatype";
	public static final String PROPERTY = "Property";

	public static final String LANGSTRING = "langString";
	public static final String LANG = "lang";
	public static final String VALUE = "value";
	@Deprecated
	public static final String OBJECTSTATEMENT = "objectStatement";
	@Deprecated
	public static final String OBJECTPREDICATE = "objectPredicate";
	@Deprecated
	public static final String OBJECT = "object";
	@Deprecated
	public static final String DATASTATEMENT = "dataStatement";
	@Deprecated
	public static final String DATAPREDICATE = "dataPredicate";
	@Deprecated
	public static final String DATA = "data";

	public static final String INVERSEOF = "inverseOf ";
	public static final String SPARQL_MODEL = "SparqlModel";
	@Deprecated
	public static final String ENTITY = "_Entity";
	@Deprecated
	public static final String ENTITYTYPE = "_Class";
	public static final String ENTITYCONTAINER = "Container";
	public static final String ENTITYCONTAINERNAMESPACE = "Instances";
	@Deprecated
	public static final String NAVIGATIONPROPERTY = "_ObjectProperty";
	public static final String PLURAL = "";
	public static final String FROMROLE = "";
	public static final String TOROLE = "";
	@Deprecated
	public static final String HASH = "#";
	public static final String PREDICATE_SEPARATOR = "_";;
	public static final String CLASS_SEPARATOR = "_";;

	public static final String DUPLICATEROLE = "1";

	public static final String CLASS_LABEL_PREFIX = "";
	public static final String PROPERTY_LABEL_PREFIX = "";
	public static final String NAVIGATIONPROPERTY_LABEL_PREFIX = "";
	@Deprecated
	public static final String INVERSENAVIGATIONPROPERTY_LABEL_PREFIX = "has ";
	@Deprecated
	public static final Object RDF_LANG_STRING = RDF + "." + LANGSTRING;

	public static final Value URI_ASSOCIATIONQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#associationQuery");
	public static final Value URI_CLASSQUERY = valueFactoryImpl.createIRI("http://inova8.com/odata4sparql#classQuery");
	@Deprecated
	public static final Value URI_DATASETQUERY = valueFactoryImpl
			.createIRI("http://inova8.com/odata4sparql#datasetQuery");
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
	public static final String NULLABLE = "Nullable";
	public static final String FALSE = "false";

	public static String SAP_LABEL_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_LABEL;
	public static String SAP_HEADING_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_HEADING;
	public static String SAP_QUICKINFO_FQN = RdfConstants.SAP_ANNOTATION_NS + "." + RdfConstants.SAP_QUICKINFO;
	public static String RDFS_CLASS_FQN = RdfConstants.RDFS + "." + RdfConstants.RDFS_CLASS_LABEL;
	public static String PROPERTY_FQN = RdfConstants.RDF + "." + RdfConstants.PROPERTY;
	public static String DATATYPE_FQN =RdfConstants.RDF + "." + RdfConstants.DATATYPE;
	public static String INVERSEOF_FQN =RdfConstants.OWL + "." + RdfConstants.INVERSEOF;
	public static String OWL_EQUIVALENTPROPERTY_FQN = RdfConstants.OWL + "." +RdfConstants.OWL_EQUIVALENTPROPERTY_LABEL;
	public static String ONTOLOGY_FQN = RdfConstants.OWL + "." +RdfConstants.ONTOLOGY;

	static CsdlTerm sapLabelTerm = new CsdlTerm().setName(RdfConstants.SAP_LABEL);
	static CsdlTerm sapheadingTerm = new CsdlTerm().setName(RdfConstants.SAP_HEADING);
	static CsdlTerm sapquickinfoTerm = new CsdlTerm().setName(RdfConstants.SAP_QUICKINFO);
	static CsdlTerm rdfsClassTerm = new CsdlTerm().setName(RdfConstants.RDFS_CLASS_LABEL);
	static CsdlTerm rdfPropertyTerm = new CsdlTerm().setName(RdfConstants.PROPERTY);
	static CsdlTerm rdfsDatatypeTerm = new CsdlTerm().setName(RdfConstants.DATATYPE);
	static CsdlTerm owlInverseOfTerm = new CsdlTerm().setName(RdfConstants.INVERSEOF);
	static CsdlTerm owlEquivalentPropertyTerm = new CsdlTerm().setName(RdfConstants.OWL_EQUIVALENTPROPERTY_LABEL);
	static CsdlTerm owlOntologyTerm = new CsdlTerm().setName(RdfConstants.ONTOLOGY);

	public final static List<String> RDF_DATATYPES = new ArrayList<String>();

	public static final HashMap<String, CsdlTerm> TERMS = new HashMap<String, CsdlTerm>();

	public static Hashtable<Value, String> getMetaQueries() {
		return metaQueries;
	}

	static {
		//Initialize the file dependent constants
		try {
			repositoryManagerDirPath = URLDecoder.decode(RdfConstants.class.getResource("/").getFile(), "UTF-8");
			repositoryManagerDir = new File(repositoryManagerDirPath);
			odata4sparqlFile = repositoryManagerDirPath + "repositories/odata4sparql.rdf";
			rdfFile = repositoryManagerDirPath + "repositories/22-rdf-syntax-ns.ttl";
			rdfsFile = repositoryManagerDirPath + "repositories/rdf-schema.ttl";
			modelFile = repositoryManagerDirPath + "repositories/models.ttl";
			sailFile = repositoryManagerDirPath + "repositories/sail.rdf";
			spFile = repositoryManagerDirPath + "repositories/sp.ttl";
		} catch (UnsupportedEncodingException e) {
			log.error("Cannot decode file directory to be used for repository: " + e.getMessage());
		}
		//Initialize the RDF datatypes
		RDF_DATATYPES.add((RdfConstants.RDF_PLAIN_LITERAL));
		RDF_DATATYPES.add(("http://www.w3.org/2000/01/rdf-schema#Literal"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#decimal"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#Literal"));
		RDF_DATATYPES.add((RdfConstants.XSD_STRING));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#boolean"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#float"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#double"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#duration"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#dateTime"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#time"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#date"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gYearMonth"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gYear"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gMonthDay"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gDay"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#gMonth"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#hexBinary"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#base64Binary"));
		RDF_DATATYPES.add(("http://www.w3.org/2001/XMLSchema#anyURI"));
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
