package com.cloud.core.config;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.config.ConfigItem;

/**
 * Used to group {@link ConfigItem} objects.
 * 
 * @author VSilva
 * @version 1.0.1 1/2/2019 new toJSON() method. 
 *
 */
public class ConfigGroup {
	public String name;			// rendered as HTML <FIELDSET> Starts w/ cfg_group
	public String title;		// Group name: Server Deatils, etc.
	public String description;	// Description (optional)
	
	public ConfigGroup(String name, String title) {
		this.name 	= name;
		this.title	= title;
	}
	public ConfigGroup(String name, String title, String description) {
		this.name 			= name;
		this.title			= title;
		this.description	= description;
	}
	
	@Override
	public String toString() {
		return "[grp:" + name + " t:" + title + (description!= null ? "d:" + description : "") + "]";
	}
	
	public String toXML() {
		return "\n<group>" 
				+ ConfigItem.LFCR + "<name>" + name + "</name>" 
				+ ConfigItem.LFCR + "<title>" + title + "</title>" 
				+ (description!= null ? ConfigItem.LFCR + "<description>" + description + "</description>" : "") + "\n</group>";
	}
	
	/**
	 * @return JSON: { 'name': 'title'}
	 * @throws JSONException On JSON I/O errors.
	 */
	public JSONObject toJSON() throws JSONException {
		JSONObject root = new JSONObject();
		root.put(name, title);
		return root;
	}
}