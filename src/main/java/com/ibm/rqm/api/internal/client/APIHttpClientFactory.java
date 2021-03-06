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
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

import org.apache.commons.httpclient.HttpStatus;

import com.ibm.rqm.api.internal.client.qm.APIHttpClient;
import com.ibm.rqm.api.internal.util.IAPIConstants;

/**
 * <p>OSLC HTTP client factory.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 */
public class APIHttpClientFactory implements IAPIConstants{
    
	private static HashMap<URL, APIHttpClient> clients = new HashMap<URL, APIHttpClient>();
    
    public static APIHttpClient getClient(URL serverUrl, String username, String password) throws IOException{        
        
    	APIHttpClient client = null;
        
    	if (clients.containsKey(serverUrl)) {
            client = clients.get(serverUrl);
        } 
    	else {
        	
    		//Resolve the context root (https://<server>:<port>/<context root>/):
    		String contextRoot = serverUrl.getPath().replaceAll(FORWARD_SLASH, " ").trim().toLowerCase(Locale.ENGLISH); //$NON-NLS-1$
    		
        	//Assumption: 'qm' and 'jazz' context roots default to QM.
        	if((CONTEXT_ROOT_JAZZ.equals(contextRoot)) || (CONTEXT_ROOT_QM.equals(contextRoot))){ 
        		client = new APIHttpClient(serverUrl);
        	}
        	
        	int returnCode = client.login(username, password);

			if ((returnCode != HttpStatus.SC_OK) && (returnCode != HttpStatus.SC_MOVED_TEMPORARILY)) {
				throw new IOException("Error logging into server '" + serverUrl + "'. Return Code: " + returnCode); //$NON-NLS-1$ //$NON-NLS-2$
			}

			clients.put(serverUrl, client);
        }
    	
        return client;
    }
}
