package com.cloud.docker;

/**
 * <h1>ANSI Escape Codes</h1>
 * <p>
 * Stuff taken from https://github.com/morikuni/aec/blob/master/aec.go
 * </p>
 * @see https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
 * @author VSilva
 *
 */
public class AnsiEscapeCodes {
	//static final String ESC = "\\u001b[";
	static final String ESC 	= "\\x1b[";
	static final String FGCOL 	= ESC + "38;5;";
	
	public enum EraseMode {
		// All erase all.
		All,

		// Head erase to head.
		Head, 

		// Tail erase to tail.
		Tail 
	}
	
	/**
	 * Up moves up the cursor.
	 * @param n Number of lines.
	 * @return \x1b{N}A
	 */
	public static String up (int n) {
		return String.format("%s%dA", ESC, n);
	}

	public static String down (int n) {
		return String.format("%s%dB", ESC, n);
	}

	public static String left (int n) {
		return String.format("%s%dD", ESC, n);
	}
	
	public static String right (int n) {
		return String.format("%s%dC", ESC, n);
	}
	
	/**
	 * Up moves up the cursor.
	 * @param n Number of lines.
	 * @return \x1b{0-2}K
	 */
	public static String eraseLine (EraseMode m) {
		return String.format("%s%dK", ESC, m.ordinal());
	}

	/**
	 * https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
	 * @return Foreground \\x1b[38;5;9m
	 */
	public static String red () {
		return String.format("%s%dm", FGCOL, 9);
	}
	/**
	 * @return Foreground \\x1b[38;5;2m
	 */
	public static String green () {
		return String.format("%s%dm", FGCOL, 10);
	}
	/**
	 * @return Foreground \\x1b[38;5;11m
	 */
	public static String yellow () {
		return String.format("%s%dm", FGCOL, 11);
	}
	/**
	 * @return Foreground \\x1b[38;5;12m
	 */
	public static String blue () {
		return String.format("%s%dm", FGCOL, 12);
	}
	/**
	 * @return Foreground \\x1b[38;5;15m
	 */
	public static String white () {
		return String.format("%s%dm", FGCOL, 15);
	}

}
