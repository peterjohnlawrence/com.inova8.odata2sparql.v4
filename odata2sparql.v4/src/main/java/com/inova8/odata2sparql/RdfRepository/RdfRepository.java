/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.RdfRepository;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.rdf4j.model.Namespace;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Constants.TextSearchType;

public class RdfRepository {
	private RdfRepositories repositories;

	private RdfConnection dataEndPoint;
	private RdfConnection modelEndPoint;
	private RdfConnection operationEndPoint;
	private RdfRoleRepository dataRepository;
	private RdfRoleRepository modelRepository;
	private final Namespace defaultPrefix;
	private final TreeMap<String, Namespace> namespaces;
	private int defaultQueryLimit;
	private final String modelName;


	private Boolean withRdfAnnotations = false;
	private Boolean withSapAnnotations = false;
	private Boolean useBaseType = false;
	private Boolean expandOperations = false;
	private Boolean expandOrderbyDefault;
	private Integer expandSkipDefault;
	private Integer expandTopDefault;
	private TextSearchType textSearchType;
	private boolean withFKProperties = false;
	private boolean withMatching = true;
	private boolean includeImplicitRDF = false;
	private boolean bottomUpSPARQLOptimization = true;
	private String match = RdfConstants.DEFAULTMATCH;

	RdfRepository(RdfRepositories repositories, String modelName, Namespace defaultPrefix,
			TreeMap<String, Namespace> namespaces) {
		super();
		this.repositories = repositories;
		this.modelName = modelName;
		this.defaultPrefix = defaultPrefix;
		this.namespaces = namespaces;
	}

	public RdfRepositories getRepositories() {
		return repositories;
	}

	/**
	 * @param dataRepository
	 *            the dataRepository to set
	 */
	public void setDataRepository(RdfRoleRepository dataRepository) {
		this.dataRepository = dataRepository;
		this.dataEndPoint = new RdfConnection(dataRepository);
	}

	/**
	 * @param modelRepository
	 *            the modelRepository to set
	 */
	public void setModelRepository(RdfRoleRepository modelRepository) {
		this.modelRepository = modelRepository;

		this.modelEndPoint = new RdfConnection(modelRepository);
		this.operationEndPoint = this.modelEndPoint;
	}

	/**
	 * @return the dataRepository
	 */
	public RdfRoleRepository getDataRepository() {
		return dataRepository;
	}

	/**
	 * @return the modelRepository
	 */
	public RdfRoleRepository getModelRepository() {
		return modelRepository;
	}

	public String getModelName() {
		return modelName;
	}

	public String defaultNamespace() {

		return defaultPrefix.getName();
	}

	public TreeMap<String, Namespace> getNamespaces() {

		return namespaces;
	}

	public TreeMap<String, Namespace> addNamespaces(TreeMap<String, Namespace> additionalNamespaces) {
		for (Entry<String, Namespace> additionalNamespaceEntry : additionalNamespaces.entrySet()) {
			namespaces.put(additionalNamespaceEntry.getKey(), additionalNamespaceEntry.getValue());
		}
		return namespaces;
	}

	public RdfConnection getDataEndpoint() {
		return dataEndPoint;
	}

	public RdfConnection getModelEndPoint() {
		return modelEndPoint;
	}

	public RdfConnection getOperationEndPoint() {
		return operationEndPoint;
	}

	public String getDefaultPrefix() {
		return defaultPrefix.getPrefix();
	}

	public int getDefaultQueryLimit() {
		return defaultQueryLimit;
	}

	public void setDefaultQueryLimit(int defaultQueryLimit) {
		this.defaultQueryLimit = defaultQueryLimit;
	}

	public Boolean getWithRdfAnnotations() {
		return withRdfAnnotations;
	}

	public void setWithRdfAnnotations(Boolean withRdfAnnotations) {
		this.withRdfAnnotations = withRdfAnnotations;
	}

	public Boolean getWithSapAnnotations() {
		return withSapAnnotations;
	}

	public void setWithSapAnnotations(Boolean withSapAnnotations) {
		this.withSapAnnotations = withSapAnnotations;
	}

	public Boolean getUseBaseType() {
		return useBaseType;
	}

	public void setUseBaseType(boolean useBaseType) {
		this.useBaseType = useBaseType;
	}

	public Boolean getExpandOperations() {
		return expandOperations;
	}

	public void setExpandOperations(boolean expandOperations) {
		this.expandOperations = expandOperations;
	}

	public void setExpandTopDefault(Integer expandTopDefault) {
		this.expandTopDefault = expandTopDefault;
	}

	public Integer getExpandTopDefault() {
		return this.expandTopDefault;
	}

	public void setExpandSkipDefault(Integer expandSkipDefault) {
		this.expandSkipDefault = expandSkipDefault;
	}

	public Integer getExpandSkipDefault() {
		return this.expandSkipDefault;
	}

	public void setExpandOrderbyDefault(boolean expandOrderbyDefault) {
		this.expandOrderbyDefault = expandOrderbyDefault;
	}

	public Boolean  getExpandOrderbyDefault() {
		return this.expandOrderbyDefault;
	}

	public TextSearchType getTextSearchType() {
		return textSearchType;
	}

	public void setTextSearchType(TextSearchType textSearchType) {
		this.textSearchType = textSearchType;
	}

	public void setWithFKProperties(boolean withFKProperties) {
		this.withFKProperties = withFKProperties;
	}

	public boolean getWithFKProperties() {
		return withFKProperties;
	}

	public boolean isWithMatching() {
		return withMatching;
	}

	public void setWithMatching(boolean withMatching) {
		this.withMatching = withMatching;
	}

	public String getMatch() {
		return match;
	}

	public void setMatch(String match) {
		this.match = match;
	}
	public boolean isIncludeImplicitRDF() {
		return includeImplicitRDF;
	}

	public void setIncludeImplicitRDF(boolean includeImplicitRDF) {
		this.includeImplicitRDF = includeImplicitRDF;
	}

	public boolean isBottomUpSPARQLOptimization() {
		return bottomUpSPARQLOptimization;
	}

	public void setBottomUpSPARQLOptimization(boolean bottomUpSPARQLOptimization) {
		this.bottomUpSPARQLOptimization = bottomUpSPARQLOptimization;
	}
}
