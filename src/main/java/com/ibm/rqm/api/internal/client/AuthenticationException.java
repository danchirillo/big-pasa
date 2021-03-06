/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.client;

/**
 * <p>Jazz HTTP client authentication exception.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 */
public class AuthenticationException extends HttpClientException {

	private static final long serialVersionUID = 1L;

	public AuthenticationException(String message) {
        super(message);
    }
	
	public AuthenticationException(String message, String method, int returnCode) {
		super(message, method, returnCode);
	}
}