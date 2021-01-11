package com.cloud.core.w3;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.w3.WebClient;

/**
 * A zero dependencies OAuth2 client
 * <ul>
 * <li> Use it to fetch access tokens.
 * <li> Grant types: client_credentials. password.
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.1
 *
 */
public class OAuth2 {

	public enum GrantType {
		client_credentials,
		password
		// Others here...
	}
	
	/**
     * Stores the response from a MSCRM OAuth2 request. Similar to ADAL4J AuthenticationResult
     * @author VSilva
     *
     */
	public static class OAuth2AuthenticationResult {
		//final JSONObject root;
		final String acc_tok;
		final String ref_tok;
		final String id_tok;
		final String tok_type;
		final long exp_on;
		final int exp_in;
		
		//{"token_type":"Bearer","expires_in":"3600","ext_expires_in":"3600","expires_on":"1570063268","not_before":"1570059368","resource":"00000002-0000-0000-c000-000000000000","access_token":"TOK"}
		public OAuth2AuthenticationResult(final String payload) throws JSONException {
			// ERROR: {"error":"unsupported_grant_type","error_description":"AADSTS70003: The access grant 'client_credentials1' is not supported.\r\nTrace ID: b5117b51-ac60-4e54-a341-69e2aa8c5100\r\nCorrelation ID: c81c6d64-f200-4165-b934-c315492719f2\r\nTimestamp: 2019-10-03 10:28:40Z","error_codes":[70003],"timestamp":"2019-10-03 10:28:40Z","trace_id":"b5117b51-ac60-4e54-a341-69e2aa8c5100","correlation_id":"c81c6d64-f200-4165-b934-c315492719f2"}
			JSONObject root = new JSONObject(payload);
			// check 4 error
			if ( root.has("error")) {
				throw new JSONException(root.optString("error_description"));
			}
			acc_tok			= root.getString("access_token");
			tok_type		= root.getString("token_type");
			exp_on			= root.getLong("expires_on");
			exp_in			= root.getInt("expires_in");
			ref_tok			= root.optString("refresh_token");
			id_tok			= root.optString("id_token");
		}
		public String getAccessToken() throws JSONException {
			return acc_tok;
		}
		public String getAccessTokenType() throws JSONException {
			return tok_type;
		}
		public Date getExpiresOnDate()  {
			// Note: MS sends UNIX time
			return new Date(exp_on * 1000);
		}
		public int getExpiresIn() {
			return exp_in;
		}
		public String getRefreshToken() throws JSONException {
			return ref_tok;
		}
		public String getIdToken() throws JSONException {
			return id_tok;
		}
		@Override
		public String toString() {
			return tok_type + " Expires on: " + getExpiresOnDate() + " Expires-in(s) " + exp_in + " " + acc_tok;
		}
	}

	/**
	 * <h2>Fetch an access token </h2>
	 * This implementation has zero dependencies. 
	 * <ul>
	 * <li> POST https://AUTHORITY/GUID for example: https://login.microsoftonline.com/e45b00f2-be66-4d82-a9d5-362fc62f5c25
	 * <li> https://auth0.com/docs/api-auth/tutorials/client-credentials
	 * <li> https://stackoverflow.com/questions/31430855/onedrive-for-business-invalid-request-error-descriptionaadsts90014-the-r
	 * </ul>
	 * @param grantType See {@link GrantType}.
	 * @param clientId For example: e3c88a88-a888-4c2b-8437-d35d85da749c
	 * @param resource OAuth2 resource. For example https://clouddemo.crm.dynamics.com
	 * @param clientSecret Application password
	 * @param user Optional - required for grant type password only.
	 * @param password  Optional - required for grant type password only.
	 * @param authority For example- MCCRM: https://login.microsoftonline.com/e45b00f2-be66-4d82-a9d5-362fc62f5c25
	 * @return {@link OAuth2AuthenticationResult} with token information.
	 * @throws IOException on HTTP errors. 
	 * @throws JSONException on JSON parse errors
	 */
	public static OAuth2AuthenticationResult authenticate(GrantType grantType, String clientId,	String resource, String clientSecret, String user, String password, String authority)
			throws JSONException, IOException 
	{
		/**
		 * POST https://login.microsoftonline.com/e45b00f2-be66-4d82-a9d5-362fc62f5c25/oauth2/token
		 * "headers": {
		    "Content-Type": "application/x-www-form-urlencoded",
		    "Accept": "application/json, text/plain, *\/*"
		  },
		  "data": "grant_type=client_credentials&client_id=e3c88a88-a888-4c2b-8437-d35d85da749c&client_secret=W*td@f93kb6eEamg*zNpfJhsV3jk:9U9"
		 */
		final String endPointMS 		= authority; // + "/oauth2/token" ;
		final String contentType		= "application/x-www-form-urlencoded";
		final String payload			= "grant_type=" + grantType.name() + "&client_id=" 
				+ clientId + "&client_secret=" + clientSecret
				// required for grant type password only
				+ (user != null ? "&username=" + user : "")
				+ (password != null ? "&password=" + password : "")
				+ (resource != null ? "&resource=" + resource : "")
				;
		
		final Map<String, String> hrds	= new HashMap<String, String>();
		hrds.put( "Accept", "application/json, text/plain, */*");
		
		WebClient wc 		= new WebClient(endPointMS);
		//wc.setDebug(true);
		//wc.logToStdOut(true);
		final String resp 	= wc.doPost(payload, contentType, hrds);

		// parse: {"token_type":"Bearer","expires_in":"3600","ext_expires_in":"3600","expires_on":"1570063268","not_before":"1570059368","resource":"00000002-0000-0000-c000-000000000000","access_token":"TOK"}
		return new OAuth2AuthenticationResult(resp);
	}

	/**
	 * Helper method for authentication via grant type client_credentials.
	 * @param grantType See {@link GrantType}: client_credentials.
	 * @param clientId For example: e3c88a88-a888-4c2b-8437-d35d85da749c
	 * @param resource OAuth2 resource. For example https://clouddemo.crm.dynamics.com
	 * @param clientSecret Application password
	 * @param authority For example- MCCRM: https://login.microsoftonline.com/e45b00f2-be66-4d82-a9d5-362fc62f5c25
	 * @return {@link OAuth2AuthenticationResult} with token information.
	 * @throws Exception on HTTP errors.
	 */
	public static OAuth2AuthenticationResult authenticate(GrantType grantType, String clientId,	String clientSecret, String authority)
			throws Exception 
	{
		return authenticate(grantType, clientId, null, clientSecret, null, null, authority);
	}


}
