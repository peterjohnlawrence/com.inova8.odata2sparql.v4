package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

public class RdfEdmProvider extends CsdlAbstractEdmProvider {
	private final Log log = LogFactory.getLog(RdfEdmProvider.class);
	private final RdfEdmModelProvider rdfEdmModelProvider;

	RdfEdmProvider(String odataVersion, RdfRepository rdfRepository) throws OData2SparqlException {
		this.rdfEdmModelProvider = new RdfEdmModelProvider(rdfRepository, odataVersion);
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

	public RdfAssociation getMappedNavigationProperty(FullQualifiedName edmNavigationProperty) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedNavigationProperty(edmNavigationProperty);
	}

	/**
	 * @return the odataVersion
	 */
	public String getOdataVersion() {
		return this.rdfEdmModelProvider.getOdataVersion();
	}

	/**
	 * @return the edmMetadata
	 */
	public RdfModelToMetadata getEdmMetadata() {
		return this.rdfEdmModelProvider.getEdmMetadata();
	}

	/**
	 * @return the rdfRepository
	 */
	public RdfRepository getRdfRepository() {
		return this.rdfEdmModelProvider.getRdfRepository();
	}

	/**
	 * @return the rdfModel
	 */
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
			log.fatal("NullPointerException getEntityType " + edmFQName);
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
			log.fatal("NullPointerException getComplexType " + edmFQName);
			throw new ODataException("NullPointerException getComplexType " + edmFQName);
		}
		return null;
	}

/*	@Override
	public Association getAssociation(final FullQualifiedName edmFQName) throws ODataException {
		String nameSpace = edmFQName.getNamespace();
		try {
			for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				if (nameSpace.equals(schema.getNamespace())) {
					String associationName = edmFQName.getName();
					for (Association association : schema.getAssociations()) {
						if (associationName.equals(association.getName())) {
							return association;
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getAssociation " + edmFQName);
			throw new ODataException("NullPointerException getAssociation " + edmFQName);
		}
		return null;
	}*/

	@Override
	public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, final String name) throws ODataException {

		try {
			for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				CsdlEntityContainer schemaEntityContainer = schema.getEntityContainer();
				if (entityContainer.equals(schemaEntityContainer.getName())) {
					for (CsdlEntitySet entitySet : schemaEntityContainer.getEntitySets()) {
						if (name.equals(entitySet.getName())) {
							return entitySet;
						}
					}

				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getEntitySet " + entityContainer + " " + name);
			throw new ODataException("NullPointerException getEntitySet " + entityContainer + " " + name);
		}
		return null;
	}

/*	@Override
	public AssociationSet getAssociationSet(FullQualifiedName entityContainer, final FullQualifiedName association,
			final String sourceEntitySetName, final String sourceEntitySetRole) throws ODataException {

		try {
			for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				CsdlEntityContainer schemaEntityContainer = schema.getEntityContainer();
				if (entityContainer.equals(schemaEntityContainer.getName())) {

					for (AssociationSet associationSet : schemaEntityContainer.getAssociationSets()) {
						if (association.equals(associationSet.getAssociation())) {
							return associationSet;
						}
					}

				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getAssociationSet " + entityContainer + " " + association);
			throw new ODataException("NullPointerException getAssociationSet " + entityContainer + " " + association);
		}
		return null;
	}*/

	@Override
	public CsdlFunctionImport getFunctionImport(FullQualifiedName entityContainer, final String name)
			throws ODataException {

		try {
			for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				CsdlEntityContainer schemaEntityContainer = schema.getEntityContainer();
				if (entityContainer.equals(schemaEntityContainer.getName())) {
					for (CsdlFunctionImport functionImport : schemaEntityContainer.getFunctionImports()) {
						if (name.equals(functionImport.getName())) {
							return functionImport;
						}
					}

				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getFunctionImport " + entityContainer + " " + name);
			throw new ODataException("NullPointerException getFunctionImport " + entityContainer + " " + name);
		}

		return null;
	}

	@Override
	public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName name) throws ODataException {
		if (name == null) {
			// Assume request for null container means default container
			return new CsdlEntityContainerInfo().setContainerName(new FullQualifiedName(RdfConstants.ENTITYCONTAINER));
		} else {
			try {
				for (CsdlSchema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {

					CsdlEntityContainer schemaEntityContainer = schema.getEntityContainer();
					if (name.equals(schemaEntityContainer.getName())) {
						return new CsdlEntityContainerInfo().setContainerName(name);
					}
				}
			} catch (NullPointerException e) {
				log.fatal("NullPointerException getEntityContainerInfo " + name);
				throw new ODataException("NullPointerException getEntityContainerInfo " + name);
			}
		}
		return null;
	}
}
