package com.cloud.cluster.multicast;

import java.util.Map;

import org.json.JSONObject;

import com.cloud.cluster.multicast.EndPoint;

/**
 * Represents a service HTTP end point (GET, or POST) with optional HTTP headers.
 * 
 * <pre>{
  "method": "GET",
  "url": "http://localhost:8080/stop",
  "headers": null
 }</pree>
 * @author VSilva
 *
 */
public class EndPoint {
	// HTTP method
	String method;
	
	// URL
	String url;
	
	// Optional HTTP headers
	Map<String, String> headers;
	
	public EndPoint(String method, String url, Map<String, String> headers) {
		super();
		this.method = method;
		this.url = url;
		this.headers = headers;
	}
	
	@Override
	public String toString() {
		return toJSON();
	}
	
	public String toJSON() {
		JSONObject root = new JSONObject(headers);
		return String.format("{\"method\": \"%s\", \"url\": \"%s\", \"headers\": %s}", method, url, root.toString());
	}
	
	public static EndPoint get (String url) {
		return get(url, null);
	}
	
	public static EndPoint get (String url, Map<String, String> headers) {
		return new EndPoint("GET", url, headers);
	}

	public static EndPoint post (String url) {
		return post(url, null);
	}
	
	public static EndPoint post (String url, Map<String, String> headers) {
		return new EndPoint("POST", url, headers);
	}
	
}
