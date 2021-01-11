package com.cloud.ssh;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * Default SSH handshake events provided by JSch
 * 
 * @author VSilva
 * 
 */
public /*abstract*/ class SSHDefaultUserInfo implements UserInfo, UIKeyboardInteractive {
	public String getPassword() {
		return null;
	}

	/**
	 * Fires when SSH prompts for a yes/no answer. Implement to handle Y/N prompts.
	 * @param The Y/N prompt.
	 * @return True means yes, false mean no.
	 */
	public boolean promptYesNo(String str) {
		// No display, always accept
		return true;
	}

	public String getPassphrase() {
		return null;
	}

	public boolean promptPassphrase(String message) {
		return false;
	}

	public boolean promptPassword(String message) {
		return false;
	}

	public void showMessage(String message) {
	}

	public String[] promptKeyboardInteractive(String destination, String name,	String instruction, String[] prompt, boolean[] echo) {
		return null;
	}
}