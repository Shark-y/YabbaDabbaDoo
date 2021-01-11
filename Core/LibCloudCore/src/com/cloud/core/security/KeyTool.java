package com.cloud.core.security;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;


public class KeyTool {

	public enum KeyType { Public, Private};
	
	/**
	 * Parse a key type
	 * @param name One of {@link KeyType}.
	 * @return {@link KeyType}.
	 */
	public static KeyType parseKeyType(String name) {
		try {
			return KeyType.valueOf(name);
		} catch (Exception e) {
			System.err.println("[LIC] " + e.toString());
			return KeyType.Public;
		}
	}
	
	/**
	 * Read a key public or private from the class path or file system.
	 * @param keyFileName Key path on the file system or class path.
	 * @param type See {@link KeyType}.
	 * @return Security {@link Key}.
	 * @throws IOException
	 */
	public static Key readKeyFromFile(String keyFileName, KeyType type) throws IOException {
		if ( type == KeyType.Public) {
			return readPublicKeyFromFile(keyFileName);
		}
		return readPrivateKeyFromFile(keyFileName);
	}
	
	/**
	 * Read a private key from the system class path or file system
	 * @param keyFileName Private Key path on the file system or class path.
	 * @return {@link PrivateKey}.
	 * @throws IOException
	 */
	public static PrivateKey readPrivateKeyFromFile(String keyFileName) throws IOException {
		ObjectInputStream oin = null; 
		try {
			oin =  new ObjectInputStream(new BufferedInputStream(KeyTool.class.getResourceAsStream(keyFileName)));
		}
		catch ( IOException ioe) {
			oin = new ObjectInputStream(new FileInputStream(keyFileName));
		}
		try {
			BigInteger m = (BigInteger) oin.readObject();
			BigInteger e = (BigInteger) oin.readObject();
			RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
			KeyFactory fact 	= KeyFactory.getInstance("RSA");
			PrivateKey priKey 	= fact.generatePrivate(keySpec);
			return priKey; 
		} catch (Exception e) {
			throw new RuntimeException("Encryption serialization error", e);
		} finally {
			oin.close();
		}
	}

	/**
	 * Read a public key from the class path or file system
	 * @param keyFileName Public Key path on the file system or class path.
	 * @return Security {@link PublicKey}.
	 * @throws IOException
	 */
	public static PublicKey readPublicKeyFromFile(String keyFileName) throws IOException {
		ObjectInputStream oin = null;
		try {
			oin =  new ObjectInputStream(new BufferedInputStream(KeyTool.class.getResourceAsStream(keyFileName)));
		}
		catch ( IOException ioe) {
			oin = new ObjectInputStream(new FileInputStream(keyFileName));
		}
		try {	
			BigInteger m = (BigInteger) oin.readObject();
			BigInteger e = (BigInteger) oin.readObject();
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PublicKey key 	= fact.generatePublic(keySpec);
			return key; 
		}
		catch (Exception e) {
			throw new RuntimeException("Encryption serialization error", e);
		} finally {
			oin.close();
		}
	}


}
