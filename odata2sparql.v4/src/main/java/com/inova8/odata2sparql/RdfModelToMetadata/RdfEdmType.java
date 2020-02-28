package com.inova8.odata2sparql.RdfModelToMetadata;

import java.util.Map;
import java.util.TreeMap;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

import com.inova8.odata2sparql.Constants.RdfConstants;

public class RdfEdmType {
	private static final Map<String, EdmPrimitiveTypeKind> SIMPLE_TYPE_MAPPING = new TreeMap<String, EdmPrimitiveTypeKind>();

	static {
		SIMPLE_TYPE_MAPPING.put(RdfConstants.RDF_PLAIN_LITERAL, EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2000/01/rdf-schema#Literal", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#decimal", EdmPrimitiveTypeKind.Decimal);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#Literal", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put(RdfConstants.XSD_STRING, EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#boolean", EdmPrimitiveTypeKind.Boolean);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#float", EdmPrimitiveTypeKind.Double);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#double", EdmPrimitiveTypeKind.Double);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#duration", EdmPrimitiveTypeKind.Int16);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#dateTime", EdmPrimitiveTypeKind.DateTimeOffset);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#time", EdmPrimitiveTypeKind.DateTimeOffset);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#date", EdmPrimitiveTypeKind.Date);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gYearMonth", EdmPrimitiveTypeKind.Date);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gYear", EdmPrimitiveTypeKind.Date);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gMonthDay", EdmPrimitiveTypeKind.Date);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gDay", EdmPrimitiveTypeKind.Date);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gMonth", EdmPrimitiveTypeKind.Date);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#hexBinary", EdmPrimitiveTypeKind.Binary);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#base64Binary", EdmPrimitiveTypeKind.Binary);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#anyURI", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#QName", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#NOTATION", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#normalizedString", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#token", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#language", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#IDREFS", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#ENTITIES", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#NMTOKEN", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#Name", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#NCName", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#ID", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#IDREF", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#ENTITY", EdmPrimitiveTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#integer", EdmPrimitiveTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#nonPositiveInteger", EdmPrimitiveTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#negativeInteger", EdmPrimitiveTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#long", EdmPrimitiveTypeKind.Int64);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#int", EdmPrimitiveTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#short", EdmPrimitiveTypeKind.Int16);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#byte", EdmPrimitiveTypeKind.Byte);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#nonNegativeInteger", EdmPrimitiveTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#unsignedLong", EdmPrimitiveTypeKind.Int64);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#unsignedInt", EdmPrimitiveTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#unsignedShort", EdmPrimitiveTypeKind.Int16);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#unsignedByte", EdmPrimitiveTypeKind.Byte);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#positiveInteger", EdmPrimitiveTypeKind.Int32);

	};
	public static EdmPrimitiveTypeKind getEdmType(String propertyTypeName) {
		if (propertyTypeName== null || !SIMPLE_TYPE_MAPPING.containsKey(propertyTypeName))
		return EdmPrimitiveTypeKind.String;
		return SIMPLE_TYPE_MAPPING.get(propertyTypeName);
	}
	public static boolean isAnyUri(String propertyTypeName) {
		if (propertyTypeName== null || !SIMPLE_TYPE_MAPPING.containsKey(propertyTypeName))
		return false;
		return  propertyTypeName.equals("http://www.w3.org/2001/XMLSchema#anyURI");
	};
	public static String anyUri() {
		return "Edm.anyURI";
	};
}
