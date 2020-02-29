package com.inova8.odata2sparql.SparqlBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;

public class SparqlChangeManager {
	private final static Logger log = LoggerFactory.getLogger(SparqlChangeManager.class);
	
	public static void clear(RdfEdmProvider rdfEdmProvider) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		StringBuilder clear = new StringBuilder();
		try {
			//clear.append("DELETE {GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">{?s ?p ?o}	}WHERE { GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">{ ?s ?p ?o }}");
			clear.append("DROP SILENT GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">");
			sparqlStatement= new SparqlStatement(clear.toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);
	}
	public static void archive(RdfEdmProvider rdfEdmProvider) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		StringBuilder archive = new StringBuilder();
		SimpleDateFormat formatter = new SimpleDateFormat("yyy-mm-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String serialNumber = "DT-" + formatter.format( new Date());
		try {
			archive.append("MOVE SILENT GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append("> TO GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append("/").append(serialNumber).append(">");
			sparqlStatement= new SparqlStatement(archive.toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);
	}
	public static void rollback(RdfEdmProvider rdfEdmProvider) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		StringBuilder rollback = new StringBuilder();
		try {
			rollback
			.append("DELETE{ \n")
			.append("	GRAPH ?addedGraph \n")
			.append("	{ \n")
			.append("		?addedSubject ?addedPredicate ?addedObject \n")
			.append("	} \n")
			.append("	GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append("> \n")
			.append("	{ \n")
			.append("		?change a <http://inova8.com/odata4sparql#Change> ; \n")
			.append("			<http://inova8.com/odata4sparql#created> ?created . \n")
			.append("		?change  <http://inova8.com/odata4sparql#added> ?added .  \n")  
			.append("		?added <http://inova8.com/odata4sparql#subject> ?addedSubject ; \n")
			.append("			<http://inova8.com/odata4sparql#predicate> ?addedPredicate; \n")
			.append("			<http://inova8.com/odata4sparql#object> ?addedObject  ; \n")
			.append("			<http://inova8.com/odata4sparql#graph> ?addedGraph . \n")
			.append("		?change  <http://inova8.com/odata4sparql#deleted> ?deleted . \n")   
			.append("		?deleted <http://inova8.com/odata4sparql#subject> ?deletedSubject ; \n")
			.append("			<http://inova8.com/odata4sparql#predicate> ?deletedPredicate; \n")
			.append("			<http://inova8.com/odata4sparql#object> ?deletedObject ; \n")
			.append("			<http://inova8.com/odata4sparql#graph> ?deletedGraph . \n")
			.append("	} \n")
			.append("} \n")
			.append("INSERT{ \n")
			.append("	GRAPH ?addedGraph \n")
			.append("	{ \n")
			.append("		?deletedSubject ?deletedPredicate ?deletedObject \n")
			.append("	} \n")
			.append("} \n")
			.append("where{ \n")
			.append("	select ?change ?created ?added ?addedSubject ?addedPredicate ?addedObject ?addedGraph  ?deleted ?deletedSubject ?deletedPredicate ?deletedObject ?deletedGraph  { \n");
			
			rollback
			.append("		graph <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">{ \n")
			.append("			?change a <http://inova8.com/odata4sparql#Change> ; \n")
			.append("				<http://inova8.com/odata4sparql#created> ?created . \n");
			
			rollback
			.append("			OPTIONAL{  \n")
			.append("				?change  <http://inova8.com/odata4sparql#added> ?added . \n")   
			.append("				?added <http://inova8.com/odata4sparql#subject> ?addedSubject ; \n")
			.append("					<http://inova8.com/odata4sparql#predicate> ?addedPredicate; \n")
			.append("					<http://inova8.com/odata4sparql#object> ?addedObject  ; \n")
			.append("					<http://inova8.com/odata4sparql#graph> ?addedGraph . \n")
			.append("			} \n")
			.append("			OPTIONAL{  \n")
			.append("				?change  <http://inova8.com/odata4sparql#deleted> ?deleted .  \n")  
			.append("				?deleted <http://inova8.com/odata4sparql#subject> ?deletedSubject ; \n")
			.append("					<http://inova8.com/odata4sparql#predicate> ?deletedPredicate ; \n")
			.append("					<http://inova8.com/odata4sparql#object> ?deletedObject ; \n")
			.append("					<http://inova8.com/odata4sparql#graph> ?deletedGraph . \n")
			.append("			} \n")
			.append("		} \n");
			
			rollback
			.append("	}	order by desc(?created) \n");
			
			rollback
			.append("} ");
			
			sparqlStatement= new SparqlStatement(rollback.toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);
	}
	public static void rollback(RdfEdmProvider rdfEdmProvider, String change) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		StringBuilder rollback = new StringBuilder();
		try {
			rollback
			.append("DELETE{ \n")
			.append("	GRAPH ?addedGraph \n")
			.append("	{ \n")
			.append("		?addedSubject ?addedPredicate ?addedObject \n")
			.append("	} \n")
			.append("	GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append("> \n")
			.append("	{ \n")
			.append("		?change a <http://inova8.com/odata4sparql#Change> ; \n")
			.append("			<http://inova8.com/odata4sparql#created> ?created . \n")
			.append("		?change  <http://inova8.com/odata4sparql#added> ?added .  \n")  
			.append("		?added <http://inova8.com/odata4sparql#subject> ?addedSubject ; \n")
			.append("			<http://inova8.com/odata4sparql#predicate> ?addedPredicate; \n")
			.append("			<http://inova8.com/odata4sparql#object> ?addedObject  ; \n")
			.append("			<http://inova8.com/odata4sparql#graph> ?addedGraph . \n")
			.append("		?change  <http://inova8.com/odata4sparql#deleted> ?deleted . \n")   
			.append("		?deleted <http://inova8.com/odata4sparql#subject> ?deletedSubject ; \n")
			.append("			<http://inova8.com/odata4sparql#predicate> ?deletedPredicate; \n")
			.append("			<http://inova8.com/odata4sparql#object> ?deletedObject ; \n")
			.append("			<http://inova8.com/odata4sparql#graph> ?deletedGraph . \n")
			.append("	} \n")
			.append("} \n")
			.append("INSERT{ \n")
			.append("	GRAPH ?addedGraph \n")
			.append("	{ \n")
			.append("		?deletedSubject ?deletedPredicate ?deletedObject \n")
			.append("	} \n")
			.append("} \n")
			.append("where{ \n")
			.append("	select ?change ?created ?added ?addedSubject ?addedPredicate ?addedObject ?addedGraph  ?deleted ?deletedSubject ?deletedPredicate ?deletedObject ?deletedGraph  { \n");
			
			rollback
			.append("		BIND( <").append(change).append("> as ?change \n");
			
			rollback
			.append("		graph <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">{ \n")
			.append("			?change a <http://inova8.com/odata4sparql#Change> ; \n")
			.append("				<http://inova8.com/odata4sparql#created> ?created . \n");
			
			rollback
			.append("			OPTIONAL{  \n")
			.append("				?change  <http://inova8.com/odata4sparql#added> ?added . \n")   
			.append("				?added <http://inova8.com/odata4sparql#subject> ?addedSubject ; \n")
			.append("					<http://inova8.com/odata4sparql#predicate> ?addedPredicate; \n")
			.append("					<http://inova8.com/odata4sparql#object> ?addedObject  ; \n")
			.append("					<http://inova8.com/odata4sparql#graph> ?addedGraph . \n")
			.append("			} \n")
			.append("			OPTIONAL{  \n")
			.append("				?change  <http://inova8.com/odata4sparql#deleted> ?deleted .  \n")  
			.append("				?deleted <http://inova8.com/odata4sparql#subject> ?deletedSubject ; \n")
			.append("					<http://inova8.com/odata4sparql#predicate> ?deletedPredicate ; \n")
			.append("					<http://inova8.com/odata4sparql#object> ?deletedObject ; \n")
			.append("					<http://inova8.com/odata4sparql#graph> ?deletedGraph . \n")
			.append("			} \n")
			.append("		} \n");
			
			rollback
			.append("	} \n");
			
			rollback
			.append("} ");
			
			sparqlStatement= new SparqlStatement(rollback.toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);
	}
	public static void rollback(RdfEdmProvider rdfEdmProvider, Date backTo) throws OData2SparqlException {
		SparqlStatement sparqlStatement = null;
		StringBuilder rollback = new StringBuilder();
		String backToString= new  SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(backTo);
		backToString  =  "\"" + backToString + "\"^^xsd:dateTime";
		try {
			rollback
			.append("DELETE{ \n")
			.append("	GRAPH ?addedGraph \n")
			.append("	{ \n")
			.append("		?addedSubject ?addedPredicate ?addedObject \n")
			.append("	} \n")
			.append("	GRAPH <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append("> \n")
			.append("	{ \n")
			.append("		?change a <http://inova8.com/odata4sparql#Change> ; \n")
			.append("			<http://inova8.com/odata4sparql#created> ?created . \n")
			.append("		?change  <http://inova8.com/odata4sparql#added> ?added .  \n")  
			.append("		?added <http://inova8.com/odata4sparql#subject> ?addedSubject ; \n")
			.append("			<http://inova8.com/odata4sparql#predicate> ?addedPredicate; \n")
			.append("			<http://inova8.com/odata4sparql#object> ?addedObject  ; \n")
			.append("			<http://inova8.com/odata4sparql#graph> ?addedGraph . \n")
			.append("		?change  <http://inova8.com/odata4sparql#deleted> ?deleted . \n")   
			.append("		?deleted <http://inova8.com/odata4sparql#subject> ?deletedSubject ; \n")
			.append("			<http://inova8.com/odata4sparql#predicate> ?deletedPredicate; \n")
			.append("			<http://inova8.com/odata4sparql#object> ?deletedObject ; \n")
			.append("			<http://inova8.com/odata4sparql#graph> ?deletedGraph . \n")
			.append("	} \n")
			.append("} \n")
			.append("INSERT{ \n")
			.append("	GRAPH ?addedGraph \n")
			.append("	{ \n")
			.append("		?deletedSubject ?deletedPredicate ?deletedObject \n")
			.append("	} \n")
			.append("} \n")
			.append("where{ \n")
			.append("	select ?change ?created ?added ?addedSubject ?addedPredicate ?addedObject ?addedGraph  ?deleted ?deletedSubject ?deletedPredicate ?deletedObject ?deletedGraph  { \n");
			
			rollback
			.append("		graph <").append(rdfEdmProvider.getRdfModel().getRdfRepository().getDataRepository().getChangeGraphUrl()).append(">{ \n")
			.append("			?change a <http://inova8.com/odata4sparql#Change> ; \n")
			.append("				<http://inova8.com/odata4sparql#created> ?created . \n");
			
			rollback
			.append("				FILTER (?created < ").append(backToString).append(". \n");
			
			rollback
			.append("			OPTIONAL{  \n")
			.append("				?change  <http://inova8.com/odata4sparql#added> ?added . \n")   
			.append("				?added <http://inova8.com/odata4sparql#subject> ?addedSubject ; \n")
			.append("					<http://inova8.com/odata4sparql#predicate> ?addedPredicate; \n")
			.append("					<http://inova8.com/odata4sparql#object> ?addedObject  ; \n")
			.append("					<http://inova8.com/odata4sparql#graph> ?addedGraph . \n")
			.append("			} \n")
			.append("			OPTIONAL{  \n")
			.append("				?change  <http://inova8.com/odata4sparql#deleted> ?deleted .  \n")  
			.append("				?deleted <http://inova8.com/odata4sparql#subject> ?deletedSubject ; \n")
			.append("					<http://inova8.com/odata4sparql#predicate> ?deletedPredicate ; \n")
			.append("					<http://inova8.com/odata4sparql#object> ?deletedObject ; \n")
			.append("					<http://inova8.com/odata4sparql#graph> ?deletedGraph . \n")
			.append("			} \n")
			.append("		} \n");
			
			rollback
			.append("	} order by desc(?created)\n");
			
			rollback
			.append("} ");
			
			sparqlStatement= new SparqlStatement(rollback.toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new OData2SparqlException(e.getMessage());
		}
		sparqlStatement.executeDelete(rdfEdmProvider);
	}
}
