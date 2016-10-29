package com.inova8.odata2sparql.uri;

import java.util.HashMap;
import java.util.HashSet;

//import org.apache.olingo.odata2.api.edm.EdmEntitySet;
//import org.apache.olingo.odata2.api.edm.EdmException;
//import org.apache.olingo.odata2.api.exception.ODataApplicationException;
//import org.apache.olingo.odata2.api.uri.ExpandSelectTreeNode;
//import org.apache.olingo.odata2.api.uri.UriInfo;
//import org.apache.olingo.odata2.api.uri.UriParser;
//import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
//
//import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
//import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
//import com.inova8.odata2sparql.SparqlExpressionVisitor.SparqlExpressionVisitor;

public class UriInfo {

//	private final UriType uriType;
//	private final UriInfo uriInfo;
//	private RdfEntityType rdfEntityType = null;
//	private RdfEntityType rdfTargetEntityType = null;
//	private EdmEntitySet edmEntitySet = null;
//	private EdmEntitySet edmTargetEntitySet = null;
//	private ExpandSelectTreeNode expandSelectTreeNode;
//	private HashMap<String, RdfAssociation> expandSelectNavPropertyMap;
//	private SparqlExpressionVisitor filterClause;
//	private HashMap<String, HashSet<String>> selectPropertyMap;
//	
//	prepareBuilder();
//	
//	private void prepareBuilder() throws EdmException, ExceptionVisitExpression, ODataApplicationException {
//		//Prepare what is required to create the SPARQL
//		switch (this.uriType) {
//		case URI1: {
//			edmEntitySet = this.uriInfo.getStartEntitySet();
//			edmTargetEntitySet = uriInfo.getTargetEntitySet();
//			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
//			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
//			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
//			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
//			filterClause = filterClause(uriInfo.getFilter(), rdfEntityType);
//			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
//		}
//			break;
//		case URI2: {
//			edmEntitySet = this.uriInfo.getStartEntitySet();
//			edmTargetEntitySet = uriInfo.getTargetEntitySet();
//			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
//			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
//			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
//			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
//			filterClause = filterClause(uriInfo.getFilter(), rdfEntityType);
//			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
//		}
//			break;
//		case URI6A: {
//			edmEntitySet = this.uriInfo.getStartEntitySet();
//			edmTargetEntitySet = uriInfo.getTargetEntitySet();
//			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
//			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
//			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
//			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
//			filterClause = filterClause(uriInfo.getFilter(), rdfEntityType);
//			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
//		}
//			break;
//		case URI6B: {
//			edmEntitySet = this.uriInfo.getStartEntitySet();
//			edmTargetEntitySet = uriInfo.getTargetEntitySet();
//			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
//			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
//			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
//			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
//			filterClause = filterClause(uriInfo.getFilter(), rdfTargetEntityType);
//			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
//		}
//			break;
//		case URI7B: {
//			//TO TEST
//			edmEntitySet = this.uriInfo.getStartEntitySet();
//			edmTargetEntitySet = uriInfo.getTargetEntitySet();
//			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
//			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
//			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
//			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
//			filterClause = filterClause(uriInfo.getFilter(), rdfTargetEntityType);//rdfEntityType);
//			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
//		}
//			break;
//		case URI15: {
//			edmEntitySet = this.uriInfo.getStartEntitySet();
//			edmTargetEntitySet = uriInfo.getTargetEntitySet();
//			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
//			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
//			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
//			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
//			filterClause = filterClause(uriInfo.getFilter(), rdfTargetEntityType);
//			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
//		}
//			break;
//		case URI16: {
//			//To be tested
//			edmEntitySet = this.uriInfo.getStartEntitySet();
//			edmTargetEntitySet = uriInfo.getTargetEntitySet();
//			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
//			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
//			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
//			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
//			filterClause = filterClause(uriInfo.getFilter(), rdfTargetEntityType);
//			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
//		}
//			break;
//		default:
//		}
//	}
}
