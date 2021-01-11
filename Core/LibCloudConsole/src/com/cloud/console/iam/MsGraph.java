package com.cloud.console.iam;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.w3.OAuth2;
import com.cloud.core.w3.WebClient;
import com.cloud.core.w3.OAuth2.GrantType;

public class MsGraph {

	// https://portal.azure.com/
	static final String BASE_URL = "https://graph.microsoft.com/";
	
	/**
	 * https://portal.azure.com/
	 * @param endPoint https://login.microsoftonline.com/[TENANT-ID]
	 * @param clientId Application client id.
	 * @param secret Application secret.
	 * @return Access token
	 * @throws Exception on I/O errors.
	 */
	public static String getAccessToken (final String endPoint, final String clientId, final String secret) throws IOException, JSONException {
		OAuth2.OAuth2AuthenticationResult res = OAuth2.authenticate(GrantType.client_credentials, clientId, BASE_URL, secret, null, null, endPoint);
		return res.getAccessToken();
	}

	public static String getAccessTokenViaPassword (final String endPoint, final String clientId, final String secret, final String user, final String pwd) throws IOException, JSONException {
		OAuth2.OAuth2AuthenticationResult res = OAuth2.authenticate(GrantType.password, clientId, BASE_URL, secret, user, pwd, endPoint);
		return res.getAccessToken();
	}
	
	/**
	 * https://docs.microsoft.com/en-us/graph/api/organization-get?view=graph-rest-1.0&tabs=http#code-try-1
	 * @param token Access token.
	 * @return The value from {"@odata.context":"https://graph.microsoft.com/v1.0/$metadata#organization","value":[{"id":"e45b00f2-be66-4d82-a9d5-362fc62f5c25","deletedDateTime":null,"businessPhones":["919-582-6479"],"city":"Bloomington","country":null,"countryLetterCode":"US","createdDateTime":"2019-05-09T19:10:15Z","directorySizeQuota":{"used":166,"total":50000},"displayName":"ConvergeOne","marketingNotificationEmails":[],"onPremisesLastSyncDateTime":null,"onPremisesSyncEnabled":null,"postalCode":"55437","preferredLanguage":"en","privacyProfile":null,"securityComplianceNotificationMails":[],"securityComplianceNotificationPhones":[],"state":"MN","street":"10900 Nesbitt Avenue South","technicalNotificationMails":["rcole@convergeone.com"],"tenantType":"AAD","assignedPlans":[{"assignedDateTime":"2020-05-09T05:39:16Z","capabilityStatus":"Enabled","service":"MicrosoftFormsProTest","servicePlanId":"97f29a83-1a20-44ff-bf48-5e4ad11f3e51"},{"assignedDateTime":"2020-05-09T05:39:16Z","capabilityStatus":"Enabled","service":"DYN365AISERVICEINSIGHTS","servicePlanId":"1412cdc1-d593-4ad1-9050-40c30ad0b023"},{"assignedDateTime":"2019-05-09T19:48:56Z","capabilityStatus":"Enabled","service":"ProcessSimple","servicePlanId":"50e68c76-46c6-4674-81f9-75456511b170"},{"assignedDateTime":"2019-05-09T19:48:56Z","capabilityStatus":"Enabled","service":"CRM","servicePlanId":"17ab22cd-a0b3-4536-910a-cb6eb12696c0"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"SharePoint","servicePlanId":"e95bec33-7c88-4a70-8e19-b10bd9d0c014"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"exchange","servicePlanId":"113feb6c-3fe4-4440-bddc-54d774bf0318"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"MicrosoftOffice","servicePlanId":"fafd7243-e5c1-4a3a-9e40-495efcb1d3c3"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"SharePoint","servicePlanId":"5dbe027f-2339-4123-9542-606e4d348a72"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"ProcessSimple","servicePlanId":"b650d915-9886-424b-a08d-633cede56f57"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"PowerAppsService","servicePlanId":"0b03f40b-c404-40c3-8651-2aceb74365fa"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"CRM","servicePlanId":"d56f3deb-50d8-465a-bedb-f079817ccac1"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"Netbreeze","servicePlanId":"03acaee3-9492-4f40-aed4-bcb6b32981b6"},{"assignedDateTime":"2019-05-09T19:26:04Z","capabilityStatus":"Enabled","service":"SharePoint","servicePlanId":"fe71d6c3-a2ea-4499-9778-da042bf08063"}],"provisionedPlans":[{"capabilityStatus":"Enabled","provisioningStatus":"Success","service":"exchange"},{"capabilityStatus":"Enabled","provisioningStatus":"Success","service":"CRM"},{"capabilityStatus":"Enabled","provisioningStatus":"Success","service":"Netbreeze"},{"capabilityStatus":"Enabled","provisioningStatus":"Success","service":"SharePoint"},{"capabilityStatus":"Enabled","provisioningStatus":"Success","service":"SharePoint"},{"capabilityStatus":"Enabled","provisioningStatus":"Success","service":"SharePoint"},{"capabilityStatus":"Enabled","provisioningStatus":"Success","service":"CRM"}],"verifiedDomains":[{"capabilities":"Email, OfficeCommunicationsOnline","isDefault":true,"isInitial":true,"name":"convergeonemsd.onmicrosoft.com","type":"Managed"}]}]}
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public static JSONArray getOrganizations (final String token) throws IOException, JSONException {
		return fetchValue(BASE_URL + "v1.0/organization", token);
	}
	
	/*
	 * Fetch the json response value from a Graph endpoint https://graph.microsoft.com/v1.0
	 */
	static JSONArray fetchValue (final String url, final String token) throws IOException, JSONException {
		WebClient wc = new WebClient(url);

		Map<String, String> hdrs = new HashMap<String, String>();
		hdrs.put("Authorization", "Bearer " + token);
		hdrs.put("Accept", "application/json, text/plain, */*");
		
		final String resp = wc.doGet(hdrs);
		/** error {  "error": {
		    "code": "InvalidAuthenticationToken",
		    "message": "Access token is empty.",
		    "innerError": {
		      "date": "2020-09-04T18:35:45",
		      "request-id": "a0aeff27-91f3-419d-869e-3f279f96df02"
		    }
		  }
		} */
		JSONObject root = new JSONObject(resp);

		if ( root.has("error")) {
			JSONObject error = root.getJSONObject("error");
			throw new IOException(error.getString("code") + ": " + error.getString("message"));
		}
		return root.getJSONArray("value");
	}

	/**
	 * 
	 * @param token Access token.
	 * @param displayName Organization display name.
	 * @return [{"capabilities": "Email, OfficeCommunicationsOnline", "isDefault": true,"isInitial": true, "name": "convergeonemsd.onmicrosoft.com","type": "Managed"	}]
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONArray getOrgDomains (final String token, String displayName) throws IOException, JSONException {
		JSONArray orgs = getOrganizations(token);
		
		for (int i = 0; i < orgs.length(); i++) {
			JSONObject org = orgs.getJSONObject(i);
			if ( org.getString("displayName").equals(displayName)) {
				return org.getJSONArray("verifiedDomains");
			}
		}
		return new JSONArray();
	}

	/**
	 * 
	 * @param token Access toen
	 * @return {"ConvergeOne":[{"isDefault":true,"capabilities":"Email, OfficeCommunicationsOnline","isInitial":true,"name":"convergeonemsd.onmicrosoft.com","type":"Managed"}]}
	 */
	public static JSONObject getDomainsFromOrgs (final String token) throws IOException, JSONException {
		JSONArray orgs = getOrganizations(token);
		JSONObject root = new JSONObject();
		
		for (int i = 0; i < orgs.length(); i++) {
			JSONObject org = orgs.getJSONObject(i);
			root.put(org.getString("displayName"), org.getJSONArray("verifiedDomains"));
		}
		return root;
	}

	/**
	 * GET https://graph.microsoft.com/v1.0/domains
	 * @param token Access token
	 * @return [{"isDefault":true,"isAdminManaged":true,"isRoot":true,"isVerified":true,"isInitial":true,"authenticationType":"Managed","availabilityStatus":null,"id":"convergeonemsd.onmicrosoft.com","state":null,"supportedServices":["Email","OfficeCommunicationsOnline"],"passwordValidityPeriodInDays":2147483647,"passwordNotificationWindowInDays":14}]
	 */
	public static JSONArray getDomains (final String token) throws IOException, JSONException {
		return fetchValue(BASE_URL + "v1.0/domains", token);	
	}

}
