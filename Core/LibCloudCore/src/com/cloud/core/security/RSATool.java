package com.cloud.core.security;

import java.math.BigInteger;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;

import com.cloud.core.types.CoreTypes;

/**
 * A-symetric (RSA) decryption tools and others.
 * @author VSilva
 *
 */
public class RSATool {

	/**
	 * Decrypt a set of bytes.
	 * @param key The key used to decrypt.
	 * @param data bytes to decrypt.
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt( Key key, byte[] data) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, key);

		byte[] cipherData = cipher.doFinal(data);
		return cipherData;
	}

	/**
	 * Decrypt an encoded string.
	 * @param key decryption key.
	 * @param secret The string to decrypt.
	 * @return
	 * @throws Exception
	 */
	public static String decrypt(Key key, String secret) throws Exception {
		return new String(decrypt(key, stringToBytes(secret)), CoreTypes.CHARSET_UTF8.name());
	}
	
	private static byte[] stringToBytes(String s) {
		byte[] b2 = new BigInteger(s, 36).toByteArray();
		return Arrays.copyOfRange(b2, 1, b2.length);
	}
}
