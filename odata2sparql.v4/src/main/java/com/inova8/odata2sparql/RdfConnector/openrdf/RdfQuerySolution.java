package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.openrdf.model.Literal;
import org.openrdf.query.BindingSet;

public class RdfQuerySolution {
	private final BindingSet querySolution;
	RdfQuerySolution(BindingSet querySolution) {
		this.querySolution =querySolution;
	}
	public RdfNode getRdfNode(String string) {
		if(querySolution.getValue(string)!=null){
			return new RdfNode(querySolution.getValue(string));
		}else{
			return null;
		}
	}
	public RdfLiteral getRdfLiteral(String string) {	
		return new RdfLiteral((Literal)querySolution.getValue(string));
	}
}

