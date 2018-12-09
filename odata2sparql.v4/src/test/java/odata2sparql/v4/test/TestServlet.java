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
public class TestServlet {
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
	private String repository;
	private String requestURI;
	private String query;
	private String options;
	private String expected;
	@SuppressWarnings("unused")
	private String comments;

	public TestServlet(String group, String subgroup, String test, String skip, String repository, String requestURI,
			String query, String options, String expected, String comments) {
		this.group = group;
		this.subgroup = subgroup;
		this.test = test;
		this.skip = skip;
		this.repository = repository;
		this.requestURI = requestURI;
		this.query = query;
		this.options = options;
		this.expected = expected;
		this.comments = comments;
	}

	public static Collection<String[]> getTestData(String fileName) throws IOException {
		List<String[]> records = new ArrayList<String[]>();
		String record;
		BufferedReader file = new BufferedReader(new FileReader(fileName));
		Boolean titleRow = true;
		int maxlength = 10;
		while ((record = file.readLine()) != null) {
			// assume tab delimited
			String fields[] = record.split("\t");
			if (titleRow)
				maxlength = fields.length;
			fields = copyOf(fields, maxlength);
			//			// strip " that Excel will sometimes add
			//			if(fields[3].startsWith("\"")) fields[3] =fields[3].substring(1, fields[3].length() - 1);
			//			if(fields[4].equals("?")){
			//				fields[4]=null;
			//			}else if(fields[4].charAt(0)=='"'){
			//				// strip " that Excel will sometimes add
			//				if(fields[4].startsWith("\"")) fields[4]=fields[4].substring(1, fields[4].length() - 1);
			//			}else{
			//				
			//			}
			//			// replace the expected JSON embedded "" as added by Excel
			if (fields[5] != null && fields[5].startsWith("\""))
				fields[5] = fields[5].substring(1, fields[5].length() - 1);
			if (fields[6] != null && fields[6].startsWith("\""))
				fields[6] = fields[6].substring(1, fields[6].length() - 1);
			if (fields[8] != null && fields[8] != "") {
				fields[8] = fields[8].replace("\"\"", "\"");
				// strip " that Excel will sometimes add
				if (fields[8].startsWith("\""))
					fields[8] = fields[8].substring(1, fields[8].length() - 1);
			} else {
				fields[8] = "?";
			}
			if (!titleRow) {
				if (fields[0] != null ) records.add(fields);
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
	@Parameters(name = "{2}:URI={5}?{6}")
	public static Collection<String[]> testData() throws IOException {
		return getTestData("src/test/resources/TestServlet.txt");
	}

	@BeforeClass
	public static void setUp() throws Exception {
		servlet = new RdfODataServlet();
		servletContext = new MockServletContext();
		servletConfig = new MockServletConfig(servletContext);
		request = new MockHttpServletRequest(servletContext);
		servletContext.addInitParameter(ServletContext.TEMPDIR, "C:\\");
		servletContext.addInitParameter("configFolder", "/var/opt/inova8/odata2sparql");
		servletContext.addInitParameter("repositoryFolder", "V4");
		servletContext.addInitParameter("repositoryUrl", "");
		request.setServerName("localhost:8080/odata2sparql/");
		request.setMethod("GET");
		servlet.init(servletConfig);
	}

	@Test
	public void serviceRequest() throws ServletException, IOException {
		if (!(repository == null || repository.isEmpty() || requestURI == null || requestURI.isEmpty())) {
			if (skip.isEmpty()) {
				request.setPathInfo("/" + repository);
				request.setRequestURI(repository + "/" + requestURI);
				request.setQueryString(query + options);
				response = new MockHttpServletResponse();
				servlet.service(request, response);
				assertStringEquals(expected, response.getContentAsString());
				//response.reset();
				//response.flushBuffer();

			}
		}
	}
}
