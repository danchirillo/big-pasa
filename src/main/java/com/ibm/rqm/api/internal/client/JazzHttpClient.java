/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2020. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.protocol.Protocol;

import com.ibm.rqm.api.internal.util.APIUtils;
import com.ibm.rqm.api.internal.util.IAPIConstants;
import com.ibm.rqm.api.internal.util.LogUtils;

/**
 * <p>Jazz HTTP client.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 1.0
 * @since   0.9
 */
public abstract class JazzHttpClient extends HttpClient implements IAPIConstants {

	private String username;
	private String password;
	private URL serverUrl;
	private boolean isInitialized = false;
	// Adding configContext as an optional parameter to handle configurations (starting from 6.0.x version)
	private String configContext = null;

	private static final int MAX_RETRIES = 2;
	private static final int RETRY_DELAY = 3000; //3 seconds
	private final static String JAZZ_LOGOUT_URL = "service/com.ibm.team.repository.service.internal.ILogoutRestService"; //$NON-NLS-1$

	public abstract Map<String, String> getRequestHeaders();
	
	/**
	 * @param serverUrl
	 * @throws MalformedURLException
	 */
	protected JazzHttpClient(URL serverUrl){

		super();

		this.serverUrl = serverUrl;

		getParams().setParameter(HTTP_HEADER_SINGLE_COOKIE_HEADER, true);
	}

	public int relogin() throws IOException {
		return login();
	}

	protected int login(String user, String password) throws IOException {	    

		this.username = user;
		this.password = password;

		if(!isInitialized){

			try {
				Protocol.registerProtocol(PROTOCOL_HTTPS, new Protocol(PROTOCOL_HTTPS, TrustingSSLProtocolSocketFactory.getInstance(), 443));
			} 
			catch (GeneralSecurityException g) {
				throw new HttpClientException(g.getMessage());
			}

			isInitialized = true;
		}

		return login();
	}

	protected int login() throws IOException {

		GetMethod get = new GetMethod(getServerUrl() + "auth/authrequired"); //$NON-NLS-1$
		int responseCode = executeMethod(get);
		followRedirects(get, responseCode);
		get.releaseConnection();
		
		GetMethod get2 = new GetMethod(getServerUrl() + "authenticated/identity"); //$NON-NLS-1$
		responseCode = executeMethod(get2);
		followRedirects(get2, responseCode);
		get2.releaseConnection();
		
		HttpMethodBase authenticationMethod = null;
		Header authenticateHeader = get2.getResponseHeader("WWW-Authenticate"); //$NON-NLS-1$
		
		//Configure the HTTP client for basic authentication:
		if ((responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) && (authenticateHeader != null) && (authenticateHeader.getValue().toLowerCase().indexOf("basic realm") == 0)) { //$NON-NLS-1$
				
			HostConfiguration clientHostConfiguration = getHostConfiguration();

			super.getState().setCredentials(new AuthScope(clientHostConfiguration.getHost(), clientHostConfiguration.getPort()), new UsernamePasswordCredentials(username, password));

			authenticationMethod = new GetMethod(getServerUrl() + "/authenticated/identity"); //$NON-NLS-1$
			responseCode = super.executeMethod(get2); 	
		}
		
		//Configure the HTTP client for form authentication:
		else{

			authenticationMethod = new PostMethod(getServerUrl() + "j_security_check"); //$NON-NLS-1$

			fixMethodHeader(authenticationMethod);

			NameValuePair[] nvps = new NameValuePair[2];
			nvps[0] = new NameValuePair("j_username", username); //$NON-NLS-1$
			nvps[1] = new NameValuePair("j_password", password); //$NON-NLS-1$
			((PostMethod)(authenticationMethod)).addParameters(nvps);
			((PostMethod)(authenticationMethod)).addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8"); //$NON-NLS-1$ //$NON-NLS-2$

			responseCode = executeMethod(authenticationMethod);
			Header location = authenticationMethod.getResponseHeader(HTTP_HEADER_LOCATION);
			if (location!=null && location.getValue().indexOf("authfailed")>=0) { //$NON-NLS-1$
				responseCode = HttpURLConnection.HTTP_UNAUTHORIZED;
			}
		}
		
		if ((responseCode != HttpURLConnection.HTTP_OK) && (responseCode != HttpURLConnection.HTTP_MOVED_TEMP)) {
			String body = ""; //$NON-NLS-1$
			try {
				body = authenticationMethod.getResponseBodyAsString();
			} catch(Exception e) {

			}
			LogUtils.logError("login error (response code) - "+responseCode + LINE_SEPARATOR + "Response Body: "+body);			 //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			followRedirects(authenticationMethod, responseCode);
			GetMethod get3 = new GetMethod(getServerUrl() + "service/com.ibm.team.repository.service.internal.webuiInitializer.IWebUIInitializerRestService/initializationData"); //$NON-NLS-1$
			fixMethodHeader(get3);
			responseCode = executeMethod(get3);
			followRedirects(get3, responseCode);
			get3.releaseConnection();   
		}

		authenticationMethod.releaseConnection();

		return responseCode;
	}
	
	/**
	 * Logs out of the RQM server.
	 * @return The response code of the logout
	 */
	public int logout()
	{
		HttpMethodBase authenticationMethod = new PostMethod(getServerUrl() + JAZZ_LOGOUT_URL);
		authenticationMethod.setFollowRedirects(false);
		
		UsernamePasswordCredentials credentials = (UsernamePasswordCredentials)super.getState().getCredentials(new AuthScope(serverUrl.getHost(),serverUrl.getPort()));
		if (credentials != null && !(credentials.getUserName().isEmpty() && credentials.getPassword().isEmpty())) {
			authenticationMethod.addRequestHeader("Authorization:", "Base " + credentials.getUserName() + ":" + credentials.getPassword()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		String body = ""; //$NON-NLS-1$
		String status = ""; //$NON-NLS-1$
		int responseCode = 0;;
		try {
			responseCode = executeMethod(authenticationMethod);
			body = authenticationMethod.getResponseBodyAsString();
			status = authenticationMethod.getStatusText();
		} catch (Exception e) {
			LogUtils.logError("Log out error (response code) - " + responseCode + LINE_SEPARATOR + //$NON-NLS-1$
					"Status Code:" + status + LINE_SEPARATOR +  //$NON-NLS-1$
					"Response Body: " + body); //$NON-NLS-1$
		}
		
		return responseCode;
	}
	
	public void setConfigContext(String configContext) {
		this.configContext = configContext;
	}
	
	public String getConfigContext() {
		return this.configContext;
	}

	private void fixMethodHeader(HttpMethodBase method){
		
		Map<String, String> requestHeaders = getRequestHeaders();
			
		for(String name : requestHeaders.keySet()){
			method.setRequestHeader(name, requestHeaders.get(name));			
		}
    }

	private void followRedirects(HttpMethodBase method, int responseCode) throws IOException {
		Header location = method.getResponseHeader(HTTP_HEADER_LOCATION);
		while (location != null && responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
			GetMethod get3 = new GetMethod(location.getValue());
			try {				
				responseCode = executeMethod(get3);
				if (responseCode != HttpURLConnection.HTTP_OK){
					LogUtils.logTrace("after login, attempt to access init data returned: "+responseCode); //$NON-NLS-1$
				}
				location = get3.getResponseHeader(HTTP_HEADER_LOCATION);
			} 
			finally {
				get3.releaseConnection();           
			}
		} 		
	}

	private int retryableMethodExecution(HttpMethod m) throws IOException, HttpException {
		int retCode = -1;
		for (int currentTry = 0; currentTry < MAX_RETRIES; currentTry++) {
			try {
				retCode = executeMethod(m);

				if (m.getStatusCode() < 400) {
					break;
				} 
			} catch (IOException e) {
				LogUtils.logInfo(e.toString());
			}
			LogUtils.logInfo("Error received: " + m.getStatusCode() + ", retry #: " + currentTry); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				Thread.sleep(RETRY_DELAY);
			} catch (InterruptedException e) {
				// no-op
			}
		}
		return retCode;
	}

	private boolean checkForRelogin(HttpMethod m) throws IOException {
		boolean relogin = false;
		Header h = m.getResponseHeader(HTTP_HEADER_AUTHENTICATION);
		if (h != null && h.getValue() != null && h.getValue().equals(HTTP_HEADER_VALUE_AUTHENTICATION_REQUIRED)) {
			LogUtils.logInfo("Authentication expired, relogin required"); //$NON-NLS-1$
			relogin();
			relogin = true;
		}
		return relogin;
	}

	public int delete(String uri) throws IOException{
		return (delete(uri, null));
	}

	public int delete(String uri, String queryString) throws IOException{

		DeleteMethod delete = new DeleteMethod(uri);

		if(queryString != null){
			delete.setQueryString(queryString);
		}

		try{

			fixMethodHeader(delete);
			retryableMethodExecution(delete);
			if (checkForRelogin(delete)) {
				delete = new DeleteMethod(uri);
				fixMethodHeader(delete);
				retryableMethodExecution(delete);
				if (checkForRelogin(delete)) {
					delete = new DeleteMethod(uri);
					fixMethodHeader(delete);
					retryableMethodExecution(delete);
				}    
			}  
			return (delete.getStatusCode());
		}
		finally{
			delete.releaseConnection();
		}
	}
	
	public int head(String uri) throws IOException {
		return (head(uri, null));
	}
	
	public int head(String uri, String queryString) throws IOException{

		HeadMethod head = new HeadMethod(uri);

		if(queryString != null){
			head.setQueryString(queryString);
		}
		
		try{

			fixMethodHeader(head);
			retryableMethodExecution(head);
			if (checkForRelogin(head)) {
				head = new HeadMethod(uri);
				fixMethodHeader(head);
				retryableMethodExecution(head);
				if (checkForRelogin(head)) {
					head = new HeadMethod(uri);
					fixMethodHeader(head);
					retryableMethodExecution(head);
				}    
			}  
			return (head.getStatusCode());
		}
		finally{
			head.releaseConnection();
		}
	}

	public InputStream get(String uri) throws IOException{
		return (get(uri, null));
	}
	
	public InputStream get(String uri, String queryString) throws IOException{

		GetMethod get = new GetMethod(uri);

		if(queryString != null){
			get.setQueryString(queryString);
		}
		
		// If not null, set configContext for any potential REST API query
		if(this.configContext != null) {
			queryString = ((queryString != null && !queryString.isEmpty()) ? "&" : "") + MessageFormat.format(CONFIG_CONTEXT_PARAMETER, configContext); 
		}
		
		try{

			fixMethodHeader(get);
			retryableMethodExecution(get);
			if (checkForRelogin(get)) {
				get = new GetMethod(uri);
				fixMethodHeader(get);
				retryableMethodExecution(get);
				if (checkForRelogin(get)) {
					get = new GetMethod(uri);
					fixMethodHeader(get);
					retryableMethodExecution(get);
				}    
			}  
			int response = get.getStatusCode(); 
			if (response != HttpStatus.SC_OK && response != HttpStatus.SC_MOVED_TEMPORARILY) {
				throw new HttpClientException(get.getResponseBodyAsString(), "get(" + uri + ")", get.getStatusCode()); //$NON-NLS-1$ //$NON-NLS-2$
			}        

			return (APIUtils.copy(get.getResponseBodyAsStream()));
		}
		finally{
			get.releaseConnection();
		}
	}
	
	public String put(String uri, String xmlContent) throws IOException {
		return (put(uri, xmlContent, null));
	}

	public String put(String uri, String xmlContent, String queryString) throws IOException{

		PutMethod put = new PutMethod(uri);

		if(queryString != null){
			put.setQueryString(queryString);
		}
		
		// If not null, set configContext for any potential REST API query
		if(this.configContext != null) {
			queryString = ((queryString != null && !queryString.isEmpty()) ? "&" : "") + MessageFormat.format(CONFIG_CONTEXT_PARAMETER, configContext); 
		}

		try{

			fixMethodHeader(put);        
			StringRequestEntity xml = new StringRequestEntity(APIUtils.scrubXmlCharacters(xmlContent), MEDIA_TYPE_APPLICATION_XML, ENCODING_UTF8);        
			put.setRequestEntity(xml);        
			retryableMethodExecution(put);
			if (checkForRelogin(put)) {
				put = new PutMethod(uri);
				fixMethodHeader(put);
				put.setRequestEntity(xml);
				retryableMethodExecution(put);
			}        
			if (put.getStatusCode() != HttpStatus.SC_OK && put.getStatusCode() != HttpStatus.SC_CREATED) { 
				// Handle "see other" (redirect) responses.
				// Set the exception message to the URL of the target resource.
				// TER/EWI often returns this value to prevent duplicates
				if (put.getStatusCode() == HttpStatus.SC_SEE_OTHER) {
					Header seeOther = put.getResponseHeader(HTTP_HEADER_CONTENT_LOCATION);
					String seeOtherVal = null;
					if (seeOther != null && seeOther.getValue() != null) {
						seeOtherVal = seeOther.getValue();
					}
					throw new HttpClientException(seeOtherVal, "PUT", put.getStatusCode()); //$NON-NLS-1$
				} else {
					throw new HttpClientException(put.getResponseBodyAsString(), "PUT", put.getStatusCode());     //$NON-NLS-1$
				}            
			}        
			return uri;
		}
		finally{
			put.releaseConnection();
		}
	}

	public String post(String uri, String content, String contentType, String queryString) throws IOException{

		PostMethod post = new PostMethod(uri);

		if(queryString != null){
			post.setQueryString(queryString);
		}

		try{

			fixMethodHeader(post);        
			StringRequestEntity xml = new StringRequestEntity(APIUtils.scrubXmlCharacters(content), contentType, ENCODING_UTF8);        
			post.setRequestEntity(xml);        
			retryableMethodExecution(post);
			if (checkForRelogin(post)) {
				post = new PostMethod(uri);
				fixMethodHeader(post);
				post.setRequestEntity(xml);
				retryableMethodExecution(post);
			}        
			if (post.getStatusCode() != HttpStatus.SC_OK && post.getStatusCode() != HttpStatus.SC_CREATED) { 
				// Handle "see other" (redirect) responses.
				// Set the exception message to the URL of the target resource.
				// TER/EWI often returns this value to prevent duplicates
				if (post.getStatusCode() == HttpStatus.SC_SEE_OTHER) {
					Header seeOther = post.getResponseHeader(HTTP_HEADER_CONTENT_LOCATION);
					String seeOtherVal = null;
					if (seeOther != null && seeOther.getValue() != null) {
						seeOtherVal = seeOther.getValue();
					}
					throw new HttpClientException(seeOtherVal, "POST", post.getStatusCode()); //$NON-NLS-1$
				} else {
					throw new HttpClientException(post.getResponseBodyAsString(), "POST", post.getStatusCode());     //$NON-NLS-1$
				}            
			}        
			return post.getResponseBodyAsString();
		}
		finally{
			post.releaseConnection();
		}
	}
	public String postAttachment(String uri, byte[] attachmentBytes, String attachmentName) throws IOException{

		PostMethod post = new PostMethod(uri);
		
		// If not null, set configContext for any potential REST API query
		if(this.configContext != null) {
			post.addParameter(OSLC_CONFIG_PARAM_NAME, this.configContext);
		}

		try{

			fixMethodHeader(post);
			
			ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
			fileContent.write(attachmentBytes);
			ByteArrayPartSource baps = new ByteArrayPartSource(attachmentName, fileContent.toByteArray());
			FilePart filePart = new FilePart(attachmentName, baps);	
			post.setRequestEntity(new MultipartRequestEntity(new Part[]{ filePart }, post.getParams()));
			retryableMethodExecution(post);
			if (checkForRelogin(post)) {
				post = new PostMethod(uri);
				if(this.configContext != null) {
					post.addParameter(OSLC_CONFIG_PARAM_NAME, this.configContext);
				}
				fixMethodHeader(post);
				post.setRequestEntity(new MultipartRequestEntity(new Part[]{ filePart }, post.getParams()));
				retryableMethodExecution(post);
			}        
			if (post.getStatusCode() != HttpStatus.SC_OK && post.getStatusCode() != HttpStatus.SC_CREATED) { 
				// Handle "see other" (redirect) responses.
				// Set the exception message to the URL of the target resource.
				// TER/EWI often returns this value to prevent duplicates
				if (post.getStatusCode() == HttpStatus.SC_SEE_OTHER) {
					Header seeOther = post.getResponseHeader(HTTP_HEADER_CONTENT_LOCATION);
					String seeOtherVal = null;
					if (seeOther != null && seeOther.getValue() != null) {
						seeOtherVal = seeOther.getValue();
					}
					throw new HttpClientException(seeOtherVal, "POST", post.getStatusCode()); //$NON-NLS-1$
				} else {
					throw new HttpClientException(post.getResponseBodyAsString(), "POST", post.getStatusCode());     //$NON-NLS-1$
				}            
			}    
			else{

				Header locationHeader = post.getResponseHeader(HTTP_HEADER_CONTENT_LOCATION);

				if (locationHeader != null){
					
					String location = locationHeader.getValue();
					
					if((location != null) && (!location.trim().isEmpty())){
						return location;
					}
				}
			}
			return uri;
		}
		finally{
			post.releaseConnection();
		}
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * <p>Resolves the server URL with the trailing separator character.</p>
	 * 
	 * @return The server URL with the trailing separator character.
	 */
	public String getServerUrl(){
		return (serverUrl.toString());
	}
}