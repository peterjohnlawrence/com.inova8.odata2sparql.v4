/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfRepository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.SailReadOnlyException;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.spin.config.SpinSailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.*;
import com.inova8.odata2sparql.Exception.OData2SparqlException;

/**
 * The Class RdfRepositories.
 */
public class RdfRepositories {

	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfRepositories.class);
	
	/** The repository manager. */
	private RepositoryManager repositoryManager = null;
	
	/** The repository folder. */
	private final String repositoryFolder;
	
	/** The repository url. */
	private final String repositoryUrl;
	
	/** The repository dir. */
	private final String repositoryDir;
	
	/** The rdf repository list. */
	private Map<String, RdfRepository> rdfRepositoryList = new TreeMap<String, RdfRepository>();
	
	/** The local repository manager model. */
	String localRepositoryManagerModel = RdfConstants.repositoryWorkingDirectory;
	
	/** The local repository manager directory. */
	String localRepositoryManagerDirectory = RdfConstants.repositoryWorkingDirectory;

	/**
	 * Instantiates a new rdf repositories.
	 *
	 * @param configFolder the config folder
	 * @param repositoryFolder the repository folder
	 * @param repositoryUrl the repository url
	 * @param repositoryDir the repository dir
	 */
	public RdfRepositories(String configFolder, String repositoryFolder, String repositoryUrl, String repositoryDir) {
		super();
		if (configFolder == null || configFolder.isEmpty()) {
		} else {
		}
		if (repositoryFolder == null || repositoryFolder.isEmpty()) {
			this.repositoryFolder = RdfConstants.DEFAULTFOLDER;
		} else {
			this.repositoryFolder = repositoryFolder;
		}
		this.repositoryUrl = repositoryUrl;
		this.repositoryDir = repositoryDir;
		try {
			loadRepositories();
		} catch (OData2SparqlException e) {
			log.error("Cannot load repositories", e);
		} catch (RepositoryConfigException e) {
			log.error("Cannot load repositories", e);
		}
	}

	/**
	 * Gets the local repository manager model.
	 *
	 * @return the local repository manager model
	 */
	public String getLocalRepositoryManagerModel() {
		return localRepositoryManagerModel;
	}

	/**
	 * Gets the local repository manager directory.
	 *
	 * @return the local repository manager directory
	 */
	public String getLocalRepositoryManagerDirectory() {
		return localRepositoryManagerDirectory;
	}

	/**
	 * Reload.
	 */
	public void reload() {
		repositoryManager.shutDown();
		try {
			loadRepositories();
		} catch (OData2SparqlException e) {
			log.error("Cannot load repositories", e);
		} catch (RepositoryConfigException e) {
			log.error("Cannot load repositories", e);
		}
	}

	/**
	 * Gets the rdf repository.
	 *
	 * @param rdfRepositoryID the rdf repository ID
	 * @return the rdf repository
	 */
	public RdfRepository getRdfRepository(String rdfRepositoryID) {
		return rdfRepositoryList.get(rdfRepositoryID);
	}

	/**
	 * Gets the rdf repositories.
	 *
	 * @return the rdf repositories
	 */
	public Map<String, RdfRepository> getRdfRepositories() {
		return rdfRepositoryList;
	}

	/**
	 * Verify RDF 4 J update endpoint url.
	 *
	 * @param queryEndpointUrl the query endpoint url
	 * @param updateEndpointUrl the update endpoint url
	 * @param profile the profile
	 * @return the string
	 */
	private String verifyRDF4JUpdateEndpointUrl(String queryEndpointUrl, String updateEndpointUrl, String profile) {
		if (profile == "" || profile.equals(SPARQLProfile.RDF4J.getCode())) {
			URI updateEndpointUri;
			if (updateEndpointUrl == "")
				return queryEndpointUrl;
			else
				try {
					updateEndpointUri = new URI(updateEndpointUrl);
					String[] updateEndpointUriParts = updateEndpointUri.getPath().split("/");
					String updateEndpointUriLastPart = updateEndpointUriParts[updateEndpointUriParts.length - 1];
					if (!updateEndpointUriLastPart.equals("statements")) {
						try {
							URI queryEndpointUri = new URI(queryEndpointUrl);
							updateEndpointUrl = queryEndpointUri.getScheme() + "://" + queryEndpointUri.getAuthority()
									+ queryEndpointUri.getPath() + "/statements";
							log.error(
									"updateEndpointUrl for RDF4J must end with /statements. Modified queryEndpointUrl instead to create: "
											+ updateEndpointUrl);
							return updateEndpointUrl;
						} catch (URISyntaxException e) {
							log.error("queryEndpointUrl invalid, cannot be used to construct updateEndpointUrl:",
									queryEndpointUrl);
						}

					} else {
						return updateEndpointUrl;
					}
				} catch (URISyntaxException e) {
					log.error("updateEndpointUrl invalid:", updateEndpointUrl);
				}

		} else {
			if (updateEndpointUrl == "")
				return queryEndpointUrl;
			else
				return updateEndpointUrl;
		}
		return updateEndpointUrl;

	}

	/**
	 * Load repositories.
	 *
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws RepositoryConfigException the repository config exception
	 */
	private void loadRepositories() throws OData2SparqlException, RepositoryConfigException {
		try {
			if (this.repositoryUrl != null && !this.repositoryUrl.isEmpty()) {
				try {
					//repositoryManager = bootstrapRemoteRepository(properties.getProperty(RdfConstants.repositoryUrl));
					repositoryManager = bootstrapRemoteRepository(this.repositoryUrl);
				} catch (OData2SparqlException e) {
					try {
						repositoryManager = bootstrapLocalRepository();
					} catch (OData2SparqlException | IOException e1) {
						log.error("Tried everything. Cannot locate a suitable repository", e1);
					}
				}
			} else {
				try {
					repositoryManager = bootstrapLocalRepository();
				} catch (OData2SparqlException | IOException e1) {
					log.error("Tried everything. Cannot locate a suitable repository", e1);
				}
			}

			Repository systemRepository = repositoryManager.getRepository(RdfConstants.systemId);
			RepositoryConnection modelsConnection = systemRepository.getConnection();

			//Bootstrap the standard queries

			readQueries(modelsConnection, RdfConstants.RDFSModel);

			try {
				//Identify endpoints and create corresponding repositories:
				String queryString = RdfConstants.getMetaQueries().get(RdfConstants.URI_REPOSITORYQUERY);

				TupleQuery tupleQuery = modelsConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				TupleQueryResult queryResult = tupleQuery.evaluate();
				ArrayList<BindingSet> results = new ArrayList<BindingSet>();
				{
					try {
						while (queryResult.hasNext()) {
							// iterate over the results and load to arraylist to avoid apparent nested query blocking
							results.add(queryResult.next());
						}
					} finally {
						queryResult.close();
					}
					Iterator<BindingSet> result = results.iterator();
					while (result.hasNext()) {
						// iterate over each dataset
						try {
							BindingSet bindingSet = result.next();
							Value valueOfDataset = bindingSet.getValue("Dataset");
							@SuppressWarnings("unused")
							Literal valueOfDatasetName = (Literal) bindingSet.getValue("DatasetName");
							log.info("Dataset loaded: " + valueOfDataset.toString());

							Value valueOfDefaultNamespace = bindingSet.getValue("defaultNamespace");
							Literal valueOfDefaultPrefix = (Literal) bindingSet.getValue("defaultPrefix");

							Literal valueOfDataRepositoryID = (Literal) bindingSet.getValue("DataRepositoryID");
							Value valueOfDataRepositoryImplType = bindingSet.getValue("DataRepositoryImplType");
							Value valueOfDataRepositoryImplURL = bindingSet.getValue("DataRepositoryImplURL");
							Value valueOfDataRepositoryImplQueryEndpoint = bindingSet
									.getValue("DataRepositoryImplQueryEndpoint");
							Value valueOfDataRepositoryImplUpdateEndpoint = bindingSet
									.getValue("DataRepositoryImplUpdateEndpoint");
							Value valueOfDataRepositoryImplProfile = bindingSet.getValue("DataRepositoryImplProfile");
							Literal valueOfDataRepositoryImplQueryLimit = (Literal) bindingSet
									.getValue("DataRepositoryImplQueryLimit");

							Value valueOfDataRepositoryInsertGraph = bindingSet.getValue("DataRepositoryInsertGraph");
							Value valueOfDataRepositoryRemoveGraph = bindingSet.getValue("DataRepositoryRemoveGraph");
							Value valueOfDataRepositoryChangeGraph = bindingSet.getValue("DataRepositoryChangeGraph");

							Literal valueOfVocabularyRepositoryID = (Literal) bindingSet
									.getValue("VocabularyRepositoryID");
							Value valueOfVocabularyRepositoryImplType = bindingSet
									.getValue("VocabularyRepositoryImplType");
							Value valueOfVocabularyRepositoryImplURL = bindingSet
									.getValue("VocabularyRepositoryImplURL");
							Value valueOfVocabularyRepositoryImplQueryEndpoint = bindingSet
									.getValue("VocabularyRepositoryImplQueryEndpoint");
							Value valueOfVocabularyRepositoryImplUpdateEndpoint = bindingSet
									.getValue("VocabularyRepositoryImplUpdateEndpoint");
							Value valueOfVocabularyRepositoryImplProfile = bindingSet
									.getValue("VocabularyRepositoryImplProfile");
							Literal valueOfVocabularyRepositoryImplQueryLimit = (Literal) bindingSet
									.getValue("VocabularyRepositoryImplQueryLimit");
							Literal valueOfWithRdfAnnotations = (Literal) bindingSet.getValue("withRdfAnnotations");
							Literal valueOfWithSapAnnotations = (Literal) bindingSet.getValue("withSapAnnotations");
							Literal valueOfUseBaseType = (Literal) bindingSet.getValue("useBaseType");
							Literal valueOfWithFKProperties = (Literal) bindingSet.getValue("withFKProperties");
							Literal valueOfWithMatching = (Literal) bindingSet.getValue("withMatching");
							Literal valueOfMatch = (Literal) bindingSet.getValue("match");
							Literal valueOfExpandOperations = (Literal) bindingSet.getValue("expandOperations");
							Literal valueOfExpandTopDefault = (Literal) bindingSet.getValue("expandTopDefault");
							Literal valueOfExpandSkipDefault = (Literal) bindingSet.getValue("expandSkipDefault");
							Literal valueOfExpandOrderbyDefault = (Literal) bindingSet.getValue("expandOrderbyDefault");
							Literal valueOfIncludeImplicitRDF = (Literal) bindingSet.getValue("includeImplicitRDF");
							Literal valueOfSupportScripting = (Literal) bindingSet.getValue("supportScripting");
							Literal valueOfBottomUpSPARQLOptimization = (Literal) bindingSet.getValue("bottomUpSPARQLOptimization");
							Value valueOfTextSearchType = bindingSet.getValue("textSearchType");
							//Create and add the corresponding repositories
							RepositoryConfig dataRepositoryConfig;
							try {
								dataRepositoryConfig = repositoryManager
										.getRepositoryConfig(valueOfDataRepositoryID.getLabel());
								if (dataRepositoryConfig == null)
									throw new RepositoryException();
							} catch (RepositoryException e) {
								switch (valueOfDataRepositoryImplType.toString()) {
								case "http://www.openrdf.org#SPARQLRepository":
									SPARQLRepositoryConfig sparqlDataRepositoryTypeSpec = new SPARQLRepositoryConfig();
									sparqlDataRepositoryTypeSpec
											.setQueryEndpointUrl(valueOfDataRepositoryImplQueryEndpoint.stringValue());

									sparqlDataRepositoryTypeSpec.setUpdateEndpointUrl(verifyRDF4JUpdateEndpointUrl(
											valueOfDataRepositoryImplQueryEndpoint.stringValue(),
											valueOfDataRepositoryImplUpdateEndpoint.stringValue(),
											valueOfDataRepositoryImplProfile.toString()));
									dataRepositoryConfig = new RepositoryConfig(valueOfDataRepositoryID.stringValue(),
											sparqlDataRepositoryTypeSpec);

									repositoryManager.addRepositoryConfig(dataRepositoryConfig);
									break;
								case "http://www.openrdf.org#SystemRepository":
									break;
								case "http://www.openrdf.org#HTTPRepository":
									HTTPRepositoryConfig httpDataRepositoryTypeSpec = new HTTPRepositoryConfig();
									httpDataRepositoryTypeSpec.setURL(valueOfDataRepositoryImplURL.stringValue());
									dataRepositoryConfig = new RepositoryConfig(valueOfDataRepositoryID.stringValue(),
											httpDataRepositoryTypeSpec);
									repositoryManager.addRepositoryConfig(dataRepositoryConfig);
									break;
								case "http://www.openrdf.org#ProxyRepository":
									break;
								case "http://www.openrdf.org#SailRepository":
									if (((IRI) valueOfDataset).getLocalName().equals("ODATA2SPARQL")) {
										//Do nothing as the SYSTEM has already been configured.
										//repositoryManager.addRepositoryConfig(repositoryManager.getRepositoryConfig("ODATA2SPARQL"));

									} else {
									}
									break;
								default:
									log.error("Unrecognized repository implementatiomn type: ");
									break;
								}
							}
							RepositoryConfig vocabularyRepositoryConfig;
							try {
								vocabularyRepositoryConfig = repositoryManager
										.getRepositoryConfig(valueOfVocabularyRepositoryID.getLabel());
								if (vocabularyRepositoryConfig == null)
									throw new RepositoryException();
							} catch (RepositoryException e) {
								//repositoryManager.removeRepository(valueOfVocabularyRepositoryID.stringValue());
								switch (valueOfVocabularyRepositoryImplType.toString()) {
								case "http://www.openrdf.org#SPARQLRepository":
									SPARQLRepositoryConfig sparqlVocabularyRepositoryTypeSpec = new SPARQLRepositoryConfig();
									sparqlVocabularyRepositoryTypeSpec.setQueryEndpointUrl(
											valueOfVocabularyRepositoryImplQueryEndpoint.stringValue());
									sparqlVocabularyRepositoryTypeSpec
											.setUpdateEndpointUrl(verifyRDF4JUpdateEndpointUrl(
													valueOfVocabularyRepositoryImplQueryEndpoint.stringValue(),
													valueOfVocabularyRepositoryImplUpdateEndpoint.stringValue(),
													valueOfVocabularyRepositoryImplProfile.toString()));
									vocabularyRepositoryConfig = new RepositoryConfig(
											valueOfVocabularyRepositoryID.stringValue(),
											sparqlVocabularyRepositoryTypeSpec);

									repositoryManager.addRepositoryConfig(vocabularyRepositoryConfig);
									break;
								case "http://www.openrdf.org#SystemRepository":

									break;
								case "http://www.openrdf.org#HTTPRepository":
									HTTPRepositoryConfig httpVocabularyRepositoryTypeSpec = new HTTPRepositoryConfig();
									httpVocabularyRepositoryTypeSpec
											.setURL(valueOfVocabularyRepositoryImplURL.stringValue());
									vocabularyRepositoryConfig = new RepositoryConfig(
											valueOfVocabularyRepositoryID.stringValue(),
											httpVocabularyRepositoryTypeSpec);
									repositoryManager.addRepositoryConfig(vocabularyRepositoryConfig);
									break;
								case "http://www.openrdf.org#ProxyRepository":
									break;
								case "http://www.openrdf.org#SailRepository":
									if (((IRI) valueOfDataset).getLocalName().equals("ODATA2SPARQL")) {
										//Do nothing as the SYSTEM has already been configured.
										//repositoryManager.addRepositoryConfig(repositoryManager.getRepositoryConfig("ODATA2SPARQL"));
									} else {
									}
									break;
								default:
									log.error("Unrecognized repository implementatiomn type: ");
									break;
								}
							}
							TreeMap<String, Namespace> namespaces = readPrefixes(modelsConnection, valueOfDataset);
							Namespace defaultPrefix = null;
							try {
								if ((valueOfDefaultPrefix == null) || (valueOfDefaultNamespace == null)) {
									log.error("Null default prefix or namespace for " + valueOfDataset
											+ ". Check repository or models.ttl for statements { odata4sparql:<prefix>  odata4sparql:defaultPrefix odata4sparql:<prefix> ;   odata4sparql:namespace <namespace> ; .. }");
									throw new RepositoryConfigException(
											"Null default prefix or namespace for" + valueOfDataset);
								} else {
									defaultPrefix = namespaces.get(valueOfDefaultPrefix.stringValue());
									if (defaultPrefix == null) {
										defaultPrefix = new SimpleNamespace(valueOfDefaultPrefix.stringValue(),
												valueOfDefaultNamespace.stringValue());
										namespaces.put(valueOfDefaultPrefix.stringValue(), defaultPrefix);
									}
								}
							} catch (NullPointerException e) {
								log.warn("Null default prefix", e);
							}
							RdfRepository repository = new RdfRepository(this, ((IRI) valueOfDataset).getLocalName(),
									defaultPrefix, namespaces);

							switch (valueOfDataRepositoryImplType.toString()) {
							case "http://www.openrdf.org#SPARQLRepository":
								repository.setDataRepository(new RdfRoleRepository(
										repositoryManager.getRepository(valueOfDataRepositoryID.stringValue()),
										Integer.parseInt(valueOfDataRepositoryImplQueryLimit.stringValue()),
										SPARQLProfile.get(valueOfDataRepositoryImplProfile.stringValue()),
										valueOfDataRepositoryImplQueryEndpoint.stringValue(),
										valueOfDataRepositoryImplUpdateEndpoint.stringValue()));
								break;
							case "http://www.openrdf.org#SystemRepository":
								break;
							case "http://www.openrdf.org#HTTPRepository":
								repository.setDataRepository(new RdfRoleRepository(
										repositoryManager.getRepository(valueOfDataRepositoryID.stringValue()),
										Integer.parseInt(valueOfDataRepositoryImplQueryLimit.stringValue()),
										SPARQLProfile.get(valueOfDataRepositoryImplProfile.stringValue()),
										valueOfDataRepositoryImplURL.stringValue()));
								break;
							case "http://www.openrdf.org#ProxyRepository":
								break;
							case "http://www.openrdf.org#SailRepository":
								repository.setDataRepository(
										new RdfRoleRepository(repositoryManager.getRepository("ODATA2SPARQL"),
												Integer.parseInt(valueOfDataRepositoryImplQueryLimit.stringValue()),
												SPARQLProfile.get(valueOfDataRepositoryImplProfile.stringValue())));
								break;
							default:
								log.error("Unrecognized repository implementatiomn type: ");
								break;
							}
							if (valueOfDataRepositoryInsertGraph != null)
								repository.getDataRepository()
										.setInsertGraphUrl(valueOfDataRepositoryInsertGraph.stringValue());
							if (valueOfDataRepositoryRemoveGraph != null)
								repository.getDataRepository()
										.setRemoveGraphUrl(valueOfDataRepositoryRemoveGraph.stringValue());
							if (valueOfDataRepositoryChangeGraph != null)
								repository.getDataRepository()
										.setChangeGraphUrl(valueOfDataRepositoryChangeGraph.stringValue());

							switch (valueOfVocabularyRepositoryImplType.toString()) {
							case "http://www.openrdf.org#SPARQLRepository":
								repository.setModelRepository(new RdfRoleRepository(
										repositoryManager.getRepository(valueOfVocabularyRepositoryID.stringValue()),
										Integer.parseInt(valueOfVocabularyRepositoryImplQueryLimit.stringValue()),
										SPARQLProfile.get(valueOfVocabularyRepositoryImplProfile.stringValue()),
										valueOfVocabularyRepositoryImplQueryEndpoint.stringValue(),
										valueOfVocabularyRepositoryImplUpdateEndpoint.stringValue()));
								break;
							case "http://www.openrdf.org#SystemRepository":
								break;
							case "http://www.openrdf.org#HTTPRepository":
								repository.setModelRepository(new RdfRoleRepository(
										repositoryManager.getRepository(valueOfVocabularyRepositoryID.stringValue()),
										Integer.parseInt(valueOfVocabularyRepositoryImplQueryLimit.stringValue()),
										SPARQLProfile.get(valueOfVocabularyRepositoryImplProfile.stringValue()),
										valueOfVocabularyRepositoryImplURL.stringValue()));
								break;
							case "http://www.openrdf.org#ProxyRepository":
								break;
							case "http://www.openrdf.org#SailRepository":
								repository.setModelRepository(new RdfRoleRepository(
										repositoryManager.getRepository("ODATA2SPARQL"),
										Integer.parseInt(valueOfVocabularyRepositoryImplQueryLimit.stringValue()),
										SPARQLProfile.get(valueOfVocabularyRepositoryImplProfile.stringValue())));
								break;
							default:
								log.error("Unrecognized repository implementatiomn type: ");
								break;
							}
							if (valueOfWithRdfAnnotations != null) {
								repository.setWithRdfAnnotations(
										Boolean.parseBoolean(valueOfWithRdfAnnotations.stringValue()));
							} else {
								repository.setWithRdfAnnotations(false);
							}
							if (valueOfWithSapAnnotations != null) {
								repository.setWithSapAnnotations(
										Boolean.parseBoolean(valueOfWithSapAnnotations.stringValue()));
							} else {
								repository.setWithSapAnnotations(false);
							}
							if (valueOfUseBaseType != null) {
								repository.setUseBaseType(Boolean.parseBoolean(valueOfUseBaseType.stringValue()));
							} else {
								repository.setUseBaseType(true);
							}
							if (valueOfWithFKProperties != null) {
								repository.setWithFKProperties(
										Boolean.parseBoolean(valueOfWithFKProperties.stringValue()));
							} else {
								repository.setWithFKProperties(false);
							}
							if (valueOfWithMatching != null) {
								repository.setWithMatching(Boolean.parseBoolean(valueOfWithMatching.stringValue()));
							} else {
								repository.setWithMatching(false);
							}
							if (valueOfMatch != null) {
								repository.setMatch(valueOfMatch.stringValue());
							} else {
								repository.setMatch(RdfConstants.DEFAULTMATCH);
							}
							if (valueOfExpandOperations != null) {
								repository.setExpandOperations(
										Boolean.parseBoolean(valueOfExpandOperations.stringValue()));
							} else {
								repository.setExpandOperations(true);
							}
							if (valueOfExpandTopDefault != null) {
								repository.setExpandTopDefault(Integer.parseInt(valueOfExpandTopDefault.stringValue()));
							} else {
								repository.setExpandTopDefault(null);
							}
							if (valueOfExpandSkipDefault != null) {
								repository
										.setExpandSkipDefault(Integer.parseInt(valueOfExpandSkipDefault.stringValue()));
							} else {
								repository.setExpandTopDefault(null);
							}
							if (valueOfExpandOrderbyDefault != null) {
								repository.setExpandOrderbyDefault(
										Boolean.parseBoolean(valueOfExpandOrderbyDefault.stringValue()));
							} else {
								repository.setExpandOrderbyDefault(false);
							}
							if (valueOfTextSearchType != null) {
								repository.setTextSearchType(TextSearchType.get(valueOfTextSearchType.stringValue()));
							} else {
								repository.setTextSearchType(TextSearchType.DEFAULT);
							}
							if (valueOfIncludeImplicitRDF != null) {
								repository.setIncludeImplicitRDF(
										Boolean.parseBoolean(valueOfIncludeImplicitRDF.stringValue()));
							} else {
								repository.setIncludeImplicitRDF(false);
							}
							if (valueOfBottomUpSPARQLOptimization != null) {
								repository.setBottomUpSPARQLOptimization(
										Boolean.parseBoolean(valueOfBottomUpSPARQLOptimization.stringValue()));
							} else {
								repository.setBottomUpSPARQLOptimization(true);
							}
							if (valueOfSupportScripting != null) {
								repository.setSupportScripting(
										Boolean.parseBoolean(valueOfSupportScripting.stringValue()));
							} else {
								repository.setSupportScripting(false);
							}
							rdfRepositoryList.put(((IRI) valueOfDataset).getLocalName(), repository);
						} catch (RepositoryConfigException e) {
							log.warn("Failed to complete definition of dataset\n" + e.getLocalizedMessage());
						}
					}
				}
			} catch (MalformedQueryException e) {
				log.error("MalformedQuery " + RdfConstants.getMetaQueries().get(RdfConstants.URI_REPOSITORYQUERY), e);
				throw new OData2SparqlException();
			} catch (QueryEvaluationException e) {
				log.error("Query Evaluation Exception "
						+ RdfConstants.getMetaQueries().get(RdfConstants.URI_REPOSITORYQUERY), e);
				throw new OData2SparqlException();
			} finally {
				modelsConnection.close();
			}
		} catch (RepositoryException e) {
			log.error("Cannot get connection to repository: check directory", e);
			throw new OData2SparqlException();
		} finally {
			//Cannot shutdown repositoryManager at this stage as it will terminate the connections to the individual repositories
			//repositoryManager.shutDown();
		}
	}

	/**
	 * Bootstrap remote repository.
	 *
	 * @param repositoryUrl the repository url
	 * @return the repository manager
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	@SuppressWarnings("deprecation")
	private RepositoryManager bootstrapRemoteRepository(String repositoryUrl) throws OData2SparqlException {
		RemoteRepositoryManager repositoryManager = new RemoteRepositoryManager(repositoryUrl);
		log.info("Trying remote Repository at " + repositoryUrl);
		try {
			repositoryManager.initialize();
			repositoryManager.getRepositoryInfo(RdfConstants.systemId);
			log.warn("No populated system repository at " + repositoryUrl + ". Will attempt to preload");
			preloadSystemRepository(repositoryManager);
		} catch (RepositoryException e) {
			log.warn("Cannot initialize remote repository manager at " + repositoryUrl
					+ ". Will use local repository instead");
			throw new OData2SparqlException("RdfRepositories bootstrapRemoteRepository failure", null);
		}
		return repositoryManager;
	}

	/**
	 * Bootstrap local repository.
	 *
	 * @return the repository manager
	 * @throws OData2SparqlException the o data 2 sparql exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("deprecation")
	private RepositoryManager bootstrapLocalRepository() throws OData2SparqlException, IOException {
		//Create a local repository manager for managing all of the endpoints including the model itself
		//String localRepositoryManagerDirectory=RdfConstants.repositoryWorkingDirectory;
		if (this.repositoryFolder != null && !this.repositoryFolder.isEmpty()) {
			// Should be local to Tomcat as specific to each running servlet/webapp
			localRepositoryManagerDirectory = Paths.get(repositoryDir, this.repositoryFolder).toString();
		}
		try {
			FileUtil.deleteDir(new File(localRepositoryManagerDirectory));
		} catch (IOException e) {
			log.info("Local repository directory does not yet exists at " + localRepositoryManagerDirectory);
			FileUtil.createDirIfNotExists(new File(localRepositoryManagerDirectory));
			//throw new RepositoryConfigException(e);
		}
		LocalRepositoryManager repositoryManager = new LocalRepositoryManager(
				new File(localRepositoryManagerDirectory));
		log.info("Using local repository at " + localRepositoryManagerDirectory);

		try {
			repositoryManager.initialize();
		} catch (RepositoryException e) {
			log.error("Cannot initialize repository manager at " + localRepositoryManagerDirectory
					+ "Check also web-xml init-param repositoryFolder", e);
			throw new OData2SparqlException("RdfRepositories loadRepositories failure", e);
		}

		preloadSystemRepository(repositoryManager);
		return repositoryManager;

	}

	/**
	 * Preload system repository.
	 *
	 * @param repositoryManager the repository manager
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	protected void preloadSystemRepository(RepositoryManager repositoryManager) throws OData2SparqlException {
		//Create a configuration for the system repository implementation which is a native store

		SailImplConfig systemRepositoryImplConfig = new MemoryStoreConfig();
		systemRepositoryImplConfig = new SpinSailConfig(systemRepositoryImplConfig);
		RepositoryImplConfig systemRepositoryTypeSpec = new SailRepositoryConfig(systemRepositoryImplConfig);
		RepositoryConfig systemRepositoryConfig = new RepositoryConfig(RdfConstants.systemId, systemRepositoryTypeSpec);
		try {
			// Remove any existing system repository
			// Also seems to be necessary to get a 'clean' system that can be SPARQL queried
			repositoryManager.removeRepository(RdfConstants.systemId);
			repositoryManager.addRepositoryConfig(systemRepositoryConfig);
		} catch (SailReadOnlyException e) {
			log.info("Repository read-only: will clear and reload", e);
			throw new OData2SparqlException();
		} catch (RepositoryException e) {
			log.error("Cannot add configuration to repository", e);
			throw new OData2SparqlException();
		} catch (RepositoryConfigException e) {
			log.error("Cannot add configuration to repository", e);
			throw new OData2SparqlException();
		}

		Repository systemRepository = null;

		try {
			systemRepository = repositoryManager.getRepository(RdfConstants.systemId);
		} catch (RepositoryConfigException e) {
			log.error("Cannot find " + RdfConstants.systemId + " repository", e);
			throw new OData2SparqlException();
		} catch (RepositoryException e) {
			log.error("Cannot find " + RdfConstants.systemId + " repository", e);
			throw new OData2SparqlException();
		}

		RepositoryConnection modelsConnection;
		try {
			modelsConnection = systemRepository.getConnection();
			//Clear any contents to make sure we load a fresh models.ttl
			//TODO does not seem to be clearing triples
			modelsConnection.clear();
			//String localRepositoryManagerModel=RdfConstants.repositoryWorkingDirectory;
			if (this.repositoryFolder != null && !this.repositoryFolder.isEmpty()) {
				localRepositoryManagerModel = Paths
						.get(RdfConstants.repositoryWorkingDirectory, this.repositoryFolder, "models.ttl").toString();
			} else {
				localRepositoryManagerModel = Paths.get(RdfConstants.repositoryWorkingDirectory, "models.ttl")
						.toString();
			}
			log.info("Loading models.ttl from " + localRepositoryManagerModel);
			try {
				modelsConnection.add(new File(localRepositoryManagerModel), RdfConstants.systemIRI, RDFFormat.TURTLE);
			} catch (RDFParseException e) {
				log.error("RDFParseException: Cannot parse  " + localRepositoryManagerModel
						+ " Check to ensure valid RDF/XML or TTL", e);
				System.exit(1);
				//throw new Olingo2SparqlException();
			} catch (IOException e) {
				log.error("Cannot access " + localRepositoryManagerModel
						+ " Check it is located in correct directory and is visible", e);
				System.exit(1);
				//throw new Olingo2SparqlException();
			} catch (RepositoryException e) {
				log.error("RepositoryException: Cannot access " + localRepositoryManagerModel
						+ " Check it is located in WEBINF/classes/", e);
				System.exit(1);
				//throw new Olingo2SparqlException();
			} finally {

			}
			try {
				log.info("Loading odata4sparql from " + RdfConstants.odata4sparqlFile);
				modelsConnection.add(new File(RdfConstants.odata4sparqlFile), RdfConstants.systemIRI, RDFFormat.RDFXML);
			} catch (RDFParseException e) {
				log.error("Cannot parse " + RdfConstants.odata4sparqlFile, e);
				throw new OData2SparqlException();
			} catch (IOException e) {
				log.error("Cannot access " + RdfConstants.odata4sparqlFile, e);
				throw new OData2SparqlException();
			} finally {

			}

			//			try {
			//				log.info("Loading rdf from " + RdfConstants.rdfFile);
			//				modelsConnection.add(new File(RdfConstants.rdfFile), RdfConstants.systemId, RDFFormat.TURTLE);
			//			} catch (RDFParseException e) {
			//				log.error("Cannot parse " + RdfConstants.rdfFile, e);
			//				throw new OData2SparqlException();
			//			} catch (IOException e) {
			//				log.error("Cannot access " + RdfConstants.rdfFile, e);
			//				throw new OData2SparqlException();
			//			} finally {
			//
			//			}
			//			try {
			//				log.info("Loading rdfs from " + RdfConstants.rdfsFile);
			//				modelsConnection.add(new File(RdfConstants.rdfsFile), RdfConstants.systemId, RDFFormat.TURTLE);
			//			} catch (RDFParseException e) {
			//				log.error("Cannot parse " + RdfConstants.rdfsFile, e);
			//				throw new OData2SparqlException();
			//			} catch (IOException e) {
			//				log.error("Cannot access " + RdfConstants.rdfsFile, e);
			//				throw new OData2SparqlException();
			//			} finally {
			//
			//			}
			try {
				log.info("Loading sail from " + RdfConstants.sailFile);
				modelsConnection.add(new File(RdfConstants.sailFile), RdfConstants.systemIRI, RDFFormat.RDFXML);
			} catch (RDFParseException e) {
				log.error("Cannot parse " + RdfConstants.sailFile, e);
				throw new OData2SparqlException();
			} catch (IOException e) {
				log.error("Cannot access " + RdfConstants.sailFile, e);
				throw new OData2SparqlException();
			} finally {

			}
			//			try {
			//				log.info("Loading sp from " + RdfConstants.spFile);
			//				modelsConnection.add(new File(RdfConstants.spFile), RdfConstants.systemId, RDFFormat.TURTLE);
			//			} catch (RDFParseException e) {
			//				log.error("Cannot parse " + RdfConstants.spFile, e);
			//				throw new OData2SparqlException();
			//			} catch (IOException e) {
			//				log.error("Cannot access " + RdfConstants.spFile, e);
			//				throw new OData2SparqlException();
			//			} finally {
			//
			//			}
			try {
				log.info("Loading contextmenu from " + RdfConstants.contextmenuFile);
				modelsConnection.add(new File(RdfConstants.contextmenuFile), RdfConstants.systemIRI, RDFFormat.RDFXML);
			} catch (RDFParseException e) {
				log.error("Cannot parse " + RdfConstants.contextmenuFile, e);
				throw new OData2SparqlException();
			} catch (IOException e) {
				log.error("Cannot access " + RdfConstants.contextmenuFile, e);
				throw new OData2SparqlException();
			} finally {

			}
			try {
				log.info("Loading olgap from " + RdfConstants.olgapFile);
				modelsConnection.add(new File(RdfConstants.olgapFile), RdfConstants.systemIRI, RDFFormat.RDFXML);
			} catch (RDFParseException e) {
				log.error("Cannot parse " + RdfConstants.olgapFile, e);
				throw new OData2SparqlException();
			} catch (IOException e) {
				log.error("Cannot access " + RdfConstants.olgapFile, e);
				throw new OData2SparqlException();
			} finally {

			}
			try {
				log.info("Loading search from " + RdfConstants.searchFile);
				modelsConnection.add(new File(RdfConstants.searchFile), RdfConstants.systemIRI, RDFFormat.RDFXML);
			} catch (RDFParseException e) {
				log.error("Cannot parse " + RdfConstants.searchFile, e);
				throw new OData2SparqlException();
			} catch (IOException e) {
				log.error("Cannot access " + RdfConstants.searchFile, e);
				throw new OData2SparqlException();
			} finally {

			}
			try {
				log.info("Loading change from " + RdfConstants.changeFile);
				modelsConnection.add(new File(RdfConstants.changeFile), RdfConstants.systemIRI, RDFFormat.RDFXML);
			} catch (RDFParseException e) {
				log.error("Cannot parse " + RdfConstants.changeFile, e);
				throw new OData2SparqlException();
			} catch (IOException e) {
				log.error("Cannot access " + RdfConstants.changeFile, e);
				throw new OData2SparqlException();
			} finally {

			}
			try {
				log.info("Loading script from " + RdfConstants.scriptFile);
				modelsConnection.add(new File(RdfConstants.scriptFile), RdfConstants.systemIRI, RDFFormat.RDFXML);
			} catch (RDFParseException e) {
				log.error("Cannot parse " + RdfConstants.scriptFile, e);
				throw new OData2SparqlException();
			} catch (IOException e) {
				log.error("Cannot access " + RdfConstants.scriptFile, e);
				throw new OData2SparqlException();
			} finally {

			}
		} catch (RepositoryException e) {
			log.error("Cannot connect to local system repository", e);
			throw new OData2SparqlException();
		}
	}

	/**
	 * Read prefixes.
	 *
	 * @param modelsConnection the models connection
	 * @param valueOfDataset the value of dataset
	 * @return the tree map
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private TreeMap<String, Namespace> readPrefixes(RepositoryConnection modelsConnection, Value valueOfDataset)
			throws OData2SparqlException {
		TreeMap<String, Namespace> namespaces = new TreeMap<String, Namespace>();
		try {
			//Identify prefixes for the provided dataset:
			String queryString = RdfConstants.getMetaQueries().get(RdfConstants.URI_PREFIXQUERY);
			TupleQuery tupleQuery = modelsConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			tupleQuery.setBinding("Dataset", valueOfDataset);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				while (result.hasNext()) { // iterate over the result
					BindingSet bindingSet = result.next();
					Value valueOfPrefix = bindingSet.getValue("prefix");
					Value valueOfNamespace = bindingSet.getValue("namespace");
					namespaces.put(valueOfPrefix.stringValue(),
							new SimpleNamespace(valueOfPrefix.stringValue(), valueOfNamespace.stringValue().trim()));
				}
			} finally {
				result.close();
			}

		} catch (MalformedQueryException e) {
			log.error("MalformedQuery " + RdfConstants.getMetaQueries().get(RdfConstants.URI_PREFIXQUERY), e);
			throw new OData2SparqlException("RdfRepositories readPrefixes failure", e);
		} catch (RepositoryException e) {
			log.error("RepositoryException " + RdfConstants.getMetaQueries().get(RdfConstants.URI_PREFIXQUERY), e);
			throw new OData2SparqlException("RdfRepositories readPrefixes failure", e);
		} catch (QueryEvaluationException e) {
			log.error("QueryEvaluationException " + RdfConstants.getMetaQueries().get(RdfConstants.URI_PREFIXQUERY), e);
			throw new OData2SparqlException("RdfRepositories readPrefixes failure", e);
		} finally {
		}
		return namespaces;

	}

	/**
	 * Read queries.
	 *
	 * @param modelsConnection the models connection
	 * @param RDFSModel the RDFS model
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	private void readQueries(RepositoryConnection modelsConnection, Value RDFSModel) throws OData2SparqlException {
		Hashtable<Value, Hashtable<Value, String>> metaModels = RdfConstants.getMetaModels();
		//Bootstrap the standard queries
		try {
			TupleQuery tupleQuery = modelsConnection.prepareTupleQuery(QueryLanguage.SPARQL,
					RdfConstants.bootStrapQuery);
			tupleQuery.setBinding("Metadata", RDFSModel);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					Value valueOfMetaModel = bindingSet.getValue("Metadata");
					Hashtable<Value, String> metaModelQueries;
					if (metaModels.containsKey(valueOfMetaModel)) {
						metaModelQueries = metaModels.get(valueOfMetaModel);
					} else {
						metaModelQueries = new Hashtable<Value, String>();
						metaModels.put(valueOfMetaModel, metaModelQueries);
					}

					Value valueOfQuery = bindingSet.getValue("Query");
					Value valueOfQueryString = bindingSet.getValue("QueryString");

					metaModelQueries.put(valueOfQuery, valueOfQueryString.stringValue());
				}
				RdfConstants.setMetaQueries(metaModels.get(RdfConstants.URI_DEFAULTMETAMODEL));
			} finally {
				result.close();
			}
		} catch (MalformedQueryException e) {
			log.error("Malformed Bootstrap Query ", e);
			throw new OData2SparqlException("RdfRepositories readQueries failure", e);
		} catch (RepositoryException e) {
			log.error("RepositoryException Bootstrap Query  ", e);
			throw new OData2SparqlException("RdfRepositories readQueries failure", e);
		} catch (QueryEvaluationException e) {
			log.error("QueryEvaluationException Bootstrap Query ", e);
			throw new OData2SparqlException("RdfRepositories readQueries failure", e);
		} finally {
		}
	}
}
