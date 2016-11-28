package odata2sparql.v4.test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

import com.inova8.odata2sparql.RdfODataServlet;

public class TestServlet extends Mockito{
	   @Test
	    public void testServlet() throws Exception {
		   
	        HttpServletRequest request = mock(HttpServletRequest.class);       
	        HttpServletResponse response = mock(HttpServletResponse.class);   
	        request.setServerName("www.example.com");
	        request.setRequestURI("/NW/$metadata");
	        request.setQueryString("param1=value1&param");

	        new RdfODataServlet().service(request, response);

	        verify(request, atLeast(1)).getParameter("username"); // only if you want to verify username was called...

//	        assertTrue(FileUtils.readFileToString(new File("somefile.txt"), "UTF-8")
//	                   .contains("My Expected String"));
	    }
}
