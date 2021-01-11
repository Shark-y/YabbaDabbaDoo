/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cloud.core.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cloud.core.io.Base64;
import com.cloud.core.types.CoreTypes;

/**
 * Symetric cipher based encryption tool.
 * To Encrypt:<pre>
 * byte[] secret 	= EncryptionTool.encrypt("hello world") 
 * String enc 	= EncryptionTool.toHex(secret);</pre>
 * To deprypt:<pre>
 *  String dec = new String(EncryptionTool.decrypt(EncryptionTool.hexStringToByteArray(enc)));
 * </pre>
 * @author vsilva
 *
 */
public class EncryptionTool {

    private static Cipher cipher;
    private static SecretKeySpec key;
    
    private static final byte[] BYTES = {
        -73, 0, -102, 71, -100, -111, 62, -82, -8, 115, -109, 7, -67, -78, -50, 27};
    
    static private final String Algorithm = "AES";

    /** Default salt size */
	private static final int DEFAULT_SALT_SIZE = 16;
	
	/** Tag used to prefix a salted password */
	private static final String ENC_WITH_SALT_PREFIX = "ENC+SALT" + DEFAULT_SALT_SIZE + ":";

	/** Prefix used in all encrypted passwords: ENC:xxxx
	 * @deprecated Passwords must be salted for security reasons. 
	 */
	public static final String ENCRYPTED_PWD_PREFIX = "ENC:";

    static {
        try {
            cipher 	= Cipher.getInstance(Algorithm);
            key 	= new SecretKeySpec(BYTES, Algorithm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Turns array of bytes into string
     * 
     * @param buf
     *            Array of bytes to convert to hex string
     * @return Generated hex string
     */
    public static String toHex(byte buf[]) {
		StringBuilder sb 	= new StringBuilder( 2 * buf.length);
		
		for ( byte b : buf) {
			sb.append(String.format("%02X", b & 0xff));
		}
		return sb.toString();
    } 

    /**
     * Convert a string into an array of bytes.
     * @param s String to convert.
     * @return array of bytes.
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Encrypt
     * @param text
     * @return
     */
    static public byte[] encrypt(String text) {
        if (cipher == null || text == null) {
            return null;
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(text.getBytes(CoreTypes.CHARSET_UTF8));
        } catch (Exception e) {
            //e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypt a text string.
     * @param text Plain text to encrypt.
     * @return Encrypted data encoded as a HEX string.
     */
    public static String encryptText(String text) {
    	return EncryptionTool.toHex(EncryptionTool.encrypt(text));
    }

    /**
     * Decrypt a HEX encoded text string.
     * @param hexString Hex encoded string to decrypt.
     * @return Plain text (UTF-8).
     */
    public static String decryptText(String hexString) {
    	return new String(EncryptionTool.decrypt(EncryptionTool.hexStringToByteArray(hexString)), CoreTypes.CHARSET_UTF8);
    }

    /**
     * Decrypt
     * @param text
     * @return
     */
    static public byte[] decrypt(byte[] text) {
        if (cipher == null || text == null) {
            return null;
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(text);
        } catch (Exception e) {
            //e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static void dumpKey() {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance(Algorithm);
            kgen.init(128); //112);
            byte[] k = kgen.generateKey().getEncoded();
            for (int i = 0; i < k.length; i++) {
                System.out.print(k[i] + ",");
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Default hash subroutine (Using SHA-256). Note: MD5, SHA-1 have been removed.
     * @see https://security.googleblog.com/2017/02/announcing-first-sha1-collision.html
     * @param string String to hash.
     * @return Hashed string (using SHA-256).
     */
	public static String HASH(String string) {
		try {
			return HASH(string, "SHA-256");
		} catch (Exception e) {
			return null;
		}
	}
	
    /**
     * Generate an MD5 from a string.
     * @param string String to hash.
     * @return MD5 of the string.
     */
	/* removed 4/11/2017 - https://security.googleblog.com/2017/02/announcing-first-sha1-collision.html
	public static String MD5(String string) {
		try {
			return HASH(string, "MD5");
		} catch (Exception e) {
			return null;
		}
	} */

	/**
	 * Create a SHA1 hash from a string.
	 * @param string String to hash.
	 * @return A hex encoded SHA1 hash of the string.
	 */
	/* removed 4/11/2017 - https://security.googleblog.com/2017/02/announcing-first-sha1-collision.html
	public static String SHA1(String string) {
		try {
			return HASH(string, "SHA1");
		} catch (Exception e) {
			return null;
		}
	} */
	
	/**
	 * Generalized hash.
	 * @param string String to hash.
	 * @param algorithm Hash algorithm.
	 * @return A hexadecimal encoded hash using the provided algorithm.
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static String HASH(String string, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md 	= MessageDigest.getInstance(algorithm);
		byte[] hash 		= md.digest(string.getBytes("UTF-8"));
		StringBuilder sb 	= new StringBuilder( 2 * hash.length);
		for ( byte b : hash) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}
	
	/**
	 * A signature algorithm using HMAC-SHA1.
	 * @param payload String to sign.
	 * @param keyString A signature key.
	 * @return Signature as an array of bytes.
	 */
	public static byte[] HMAC_SHA1(String payload, String keyString) {
		Mac 	mac;
		byte[] 	key;

		try {
			mac 				= Mac.getInstance("HmacSHA1");
			key 				= keyString.getBytes("UTF-8");
			SecretKeySpec spec	= new SecretKeySpec(key, "HmacSHA1");
			mac.init(spec);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return mac.doFinal(payload.getBytes(CoreTypes.CHARSET_UTF8));
	}
	
	/**
	 * Sign a string using a HmacSHA1 algorithm.
	 * @param payload String to sign.
	 * @param key Signature key.
	 * @return A {@link Base64} encoded signature of the payload using the key.
	 */
	static public String signWithHmacSHA1(String payload, String key) {
		return Base64.encode(HMAC_SHA1(payload, key));
	}

	/**
	 * Verify a signature.
	 * @param signature The digital signature.
	 * @param key The key used for verification.
	 * @param payload Original payload.
	 * @return True if the signature is valid.
	 */
	public static boolean verifySignature(String signature, String key, String payload) {
		return signWithHmacSHA1(payload, key).equals(signature);
	}
	
	/**
	 * GUID generator.
	 * @return Ramdom UUID.
	 */
	public static String UUID() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Get a random salt of a given length.
	 * @param len Salt length in characters.
	 * @return Base64 encoded salt of len characters.
	 */
	public static String getRandomSalt(int len) {
		byte[] bytes = new byte[len];
		CoreTypes.RANDOM.nextBytes(bytes);
		String b64 = com.cloud.core.io.Base64.encode(bytes); 
		return b64.length() != len ? b64.substring(0, len) : b64;
	}
	
	/**
	 * Encrypt and salt a plain string. 
	 * @param salt Random salt.
	 * @param plain Plain text.
	 * @return SALT + {SALTED-CRYPTO (HEX)}
	 */
	public static String encryptTextWithSalt(String salt, String plain) {
		return salt + EncryptionTool.encryptText(plain + salt);
	}

	/**
	 * Decrypt an encoded HEX string.
	 * @param salt Random salt.
	 * @param secret Hex encoded secret.
	 * @return
	 */
	public static String decryptTextWithSalt(String salt, String secret) {
		return EncryptionTool.decryptText(secret.replace(salt, "")).replace(salt, "");
	}

	/**
	 * Encrypt a plain string and tag it as encrypted.
	 * @param plain Plain text. <b>Note: If the text is already encrypted it will do nothing.</b>
	 * @return {TAG}{ENCRYPTED} => TAG := [ENC: | ENC+SALT{SIZE}:]
	 */
	public static String encryptAndTagPassword(String plain) {
		// 8/9/2020 - Avoid double encryption.
		if ( isEncryptedText(plain)) {
			return plain;
		}
		if ( isSaltedText(plain)) {
			return EncryptionTool.decryptText(plain.split(":")[1]); 
		}
		return ENC_WITH_SALT_PREFIX + encryptTextWithSalt(getRandomSalt(DEFAULT_SALT_SIZE), plain);
	}

	/**
	 * Decrypt a tagged ENC:{SECRET} and optionally salted (ENC+SALTn:...) password.
	 * <li> Tagged & salted: ENC+SALT16:ii89I2o/jMxchvPX2D5D8F090A9BAC5...
	 * <li> Tagged not salted (DEPRECATED): ENC:B38261578C4E281BCA28C1BA090709FC
	 * @param tagged Tagged secret. If the text is not tagged as encrypted then the input string is returned.
	 * @return Plain text.
	 */
	public static String decryptTaggedPassword (String tagged) {
		if ( ! isEncryptedText(tagged)) {
			return tagged;
		}
		if ( ! isSaltedText(tagged)) {
			return EncryptionTool.decryptText(tagged.split(":")[1]); 
		}
		String untagged = tagged.replace(ENC_WITH_SALT_PREFIX, "");
		String salt 	= untagged.substring(0, DEFAULT_SALT_SIZE);
		return decryptTextWithSalt(salt, untagged); 
	}

	/**
	 * IS encrypted.
	 * @param text Secret: ENC:xxxx or ENC+SALT16:...
	 * @return True if text matches any of the crypto tags: TAG := [ENC: | ENC+SALT{SIZE}:]
	 */
	public static boolean isEncryptedText (String text) {
		return text.matches("^ENC.*?:.*");
	}

	/**
	 * Check if text is salted. If it starts with ENC+SALT{n}:...
	 * @param text Text to test.
	 * @return True if encrypted and salted secret.
	 */
	public static boolean isSaltedText (String text) {
		return text.matches("^ENC\\+SALT.*?:.*");
	}

}
