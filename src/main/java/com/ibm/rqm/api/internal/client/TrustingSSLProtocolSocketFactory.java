/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * <p>Jazz HTTP client trusting SLL protocol socket factory.</p>
 * 
 * <p>Note: Copied from <code>com.ibm.rqm.ct</code> plug-in.</p>
 *  
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 */
public class TrustingSSLProtocolSocketFactory implements SecureProtocolSocketFactory, ProtocolSocketFactory {

	private SSLContext sslcontext = null;
	private static TrustingSSLProtocolSocketFactory instance;

	public static TrustingSSLProtocolSocketFactory getInstance() throws GeneralSecurityException {
		if (instance == null) {
			instance = new TrustingSSLProtocolSocketFactory();
		}
		return instance;
	}

	private TrustingSSLProtocolSocketFactory() throws GeneralSecurityException {
		super();

		// Create a trust manager that does not validate certificate chains
		TrustManager trustAllCerts = new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		};
		sslcontext = SSLContextUtil.createSSLContext(trustAllCerts);
	}

	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return sslcontext.getSocketFactory().createSocket(host, port);
	}

	public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException, UnknownHostException {
		return sslcontext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
	}

	public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {

		int timeout = params.getConnectionTimeout();
		SocketFactory socketfactory = sslcontext.getSocketFactory();
		if (timeout == 0) {
			return socketfactory.createSocket(host, port, localAddress, localPort);
		} else {
			Socket socket = socketfactory.createSocket();
			SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
			SocketAddress remoteaddr = new InetSocketAddress(host, port);
			socket.bind(localaddr);
			socket.connect(remoteaddr, timeout);
			return socket;
		}
	}
	
	public SSLContext getSSLContext() {
		return sslcontext;
	}
	
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose)throws IOException, UnknownHostException {
		return getSSLContext().getSocketFactory().createSocket(socket,host,	port, autoClose);
	}

}
