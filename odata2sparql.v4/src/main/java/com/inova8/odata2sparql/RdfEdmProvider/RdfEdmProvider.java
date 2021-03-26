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

public class RdfEdmProvider extends CsdlAbstractEdmProvider {
	private final Logger log = LoggerFactory.getLogger(RdfEdmProvider.class);
	private final RdfEdmModelProvider rdfEdmModelProvider;
	private final RdfEdmProviders rdfEdmProviders;
	

	RdfEdmProvider( RdfEdmProviders rdfEdmProviders, RdfRepository rdfRepository) throws OData2SparqlException {
		this.rdfEdmModelProvider = new RdfEdmModelProvider(rdfRepository);
		this.rdfEdmProviders =rdfEdmProviders;
		rdfEdmProviders.getRdfEdmProviders().put(rdfRepository.getModelName(), this);
	}

	public RdfEntityType getMappedEntityType(FullQualifiedName fullQualifiedName) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedEntityType(fullQualifiedName);
	} 

	public RdfEntityType getRdfEntityTypefromEdmEntitySet(EdmEntitySet edmEntitySet) throws EdmException {
		return this.getMappedEntityType(new FullQualifiedName(edmEntitySet.getEntityType().getNamespace(), edmEntitySet
				.getEntityType().getName()));  
	}
	public RdfProperty getMappedProperty(FullQualifiedName fqnProperty) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedProperty(fqnProperty);
	}

	public RdfNavigationProperty getMappedNavigationProperty(FullQualifiedName edmNavigationProperty) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedNavigationProperty(edmNavigationProperty);
	}

	public RdfModelToMetadata getEdmMetadata() {
		return this.rdfEdmModelProvider.getEdmMetadata();
	}

	public RdfRepository getRdfRepository() {
		return this.rdfEdmModelProvider.getRdfRepository();
	}

	public RdfModel getRdfModel() {
		return this.rdfEdmModelProvider.getRdfModel();
	}
 
	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {
		return this.rdfEdmModelProvider.getEdmMetadata().getSchemas();
	}

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

	@Override
	public CsdlTerm getTerm(FullQualifiedName termName) throws ODataException {
		return RdfConstants.TERMS.get(termName.getName());
	}

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

	public RdfEdmProviders getRdfEdmProviders() {
		return rdfEdmProviders;
	}


	
}
