/*
 * inova8 2020
 */
package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.edm.provider.CsdlTerm;
import org.apache.olingo.commons.api.ex.ODataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

/**
 * The Class RdfEdmProvider.
 */
public class RdfEdmProvider extends CsdlAbstractEdmProvider {
	
	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RdfEdmProvider.class);
	
	/** The rdf edm model provider. */
	private final RdfEdmModelProvider rdfEdmModelProvider;
	
	/** The rdf edm providers. */
	private final RdfEdmProviders rdfEdmProviders;
	

	/**
	 * Instantiates a new rdf edm provider.
	 *
	 * @param rdfEdmProviders the rdf edm providers
	 * @param rdfRepository the rdf repository
	 * @throws OData2SparqlException the o data 2 sparql exception
	 */
	RdfEdmProvider( RdfEdmProviders rdfEdmProviders, RdfRepository rdfRepository) throws OData2SparqlException {
		this.rdfEdmModelProvider = new RdfEdmModelProvider(rdfRepository);
		this.rdfEdmProviders =rdfEdmProviders;
		rdfEdmProviders.getRdfEdmProviders().put(rdfRepository.getModelName(), this);
	}

	/**
	 * Gets the mapped entity type.
	 *
	 * @param fullQualifiedName the full qualified name
	 * @return the mapped entity type
	 */
	public RdfEntityType getMappedEntityType(FullQualifiedName fullQualifiedName) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedEntityType(fullQualifiedName);
	} 

	/**
	 * Gets the rdf entity typefrom edm entity set.
	 *
	 * @param edmEntitySet the edm entity set
	 * @return the rdf entity typefrom edm entity set
	 * @throws EdmException the edm exception
	 */
	public RdfEntityType getRdfEntityTypefromEdmEntitySet(EdmEntitySet edmEntitySet) throws EdmException {
		return this.getMappedEntityType(new FullQualifiedName(edmEntitySet.getEntityType().getNamespace(), edmEntitySet
				.getEntityType().getName()));  
	}
	
	/**
	 * Gets the mapped property.
	 *
	 * @param fqnProperty the fqn property
	 * @return the mapped property
	 */
	public RdfProperty getMappedProperty(FullQualifiedName fqnProperty) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedProperty(fqnProperty);
	}

	/**
	 * Gets the mapped navigation property.
	 *
	 * @param edmNavigationProperty the edm navigation property
	 * @return the mapped navigation property
	 */
	public RdfNavigationProperty getMappedNavigationProperty(FullQualifiedName edmNavigationProperty) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedNavigationProperty(edmNavigationProperty);
	}

	/**
	 * Gets the edm metadata.
	 *
	 * @return the edm metadata
	 */
	public RdfModelToMetadata getEdmMetadata() {
		return this.rdfEdmModelProvider.getEdmMetadata();
	}

	/**
	 * Gets the rdf repository.
	 *
	 * @return the rdf repository
	 */
	public RdfRepository getRdfRepository() {
		return this.rdfEdmModelProvider.getRdfRepository();
	}

	/**
	 * Gets the rdf model.
	 *
	 * @return the rdf model
	 */
	public RdfModel getRdfModel() {
		return this.rdfEdmModelProvider.getRdfModel();
	}
 
	/**
	 * Gets the schemas.
	 *
	 * @return the schemas
	 * @throws ODataException the o data exception
	 */
	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {
		return this.rdfEdmModelProvider.getEdmMetadata().getSchemas();
	}

	/**
	 * Gets the entity type.
	 *
	 * @param edmFQName the edm FQ name
	 * @return the entity type
	 * @throws ODataException the o data exception
	 */
	@Override
	public CsdlEntityType getEntityType(final FullQualifiedName edmFQName) throws ODataException {

		String nameSpace = edmFQName.getNamespace();
		try {
			for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				if (nameSpace.equals(schema.getNamespace())) {
					String entityTypeName = edmFQName.getName();
					for (CsdlEntityType entityType : schema.getEntityTypes()) {
						if (entityTypeName.equals(entityType.getName())) {
							return entityType;
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.error("NullPointerException getEntityType " + edmFQName);
			throw new ODataException("NullPointerException getEntityType " + edmFQName);
		}
		return null;
	}

	/**
	 * Gets the complex type.
	 *
	 * @param edmFQName the edm FQ name
	 * @return the complex type
	 * @throws ODataException the o data exception
	 */
	@Override
	public CsdlComplexType getComplexType(final FullQualifiedName edmFQName) throws ODataException {

		String nameSpace = edmFQName.getNamespace();
		try {
			for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				if (nameSpace.equals(schema.getNamespace())) {
					String complexTypeName = edmFQName.getName();
					for (CsdlComplexType complexType : schema.getComplexTypes()) {
						if (complexTypeName.equals(complexType.getName())) {
							return complexType;
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.error("NullPointerException getComplexType " + edmFQName);
			throw new ODataException("NullPointerException getComplexType " + edmFQName);
		}
		return null;
	}

	/**
	 * Gets the entity set.
	 *
	 * @param entityContainer the entity container
	 * @param name the name
	 * @return the entity set
	 * @throws ODataException the o data exception
	 */
	@Override
	public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, final String name) throws ODataException {

		try {
			//for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {			
				CsdlEntityContainer schemaEntityContainer = this.rdfEdmModelProvider.getEdmMetadata().getSchema(entityContainer.getNamespace()).getEntityContainer();
				//if (entityContainer.equals(schemaEntityContainer.getName())) {
					for (CsdlEntitySet entitySet : schemaEntityContainer.getEntitySets()) {
						if (name.equals(entitySet.getName())) {
							return entitySet;
						}
					}

				//}
		//	}
		} catch (NullPointerException e) {
			log.error("NullPointerException getEntitySet " + entityContainer + " " + name);
			throw new ODataException("NullPointerException getEntitySet " + entityContainer + " " + name);
		}
		return null;
	}

	/**
	 * Gets the function import.
	 *
	 * @param entityContainer the entity container
	 * @param name the name
	 * @return the function import
	 * @throws ODataException the o data exception
	 */
	@Override
	public CsdlFunctionImport getFunctionImport(FullQualifiedName entityContainer, final String name)
			throws ODataException {

		try {
			//for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				CsdlEntityContainer schemaEntityContainer = this.rdfEdmModelProvider.getEdmMetadata().getSchema(entityContainer.getNamespace()).getEntityContainer();
				
				//if (entityContainer.equals(schemaEntityContainer.getName())) {
					for (CsdlFunctionImport functionImport : schemaEntityContainer.getFunctionImports()) {
						if (name.equals(functionImport.getName())) {
							return functionImport;
						}
					}

				//}
			//}
		} catch (NullPointerException e) {
			log.error("NullPointerException getFunctionImport " + entityContainer + " " + name);
			throw new ODataException("NullPointerException getFunctionImport " + entityContainer + " " + name);
		}

		return null;
	}

	/**
	 * Gets the entity container info.
	 *
	 * @param entityContainer the entity container
	 * @return the entity container info
	 * @throws ODataException the o data exception
	 */
	@Override
	public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainer) throws ODataException {
		if (entityContainer == null) {
			// Assume request for null container means default container
			return new CsdlEntityContainerInfo().setContainerName(new FullQualifiedName(RdfConstants.ENTITYCONTAINERNAMESPACE,RdfConstants.ENTITYCONTAINER));
		} else {
			try {
				for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
					CsdlEntityContainer schemaEntityContainer = schema.getEntityContainer();
					if (entityContainer.toString().equals(schemaEntityContainer.getName())) {
						return new CsdlEntityContainerInfo().setContainerName(entityContainer);
					}
				}
			} catch (NullPointerException e) {
				log.error("NullPointerException getEntityContainerInfo " + entityContainer);
				throw new ODataException("NullPointerException getEntityContainerInfo " + entityContainer);
			}
		}
		return null;
	}

	/**
	 * Gets the functions.
	 *
	 * @param functionName the function name
	 * @return the functions
	 * @throws ODataException the o data exception
	 */
	@Override
	public List<CsdlFunction> getFunctions(FullQualifiedName functionName) throws ODataException {
	//	return super.getFunctions(functionName);
		String nameSpace = functionName.getNamespace();
		try {
			for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				if (nameSpace.equals(schema.getNamespace())) {
					String fnName = functionName.getName();
					for (CsdlFunction function : schema.getFunctions()) {
						if (fnName.equals(function.getName())) {
							ArrayList<CsdlFunction> listFunction = new ArrayList<CsdlFunction>();
							listFunction.add(function);
							return listFunction;
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.error("NullPointerException getFunctions " + functionName);
			throw new ODataException("NullPointerException getFunctions " + functionName);
		}
		return null;
	}

	/**
	 * Gets the term.
	 *
	 * @param termName the term name
	 * @return the term
	 * @throws ODataException the o data exception
	 */
	@Override
	public CsdlTerm getTerm(FullQualifiedName termName) throws ODataException {
		return RdfConstants.TERMS.get(termName.getName());
	}

	/**
	 * Gets the entity container.
	 *
	 * @return the entity container
	 * @throws ODataException the o data exception
	 */
	@Override
	public CsdlEntityContainer getEntityContainer() throws ODataException {
		for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
			CsdlEntityContainer schemaEntityContainer = schema.getEntityContainer();
			//First one found will be the container, as only one allowed in V4
			if (schemaEntityContainer!=null) {
				return schemaEntityContainer;
			}
		}	
		return null;
	}

	/**
	 * Gets the rdf edm providers.
	 *
	 * @return the rdf edm providers
	 */
	public RdfEdmProviders getRdfEdmProviders() {
		return rdfEdmProviders;
	}


	
}
