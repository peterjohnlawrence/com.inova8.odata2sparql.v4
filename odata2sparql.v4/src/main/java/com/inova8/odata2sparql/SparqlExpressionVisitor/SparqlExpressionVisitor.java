/*
 * inova8 2020
 */
package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceLambdaAll;
import org.apache.olingo.server.api.uri.UriResourceLambdaAny;
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
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfNavigationProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.SparqlStatement.SparqlEntity;

/**
 * The Class SparqlExpressionVisitor.
 */
public class SparqlExpressionVisitor implements ExpressionVisitor<Object> {
	
	/** The subject postfix. */
	private final String SUBJECT_POSTFIX = "_s";
	
	/** The s path. */
	private String sPath = "";
	
	/** The properties. */
	private final HashSet<RdfProperty> properties;
	
	/** The navigation properties. */
	private final TreeMap<String, HashSet<RdfProperty>> navigationProperties;

	/** The nav property property filters. */
	private final TreeMap<String, NavPropertyPropertyFilter> navPropertyPropertyFilters;
	
	/** The rdf model. */
	private final RdfModel rdfModel;


	/** The entity type. */
	private final RdfEntityType entityType;
	
	/** The condition string. */
	private String conditionString = "";
	
	/** The all status. */
	private final Boolean allStatus = false;
	
	/** The rdf model to metadata. */
	private final RdfModelToMetadata rdfModelToMetadata;
	
	/** The lambda navigation property. */
	private RdfNavigationProperty lambdaNavigationProperty;
	
	/** The lambda all. */
	private boolean lambdaAll = false;
	
	/** The primitive property graph pattern. */
	private String primitivePropertyGraphPattern="";

	/**
	 * Instantiates a new sparql expression visitor.
	 *
	 * @param rdfModel the rdf model
	 * @param rdfModelToMetadata the rdf model to metadata
	 * @param entityType the entity type
	 * @param path the path
	 */
	public SparqlExpressionVisitor(RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, RdfEntityType entityType,
			String path) {
		super();
		this.rdfModel = rdfModel;
		this.entityType = entityType;
		this.sPath = path;
		this.rdfModelToMetadata =rdfModelToMetadata;
		this.properties = new HashSet<RdfProperty>();
		this.navigationProperties = new TreeMap<String, HashSet<RdfProperty>>();
		this.navPropertyPropertyFilters = new TreeMap<String, NavPropertyPropertyFilter>();
	}
	
	/**
	 * Instantiates a new sparql expression visitor.
	 *
	 * @param rdfModel the rdf model
	 * @param rdfModelToMetadata the rdf model to metadata
	 * @param entityType the entity type
	 * @param path the path
	 * @param lambdaNavigationProperty the lambda navigation property
	 * @param parentExpressionVisitor the parent expression visitor
	 * @param lambdaAll the lambda all
	 */
	public SparqlExpressionVisitor(RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, RdfEntityType entityType,
			String path,RdfNavigationProperty lambdaNavigationProperty ,SparqlExpressionVisitor parentExpressionVisitor, boolean lambdaAll ) {
		super();
		this.rdfModel = rdfModel;
		this.entityType = entityType;
		this.sPath = path;
		this.rdfModelToMetadata =rdfModelToMetadata;
		this.lambdaNavigationProperty = lambdaNavigationProperty;
		this.properties = parentExpressionVisitor.getProperties() ;
		this.navigationProperties = parentExpressionVisitor.getNavigationProperties();
		this.navPropertyPropertyFilters =  parentExpressionVisitor.getNavPropertyPropertyFilters();
		this.lambdaAll =lambdaAll;
	}
	
	/**
	 * Checks if is all status.
	 *
	 * @return true, if is all status
	 */
	public boolean isAllStatus() {
		//TODO
		return allStatus;
	}

	/**
	 * Gets the condition string.
	 *
	 * @return the condition string
	 */
	public String getConditionString() {
		return conditionString;
	}

	/**
	 * Sets the condition string.
	 *
	 * @param conditionString the new condition string
	 */
	public void setConditionString(String conditionString) {
		this.conditionString = conditionString;

	}

	/**
	 * Gets the aggregate filter clause.
	 *
	 * @return the aggregate filter clause
	 */
	public String getAggregateFilterClause() {
		return this.isAllStatus() ? conditionString : "";
	}

	/**
	 * Gets the nav property property filters.
	 *
	 * @return the nav property property filters
	 */
	public TreeMap<String, NavPropertyPropertyFilter> getNavPropertyPropertyFilters() {
		return navPropertyPropertyFilters;
	}
	
	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public HashSet<RdfProperty> getProperties() {
		return properties;
	}
	
	/**
	 * Gets the navigation properties.
	 *
	 * @return the navigation properties
	 */
	public TreeMap<String, HashSet<RdfProperty>> getNavigationProperties() {
		return navigationProperties;
	}

	/**
	 * Gets the navigation property subjects.
	 *
	 * @return the navigation property subjects
	 */
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

	/**
	 * Gets the property clause.
	 *
	 * @return the property clause
	 */
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

	/**
	 * Gets the filter clause.
	 *
	 * @return the filter clause
	 */
	public String getFilterClause() {
		if (!allStatus) {
			if (conditionString != "")
				return "FILTER(" + conditionString + ")";
		}
		return "";
	}

	/**
	 * Put nav property property filter.
	 *
	 * @param sPath the s path
	 * @param navProperty the nav property
	 * @param property the property
	 * @param filter the filter
	 */
	private void putNavPropertyPropertyFilter(String sPath, RdfNavigationProperty navProperty, RdfProperty property,
			String filter) {
		NavPropertyPropertyFilter navPropertyPropertyFilter;
		if (!(navPropertyPropertyFilters.containsKey(sPath))) {
			navPropertyPropertyFilter = new NavPropertyPropertyFilter();
			navPropertyPropertyFilters.put(sPath, navPropertyPropertyFilter);
		} else {
			navPropertyPropertyFilter = navPropertyPropertyFilters.get(sPath);
		}
		TreeMap<String, PropertyFilter> propertyFilters = navPropertyPropertyFilter.getPropertyFilters();
		PropertyFilter propertyFilter;
		if (property != null) {
			if (!propertyFilters.containsKey(property.propertyName)) {
				propertyFilter = new PropertyFilter(sPath,property);
				propertyFilters.put(property.propertyName, propertyFilter);
			} else {
				propertyFilter = propertyFilters.get(property.propertyName);
			}
			if (filter != null && !filter.isEmpty())
				propertyFilter.getFilters().add(filter);
		}
		if (navProperty != null) {
			if (!propertyFilters.containsKey(navProperty.getNavigationPropertyName())) {
				propertyFilter = new PropertyFilter(sPath,navProperty);
				propertyFilters.put(navProperty.getNavigationPropertyName(), propertyFilter);
			} else {
				propertyFilter = propertyFilters.get(navProperty.getNavigationPropertyName());
			}
			if (filter != null && !filter.isEmpty())
				propertyFilter.getFilters().add(filter);
		}
	}

	/**
	 * Visit binary operator.
	 *
	 * @param operator the operator
	 * @param left the left
	 * @param right the right
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public Object visitBinaryOperator(BinaryOperatorKind operator, Object left, Object right)
			throws ExpressionVisitException, ODataApplicationException {
		if(this.lambdaAll ) {			
			String graphPattern = lambdaNavigationProperty.getGraphPattern(sPath);		
			return "NOT EXISTS {" + graphPattern   + this.primitivePropertyGraphPattern + " FILTER(" + inverseBinaryOperation(operator, left, right) + ")}";
		}else {
			return binaryOperation(operator, left, right);
		}
	}
	
	/**
	 * Binary operation.
	 *
	 * @param operator the operator
	 * @param left the left
	 * @param right the right
	 * @return the object
	 */
	private Object binaryOperation(BinaryOperatorKind operator, Object left, Object right) {
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
	
	/**
	 * Inverse binary operation.
	 *
	 * @param operator the operator
	 * @param left the left
	 * @param right the right
	 * @return the object
	 */
	private Object inverseBinaryOperation(BinaryOperatorKind operator, Object left, Object right) {
		String sparqlOperator = "";
		switch (operator) {
		case EQ:
			sparqlOperator = "!=";
			break;
		case NE:
			sparqlOperator = "=";
			break;
		case OR:
			sparqlOperator = "&&";
			break;
		case AND:
			sparqlOperator = "||";
			break;
		case GE:
			sparqlOperator = "<";
			break;
		case GT:
			sparqlOperator = "<=";
			break;
		case LE:
			sparqlOperator = ">";
			break;
		case LT:
			sparqlOperator = ">=";
			break;
		default:
			//Other operators are not supported for SQL Statements
			throw new UnsupportedOperationException("Unsupported operator: " + operator.toString());
		}
		//return the binary statement
		return "(" + left + " " + sparqlOperator + " " + right + ")";
	}
	
	/**
	 * Visit unary operator.
	 *
	 * @param operator the operator
	 * @param operand the operand
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
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

	/**
	 * Visit method call.
	 *
	 * @param methodCall the method call
	 * @param parameters the parameters
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Object visitMethodCall(MethodKind methodCall, List parameters)
			throws ExpressionVisitException, ODataApplicationException {
		String sparqlmethod = "";
		switch (methodCall) {
		case ENDSWITH:
			sparqlmethod = "STRENDS(xsd:string(" + parameters.get(0) + ")," + parameters.get(1) + ")";
			break;
		case INDEXOF:
			sparqlmethod = "";
			break;
		case STARTSWITH:
			sparqlmethod = "STRSTARTS(xsd:string(" + parameters.get(0) + ")," + parameters.get(1) + ")";
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
			if(parameters.get(0).toString().equals("\"\"") || parameters.get(1).toString().equals("\"\"") ){
				sparqlmethod="true";
			}else {
				if(parameters.get(0).toString().startsWith("?")) {
					sparqlmethod = "regex(str(" + parameters.get(0) + "),xsd:string(" + parameters.get(1) + "), \"i\")";
				}else {
					sparqlmethod = "regex(xsd:string(" + parameters.get(1) + "),str(" + parameters.get(0) + "), \"i\")";
				}
			}
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

	/**
	 * Visit lambda expression.
	 *
	 * @param lambdaFunction the lambda function
	 * @param lambdaVariable the lambda variable
	 * @param expression the expression
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public Object visitLambdaExpression(String lambdaFunction, String lambdaVariable, Expression expression)
			throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Visit literal.
	 *
	 * @param literal the literal
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
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
				return "\"" + literal.getText() + "\"^^xsd:dateTime";
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

	/**
	 * Visit member.
	 *
	 * @param member the member
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public Object visitMember(Member member) throws ExpressionVisitException, ODataApplicationException {
		String currentPath = this.sPath;
		RdfEntityType currentEntityType = this.entityType;
		if(currentPath=="") {
			//Refers to the root path, not an expanded property
			currentPath=currentEntityType.entityTypeName;
		}
		RdfNavigationProperty currentNavigationProperty = null;
		RdfNavigationProperty lambdaNavigationProperty = null;
		List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();
		UriResource lastUriResourcePart = uriResourceParts.get(uriResourceParts.size()-1);
		UriResourceKind lastUriResourcePartKind = lastUriResourcePart.getKind();
		for(UriResource memberPart: uriResourceParts) {
				String visitProperty;
				switch( memberPart.getKind()){
				case navigationProperty:
					if(lastUriResourcePartKind.equals(UriResourceKind.lambdaAll) ) {
						lambdaNavigationProperty =  currentEntityType.findNavigationProperty(memberPart.toString());		
						RdfEntityType lambdaEntityType = lambdaNavigationProperty.getRangeClass();	
						currentEntityType = lambdaEntityType;
					}else {
						currentPath= visitMemberNavigationPropertyPart(currentPath,currentEntityType,member, memberPart);
						currentNavigationProperty =  currentEntityType.findNavigationProperty(memberPart.toString());		
						currentEntityType = currentNavigationProperty.getRangeClass();
					}
					break;
				case primitiveProperty: 
					if(lambdaAll) {
						String memberProperty = memberPart.toString();		
						RdfProperty rdfProperty = currentEntityType.findProperty(memberProperty);
						visitProperty = "?" + currentPath + memberProperty + RdfConstants.PROPERTY_POSTFIX;
						
						primitivePropertyGraphPattern =  rdfProperty.getGraphPattern(currentPath);// "?" + currentPath +"_s <" + rdfProperty.getPropertyURI() + "> " + visitProperty ;
						visitProperty = castVariable(rdfProperty, visitProperty);
						return visitProperty ;
					}else {
						return visitMemberPrimitivePropertyPart(currentPath,currentEntityType,member, memberPart);
					}
				case lambdaVariable: 
					if(lambdaAll) {
						currentPath =  currentPath + this.lambdaNavigationProperty.getNavigationPropertyName();
					}
					break;
				case lambdaAll:
					UriResourceLambdaAll lambdaAllMemberPart = (UriResourceLambdaAll)memberPart;
					Expression lambdaAllExpression = lambdaAllMemberPart.getExpression();	
					SparqlExpressionVisitor lambdaAllExpressionVisitor = new SparqlExpressionVisitor(rdfModel, this.rdfModelToMetadata,
							currentEntityType, currentPath, lambdaNavigationProperty,this,true);
					Object lambdaAllVisitorResult;
					String allResult;
					lambdaAllVisitorResult = lambdaAllExpression.accept(lambdaAllExpressionVisitor);
					allResult = new String((String) lambdaAllVisitorResult);
					lambdaAllExpressionVisitor.setConditionString(allResult);
					return allResult;
				case lambdaAny: 
					UriResourceLambdaAny lambdaAnyMemberPart = (UriResourceLambdaAny)memberPart;
					Expression lambdaAnyExpression = lambdaAnyMemberPart.getExpression();	
					lambdaNavigationProperty = currentNavigationProperty;
					SparqlExpressionVisitor lambdaAnyExpressionVisitor = new SparqlExpressionVisitor(rdfModel, this.rdfModelToMetadata,
							currentEntityType, currentPath, lambdaNavigationProperty,this,false);
					Object lambdaAnyVisitorResult;
					String anyResult;
					lambdaAnyVisitorResult = lambdaAnyExpression.accept(lambdaAnyExpressionVisitor);
					anyResult = new String((String) lambdaAnyVisitorResult);
					lambdaAnyExpressionVisitor.setConditionString(anyResult);
					return anyResult;
				default:
				}
		 }
		return null;	
	}

	/**
	 * Visit member primitive property part.
	 *
	 * @param currentPath the current path
	 * @param currentEntityType the current entity type
	 * @param member the member
	 * @param memberPart the member part
	 * @return the string
	 */
	protected String visitMemberPrimitivePropertyPart(String currentPath,RdfEntityType currentEntityType,Member member, UriResource memberPart) {
		RdfProperty rdfProperty;
		String memberProperty = memberPart.toString();		
		rdfProperty = currentEntityType.findProperty(memberProperty);
		if (currentEntityType.isOperation()) {
			return "?" + rdfProperty.getVarName();
		} else {
			if (member instanceof UriResourceNavigation) {
				if (currentPath.isEmpty()) {
					currentPath = currentEntityType.entityTypeName;
				}
				currentPath += member.toString();
				putNavPropertyPropertyFilter(currentPath, null, null, null);

				return currentPath;
			} else if (RdfConstants.SUBJECT.equals(memberProperty)) {
				//TODO still need to add full path to disambiguate
				//rdfProperty = entityType.findProperty(edmProperty.getName());
				//properties.add(rdfProperty);
				if (!currentPath.isEmpty()) {
					//rdfProperty = entityType.findProperty(memberProperty);
					if (navigationProperties.containsKey(sPath)) {
						navigationProperties.get(sPath).add(rdfProperty);
					} else {
						HashSet<RdfProperty> properties = new HashSet<RdfProperty>();
						properties.add(rdfProperty);
						navigationProperties.put(sPath, properties);
					}
					//putNavPropertyPropertyFilter(sPath,null,rdfProperty,null); 
					String visitProperty = "?" + currentPath + SUBJECT_POSTFIX;
					currentPath = "";
					return visitProperty;
				} else {
					sPath = "";
					return "?" + entityType.entityTypeName + SUBJECT_POSTFIX;
				}
			} else if (rdfProperty.isFK()) {
				properties.add(rdfProperty);
				putNavPropertyPropertyFilter(currentPath, null, rdfProperty, null);
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
				putNavPropertyPropertyFilter(currentPath, null, rdfProperty, null);
				String visitProperty = null;
				if (currentPath.equals("")) {
					visitProperty = "?" + entityType.getEDMEntityTypeName() + memberProperty
							+ RdfConstants.PROPERTY_POSTFIX;
				} else {
					visitProperty = "?" + currentPath + memberProperty + RdfConstants.PROPERTY_POSTFIX;
				}
				visitProperty = castVariable(rdfProperty, visitProperty);
				sPath = "";
				return visitProperty;
			}
		}
	}
	
	/**
	 * Visit member navigation property part.
	 *
	 * @param currentPath the current path
	 * @param currentEntityType the current entity type
	 * @param member the member
	 * @param memberPart the member part
	 * @return the string
	 */
	protected String visitMemberNavigationPropertyPart(String currentPath,RdfEntityType currentEntityType,Member member, UriResource memberPart) {
		RdfNavigationProperty rdfNavigationProperty;
		String memberProperty = memberPart.toString();		
		rdfNavigationProperty = currentEntityType.findNavigationProperty(memberProperty);
		putNavPropertyPropertyFilter(currentPath, rdfNavigationProperty, null, null);
		String visitProperty =  currentPath + memberProperty;
		return visitProperty;
	}
	
	/**
	 * Cast variable.
	 *
	 * @param rdfProperty the rdf property
	 * @param visitProperty the visit property
	 * @return the string
	 */
	private String castVariable(RdfProperty rdfProperty, String visitProperty) {
		switch (rdfProperty.getPropertyTypeName()) {
		case RdfConstants.XSD_DATETIME:
			visitProperty = "STRDT(xsd:string(" + visitProperty + "),<"+ RdfConstants.XSD_DATETIME + ">)";
			break;
		case RdfConstants.XSD_DATE:
			visitProperty = "STRDT(xsd:string(" + visitProperty + "),<" + RdfConstants.XSD_DATE + ">)";
			break;
		default:
			break;
		}
		return visitProperty;
	}

	/**
	 * Visit alias.
	 *
	 * @param aliasName the alias name
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public Object visitAlias(String aliasName) throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Visit type literal.
	 *
	 * @param type the type
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public Object visitTypeLiteral(EdmType type) throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Visit lambda reference.
	 *
	 * @param variableName the variable name
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public Object visitLambdaReference(String variableName) throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Visit enum.
	 *
	 * @param type the type
	 * @param enumValues the enum values
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Object visitEnum(EdmEnumType type, List enumValues)
			throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Visit binary operator.
	 *
	 * @param operator the operator
	 * @param left the left
	 * @param right the right
	 * @return the object
	 * @throws ExpressionVisitException the expression visit exception
	 * @throws ODataApplicationException the o data application exception
	 */
	@Override
	public Object visitBinaryOperator(BinaryOperatorKind operator, Object left, List<Object> right)
			throws ExpressionVisitException, ODataApplicationException {
		// TODO Auto-generated method stub
		return null;
	}

}
