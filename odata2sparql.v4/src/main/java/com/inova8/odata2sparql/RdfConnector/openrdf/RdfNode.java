/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

//import org.openrdf.model.Value;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;


/**
 * The Class RdfNode.
 */
public class RdfNode {
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfNode.class);
	
	/** The node. */
	private final Value node;

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
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

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return this.node.stringValue().hashCode();
	}

	/**
	 * Instantiates a new rdf node.
	 *
	 * @param node the node
	 */
	public RdfNode(Value node) {
		this.node = node;
	}

	/**
	 * Gets the node.
	 *
	 * @return the node
	 */
	public Value getNode() {
		return node;
	}

	/**
	 * Checks if is iri.
	 *
	 * @return true, if is iri
	 */
	public boolean isIRI() {
		return node instanceof IRI;
	}

	/**
	 * Checks if is blank.
	 *
	 * @return true, if is blank
	 */
	public boolean isBlank() {
		return node instanceof BNode;
	}

	/**
	 * Gets the iri.
	 *
	 * @return the iri
	 */
	public Object getIRI() {
		if (this.isBlank()) {
			return ((BNode) node).toString();
		} else {
			return ((IRI) node).stringValue();
		}
	}
	
	/**
	 * Gets the IRI string.
	 *
	 * @return the IRI string
	 */
	public String getIRIString() {
		return this.getIRI().toString();
	}
	
	/**
	 * Gets the literal datatype.
	 *
	 * @return the literal datatype
	 */
	public IRI getLiteralDatatype() {
		return ((Literal) node).getDatatype();
	}

	/**
	 * Gets the literal value.
	 *
	 * @return the literal value
	 */
	public Literal getLiteralValue() {
		return ((Literal) node);
	}

	/**
	 * Gets the literal object.
	 *
	 * @return the literal object
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public Object getLiteralObject() throws OData2SparqlException {
		try {
			if (this.getLiteralDatatype() != null) {
				switch (this.getLiteralDatatype().stringValue()) {
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
					if(this.getLiteralValue().getLabel().isEmpty()) {
						return null;
					}else{
						return this.getLiteralValue().decimalValue();
					}
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
					log.debug("RdfNode getLiteralObject failure. Datatype:" + this.getLiteralDatatype().stringValue()
							+ ". Value: " + this.getLiteralValue().stringValue());
					//throw new Olingo2SparqlException("RdfNode getLiteralObject failure");
					return this.getLiteralValue().stringValue();
				}
			} else {
				return this.getLiteralValue();
			}
		} catch (Exception e) {
			return this.getLiteralValue();
		}		
	}

	/**
	 * Gets the local name.
	 *
	 * @return the local name
	 */
	public String getLocalName() {
		if(this.isBlank()) {
			return ((BNode) node).toString();
		}else {
			return ((IRI) node).getLocalName();
		}
	}

	/**
	 * Gets the namespace.
	 *
	 * @return the namespace
	 */
	public String getNamespace() {
		String nameSpace = ((IRI) node).getNamespace();
		if(this.isIRI()) return ((IRI) node).getNamespace();
		//String nameSpace =   node.getNamespace();
		String lastCharacter = nameSpace.substring(nameSpace.length() - 1);
		if (lastCharacter.equals(RdfConstants.QNAME_SEPARATOR))
			return nameSpace.substring(0, nameSpace.length() - 1);
		else
			return nameSpace;
	}

	/**
	 * Gets the literal.
	 *
	 * @return the literal
	 */
	public RdfLiteral getLiteral() {
		RdfLiteral rdfLiteral = new RdfLiteral((Literal) node);
		return rdfLiteral;
	}

}
