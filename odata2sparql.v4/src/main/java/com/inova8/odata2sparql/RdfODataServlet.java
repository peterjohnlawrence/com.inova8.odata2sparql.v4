package com.inova8.odata2sparql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.EscapeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.Processor.EntityCollectionProcessor;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProviders;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;
import com.inova8.odata2sparql.SparqlProcessor.SparqlBatchProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlComplexProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlDefaultProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlEntityProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlPrimitiveValueProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlReferenceCollectionProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlReferenceProcessor;

public class RdfODataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger log = LoggerFactory.getLogger(RdfODataServlet.class);
	static private RdfEdmProviders rdfEdmProviders = null;
	static String version = null;
	static String buildDate = null;

	protected void service(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			if(rdfEdmProviders==null)rdfEdmProviders=getRdfEdmProviders();
			if (req.getPathInfo() != null && (!req.getPathInfo().equals("/"))) {
				String service = req.getPathInfo().split("/")[1];
				if (service.equalsIgnoreCase(RdfConstants.RESET)) {
					log.info(RdfConstants.RESET + " requested: " + req.getPathInfo().split("/")[2]);
					rdfEdmProviders.reset(req.getPathInfo().split("/")[2]);
					simpleResponse(req, resp, RdfConstants.RESET + ": " + req.getPathInfo().split("/")[2]);
				} else if (service.equalsIgnoreCase(RdfConstants.RELOAD)) {
					log.info(RdfConstants.RELOAD + " requested");
					rdfEdmProviders.reload();
					simpleResponse(req, resp, RdfConstants.RELOAD + " complete");
				} else if (service.equalsIgnoreCase(RdfConstants.LOGS)) {
					//TODO #106
					htmlResponse(req, resp, "/WEB-INF/classes/logs/odata2sparql.v4.log.html");
				} else if (service.equalsIgnoreCase(RdfConstants.CHANGES)) {
					log.info(RdfConstants.CHANGES + " requested");
					//$delta/<service>/<option>, option = clear, rollback, commit
					String segments[] = req.getRequestURI().split("/");
					rdfEdmProviders.changes(segments[segments.length-2],segments[segments.length-1]); 
					simpleResponse(req, resp, RdfConstants.CHANGES + " on " + segments[segments.length-2] + " " + segments[segments.length-1] + "'ed");
				} else {
					//Find provider matching service name			
					RdfEdmProvider rdfEdmProvider = rdfEdmProviders.getRdfEdmProvider(service);
					// create odata handler and configure it with CsdlEdmProvider and Processor
					if (rdfEdmProvider != null) {
						OData odata = OData.newInstance();
						ServiceMetadata edm = odata.createServiceMetadata(rdfEdmProvider,
								new ArrayList<EdmxReference>());
						ODataHttpHandler handler = odata.createHandler(edm);
						//Reserve first parameter for either the service name or a$RESET. $RELOAD
						handler.setSplit(1);
						handler.register(new EntityCollectionProcessor(rdfEdmProvider));
						handler.register(new SparqlEntityProcessor(rdfEdmProvider));
						handler.register(new SparqlPrimitiveValueProcessor(rdfEdmProvider));
						handler.register(new SparqlComplexProcessor(rdfEdmProvider));
						handler.register(new SparqlDefaultProcessor());
						handler.register(new SparqlReferenceCollectionProcessor(rdfEdmProvider));
						handler.register(new SparqlReferenceProcessor(rdfEdmProvider));
						handler.register(new SparqlBatchProcessor(rdfEdmProvider));
						//handler.register(new SparqlErrorProcessor());
						log.info(req.getMethod() + ": " + req.getPathInfo() + " Query: " + req.getQueryString());
						if (!req.getMethod().equals("OPTIONS")) {
							//Required to satisfy OpenUI5 V1.54.3 and above
							resp.addHeader("Access-Control-Expose-Headers", "DataServiceVersion,OData-Version");
							handler.process(req, resp);
						} else {
							optionsResponse(resp);
						}
					} else {
						throw new OData2SparqlException("No service matching " + service + " found");
					}
				}
			} else {
				htmlTemplateResponse(req, resp, "/templates/index.vm");
			}
		} catch (RuntimeException | OData2SparqlException e) {
			log.error("Server Error occurred in RdfODataServlet", e);
			//throw new ServletException(e);
			resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
		}
	}

	private synchronized RdfEdmProviders getRdfEdmProviders() {
		if(rdfEdmProviders==null) {
			ServletContext servletContext = getServletContext();
			File repositoryDir = (File) servletContext.getAttribute(ServletContext.TEMPDIR);
			rdfEdmProviders=  new RdfEdmProviders(servletContext.getInitParameter("configFolder"),
					servletContext.getInitParameter("repositoryFolder"), this.getInitParameter("repositoryUrl"),
					repositoryDir.getAbsolutePath());
			//Set to UTC so string date objects without assigned timnezone are assumed to be UTC.
			//TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		}
		return rdfEdmProviders;
	}
	private void optionsResponse(final HttpServletResponse resp) {
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Access-Control-Allow-Headers",
				"Content-Type, Content-Length, Authorization, Accept, X-Requested-With, X-CSRF-Token, odata-maxversion, odata-version,mime-version");
		resp.addHeader("Access-Control-Allow-Methods", "PUT, POST, GET, DELETE, OPTIONS");
		resp.addHeader("Access-Control-Expose-Headers", "DataServiceVersion,OData-Version");
		resp.addHeader("Access-Control-Max-Age", "86400");
		resp.addHeader("OData-Version", "4.0");
		resp.addHeader("OData-MaxVersion", "4.0");
		resp.setStatus(200);
	}

	private void htmlResponse(HttpServletRequest req, HttpServletResponse resp, String textResponse)
			throws IOException {
		try {
			InputStream in = getServletContext().getResourceAsStream(textResponse);
			String contents = IOUtils.toString(in, Charset.defaultCharset());
			simpleResponse(req, resp, contents);
			in.close();
		} catch (Exception e) {
			simpleResponse(req, resp, "odata2sparql.v4");
		}
	}
	@SuppressWarnings("unused")
	private void readVersion()
			throws IOException {
		try {
			InputStream in = getServletContext().getResourceAsStream( "/WEB-INF/classes/version.txt");
			if( in != null) {
			String contents = IOUtils.toString(in, Charset.defaultCharset());
			version = contents.split("\r\n")[0].split("=")[1];
			buildDate = contents.split("\r\n")[1].split("=")[1];
			in.close();
			}else {
				log.error("Cannot read version information");
			}
		} catch (Exception e) {
			log.error("Cannot read version information", e);
		}
	}
	private void htmlTemplateResponse(HttpServletRequest req, HttpServletResponse resp, String textTemplate)
			throws IOException {
		try {
			Properties props = setTemplateLocation();
			Velocity.init(props);		
			
			Template uiTemplate = null;

			uiTemplate = Velocity.getTemplate(textTemplate, Charset.defaultCharset().displayName());
			StringWriter uiWriter = new StringWriter();
			VelocityContext uiContext = new VelocityContext();
			uiContext.put("esc", new EscapeTool());
			uiContext.put("repositories", new ArrayList<RdfRepository>(rdfEdmProviders.getRepositories().getRdfRepositories().values()));
			uiContext.put("version",RdfODataServlet.version);
			uiContext.put("buildDate",RdfODataServlet.buildDate);
			uiContext.put("repositoryLocation",rdfEdmProviders.getRepositories().getLocalRepositoryManagerDirectory());
			uiContext.put("modelsLocation",rdfEdmProviders.getRepositories().getLocalRepositoryManagerModel());
			
			uiTemplate.merge(uiContext, uiWriter);

			simpleResponse(req, resp, uiWriter.toString());


		} catch (Exception e) {
			simpleResponse(req, resp, "odata2sparql.v4");
		}
	}
	private static Properties setTemplateLocation() {
		Properties props = new Properties();
		String sPath = getWorkingPath();
		props.put("file.resource.loader.path", sPath);
		props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
		return props;
	}
	private static String getWorkingPath() {
		URL main = RdfODataServlet.class.getResource("RdfODataServlet.class");
		File file = new File(main.getPath());
		Path path = Paths.get(file.getPath());
		String sPath = path.getParent().getParent().getParent().getParent().toString();
		return sPath;
	}
	private void simpleResponse(final HttpServletRequest req, final HttpServletResponse resp, String textResponse)
			throws IOException {
		PrintWriter out = resp.getWriter();
		out.println(textResponse);
		//resp.getOutputStream().println(textResponse);
	}
}
