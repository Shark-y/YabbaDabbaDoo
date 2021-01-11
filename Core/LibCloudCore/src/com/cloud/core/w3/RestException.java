package com.cloud.core.w3;

public class RestException extends Exception {

	private static final long serialVersionUID = 6459410105192869713L;

	private int httpStatus;
	private String httpMessage;
	private  String contentType;
	
	public RestException(String text) {
		super(text);
	}
	
	public RestException(String message, int httpStatus, String httpStatusMessage) {
		super(message);
		this.httpStatus 	= httpStatus;
		this.httpMessage 	= httpStatusMessage;
	}

	public RestException(int status, String contentType,  String message) {
		super(message);
		this.httpStatus 	= status;
		this.httpMessage 	= message;
		this.contentType	= contentType;
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public String getHttpMessage() {
		return httpMessage;
	}
	
	public String getContentType() {
		return contentType;
	}

	@Override
	public String toString() {
		return httpMessage + " (" + httpStatus + "): " +  super.getMessage();
	}
}
