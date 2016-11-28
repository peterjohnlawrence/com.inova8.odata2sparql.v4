package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.Locale;

import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;

public class Util {
	public static EdmEntitySet getNavigationTargetEntitySet(EdmEntitySet startEdmEntitySet,
			EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException {

		EdmEntitySet navigationTargetEntitySet = null;

		String navPropName = edmNavigationProperty.getName();
		
		EdmEntityType bindingTargetEntityType = startEdmEntitySet.getEntityType().getNavigationProperty(navPropName).getType();
		EdmBindingTarget edmBindingTarget=null;
		for(EdmEntitySet entitySet : startEdmEntitySet.getEntityContainer().getEntitySets()){		
			if(entitySet.getEntityType().equals(bindingTargetEntityType) ){
				edmBindingTarget = entitySet;
				break;
			}
		}	
		//edmBindingTarget = startEdmEntitySet.getRelatedBindingTarget(navPropName);
		if (edmBindingTarget == null) {
			throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
					Locale.ROOT);
		}

		if (edmBindingTarget instanceof EdmEntitySet) {
			navigationTargetEntitySet = (EdmEntitySet) edmBindingTarget;
		} else {
			throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
					Locale.ROOT);
		}

		return navigationTargetEntitySet;
	}
//	public static String URLDecodeEntityKey(String encodedEntityKey) {
//
//		String decodedEntityKey = encodedEntityKey;
//		decodedEntityKey = encodedEntityKey.replace("@", "/");
//		decodedEntityKey = encodedEntityKey.replace("%3A", ":");
//		return decodedEntityKey;
//	}
//	public static String URLEncodeEntityKey(String entityKey) {
//		String encodedEntityKey = entityKey;
//		encodedEntityKey = encodedEntityKey.replace("/", "@");
//		//Required by Batch otherwise URIs fail
//		encodedEntityKey = encodedEntityKey.replace(":", "%3A");
//		return encodedEntityKey;
//	}
}
