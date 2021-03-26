/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfRepository;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.rdf4j.model.Namespace;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.TextSearchType;

/**
 * The Class RdfRepository.
 */
public class RdfRepository {
	
	/** The repositories. */
	private RdfRepositories repositories;

	/** The data end point. */
	private RdfConnection dataEndPoint;
	
	/** The model end point. */
	private RdfConnection modelEndPoint;
	
	/** The operation end point. */
	private RdfConnection operationEndPoint;
	
	/** The data repository. */
	private RdfRoleRepository dataRepository;
	
	/** The model repository. */
	private RdfRoleRepository modelRepository;
	
	/** The default prefix. */
	private final Namespace defaultPrefix;
	
	/** The namespaces. */
	private final TreeMap<String, Namespace> namespaces;
	
	/** The default query limit. */
	private int defaultQueryLimit;
	
	/** The model name. */
	private final String modelName;


	/** The with rdf annotations. */
	private Boolean withRdfAnnotations = false;
	
	/** The with sap annotations. */
	private Boolean withSapAnnotations = false;
	
	/** The use base type. */
	private Boolean useBaseType = false;
	
	/** The expand operations. */
	private Boolean expandOperations = false;
	
	/** The expand orderby default. */
	private Boolean expandOrderbyDefault;
	
	/** The expand skip default. */
	private Integer expandSkipDefault;
	
	/** The expand top default. */
	private Integer expandTopDefault;
	
	/** The text search type. */
	private TextSearchType textSearchType;
	
	/** The with FK properties. */
	private boolean withFKProperties = false;
	
	/** The with matching. */
	private boolean withMatching = true;
	
	/** The include implicit RDF. */
	private boolean includeImplicitRDF = false;
	
	/** The bottom up SPARQL optimization. */
	private boolean bottomUpSPARQLOptimization = true;
	
	/** The support scripting. */
	private boolean supportScripting=false;
	
	/** The match. */
	private String match = RdfConstants.DEFAULTMATCH;


	/**
	 * Instantiates a new rdf repository.
	 *
	 * @param repositories the repositories
	 * @param modelName the model name
	 * @param defaultPrefix the default prefix
	 * @param namespaces the namespaces
	 */
	RdfRepository(RdfRepositories repositories, String modelName, Namespace defaultPrefix,
			TreeMap<String, Namespace> namespaces) {
		super();
		this.repositories = repositories;
		this.modelName = modelName;
		this.defaultPrefix = defaultPrefix;
		this.namespaces = namespaces;
	}

	/**
	 * Gets the repositories.
	 *
	 * @return the repositories
	 */
	public RdfRepositories getRepositories() {
		return repositories;
	}

	/**
	 * Sets the data repository.
	 *
	 * @param dataRepository            the dataRepository to set
	 */
	public void setDataRepository(RdfRoleRepository dataRepository) {
		this.dataRepository = dataRepository;
		this.dataEndPoint = new RdfConnection(dataRepository);
	}

	/**
	 * Sets the model repository.
	 *
	 * @param modelRepository            the modelRepository to set
	 */
	public void setModelRepository(RdfRoleRepository modelRepository) {
		this.modelRepository = modelRepository;

		this.modelEndPoint = new RdfConnection(modelRepository);
		this.operationEndPoint = this.modelEndPoint;
	}

	/**
	 * Gets the data repository.
	 *
	 * @return the dataRepository
	 */
	public RdfRoleRepository getDataRepository() {
		return dataRepository;
	}

	/**
	 * Gets the model repository.
	 *
	 * @return the modelRepository
	 */
	public RdfRoleRepository getModelRepository() {
		return modelRepository;
	}

	/**
	 * Gets the model name.
	 *
	 * @return the model name
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Default namespace.
	 *
	 * @return the string
	 */
	public String defaultNamespace() {

		return defaultPrefix.getName();
	}

	/**
	 * Gets the namespaces.
	 *
	 * @return the namespaces
	 */
	public TreeMap<String, Namespace> getNamespaces() {

		return namespaces;
	}

	/**
	 * Adds the namespaces.
	 *
	 * @param additionalNamespaces the additional namespaces
	 * @return the tree map
	 */
	public TreeMap<String, Namespace> addNamespaces(TreeMap<String, Namespace> additionalNamespaces) {
		for (Entry<String, Namespace> additionalNamespaceEntry : additionalNamespaces.entrySet()) {
			namespaces.put(additionalNamespaceEntry.getKey(), additionalNamespaceEntry.getValue());
		}
		return namespaces;
	}

	/**
	 * Gets the data endpoint.
	 *
	 * @return the data endpoint
	 */
	public RdfConnection getDataEndpoint() {
		return dataEndPoint;
	}

	/**
	 * Gets the model end point.
	 *
	 * @return the model end point
	 */
	public RdfConnection getModelEndPoint() {
		return modelEndPoint;
	}

	/**
	 * Gets the operation end point.
	 *
	 * @return the operation end point
	 */
	public RdfConnection getOperationEndPoint() {
		return operationEndPoint;
	}

	/**
	 * Gets the default prefix.
	 *
	 * @return the default prefix
	 */
	public String getDefaultPrefix() {
		return defaultPrefix.getPrefix();
	}

	/**
	 * Gets the default query limit.
	 *
	 * @return the default query limit
	 */
	public int getDefaultQueryLimit() {
		return defaultQueryLimit;
	}

	/**
	 * Sets the default query limit.
	 *
	 * @param defaultQueryLimit the new default query limit
	 */
	public void setDefaultQueryLimit(int defaultQueryLimit) {
		this.defaultQueryLimit = defaultQueryLimit;
	}

	/**
	 * Gets the with rdf annotations.
	 *
	 * @return the with rdf annotations
	 */
	public Boolean getWithRdfAnnotations() {
		return withRdfAnnotations;
	}

	/**
	 * Sets the with rdf annotations.
	 *
	 * @param withRdfAnnotations the new with rdf annotations
	 */
	public void setWithRdfAnnotations(Boolean withRdfAnnotations) {
		this.withRdfAnnotations = withRdfAnnotations;
	}

	/**
	 * Gets the with sap annotations.
	 *
	 * @return the with sap annotations
	 */
	public Boolean getWithSapAnnotations() {
		return withSapAnnotations;
	}

	/**
	 * Sets the with sap annotations.
	 *
	 * @param withSapAnnotations the new with sap annotations
	 */
	public void setWithSapAnnotations(Boolean withSapAnnotations) {
		this.withSapAnnotations = withSapAnnotations;
	}

	/**
	 * Gets the use base type.
	 *
	 * @return the use base type
	 */
	public Boolean getUseBaseType() {
		return useBaseType;
	}

	/**
	 * Sets the use base type.
	 *
	 * @param useBaseType the new use base type
	 */
	public void setUseBaseType(boolean useBaseType) {
		this.useBaseType = useBaseType;
	}

	/**
	 * Gets the expand operations.
	 *
	 * @return the expand operations
	 */
	public Boolean getExpandOperations() {
		return expandOperations;
	}

	/**
	 * Sets the expand operations.
	 *
	 * @param expandOperations the new expand operations
	 */
	public void setExpandOperations(boolean expandOperations) {
		this.expandOperations = expandOperations;
	}

	/**
	 * Sets the expand top default.
	 *
	 * @param expandTopDefault the new expand top default
	 */
	public void setExpandTopDefault(Integer expandTopDefault) {
		this.expandTopDefault = expandTopDefault;
	}

	/**
	 * Gets the expand top default.
	 *
	 * @return the expand top default
	 */
	public Integer getExpandTopDefault() {
		return this.expandTopDefault;
	}

	/**
	 * Sets the expand skip default.
	 *
	 * @param expandSkipDefault the new expand skip default
	 */
	public void setExpandSkipDefault(Integer expandSkipDefault) {
		this.expandSkipDefault = expandSkipDefault;
	}

	/**
	 * Gets the expand skip default.
	 *
	 * @return the expand skip default
	 */
	public Integer getExpandSkipDefault() {
		return this.expandSkipDefault;
	}

	/**
	 * Sets the expand orderby default.
	 *
	 * @param expandOrderbyDefault the new expand orderby default
	 */
	public void setExpandOrderbyDefault(boolean expandOrderbyDefault) {
		this.expandOrderbyDefault = expandOrderbyDefault;
	}

	/**
	 * Gets the expand orderby default.
	 *
	 * @return the expand orderby default
	 */
	public Boolean  getExpandOrderbyDefault() {
		return this.expandOrderbyDefault;
	}

	/**
	 * Gets the text search type.
	 *
	 * @return the text search type
	 */
	public TextSearchType getTextSearchType() {
		return textSearchType;
	}

	/**
	 * Sets the text search type.
	 *
	 * @param textSearchType the new text search type
	 */
	public void setTextSearchType(TextSearchType textSearchType) {
		this.textSearchType = textSearchType;
	}

	/**
	 * Sets the with FK properties.
	 *
	 * @param withFKProperties the new with FK properties
	 */
	public void setWithFKProperties(boolean withFKProperties) {
		this.withFKProperties = withFKProperties;
	}

	/**
	 * Gets the with FK properties.
	 *
	 * @return the with FK properties
	 */
	public boolean getWithFKProperties() {
		return withFKProperties;
	}

	/**
	 * Checks if is with matching.
	 *
	 * @return true, if is with matching
	 */
	public boolean isWithMatching() {
		return withMatching;
	}

	/**
	 * Sets the with matching.
	 *
	 * @param withMatching the new with matching
	 */
	public void setWithMatching(boolean withMatching) {
		this.withMatching = withMatching;
	}

	/**
	 * Gets the match.
	 *
	 * @return the match
	 */
	public String getMatch() {
		return match;
	}

	/**
	 * Sets the match.
	 *
	 * @param match the new match
	 */
	public void setMatch(String match) {
		this.match = match;
	}
	
	/**
	 * Checks if is include implicit RDF.
	 *
	 * @return true, if is include implicit RDF
	 */
	public boolean isIncludeImplicitRDF() {
		return includeImplicitRDF;
	}

	/**
	 * Sets the include implicit RDF.
	 *
	 * @param includeImplicitRDF the new include implicit RDF
	 */
	public void setIncludeImplicitRDF(boolean includeImplicitRDF) {
		this.includeImplicitRDF = includeImplicitRDF;
	}
	
	/**
	 * Checks if is support scripting.
	 *
	 * @return true, if is support scripting
	 */
	public boolean isSupportScripting() {
		return supportScripting;
	}
	
	/**
	 * Sets the support scripting.
	 *
	 * @param supportScripting the new support scripting
	 */
	public void setSupportScripting(boolean supportScripting) {
		this.supportScripting = supportScripting;
	}
	
	/**
	 * Checks if is bottom up SPARQL optimization.
	 *
	 * @return true, if is bottom up SPARQL optimization
	 */
	public boolean isBottomUpSPARQLOptimization() {
		return bottomUpSPARQLOptimization;
	}

	/**
	 * Sets the bottom up SPARQL optimization.
	 *
	 * @param bottomUpSPARQLOptimization the new bottom up SPARQL optimization
	 */
	public void setBottomUpSPARQLOptimization(boolean bottomUpSPARQLOptimization) {
		this.bottomUpSPARQLOptimization = bottomUpSPARQLOptimization;
	}
}
