/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlStatement;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.TreeMap;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrefixes;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.Utils.RdfNodeComparator;

/**
 * The Class SparqlEntity.
 */
public class SparqlEntity extends Entity {
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfModel.class);
	
	/** The datatype properties. */
	private final TreeMap<RdfNode, Object> datatypeProperties = new TreeMap<RdfNode, Object>(new RdfNodeComparator());
	
	/** The matching. */
	private HashSet<SparqlEntity> matching = new HashSet<SparqlEntity>();
	
	/** The subject node. */
	private final RdfNode subjectNode;
	
	/** The subject. */
	private final String subject;
	
	/** The rdf prefixes. */
	private final RdfPrefixes rdfPrefixes;
	
	/** The rdf entity type. */
	private RdfEntityType rdfEntityType;
	
	/** The is expanded entity. */
	private boolean isExpandedEntity = false;
	
	/** The is target entity. */
	private boolean isTargetEntity = false;

	/**
	 * Instantiates a new sparql entity.
	 *
	 * @param subjectNode the subject node
	 * @param rdfPrefixes the rdf prefixes
	 */
	SparqlEntity(RdfNode subjectNode, RdfPrefixes rdfPrefixes) {
		super();
		this.subjectNode = subjectNode;
		this.rdfPrefixes = rdfPrefixes;
		this.subject = this.rdfPrefixes.toQName(subjectNode, RdfConstants.QNAME_SEPARATOR);
		this.addProperty(new Property(null, RdfConstants.SUBJECT, ValueType.PRIMITIVE,
				SparqlEntity.URLEncodeEntityKey(this.subject)));
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public URI getId() {
		try {
			if (this.getEntityType()!=null) {
			if (this.getEntityType().isOperation()) {
				String id = rdfEntityType.getEDMEntitySetName() + "(";
				boolean first = true;
				for (RdfPrimaryKey keyProperty : rdfEntityType.getPrimaryKeys()) {
					if (!first)
						id = id + ",";
					Property propertyValue = this.getProperty(keyProperty.getPrimaryKeyName());
					id = id + propertyValue.getName() + "='"
							+ propertyValue.getValue().toString()
									.replace(RdfConstants.QNAME_SEPARATOR, RdfConstants.QNAME_SEPARATOR_ENCODED)
									.replace(":", RdfConstants.QNAME_SEPARATOR_ENCODED)
									// added to handle literals that might contain illegal characters
									.replace(" ", RdfConstants.QNAME_SEPARATOR_ENCODED)
							+ "'";
					first = false;
				}
				id = id + ")";
				return new URI(id);
			} else {
				return new URI(this.getEntityType().getEDMEntitySetName() + "('"
						+ subject.replace(RdfConstants.QNAME_SEPARATOR, RdfConstants.QNAME_SEPARATOR_ENCODED) + "')");
			}
			}else {
				return new URI(this.subject);
			}
		} catch (URISyntaxException e) {
			throw new ODataRuntimeException("Unable to create id for entity: " + this.subject,
					e);
		}
	}

	/**
	 * URL decode entity key.
	 *
	 * @param encodedEntityKey the encoded entity key
	 * @return the string
	 */
	public static String URLDecodeEntityKey(String encodedEntityKey) {

		String decodedEntityKey = encodedEntityKey;
		decodedEntityKey = encodedEntityKey.replace("@", "/");
		decodedEntityKey = encodedEntityKey.replace("%25", "%");
		decodedEntityKey = encodedEntityKey.replace("%3A", ":");
		return decodedEntityKey;
	}

	/**
	 * URL encode entity key.
	 *
	 * @param entityKey the entity key
	 * @return the string
	 */
	public static String URLEncodeEntityKey(String entityKey) {
		String encodedEntityKey = entityKey;
		encodedEntityKey = encodedEntityKey.replace("/", "@");
		return encodedEntityKey;
	}

	/**
	 * Gets the subject.
	 *
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Gets the subject node.
	 *
	 * @return the subject node
	 */
	public RdfNode getSubjectNode() {
		return subjectNode;
	}

	/**
	 * Gets the datatype properties.
	 *
	 * @return the datatype properties
	 */
	public TreeMap<RdfNode, Object> getDatatypeProperties() {
		return datatypeProperties;
	}

	/**
	 * Gets the entity type.
	 *
	 * @return the entity type
	 */
	public RdfEntityType getEntityType() {
		return rdfEntityType;
	}

	/**
	 * Sets the entity type.
	 *
	 * @param rdfEntityType the new entity type
	 */
	public void setEntityType(RdfEntityType rdfEntityType) {
		this.rdfEntityType = rdfEntityType;
	}

	/**
	 * Assert target entity type.
	 *
	 * @param rdfEntityType the rdf entity type
	 */
	public void assertTargetEntityType(RdfEntityType rdfEntityType) {
		this.setTargetEntity(true);
		if(this.getEntityType()!=null &&  rdfEntityType != this.getEntityType()) {
			//This means that the same entity has been used in an expanded query but when it is off a different type
			log.warn("Assert target type: " + rdfEntityType.getEDMEntityTypeName() + " of: " + this.getId());
		}
		this.rdfEntityType = rdfEntityType;
	}
	
	/**
	 * Assert entity type.
	 *
	 * @param rdfEntityType the rdf entity type
	 */
	public void assertEntityType(RdfEntityType rdfEntityType) {
		if(!this.isTargetEntity()) {
			//Don't overwrite assertion of targetEntity
			this.rdfEntityType = rdfEntityType;
		}
	}
	
	/**
	 * Checks if is expanded entity.
	 *
	 * @return true, if is expanded entity
	 */
	public boolean isExpandedEntity() {
		return isExpandedEntity;
	}

	/**
	 * Sets the expanded entity.
	 *
	 * @param isExpandedEntity the new expanded entity
	 */
	public void setExpandedEntity(boolean isExpandedEntity) {
		this.isExpandedEntity = isExpandedEntity;
	}

	/**
	 * Checks if is target entity.
	 *
	 * @return true, if is target entity
	 */
	public boolean isTargetEntity() {
		return isTargetEntity;
	}

	/**
	 * Sets the target entity.
	 *
	 * @param isTargetEntity the new target entity
	 */
	public void setTargetEntity(boolean isTargetEntity) {
		this.isTargetEntity = isTargetEntity;
	}

	/**
	 * Contains property.
	 *
	 * @param name the name
	 * @return the boolean
	 */
	Boolean containsProperty(final String name) {
		Boolean result = false;
		for (Property property : this.getProperties()) {
			if (name.equals(property.getName())) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Adds the matching.
	 *
	 * @param matchingEntity the matching entity
	 */
	public void addMatching(SparqlEntity matchingEntity) {
		if (!this.equals(matchingEntity))
			matching.add(matchingEntity);
	}

	/**
	 * Gets the matching.
	 *
	 * @return the matching
	 */
	public HashSet<SparqlEntity> getMatching() {
		return matching;
	}

}
