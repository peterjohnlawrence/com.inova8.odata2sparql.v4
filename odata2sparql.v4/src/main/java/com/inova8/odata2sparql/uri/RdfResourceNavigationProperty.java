/*
 * inova8 2020
 */
package com.inova8.odata2sparql.uri;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;

/**
 * The Class RdfResourceNavigationProperty.
 */
public class RdfResourceNavigationProperty extends RdfResourcePart {
	
	/** The rdf edm provider. */
	RdfEdmProvider rdfEdmProvider;
	
	/** The edm navigation property. */
	EdmNavigationProperty edmNavigationProperty;
	
	/** The key predicates. */
	List<UriParameter> keyPredicates;

	/**
	 * Instantiates a new rdf resource navigation property.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param uriResourceNavigation the uri resource navigation
	 */
	public RdfResourceNavigationProperty(RdfEdmProvider rdfEdmProvider, UriResourceNavigation uriResourceNavigation) {
		super.setUriResourceKind(UriResourceKind.navigationProperty);
		this.rdfEdmProvider = rdfEdmProvider;
		keyPredicates = uriResourceNavigation.getKeyPredicates();
		EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
		this.edmNavigationProperty = edmNavigationProperty;
	}

	/**
	 * Gets the edm navigation property.
	 *
	 * @return the edm navigation property
	 */
	public EdmNavigationProperty getEdmNavigationProperty() {
		return edmNavigationProperty;
	}

	/**
	 * Gets the nav path.
	 *
	 * @return the nav path
	 */
	public String getNavPath() {
		if (!keyPredicates.isEmpty()) {
			return edmNavigationProperty.getName() + "(" + getLocalKey() + ")";
		} else {
			return edmNavigationProperty.getName();
		}
	}

	/**
	 * Gets the decoded key.
	 *
	 * @return the decoded key
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public String getDecodedKey() throws OData2SparqlException {
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
			String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(keyPredicates.get(0).getText());
			String expandedKey = rdfEdmProvider.getRdfModel().getRdfPrefixes()
					.expandPrefix(decodedEntityKey.substring(1, decodedEntityKey.length() - 1));
			return "<" + expandedKey + ">";
		} else {
			return "";
		}
	}

	/**
	 * Gets the local key.
	 *
	 * @return the local key
	 */
	public String getLocalKey() {
		if (keyPredicates.size() > 1) {
			String pathVariable = "";
			for (UriParameter entityKey : keyPredicates) {
				pathVariable = entityKey.getReferencedProperty() + "=" + entityKey.getText() + ",";
			}
			return pathVariable.substring(0, pathVariable.length() - 1);
		} else if (!keyPredicates.isEmpty()) {
			return keyPredicates.get(0).getText();
		} else {
			return "";
		}
	}
	
	/**
	 * Gets the subject id.
	 *
	 * @return the subject id
	 */
	public String getSubjectId() {
		if (keyPredicates.size() > 1) {
			String pathVariable = "";
			for (UriParameter entityKey : keyPredicates) {
				String key = entityKey.getText();
				pathVariable = key.substring(1, key.length()-1) + ",";
			}
			return pathVariable.substring(0, pathVariable.length() - 1);
		} else if (!keyPredicates.isEmpty()) {
			String key = keyPredicates.get(0).getText();
			return key.substring(1, key.length()-1);
		} else {
			return null;
		}
	}

	/**
	 * Checks for key.
	 *
	 * @return true, if successful
	 */
	public boolean hasKey() {
		return !keyPredicates.isEmpty();
	}
}
