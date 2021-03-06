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

/**
 * <p>Jazz HTTP client exception.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 */
public class HttpClientException extends IOException {
   
	private int returnCode = -1;
    private String method = null;

	private static final long serialVersionUID = 1L;

    public HttpClientException(String message) {
        super(message);
    }

    public HttpClientException(String message, String method, int returnCode) {
       
    	super(message);
        
        this.method = method;
    	this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public String toString() {

    	StringBuffer buffer = new StringBuffer(HttpClientException.class.getSimpleName());
    	buffer.append(" executing method '"); //$NON-NLS-1$

    	if(method != null){    		
    		buffer.append(method);
    	}
    	else{
    		buffer.append("<unknown>"); //$NON-NLS-1$
    	}

    	buffer.append('\'');   	

    	if(returnCode != -1){

    		buffer.append(" (return code = "); //$NON-NLS-1$
    		buffer.append(returnCode);
    		buffer.append(')');   	
    	}

    	buffer.append(": ");   	 //$NON-NLS-1$
    	buffer.append(getMessage());

    	return (buffer.toString());
    }
}
