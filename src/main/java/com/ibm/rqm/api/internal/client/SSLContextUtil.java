/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2013, 2020. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.client;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.ibm.rqm.api.internal.util.LogUtils;

/**
 * <p>Jazz HTTP client SLL context utilities.</p>
 * 
 * <p>Note: Copied from <code>com.ibm.rqm.ct</code> plug-in.</p>
 *  
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 */
public class SSLContextUtil {
    
	
    /**
     * Encryption algorithm TLS then SSL - IBM JREs
     */
    public static final String SSL_TLS = "SSL_TLS"; //$NON-NLS-1$
    /**
     * Encryption algorithm TLS then SSL - Sun JREs
     */
    public static final String SSLV3 = "SSLv3"; //$NON-NLS-1$
    /**
     * Encryption algorithm TLS
     */
    public static final String TLS = "TLS"; //$NON-NLS-1$
    /**
     * Encryption algorithm SSL 
     */
    public static final String SSL = "SSL"; //$NON-NLS-1$
        
    
    /**
     * Creates an SSL context factory.
     * The returned SSLContext will be created so that it is compatible with the
     * current security environment.  If a FIPS environment is detected then a
     * FIPS 140-2 complaint context will be returned. 
     * 
     * @return a {@link SSLContext}
     * @since 2.0.1
     */
    public static SSLContext createSSLContext(TrustManager trustManager) {
    	SSLContext context = null;
    	String overrideProtocol = "TSLv1.2";
	    if (overrideProtocol != null)  {
	    	LogUtils.logInfo("Attempting to create protocol context using system property: " + overrideProtocol);  //$NON-NLS-1$
	        context = createSSLContext(overrideProtocol, trustManager);
	    }
 	    
 	   if (context== null)  {
 		   	LogUtils.logInfo("Attempting to create SSL_TLS context");  //$NON-NLS-1$
	        context = createSSLContext(SSL_TLS, trustManager);
	    }
        
        if (context == null) {
        	LogUtils.logInfo("Unable to create SSL_TLS context, trying SSLv3");  //$NON-NLS-1$
            // When SSL_TLS doesn't work (e.g. under FIPS or Sun JRE), try SSLv3
            context = createSSLContext(SSLV3, trustManager);
        }
        
        if (context == null) {
            LogUtils.logInfo("Unable to create SSLv3 context, trying TLS");  //$NON-NLS-1$
            // When SSLv3 doesn't work (e.g. under FIPS), try TLS
            context = createSSLContext(TLS, trustManager);
        }

        if (context == null) {
            LogUtils.logInfo("Unable to create TLS context, trying SSL"); //$NON-NLS-1$
            // Fall back to just SSL when the above two are not available
            context = createSSLContext(SSL, trustManager);
        }

        if (context == null) {
            /* No encryption algorithm worked.  Give up.  This should never happen
             * in any of our supported configurations. */
            throw new RuntimeException("No acceptable encryption algorithm found"); //$NON-NLS-1$
        }

        return context;
    }
    
    // Returns null when the given algorithm fails
    private static SSLContext createSSLContext(String algorithm, TrustManager trustManager) {
        SSLContext context;
        try {
            context = SSLContext.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
        	LogUtils.logInfo("Error creating SSL context.", e); //$NON-NLS-1$
            return null;
        }
        try {
            context.init(null, new TrustManager[] { trustManager }, null);
        } catch (KeyManagementException e) {
        	LogUtils.logInfo("Error creating SSL context.", e); //$NON-NLS-1$
            return null;
        }

        /* Create a socket to ensure this algorithm is acceptable.  This will
         * correctly disallow certain configurations (such as SSL_TLS under FIPS) */
        try {
            Socket s = context.getSocketFactory().createSocket();
            s.close();
        } catch (IOException e) {
        	LogUtils.logInfo("Error creating SSL context.", e); //$NON-NLS-1$
            return null;
        } catch (IllegalArgumentException e) {
        	LogUtils.logInfo("Error creating SSL context.", e); //$NON-NLS-1$
            return null;
        }
        return context;
    }   
}