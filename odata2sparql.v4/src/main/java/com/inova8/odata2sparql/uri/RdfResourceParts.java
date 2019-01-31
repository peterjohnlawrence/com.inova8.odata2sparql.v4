package com.inova8.odata2sparql.uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;

public class RdfResourceParts {
	private final Logger log = LoggerFactory.getLogger(RdfResourceParts.class);
	final ArrayList<RdfResourcePart> rdfResourceParts = new ArrayList<RdfResourcePart>();
	final RdfEdmProvider rdfEdmProvider;
	private RdfResourcePart lastResourcePart;
	private String lastPropertyName;
	private EdmComplexType lastComplexType;
	private RdfProperty lastComplexProperty;
	private RdfEntityType responseRdfEntityType;
	private RdfResourcePart penultimateResourcePart;
	private boolean isRef;
	private String decodedKey;
	private String subjectId;
	private String localKey;
	private String targetSubjectId;
	private String entityString;
	private String navPath;
	private int size;
	private EdmEntitySet responseEntitySet;
	private UriInfo uriInfo;
	private UriType uriType;
	private RdfResourceEntitySet entitySet;

	public RdfResourceParts(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo) throws EdmException, ODataApplicationException {
		this.rdfEdmProvider = rdfEdmProvider;
		this.uriInfo = uriInfo;
		for (UriResource resourcePart : uriInfo.getUriResourceParts()) {
			switch (resourcePart.getKind()) {
			case entitySet:
				UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePart;
				rdfResourceParts.add(new RdfResourceEntitySet(this.rdfEdmProvider, uriResourceEntitySet));
				break;
			case navigationProperty:
				UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePart;
				rdfResourceParts.add(new RdfResourceNavigationProperty(this.rdfEdmProvider, uriResourceNavigation));
				break;
			case complexProperty:
				UriResourceComplexProperty uriResourceComplexProperty = (UriResourceComplexProperty) resourcePart;
				rdfResourceParts.add(new RdfResourceComplexProperty(this.rdfEdmProvider, uriResourceComplexProperty));
				break;
			case primitiveProperty:
				UriResourcePrimitiveProperty uriResourcePrimitiveProperty = (UriResourcePrimitiveProperty) resourcePart;
				rdfResourceParts.add(new RdfResourceProperty(this.rdfEdmProvider, uriResourcePrimitiveProperty));
				break;
			case ref:
				List<UriResource> uriResourceParts = uriInfo.asUriInfoResource().getUriResourceParts();
				isRef = uriResourceParts.get(uriResourceParts.size()-1).toString().equals("$ref");
				break;
			default:
				log.error(resourcePart.getKind().toString() + " not handled in resourceParts constructor");
				break;

			}
		}
		build();
	}
	public ContextURL contextUrl(ODataRequest request,OData odata) throws ODataApplicationException {
		ContextURL contextUrl = null;
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();
		try {
			//Need absolute URI for PowerQuery and Linqpad (and probably other MS based OData clients) URLEncoder.encode(q, "UTF-8");
			String selectList = odata.createUriHelper().buildContextURLSelectList(this.getResponseEntitySet().getEntityType(), expandOption,
					selectOption);
			switch(uriType) {
			case URI1:
				contextUrl = ContextURL.with()
						.entitySet(this.getEntitySet()
						.getEdmEntitySet())
						.selectList(selectList)
						.serviceRoot(new URI(request.getRawBaseUri() + "/"))
						.build();				
				break;
			case URI2:
				contextUrl = ContextURL.with()
						//.entitySet(rdfResourceParts.getEntitySet().getEdmEntitySet())
						//.keyPath(rdfResourceParts.getLocalKey())
						.entitySetOrSingletonOrType(this.getEntitySet().getEdmEntitySet().getEntityType().getName())
						.suffix(ContextURL.Suffix.ENTITY)
						.selectList(selectList)
						.oDataPath(request.getRawBaseUri())
						.serviceRoot(new URI(request.getRawBaseUri() + "/"))
						.build();
				break;
			case URI3: break;
			case URI4: 
				contextUrl = ContextURL.with()
						.entitySet(this.getEntitySet().getEdmEntitySet())
						.keyPath(this.getLocalKey())
						.navOrPropertyPath(this.getNavPath())
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();				
				break;
			case URI5: 
				contextUrl = ContextURL.with()
						.entitySet(this.getEntitySet().getEdmEntitySet())
						.keyPath(this.getLocalKey())
						.navOrPropertyPath(this.getNavPath())
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;	
			case URI6A:
				contextUrl = ContextURL.with()
						.entitySet(this.getEntitySet().getEdmEntitySet())
						.keyPath(this.getLocalKey())
						.navOrPropertyPath(this.getNavPath())
						.suffix(ContextURL.Suffix.ENTITY)
						.selectList(selectList)
						.serviceRoot(new URI(request.getRawBaseUri() + "/"))
						.build();				
				break;
			case URI6B: 
				contextUrl = ContextURL.with()
						.entitySet(this.getEntitySet()
						.getEdmEntitySet())
						.keyPath(this.getLocalKey())
						.navOrPropertyPath(this.getNavPath())
						.selectList(selectList)
						.serviceRoot(new URI(request.getRawBaseUri() + "/"))
						.build();	
				break;
			case URI7A: 
				contextUrl = ContextURL.with()
					.suffix(ContextURL.Suffix.REFERENCE)
					.serviceRoot(new URI(request.getRawBaseUri() + "/"))
					.build();					
				break;
			case URI7B: 
				contextUrl = ContextURL.with()
						.asCollection()
						.suffix(ContextURL.Suffix.REFERENCE)
						.serviceRoot(new URI(request.getRawBaseUri() + "/"))
						.build();	
				break;
			case URI14: break;
			case URI15: break;
			default:
				break;
			}
		} catch (URISyntaxException | SerializerException e) {
			throw new ODataApplicationException("Invalid RawBaseURI " + request.getRawBaseUri(),
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
		}
		return contextUrl;

	}
	private void build() throws EdmException, ODataApplicationException {
		uriType = _getUriType();
		lastResourcePart = _getLastResourcePart();
		lastPropertyName = _getLastPropertyName();
		lastComplexType = _getLastComplexType();
		lastComplexProperty=_getLastComplexProperty();
		responseEntitySet=_getResponseEntitySet();
		responseRdfEntityType=_getResponseRdfEntityType();
		penultimateResourcePart = _getPenultimateResourcePart();
		decodedKey= _getDecodedKey();
		localKey= _getLocalKey();
		subjectId=_getSubjectId();
		targetSubjectId=_getTargetSubjectId();
		entitySet = _getEntitySet();
		entityString=_getEntityString();
		navPath=_getNavPath();
		size=_size();
	}
	public UriInfo getUriInfo() {
		return uriInfo;
	}
	public UriType getUriType() {
		return uriType;
	}
	public RdfResourcePart getLastResourcePart() {
		return lastResourcePart;
	}
	public String getLastPropertyName() {
		return lastPropertyName;
	}
	public EdmComplexType getLastComplexType() {
		return lastComplexType;
	}
	public RdfProperty getLastComplexProperty() {
		return lastComplexProperty;
	}
	public EdmEntitySet getResponseEntitySet() {
		return responseEntitySet;
	}
	public RdfEntityType getResponseRdfEntityType() {
		return responseRdfEntityType;
	}
	public RdfResourcePart getPenultimateResourcePart() {
		return penultimateResourcePart;
	}
	public String getDecodedKey() {
		return decodedKey;
	}
	public String getSubjectId() {
		return subjectId;
	}
	public String getLocalKey() {
		return localKey;
	}
	public String getTargetSubjectId() {
		return targetSubjectId;
	}
	public RdfResourceEntitySet getEntitySet() {
		return entitySet;
	}
	public String getEntityString() {
		return entityString;
	}
	public String getNavPath() {
		return navPath;
	}
	public int size() {
		return size;
	}
	private RdfResourcePart _getLastResourcePart() {
		return rdfResourceParts.get(rdfResourceParts.size() - 1);
	}

	private String _getLastPropertyName() {
		RdfResourcePart lastResourcePart = _getLastResourcePart();
		if (lastResourcePart.uriResourceKind.equals(UriResourceKind.primitiveProperty)) {
			return ((RdfResourceProperty) lastResourcePart).getEdmProperty().getName();
		} else if (lastResourcePart.uriResourceKind.equals(UriResourceKind.complexProperty)) {
			return ((RdfResourceComplexProperty) lastResourcePart).getComplexType().getName();
		}
		return null;
	}

	private EdmComplexType _getLastComplexType() {
		for (int j = rdfResourceParts.size() - 1; j >= 0; j--) {
			switch (rdfResourceParts.get(j).getUriResourceKind()) {
			case complexProperty:
				return ((RdfResourceComplexProperty) rdfResourceParts.get(j)).getComplexType();
			default:
				break;
			}
		}
		return null;
	}

	private RdfProperty _getLastComplexProperty() throws EdmException, ODataApplicationException {	
		EdmComplexType edmComplexType = null ;
		int complexIndex = 0;
		for (int j = rdfResourceParts.size() - 1; j >= 0; j--) {
			if( rdfResourceParts.get(j).getUriResourceKind().equals(UriResourceKind.complexProperty)) {
				edmComplexType = ((RdfResourceComplexProperty) rdfResourceParts.get(j)).getComplexType();
				complexIndex = j;
				break;
			}
		}
		if(edmComplexType == null )
		{
			return null;
		}else {
			RdfEntityType rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(getEntitySet(complexIndex-1));

			return rdfEntityType.findProperty(edmComplexType.getName());
		}
	}
	private RdfEntityType _getResponseRdfEntityType() throws EdmException, ODataApplicationException {
		return rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(_getResponseEntitySet());
	}
	private RdfResourcePart _getPenultimateResourcePart() {
		if (_size() > 1) {
			return rdfResourceParts.get(rdfResourceParts.size() - 2);
		} else {
			return null;
		}
	}
	private String _getDecodedKey() {
		return getAsEntitySet(0).getDecodedKey();
	}

	private String _getLocalKey() {

		return getAsEntitySet(0).getLocalKey();
	}

	private String _getSubjectId() {

		return getAsEntitySet(0).getSubjectId();
	}

	private String _getTargetSubjectId() {

		for (int j = rdfResourceParts.size() - 1; j >= 0; j--) {
			switch (rdfResourceParts.get(j).getUriResourceKind()) {
			case navigationProperty:
				if (getAsNavigationProperty(j).hasKey()) {
					return getAsNavigationProperty(j).getSubjectId();
				} else if (!getAsNavigationProperty(j).getEdmNavigationProperty().isCollection()) {
					return null;
				}
				break;
			case entitySet:
				if (getAsEntitySet(j).hasKey()) {
					return getAsEntitySet(j).getSubjectId();
				}
				break;
			default:
				break;
			}
		}
		return null;
	}

	private String _getEntityString() {

		return getAsEntitySet(0).geEntityString();
	}
	private RdfResourceEntitySet _getEntitySet() {

		return getAsEntitySet(0);
	}
	private String _getNavPath() {
		String navPath = "";
		for (RdfResourcePart rdfResourcePart : rdfResourceParts.subList(1, rdfResourceParts.size())) {
			navPath = navPath + rdfResourcePart.getNavPath() + "/";
		}
		if (navPath.isEmpty()) {
			return null;
		} else {
			return navPath.substring(0, navPath.length() - 1);
		}
	}

	private int _size() {
		return rdfResourceParts.size();
	}
	private EdmEntitySet _getResponseEntitySet() throws ODataApplicationException {

		for (int j = rdfResourceParts.size() - 1; j >= 0; j--) {
			switch (rdfResourceParts.get(j).getUriResourceKind()) {
			case navigationProperty:
				//EdmNavigationProperty edmNavigationProperty = getAsNavigationProperty(j).getEdmNavigationProperty();
				if(getResourceKind(j-1).equals(UriResourceKind.complexProperty)) {
					return Util.getNavigationTargetEntitySet(getPriorEntitySet(j), getAsComplexProperty(j-1).complexType, getAsNavigationProperty(j).getEdmNavigationProperty());  
				}else {
					return Util.getNavigationTargetEntitySet(getPriorEntitySet(j),  getAsNavigationProperty(j).getEdmNavigationProperty());  
				}
			case entitySet:
				return getAsEntitySet(j).getEdmEntitySet();
			default:
				break;
			}
		}
		return (EdmEntitySet) null;
	}

	// <entitySetPath> ::= EntitySet | <entityPath> / NavSet | <complexPath> / NavSet
	// <entityPath>    ::=  <entitySetPath> ( Key ) | <entityPath> / NavProp | <complexPath> / NavProp
	// <complexPath>   ::= <entityPath> / Complex
	// <propertyPath>  ::= <entityPath> / Property

	private UriType _getUriType() {
		if (rdfResourceParts.size() == 1) {
			if (getAsEntitySet(0).hasKey()) {
				// <entitySetPath>/Key
				return UriType.URI2;
			} else {
				// <entitySetPath>
				return UriType.URI1;
			}
		} else if (_getLastResourcePart().getUriResourceKind().equals(UriResourceKind.navigationProperty)) {
			RdfResourceNavigationProperty lastNavigationProperty = ((RdfResourceNavigationProperty) (_getLastResourcePart()));

			if (lastNavigationProperty.hasKey()) {
				// <entitySetPath>/Key
				if(isRef)
					return UriType.URI7A;
				else
					return UriType.URI2;
			} else if (lastNavigationProperty.getEdmNavigationProperty().isCollection()) {
				// <entityPath>/NavSet
				if(isRef)
					return UriType.URI7B;
				else
					return UriType.URI6B;
			} else {
				// <entityPath>/NavProp
				if(isRef)
					return UriType.URI7A;
				else
					return UriType.URI6A;
			}
		} else if (_getLastResourcePart().getUriResourceKind().equals(UriResourceKind.complexProperty)) {
			//	<entityPath>/Complex
			return UriType.URI3;

		} else if (_getLastResourcePart().getUriResourceKind().equals(UriResourceKind.primitiveProperty)) {
			//return UriType.URI4/5
			if (_getPenultimateResourcePart().getUriResourceKind().equals(UriResourceKind.complexProperty)) {
				// <complexPath>/Property
				return UriType.URI4;
			} else {
				//<entityPath>/Property
				return UriType.URI5;
			}
		}
		return null;
	}

	private RdfResourceEntitySet getAsEntitySet(int index) {

		return (RdfResourceEntitySet) (rdfResourceParts.get(index));
	}

	private EdmEntitySet getEntitySet(int index) {
		switch (getResourceKind(index)) {
		case entitySet:
			return getAsEntitySet(index).getEdmEntitySet();
		case navigationProperty:
			EdmEntityType entityType = getAsNavigationProperty(index).getEdmNavigationProperty().getType();

			EdmEntitySet startEntitySet = null;
			for (int i = index - 1; i >= 0; i--) {
				if (getResourceKind(i).equals(UriResourceKind.entitySet)) {
					startEntitySet = getAsEntitySet(i).getEdmEntitySet();
				}
			}
			for (EdmEntitySet entitySet : startEntitySet.getEntityContainer().getEntitySets()) {
				if (entitySet.getEntityType().equals(entityType)) {
					return entitySet;
				}
			}
			return null;
		case complexProperty:
		default:
			log.error(getResourceKind(index).toString() + " not handled for getEntitySet");
			return null;
		}
	}

	public RdfResourceNavigationProperty getAsNavigationProperty(int index) {
		return (RdfResourceNavigationProperty) (rdfResourceParts.get(index));
	}

	public RdfResourceComplexProperty getAsComplexProperty(int index) {
		return (RdfResourceComplexProperty) (rdfResourceParts.get(index));
	}

	@SuppressWarnings("unused")
	private RdfResourceProperty getAsProperty(int index) {
		return (RdfResourceProperty) (rdfResourceParts.get(index));
	}

	public UriResourceKind getResourceKind(int index) {
		return rdfResourceParts.get(index).getUriResourceKind();
	}


	private EdmEntitySet getPriorEntitySet(int index) throws ODataApplicationException {
		int startIndex = 0;
		RdfResourceEntitySet startEntitySet = getAsEntitySet(0);
		for (int j = index - 1; j > 0; j--) {
			switch (rdfResourceParts.get(j).getUriResourceKind()) {
			case entitySet:
				startIndex = j;
				startEntitySet = getAsEntitySet(j);
				break;
			default:
				break;
			}
			if (startIndex>0) break;
		}
		EdmEntitySet edmEntitySet = startEntitySet.getEdmEntitySet();
		for (int j = startIndex+1; j < index; j++) {
			switch (rdfResourceParts.get(j).getUriResourceKind()) {
			case navigationProperty:
				edmEntitySet=Util.getNavigationTargetEntitySet(edmEntitySet,  getAsNavigationProperty(j).getEdmNavigationProperty());
				break;
			default:
				break;
			}
		}
		return (EdmEntitySet) edmEntitySet;
	}
	public Boolean isValueRequest() {
		UriResource lastResourcePart = uriInfo.getUriResourceParts().get(uriInfo.getUriResourceParts().size() - 1);
		return lastResourcePart.getSegmentValue().equals("$value");
	}
	public Boolean isRefRequest() {
		UriResource lastResourcePart = uriInfo.getUriResourceParts().get(uriInfo.getUriResourceParts().size() - 1);
		return lastResourcePart.getSegmentValue().equals("$ref");
	}

}
