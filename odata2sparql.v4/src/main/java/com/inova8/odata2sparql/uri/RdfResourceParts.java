package com.inova8.odata2sparql.uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.CustomQueryOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.Util;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;

public class RdfResourceParts {

	private final Logger log = LoggerFactory.getLogger(RdfResourceParts.class);
	final ArrayList<RdfResourcePart> rdfResourceParts = new ArrayList<RdfResourcePart>();
	final RdfEdmProvider rdfEdmProvider;
	private RdfResourcePart lastResourcePart;
	private String lastPropertyName;
	private String lastNavPropertyName;
	private EdmComplexType lastComplexType;
	private RdfProperty lastComplexProperty;
	private RdfNavigationProperty lastNavProperty;
	private RdfEntityType responseRdfEntityType;
	private RdfResourcePart penultimateResourcePart;
	private boolean isRef;
	private boolean isFunction;
	private String decodedKey;
	private String subjectId;
	private String localKey;
	private String targetSubjectId;
	private String entityString;
	private String navPathString;
	private int size;
	private Map<String,Object> customQueryOptions;
	private EdmEntitySet responseEntitySet;
	private UriInfo uriInfo;
	private UriType uriType;
	private RdfResourceEntitySet entitySet;
	private static UrlValidator urlValidator = new UrlValidator();
	public RdfResourceParts(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo) throws EdmException, ODataException, OData2SparqlException {
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
				isRef = uriResourceParts.get(uriResourceParts.size() - 1).toString().equals("$ref");
				break;
			case function:
				UriResourceFunction function = (UriResourceFunction) resourcePart;
				EdmEntitySet edmEntitySet = function.getFunctionImport().getReturnedEntitySet();
				rdfResourceParts
						.add(new RdfResourceEntitySet(this.rdfEdmProvider, edmEntitySet, function.getParameters()));
				isFunction = true;
				break;
			default:
				log.error(resourcePart.getKind().toString() + " not handled in resourceParts constructor");
				break;
			}
		}
		build();
	}

	public ContextURL contextUrl(ODataRequest request, OData odata) throws ODataApplicationException {
		ContextURL contextUrl = null;
		SelectOption selectOption = uriInfo.getSelectOption();
		ExpandOption expandOption = uriInfo.getExpandOption();
		try {
			//Need absolute URI for PowerQuery and Linqpad (and probably other MS based OData clients) URLEncoder.encode(q, "UTF-8");
			String selectList = odata.createUriHelper()
					.buildContextURLSelectList(this.getResponseEntitySet().getEntityType(), expandOption, selectOption);
			switch (uriType) {
			case URI1:
				/**
				 * Entity set
				 */
				contextUrl = ContextURL.with().entitySet(this.getEntitySet().getEdmEntitySet()).selectList(selectList)
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI2:
				/**
				 * Entity set with key predicate
				 */
				contextUrl = ContextURL.with().entitySetOrSingletonOrType(this.getEntitySet().getEntitySetName())// getEdmEntitySet().getEntityType().getName())
						.suffix(ContextURL.Suffix.ENTITY).selectList(selectList).oDataPath(request.getRawBaseUri())
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI3:
				/**
				 * Complex property of an entity
				 */
				contextUrl = ContextURL.with().entitySet(this.getEntitySet().getEdmEntitySet())
						.keyPath(this.getLocalKey()).navOrPropertyPath(this.getNavPathString())
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI4:
				/**
				 * Simple property of a complex property of an entity
				 */
				contextUrl = ContextURL.with().entitySet(this.getEntitySet().getEdmEntitySet())
						.keyPath(this.getLocalKey()).navOrPropertyPath(this.getNavPathString())
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI5:
				/**
				 * Simple property of an entity
				 */
				contextUrl = ContextURL.with().entitySet(this.getEntitySet().getEdmEntitySet())
						.keyPath(this.getLocalKey()).navOrPropertyPath(this.getNavPathString())
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI6A:
				/**
				 * Navigation property of an entity with target multiplicity '1'
				 * or '0..1'
				 */
				contextUrl = ContextURL.with().entitySet(this.getEntitySet().getEdmEntitySet())
						.keyPath(this.getLocalKey()).navOrPropertyPath(this.getNavPathString())
						.suffix(ContextURL.Suffix.ENTITY).selectList(selectList)
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI6B:
				/**
				 * Navigation property of an entity with target multiplicity '*'
				 */
				contextUrl = ContextURL.with().entitySet(this.getEntitySet().getEdmEntitySet())
						.keyPath(this.getLocalKey()).navOrPropertyPath(this.getNavPathString()).selectList(selectList)
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI7A:
				/**
				 * Link to a single entity
				 */
				contextUrl = ContextURL.with().suffix(ContextURL.Suffix.REFERENCE)
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI7B:
				/**
				 * Link to multiple entities
				 */
				contextUrl = ContextURL.with().asCollection().suffix(ContextURL.Suffix.REFERENCE)
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI10:
				/**
				 * Function import returning a single instance of an entity type
				 */
				break;
			case URI11:
				/**
				 * Function import returning a collection of complex-type
				 * instances
				 */
				contextUrl = ContextURL.with()
						.entitySetOrSingletonOrType(this.getEntitySet().getEdmEntitySet().getEntityType().getName())
						.suffix(ContextURL.Suffix.ENTITY).selectList(selectList).oDataPath(request.getRawBaseUri())
						.serviceRoot(new URI(request.getRawBaseUri() + "/")).build();
				break;
			case URI12:
				/**
				 * Function import returning a single instance of a complex type
				 */
				break;
			case URI13:
				/**
				 * Function import returning a collection of primitive-type
				 * instances
				 */
				break;
			case URI14:
				/**
				 * Function import returning a single instance of a primitive
				 * type
				 */
				break;
			case URI15:
				/**
				 * Count of an entity set
				 */
				break;
			default:
				break;
			}
		} catch (URISyntaxException | SerializerException e) {
			throw new ODataApplicationException("Invalid RawBaseURI " + request.getRawBaseUri(),
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
		}
		return contextUrl;

	}

	private void build() throws EdmException, ODataException, OData2SparqlException {
		uriType = _getUriType();
		lastResourcePart = _getLastResourcePart();
		lastPropertyName = _getLastPropertyName();
		lastNavPropertyName = _getLastNavPropertyName();
		lastComplexType = _getLastComplexType();
		lastComplexProperty = _getLastComplexProperty();
		responseEntitySet = _getResponseEntitySet();
		responseRdfEntityType = _getResponseRdfEntityType();
		penultimateResourcePart = _getPenultimateResourcePart();
		decodedKey = _getDecodedKey();
		localKey = _getLocalKey();
		subjectId = _getSubjectId();
		targetSubjectId = _getTargetSubjectId();
		entitySet = _getEntitySet();
		entityString = _getEntityString();
		navPathString = _getNavPathString();
		lastNavProperty = _getLastNavProperty();
		size = _size();
		customQueryOptions = _getCustomQueryOption();
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
	public String getLastNavPropertyName() {
		return lastNavPropertyName;
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
	public String getValidatedSubjectIdUrl() throws OData2SparqlException {
		return validatedId(subjectId);
	}
	public String getLocalKey() {
		return localKey;
	}

	public String getTargetSubjectId() {
		return targetSubjectId;
	}
	public String getValidatedTargetSubjectIdUrl() throws OData2SparqlException {
		return validatedId(targetSubjectId);
	}

	private String validatedId(String id) throws OData2SparqlException {
		if (id != null) {
			String expandedKey = rdfEdmProvider.getRdfModel().getRdfPrefixes().expandPredicateKey(id);
			if (urlValidator.isValid(expandedKey)) {
				return expandedKey;
			} else {
				throw new EdmException("Invalid key: " + id, null);
			}
		} else {
			return null;
		}
	}
	public RdfResourceEntitySet getEntitySet() {
		return entitySet;
	}

	public String getEntityString() {
		return entityString;
	}

	public String getNavPathString() {
		return navPathString;
	}

	public int size() {
		return size;
	}
	
	private Map<String, Object> _getCustomQueryOption() {
		HashMap<String, Object> customQueryOptions = new HashMap<String,Object>();
		customQueryOptions.put(RdfConstants.SERVICE, "<"+ this.rdfEdmProvider.getRdfRepository().getDataRepository().getServiceUrl()+">");
		if (this.uriInfo.getCustomQueryOptions().size() == 0) {
			return customQueryOptions;
		} else {		
			for (CustomQueryOption customQueryOption:this.uriInfo.getCustomQueryOptions()) {
				customQueryOptions.put(customQueryOption.getName(), customQueryOption.getText());
			}
			return customQueryOptions;
		}
	}

	private RdfResourcePart _getLastResourcePart() {
		if (rdfResourceParts.size() == 0) {
			return null;
		} else {
			return rdfResourceParts.get(rdfResourceParts.size() - 1);
		}

	}

	private String _getLastPropertyName() {
		RdfResourcePart lastResourcePart = _getLastResourcePart();
		if (lastResourcePart != null) {
			if (lastResourcePart.uriResourceKind.equals(UriResourceKind.primitiveProperty)) {
				return ((RdfResourceProperty) lastResourcePart).getEdmProperty().getName();
			} else if (lastResourcePart.uriResourceKind.equals(UriResourceKind.complexProperty)) {
				return ((RdfResourceComplexProperty) lastResourcePart).getComplexType().getName();
			}
		}
		return null;
	}
	private RdfNavigationProperty _getLastNavProperty() {
		RdfResourcePart lastResourcePart = _getLastResourcePart();
		if (lastResourcePart != null) {
			if (lastResourcePart.uriResourceKind.equals(UriResourceKind.navigationProperty)) {
				EdmNavigationProperty edmNavigationProperty =  ((RdfResourceNavigationProperty) lastResourcePart).getEdmNavigationProperty();
				RdfNavigationProperty navProperty = this.getEntitySet().getRdfEntityType().findNavigationPropertyByEDMNavigationPropertyName(edmNavigationProperty.getName());
				return navProperty;
			} else if (lastResourcePart.uriResourceKind.equals(UriResourceKind.complexProperty)) {
				return null;//TODO
			}
		}
		return null;

	}
	private String _getLastNavPropertyName() {
		RdfResourcePart lastResourcePart = _getLastResourcePart();
		if (lastResourcePart != null) {
			if (lastResourcePart.uriResourceKind.equals(UriResourceKind.navigationProperty)) {
				return ((RdfResourceNavigationProperty) lastResourcePart).getEdmNavigationProperty().getName();
			} else if (lastResourcePart.uriResourceKind.equals(UriResourceKind.complexProperty)) {
				return ((RdfResourceComplexProperty) lastResourcePart).getComplexType().getName();
			}
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
		EdmComplexType edmComplexType = null;
		int complexIndex = 0;
		for (int j = rdfResourceParts.size() - 1; j >= 0; j--) {
			if (rdfResourceParts.get(j).getUriResourceKind().equals(UriResourceKind.complexProperty)) {
				edmComplexType = ((RdfResourceComplexProperty) rdfResourceParts.get(j)).getComplexType();
				complexIndex = j;
				break;
			}
		}
		if (edmComplexType == null) {
			return null;
		} else {
			RdfEntityType rdfEntityType = null;
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(getEntitySet(complexIndex - 1));
			return rdfEntityType.findProperty(edmComplexType.getName().replace(RdfConstants.SHAPE_POSTFIX, ""));
		}
	}

	private RdfEntityType _getResponseRdfEntityType() throws EdmException, ODataApplicationException {
		if (isFunction) {
			return responseRdfEntityType;
		} else {
			return rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(_getResponseEntitySet());
		}
	}

	private RdfResourcePart _getPenultimateResourcePart() {
		if (_size() > 1) {
			return rdfResourceParts.get(rdfResourceParts.size() - 2);
		} else {
			return null;
		}
	}

	private String _getDecodedKey() throws OData2SparqlException {
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0).getDecodedKey();
		return null;
	}

	private String _getLocalKey() {
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0).getLocalKey();
		return null;
	}

	private String _getSubjectId() {
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0).getSubjectId();
		return null;
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
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0).geEntityString();
		return null;
	}

	private RdfResourceEntitySet _getEntitySet() {
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0);
		return null;
	
	}

	private String _getNavPathString() {
		if (rdfResourceParts.size() == 0) {
			return null;
		} else {
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
	}
	public ArrayList<RdfResourcePart> getNavPath() {
		ArrayList<RdfResourcePart> navParts = new ArrayList<RdfResourcePart>();
		if (rdfResourceParts.size() == 0) {
			return null;
		} else {
			for (RdfResourcePart rdfResourcePart : rdfResourceParts.subList(1, rdfResourceParts.size())) {
				navParts.add(rdfResourcePart);
			}
			if (navParts.isEmpty()) {
				return null;
			} else {
				return navParts;
			}
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
				if (getResourceKind(j - 1).equals(UriResourceKind.complexProperty)) {
					return Util.getNavigationTargetEntitySet(getPriorEntitySet(j),
							getAsComplexProperty(j - 1).complexType,
							getAsNavigationProperty(j).getEdmNavigationProperty());
				} else {
					return Util.getNavigationTargetEntitySet(getPriorEntitySet(j),
							getAsNavigationProperty(j).getEdmNavigationProperty());
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

	private UriType _getUriType() throws ODataException {
		if (isFunction()) {
			UriResource functionResource = uriInfo.asUriInfoResource().getUriResourceParts().get(0);
			CsdlFunctionImport functionImport = rdfEdmProvider.getEntityContainer()
					.getFunctionImport(functionResource.getSegmentValue());
			List<CsdlFunction> function = rdfEdmProvider.getFunctions(functionImport.getFunctionFQN());
			CsdlReturnType functionReturnType = function.get(0).getReturnType();

			responseRdfEntityType = rdfEdmProvider.getMappedEntityType(functionReturnType.getTypeFQN());
			if (functionReturnType.isCollection()) {
				//URI11/13
				return UriType.URI11;
			} else {
				//URI10/12
				return UriType.URI10;
			}
		} else {
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
					if (isRef)
						return UriType.URI7A;
					else
						return UriType.URI2;
				} else if (lastNavigationProperty.getEdmNavigationProperty().isCollection()) {
					// <entityPath>/NavSet
					if (isRef)
						return UriType.URI7B;
					else
						return UriType.URI6B;
				} else {
					// <entityPath>/NavProp
					if (isRef)
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
		}
		return null;
	}

	private RdfResourceEntitySet getAsEntitySet(int index) {
		if (rdfResourceParts.size() == 0) {
			return null;
		} else {
		}
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
			return getEntitySet(index-1);
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
			if (startIndex > 0)
				break;
		}
		EdmEntitySet edmEntitySet = startEntitySet.getEdmEntitySet();
		for (int j = startIndex + 1; j < index; j++) {
			switch (rdfResourceParts.get(j).getUriResourceKind()) {
			case navigationProperty:
				edmEntitySet = Util.getNavigationTargetEntitySet(edmEntitySet,
						getAsNavigationProperty(j).getEdmNavigationProperty());
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

	public boolean isFunction() {
		return isFunction;
	}

	public RdfNavigationProperty getLastNavProperty() {
		return lastNavProperty;
	}

	public Map<String,Object> getCustomQueryOptions() {
		return customQueryOptions;
	}
	public String getCustomQueryOptionsArgs() {
		if(getCustomQueryOptions()!=null) {
			String customQueryOptionsArgs ="";
			for( Entry<String, Object> customQueryOptionEntry:getCustomQueryOptions().entrySet()) {
				customQueryOptionsArgs +=",'"+ customQueryOptionEntry.getKey() + "'," + customQueryOptionEntry.getValue();
			}
			return customQueryOptionsArgs;
		}else {
			return "";
		}
	}

}
