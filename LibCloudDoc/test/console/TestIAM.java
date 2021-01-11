package console;

import com.cloud.console.iam.MsGraph;

public class TestIAM {

	static void testAzure1 () {
		try {
			String endPoint = "https://login.microsoftonline.com/e45b00f2-be66-4d82-a9d5-362fc62f5c25/oauth2/token";
			String clientId = "e3c88a88-a888-4c2b-8437-d35d85da749c";
			String secret = ".485Xp02Q~_x70Wq5D6IZ~Q1B_mHf8DL._";
			String userName = "vsilva@convergeonemsd.onmicrosoft.com";
			String pwd = "Thenewcti1";
			String tok = MsGraph.getAccessTokenViaPassword(endPoint, clientId, secret, userName, pwd);
			System.out.println(tok);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void testAzure2 () {
		try {
			// https://docs.microsoft.com/en-us/azure/active-directory/azuread-dev/azure-ad-endpoint-comparison
			// AADSTS901002: The 'resource' request parameter is not supported.
			//String endPoint = "https://login.microsoftonline.com/008b5356-0a40-4980-ad65-f2eef1c7f975/oauth2/v2.0/token";
			String endPoint = "https://login.microsoftonline.com/008b5356-0a40-4980-ad65-f2eef1c7f975/oauth2/token";
			String clientId = "d2b512b4-222f-43dd-9a4b-568d449780a8";
			String secret = "oOdsJ-ae.6H9Y4s_9YScL66N._U~bEIj4v";
			String userName = "vsilva@convergeone.com";
			String pwd = "26Ortiz26";
			String tok = MsGraph.getAccessTokenViaPassword(endPoint, clientId, secret, userName, pwd);
			System.out.println(tok);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		testAzure1();
		//testAzure2();
	}

}
