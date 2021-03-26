/*
 * inova8 2020
 */
package com.inova8.odata2sparql;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 * The Class OData2SPARQLServer.
 */
public class OData2SPARQLServer {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args)  throws Exception {
        // Create a basic jetty server object that will listen on port 8080.
        Server server = new Server(8080);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(RdfODataServlet.class, "/odata2sparql.v4/*");
        server.start();
        server.join();
	}
}
