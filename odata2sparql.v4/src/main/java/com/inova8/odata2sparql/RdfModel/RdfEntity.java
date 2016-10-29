package com.inova8.odata2sparql.RdfModel;


import java.util.HashMap;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNode;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrefixes;

public class RdfEntity extends HashMap<String, Object>{
	private static final long serialVersionUID = 1L;
	private final HashMap<RdfNode, Object> datatypeProperties = new HashMap<RdfNode, Object>();
	private final String subject;
	private final RdfPrefixes rdfPrefixes;
	private RdfEntityType rdfEntityType;
	private boolean isExpandedEntity =false;
	private boolean isTargetEntity =false;

	public RdfEntity(RdfNode subjectNode, RdfPrefixes rdfPrefixes) {
		super();
		this.rdfPrefixes=rdfPrefixes;
		this.subject =  this.rdfPrefixes.toQName(subjectNode); //subjectNode.toQName(this.rdfPrefixes);
		this.put(RdfConstants.SUBJECT, RdfEntity.URLEncodeEntityKey(this.subject));	
	}
    
	public static String URLDecodeEntityKey(String encodedEntityKey) {

		String decodedEntityKey = encodedEntityKey;
		decodedEntityKey = encodedEntityKey.replace("@", "/");
		decodedEntityKey = encodedEntityKey.replace("%3A", ":");
		return decodedEntityKey;
	}
	public static String URLEncodeEntityKey(String entityKey) {
		String encodedEntityKey = entityKey;
		encodedEntityKey = encodedEntityKey.replace("/", "@");
		//Required by Batch otherwise URIs fail
		encodedEntityKey = encodedEntityKey.replace(":", "%3A");
		return encodedEntityKey;
	}

	public String getSubject() {
		return subject;
	}

	public HashMap<RdfNode, Object> getDatatypeProperties() {
		return datatypeProperties;
	}

	public RdfEntityType getEntityType() {
		return rdfEntityType;
	}

	public void setEntityType(RdfEntityType rdfEntityType) {
		this.rdfEntityType = rdfEntityType;
	}

	public boolean isExpandedEntity() {
		return isExpandedEntity;
	}

	public void setExpandedEntity(boolean isExpandedEntity) {
		this.isExpandedEntity = isExpandedEntity;
	}

	public boolean isTargetEntity() {
		return isTargetEntity;
	}

	public void setTargetEntity(boolean isTargetEntity) {
		this.isTargetEntity = isTargetEntity;
	}	
}
