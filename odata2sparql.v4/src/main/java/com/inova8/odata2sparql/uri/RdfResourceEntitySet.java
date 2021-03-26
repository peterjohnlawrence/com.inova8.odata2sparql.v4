/*
 * inova8 2020
 */
package com.inova8.odata2sparql.uri;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;

/**
 * The Class RdfResourceEntitySet.
 */
public class RdfResourceEntitySet extends RdfResourcePart {
	
	/** The rdf edm provider. */
	RdfEdmProvider rdfEdmProvider;
	
	/** The edm entity set. */
	EdmEntitySet edmEntitySet;
	
	/** The rdf entity type. */
	RdfEntityType rdfEntityType;
	
	/** The key predicates. */
	List<UriParameter> keyPredicates;

	/**
	 * Instantiates a new rdf resource entity set.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param uriResourceEntitySet the uri resource entity set
	 */
	public RdfResourceEntitySet(RdfEdmProvider rdfEdmProvider, UriResourceEntitySet uriResourceEntitySet) {
		super.setUriResourceKind(UriResourceKind.entitySet);
		this.rdfEdmProvider = rdfEdmProvider;
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
		keyPredicates = uriResourceEntitySet.getKeyPredicates();
		rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		this.edmEntitySet = edmEntitySet;
	}
	
	/**
	 * Instantiates a new rdf resource entity set.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param edmEntitySet the edm entity set
	 * @param keyPredicates the key predicates
	 */
	public RdfResourceEntitySet(RdfEdmProvider rdfEdmProvider, EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) {
		super.setUriResourceKind(UriResourceKind.entitySet);
		this.rdfEdmProvider = rdfEdmProvider;
		this.keyPredicates = keyPredicates;
		rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		this.edmEntitySet = edmEntitySet;
	}
	
	/**
	 * Instantiates a new rdf resource entity set.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param keyPredicates the key predicates
	 */
	public RdfResourceEntitySet(RdfEdmProvider rdfEdmProvider, List<UriParameter> keyPredicates) {
		this.rdfEdmProvider = rdfEdmProvider;
		this.keyPredicates = keyPredicates;
	}
	
	/**
	 * Gets the edm entity set.
	 *
	 * @return the edm entity set
	 */
	public EdmEntitySet getEdmEntitySet() {
		return edmEntitySet;
	}

	/**
	 * Gets the rdf entity type.
	 *
	 * @return the rdf entity type
	 */
	public RdfEntityType getRdfEntityType() {
		return rdfEntityType;
	}

	/**
	 * Gets the key predicates.
	 *
	 * @return the key predicates
	 */
	public List<UriParameter> getKeyPredicates() {
		return keyPredicates;
	}

	/**
	 * Gets the decoded key.
	 *
	 * @param uriInfo the uri info
	 * @return the decoded key
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public String getDecodedKey(UriInfo uriInfo) throws OData2SparqlException {
		if (keyPredicates.size() > 1) {
			String pathVariable = "";
			for (UriParameter entityKey : keyPredicates) {
				String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(entityKey.getText());
				String expandedKey = rdfEdmProvider.getRdfModel().getRdfPrefixes()
						.expandPrefix(decodedEntityKey.substring(1, decodedEntityKey.length() - 1));
				pathVariable = entityKey.getReferencedProperty() + "=<" + expandedKey + ">,";
			}
			return pathVariable.substring(0, pathVariable.length() - 1);
		} else if (!keyPredicates.isEmpty()) {
			String key =RdfResourceParts.getParameter(keyPredicates.get(0));
//			String key ;
//			if(keyPredicates.get(0).getAlias()!=null ) {
//				key= uriInfo.getValueForAlias(keyPredicates.get(0).getAlias());
//			//	key = key.substring(1,key.length()-1);
//			}else {
//				 key = keyPredicates.get(0).getText();
//			}
			String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(key);
			String expandedKey = rdfEdmProvider.getRdfModel().getRdfPrefixes()
					.expandPrefix(decodedEntityKey.substring(1, decodedEntityKey.length() - 1));
			return "<" + expandedKey + ">";
		} else {
			return null;
		}
	}

	/**
	 * Gets the local key.
	 *
	 * @param uriInfo the uri info
	 * @return the local key
	 */
	public String getLocalKey(UriInfo uriInfo) {
		if (keyPredicates.size() > 1) {
			return null;
		} else if (!keyPredicates.isEmpty()) {
			try {
				String key =RdfResourceParts.getParameter(keyPredicates.get(0));
				key = key.substring(1, key.length() - 1);
				return  "'" +  UriUtils.encodeUri( key) + "'";
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Gets the subject id.
	 *
	 * @param uriInfo the uri info
	 * @return the subject id
	 */
	public String getSubjectId(UriInfo uriInfo) {
		if (keyPredicates.size() > 1) {
			return null;
		} else if (!keyPredicates.isEmpty()) {
			String key =RdfResourceParts.getParameter(keyPredicates.get(0));
			key = key.substring(1, key.length() - 1);
			try {
				return key =UriUtils.encodeUri(key);
			} catch (UnsupportedEncodingException e) {
				return null;
			}
			//key = UriUtils.encodeQName(key);
			//return key.substring(1, key.length() - 1);
		} else {
			return null;
		}
	}

	/**
	 * Gets the entity string.
	 *
	 * @param uriInfo the uri info
	 * @return the entity string
	 */
	public String getEntityString(UriInfo uriInfo) {

		return getRdfEntityType().getEntityTypeName() + "(" + getLocalKey(uriInfo) + ")";
	}

	/**
	 * Checks for key.
	 *
	 * @return true, if successful
	 */
	public boolean hasKey() {
		return !keyPredicates.isEmpty();
	}
	
	/**
	 * Gets the entity set name.
	 *
	 * @return the entity set name
	 */
	public String getEntitySetName() {
		
		return  this.edmEntitySet.getName();
	}
}
