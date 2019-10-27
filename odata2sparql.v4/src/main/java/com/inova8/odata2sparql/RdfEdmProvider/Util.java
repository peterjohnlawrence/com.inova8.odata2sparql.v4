package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

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
	public static EdmEntitySet getNavigationTargetEntitySet(List<EdmEntitySet> edmEntitySets, EdmEntityType startEdmEntityType,
			EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException {

		EdmEntitySet navigationTargetEntitySet = null;

		String navPropName = edmNavigationProperty.getName();
		
		EdmEntityType bindingTargetEntityType = startEdmEntityType;
		EdmBindingTarget edmBindingTarget=null;
		for(EdmEntitySet entitySet : edmEntitySets){		
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
	public static EdmEntitySet getEdmEntitySet(UriInfoResource uriInfo) throws ODataApplicationException {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
         // To get the entity set we have to interpret all URI segments
        if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Invalid resource type for first segment.",
                                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),Locale.ENGLISH);
        }

        UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

        return uriResource.getEntitySet();
    }
	public static EdmEntitySet getNavigationTargetEntitySet(EdmEntitySet startEdmEntitySet, EdmComplexType complexType,
			EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException {
		EdmEntitySet navigationTargetEntitySet = null;

		String navPropName = edmNavigationProperty.getName();
		
		EdmEntityType bindingTargetEntityType = complexType.getNavigationProperty(navPropName).getType();
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
}
