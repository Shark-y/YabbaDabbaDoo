package com.rts.datasource;

import java.io.IOException;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.services.ServiceStatus;
import com.rts.datasource.IDataSource.DataSourceType;

/**
 * Base class for all data sources.
 * @author VSilva
 *
 */
public abstract class BaseDataSource {

	protected final DataSourceType type;
	
	/** Data source name */
	protected final String name;
	
	/** Data source description */
	protected final String description;

	/** Object status */
	protected final ServiceStatus status;
	
	/** Initialization parameters */
	protected JSONObject params; 

	/**
	 * Construct.
	 * @param type See {@link DataSourceType} for types.
	 * @param name data source name.
	 * @param description data source description.
	 * @throws IOException if the name or description are null.
	 */
	public BaseDataSource(DataSourceType type, String name, String description) {
		super();
		this.type 			= type;
		this.name 			= Objects.requireNonNull(name, "Data source name is required.");
		this.description 	= Objects.requireNonNull(description, "Data source description is required.");
		this.params			= new JSONObject();
		this.status			= new ServiceStatus(); // OFF_LINE
	}
	
	/**
	 * Construct.
	 * @param type See {@link DataSourceType} for types.
	 * @param name data source name.
	 * @param description data source description.
	 * @throws IOException if the name or description are null.
	 */
	public BaseDataSource(String type, String name, String description) {
		this(DataSourceType.valueOf(type), name, description);
	}

	public BaseDataSource(JSONObject ds) throws JSONException {
		this(ds.getString("type"), ds.getString("name"), ds.optString("description"));
		params 	= ds.getJSONObject("params");
	}
	
	/**
	 * Serialize to JSON
	 * @return { "type": TYPE, "name": "NAME", "description": "DESC", "params": {INITIALIZATION_PARAMETERS}}
	 * @throws JSONException
	 */
	public JSONObject toJSON() throws JSONException {
		JSONObject root = new JSONObject();
		root.put("type", type.name());
		root.put("name", name);
		root.putOpt("description", description);
		root.put("params", params);
		return root;
	}

	public ServiceStatus getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return name + " " + description + " " + status;
	}
	
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
	
}
