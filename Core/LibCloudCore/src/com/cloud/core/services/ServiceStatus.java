package com.cloud.core.services;

import com.cloud.core.provider.IServiceLifeCycle;

/**
 * Describes the status of any service or client. Status can be:
 * <ol>
 * <li> OFF_LINE: Service is off line,
 * <li> ON_LINE: Service is on line.
 * <li> SERVICE_ERROR: An error occurred. See the optional description.
 * <li> STARTED_WITH_ERRORS : Started but something happened (check the logs).
 * </ol>
 * @author VSilva
 * @version 1.0.1 - 11/8/2017 Default constructor, set default values for status and description.
 *
 */
public class ServiceStatus {

	/** 
	 * Service Status: OFF_LINE, ON_LINE, SERVICE_ERROR
	 * @author VSilva
	 *
	 */
	public enum Status { /*UNKNOWN,*/ 
		OFF_LINE, 			// Adapter not started
		ON_LINE, 			// Adapter started (no errors)
		SERVICE_ERROR,			// Adapter failed to start
		STARTED_WITH_ERRORS, 	// Started but something happened (check the logs).
		CONNECTING,				// 8/15/2019
		IDLE,					// 8/15/2019
		UNKNOWN,				// 8/15/2019
		RUNNING					// 10/12/2019
		
		; // 10/12/2019
		public String getStatus() {
			return name().toString();
		}
	};

	Status status;			// service status
	String description;		// status description

	/**
	 * Construct with initial values.
	 * @param status The initial Service {@link Status}.
	 * @param desc The initial description.
	 */
	public ServiceStatus(Status status, String desc) {
		this.status 		= status;
		this.description	= desc;
	}
	
	/**
	 * Construct with default values: status = OFF_LINE, description = Down.
	 */
	public ServiceStatus() {
		this.status			= Status.OFF_LINE;
		this.description	= ""; // 11/16/2017 - Default is empty "Down";
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String statusDescription) {
		this.description = statusDescription;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
	public void setStatus(Status status, String description) {
		this.status = status;
		this.description = description;
	}
	
	/**
	 * Convert the {@link IServiceLifeCycle.Status} enum to an equivalent http code.
	 * @param status {@link IServiceLifeCycle.Status}
	 * @return Http code ON_LINE = 200, OFF_LINE = 503, SERVICE_ERROR = 500.
	 */
	public static int toHTTPCode (Status status) {
		switch (status) {
		case ON_LINE:
			return 200;
		case OFF_LINE:
			return 503;
		case SERVICE_ERROR:
			return 500;
		default:
			return 0;
		}
	}
	
	@Override
	public String toString() {
		return status != null ?  description != null ? status + " " + description : status.name() : "";
	}
}
