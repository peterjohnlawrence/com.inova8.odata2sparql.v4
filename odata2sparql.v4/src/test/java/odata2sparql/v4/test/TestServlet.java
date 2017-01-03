package odata2sparql.v4.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.inova8.odata2sparql.RdfODataServlet;

@RunWith(value = Parameterized.class)
public class TestServlet {
	private RdfODataServlet servlet;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private String fIndex;
	private String fURI;
	private String fGroup;
	private String fRequestURI;
	private String fRequestQuery=null;
	private String fPass;
	private String fExpected;

	public TestServlet(String fIndex, String fURI, String fGroup, String fRequestURI, String fRequestQuery, String fPass, String fExpected) {
		this.fIndex = fIndex;
		this.fURI = fURI;
		this.fGroup = fGroup;
		this.fRequestURI = fRequestURI;
		this.fRequestQuery = fRequestQuery;
		this.fPass = fPass;
		this.fExpected = fExpected;
	}

	public static Collection<String[]> getTestData(String fileName) throws IOException {
		List<String[]> records = new ArrayList<String[]>();
		String record;
		BufferedReader file = new BufferedReader(new FileReader(fileName));
		while ((record = file.readLine()) != null) {
			// assume tab deliminated
			
			String fields[] = record.split("\t");
			// strip " that Excel will sometimes add
			if(fields[3].startsWith("\"")) fields[3] =fields[3].substring(1, fields[3].length() - 1);
			if(fields[4].equals("?")){
				fields[4]=null;
			}else if(fields[4].charAt(0)=='"'){
				// strip " that Excel will sometimes add
				if(fields[4].startsWith("\"")) fields[4]=fields[4].substring(1, fields[4].length() - 1);
			}else{
				
			}
			// replace the expected JSON embedded "" as added by Excel
			fields[6]=fields[6].replace("\"\"", "\"");
			// strip " that Excel will sometimes add
			if(fields[6].startsWith("\"")) fields[6] =fields[6].substring(1, fields[6].length() - 1);
			records.add(fields);
		}
		file.close();
		return records;
	}

	String removeExcessWhiteSpace(String input) {
		String output = input.trim().replaceAll("\\s+", " ");
		return output;
	}

	void assertStringEquals(String expected, String actual) {
		assertEquals(removeExcessWhiteSpace(expected), removeExcessWhiteSpace(actual));
	}

	@Parameters( name = "{0}:{1}/{2}:URI={3}?{4}" )
	public static Collection<String[]> testData() throws IOException {
		return getTestData("src/test/resources/TestServlet.txt");
	}

	@Before
	public void setUp() {
		servlet = new RdfODataServlet();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		// request.addParameter("username", "scott");
		// request.addParameter("password", "tiger");
		request.setServerName("www.inova8.com");
		request.setMethod("GET");
	}

	@Test
	public void serviceRequest() throws ServletException, IOException {

		request.setPathInfo("/NW"+fRequestURI);
		request.setRequestURI("/NW"+fRequestURI);
		if(fRequestQuery!=null) request.setQueryString(fRequestQuery);
		System.out.println(fURI+"/"+fGroup+":URI=" + fRequestURI+"?" + fRequestQuery);

		servlet.service(request, response);
		assertStringEquals(fExpected, response.getContentAsString());

	}
}
