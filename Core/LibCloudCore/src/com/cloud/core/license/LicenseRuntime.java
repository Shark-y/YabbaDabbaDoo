package com.cloud.core.license;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * License runtime  information class. The information is defined in
 * the class {@link LicenseRuntimeItem}. It includes:
 * <li>Client ID (agent)
 * <li>Client IP address
 * <li>Client login date
 * @author sharky
 *
 */
public class LicenseRuntime {

	/**
	 * {@link LicenseRuntime} items
	 * @author sharky
	 *
	 */
	static public class LicenseRuntimeItem {
		public String clientId;
		public String ipAddress;
		public Date loginDate;
		
		private Map<String, String> userData;
		
		/**
		 * License runtime constructor.
		 * @param clientId Client id.
		 * @param loginDate Login date.
		 * @param ip IP address of the client.
		 */
		public LicenseRuntimeItem(String clientId, Date loginDate, String ip) {
			this.clientId 	= clientId;
			// Findbugs LicenseRuntime.java:40 new com.cloud.core.license.LicenseRuntime$LicenseRuntimeItem(String, Date, String) may expose internal representation by storing an externally mutable object into LicenseRuntime$LicenseRuntimeItem.loginDate [Of Concern(18), Normal confidence]
			this.loginDate 	= new Date(loginDate.getTime());
			this.ipAddress 	= ip;
			this.userData	= new HashMap<String, String>();
		}
		
		/**
		 * Store a user defined key-value pair.
		 * @param key User key.
		 * @param value Custom value.
		 */
		public void putUserData(String key, String value) {
			userData.put(key, value);
		}
		
		/**
		 * Get user defined value.
		 * @param key User key.
		 * @return Custom value.
		 */
		public String getUserData (String key) {
			return userData.get(key);
		}
		
		@Override
		public String toString() {
			return "[" + clientId + " Ip:" + ipAddress + " LoginDate:" + loginDate + "]";
		}
	}
	
	private List<LicenseRuntimeItem> items = new ArrayList<LicenseRuntime.LicenseRuntimeItem>();
	
	public void addItem(LicenseRuntimeItem item) {
		items.add(item);
	}
	
	public List<LicenseRuntimeItem> getItems() {
		return items;
	}
}
