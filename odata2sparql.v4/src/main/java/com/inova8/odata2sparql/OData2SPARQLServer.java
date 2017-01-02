package com.inova8.odata2sparql;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

public class OData2SPARQLServer {

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
