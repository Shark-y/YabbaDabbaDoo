package com.rts.core;

import java.util.Objects;

/**
 * Validation helper extending the JVM {@link Objects} class.
 * 
 * <pre>
 * // Throws NullPonterEx if null or IllegalArgumentEx if empty.
 * final String driver = Validators.requireNotNullAndNotEmpty(request.getParameter("db_drv") ,"DB driver is required.");
 * final String url 	= Validators.requireNotNullAndNotEmpty(request.getParameter("db_url") ,"DB URL is required.");
 * </pre>
 * 
 * @author VSilva
 * @see The {@link Objects} class.
 * @version 1.0.0
 */
public class Validators {

	/**
	 * Similar to {@link Objects} requireNotNull but for a string. It also checks that the string is not empty.
	 * @param obj String to check.
	 * @param msg The message to return is the validation fails.
	 * @return The object to validate. Throws {@link NullPointerException} if null or {@link IllegalArgumentException} if empty.
	 */
	public static String requireNotNullAndNotEmpty (String obj, String msg) {
		if ( obj != null && obj.isEmpty()){
			throw new IllegalArgumentException(msg);
		}
		return java.util.Objects.requireNonNull(obj, msg);
	}
}
