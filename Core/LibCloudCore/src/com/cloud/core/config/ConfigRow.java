package com.cloud.core.config;

import com.cloud.core.config.ConfigItem;

/**
 * Class used to render multiple {@link ConfigItem}'s in a single row.
 * @author VSilva
 *
 */
public class ConfigRow {
	public String id;			// row id
	public boolean rendered;	// if true already rendered
	
	public ConfigRow(String id, boolean rendered) {
		super();
		this.id = id;
		this.rendered = rendered;
	}
	
	
}
