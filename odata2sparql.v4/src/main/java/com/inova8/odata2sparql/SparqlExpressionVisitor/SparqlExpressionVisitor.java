/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;

public class SparqlExpressionVisitor implements ExpressionVisitor<Object> {
	private final String SUBJECT_POSTFIX = "_s";
	private String sPath = "";
	private final HashSet<RdfProperty> properties = new HashSet<RdfProperty>();
	private final HashMap<String, HashSet<RdfProperty>> navigationProperties = new HashMap<String, HashSet<RdfProperty>>();

	private final HashMap<String, NavPropertyPropertyFilter> navPropertyPropertyFilters = new HashMap<String, NavPropertyPropertyFilter>();
	private final RdfModel rdfModel;
	private final RdfEntityType entityType;
	private String conditionString = "";
	private final Boolean allStatus = false;

	public SparqlExpressionVisitor(RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, RdfEntityType entityType,
			String path) {
		super();
		this.rdfModel = rdfModel;
		this.entityType = entityType;
		this.sPath = path;
	}

	public boolean isAllStatus() {
		//TODO
		return allStatus;
	}

	public String getConditionString() {
		return conditionString;
	}

	public void setConditionString(String conditionString) {
		this.conditionString = conditionString;

	}

	public String getAggregateFilterClause() {
		return this.isAllStatus() ? conditionString : "";
	}

	public HashMap<String, NavPropertyPropertyFilter> getNavPropertyPropertyFilters() {
		return navPropertyPropertyFilters;
	}

	public HashMap<String, HashSet<RdfProperty>> getNavigationProperties() {
		return navigationProperties;
	}

	public String getNavigationPropertySubjects() {
		String navigationPropertySubject = "";
		for (Entry<String, HashSet<RdfProperty>> navigationPropertyEntry : navigationProperties.entrySet()) {
			if (entityType.isOperation()) {

			} else {//navigationPropertyEntry.getValue().
				navigationPropertySubject += "?" + navigationPropertyEntry.getKey() + "_s ";
			}
		}
		return navigationPropertySubject;
	}

	public String getPropertyClause() {
		String propertyClause = "";
		String key = entityType.entityTypeName;
		for (RdfProperty property : properties) {
			if (entityType.isOperation()) {

			} else {
				if (property.getIsKey()) {
					if (properties.size() > 1) {
						//prefix predicate with key
						propertyClause += "BIND( ?" + key + "_s as ?" + key + property.propertyName
								+ RdfConstants.PROPERTY_POSTFIX + ").";
					} else {
						//prefix predicate with key
						propertyClause += "?" + key + "_s <" + property.getPropertyURI() + "> ?" + key
								+ property.propertyName + RdfConstants.PROPERTY_POSTFIX + " .";
					}
				} else {
					//prefix predicate with key
					propertyClause += "?" + key + "_s <" + property.getPropertyURI() + "> ?" + key
							+ property.propertyName + RdfConstants.PROPERTY_POSTFIX + " .";
				}
			}
		}
		for (Entry<String, HashSet<RdfProperty>> navigationPropertyEntry : navigationProperties.entrySet()) {
			if (entityType.isOperation()) {

			} else {//navigationPropertyEntry.getValue().
				for (RdfProperty rdfProperty : navigationPropertyEntry.getValue()) {
					if (rdfProperty.getEDMPropertyName().equals(RdfConstants.SUBJECT)) {
						//TODO need to add the navigation property to this subject rather than <rdf:subject>
					} else {
						propertyClause += "?" + navigationPropertyEntry.getKey() + "_s <" + rdfProperty.getPropertyURI()
								+ "> ?" + navigationPropertyEntry.getKey() + rdfProperty.propertyName
								+ RdfConstants.PROPERTY_POSTFIX + " .";
					}
				}
			}
		}
		return propertyClause;

	}

	public String getFilterClause() {
		if (!allStatus) {
			if (conditionString != "")
				return "FILTER(" + conditionString + ")";
		}
		return "";
	}

	private void putNavPropertyPropertyFilter(String sPath, RdfAssociation navProperty, RdfProperty property,
			String filter) {
		NavPropertyPropertyFilter navPropertyPropertyFilter;
		if (!(navPropertyPropertyFilters.containsKey(sPath))) {
			navPropertyPropertyFilter = new NavPropertyPropertyFilter();
			navPropertyPropertyFilters.put(sPath, navPropertyPropertyFilter);
		} else {
			navPropertyPropertyFilter = navPropertyPropertyFilters.get(sPath);
		}
		HashMap<String, PropertyFilter> propertyFilters = navPropertyPropertyFilter.getPropertyFilters();
		PropertyFilter propertyFilter;
		if (property != null) {
			if (!propertyFilters.containsKey(property.propertyName)) {
				propertyFilter = new PropertyFilter(property);
				propertyFilters.put(property.propertyName, propertyFilter);
			} else {
				propertyFilter = propertyFilters.get(property.propertyName);
			}
			if (filter != null && !filter.isEmpty())
				propertyFilter.getFilters().add(filter);
		}
	}

	@Override
	public Object visitBinaryOperator(BinaryOperatorKind operator, Object left, Object right)
			throws ExpressionVisitException, ODataApplicationException {
		String sparqlOperator = "";
		switch (operator) {
		case EQ:
			sparqlOperator = "=";
			break;
		case NE:
			sparqlOperator = "!=";
			break;
		case OR:
			sparqlOperator = "||";
			break;
		case AND:
			sparqlOperator = "&&";
			break;
		case GE:
			sparqlOperator = ">=";
			break;
		case GT:
			sparqlOperator = ">";
			break;
		case LE:
			sparqlOperator = "<=";
			break;
		case LT:
			sparqlOperator = "<";
			break;
		default:
			//Other operators are not supported for SQL Statements
			throw new UnsupportedOperationException("Unsupported operator: " + operator.toString());
		}
		//return the binary statement
		return "(" + left + " " + sparqlOperator + " " + right + ")";
	}

	@Override
	public Object visitUnaryOperator(UnaryOperatorKind operator, Object operand)
			throws ExpressionVisitException, ODataApplicationException {
		String sparqlunary = "";
		switch (operator) {
		case MINUS:
			sparqlunary = "-";
			break;
		case NOT:
			sparqlunary = "!";
			break;
		default:
			throw new UnsupportedOperationException("Unsupported unary: " + operator.toString());
		}
		return sparqlunary;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object visitMethodCall(MethodKind methodCall, List parameters)
			throws ExpressionVisitException, ODataApplicationException {
		String sparqlmethod = "";
		switch (methodCall) {
		case ENDSWITH:
			sparqlmethod = "STRENDS(" + parameters.get(1) + "," + parameters.get(0) + ")";
			break;
		case INDEXOF:
			sparqlmethod = "";
			break;
		case STARTSWITH:
			sparqlmethod = "STRSTARTS(" + parameters.get(1) + "," + parameters.get(0) + ")";
			break;
		case TOLOWER:
			sparqlmethod = "LCASE(" + parameters.get(0) + ")";
			break;
		case TOUPPER:
			sparqlmethod = "UCASE(" + parameters.get(0) + ")";
			break;
		case TRIM:
			sparqlmethod = "";
			break;
		case SUBSTRING:
			if (parameters.size() > 2) {
				sparqlmethod = "substr(" + parameters.get(1) + "," + parameters.get(0) + "," + parameters.get(2) + ")";
			} else {
				sparqlmethod = "substr(" + parameters.get(1) + "," + parameters.get(0) + ")";
			}
			break;
		case CONTAINS:
			sparqlmethod = "regex(" + parameters.get(0) + "," + parameters.get(1) + ", \"i\")";
			//TODO replacing with contains sparqlmethod = "contains(" + parameters.get(1) + "," + parameters.get(0) + ")";
			break;
		case CONCAT:
			sparqlmethod = "concat(" + parameters.get(0) + "," + parameters.get(1) + ")";
			break;
		case LENGTH:
			sparqlmethod = "STRLEN(" + parameters.get(0) + ")";
			break;
		case YEAR:
			sparqlmethod = "year(" + parameters.get(0) + ")";
			break;
		case MONTH:
			sparqlmethod = "month(" + parameters.get(0) + ")";
			break;
		case DAY:
			sparqlmethod = "day(" + parameters.get(0) + ")";
			break;
		case HOUR:
			sparqlmethod = "hours(" + parameters.get(0) + ")";
			break;
		case MINUTE:
			sparqlmethod = "minutes(" + parameters.get(0) + ")";
			break;
		case SECOND:
			sparqlmethod = "seconds(" + parameters.get(0) + ")";
			break;
		case ROUND:
			sparqlmethod = "round(" + parameters.get(0) + ")";
			break;
		case FLOOR:
			sparqlmethod = "floor(" + parameters.get(0) + ")";
			break;
		case CEILING:
			sparqlmethod = "ceil(" + parameters.get(0) + ")";
			break;
		default:
			throw new UnsupportedOperationException("Unsupported method: " + methodCall.toString());
		}
		return sparqlmethod;

	}

	@Override
	public Object visitLambdaExpression(String lambdaFunction, String lambdaVariable, Expression expression)
			throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException {
		String decodedEntityKey = SparqlEntity.URLDecodeEntityKey(literal.toString());
		try{
			return "<"+ this.rdfModel.getRdfPrefixes().expandPredicateKey(decodedEntityKey)+">";
		
		} catch(Exception e) {
			switch (literal.getType().toString()) {
			case "Null":
				return "null";
			case "Edm.Time":
				return "\"" + literal.getText() + "\"^^xsd:time";
			case "Edm.Date":
				return "\"" + literal.getText() + "\"^^xsd:date";
			case "Edm.DateTime":
				return "\"" + literal.getText() + "\"^^xsd:dateTime";
			case "Edm.DateTimeOffset":
			case "Edm.String":
				return "\"" + literal.getText().substring(1, literal.getText().length() - 1) + "\"";
			case "Edm.Guid":
				return "guid\"" + literal.getText() + "\"";
			case "Edm.Binary":
				return "X\"" + literal.getText() + "\"";
			default:
				return literal.getText();
			}
		}
	}

	@Override
	public Object visitMember(Member member) throws ExpressionVisitException, ODataApplicationException {
		RdfProperty rdfProperty = null;
		String memberProperty = member.getResourcePath().getUriResourceParts().get(0).toString();
		try {
			rdfProperty = entityType.findProperty(memberProperty);
			if (entityType.isOperation()) {
				return "?" + rdfProperty.getVarName();
			} else {
				if (member instanceof UriResourceNavigation) {
					if (sPath.isEmpty()) {
						sPath = entityType.entityTypeName;
					}
					//Need to create path
					//currentNavigationProperty = (EdmNavigationProperty) edmProperty;
					sPath += member.toString();
					putNavPropertyPropertyFilter(sPath, null, null, null);

					return sPath;
				} else if (RdfConstants.SUBJECT.equals(memberProperty)) {
					//TODO still need to add full path to disambiguate
					//rdfProperty = entityType.findProperty(edmProperty.getName());
					//properties.add(rdfProperty);
					if (!sPath.isEmpty()) {
						//rdfProperty = entityType.findProperty(memberProperty);
						if (navigationProperties.containsKey(sPath)) {
							navigationProperties.get(sPath).add(rdfProperty);
						} else {
							HashSet<RdfProperty> properties = new HashSet<RdfProperty>();
							properties.add(rdfProperty);
							navigationProperties.put(sPath, properties);
						}
						//putNavPropertyPropertyFilter(sPath,null,rdfProperty,null); 
						String visitProperty = "?" + sPath + SUBJECT_POSTFIX;
						sPath = "";
						return visitProperty;
					} else {
						sPath = "";
						return "?" + entityType.entityTypeName + SUBJECT_POSTFIX;
					}
				} else if (rdfProperty.isFK()) {
					properties.add(rdfProperty);
					putNavPropertyPropertyFilter(entityType.entityTypeName, null, rdfProperty, null);
 					String visitProperty = null;
					if (sPath.equals("")) {
						visitProperty = "?" + entityType.getEDMEntityTypeName() + memberProperty
								+ RdfConstants.PROPERTY_POSTFIX;
					} else {
						visitProperty = "?" + sPath + memberProperty + RdfConstants.PROPERTY_POSTFIX;
					}
					return visitProperty;
					
				}else {
					//rdfProperty = entityType.findProperty(memberProperty);
					properties.add(rdfProperty);
					putNavPropertyPropertyFilter(entityType.entityTypeName, null, rdfProperty, null);
					String visitProperty = null;
					if (sPath.equals("")) {
						visitProperty = "?" + entityType.getEDMEntityTypeName() + memberProperty
								+ RdfConstants.PROPERTY_POSTFIX;
					} else {
						visitProperty = "?" + sPath + memberProperty + RdfConstants.PROPERTY_POSTFIX;
					}
					visitProperty = castVariable(rdfProperty, visitProperty);
					sPath = "";
					return visitProperty;
				}
			}
		} catch (EdmException e) {
			throw new UnsupportedOperationException("Unrecognized property");//+ uriLiteral);
		}
	}

	private String castVariable(RdfProperty rdfProperty, String visitProperty) {
		switch (rdfProperty.getPropertyTypeName()) {
		case RdfConstants.XSD_DATETIME:
			visitProperty = "<" + RdfConstants.XSD_DATETIME + ">(" + visitProperty + ")";
			break;
		case RdfConstants.XSD_DATE:
			visitProperty = "<" + RdfConstants.XSD_DATE + ">(" + visitProperty + ")";
			break;
		default:
			break;
		}
		return visitProperty;
	}

	@Override
	public Object visitAlias(String aliasName) throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitTypeLiteral(EdmType type) throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitLambdaReference(String variableName) throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object visitEnum(EdmEnumType type, List enumValues)
			throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

}
