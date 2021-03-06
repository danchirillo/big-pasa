/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2013. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * <p>Jazz HTTP client SLL protocol socket factory.</p>
 * 
 * <p>Note: Copied from <code>com.ibm.rqm.ct</code> plug-in.</p>
 *  
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 */
public class SSLProtocolSocketFactory implements SecureProtocolSocketFactory {

	public static final ProtocolSocketFactory INSTANCE = new SSLProtocolSocketFactory();
	
	private SSLContext sslContext = null ;
	
    private SSLProtocolSocketFactory() {
        super();
    }
    
    private synchronized SSLContext getSSLContext(){
    	if(sslContext == null){
			sslContext = SSLContextUtil.createSSLContext(new AcceptAllTrustManager());
    	}
    	return sslContext;
    }

    public Socket createSocket(
        String host,
        int port,
        InetAddress clientHost,
        int clientPort)
        throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(
            host,
            port,
            clientHost,
            clientPort
        );
    }

    public Socket createSocket(
        final String host,
        final int port,
        final InetAddress localAddress,
        final int localPort,
        final HttpConnectionParams params
    ) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (params == null) {
            //throw new IllegalArgumentException(Messages.parametersMayNotBeNull);
        }
        return createSocket(host, port, localAddress, localPort);
    }

    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(
            host,
            port
        );
    }

    public Socket createSocket(
        Socket socket,
        String host,
        int port,
        boolean autoClose)
        throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(
            socket,
            host,
            port,
            autoClose
        );
    }
    
    

    
}
