/*
 * inova8 2020
 */
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

/**
 * The Class OData2SPARQLTest.
 */
@RunWith(value = Parameterized.class)
public class OData2SPARQLTest {
	
	/** The servlet. */
	private static RdfODataServlet servlet;
	
	/** The servlet context. */
	private static MockServletContext servletContext;
	
	/** The servlet config. */
	private static MockServletConfig servletConfig;
	
	/** The request. */
	private static MockHttpServletRequest request;
	
	/** The response. */
	private static MockHttpServletResponse response;
	
	/** The group. */
	@SuppressWarnings("unused")
	private String group;
	
	/** The subgroup. */
	@SuppressWarnings("unused")
	private String subgroup;
	
	/** The test. */
	@SuppressWarnings("unused")
	private String test;
	
	/** The skip. */
	private String skip;
	
	/** The method. */
	private String method;
	
	/** The repository. */
	private String repository;
	
	/** The request URI. */
	private String requestURI;
	
	/** The query. */
	private String query;
	
	/** The options. */
	private String options;
	
	/** The expected. */
	private String expected;
	
	/** The comments. */
	@SuppressWarnings("unused")
	private String comments;
	
	/** The index. */
	@SuppressWarnings("unused")
	private String index;
	
	/**
	 * Instantiates a new o data 2 SPARQL test.
	 *
	 * @param group the group
	 * @param subgroup the subgroup
	 * @param test the test
	 * @param skip the skip
	 * @param method the method
	 * @param repository the repository
	 * @param requestURI the request URI
	 * @param query the query
	 * @param options the options
	 * @param expected the expected
	 * @param comments the comments
	 */
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

	/**
	 * Gets the test data.
	 *
	 * @param fileName the file name
	 * @return the test data
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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

	/**
	 * Copy of.
	 *
	 * @param array the array
	 * @param length the length
	 * @return the string[]
	 */
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

	/**
	 * Removes the excess white space.
	 *
	 * @param input the input
	 * @return the string
	 */
	String removeExcessWhiteSpace(String input) {
		String output = input.trim().replaceAll("\\s+", " ");
		return output;
	}

	/**
	 * Assert string equals.
	 *
	 * @param expected the expected
	 * @param actual the actual
	 */
	void assertStringEquals(String expected, String actual) {
		assertEquals(removeExcessWhiteSpace(expected), removeExcessWhiteSpace(actual));
	}

	/**
	 * Test data.
	 *
	 * @return the collection
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	//@Parameters(name = "{0}:{1}/{2}:URI={3}?{4}")
	@Parameters(name = "{10}:{3}/{2}:URI={5}/{6}?{7}")
	public static Collection<String[]> testData() throws IOException {
		//return getTestData("src/test/resources/CRUDTests.csv");
		return getTestData("src/test/resources/GetTests.csv");
	}

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		servlet = new RdfODataServlet();
		servletContext = new MockServletContext();
		servletContext.addInitParameter(ServletContext.TEMPDIR, "C:\\");
		servletContext.addInitParameter("configFolder", "/var/opt/inova8/odata2sparql");
		servletContext.addInitParameter("repositoryFolder", ".default");
		servletContext.addInitParameter("repositoryUrl", "");
		servletConfig = new MockServletConfig(servletContext);
		servlet.init(servletConfig);
	}

	/**
	 * Service request.
	 *
	 * @throws ServletException the servlet exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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
