/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2012, 2020. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import com.ibm.rqm.api.internal.client.JazzHttpClient;

public class FeedReader {

	private static final String ID_END_TAG = "</id>"; //$NON-NLS-1$
	private static final String ID_TAG_NAME = "id"; //$NON-NLS-1$
	private static final String ENTRY_TAG_NAME = "entry"; //$NON-NLS-1$
	private static final String LINK_TAG_NAME = "link"; //$NON-NLS-1$
	private static final String HREF_ATTR = "href"; //$NON-NLS-1$
	private static final String LINK_REL_ATTR = "rel"; //$NON-NLS-1$
	private static final String LAST_ATTR_VAL = "last"; //$NON-NLS-1$
	private static final String PAGE_CGI_PARAM = "page="; //$NON-NLS-1$
	private static final String TOKEN_CGI_PARAM = "token="; //$NON-NLS-1$
	private static final Namespace ATOM_NAMESPACE = Namespace.getNamespace(IAPIConstants.NAMESPACE_URI_ATOM);
	private static final Namespace QM_NAMESPACE = Namespace.getNamespace(IAPIConstants.NAMESPACE_URI_ALM_QM);

	//Example: https://localhost:9443/jazz/service/com.ibm.rqm.integration.service.IIntegrationService/resources/Quality Manager/testplan?fields=feed/entry/content/testplan[title='test plan title']/*&token=_tlK6IRwWEeO8B8Ckgz4A6Q&page=5
	private static final String TOKEN_REQUEST_PARAMETER_REGULAR_EXPRESSION = "(.+[\\?\\&]{1}" + TOKEN_CGI_PARAM + ")(_[A-Za-z0-9-_]{22})(\\&?.*)"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String PAGE_REQUEST_PARAMETER_REGULAR_EXPRESSION = "(.+[\\?\\&]{1}" + PAGE_CGI_PARAM + ")([0-9]+)(\\&?.*)"; //$NON-NLS-1$ //$NON-NLS-2$
	
	private static Pattern tokenRequestParameterPattern = Pattern.compile(TOKEN_REQUEST_PARAMETER_REGULAR_EXPRESSION);
	private static Pattern pageRequestParameterPattern = Pattern.compile(PAGE_REQUEST_PARAMETER_REGULAR_EXPRESSION);

	public static enum Include {
		ACTIVE_ONLY, ARCHIVED_ONLY, PURGED_ONLY, ACTIVE_AND_ARCHIVED, ARCHIVED_AND_PURGED, ACTIVE_AND_ARCHIVED_AND_PURGED;

		public static boolean isArchived(Include include) {
			return ((include != null) && ((include.equals(ARCHIVED_ONLY)) || (include.equals(ACTIVE_AND_ARCHIVED)) || (include.equals(ARCHIVED_AND_PURGED)) || (include.equals(ACTIVE_AND_ARCHIVED_AND_PURGED))));
		}

		public static boolean isPurged(Include include) {
			return ((include != null) && ((include.equals(PURGED_ONLY)) || (include.equals(ARCHIVED_AND_PURGED)) || (include.equals(ACTIVE_AND_ARCHIVED_AND_PURGED))));
		}

		public static boolean isArchivedOrPurged(Include include) {
			return ((isArchived(include)) || (isPurged(include)));
		}

		public static boolean shouldInclude(Include include, boolean isArchived, boolean isPurged) {

			if(include != null) {

				if(include.equals(ACTIVE_ONLY)) {
					return ((!isArchived) && (!isPurged));
				}
				else if(include.equals(ARCHIVED_ONLY)) {
					return ((isArchived) && (!isPurged));					
				}				
				else if(include.equals(PURGED_ONLY)) {
					return (isPurged);										
				}
				else if(include.equals(ACTIVE_AND_ARCHIVED)) {
					return (!isPurged);										
				}
				else if(include.equals(ARCHIVED_AND_PURGED)) {
					return ((isArchived) || (isPurged));
				}
			}

			return true;
		}
	}
	
	public static List<String> getIds(JazzHttpClient client, String uri, String artifactType, boolean ignoreReadErrors) throws IOException {
		return (getIds(client, uri, artifactType, ignoreReadErrors, null));
	}
	
	public static List<String> getIds(JazzHttpClient client, String uri, String artifactType, boolean ignoreReadErrors, Include include) throws IOException {
		
		List<String> ids = new ArrayList<String>();
		
		List<Element> entries = getEntries(client, uri, artifactType, ignoreReadErrors, include);
		
		scanFeedForIds(entries, artifactType, ids);
		
		return ids;
	}

	public static List<Element> getEntries(JazzHttpClient client, String uri, String artifactType, boolean ignoreReadErrors) throws IOException {
		return (getEntries(client, uri, artifactType, ignoreReadErrors, null));
	}

	@SuppressWarnings("unchecked")
	public static List<Element> getEntries(JazzHttpClient client, String uri, String artifactType, boolean ignoreReadErrors, Include include) throws IOException {

		List<Element> entries = new ArrayList<Element>();
		String xml = null;

		String queryString = null;
		
		if(Include.isArchivedOrPurged(include)) {
			queryString = "includeArchived=true"; //$NON-NLS-1$
		}

		try{

			SAXBuilder xmlIn = new SAXBuilder();

			LogUtils.logTrace("Reading feed '" + uri + (APIUtils.isSet(queryString) ? ("?" + queryString) : "") + "' for artifact type '" + artifactType + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			InputStream feed = client.get(uri, queryString);  
			xml = APIUtils.toString(feed);
			Document feedDoc = xmlIn.build(new ByteArrayInputStream(xml.getBytes()));			
			List<Element> feedDocEntries = feedDoc.getRootElement().getChildren(ENTRY_TAG_NAME, ATOM_NAMESPACE);
			if((feedDocEntries != null) && (!feedDocEntries.isEmpty())){
				
				for(Element entry : feedDocEntries) {
					
					boolean archived = false;
					boolean purged = false;
					
					final Element archivedElement = entry.getChild("archived", QM_NAMESPACE); //$NON-NLS-1$
					
					if(archivedElement != null) {
						archived = Boolean.parseBoolean(archivedElement.getValue());
					}

					final Element purgedElement = entry.getChild("purged", QM_NAMESPACE); //$NON-NLS-1$
					
					if(purgedElement != null) {
						purged = Boolean.parseBoolean(purgedElement.getValue());
					}
					
					if(Include.shouldInclude(include, archived, purged)) {
						entries.add(entry);
					}
				}				
			}

			int lastPage = 0;
			String token = null;
			List<Element> links = feedDoc.getRootElement().getChildren(LINK_TAG_NAME, ATOM_NAMESPACE);
			for (Element link : links) {
				Attribute relAtt = link.getAttribute(LINK_REL_ATTR);
				if (relAtt != null && relAtt.getValue().equals(LAST_ATTR_VAL)) {
					
					//Example: https://localhost:9443/jazz/service/com.ibm.rqm.integration.service.IIntegrationService/resources/Quality Manager/testplan?fields=feed/entry/content/testplan[title='test plan title']/*&token=_tlK6IRwWEeO8B8Ckgz4A6Q&page=5
					String href = link.getAttribute(HREF_ATTR).getValue(); 

					Matcher tokenRequestParameterMatcher = tokenRequestParameterPattern.matcher(href);

					if(tokenRequestParameterMatcher.find()) {
						token = tokenRequestParameterMatcher.group(2);
					}
					else {
						LogUtils.logError("Missing token request parameter!"); //$NON-NLS-1$
					}            

					Matcher pageRequestParameterMatcher = pageRequestParameterPattern.matcher(href);

					if(pageRequestParameterMatcher.find()) {
						lastPage = Integer.valueOf(pageRequestParameterMatcher.group(2));
					}
					else {
						LogUtils.logError("Missing page request parameter!"); //$NON-NLS-1$
					}            
				}
			}

			// skip the page we already fetched initially
			for (int page=1; page<=lastPage; page++) {
				try {
				feed = client.get(uri, (APIUtils.isSet(queryString) ? (queryString + "&") : "") + TOKEN_CGI_PARAM + token + '&' + PAGE_CGI_PARAM + page); //$NON-NLS-1$ //$NON-NLS-2$
				if (feed != null && feed.available() > 0) { 
					xml = APIUtils.toString(feed);
					feedDoc = xmlIn.build(new ByteArrayInputStream(xml.getBytes()));
					feedDocEntries = feedDoc.getRootElement().getChildren(ENTRY_TAG_NAME, ATOM_NAMESPACE);
					if((feedDocEntries != null) && (!feedDocEntries.isEmpty())){
						
						for(Element entry : feedDocEntries) {
							
							boolean archived = false;
							boolean purged = false;
							
							final Element archivedElement = entry.getChild("archived", QM_NAMESPACE); //$NON-NLS-1$
							
							if(archivedElement != null) {
								archived = Boolean.parseBoolean(archivedElement.getValue());
							}

							final Element purgedElement = entry.getChild("purged", QM_NAMESPACE); //$NON-NLS-1$
							
							if(purgedElement != null) {
								purged = Boolean.parseBoolean(purgedElement.getValue());
							}
							
							if(Include.shouldInclude(include, archived, purged)) {
								entries.add(entry);
							}
						}			
					}  
				} else {
					LogUtils.logError("Empty feed page!"); //$NON-NLS-1$
				}
				}
				catch(Exception ex) {
					if(ignoreReadErrors){
						LogUtils.logError(ex.toString(), ex);							
					}
					else{
						throw ex;
					}
				}
			}
			LogUtils.logTrace("Done reading feed '" + uri + (APIUtils.isSet(queryString) ? ("?" + queryString) : "") + "' for artifact type '" + artifactType + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} 
		catch (IOException i) {

			if(ignoreReadErrors){
				LogUtils.logError(i.toString(), i);							
			}
			else{
				throw new IOException("Error reading feed '" + uri + (APIUtils.isSet(queryString) ? ("?" + queryString) : "") + "' for artifact type '" + artifactType + "'.", i); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		catch (Exception e) {

			if(xml != null){
				LogUtils.logError("Done reading feed '" + uri + (APIUtils.isSet(queryString) ? ("?" + queryString) : "") + "' for artifact type '" + artifactType + "': " + xml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			if(ignoreReadErrors){
				LogUtils.logError(e.toString(), e);							
			}
			else{
				throw new IOException("Error reading feed '" + uri + (APIUtils.isSet(queryString) ? ("?" + queryString) : "") + "' for artifact type '" + artifactType + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

		return entries;
	}    

	private static void scanFeedForIds(List<Element> entries, String artifactType, List<String> ids) {
		for (Element entry : entries) {
			Element idElem = entry.getChild(ID_TAG_NAME, ATOM_NAMESPACE);
			if (idElem != null) {
				String id = idElem.getValue();
				if (id != null && id.length() > 0) {
					//NOTE: request feed IDs are not correct
					if (!artifactType.equals("request")) { //$NON-NLS-1$
						int artifactIndex = id.indexOf("/" + artifactType + "/");                     //$NON-NLS-1$ //$NON-NLS-2$
						if (artifactIndex >= 0) {
							id = id.substring(artifactIndex + artifactType.length() + 2);
						}
					} else {
						int artifactIndex = id.indexOf(artifactType);                    
						if (artifactIndex >= 0) {
							id = id.substring(artifactIndex + artifactType.length() + 1);
						}                        
					}
					id = id.replaceAll(ID_END_TAG, "");  //$NON-NLS-1$
					ids.add(id);
				}
			}
		}        
	}
}
