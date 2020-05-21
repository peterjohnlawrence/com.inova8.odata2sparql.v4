package com.inova8.odata2sparql.uri;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;

public class RdfResourceNavigationProperty extends RdfResourcePart {
	RdfEdmProvider rdfEdmProvider;
	EdmNavigationProperty edmNavigationProperty;
	List<UriParameter> keyPredicates;

	public RdfResourceNavigationProperty(RdfEdmProvider rdfEdmProvider, UriResourceNavigation uriResourceNavigation) {
		super.setUriResourceKind(UriResourceKind.navigationProperty);
		this.rdfEdmProvider = rdfEdmProvider;
		keyPredicates = uriResourceNavigation.getKeyPredicates();
		EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
		this.edmNavigationProperty = edmNavigationProperty;
	}

	public EdmNavigationProperty getEdmNavigationProperty() {
		return edmNavigationProperty;
	}

	public String getNavPath() {
		if (!keyPredicates.isEmpty()) {
			return edmNavigationProperty.getName() + "(" + getLocalKey() + ")";
		} else {
			return edmNavigationProperty.getName();
		}
	}

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

	public boolean hasKey() {
		return !keyPredicates.isEmpty();
	}
}
