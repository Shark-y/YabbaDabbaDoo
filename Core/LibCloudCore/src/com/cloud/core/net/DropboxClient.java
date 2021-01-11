package com.cloud.core.net;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.WebClient;

/**
 * <pre>
 * DROPBOX - https://www.dropbox.com/developers/apps/info/qxkl5xyh5wlcbob
 * User converge_one@yahoo.com/Thenewcti1
 * 
 * App: C1AS_AutoUpdate
 * App key: qxkl5xyh5wlcbob
 * App Secret: b127uczw1pld4t0
 * 
 * Access Token (Apps/AutoUpdate): F1S6eC-N7sAAAAAAAAAADApR55PokUZoqQ909QXQR861ZX_qnZx-FfZvfFs1oEw-
 * Access Token (Apps/LogStorage): F1S6eC-N7sAAAAAAAAAAJbGhvc4h17DcZjLRSCcztey9Ig-KKjwYp1InqylkyrGk
 * 
 * Docs - https://www.dropbox.com/developers/documentation/http/documentation#files-list_folder
 * 
 * </pre>
 * @author VSilva
 *
 */
public class DropboxClient {

	private static final String API_URL 		= "https://api.dropboxapi.com";

	private static final String CONTENT_URL 	= "https://content.dropboxapi.com";

	private static final String CONTENT_JSON 	= "application/json; charset=utf-8";
	
	public static final String KEY_TOKEN = "TOKEN";
	
	// access token
	private String token;
	
	/**
	 * Class to warp a remote Dropbox file.
	 * @author VSilva
	 *
	 */
	public static class DropboxFile {
		String name;
		int size;
		boolean isFile;
		
		public DropboxFile(String name, int size, boolean isFile) {
			super();
			this.name = name;
			this.size = size;
			this.isFile = isFile;
		}
		public String getName() {
			return name;
		}
		public int getSize() {
			return size;
		}
		@Override
		public String toString() {
			return String.format("Name: %s, Size: %d IsFile: %s" ,name, size, isFile);
		}
	}
	
	/**
	 * Simple DropBox client using OAuth2. See https://www.dropbox.com/developers/documentation
	 * @param config Configuration map: TOKEN = Access token
	 * @throws IOException If the access token is missing or any other required configuration value.
	 */
	public DropboxClient(Map<String, String> config) throws IOException {
		token = config.get(KEY_TOKEN);
		if ( token == null )	throw new IOException("Access token is required.");
	}
	
	/**
	 * Download a File: POST - https://content.dropboxapi.com/2/files/download
	 * <pre>Headers:
	 *  "Authorization": "Bearer F1S6eC-N7sAAAAAAAAAADApR55PokUZoqQ909QXQR861ZX_qnZx-FfZvfFs1oEw-",
	 *  "Dropbox-API-Arg": "{\"path\": \"/CloudContactCenterLPAES01.ini\"}",
	 *  "Content-Type": "application/octet-stream; charset=utf-8",
	 * Response: File contents. </pre>
	 * @param path Path of the Dropbox file to download.
	 * @param dest Destination path in the local file system.
	 * @throws IOException on I/O errors.
	 */
	public void download(String path, String dest) throws IOException {
		WebClient wc 				= new WebClient(CONTENT_URL + "/2/files/download");
		String payload 				= String.format("{ \"path\": \"%s\" }", path);
		Map<String, String> headers = getDefaultHeaders(CoreTypes.CONTENT_TYPE_OCTET);
		headers.put("Dropbox-API-Arg", payload);
		
		FileOutputStream os 		= null;
		try {
			os = new FileOutputStream(dest);
			wc.doPost(os, "", CoreTypes.CONTENT_TYPE_OCTET, headers);
			os.close();
		} finally {
			IOTools.closeStream(os);
		}
	}

	private Map<String, String> getDefaultHeaders(final String contentType) {
		Map<String, String> hdrs = new HashMap<String, String>();
		/** 8/8/2020 FIX FOR 0-08-08 19:13:42 ErrorNotificationSystem [CLUSTER-MGR:ERROR] Upload to cloud storage C:\Users\vsilva\AppData\Local\Temp\\NodeLogsVLADS5014_08-08-2020.zip
		java.io.IOException: Error in call to API function "files/upload": Bad HTTP "Content-Type" header: "application/x-www-form-urlencoded,application/octet-stream".  Expecting one of "application/octet-stream", "text/plain; charset=dropbox-cors-hack".
			at com.cloud.core.net.DropboxClient.upload(DropboxClient.java:229)
		*/
		//hdrs.put("Content-Type", contentType);
		hdrs.put("Authorization", "Bearer " + token);
		return hdrs;
	}
	
	/**
	 * List files: POST - https://api.dropboxapi.com/2/files/list_folder
	 * <pre>
	 * Headers: 
	 *    "Authorization": "Bearer F1S6eC-N7sAAAAAAAAAADApR55PokUZoqQ909QXQR861ZX_qnZx-FfZvfFs1oEw-",
	 *    "Content-Type": "application/json; charset=utf-8",
	 * 
	 * Payload
	 * {
	    "path": "",
	    "recursive": false,
	    "include_media_info": false,
	    "include_deleted": false,
	    "include_has_explicit_shared_members": false,
	    "include_mounted_folders": true
	}
	 * Response:
	 * {
	  "entries": [
	    {
	      ".tag": "file",
	      "name": "CloudContactCenterLPAES01.ini",
	      "path_lower": "/cloudcontactcenterlpaes01.ini",
	      "path_display": "/CloudContactCenterLPAES01.ini",
	      "id": "id:kXDskdn95TAAAAAAAAAAFw",
	      "client_modified": "2018-06-12T17:55:59Z",
	      "server_modified": "2018-06-12T17:55:59Z",
	      "rev": "1c5d4dd80",
	      "size": 1151,
	      "content_hash": "42d8b81863c0b5854c4105f3e1a08c714e5a334998b1dd0c03857fceec5a2ca9"
	    }
	  ],
	  "cursor": "AAFugC4cfXUTJSVoerrUpDN5dGS4X_ZxfzgzMxzwxh_-JXy2jQC4ydqMeEBPyR77SObsDGItYH9NdAkMAyTjjzHZ89iP6WcQIXtzXEIpRHVk0gkKyp8ZeW_1AUT1OjRsBP3-aIenj7GohyCob-lYza63",
	  "has_more": false
	} </pre>
	 * @param path DB Base path.
	 * @throws IOException on W3 errors.
	 * @throws JSONException on JSON parse errors.
	 */
	public JSONObject listAsJSON(String path) throws IOException, JSONException {
		WebClient wc 				= new WebClient(API_URL + "/2/files/list_folder");
		String payload 				= String.format("{ \"path\": \"%s\" }", path);
		Map<String, String> headers = getDefaultHeaders(CONTENT_JSON);
		
		String resp = wc.doPost(payload, CONTENT_JSON, headers);
		
		if ( wc.getResponseCode() != 200) {
			throw new IOException(resp);
		}
		return new JSONObject(resp);
	}
	
	/**
	 * List files in a folder.
	 * @param path Base path.
	 * @return A list of {@link DropboxFile}.
	 * @throws IOException On I/O errors.
	 * @throws JSONException on JSON parse errors.
	 */
	public List<DropboxClient.DropboxFile> listFolder(String path) throws IOException, JSONException {
		JSONObject root 		= listAsJSON(path);
		JSONArray entries 		= root.getJSONArray("entries");
		List<DropboxClient.DropboxFile> files = new ArrayList<DropboxClient.DropboxFile>();
		
		for (int i = 0; i < entries.length(); i++) {
			JSONObject obj = entries.getJSONObject(i);
			files.add(new DropboxFile(obj.getString("name"), obj.optInt("size"), obj.getString(".tag").equals("file")));
		}
		return files;
	}

	/**
	 * Upload a file dropbox: <pre>curl -X POST https://content.dropboxapi.com/2/files/upload \
    --header "Authorization: Bearer " \
    --header "Dropbox-API-Arg: {\"path\": \"/Homework/math/Matrices.txt\",\"mode\": \"add\",\"autorename\": true,\"mute\": false,\"strict_conflict\": false}" \
    --header "Content-Type: application/octet-stream" \
    --data-binary @local_file.txt </pre>
	 * @param localPath The local path of the file to upload.
	 * @param remotePath The remote file name. <b>Make sure it starts with '/'</b> (e.g /MyFile.zip)
	 * @param token The access token of the folder or app. See https://www.dropbox.com/developers.
	 * @return Success: {"name": "NodeLogsVLADS5014_02-17-2019.zip", "path_lower": "/nodelogsvlads5014_02-17-2019.zip", "path_display": "/NodeLogsVLADS5014_02-17-2019.zip", "id": "id:kXDskdn95TAAAAAAAAAAUA", "client_modified": "2019-02-17T19:58:03Z", "server_modified": "2019-02-17T19:58:03Z", "rev": "01300000001272638e0", "size": 1649874, "content_hash": "..."}
	 * @see https://www.dropbox.com/developers/documentation/http/documentation#files-upload
	 * @throws IOException On I/O errors: Error in call to API function "files/upload": HTTP header "Dropbox-API-Arg": path: ...
	 * @throws JSONException On JSON errors.
	 */
	public JSONObject upload (final String localPath, final String remotePath, final String token ) throws IOException, JSONException {
		WebClient wc 				= new WebClient(CONTENT_URL + "/2/files/upload");
		String payload 				= String.format("{\"path\": \"%s\",\"mode\": \"add\",\"autorename\": true,\"mute\": false,\"strict_conflict\": false}", remotePath);
		Map<String, String> headers = getDefaultHeaders(CoreTypes.CONTENT_TYPE_OCTET);
		
		headers.put("Authorization", "Bearer " + token);
		headers.put("Dropbox-API-Arg", payload);

		FileInputStream fis 		= null;
		String resp  				= null;
		try {
			fis = new FileInputStream(localPath);
		
			// ERROR: Error in call to API function "files/upload": HTTP header "Dropbox-API-Arg": path: 'NodeLogsVLADS5014_02-17-2019.zip' did not match pattern '(/(.|[\r\n])*)|(ns:[0-9]+(/.*)?)|(id:.*)'
			// OK {"name": "NodeLogsVLADS5014_02-17-2019.zip", "path_lower": "/nodelogsvlads5014_02-17-2019.zip", "path_display": "/NodeLogsVLADS5014_02-17-2019.zip", "id": "id:kXDskdn95TAAAAAAAAAAUA", "client_modified": "2019-02-17T19:58:03Z", "server_modified": "2019-02-17T19:58:03Z", "rev": "01300000001272638e0", "size": 1649874, "content_hash": "..."}
			resp = wc.doPost(fis, CoreTypes.CONTENT_TYPE_OCTET, headers);
			fis.close();
		}
		finally {
			IOTools.closeStream(fis);
		}
		if ( resp != null && resp.contains("Error")) {
			throw new IOException(resp);
		}
		return new JSONObject(resp);
	}

	/**
	 * Get a PUBLIC shared link for a file so users can look at: <pre>curl -X POST https://api.dropboxapi.com/2/sharing/create_shared_link_with_settings \
    --header "Authorization: Bearer ..." \
    --header "Content-Type: application/json" \
    --data "{\"path\": \"/Prime_Numbers.txt\",\"settings\": {\"requested_visibility\": \"public\"}}" </pre>
    
     *<b>This method fails if the lkink already exists.</b>
     *
	 * @param remotePath The remote file name. <b>Make sure it starts with '/'</b> (e.g /MyFile.zip)
	 * @param token The access token of the folder or app. See https://www.dropbox.com/developers.
	 * @return On Success: {".tag": "file", "url": "https://www.dropbox.com/s/5ioewj5lglotbg7/NodeLogsVLADS5014_02-17-2019.zip?dl=0", "id": "id:kXDskdn95TAAAAAAAAAAUQ", "name": "NodeLogsVLADS5014_02-17-2019.zip", "path_lower": "/nodelogsvlads5014_02-17-2019.zip", "link_permissions": {"resolved_visibility": {".tag": "public"}, "requested_visibility": {".tag": "public"}, "can_revoke": true, "visibility_policies": [{"policy": {".tag": "public"}, "resolved_policy": {".tag": "public"}, "allowed": true}, {"policy": {".tag": "team_only"}, "resolved_policy": {".tag": "team_only"}, "allowed": false, "disallowed_reason": {".tag": "user_not_on_team"}}, {"policy": {".tag": "password"}, "resolved_policy": {".tag": "password"}, "allowed": false, "disallowed_reason": {".tag": "user_account_type"}}], "can_set_expiry": false, "can_remove_expiry": true, "allow_download": true, "can_allow_download": true, "can_disallow_download": false, "allow_comments": true, "team_restricts_comments": false}, "preview_type": "archive", "client_modified": "2019-02-17T20:12:18Z", "server_modified": "2019-02-17T20:12:18Z", "rev": "01a00000001272638e0", "size": 1649874}
	 * @see https://www.dropbox.com/developers/documentation/http/documentation#files-upload
	 * @throws IOException If the klink already exists: {"error_summary":"shared_link_already_exists/","error":{".tag":"shared_link_already_exists"}}
	 * @throws JSONException On JSON errors.
	 */
	public JSONObject createSharedLink (final String remotePath, final String token ) throws IOException, JSONException {
		WebClient wc 				= new WebClient(API_URL + "/2/sharing/create_shared_link_with_settings");
		String payload 				= String.format("{\"path\": \"%s\", \"settings\": {\"requested_visibility\": \"public\"}}", remotePath);
		Map<String, String> headers = getDefaultHeaders(CONTENT_JSON);
		
		headers.put("Authorization", "Bearer " + token);

		// ERROR: Error in call to API function "files/upload": HTTP header "Dropbox-API-Arg": path: 'NodeLogsVLADS5014_02-17-2019.zip' did not match pattern '(/(.|[\r\n])*)|(ns:[0-9]+(/.*)?)|(id:.*)'
		// OK {".tag": "file", "url": "https://www.dropbox.com/s/5ioewj5lglotbg7/NodeLogsVLADS5014_02-17-2019.zip?dl=0", "id": "id:kXDskdn95TAAAAAAAAAAUQ", "name": "NodeLogsVLADS5014_02-17-2019.zip", "path_lower": "/nodelogsvlads5014_02-17-2019.zip", "link_permissions": {"resolved_visibility": {".tag": "public"}, "requested_visibility": {".tag": "public"}, "can_revoke": true, "visibility_policies": [{"policy": {".tag": "public"}, "resolved_policy": {".tag": "public"}, "allowed": true}, {"policy": {".tag": "team_only"}, "resolved_policy": {".tag": "team_only"}, "allowed": false, "disallowed_reason": {".tag": "user_not_on_team"}}, {"policy": {".tag": "password"}, "resolved_policy": {".tag": "password"}, "allowed": false, "disallowed_reason": {".tag": "user_account_type"}}], "can_set_expiry": false, "can_remove_expiry": true, "allow_download": true, "can_allow_download": true, "can_disallow_download": false, "allow_comments": true, "team_restricts_comments": false}, "preview_type": "archive", "client_modified": "2019-02-17T20:12:18Z", "server_modified": "2019-02-17T20:12:18Z", "rev": "01a00000001272638e0", "size": 1649874}
		String resp = wc.doPost(payload, CONTENT_JSON, headers);
		if ( resp != null && resp.toLowerCase().contains("error")) {
			throw new IOException(resp);
		}
		return new JSONObject(resp);
	}
	
	/**
	 * List all PUBLIC shared link for a file: <pre>curl -X POST https://api.dropboxapi.com/2/sharing/list_shared_links \
    --header "Authorization: Bearer " \
    --header "Content-Type: application/json" \
    --data "{\"cursor\": \"ZtkX9_EHj3x7PMkVuFIhwKYXEpwpLwyxp9vMKomUhllil9q7eWiAu\"}" </pre>
    
     *
	 * @param remotePath The remote file name. <b>Make sure it starts with '/'</b> (e.g /MyFile.zip)
	 * @param token The access token of the folder or app. See https://www.dropbox.com/developers.
	 * @return On Success: {"has_more":false,"links":[{"path_lower":"/nodelogsvlads5014_02-17-2019.zip","id":"id:kXDskdn95TAAAAAAAAAAUQ","rev":"01a00000001272638e0","preview_type":"archive","link_permissions":{"can_remove_expiry":true,"can_disallow_download":false,"allow_download":true,"can_set_expiry":false,"allow_comments":true,"visibility_policies":[{"allowed":true,"resolved_policy":{".tag":"public"},"policy":{".tag":"public"}},{"allowed":false,"resolved_policy":{".tag":"team_only"},"policy":{".tag":"team_only"},"disallowed_reason":{".tag":"user_not_on_team"}},{"allowed":false,"resolved_policy":{".tag":"password"},"policy":{".tag":"password"},"disallowed_reason":{".tag":"user_account_type"}}],"requested_visibility":{".tag":"public"},"team_restricts_comments":false,"resolved_visibility":{".tag":"public"},"can_allow_download":true,"can_revoke":true},"name":"NodeLogsVLADS5014_02-17-2019.zip",".tag":"file","client_modified":"2019-02-17T20:12:18Z","url":"https://www.dropbox.com/s/5ioewj5lglotbg7/NodeLogsVLADS5014_02-17-2019.zip?dl=0","size":1649874,"server_modified":"2019-02-17T20:12:18Z"}]}
	 * @see https://www.dropbox.com/developers/documentation/http/documentation#files-upload
	 * @throws IOException If the klink already exists: {"error_summary":"shared_link_already_exists/","error":{".tag":"shared_link_already_exists"}}
	 * @throws JSONException On JSON errors.
	 */
	public JSONObject listSharedLinks (final String remotePath, final String token ) throws IOException, JSONException {
		WebClient wc 				= new WebClient(API_URL + "/2/sharing/list_shared_links");
		String payload 				= String.format("{\"path\": \"%s\" }", remotePath);
		Map<String, String> headers = getDefaultHeaders(CONTENT_JSON);
		
		headers.put("Authorization", "Bearer " + token);

		// ERROR: Error in call to API function "files/upload": HTTP header "Dropbox-API-Arg": path: 'NodeLogsVLADS5014_02-17-2019.zip' did not match pattern '(/(.|[\r\n])*)|(ns:[0-9]+(/.*)?)|(id:.*)'
		// OK {"has_more":false,"links":[{"path_lower":"/nodelogsvlads5014_02-17-2019.zip","id":"id:kXDskdn95TAAAAAAAAAAUQ","rev":"01a00000001272638e0","preview_type":"archive","link_permissions":{"can_remove_expiry":true,"can_disallow_download":false,"allow_download":true,"can_set_expiry":false,"allow_comments":true,"visibility_policies":[{"allowed":true,"resolved_policy":{".tag":"public"},"policy":{".tag":"public"}},{"allowed":false,"resolved_policy":{".tag":"team_only"},"policy":{".tag":"team_only"},"disallowed_reason":{".tag":"user_not_on_team"}},{"allowed":false,"resolved_policy":{".tag":"password"},"policy":{".tag":"password"},"disallowed_reason":{".tag":"user_account_type"}}],"requested_visibility":{".tag":"public"},"team_restricts_comments":false,"resolved_visibility":{".tag":"public"},"can_allow_download":true,"can_revoke":true},"name":"NodeLogsVLADS5014_02-17-2019.zip",".tag":"file","client_modified":"2019-02-17T20:12:18Z","url":"https://www.dropbox.com/s/5ioewj5lglotbg7/NodeLogsVLADS5014_02-17-2019.zip?dl=0","size":1649874,"server_modified":"2019-02-17T20:12:18Z"}]}
		String resp = wc.doPost(payload, CONTENT_JSON, headers);
		if ( resp != null && resp.toLowerCase().contains("error")) {
			throw new IOException(resp);
		}
		return new JSONObject(resp);
	}
	
	/**
	 * Invoke createSharedLink and return the url key from the JSON response payload.
	 * If the link exists createSharedLink thows {@link IOException} and listSharedLinks() is invoked.
	 * @param remotePath The remote file name. <b>Make sure it starts with '/'</b> (e.g /MyFile.zip)
	 * @param token The access token of the folder or app. See https://www.dropbox.com/developers.
	 * @return The url from the JSON response payload or null if no link can be found.
	 * @throws IOException On I/O errors: Error in call to API function "files/upload": HTTP header "Dropbox-API-Arg": path: ...
	 * @throws JSONException On JSON errors.
	 */
	public String getSharedLink (final String remotePath, final String token ) throws IOException, JSONException {
		JSONObject root = null;
		try {
			// try to create it. fails if already exists.
			root = 	createSharedLink(remotePath, token);

			// {"path_lower":"/nodelogs 2019-02-17.zip","id":"id:kXDskdn95TAAAAAAAAAAWg","rev":"012c00000001272638e0","preview_type":"archive","link_permissions":{"can_remove_expiry":true,"can_disallow_download":false,"allow_download":true,"can_set_expiry":false,"allow_comments":true,"visibility_policies":[{"allowed":true,"resolved_policy":{".tag":"public"},"policy":{".tag":"public"}},{"allowed":false,"resolved_policy":{".tag":"team_only"},"policy":{".tag":"team_only"},"disallowed_reason":{".tag":"user_not_on_team"}},{"allowed":false,"resolved_policy":{".tag":"password"},"policy":{".tag":"password"},"disallowed_reason":{".tag":"user_account_type"}}],"requested_visibility":{".tag":"public"},"team_restricts_comments":false,"resolved_visibility":{".tag":"public"},"can_allow_download":true,"can_revoke":true},"name":"NodeLogs 2019-02-17.zip",".tag":"file","client_modified":"2019-02-17T22:02:51Z","url":"https://www.dropbox.com/s/wwennvrnxsfvqpw/NodeLogs%202019-02-17.zip?dl=0","size":1649896,"server_modified":"2019-02-17T22:02:51Z"}
			return root.getString("url");
		} catch (IOException e) {
			// link exists, get the 1st from the list.
			root = listSharedLinks(remotePath, token);
			return root.getJSONArray("links").getJSONObject(0).getString("url");
		}
	}
	
}
