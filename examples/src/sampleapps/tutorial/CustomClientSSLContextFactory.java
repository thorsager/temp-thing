package sampleapps.tutorial;


import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.avaya.common.nio.managed.tlsImpl.TLSClientSSLContextFactory;

/**
 * Sample implementation of TLSClientSSLContextFactory to provide
 * custom client SSLCOntext by client Application
 * @author branjan
 *
 */

public class CustomClientSSLContextFactory implements
		TLSClientSSLContextFactory {


	private KeyStore trustStore;
	//using default trustStore
	private String trustStoreLocation = "resources/avaya.jks";
	private String trustStorePassword = "password";
    
	/**
	 *  This method creates and returns the Custom Client SSLContext
	 */
	@Override
	public SSLContext createSSLContext() {
		SSLContext context = null;
			try {
				context = SSLContext.getInstance("TLS");
				context.init(getKeyManager(), getTrustManager(), null);
			} catch (Exception e) {
				System.out.println("Could not create/initialize the custom SSLContext");
				e.printStackTrace(System.out);
			}

		return context;
	}
	
	/**
	 *  
	 * @return TrustManager[]
	 * @throws Exception 
	 */
	private TrustManager[] getTrustManager() throws Exception {
		// The default key store type is JKS.
        trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        
        trustStore.load(new FileInputStream(trustStoreLocation), trustStorePassword.toCharArray());

        // The default algorithm is SunX509.
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		return trustManagers;
	}
   
	/**
	 * dummy implementation, can be implemented and used
	 * @return  KeyManager[]
	 */
	private KeyManager[] getKeyManager() {
		return null;
	}

}
