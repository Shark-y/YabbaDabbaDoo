package com.cloud.rest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import com.cloud.core.io.IOTools;

/**
 * Utility class to do {@link KeyStore} operations:
 * 
 * <pre>
 *  String certPath = "C:/Users/vsilva/.docker/machine/certs/cert.pem";
 *  String keyPath = "C:/Users/vsilva/.docker/machine/certs/key.pem";
 *  String storePath = "C:/Users/vsilva/.docker/machine/certs/docker.jks";
 *  String pwd = "certpass";
 *  String alias = "docker";
 *  KeyStoreTool.createStoreFromPEM(certPath, keyPath, pwd, alias, storePath);
 *  </pre>

 * @author VSilva
 *
 */
public class KeyStoreTool {

	/**
	 * Given a PEM certificate, PEM key and password, create a Java keystore at athe given path
	 * @param certPath full path of the PEM encoded certificate
	 * @param keyPath path of the the PEM key
	 * @param pwd Java keystore (JKS) password
	 * @param alias The JSK alias used to store the(cert,key) pair.
	 * @param storePath Full path of the JKS.
	 * @throws Exception on security errors.
	 */
	public static void createStoreFromPEM (final String certPath, final String keyPath, final String pwd, final String alias, final String storePath) throws Exception {
		OutputStream os = null;
		try {
			// https://stackoverflow.com/questions/4325263/how-to-import-a-cer-certificate-into-a-java-keystore
			CertificateFactory cf 	= CertificateFactory.getInstance("X.509");
			Certificate cer 		= cf.generateCertificate(new FileInputStream(certPath));
			
			PrivateKeyReader pkr 	= new PrivateKeyReader(keyPath);
			PrivateKey pk 			= pkr.getPrivateKey();
			
			// https://docs.oracle.com/javase/7/docs/api/java/security/KeyStore.html
			KeyStore ks  = KeyStore.getInstance(KeyStore.getDefaultType());
			//Make an empty store
			ks.load(null); 
			ks.setKeyEntry(alias, pk, pwd.toCharArray(),  new Certificate[] {cer});
			
			os = new FileOutputStream(storePath);
			ks.store(os, pwd.toCharArray());
			os.close();
		}
		finally {
			IOTools.closeStream(os);
		}
	}
	

}
