package com.inova8.odata2sparql;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProviders;
import com.inova8.odata2sparql.SparqlProcessor.SparqlEntityCollectionProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlEntityProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlPrimitiveProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlPrimitiveValueProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlReferenceCollectionProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlReferenceProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlServiceDocumentProcessor;

public class RdfODataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Log log = LogFactory.getLog(RdfODataServlet.class);
	static private RdfEdmProviders rdfEdmProviders=null;// = new RdfEdmProviders(RdfODataServlet.getInitParamter());


	protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
			IOException {
		try {
			if(rdfEdmProviders==null){
				 rdfEdmProviders = new RdfEdmProviders(this.getInitParameter("repositoryFolder"),this.getInitParameter("repositoryUrl"));
			}
			String service = req.getPathInfo().split("/")[1];
			if (service.equals(RdfConstants.RESET)) {
				log.info(RdfConstants.RESET +  " requested: " + req.getPathInfo().split("/")[2]);
				rdfEdmProviders.reset(req.getPathInfo().split("/")[2]);
				simpleResponse(req,resp,RdfConstants.RESET +  ": " + req.getPathInfo().split("/")[2] );
			} else if (service.equals(RdfConstants.RELOAD )) {
				log.info(RdfConstants.RELOAD +  " requested");
				rdfEdmProviders.reload();
				simpleResponse(req,resp,RdfConstants.RELOAD );
			} else {
				//Find provider matching service name			
				RdfEdmProvider rdfEdmProvider = rdfEdmProviders.getRdfEdmProvider(service);
				// create odata handler and configure it with CsdlEdmProvider and Processor
				if (rdfEdmProvider != null) {
					OData odata = OData.newInstance();
					ServiceMetadata edm = odata.createServiceMetadata(rdfEdmProvider, new ArrayList<EdmxReference>());
					ODataHttpHandler handler = odata.createHandler(edm);
					//Reserve first parameter for either the service name or a$RESET. $RELOAD
					handler.setSplit(1);
					handler.register(new SparqlEntityCollectionProcessor(rdfEdmProvider));
					handler.register(new SparqlEntityProcessor(rdfEdmProvider));
					handler.register(new SparqlPrimitiveValueProcessor(rdfEdmProvider));
					handler.register(new SparqlServiceDocumentProcessor());
					handler.register(new SparqlReferenceCollectionProcessor(rdfEdmProvider));
					handler.register(new SparqlReferenceProcessor(rdfEdmProvider));
					log.info(req.getMethod() + ": "+ req.getPathInfo()+" Query: "+req.getQueryString());
					// let the handler do the work
					handler.process(req, resp);
				} else {
					throw new OData2SparqlException("No service matching " + service + " found");
				}
			}
		} catch (RuntimeException | OData2SparqlException e) {
			log.error("Server Error occurred in ExampleServlet", e);
			throw new ServletException(e);
		}
	}
	private void simpleResponse(final HttpServletRequest req, final HttpServletResponse resp, String textResponce) throws IOException{
		  Scanner scanner = new Scanner(req.getInputStream());
		  StringBuilder sb = new StringBuilder();
		  while (scanner.hasNextLine()) {
		    sb.append(scanner.nextLine());
		  }
		  resp.getOutputStream().println(textResponce);
		
	}
}
