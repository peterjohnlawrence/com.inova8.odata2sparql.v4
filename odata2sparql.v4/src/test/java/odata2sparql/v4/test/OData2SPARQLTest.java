package odata2sparql.v4.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.inova8.odata2sparql.RdfODataServlet;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockServletConfig;

@RunWith(value = Parameterized.class)
public class OData2SPARQLTest {
	private static RdfODataServlet servlet;
	private static MockServletContext servletContext;
	private static MockServletConfig servletConfig;
	private static MockHttpServletRequest request;
	private static MockHttpServletResponse response;
	@SuppressWarnings("unused")
	private String group;
	@SuppressWarnings("unused")
	private String subgroup;
	@SuppressWarnings("unused")
	private String test;
	private String skip;
	private String method;
	private String repository;
	private String requestURI;
	private String query;
	private String options;
	private String expected;
	@SuppressWarnings("unused")
	private String comments;
	@SuppressWarnings("unused")
	private String index;
	
	public OData2SPARQLTest(String group, String subgroup, String test, String skip, String method, String repository,
			String requestURI, String query, String options, String expected, String comments) {//,String index) {
		this.group = group;
		this.subgroup = subgroup;
		this.test = test;
		this.skip = skip;
		this.method = method;
		this.repository = repository;
		this.requestURI = requestURI;
		this.query = query;
		this.options = options;
		this.expected = expected;
		this.comments = comments;
		this.index = "index";
	}

	public static Collection<String[]> getTestData(String fileName) throws IOException {
		List<String[]> records = new ArrayList<String[]>();
		String record;
		BufferedReader file = new BufferedReader(new FileReader(fileName));
		Boolean titleRow = true;
		int maxlength = 11;
		int index = 0;
		while ((record = file.readLine()) != null) {
			// assume tab delimited
			index++;
			String fields[] = record.split("\t");
			if (titleRow)
				maxlength = fields.length;
			fields = copyOf(fields, maxlength);
			if (fields[6] != null && fields[6].startsWith("\""))
				fields[6] = fields[6].substring(1, fields[6].length() - 1);
			if (fields[7] != null && fields[7] != "") {
				fields[7] = fields[7].replace("\"\"", "\"");
				// strip " that Excel will sometimes add
				if (fields[7].startsWith("\""))
					fields[7] = fields[7].substring(1, fields[7].length() - 1);
			}
			if (fields[9] != null && fields[9] != "") {
				fields[9] = fields[9].replace("\"\"", "\"");
				// strip " that Excel will sometimes add
				if (fields[9].startsWith("\""))
					fields[9] = fields[9].substring(1, fields[9].length() - 1);
			} else {
				fields[9] = "?";
			}
			fields[10]= Integer.toString(index);
			if (!titleRow) {
				if (fields[0] != null)
					records.add(fields);
			} else {
				titleRow = false;
			}

		}
		file.close();
		return records;
	}

	private static String[] copyOf(String[] array, int length) {
		if (array.length == length) {
			return array;
		} else {
			String[] temp = array.clone();
			array = new String[length];
			System.arraycopy(temp, 0, array, 0, temp.length);
			return array;
		}
	}

	String removeExcessWhiteSpace(String input) {
		String output = input.trim().replaceAll("\\s+", " ");
		return output;
	}

	void assertStringEquals(String expected, String actual) {
		assertEquals(removeExcessWhiteSpace(expected), removeExcessWhiteSpace(actual));
	}

	//@Parameters(name = "{0}:{1}/{2}:URI={3}?{4}")
	@Parameters(name = "{10}:{3}/{2}:URI={5}/{6}?{7}")
	public static Collection<String[]> testData() throws IOException {
		//return getTestData("src/test/resources/CRUDTests.csv");
		return getTestData("src/test/resources/GetTests.csv");
	}

	@BeforeClass
	public static void setUp() throws Exception {
		servlet = new RdfODataServlet();
		servletContext = new MockServletContext();
		servletContext.addInitParameter(ServletContext.TEMPDIR, "C:\\");
		servletContext.addInitParameter("configFolder", "/var/opt/inova8/odata2sparql");
		servletContext.addInitParameter("repositoryFolder", "V4");
		servletContext.addInitParameter("repositoryUrl", "");
		servletConfig = new MockServletConfig(servletContext);
		servlet.init(servletConfig);
	}

	@Test
	public void serviceRequest() throws ServletException, IOException {
		request = new MockHttpServletRequest(servletContext);
		request.setServerName("localhost:8080/odata2sparql/");
		if (!(repository == null || repository.isEmpty()) ) {     
			if (skip.isEmpty()) {
				request.setMethod(method);
				request.setPathInfo("/" + repository);
				request.setRequestURI(repository + "/" + requestURI);
				switch (method) {
				case "GET":
					request.setQueryString(query + options);
					break;
				case "POST":
					request.setContent(query.getBytes());
					request.setContentType(options);
					request.addHeader("Prefer", "odata.track-changes");
					break;
				case "PUT":
					request.setContent(query.getBytes());
					request.setContentType(options);
					break;
				case "DELETE":				
					break;
				case "PATCH":
					request.setContent(query.getBytes());
					request.setContentType(options);
					break;
				default:
				}
				response = new MockHttpServletResponse();
				servlet.service(request, response);
				assertStringEquals(expected.equals("?")?"":expected, response.getContentAsString());
				//response.reset();
				//response.flushBuffer(); 
 
			}
		}
	}
}
