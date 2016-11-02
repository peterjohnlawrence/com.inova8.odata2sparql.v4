package odata2sparql.v4.test;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.runner.RunWith;

import com.eclipsesource.restfuse.Destination;
import com.eclipsesource.restfuse.HttpJUnitRunner;
import com.eclipsesource.restfuse.MediaType;
import com.eclipsesource.restfuse.Method;
import com.eclipsesource.restfuse.Response;
import com.eclipsesource.restfuse.annotation.Context;
import com.eclipsesource.restfuse.annotation.HttpTest;
@RunWith( HttpJUnitRunner.class )
public class MainTest {
	String removeExcessWhiteSpace(String input) {
		String output = input.trim().replaceAll("\\s+", " ");
		return output;
	}

	void assertStringEquals(String expected, String actual) {
		assertEquals(removeExcessWhiteSpace(expected), removeExcessWhiteSpace(actual));
	}

	@Rule
	//public Destination destination = new Destination(this,"http://services.odata.org/V3/Northwind/Northwind.svc");
	public Destination destination = new Destination(this,"http://localhost:8080/odata2sparql.v4");
	@Context
	private Response response;

	@HttpTest(
			method = Method.GET,
			headers = {@Header(name = "Accept", value = "application/xml") }  , 
			path = "/$metadata")
//	public void checkRestfuseOnlineStatus() {
//		com.eclipsesource.restfuse.Assert.assertOk(response);
//	}
	public void checkMetadata() {
		String body = response.getBody();
	//	com.eclipsesource.restfuse.Assert.assertOk( response );
		assertEquals( MediaType.TEXT_PLAIN, response.getType() );
		assertEquals( "HelloWorld", body );

	}



}
