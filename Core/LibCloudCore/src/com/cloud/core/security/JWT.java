package com.cloud.core.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.Base64;
import com.cloud.core.types.CoreTypes;

/**
 * Ultra simple JWT generator.
 * 
 * <pre>
 * // https://www.jsonwebtoken.io/
 * JSONObject payload = new JSONObject();
 * payload.put("key", "val");
 * 
 * String key= "b00abc9b8164a365f2490e320f5bae80";
 * System.out.println("JWT=" + JWT.encode(payload, key));
 * </pre>
 * 
 * @author VSilva
 * @see https://owasp.org/www-project-cheat-sheets/cheatsheets/JSON_Web_Token_Cheat_Sheet_for_Java
 */
public class JWT {

	public static final String 	DEFAULT_HEADER = "{\"alg\": \"HS256\", \"typ\": \"JWT\" }";

	/**
	 * A signature algorithm using HMAC-SHA1.
	 * @param payload String to sign.
	 * @param keyString A signature key.
	 * @return Signature as an array of bytes.
	 */
	public static byte[] HMAC_SHA256(byte[] payload, String keyString) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
		Mac 	mac;
		byte[] 	key;
		mac 				= Mac.getInstance("HmacSHA256");
		key 				= keyString.getBytes("UTF-8");
		SecretKeySpec spec	= new SecretKeySpec(key, "HmacSHA256");
		mac.init(spec);
		return mac.doFinal(payload);
	}
	
	static byte[] sign(String payload, String keyString) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
		return HMAC_SHA256(payload.getBytes("UTF-8"), keyString);
	}
	
	static String base64UrlEncode (byte[] data) {
		// URL safe: replace + wit -, / with _ and remove padding char (=)
		return Base64.encode(data).replaceAll("\\+", "-").replaceAll("/", "_").replaceAll("=", "");
	}

	static byte[] base64UrlDecode (String encoded) throws IOException {
		// URL safe: replace + wit -, / with _ and remove padding char (=)
		return Base64.decode(encoded.replaceAll("-", "\\+").replaceAll("_", "/"));
	}
	
	/**
	 * Get a default payload
	 * @return <pre>{
        "jti": "",
        "iss": "",
        "sub": "x",
        "nbf": 1450471147,
        "exp": 1450474747,
		}</pre>
	 * @throws JSONException
	 */
	public static JSONObject getDefaultPayload () throws JSONException {
		final String instance 	= CoreTypes.INSTANCE_ID;
		final long time			= System.currentTimeMillis();
		JSONObject root = new JSONObject();
		
		root.put("jti", instance + "-" + time);
		root.put("iss", instance);
		root.put("nbf", time);
		root.put("exp", 3600000);	// 1hr
		return root;
	}
	
	/**
	 * Token structure
	 * <p>Token structure example taken from JWT.IO:</p>
	 * <pre>[Base64(HEADER)].[Base64(PAYLOAD)].[Base64(SIGNATURE)]</pre>
	 * @param payload JSON payload
	 * @param key Hash key.
	 * @see https://www.jsonwebtoken.io/
	 * @return Encoded JWT.
	 */
	public static String encode (JSONObject payload, String key) throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException {
		return encode(payload.toString(), key);
	}
	
	/**
	 * Token structure
	 * <p>Token structure example taken from JWT.IO:</p>
	 * <pre>[Base64(HEADER)].[Base64(PAYLOAD)].[Base64(SIGNATURE)]</pre>
	 * @see https://www.jsonwebtoken.io/
	 * @param payload JSON payload
	 * @param key Hash key.
	 * @return Encoded JWT.
	 */
	public static String encode (String payload, String key) throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException {
		String chunk1 = base64UrlEncode (DEFAULT_HEADER.getBytes());
		String chunk2 = base64UrlEncode (payload.toString().getBytes());
		byte[] signature = sign(base64UrlEncode(DEFAULT_HEADER.getBytes()) +"." + base64UrlEncode (payload.getBytes()) , key); 
		return chunk1 + "." + chunk2	+ "." + base64UrlEncode (signature);
	}
	
	/**
	 * Verify token signature.
	 * @param token The token header is expected to be {"alg": "HS256", "typ": "JWT" }
	 * @param secret Secret used to sign it/
	 * @throws IOException on Invalid signature.
	 * @throws InvalidKeyException on format errors.
	 * @throws NoSuchAlgorithmException on format errors.
	 */
	public static void verify (final String token, final String secret) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		String[] parts = token.split("\\.");
		if ( parts.length != 3) {
			throw new IOException("Invalid JWT token format");
		}
		// parts must be multiples of 4
		while ( parts[0].length() % 4 != 0) parts[0] += "=";
		while ( parts[1].length() % 4 != 0) parts[1] += "=";
		//final String sig = parts[2];
		
		//String hdr = new String(base64UrlDecode(parts[0]));
		String pay = new String(base64UrlDecode(parts[1]));
		
		// verify sig by encoding and comparing
		String encoded = encode(pay, secret);
		if ( !token.equals(encoded)) {
			throw new IOException("Invalid signature");
		}
	}
}
