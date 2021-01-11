package com.cloud.core.provider;

/**
 * Interface to provide HTML fragments that can be inserted dynamically into HTML pages.
 * @author VSilva
 *
 */
public interface IHTMLFragment {

	/**
	 * Get an HTML fragment that can be included in JSPS & stuff.
	 * @return
	 */
	String getInnerHTML();
	
}
