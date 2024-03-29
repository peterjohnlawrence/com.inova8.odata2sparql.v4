/*
 * inova8 2020
 */
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
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmReturnType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
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

/**
 * The Class RdfResourceParts.
 */
public class RdfResourceParts {

	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfResourceParts.class);
	
	/** The rdf resource parts. */
	final ArrayList<RdfResourcePart> rdfResourceParts = new ArrayList<RdfResourcePart>();
	
	/** The rdf edm provider. */
	final RdfEdmProvider rdfEdmProvider;
	
	/** The last resource part. */
	private RdfResourcePart lastResourcePart;
	
	/** The last property name. */
	private String lastPropertyName;
	
	/** The last nav property name. */
	private String lastNavPropertyName;
	
	/** The last complex type. */
	private EdmComplexType lastComplexType;
	
	/** The last complex property. */
	private RdfProperty lastComplexProperty;
	
	/** The last nav property. */
	private RdfNavigationProperty lastNavProperty;
	
	/** The response rdf entity type. */
	private RdfEntityType responseRdfEntityType;
	
	/** The penultimate resource part. */
	private RdfResourcePart penultimateResourcePart;
	
	/** The is ref. */
	private boolean isRef;
	
	/** The is function. */
	private boolean isFunction;
	
	/** The decoded key. */
	private String decodedKey;
	
	/** The subject id. */
	private String subjectId;
	
	/** The local key. */
	private String localKey;
	
	/** The target subject id. */
	private String targetSubjectId;
	
	/** The entity string. */
	private String entityString;
	
	/** The nav path string. */
	private String navPathString;
	
	/** The size. */
	private int size;
	
	/** The custom query options. */
	private Map<String, Object> customQueryOptions;
	
	/** The response entity set. */
	private EdmEntitySet responseEntitySet;
	
	/** The uri info. */
	private static UriInfo uriInfo;
	
	/** The uri type. */
	private UriType uriType;
	
	/** The entity set. */
	private RdfResourceEntitySet entitySet;
	
	/** The url validator. */
	private static UrlValidator urlValidator = new UrlValidator();
	
	/**
	 * Gets the parameter.
	 *
	 * @param uriParameter the uri parameter
	 * @return the parameter
	 */
	public static String getParameter(UriParameter uriParameter) {
		String keyValue ;
		if(uriParameter.getAlias()!=null ) {
			keyValue= uriInfo.getValueForAlias(uriParameter.getAlias());
		}else {
			keyValue = uriParameter.getText();
		}
		return keyValue;
 }
	
	/**
	 * Instantiates a new rdf resource parts.
	 *
	 * @param rdfEdmProvider the rdf edm provider
	 * @param uriInfo the uri info
	 * @throws EdmException the edm exception
	 * @throws ODataException the o data exception
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public RdfResourceParts(RdfEdmProvider rdfEdmProvider, UriInfo uriInfo)
			throws EdmException, ODataException, OData2SparqlException {
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
				rdfResourceParts.add(new RdfResourceFunction(this.rdfEdmProvider, function, function.getParameters()));
				isFunction = true;
				break;
			//
			//				
			//				EdmEntitySet edmEntitySet = function.getFunctionImport().getReturnedEntitySet();
			//				
			//				rdfResourceParts
			//						.add(new RdfResourceEntitySet(this.rdfEdmProvider, edmEntitySet, function.getParameters()));
			//				isFunction = true;
			//				break;
			default:
				log.error(resourcePart.getKind().toString() + " not handled in resourceParts constructor");
				break;
			}
		}
		build();
	}

	/**
	 * Context url.
	 *
	 * @param request the request
	 * @param odata the odata
	 * @return the context URL
	 * @throws ODataApplicationException the o data application exception
	 */
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
						.entitySetOrSingletonOrType(this.getResponseEntitySet().getName())
						.selectList(selectList).oDataPath(request.getRawBaseUri())
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

	/**
	 * Builds the.
	 *
	 * @throws EdmException the edm exception
	 * @throws ODataException the o data exception
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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
		localKey = _getLocalKey(uriInfo);
		subjectId = _getSubjectId(uriInfo);
		targetSubjectId = _getTargetSubjectId(uriInfo);
		entitySet = _getEntitySet();
		entityString = _getEntityString(uriInfo);
		navPathString = _getNavPathString();
		lastNavProperty = _getLastNavProperty();
		size = _size();
		customQueryOptions = _getCustomQueryOption();
	}

	/**
	 * Gets the uri info.
	 *
	 * @return the uri info
	 */
	public UriInfo getUriInfo() {
		return uriInfo;
	}

	/**
	 * Gets the uri type.
	 *
	 * @return the uri type
	 */
	public UriType getUriType() {
		return uriType;
	}

	/**
	 * Gets the last resource part.
	 *
	 * @return the last resource part
	 */
	public RdfResourcePart getLastResourcePart() {
		return lastResourcePart;
	}

	/**
	 * Gets the last property name.
	 *
	 * @return the last property name
	 */
	public String getLastPropertyName() {
		return lastPropertyName;
	}

	/**
	 * Gets the last nav property name.
	 *
	 * @return the last nav property name
	 */
	public String getLastNavPropertyName() {
		return lastNavPropertyName;
	}

	/**
	 * Gets the last complex type.
	 *
	 * @return the last complex type
	 */
	public EdmComplexType getLastComplexType() {
		return lastComplexType;
	}

	/**
	 * Gets the last complex property.
	 *
	 * @return the last complex property
	 */
	public RdfProperty getLastComplexProperty() {
		return lastComplexProperty;
	}

	/**
	 * Gets the response entity set.
	 *
	 * @return the response entity set
	 */
	public EdmEntitySet getResponseEntitySet() {
		return responseEntitySet;
	}

	/**
	 * Gets the response rdf entity type.
	 *
	 * @return the response rdf entity type
	 */
	public RdfEntityType getResponseRdfEntityType() {
		return responseRdfEntityType;
	}

	/**
	 * Gets the penultimate resource part.
	 *
	 * @return the penultimate resource part
	 */
	public RdfResourcePart getPenultimateResourcePart() {
		return penultimateResourcePart;
	}

	/**
	 * Gets the decoded key.
	 *
	 * @return the decoded key
	 */
	public String getDecodedKey() {
		return decodedKey;
	}

	/**
	 * Gets the subject id.
	 *
	 * @return the subject id
	 */
	public String getSubjectId() {
		return subjectId;
	}

	/**
	 * Gets the validated subject id url.
	 *
	 * @return the validated subject id url
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public String getValidatedSubjectIdUrl() throws OData2SparqlException {
		return validatedId(subjectId);
	}

	/**
	 * Gets the local key.
	 *
	 * @return the local key
	 */
	public String getLocalKey() {
		return localKey;
	}

	/**
	 * Gets the target subject id.
	 *
	 * @return the target subject id
	 */
	public String getTargetSubjectId() {
		return targetSubjectId;
	}

	/**
	 * Gets the validated target subject id url.
	 *
	 * @return the validated target subject id url
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	public String getValidatedTargetSubjectIdUrl() throws OData2SparqlException {
		return validatedId(targetSubjectId);
	}

	/**
	 * Validated id.
	 *
	 * @param id the id
	 * @return the string
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
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

	/**
	 * Gets the entity set.
	 *
	 * @return the entity set
	 */
	public RdfResourceEntitySet getEntitySet() {
		return entitySet;
	}

	/**
	 * Gets the entity string.
	 *
	 * @return the entity string
	 */
	public String getEntityString() {
		return entityString;
	}

	/**
	 * Gets the nav path string.
	 *
	 * @return the nav path string
	 */
	public String getNavPathString() {
		return navPathString;
	}

	/**
	 * Size.
	 *
	 * @return the int
	 */
	public int size() {
		return size;
	}

	/**
	 * Gets the custom query option.
	 *
	 * @return the map
	 */
	private Map<String, Object> _getCustomQueryOption() {
		HashMap<String, Object> customQueryOptions = new HashMap<String, Object>();
		customQueryOptions.put(RdfConstants.SERVICE,
				"<" + this.rdfEdmProvider.getRdfRepository().getDataRepository().getServiceUrl() + ">");
		if (this.uriInfo.getCustomQueryOptions().size() == 0) {
			return customQueryOptions;
		} else {
			for (CustomQueryOption customQueryOption : this.uriInfo.getCustomQueryOptions()) {
				customQueryOptions.put(customQueryOption.getName(), customQueryOption.getText());
			}
			return customQueryOptions;
		}
	}

	/**
	 * Gets the last resource part.
	 *
	 * @return the rdf resource part
	 */
	private RdfResourcePart _getLastResourcePart() {
		if (rdfResourceParts.size() == 0) {
			return null;
		} else {
			return rdfResourceParts.get(rdfResourceParts.size() - 1);
		}

	}

	/**
	 * Gets the last property name.
	 *
	 * @return the string
	 */
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

	/**
	 * Gets the last nav property.
	 *
	 * @return the rdf navigation property
	 */
	private RdfNavigationProperty _getLastNavProperty() {
		RdfResourcePart lastResourcePart = _getLastResourcePart();
		if (lastResourcePart != null) {
			if (lastResourcePart.uriResourceKind.equals(UriResourceKind.navigationProperty)) {
				EdmNavigationProperty edmNavigationProperty = ((RdfResourceNavigationProperty) lastResourcePart)
						.getEdmNavigationProperty();
				RdfNavigationProperty navProperty = this.getEntitySet().getRdfEntityType()
						.findNavigationPropertyByEDMNavigationPropertyName(edmNavigationProperty.getName());
				return navProperty;
			} else if (lastResourcePart.uriResourceKind.equals(UriResourceKind.complexProperty)) {
				return null;//TODO
			}
		}
		return null;

	}

	/**
	 * Gets the last nav property name.
	 *
	 * @return the string
	 */
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

	/**
	 * Gets the last complex type.
	 *
	 * @return the edm complex type
	 */
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

	/**
	 * Gets the last complex property.
	 *
	 * @return the rdf property
	 * @throws EdmException the edm exception
	 * @throws ODataApplicationException the o data application exception
	 */
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

	/**
	 * Gets the response rdf entity type.
	 *
	 * @return the rdf entity type
	 * @throws EdmException the edm exception
	 * @throws ODataException the o data exception
	 */
	private RdfEntityType _getResponseRdfEntityType() throws EdmException, ODataException {
		if (isFunction) {
			return responseRdfEntityType;
		} else {
			return rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(_getResponseEntitySet());
		}
	}

	/**
	 * Gets the penultimate resource part.
	 *
	 * @return the rdf resource part
	 */
	private RdfResourcePart _getPenultimateResourcePart() {
		if (_size() > 1) {
			return rdfResourceParts.get(rdfResourceParts.size() - 2);
		} else {
			return null;
		}
	}

	/**
	 * Gets the decoded key.
	 *
	 * @return the string
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private String _getDecodedKey() throws OData2SparqlException {
		if (getAsEntitySet(0) != null)

			return getAsEntitySet(0).getDecodedKey(uriInfo);
		return null;
	}

	/**
	 * Gets the local key.
	 *
	 * @param uriInfo the uri info
	 * @return the string
	 */
	private String _getLocalKey(UriInfo uriInfo) {
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0).getLocalKey(uriInfo);
		return null;
	}

	/**
	 * Gets the subject id.
	 *
	 * @param uriInfo the uri info
	 * @return the string
	 */
	private String _getSubjectId(UriInfo uriInfo) {
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0).getSubjectId(uriInfo);
		return null;
	}

	/**
	 * Gets the target subject id.
	 *
	 * @param uriInfo the uri info
	 * @return the string
	 */
	private String _getTargetSubjectId(UriInfo uriInfo) {

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
					return getAsEntitySet(j).getSubjectId(uriInfo);
				}
				break;
			default:
				break;
			}
		}
		return null;
	}

	/**
	 * Gets the entity string.
	 *
	 * @param uriInfo the uri info
	 * @return the string
	 */
	private String _getEntityString(UriInfo uriInfo) {
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0).getEntityString(uriInfo);
		return null;
	}

	/**
	 * Gets the entity set.
	 *
	 * @return the rdf resource entity set
	 */
	private RdfResourceEntitySet _getEntitySet() {
		if (getAsEntitySet(0) != null)
			return getAsEntitySet(0);
		return null;

	}

	/**
	 * Gets the nav path string.
	 *
	 * @return the string
	 */
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

	/**
	 * Gets the nav path.
	 *
	 * @return the nav path
	 */
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

	/**
	 * Size.
	 *
	 * @return the int
	 */
	private int _size() {
		return rdfResourceParts.size();
	}

	/**
	 * Gets the response entity set.
	 *
	 * @return the edm entity set
	 * @throws ODataException the o data exception
	 */
	private EdmEntitySet _getResponseEntitySet() throws ODataException {

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
			case function:
				RdfResourceEntitySet bindingEntitySet = null;
				if (j > 0) {
					//Find the bound entity set if set

					EdmFunction function = ((RdfResourceFunction) rdfResourceParts.get(j)).getFunction().getFunction();
					EdmType returnType = function.getReturnType().getType();
					RdfResourcePart penultimatePart = rdfResourceParts.get(j - 1);
					switch (penultimatePart.uriResourceKind){
						case function:
							RdfResourceFunction bindingFunction = (RdfResourceFunction) penultimatePart;
							return bindingFunction.getFunction().getFunctionImport().getReturnedEntitySet();

						case entitySet:
							bindingEntitySet = (RdfResourceEntitySet) penultimatePart;
							EdmEntitySet functionResponseEntitySet = Util.locateEntitySet(
									bindingEntitySet.edmEntitySet.getEntityContainer(), (EdmEntityType) returnType);
							return functionResponseEntitySet;						
	
						default:
						
					}
					

				} else {
					//If unbound then Edm contains return entityset (doen't otherwise for some reason)
					UriResourceFunction hfunction = ((RdfResourceFunction) rdfResourceParts.get(j)).getFunction();
					EdmEntitySet functionResponseEntitySet = hfunction.getFunctionImport().getReturnedEntitySet();
					return functionResponseEntitySet;
				}

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

	/**
	 * Gets the uri type.
	 *
	 * @return the uri type
	 * @throws ODataException the o data exception
	 */
	private UriType _getUriType() throws ODataException {
		if (isFunction()) {
//			CsdlReturnType functionReturnType;
			//If a function then it must be the last of the resourceParts.
			UriResourceFunction functionResource = (UriResourceFunction)uriInfo.asUriInfoResource().getUriResourceParts()
					.get(rdfResourceParts.size() - 1);
			EdmFunction function = functionResource.getFunction();
			EdmReturnType functionReturnType = function.getReturnType();
			responseRdfEntityType = rdfEdmProvider.getMappedEntityType(functionReturnType.getType().getFullQualifiedName());
			
//			CsdlFunctionImport functionImport = rdfEdmProvider.getEntityContainer()
//					.getFunctionImport(functionResource.getSegmentValue());
//			List<CsdlFunction> function = rdfEdmProvider.getFunctions(functionImport.getFunctionFQN());
//			functionReturnType = function.get(0).getReturnType();



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

	/**
	 * Gets the as entity set.
	 *
	 * @param index the index
	 * @return the as entity set
	 */
	private RdfResourceEntitySet getAsEntitySet(int index) {
		if (rdfResourceParts.size() == 0) {
			return null;
		} else {
			switch (rdfResourceParts.get(index).getUriResourceKind()) {
			case entitySet:
				return (RdfResourceEntitySet) (rdfResourceParts.get(index));
			case function:
			default:
				return null;

			}
		}
		//	return (RdfResourceEntitySet) (rdfResourceParts.get(index));

	}

	/**
	 * Gets the entity set.
	 *
	 * @param index the index
	 * @return the entity set
	 */
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
			return getEntitySet(index - 1);
		default:
			log.error(getResourceKind(index).toString() + " not handled for getEntitySet");
			return null;
		}
	}

	/**
	 * Gets the as navigation property.
	 *
	 * @param index the index
	 * @return the as navigation property
	 */
	public RdfResourceNavigationProperty getAsNavigationProperty(int index) {
		return (RdfResourceNavigationProperty) (rdfResourceParts.get(index));
	}

	/**
	 * Gets the as complex property.
	 *
	 * @param index the index
	 * @return the as complex property
	 */
	public RdfResourceComplexProperty getAsComplexProperty(int index) {
		return (RdfResourceComplexProperty) (rdfResourceParts.get(index));
	}

	/**
	 * Gets the as property.
	 *
	 * @param index the index
	 * @return the as property
	 */
	@SuppressWarnings("unused")
	private RdfResourceProperty getAsProperty(int index) {
		return (RdfResourceProperty) (rdfResourceParts.get(index));
	}

	/**
	 * Gets the resource kind.
	 *
	 * @param index the index
	 * @return the resource kind
	 */
	public UriResourceKind getResourceKind(int index) {
		return rdfResourceParts.get(index).getUriResourceKind();
	}

	/**
	 * Gets the prior entity set.
	 *
	 * @param index the index
	 * @return the prior entity set
	 * @throws ODataApplicationException the o data application exception
	 */
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

	/**
	 * Checks if is value request.
	 *
	 * @return the boolean
	 */
	public Boolean isValueRequest() {
		UriResource lastResourcePart = uriInfo.getUriResourceParts().get(uriInfo.getUriResourceParts().size() - 1);
		return lastResourcePart.getSegmentValue().equals("$value");
	}

	/**
	 * Checks if is ref request.
	 *
	 * @return the boolean
	 */
	public Boolean isRefRequest() {
		UriResource lastResourcePart = uriInfo.getUriResourceParts().get(uriInfo.getUriResourceParts().size() - 1);
		return lastResourcePart.getSegmentValue().equals("$ref");
	}

	/**
	 * Checks if is function.
	 *
	 * @return true, if is function
	 */
	public boolean isFunction() {
		return isFunction;
	}

	/**
	 * Gets the last nav property.
	 *
	 * @return the last nav property
	 */
	public RdfNavigationProperty getLastNavProperty() {
		return lastNavProperty;
	}

	/**
	 * Gets the custom query options.
	 *
	 * @return the custom query options
	 */
	public Map<String, Object> getCustomQueryOptions() {
		return customQueryOptions;
	}

	/**
	 * Gets the custom query options args.
	 *
	 * @return the custom query options args
	 */
	public String getCustomQueryOptionsArgs() {
		String customQueryOptionsArgs = ",'cacheHash','" + rdfEdmProvider.hashCode() + "'";
		if (getCustomQueryOptions() != null) {

			for (Entry<String, Object> customQueryOptionEntry : getCustomQueryOptions().entrySet()) {
				customQueryOptionsArgs += ",'" + customQueryOptionEntry.getKey() + "',"
						+ customQueryOptionEntry.getValue();
			}
			return customQueryOptionsArgs;
		} else {
			return customQueryOptionsArgs;
		}
	}

}
