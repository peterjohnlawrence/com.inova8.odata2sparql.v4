package com.inova8.odata2sparql;

import java.io.IOException;
import java.util.ArrayList;

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

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProviders;
import com.inova8.odata2sparql.SparqlProcessor.SparqlEntityCollectionProcessor;

public class RdfODataServlet extends HttpServlet {
	 private static final long serialVersionUID = 1L;
	private final Log log = LogFactory.getLog(RdfODataServlet.class);
	static private final RdfEdmProviders rdfEdmProviders = new RdfEdmProviders();

	  protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
	    try {
	      // create odata handler and configure it with CsdlEdmProvider and Processor
	      OData odata = OData.newInstance();
	      ServiceMetadata edm = odata.createServiceMetadata(rdfEdmProviders.getRdfEdmProvider("4.0", "NW"), new ArrayList<EdmxReference>());
	      ODataHttpHandler handler = odata.createHandler(edm);
	      handler.register(new SparqlEntityCollectionProcessor());

	      // let the handler do the work
	      handler.process(req, resp);
	    } catch (RuntimeException | OData2SparqlException e) {
	    	log.error("Server Error occurred in ExampleServlet", e);
	      throw new ServletException(e);
	    }
	  }
}
