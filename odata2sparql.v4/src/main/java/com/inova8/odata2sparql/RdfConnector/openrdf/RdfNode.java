package com.inova8.odata2sparql.RdfConnector.openrdf;

//import org.openrdf.model.Value;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import com.inova8.odata2sparql.Exception.OData2SparqlException;


public class RdfNode {
	private final Log log = LogFactory.getLog(RdfNode.class);
	private final Value node;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return this.node.stringValue().equals(obj);
		} else if (obj instanceof Value) {
			return this.node.stringValue().equals(((Value) obj).stringValue());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.node.stringValue().hashCode();
	}

	RdfNode(Value node) {
		this.node = node;
	}

	public Value getNode() {
		return node;
	}

	public boolean isIRI() {
		return node instanceof IRI;
	}

	public boolean isBlank() {
		return node instanceof BNode;
	}

	public Object getIRI() {
		if (this.isBlank()) {
			return ((BNode) node).toString();
		} else {
			return ((IRI) node).toString();
		}
	}

//	public String toQName(RdfPrefixes rdfPrefixes) {
//		String qname = null;
//		if (this.isBlank()) {
//			return ((BNode) node).toString();
//		} else {
//			try {
//				qname = rdfPrefixes.getOrCreatePrefix(null, ((URI) node).getNamespace()) + ":"
//						+ ((URI) node).getLocalName();
//			} catch (OData2SparqlException e) {
//				log.error("RdfNode toQName failure. Node:" + this.node.toString() + " with exception " + e.toString());
//			}
//		}
//		return qname;
//	}

	public IRI getLiteralDatatype() {
		return ((Literal) node).getDatatype();
	}

	public Literal getLiteralValue() {
		return ((Literal) node);
	}

	public Object getLiteralObject() throws OData2SparqlException {
		if (this.getLiteralDatatype() != null) {
			switch (this.getLiteralDatatype().toString()) {
			case "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#string":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#boolean":
				return this.getLiteralValue().booleanValue();
			case "http://www.w3.org/2001/XMLSchema#float":
				return this.getLiteralValue().floatValue();
			case "http://www.w3.org/2001/XMLSchema#double":
				return this.getLiteralValue().doubleValue();
			case "http://www.w3.org/2001/XMLSchema#decimal":
				return this.getLiteralValue().decimalValue();
			case "http://www.w3.org/2001/XMLSchema#duration":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#dateTime":
				return this.getLiteralValue().calendarValue();
			case "http://www.w3.org/2001/XMLSchema#time":
				return this.getLiteralValue().calendarValue();
			case "http://www.w3.org/2001/XMLSchema#date":
				return this.getLiteralValue().calendarValue();
			case "http://www.w3.org/2001/XMLSchema#gYearMonth":
				return this.getLiteralValue().calendarValue();
			case "http://www.w3.org/2001/XMLSchema#gYear":
				return this.getLiteralValue().calendarValue();
			case "http://www.w3.org/2001/XMLSchema#gMonthDay":
				return this.getLiteralValue().calendarValue();
			case "http://www.w3.org/2001/XMLSchema#gDay":
				return this.getLiteralValue().calendarValue();
			case "http://www.w3.org/2001/XMLSchema#gMonth":
				return this.getLiteralValue().calendarValue();
			case "http://www.w3.org/2001/XMLSchema#hexBinary":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#base64Binary":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#anyURI":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#QName":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#NOTATION":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#normalizedString":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#token":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#language":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#IDREFS":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#ENTITIES":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#NMTOKEN":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#NMTOKENS":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#Name":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#NCName":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#ID":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#IDREF":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#ENTITY":
				return this.getLiteralValue().stringValue();
			case "http://www.w3.org/2001/XMLSchema#integer":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#nonPositiveInteger":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#negativeInteger":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#long":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#int":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#short":
				return this.getLiteralValue().shortValue();
			case "http://www.w3.org/2001/XMLSchema#byte":
				return this.getLiteralValue().byteValue();
			case "http://www.w3.org/2001/XMLSchema#nonNegativeInteger":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#unsignedLong":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#unsignedInt":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#unsignedShort":
				return this.getLiteralValue().shortValue();
			case "http://www.w3.org/2001/XMLSchema#unsignedByte":
				return this.getLiteralValue().byteValue();
			case "http://www.w3.org/2001/XMLSchema#positiveInteger":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#yearMonthDuration":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#dayTimeDuration":
				return this.getLiteralValue().intValue();
			case "http://www.w3.org/2001/XMLSchema#dateTimeStamp":
				return this.getLiteralValue().intValue();
			case "http://www.openlinksw.com/schemas/virtrdf#Geometry":
				return this.getLiteralValue().stringValue();

			default:
				log.debug("RdfNode getLiteralObject failure. Datatype:" + this.getLiteralDatatype().toString()
						+ ". Value: " + this.getLiteralValue().stringValue());
				//throw new Olingo2SparqlException("RdfNode getLiteralObject failure");
				return this.getLiteralValue().stringValue();
			}
		} else {
			return this.getLiteralValue();
		}
	}

	public String getLocalName() {
		return ((IRI) node).getLocalName();
	}

	public String getNamespace() {
		String nameSpace = ((IRI) node).getNamespace();
		if(this.isIRI()) return ((IRI) node).getNamespace();
		//String nameSpace =   node.getNamespace();
		String lastCharacter = nameSpace.substring(nameSpace.length() - 1);
		if (lastCharacter.equals(":"))
			return nameSpace.substring(0, nameSpace.length() - 1);
		else
			return nameSpace;
	}

	public RdfLiteral getLiteral() {
		RdfLiteral rdfLiteral = new RdfLiteral((Literal) node);
		return rdfLiteral;
	}

}
