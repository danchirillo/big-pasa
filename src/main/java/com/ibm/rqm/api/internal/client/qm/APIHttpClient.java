/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2020. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.client.qm;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.rqm.api.internal.client.JazzHttpClient;
import com.ibm.rqm.api.internal.util.FeedReader;

/**
 * <p>Quality Management (QM) OSLC HTTP client.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 */
public final class APIHttpClient extends JazzHttpClient {

	private List<String> projectAreaAliases = null;

	public APIHttpClient(URL serverUrl) {                
		super(serverUrl);        
	}

	@Override
	public int login(String username, String password) throws IOException{	    

		int returnCode = super.login(username, password);

		if(projectAreaAliases == null){

			final String projectsType = "projects"; //$NON-NLS-1$
			
			final String projectsFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_FEED, new Object[]{getServerUrl(), projectsType});

			projectAreaAliases = FeedReader.getIds(this, projectsFeedUri, projectsType, false);	
		}

		return returnCode;
	}

	@Override
	public Map<String, String> getRequestHeaders() {

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put(HTTP_HEADER_ACCEPT, MEDIA_TYPE_APPLICATION_XML);
		requestHeaders.put(HTTP_HEADER_REFERER, getServerUrl());

		return requestHeaders;
	}

	public List<String> getProjectAreaAliases(){
		return projectAreaAliases;
	}
}
