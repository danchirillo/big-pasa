/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2020. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.rqm.api.APIUtility;
import com.ibm.rqm.api.APIUtility.CmdLineArg;
import com.ibm.rqm.api.internal.client.HttpClientException;
import com.ibm.rqm.api.internal.client.JazzHttpClient;
import com.ibm.rqm.api.internal.util.APIUtils;
import com.ibm.rqm.api.internal.util.DateTimeUtils;
import com.ibm.rqm.api.internal.util.FeedReader;
import com.ibm.rqm.api.internal.util.FeedReader.Include;
import com.ibm.rqm.api.internal.util.IAPIConstants;
import com.ibm.rqm.api.internal.util.LogUtils;


/**
 * <p>Utilities for working with IBM Rational Quality Manager resources using the IBM Rational Quality Manager Reportable REST API (see https://jazz.net/wiki/bin/view/Main/RqmApi).</p>
 * 
 * <p>For more information, see <code>readme.txt</code>.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 1.0
 * @since   0.9
 */
public final class APIUtilities implements IAPIConstants {

	private final JazzHttpClient httpClient;
	private final List<String> projectAreaAliases;
	private final PrintStream resourcesPrintStream;
	private final String queryString;
	private List<String> resourceWebIds;
	private final List<String> remoteScriptTypeNames;
	private final String adapterId;
	private final boolean output;
	private final boolean test;
	private final boolean ignoreReadErrors;
	private String projectAreaAliasNames = null;
	private long longCreationDate = -1;
	private final List<String> executionStates;
	private int executionProgress = -1;
	private final List<String> resultStates;
	private final List<String> resourceTypes;
	private final int count;
	private final String sectionId;
	private final String sectionName;

	private static final Pattern READ_ALL_RESOURCES_COMMAND_PATTERN = Pattern.compile("readAll([a-zA-Z]+)Resources"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final Pattern READ_ALL_RESOURCES_HISTORY_COMMAND_PATTERN = Pattern.compile("readAll([a-zA-Z]+)ResourcesHistory"); //$NON-NLS-1$ //$NON-NLS-2$

	private static final String QMROOTNAMESPACE = "http://jazz.net/xmlns/alm/qm/v0.1/"; 

	private static final Map<String, String> REMOTE_SCRIPT_TYPE_NAME_IDS = new HashMap<String, String>();
	
	private static final Pattern INTEGRATION_SERVICE_URL_PATTERN = Pattern.compile("(.*" + Pattern.quote(INTEGRATION_SERVICE_RESOURCES_URL) + "([^/]*/)?[^/]*)/(.*)"); //$NON-NLS-1$ //$NON-NLS-2$

	private final static Pattern INLINE_IMAGE_PATTERN = Pattern.compile("(<\\s*img[^>]*src\\s*=\\s*[\"'])data:image/([^;]+);([^,]+),([^\"']+)([\"'][^>]*>)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	static{
	     
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-APPSCAN-APTR-TYPE-NAME", "com.ibm.rqm.appscan.common.scripttype.ase"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-CMD-APTR-TYPE-NAME", "com.ibm.rqm.adapter.commandline"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-RFT-APTR-TYPE-NAME", "com.ibm.rqm.adapter.rft"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-ROBOT-APTR-TYPE-NAME", "com.ibm.rqm.executionframework.common.scripttype.robot"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-RPT-APTR-TYPE-NAME", "com.ibm.rqm.executionframework.common.scripttype.rpt"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-RIT-APTR-TYPE-NAME", "com.ghc.ghTester.rqm.execution.web.type"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-RTW-APTR-TYPE-NAME", "com.ibm.rqm.executionframework.common.scripttype.rtw"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-TRT-APTR-TYPE-NAME", "com.ibm.rqm.adapter.testrt"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-SEL-APTR-TYPE-NAME", "com.ibm.rqm.adapter.selenium"); //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE_NAME_IDS.put("RQM-KEY-RPT-SERVICE-APTR-TYPE-NAME", "com.ibm.rqm.executionframework.common.scripttype.rst"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public APIUtilities(JazzHttpClient httpClient, List<String> projectAreaAliases, PrintStream resourcesPrintStream, String queryString, List<String> resourceWebIds, List<String> remoteScriptTypeNames, String adapterId, boolean output, boolean test, boolean ignoreReadErrors, long longCreationDate, List<String> executionStates, int executionProgress, List<String> resultStates, List<String> resourceTypes, String sectionId, String sectionName, int count){

		this.httpClient = httpClient;
		this.projectAreaAliases = projectAreaAliases;
		this.resourcesPrintStream = resourcesPrintStream;
		this.queryString = queryString;
		this.resourceWebIds = resourceWebIds;
		this.remoteScriptTypeNames = remoteScriptTypeNames;
		this.adapterId = adapterId;
		this.output = output;
		this.test = test;
		this.ignoreReadErrors = ignoreReadErrors;
		this.longCreationDate = longCreationDate;
		this.executionStates = executionStates;
		this.executionProgress = executionProgress;
		this.resultStates = resultStates;
		this.resourceTypes = resourceTypes;
		this.count = count;
		this.sectionId =sectionId;
		this.sectionName = sectionName;
	}

	public void run(String command) throws Exception{

		System.out.println("Starting " + (test ? "test " : "") + "command '" + command + "' in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$

		Matcher readAllResourcesCommandMatcher = READ_ALL_RESOURCES_COMMAND_PATTERN.matcher(command);
		Matcher readAllResourcesHistoryCommandMatcher = READ_ALL_RESOURCES_HISTORY_COMMAND_PATTERN.matcher(command);
		
		if (readAllResourcesCommandMatcher.matches()) {		
			
			String resourceType = readAllResourcesCommandMatcher.group(1); 
			
			XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());           

			//Write the XML declaration:
			resourcesPrintStream.println(MessageFormat.format(XML_DECLARATION_ENCODING, ENCODING_UTF8));

			//Write the ATOM feed:
			resourcesPrintStream.println("<feed xmlns=\"http://www.w3.org/2005/Atom\">"); //$NON-NLS-1$
			resourcesPrintStream.print("<title type=\"text\">"); //$NON-NLS-1$
			resourcesPrintStream.print(resourceType);
			resourcesPrintStream.print(" ATOM feed for project area"); //$NON-NLS-1$
				
			if(projectAreaAliases.size() == 1){

				resourcesPrintStream.print(" "); //$NON-NLS-1$
				resourcesPrintStream.print(projectAreaAliases.get(0));
			}
			else{
				
				resourcesPrintStream.print("s "); //$NON-NLS-1$
				
				for (int counter = 0; counter < projectAreaAliases.size(); counter++) {
					
					if(counter > 0){
						resourcesPrintStream.print(", "); //$NON-NLS-1$
					}

					resourcesPrintStream.print(projectAreaAliases.get(counter));			
				}
			}
			
			resourcesPrintStream.println("</title>"); //$NON-NLS-1$
			resourcesPrintStream.print("<id>"); //$NON-NLS-1$
			
			if(projectAreaAliases.size() == 1){
				resourcesPrintStream.print(MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), resourceType}));
			}
			else{
				resourcesPrintStream.print(MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_ALL_PROJECTS_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), resourceType}));
			}
			
			resourcesPrintStream.println("</id>"); //$NON-NLS-1$
			resourcesPrintStream.println("<link href=\"" + httpClient.getServerUrl() + "web/console/\" rel=\"alternate\"/>"); //$NON-NLS-1$ //$NON-NLS-2$

			resourcesPrintStream.print("<link rel=\"self\" href=\""); //$NON-NLS-1$
			
			if(projectAreaAliases.size() == 1){
				resourcesPrintStream.print(MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), resourceType + "?" + queryString})); //$NON-NLS-1$
			}
			else{
				resourcesPrintStream.print(MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_ALL_PROJECTS_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), resourceType + "?" + queryString})); //$NON-NLS-1$
			}
		
			resourcesPrintStream.println("\"/>"); //$NON-NLS-1$
			    
			int totalResourceCount = 0;
			
			for (String projectAreaAlias : projectAreaAliases) {

				int projectAreaResourceCount = 0;

				if(output){
					System.out.println("Running command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}

				String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType + "?" + queryString});

				List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, resourceType, ignoreReadErrors);	
				
				for (String resourceId : resourceIds) {

					String resourceUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType, resourceId});
					try {
						String resourceXml = APIUtils.toString(httpClient.get(resourceUri, queryString));
						
						if((resourceXml != null) && (resourceXml.length() > 0)){
						
							SAXBuilder saxBuilder = new SAXBuilder();
							
							Document document = saxBuilder.build(new ByteArrayInputStream(resourceXml.trim().getBytes()));
	
							//Format the XML:
							String formattedResourceXml = xmlOutputter.outputString(document).trim();

							//Remove the XML declaration:
							//Note: Formatting includes the XML declaration.
							int xmlDeclarationEndIndex = formattedResourceXml.indexOf("?>"); //$NON-NLS-1$
	
							if(xmlDeclarationEndIndex != -1){
								formattedResourceXml = formattedResourceXml.substring(xmlDeclarationEndIndex + 2).trim(); 
							}

							resourcesPrintStream.println("<entry xmlns=\"http://www.w3.org/2005/Atom\">"); //$NON-NLS-1$
							resourcesPrintStream.print("<id>"); //$NON-NLS-1$
							
							Element identifierElement = document.getRootElement().getChild(PROPERTY_IDENTIFIER, Namespace.getNamespace(NAMESPACE_URI_DC_ELEMENTS));
							
							if(identifierElement != null){
								resourcesPrintStream.print(identifierElement.getValue());
							}
	
							resourcesPrintStream.println("</id>"); //$NON-NLS-1$
							resourcesPrintStream.print("<title type=\"text\">"); //$NON-NLS-1$
	
							Element titleElement = document.getRootElement().getChild(PROPERTY_TITLE, Namespace.getNamespace(NAMESPACE_URI_DC_ELEMENTS));
	
							if(titleElement != null){
								resourcesPrintStream.print(titleElement.getValue());
							}
	
							resourcesPrintStream.println("</title>"); //$NON-NLS-1$
							resourcesPrintStream.println("<summary type=\"text\"></summary>"); //$NON-NLS-1$
							resourcesPrintStream.print("<updated>"); //$NON-NLS-1$
	
							Element updatedElement = document.getRootElement().getChild(PROPERTY_UPDATED, Namespace.getNamespace(ALM_NAMESPACE));
	
							if(updatedElement != null){
								resourcesPrintStream.print(updatedElement.getValue());
							}
	
							resourcesPrintStream.println("</updated>"); //$NON-NLS-1$
							resourcesPrintStream.print("<link href=\""); //$NON-NLS-1$
							resourcesPrintStream.print(MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), resourceType, resourceId}));
							resourcesPrintStream.println("\" rel=\"alternate\" type=\"application/xml\"></link>"); //$NON-NLS-1$
							resourcesPrintStream.println("<content type=\"application/xml\">"); //$NON-NLS-1$
							resourcesPrintStream.println(formattedResourceXml.trim());
							resourcesPrintStream.println("</content>"); //$NON-NLS-1$
							resourcesPrintStream.println("</entry>"); //$NON-NLS-1$
	
							projectAreaResourceCount++;
						}
						else{
							System.out.println("Could not read resource '" + resourceUri + "'."); //$NON-NLS-1$ //$NON-NLS-2$ 			
						}
					}
					catch(Exception ex) {
						if(ignoreReadErrors) {
							String output_str = "Unable to get resource using: " + resourceUri; //$NON-NLS-1$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
						}
						else {
							throw ex;
						}
					}
				}				

				if(output){
					System.out.println("Read " + projectAreaResourceCount + " " + resourceType + " resource" + (projectAreaResourceCount != 1 ? "s" : "") + " in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
				}

				totalResourceCount += projectAreaResourceCount;
			}
			
			//Write the ATOM feed:
			resourcesPrintStream.println("</feed>"); //$NON-NLS-1$

			if(output){
				System.out.println("Read " + totalResourceCount + " " + resourceType + " resource" + (totalResourceCount != 1 ? "s" : "") + " in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
			}
		}
		else if(readAllResourcesHistoryCommandMatcher.matches()) {
			
			String resourceType = readAllResourcesHistoryCommandMatcher.group(1); 
						
			XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());           

			int totalResourceCount = 0;
			
			for (String projectAreaAlias : projectAreaAliases) {

				int projectAreaResourceCount = 0;

				if(output){
					System.out.println("Running command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}

				String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType + "?" + queryString}); //$NON-NLS-1$

				List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, resourceType, ignoreReadErrors);	
				
				for (String resourceId : resourceIds) {

					String resourceUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType, resourceId});

					String resourceHistoryUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE_HISTORY, new Object[] {httpClient.getServerUrl(), projectAreaAlias, resourceType, resourceId});

					try {

						String resourceHistoryXml = APIUtils.toString(httpClient.get(resourceHistoryUri));
						
						if((resourceHistoryXml != null) && (resourceHistoryXml.length() > 0)){
							
							//Insert a link (resource URI for reference) into the ATOM feed before the first entry:
							int entryIndex = resourceHistoryXml.indexOf("<entry>");
							
							if(entryIndex != -1) {
								resourceHistoryXml = (resourceHistoryXml.substring(0, entryIndex) + "<link href=\"" + resourceUri + "\"/>" + resourceHistoryXml.substring(entryIndex)); //$NON-NLS-1$ //$NON-NLS-2$
							}

							//Format the XML:
							SAXBuilder saxBuilder = new SAXBuilder();

							Document resourceHistoryDocument = saxBuilder.build(new ByteArrayInputStream(resourceHistoryXml.getBytes()));
							
							String formattedResourceHistoryXml = xmlOutputter.outputString(resourceHistoryDocument).trim();

							//Remove the XML declaration:
							//Note: Formatting includes the XML declaration.
							int xmlDeclarationEndIndex = formattedResourceHistoryXml.indexOf("?>"); //$NON-NLS-1$
	
							if(xmlDeclarationEndIndex != -1){
								formattedResourceHistoryXml = formattedResourceHistoryXml.substring(xmlDeclarationEndIndex + 2).trim(); 
							}

							resourcesPrintStream.println(formattedResourceHistoryXml);
							
							projectAreaResourceCount++;
						}
						else{
							System.out.println("Unable to resolve resource history '" + resourceHistoryUri + "'."); //$NON-NLS-1$ //$NON-NLS-2$ 			
						}
					}
					catch(Exception ex) {

						if(ignoreReadErrors) {

							String output_str = "Unable to resolve resource history '" + resourceHistoryUri + "'."; //$NON-NLS-1$ //$NON-NLS-2$ 

							if(output) {
								System.out.println(output_str); 
							}

							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
						}
						else {
							throw ex;
						}
					}
				}				

				if(output){
					System.out.println("Read " + projectAreaResourceCount + " " + resourceType + " resource" + (projectAreaResourceCount != 1 ? "s" : "") + " history in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
				}

				totalResourceCount += projectAreaResourceCount;
			}
			
			//Note: Do NOT repeat the same output message is there is only one project area.
			if((output) && (projectAreaAliases.size() > 1)){
				System.out.println("Read " + totalResourceCount + " " + resourceType + " resource" + (totalResourceCount != 1 ? "s" : "") + " history in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
			}
		}
		else if("findprojectswithrequirements".equals(command.toLowerCase())){ //$NON-NLS-1$

			Map<String, Integer> foundProjectAreaAliases = new HashMap<String, Integer>();

			for (String projectAreaAlias : projectAreaAliases) {

				if(output){
					System.out.println("Running command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}

				String requirementType = "requirement"; //$NON-NLS-1$
				
				String requirementsFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, requirementType});

				int requirementCount = 0;
				
				List<String> requirementsIds = FeedReader.getIds(httpClient, requirementsFeedUri, requirementType, ignoreReadErrors);	
				
				for (String requirementsId : requirementsIds) {
					
					String lowerCaseRequirementsId = requirementsId.toLowerCase();
					
					if((!lowerCaseRequirementsId.contains("reqpro/") && (!lowerCaseRequirementsId.contains("reqprohttps/")) && (!lowerCaseRequirementsId.contains("doors/")))){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-1$
						requirementCount++;
					}
				}				
				
				if(requirementCount > 0){
					foundProjectAreaAliases.put(projectAreaAlias, requirementCount);
				}
			}

			System.out.println("Summary:"); //$NON-NLS-1$
			System.out.println("    Found " + foundProjectAreaAliases.size() + " project area" + (foundProjectAreaAliases.size() == 1 ? "." : "s with requirements.")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			if(foundProjectAreaAliases.size() > 0){

				System.out.println("    Project area alias" + (foundProjectAreaAliases.size() == 1 ? ":" : "es:")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				for (String foundProjectAreaAlias : foundProjectAreaAliases.keySet()) {
					
					int requirementCount = foundProjectAreaAliases.get(foundProjectAreaAlias);
					
					System.out.println("        " + foundProjectAreaAlias + " (" + requirementCount + " requirement" + (requirementCount == 1 ? "" : "s") + ')'); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				}
			}
		}
		else if("completeexecutiontasks".equals(command.toLowerCase())){//$NON-NLS-1$
			if(((resourceWebIds == null) || (resourceWebIds.isEmpty())) &&
			   (longCreationDate == -1) &&
			   ((executionStates == null) || (executionStates.isEmpty())) &&
			   (executionProgress == -1) &&
			   ((resultStates == null) || (resultStates.isEmpty()))){
				throw new IllegalArgumentException((test ? "Test c" : "C") + "ommand '" + command + "' requires one (or more) valid resource web ids or creation date or execution task states or execution progress or execution result states"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			
			// Command doesn't support multiple projectAreaAlias, if resource web ids are provided
			if((projectAreaAliases.size() > 1) && ((resourceWebIds != null) && (!resourceWebIds.isEmpty()))){
				throw new IllegalArgumentException("Command '" + command + "' can only work on one project area, if resource web ids are provided"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			int projectAreaResourceCount = 0;
			List<String> completeExeTaskMessages = new ArrayList<String>();
			for (String projectAreaAlias : projectAreaAliases) {
				
				if(output){
					System.out.println("Running " + (test ? "test " : "") + "command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				}
				
				List<String> executionTaskIds = new ArrayList<String>();
				
				if(resourceWebIds != null && !resourceWebIds.isEmpty()) {
					executionTaskIds = new ArrayList<String>(resourceWebIds);
				}
				else {
					// Get all execution task ids
					String tasksType = "tasks"; //$NON-NLS-1$
					
					String tasksFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, tasksType});

					executionTaskIds = FeedReader.getIds(httpClient, tasksFeedUri, tasksType, ignoreReadErrors);
				}
			
				for(String resourceId : executionTaskIds){
					// The FeedReader returns the resource id as "urn:com.ibm.rqm:tasks:21". So split resourceId by ":" and assign last part to it.
					String[] parts = resourceId.split(":"); //$NON-NLS-1$
					resourceId = ((parts != null) && (parts.length > 0)) ? parts[parts.length-1] : resourceId;
					
					String taskUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, "tasks", "urn:com.ibm.rqm:tasks:" + resourceId}); //$NON-NLS-1$ //$NON-NLS-2$

					if(output){
						System.out.println("Using " + taskUri + " to get execution request id: " + resourceId);  //$NON-NLS-1$ //$NON-NLS-2$
					}
					try {
						String taskXml = APIUtils.toString(httpClient.get(taskUri));
						if((taskXml != null ) && (taskXml.length()>0)){
							taskXml = taskXml.trim();
							
							SAXBuilder saxBuilder = new SAXBuilder();
							Document document = saxBuilder.build(new ByteArrayInputStream(taskXml.getBytes()));
	
							Element taskcurrState = document.getRootElement().getChild(PROPERTY_STATE, Namespace.getNamespace(ALM_NAMESPACE));						
							String taskcurrStateString = taskcurrState == null ? "null" : taskcurrState.getText(); //$NON-NLS-1$
							if(output){
								System.out.println("Execution request id: '" + resourceId + "' has state: [" + taskcurrStateString + "].  It will be changed to com.ibm.rqm.executionframework.common.requeststate.complete"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							}
							
							boolean processTask = true;
							// check creation date
							if(longCreationDate != -1) {
								Element taskcurrCDate = document.getRootElement().getChild(PROPERTY_CREATION_DATE, Namespace.getNamespace(NAMESPACE_URI_ALM_QM));
								long creationDate = taskcurrCDate != null ? DateTimeUtils.parseDateTime(taskcurrCDate.getText()) : -1;
								if(creationDate == -1 || creationDate >= longCreationDate) {
									processTask = false;
								}
							}
							
							// check execution task state
							if(processTask && executionStates != null && !executionStates.isEmpty()) {
								boolean foundState = false;
								for(String state: executionStates) {
									if(state.equals(taskcurrStateString)) {
										foundState = true;
										break;
									}
								}
								if(!foundState) {
									processTask = false;
								}
							}
							
							// check execution task id
							if(processTask && (resourceWebIds != null) && !resourceWebIds.isEmpty() && !resourceWebIds.contains(resourceId)) {
								processTask = false;
							}
	
							// check execution progress
							if(processTask && (executionProgress != -1)) {
								Element taskcurrProgress = document.getRootElement().getChild(PROPERTY_PROGRESS, Namespace.getNamespace(NAMESPACE_URI_ALM_QM_ADAPTER_TASK));
								int taskProgress = taskcurrProgress != null ? Integer.parseInt(taskcurrProgress.getText()) : -1;
								if(taskProgress != executionProgress) {
									processTask = false;
								}
							}
							
							// check execution result state
							if(processTask && resultStates != null && !resultStates.isEmpty()) {
								boolean foundState = false;
								Element resultElement = document.getRootElement().getChild("resultURL", Namespace.getNamespace(NAMESPACE_URI_ALM_QM_ADAPTER_TASK)); //$NON-NLS-1$
								if (resultElement != null) {
									String resultUri = resultElement.getAttributeValue("href"); //$NON-NLS-1$
									if(output){
										System.out.println("Using " + resultUri + " to get execution result for request id: " + resourceId);  //$NON-NLS-1$ //$NON-NLS-2$
									}
									try {
										String resultXml = APIUtils.toString(httpClient.get(resultUri));
										if((resultXml != null) && (resultXml.length() > 0)) {
											resultXml = resultXml.trim();
											Document resultDocument = saxBuilder.build(new ByteArrayInputStream(resultXml.getBytes()));
											Element resultStateElement = resultDocument.getRootElement().getChild(PROPERTY_STATE, Namespace.getNamespace(ALM_NAMESPACE));
											if(resultStateElement != null) {
												String resultState = resultStateElement.getText();
												for(String state: resultStates) {
													if(state.equals(resultState)) {
														foundState = true;
														break;
													}
												}
											}
										}
									}
									catch(Exception ex) {
										if(ignoreReadErrors) {
											String output_str = "Unable to get execution result content using: " + resultUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
											if(output) {
												System.out.println(output_str); 
											}
											LogUtils.logTrace(output_str);
											LogUtils.logError(ex.toString(), ex);
											continue;
										}
										else {
											throw ex;
										}
									}
								}
								if(!foundState) {
									processTask = false;
								}
							}
							
							if(processTask) {
								LogUtils.logTrace("Before " + (test ? "test " : "") + "changing state for task id '" + resourceId + "' from [" + taskcurrStateString + "] to complete " + LINE_SEPARATOR + taskXml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
								
								taskcurrState.setText("com.ibm.rqm.executionframework.common.requeststate.complete"); //$NON-NLS-1$
								
								XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());           
								
								String newTaskXml = xmlOutputter.outputString(document).trim();
		
								LogUtils.logTrace("After " + (test ? "test " : "") + "changing state for task id '" + resourceId + "' from [" + taskcurrStateString + "] to complete " + LINE_SEPARATOR + newTaskXml);  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		
								if(!test){
									httpClient.put(taskUri, newTaskXml);
								}
								
								//Capture the output message:
								completeExeTaskMessages.add((test ? "Test c" : "C") + "omplete execution task ID '" + resourceId + "' change state from [" + taskcurrStateString + "] to complete '" + taskUri + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		
								projectAreaResourceCount++;
							}
						}
					}
					catch(Exception ex) {
						if(ignoreReadErrors) {
							String output_str = "Unable to get execution task content using: " + taskUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
						}
						else {
							throw ex;
						}
					}
				}
			}
			
			if(output){

				System.out.println("Summary:"); //$NON-NLS-1$
				System.out.println("    " + (test ? "Test r" : "R") + "ead " + projectAreaResourceCount + " task resource" + (projectAreaResourceCount != 1 ? "s" : "") + " in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$

				for(String outputMsg : completeExeTaskMessages){
					System.out.println("    " + outputMsg); //$NON-NLS-1$
				}
			}

		}
		else if("addmissingadapterid".equals(command.toLowerCase())){ //$NON-NLS-1$

			if((remoteScriptTypeNames == null) || (remoteScriptTypeNames.isEmpty())){
				throw new IllegalArgumentException((test ? "Test c" : "C") + "ommand '" + command + "' requires one (or more) valid remote script types"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}

			if((adapterId == null) || (adapterId.trim().isEmpty())){
				throw new IllegalArgumentException((test ? "Test c" : "C") + "ommand '" + command + "' requires a valid adapter web ID"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			
			//Support both remote script type name and ID:
			List<String> remoteScriptTypes = new ArrayList<String>();
			
			for(String remoteScriptTypeName : remoteScriptTypeNames){
				
				remoteScriptTypes.add(remoteScriptTypeName);
				
				if(REMOTE_SCRIPT_TYPE_NAME_IDS.containsKey(remoteScriptTypeName)){
					remoteScriptTypes.add(REMOTE_SCRIPT_TYPE_NAME_IDS.get(remoteScriptTypeName));
				}
			}
			
			String resourceType = "remotescript"; //$NON-NLS-1$

			int totalResourceCount = 0;

			List<String> addedMissingAdapterIdMessages = new ArrayList<String>();
			
			for (String projectAreaAlias : projectAreaAliases) {

				int projectAreaResourceCount = 0;
				
				if(output){
					System.out.println("Running " + (test ? "test " : "") + "command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				}
				
				String remoteScriptFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType});

				List<String> remoteScriptIds = FeedReader.getIds(httpClient, remoteScriptFeedUri, resourceType, ignoreReadErrors);	
				
				for (String remoteScriptId : remoteScriptIds) {

					String remoteScriptUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType, remoteScriptId});
					try {
						String remoteScriptXml = APIUtils.toString(httpClient.get(remoteScriptUri, queryString));
						
						if((remoteScriptXml != null) && (remoteScriptXml.length() > 0)){
	
							remoteScriptXml = remoteScriptXml.trim();
							
							SAXBuilder saxBuilder = new SAXBuilder();
							
							Document document = saxBuilder.build(new ByteArrayInputStream(remoteScriptXml.trim().getBytes()));
	
							Element remoteScriptWebId = document.getRootElement().getChild(PROPERTY_WEB_ID, Namespace.getNamespace(NAMESPACE_URI_ALM_QM));
	
							if((resourceWebIds == null) || (resourceWebIds.isEmpty()) || ((remoteScriptWebId != null) && (resourceWebIds.contains(remoteScriptWebId.getValue())))){
							
								Element remoteScriptType = document.getRootElement().getChild(PROPERTY_TYPE, Namespace.getNamespace(NAMESPACE_URI_ALM_QM));
								
								if((remoteScriptType != null) && (remoteScriptTypes.contains(remoteScriptType.getValue()))){
									
									Element remoteScriptManagedAdapter = document.getRootElement().getChild(PROPERTY_MANAGED_ADAPTER, Namespace.getNamespace(NAMESPACE_URI_ALM_QM));
									
									if((remoteScriptManagedAdapter != null) && (Boolean.parseBoolean(remoteScriptManagedAdapter.getValue()))){
	
										Element remoteScriptAdapterId = document.getRootElement().getChild(PROPERTY_ADAPTER_ID, Namespace.getNamespace(NAMESPACE_URI_ALM_QM));
										
										if((remoteScriptAdapterId == null) || (!APIUtils.isSet(remoteScriptAdapterId.getValue()))){
	
											//Back-up the old remote script:
											LogUtils.logTrace("Before " + (test ? "test " : "") + "adding missing adapter ID '" + adapterId + "' to " + resourceType + " resource '" + remoteScriptUri + "':" + LINE_SEPARATOR + remoteScriptXml);  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	
											document.getRootElement().removeChild(PROPERTY_ADAPTER_ID, Namespace.getNamespace(NAMESPACE_URI_ALM_QM));
											
											remoteScriptAdapterId = new Element(PROPERTY_ADAPTER_ID, Namespace.getNamespace(NAMESPACE_URI_ALM_QM));
											remoteScriptAdapterId.setText(adapterId);
											
											document.getRootElement().addContent(remoteScriptAdapterId);
											
											XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());           
											
											String newRemoteScriptXml = xmlOutputter.outputString(document).trim();
	
											//Back-up the new remote script:
											LogUtils.logTrace("After " + (test ? "test " : "") + "adding missing adapter ID '" + adapterId + "' to " + resourceType + " resource '" + remoteScriptUri + "':" + LINE_SEPARATOR + newRemoteScriptXml);  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	
											if(!test){
												httpClient.put(remoteScriptUri, newRemoteScriptXml);
											}
											
											//Capture the output message:
											addedMissingAdapterIdMessages.add((test ? "Test a" : "A") + "dded missing adapter ID '" + adapterId + "' to " + resourceType + " resource '" + remoteScriptUri + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
										}
									}
								}
							}
				
							projectAreaResourceCount++;
						}
						else{
							System.out.println("Could not " + (test ? "test " : "") + "read resource '" + remoteScriptUri + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 			
						}
					}
					catch(Exception ex) {
						if(ignoreReadErrors) {
							String output_str = "Unable to get remote script content using: " + remoteScriptUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
						}
						else {
							throw ex;
						}
					}
				}			
				
				if(output){
					System.out.println((test ? "Test r" : "R") + "ead " + projectAreaResourceCount + " " + resourceType + " resource" + (projectAreaResourceCount != 1 ? "s" : "") + " in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
				}

				totalResourceCount += projectAreaResourceCount;
			}
			
			if(output){

				System.out.println("Summary:"); //$NON-NLS-1$
				System.out.println("    " + (test ? "Test r" : "R") + "ead " + totalResourceCount + " " + resourceType + " resource" + (totalResourceCount != 1 ? "s" : "") + " in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$

				for(String addedMissingBackLinksOutputMessage : addedMissingAdapterIdMessages){
					System.out.println("    " + addedMissingBackLinksOutputMessage); //$NON-NLS-1$
				}
			}
		}
		else if ("autoassigndefaultscript".equals(command.toLowerCase())) { //$NON-NLS-1$
			
			final String serverUrl = httpClient.getServerUrl();

			for (String projectAreaAlias : projectAreaAliases) {
				if (output) {
					System.out.println("Running command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
	
				String testSuiteType = "testsuite"; // $NON-NLS-1$ //$NON-NLS-1$
				String testSuitesFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[] { serverUrl, projectAreaAlias, testSuiteType });

				List<String> suiteIds = null;
				
				try {
					suiteIds = FeedReader.getIds(httpClient, testSuitesFeedUri, testSuiteType, ignoreReadErrors);	
				}
				catch(Exception ex) {
					if(ignoreReadErrors) {
						String output_str = "Unable to get test suites feed from project: " + projectAreaAlias + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
						if(output) {
							System.out.println(output_str); 
						}
						LogUtils.logTrace(output_str);
						LogUtils.logError(ex.toString(), ex);
						continue;
					}
					else {
						throw ex;
					}
				}				
				
				final Namespace NS_ALM_QM = Namespace.getNamespace(NAMESPACE_URI_ALM_QM);
				SAXBuilder saxBuilder = new SAXBuilder();
				XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
				
				for (String suiteId : suiteIds) {
					String suiteUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[] { serverUrl, projectAreaAlias, testSuiteType, encodeSegmentedResourceId(suiteId) });
					String suiteXml = null;
					Document suiteDocument = null;
					
					try {
						suiteXml = APIUtils.toString(httpClient.get(suiteUri));
						suiteDocument = saxBuilder.build(new ByteArrayInputStream(suiteXml.trim().getBytes()));
					}
					catch(Exception ex) {
						if(ignoreReadErrors) {
							String output_str = "Unable to get test suite content using: " + suiteUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
							continue;
						}
						else {
							throw ex;
						}
					}
					
					boolean updated = false;
					
					Element suiteElementsElement = suiteDocument.getRootElement().getChild("suiteelements", NS_ALM_QM); // $NON-NLS-1$ //$NON-NLS-1$
					@SuppressWarnings("unchecked")
					List<Element> suiteElements = suiteElementsElement.getChildren();
					if (suiteElements != null && suiteElements.size() > 0) {
						for (Element suiteElement : suiteElements) {
							
							//If the suite element does not contain a child remote/test script, resolve the remote/test script associated with the test case:
							if ((suiteElement.getChild("remotescript", NS_ALM_QM) == null) && (suiteElement.getChild("testscript", NS_ALM_QM) == null)) { //$NON-NLS-1$ //$NON-NLS-2$
								Element testCaseElement = suiteElement.getChild("testcase", NS_ALM_QM);// $NON-NLS-1$ //$NON-NLS-1$
								if (testCaseElement != null) {
									String testCaseUri = encodeResouceName(testCaseElement.getAttributeValue("href")); //$NON-NLS-1$
									String testCaseXml = null;
									Document testCaseDocument = null;
									
									try {
										testCaseXml = APIUtils.toString(httpClient.get(testCaseUri));
										testCaseDocument = saxBuilder.build(new ByteArrayInputStream(testCaseXml.trim().getBytes()));
									}
									catch(Exception ex) {
										if(ignoreReadErrors) {
											String output_str = "Unable to get test case content using: " + testCaseUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
											if(output) {
												System.out.println(output_str); 
											}
											LogUtils.logTrace(output_str);
											LogUtils.logError(ex.toString(), ex);
											continue;
										}
										else {
											throw ex;
										}
									}

									Element remoteScriptElement = null;
									Element testScriptElement = null;

									@SuppressWarnings("unchecked")
									List<Element> remoteScripts = testCaseDocument.getRootElement().getChildren("remotescript", NS_ALM_QM); // $NON-NLS-1$ //$NON-NLS-1$

									//Use the first remote script:
									if ((remoteScripts != null) && (remoteScripts.size() > 0)) {
										remoteScriptElement = remoteScripts.get(0);
									}

									@SuppressWarnings("unchecked")
									List<Element> testScripts = testCaseDocument.getRootElement().getChildren("testscript", NS_ALM_QM); // $NON-NLS-1$ //$NON-NLS-1$

									//Use the first test script:
									if ((testScripts != null) && (testScripts.size() > 0)) {
										testScriptElement = testScripts.get(0);
									}

									//If the test case contains both child remote/test script(s), resolve the remote/test script added first to the test case based on the test case history:
									if((remoteScriptElement != null) && (testScriptElement != null)){

										long remoteScriptAddDateTime = -1;
										long testScriptAddDateTime = -1;
										
										Element testCaseWebIdElement = testCaseDocument.getRootElement().getChild("webId", NS_ALM_QM); // $NON-NLS-1$ //$NON-NLS-1$

										if (testCaseWebIdElement != null) {
							
											String testCaseHistoryUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE_HISTORY, new Object[] { serverUrl, projectAreaAlias, "testcase", "urn:com.ibm.rqm:testcase:" + testCaseWebIdElement.getValue()}); //$NON-NLS-1$ //$NON-NLS-2$
											
											List<Element> testCaseHistoryEntries = FeedReader.getEntries(httpClient, testCaseHistoryUri, "testcase", ignoreReadErrors); //$NON-NLS-1$
											
											remoteScriptAddDateTime = resolveTestScriptLastAddDateTime(testCaseHistoryEntries, remoteScriptElement.getAttributeValue("href")); //$NON-NLS-1$
											testScriptAddDateTime = resolveTestScriptLastAddDateTime(testCaseHistoryEntries, testScriptElement.getAttributeValue("href")); //$NON-NLS-1$
										}
										
										if(remoteScriptAddDateTime == -1){
											
											String message = "Unable to resolve the date/time remote script '" + remoteScriptElement.getAttributeValue("href") + "' was added to test case '" + testCaseUri + "' that is associated with test suite '" + suiteUri + "'. Skipping."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

											if(ignoreReadErrors) {
												
												if(output) {
													System.out.println(message); 
												}
												
												LogUtils.logTrace(message);
											}
											else {
												throw new Exception(message);
											}
											
											continue;
										}
										else if(testScriptAddDateTime == -1){
											
											String message = "Unable to resolve the date/time test script '" + testScriptElement.getAttributeValue("href") + "' was added to test case '" + testCaseUri + "' that is associated with test suite '" + suiteUri + "'. Skipping."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

											if(ignoreReadErrors) {
												
												if(output) {
													System.out.println(message); 
												}
												
												LogUtils.logTrace(message);
											}
											else {
												throw new Exception(message);
											}
											
											continue;
										}
										
										//The remote script was added first to the test case:
										if(remoteScriptAddDateTime < testScriptAddDateTime){
											testScriptElement = null;		
										}

										//The test script was added first to the test case:
										else if(testScriptAddDateTime < remoteScriptAddDateTime){
											remoteScriptElement = null;		
										}

										//The remote/test script were added to the test case at the same time, select the remote/test script that was created first (smallest web ID):
										else{
											
											int remoteScriptWebId = -1;
											int testScriptWebId = -1;

											//Resolve the remote script web ID:
											try {
												String remoteScriptXml = APIUtils.toString(httpClient.get(remoteScriptElement.getAttributeValue("href"))); //$NON-NLS-1$
												Document remoteScriptDocument = saxBuilder.build(new ByteArrayInputStream(remoteScriptXml.trim().getBytes()));
												remoteScriptWebId = Integer.parseInt(remoteScriptDocument.getRootElement().getChild("webId", Namespace.getNamespace(NAMESPACE_URI_ALM_QM)).getValue().trim()); //$NON-NLS-1$
											}
											catch(Exception ex) {
												if(ignoreReadErrors) {
													String output_str = "Unable to resolve the web ID of remote script '" + remoteScriptElement.getAttributeValue("href") + "'. Skipping."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-2$ //$NON-NLS-3$
													if(output) {
														System.out.println(output_str); 
													}
													LogUtils.logTrace(output_str);
													LogUtils.logError(ex.toString(), ex);
													continue;
												}
												else {
													throw ex;
												}
											}

											//Resolve the test script web ID:
											try {
												String testScriptXml = APIUtils.toString(httpClient.get(testScriptElement.getAttributeValue("href"))); //$NON-NLS-1$
												Document testScriptDocument = saxBuilder.build(new ByteArrayInputStream(testScriptXml.trim().getBytes()));
												testScriptWebId = Integer.parseInt(testScriptDocument.getRootElement().getChild("webId", Namespace.getNamespace(NAMESPACE_URI_ALM_QM)).getValue().trim()); //$NON-NLS-1$
											}
											catch(Exception ex) {
												if(ignoreReadErrors) {
													String output_str = "Unable to resolve the web ID of test script '" + testScriptElement.getAttributeValue("href") + "'. Skipping."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-2$ //$NON-NLS-3$
													if(output) {
														System.out.println(output_str); 
													}
													LogUtils.logTrace(output_str);
													LogUtils.logError(ex.toString(), ex);
													continue;
												}
												else {
													throw ex;
												}
											}
																						
											//The remote script was created before the test script:
											if(remoteScriptWebId < testScriptWebId){
												testScriptElement = null;		
											}

											//The test script was created before the remote script:
											else if(testScriptWebId < remoteScriptWebId){
												remoteScriptElement = null;		
											}
										}
									}

									//Remote script:
									if(remoteScriptElement != null){

										Element defaultRemoteScriptElement = new Element("remotescript", NS_ALM_QM); //$NON-NLS-1$
										defaultRemoteScriptElement.setAttribute("href", remoteScriptElement.getAttributeValue("href")); //$NON-NLS-1$ //$NON-NLS-2$

										suiteElement.addContent(defaultRemoteScriptElement);

										updated = true;
									}
									
									//Test script:
									else if(testScriptElement != null){

										Element defaultTestScriptElement = new Element("testscript", NS_ALM_QM); //$NON-NLS-1$
										defaultTestScriptElement.setAttribute("href", testScriptElement.getAttributeValue("href")); //$NON-NLS-1$ //$NON-NLS-2$

										suiteElement.addContent(defaultTestScriptElement);

										updated = true;
									}						
								}
							}
						}
					}
					
					if (updated) {
						LogUtils.logTrace("Before " + (test ? "test " : "") + "assigning default scripts to test suite '" + suiteUri + "':" + LINE_SEPARATOR + suiteXml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						
						suiteXml = xmlOutputter.outputString(suiteDocument).trim();
						
						LogUtils.logTrace("After " + (test ? "test " : "") + "assigning default scripts to test suite '" + suiteUri + "':" + LINE_SEPARATOR + suiteXml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						
						try {
							if (!test) {
								httpClient.put(suiteUri, suiteXml);
							}
						}
						catch(Exception ex) {
							String output_str = "Unable to update the test suite: " + suiteUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
						}						
					}
				}
			}
		}				
		else if ("repairmanualexecutionscripts".equals(command.toLowerCase())) { //$NON-NLS-1$
			final String serverUrl = httpClient.getServerUrl();
			String output_str = "";					 //$NON-NLS-1$
			PostMethod postMethod = null;
			String uri;
			int responseCode = 0;
			String projectAreaAlias = ""; //$NON-NLS-1$
			
			try {
				if (!test) {
					for(int i=0; i<this.projectAreaAliases.size(); i++)
					{
						projectAreaAlias = projectAreaAliases.get(i);
						uri = MessageFormat.format(URI_TEMPLATE_MANUALEXECUTIONSCRIPT_RESTSERVICE_REPAIRMANUALEXECUTIONSCRIPT, new Object[] { serverUrl, projectAreaAlias });	
						
						postMethod = new PostMethod(uri); //$NON-NLS-1$
						if(httpClient.getConfigContext() != null) {
							postMethod.addParameter(OSLC_CONFIG_PARAM_NAME, httpClient.getConfigContext());
						}
						// Script Web Ids can be included in order to repair certain specific scripts; the scripts need to belong to the project area indicated
						if(resourceWebIds != null && !resourceWebIds.isEmpty()) {
							int size = resourceWebIds.size();
							NameValuePair[] scriptIds = new NameValuePair[size];
							for(int j = 0; j < size; j++) {
								scriptIds[j] = new NameValuePair("webIdsToRepair", resourceWebIds.get(j));
							}
							postMethod.addParameters(scriptIds);
						}
						postMethod.addRequestHeader("user-agent", "rqm"); //$NON-NLS-1$ //$NON-NLS-2$
						
						responseCode = httpClient.executeMethod(postMethod);
						
						if ((responseCode != HttpURLConnection.HTTP_OK)) {
							output_str = "Unable to repair Manual TestScripts for ProjectArea " + projectAreaAlias;															 //$NON-NLS-1$
						}
						else{							
							output_str = "Successfully Repaired Manual TestScripts for ProjectArea " + projectAreaAlias; //$NON-NLS-1$
						}
						if(output) {
							System.out.println(output_str);									
						}
						LogUtils.logTrace(output_str + "      Response : "+postMethod.getResponseBodyAsString()); //$NON-NLS-1$
					}
				}
			}
			catch(Exception ex) {
				output_str = "Unable to repair Manual TestScripts for ProjectArea " + projectAreaAlias; //$NON-NLS-1$
				if(postMethod != null)
					output_str += "      Response : "+postMethod.getResponseBodyAsString(); //$NON-NLS-1$
				if(output) {
					System.out.println(output_str); 
				}
				LogUtils.logTrace(output_str);
				LogUtils.logError(ex.toString(), ex);
			}			
		}
		else if ("removeorphanrootiterations".equals(command.toLowerCase())) { //$NON-NLS-1$
			
			final String serverUrl = httpClient.getServerUrl();

			for (String projectAreaAlias : projectAreaAliases) {
				if (output) {
					System.out.println("Running command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}				
				
				//First we will get all the test phases and map them with the test plan
				String testPhaseType = "testphase"; // $NON-NLS-1$ //$NON-NLS-1$
				String testPhasesFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[] { serverUrl, projectAreaAlias, testPhaseType });

				HashMap<String, ArrayList<String>> orderedIterations = new HashMap<String, ArrayList<String>>();				
				List<String> phaseIds = null;
				
				try {					
					phaseIds = FeedReader.getIds(httpClient, testPhasesFeedUri, testPhaseType, ignoreReadErrors);					
				}
				catch(Exception ex) {
					if(ignoreReadErrors) {
						String output_str = "Unable to get test phases feed from project: " + projectAreaAlias + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
						if(output) {
							System.out.println(output_str); 
						}
						LogUtils.logTrace(output_str);
						LogUtils.logError(ex.toString(), ex);
						continue;
					}
					else {
						throw ex;
					}
				}		
				for(String phaseId:phaseIds){
					String phaseUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[] { serverUrl, projectAreaAlias, testPhaseType, encodeSegmentedResourceId(phaseId) });
					String phaseXml = null;
					Document phaseDocument = null;
					
					try {
						phaseXml = APIUtils.toString(httpClient.get(phaseUri));
						SAXBuilder saxBuilder = new SAXBuilder();
						phaseDocument = saxBuilder.build(new ByteArrayInputStream(phaseXml.trim().getBytes()));												
					}
					catch(Exception ex) {
						if(ignoreReadErrors) {
							String output_str = "Unable to get test phase content using: " + phaseXml + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
							continue;
						}
						else {
							throw ex;
						}						
					}
					Element phaseElement = phaseDocument.getRootElement();
					if(phaseElement!= null){							
						Element plan = phaseElement.getChild("testplan",Namespace.getNamespace(NAMESPACE_URI_ALM_QM)); //$NON-NLS-1$
						Element iteration = phaseElement.getChild("iteration",Namespace.getNamespace(NAMESPACE_URI_ALM_QM));												 //$NON-NLS-1$
						String planSrc = plan.getAttributeValue("href"); //$NON-NLS-1$
						String iterationHref = iteration.getAttributeValue("href"); //$NON-NLS-1$
						ArrayList<String> iterations = orderedIterations.get(planSrc);
						
						if(iterations == null){
							iterations = new ArrayList<String>();
						}
						iterations.add(iterationHref);						
						orderedIterations.put(planSrc, iterations);					
					}
				}
				
				//Now iterate through test plans
				String testPlanType = "testplan"; // $NON-NLS-1$				 //$NON-NLS-1$
				List<String> testPlans = null;
				String testPlansFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[] { serverUrl, projectAreaAlias, testPlanType });
				
				try {					
					 testPlans = FeedReader.getIds(httpClient, testPlansFeedUri, testPlanType, ignoreReadErrors);
				}catch(Exception ex) {
					if(ignoreReadErrors) {
						String output_str = "Unable to get test plan feed from project: " + projectAreaAlias + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
						if(output) {
							System.out.println(output_str); 
						}
						LogUtils.logTrace(output_str);
						LogUtils.logError(ex.toString(), ex);
						continue;
					}
					else {
						throw ex;
					}
				}		
				final Namespace NS_ALM_QM = Namespace.getNamespace(NAMESPACE_URI_ALM_QM);
				final Namespace NS_RDF =  Namespace.getNamespace(NAMESPACE_URI_RDF);
				SAXBuilder saxBuilder = new SAXBuilder();
				for (String planId : testPlans) {
					
					String planUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[] { serverUrl, projectAreaAlias, testPlanType, encodeSegmentedResourceId(planId) });
					String planResource = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[] { serverUrl, projectAreaAlias, testPlanType, planId });
					String planXml = null;
					Document planDocument = null;
					
					try {
						planXml = APIUtils.toString(httpClient.get(planUri));
						planDocument = saxBuilder.build(new ByteArrayInputStream(planXml.trim().getBytes()));												
					}
					catch(Exception ex) {
						if(ignoreReadErrors) {
							String output_str = "Unable to get test plan content using: " + planUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
							continue;
						}
						else {
							throw ex;
						}
					}
					Element parentIterationElement = planDocument.getRootElement().getChild("parentIteration", NS_ALM_QM); // $NON-NLS-1$ //$NON-NLS-1$
					if(parentIterationElement!= null){														
						String parentIterationResource = parentIterationElement.getAttributeValue("resource", NS_RDF); //$NON-NLS-1$
						ArrayList<String> iterations = orderedIterations.get(planResource); 
						if(iterations== null || !iterations.contains(parentIterationResource)){
							String output_str = "Orphan root iteration with no associated test phase found in Test Plan:"+ planId; //$NON-NLS-1$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							
							XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());																																			
							parentIterationElement.removeAttribute("resource", NS_RDF);														 //$NON-NLS-1$
							String finalPlanXml = xmlOutputter.outputString(planDocument).trim();
							
							LogUtils.logTrace("Test Plan XML Before removing parentIteration " + (test ? "test " : "")  + planXml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							LogUtils.logTrace("Test Plan XML After removing parentIteration " + (test ? "test " : "")  + finalPlanXml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							try {
								if (!test) {
									httpClient.put(planUri, finalPlanXml);
								}
							}catch(Exception ex) {
								output_str = "Unable to update the test plan: " + planUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
								if(output) {
									System.out.println(output_str); 
								}							
							}														
						} else {																					
							String output_str = "Test plan "+ planId + " does not contain orphan root Iterations"; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
						}
					}
				}				
			}
		}
		else if ("convertinlineimages".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Assumption: All resource types are used, if the -rt/-resourceTypes argument is not specified.
			int totalResourceCount = 0;
			int totalResourceProcessedCount = 0;

			List<String> convertInlineImagesMessages = new ArrayList<String>();

			//Iterate the project areas:
			for (String projectAreaAlias : projectAreaAliases) {

				int projectAreaResourceCount = 0;
				int projectAreaResourceProcessedCount = 0;

				if(output){
					System.out.println("Running " + (test ? "test " : "") + "command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				}

				//Iterate the resource types in the project area:
				for (String resourceType : resourceTypes) {

					String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType});

					List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, resourceType, ignoreReadErrors);	

					//Iterate the resources of the resource type in the project area:
					for (String resourceId : resourceIds) {

						String resourceUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType, resourceId});

						try {

							//Resolve the resource XML of the resource type in the project area:
							String resourceXml = APIUtils.toString(httpClient.get(resourceUri, queryString));

							Matcher inlineImageMatcher = INLINE_IMAGE_PATTERN.matcher(resourceXml);

							//Only process resources that contain inline images:
							if(inlineImageMatcher.find()){

								//Note: The matcher MUST be reset.
								inlineImageMatcher = inlineImageMatcher.reset();

								StringBuffer updatedResourceXml = new StringBuffer();

								//Iterate the inline images in the resource XML:
								while(inlineImageMatcher.find()){

									String inlineImageEncoding = inlineImageMatcher.group(3);

									if(ENCODING_BASE_64.equalsIgnoreCase(inlineImageEncoding)){

										String attachmentId = null;
										String attachmentUri = null;
										String attachmentUuid = null;
										String attachmentFileName = ("inline_image." + inlineImageMatcher.group(2)); //$NON-NLS-1$

										SAXBuilder saxBuilder = new SAXBuilder();

										Element imgElement = saxBuilder.build(new ByteArrayInputStream(inlineImageMatcher.group(0).getBytes())).getRootElement();

										if(!test){

											//Create an attachment from the inline image content:
											String attachmentPostUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, "attachment"}); //$NON-NLS-1$

											//Decode the (base64) inline image content:
											byte[] imageBytes = Base64.decodeBase64(inlineImageMatcher.group(4).getBytes(ENCODING_UTF8));

											attachmentId = httpClient.postAttachment(attachmentPostUri, imageBytes, attachmentFileName);

											attachmentUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, "attachment", attachmentId}); //$NON-NLS-1$

											try{

												//Resolve the attachment UUID:	
												//Note: Convert the InputStream to a String by calling APIUtils.toString() scrub the XML content. 
												String attachmentWebId = saxBuilder.build(new StringReader(APIUtils.toString(httpClient.get(attachmentUri, queryString)))).getRootElement().getChild("webId", Namespace.getNamespace(NAMESPACE_URI_ALM_QM)).getValue(); //$NON-NLS-1$

												if ((APIUtils.isSet(attachmentWebId)) && (Integer.parseInt(attachmentWebId) > 0)) {

													//Note: Convert the InputStream to a String by calling APIUtils.toString() scrub the XML content. 
													Document attachmentFeedDocument = saxBuilder.build(new StringReader(APIUtils.toString(httpClient.get(attachmentPostUri, "fields=feed/entry/content/attachment[webId='" + attachmentWebId + "']/*&metadata=UUID")))); //$NON-NLS-1$ //$NON-NLS-2$

													List<?> entries = attachmentFeedDocument.getRootElement().getChildren("entry", Namespace.getNamespace(NAMESPACE_URI_ATOM)); //$NON-NLS-1$

													if(entries.size() == 1){

														String entryId = ((Element)(entries.get(0))).getChild("id", Namespace.getNamespace(NAMESPACE_URI_ATOM)).getValue(); //$NON-NLS-1$

														if(APIUtils.isSet(entryId)){
															attachmentUuid = entryId;
														}
													}
												}
											}
											catch(Exception e){
												//Ignore since the attachment UUID cannot be resolved and log a warning message (see below).
											}

											if(attachmentUuid != null){

												attachmentId = attachmentUuid;

												attachmentUri = MessageFormat.format(URI_TEMPLATE_ATTACHMENT_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), attachmentId});
											}
											else{

												String message = "Could not resolve the UUID of attachment '" + attachmentUri + "' in " + resourceType + " resource '" + resourceUri + "'. By default, using a ETM Reportable REST API attachment ID and URL."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

												if(output) {
													System.err.println(message); 
												}

												LogUtils.logWarning(message);
											}
										}
										else{

											//Set a dummy attachment UUID for test mode:
											attachmentId = "_0000000000000000000000"; //$NON-NLS-1$
											attachmentUri = MessageFormat.format(URI_TEMPLATE_ATTACHMENT_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), attachmentId});
										}

										imgElement.setAttribute("src", attachmentUri); //$NON-NLS-1$

										if(!APIUtils.isAttributeSet(imgElement, "id")){ //$NON-NLS-1$
											imgElement.setAttribute("id", attachmentId);																		 //$NON-NLS-1$
										}

										if(!APIUtils.isAttributeSet(imgElement, "border")){ //$NON-NLS-1$
											imgElement.setAttribute("border", "0");																		 //$NON-NLS-1$ //$NON-NLS-2$
										}

										if(!APIUtils.isAttributeSet(imgElement, "alt")){ //$NON-NLS-1$
											imgElement.setAttribute("alt", attachmentFileName);																		 //$NON-NLS-1$
										}

										XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

										inlineImageMatcher.appendReplacement(updatedResourceXml, xmlOutputter.outputString(imgElement).trim());
									}
									else{		

										String message = "Unsupported inline image content encoding '" + inlineImageEncoding + "' in " + resourceType + " resource '" + resourceUri + "'. Note, only the '" + ENCODING_BASE_64 + "' inline image content encoding is supported. Skipping inline image."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

										if(output) {
											System.err.println(message); 
										}

										LogUtils.logWarning(message);
									}
								}

								inlineImageMatcher.appendTail(updatedResourceXml);

								//Back-up the old resource XML:
								LogUtils.logTrace("Before " + (test ? "test " : "") + "converting inline images in " + resourceType + " resource '" + resourceUri + "':" + LINE_SEPARATOR + resourceXml);  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

								if(!test){
									httpClient.put(resourceUri, updatedResourceXml.toString());
								}

								//Back-up the new resource XML:
								LogUtils.logTrace("After " + (test ? "test " : "") + "converting inline images in " + resourceType + " resource '" + resourceUri + "':" + LINE_SEPARATOR + updatedResourceXml.toString());  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

								//Capture the output message:
								convertInlineImagesMessages.add((test ? "Test c" : "C") + "onverted inline images in " + resourceType + " resource '" + resourceUri + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

								projectAreaResourceProcessedCount++;
							}

							projectAreaResourceCount++;
						}
						catch(Exception e) {

							String message = "Unable to " + (test ? "test " : "") + "convert inline images in " + resourceType + " resource '" + resourceUri + "'. Skipping resource."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

							if(output) {
								System.err.println(message); 
							}

							LogUtils.logError(message);
							LogUtils.logError(e.toString(), e);
						}
					}			

					if(output){

						System.out.println((test ? "Test r" : "R") + "ead " + projectAreaResourceCount + " resource" + (projectAreaResourceCount != 1 ? "s" : "") + " of resource type '" + resourceType + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
						System.out.println((test ? "Test p" : "P") + "rocessed " + projectAreaResourceProcessedCount + " resource" + (projectAreaResourceProcessedCount != 1 ? "s" : "") + " with inline images of resource type '" + resourceType + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
					}
				}

				if((output) && (resourceTypes.size() > 1)){

					System.out.println((test ? "Test r" : "R") + "ead " + projectAreaResourceCount + " resource" + (projectAreaResourceCount != 1 ? "s" : "") + " of resource type" + (resourceTypes.size() != 1 ? "s" : "") + " '" + APIUtils.toString(resourceTypes) + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$
					System.out.println((test ? "Test p" : "P") + "rocessed " + projectAreaResourceProcessedCount + " resource" + (projectAreaResourceProcessedCount != 1 ? "s" : "") + " with inline images of resource type" + (resourceTypes.size() != 1 ? "s" : "") + " '" + APIUtils.toString(resourceTypes) + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$
				}

				totalResourceCount += projectAreaResourceCount;
				totalResourceProcessedCount += projectAreaResourceProcessedCount;
			}

			if(output){

				System.out.println("Summary:"); //$NON-NLS-1$
				System.out.println("    " + (test ? "Test r" : "R") + "ead " + totalResourceCount + " resource" + (totalResourceCount != 1 ? "s" : "") + " of resource type" + (resourceTypes.size() != 1 ? "s" : "") + " '" + APIUtils.toString(resourceTypes) + "' in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
				System.out.println("    " + (test ? "Test p" : "P") + "rocessed " + totalResourceProcessedCount + " resource" + (totalResourceProcessedCount != 1 ? "s" : "") + " with inline images of resource type" + (resourceTypes.size() != 1 ? "s" : "") + " '" + APIUtils.toString(resourceTypes) + "' in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$

				for(String addedMissingBackLinksOutputMessage : convertInlineImagesMessages){
					System.out.println("    " + addedMissingBackLinksOutputMessage); //$NON-NLS-1$
				}
			}
		} 
		else if ("removehtmltagsfromscriptsteps".equals(command.toLowerCase())) { //$NON-NLS-1$
			
			final String serverUrl = httpClient.getServerUrl();
			String output_str = ""; //$NON-NLS-1$

			for (String projectAreaAlias : projectAreaAliases) {
				if (output) {
					System.out.println("Running command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				
				//First we will get all the manual test scripts
				String testScriptType = "testscript"; // $NON-NLS-1$ //$NON-NLS-1$
				String testScriptFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[] { serverUrl, projectAreaAlias, testScriptType });
				
				List<String> scriptIds = null;
				
				if(resourceWebIds != null && !resourceWebIds.isEmpty()) {
					scriptIds = resourceWebIds;
				}
				else {
					try {					
						scriptIds = FeedReader.getIds(httpClient, testScriptFeedUri, testScriptType, ignoreReadErrors);					
					}
					catch(Exception ex) {
						if(ignoreReadErrors) {
							output_str = "Unable to get test scripts feed from project: " + projectAreaAlias + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
							continue;
						}
						else {
							throw ex;
						}
					}
				}
				for(String scriptId:scriptIds){
					String encodedId = encodeSegmentedResourceId(scriptId);
					String scriptUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[] { serverUrl, projectAreaAlias, testScriptType, encodedId });
					String scriptXml = null;
					
					try {
						scriptXml = APIUtils.toString(httpClient.get(scriptUri));
						
						// Here the correcting code take place. For the specific customer, their data was showing either correct <br/> tags encoded,
						// or mal-formed tags of the form "&lt;br/gt;". This simple code corrects those errors coming from TM Migration.
						// Any other corrections can be added here.
						
						String finalTestScriptXml = scriptXml.replaceAll("&lt;br/&gt;", "<br/>").replaceAll("&lt;br/gt;", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						
						LogUtils.logTrace("Test Script XML Before removing HTML tags " + (test ? "test " : "")  + scriptXml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						LogUtils.logTrace("Test Script XML After removing HTML tags " + (test ? "test " : "")  + finalTestScriptXml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						try {
							if (!test) {
								// Saves the changes in the form of a PUT request
								httpClient.put(scriptUri, finalTestScriptXml);
								if (output) {
									System.out.println("Script with Id " + scriptId + "Saved."); //$NON-NLS-1$ //$NON-NLS-2$
								}
								LogUtils.logTrace("Script with Id " + scriptId + "Saved."); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}catch(Exception ex) {
							output_str = "Unable to update the test script: " + scriptUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
							if(output) {
								System.out.println(output_str); 
							}							
						}
					}
					catch(Exception ex) {
						if(ignoreReadErrors) {
							output_str = "Unable to get test script " + scriptId + " content using: " + scriptXml + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							if(output) {
								System.out.println(output_str); 
							}
							LogUtils.logTrace(output_str);
							LogUtils.logError(ex.toString(), ex);
							continue;
						}
						else {
							throw ex;
						}						
					}
				}
			}
		} 
		else if(COMMAND_UNIFY_CUSTOM_SECTIONS.toLowerCase().equals(command.toLowerCase())){
			
			String sectionNameToUnify = this.sectionName;
			String desiredSectionId = this.sectionId;
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document doc = null;
			XPath xPath = XPathFactory.newInstance().newXPath();
			int numModifiedTestCases = 0;
			int failedupdates = 0;
			
			for (String projectAreaAlias : projectAreaAliases) {
				String output_str ="Running command '" + command + "' in project area '" + projectAreaAlias + "'." ; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				if(output)
					System.out.println(output_str);
				LogUtils.logTrace(output_str);
				
				//Get the templates to 'fix' in this project area
				String resourceType = "template";
				String templateFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType});
				ArrayList<String> templateIdsToModify = new ArrayList<String>();
				
				String queryString ="abbreviate=false";
				String templatesXmlFeed = APIUtils.toString(httpClient.get(templateFeedUri, queryString));
				doc = builder.parse(new ByteArrayInputStream(templatesXmlFeed.getBytes()));
				
				String expression = "//entry/content/template/sections/section[@name='"+ sectionNameToUnify +"']";
				NodeList sections = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
				//for each section named sectionNameToUnify
				for(int c=0; c<sections.getLength(); c++){
					Node section = sections.item(c);
					String id = section.getAttributes().getNamedItem("id").getNodeValue();
					if (!id.equals(desiredSectionId)){
						//Id is not right so add template identifier to TemplatesToModify
						String templateIdentifier = null;
						String templateName = null;
						NodeList templateChildren = section.getParentNode().getParentNode().getChildNodes();
						for (int x=0; x<templateChildren.getLength(); x++){
							//got the field values, no need to keep iterating
							if( templateIdentifier != null && templateName != null){ 
								break;
							}
							Node child = templateChildren.item(x);
							if(child.getNodeName().contains("title")){
								templateName = child.getTextContent();
							}
							if(child.getNodeName().contains("identifier")){
								templateIdentifier = child.getTextContent();
							}
						}
						templateIdsToModify.add(templateIdentifier);
						output_str = "Template: '" + templateName + "' has a section named '"+ sectionNameToUnify + "' that needs to be updated. Template ID is: (" + templateIdentifier + ")." ; 
						if(output)
							System.out.println(output_str);
						LogUtils.logTrace(output_str);
					}
				}
				
				if (templateIdsToModify.size() < 1){
					//no templates to modify on this PA, skip
					output_str = "No templates to modify in this project area, skipping"; 
					if(output)
						System.out.println(output_str);
					LogUtils.logTrace(output_str);
					continue;
				}
				
				resourceType = "testcase";
				String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType});
				List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, resourceType, false);	
				
				//iterate through test cases
				for(String resourceId: resourceIds){
					String resourceUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType, resourceId});
					try {
						boolean shouldModifyTestCase = false;
						boolean missingTemplate = false;
						queryString ="";
						String resourceXml = APIUtils.toString(httpClient.get(resourceUri, queryString));
						resourceXml = resourceXml.trim();
						doc = builder.parse(new ByteArrayInputStream(resourceXml.getBytes()));
						//check the test case's template
						expression = "//testcase/template";
						Node nBasedOnTemplate = (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);
						if(nBasedOnTemplate == null){
							output_str = "Test Case '"+ resourceId +"' did not display its template, will check if it contains the section"; 
							if(output)
								System.out.println(output_str);
							LogUtils.logTrace(output_str);
							shouldModifyTestCase = true;
							missingTemplate = true;
						}else{
							String basedOnTemplate = ((org.w3c.dom.Element)nBasedOnTemplate).getAttribute("href");
							for(String tmp: templateIdsToModify){
								if(tmp.equals(basedOnTemplate)){
									shouldModifyTestCase = true;
								}
							}
						}
						if(shouldModifyTestCase){
							output_str = "Need to modify Testcase: " + resourceId; 
							if(output)
								System.out.println(output_str);
							LogUtils.logTrace(output_str);
							//does it have the section?
							expression = "//testcase/*[@extensionDisplayName='" + sectionNameToUnify +"']";
							Node sectionToModify = (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);
							if(sectionToModify != null){
								if(!sectionToModify.getNodeName().equals(desiredSectionId)){
									output_str = "Updating section ID: " + sectionToModify.getNodeName(); 
									if(output)
										System.out.println(output_str);
									LogUtils.logTrace(output_str);
									doc.renameNode(sectionToModify, "", desiredSectionId);
									
								}else{
									//This test case has already been edited, skip
									output_str = "Section ID already has desired value, skipping"; 
									if(output)
										System.out.println(output_str);
									LogUtils.logTrace(output_str);
									continue;
								}
							}else{
								if(missingTemplate){
									//if the test case did not display the template it doesn't mean the section should be there, so do not create it.
									output_str ="Testcase did not have the desired section, skipping";
									if(output)
										System.out.println(output_str);
									LogUtils.logTrace(output_str);
									continue;
								}
								//section should be there but was not retrieved, so should send it empty
								output_str = "Updating ID of empty section";
								if(output)
									System.out.println(output_str);
								LogUtils.logTrace(output_str);
								org.w3c.dom.Element docElem = doc.getDocumentElement();
								org.w3c.dom.Element sectionToAdd = doc.createElement(desiredSectionId);
								sectionToAdd.setAttribute("extensionDisplayName", sectionNameToUnify);
								sectionToAdd.setAttribute("xmlns", QMROOTNAMESPACE);
								sectionToAdd.setTextContent(" ");
								docElem.appendChild(sectionToAdd);
							}
							//PUT Test Case
							String modifiedTestCase = transformDocToString(doc);
							if(!test){
								httpClient.put(resourceUri, modifiedTestCase);
								output_str = "SUCCESS! - modified test case: " + resourceUri;
								if(output)
									System.out.println(output_str);
								LogUtils.logTrace(output_str);
								numModifiedTestCases++;
							}
						}
					}
					catch(HttpClientException e){
						failedupdates++;
						output_str = "ERROR: COULD NOT UPDATE test case: " + resourceUri + " possibly because of permissions on project " + projectAreaAlias;
						LogUtils.logError(output_str);
						System.out.println(output_str);
					}
					catch(IOException e){
						failedupdates++;
						output_str = "ERROR: COULD NOT UPDATE test case: " + resourceUri + " possibly because of an invalid character " + projectAreaAlias;
						LogUtils.logError(output_str);
						System.out.println(output_str);
					}
					catch(Exception ex) {
						failedupdates++;
						output_str = "ERROR: COULD NOT UPDATE test case: " + resourceUri + " unknown cause " + projectAreaAlias;
						LogUtils.logError(output_str);
						System.out.println(output_str);
					}
				}//end for each test case
			}//end for each project area
			String output_str = command + " results: " + numModifiedTestCases + " testcases updated " + failedupdates + " failures";
			if(output)
				System.out.println(output_str);
			LogUtils.logTrace(output_str);
		} 
		else if(COMMAND_UNIFY_CUSTOM_SECTIONS_2.toLowerCase().equals(command.toLowerCase())){
			
			String sectionNameToUnify = this.sectionName;
			String desiredSectionId = this.sectionId;
			int numTemplatesModified = 0;
			int numTemplatesToModify = 0;
			
			ArrayList<String> templateIdsToModify = new ArrayList<String>();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document doc = null;
			XPath xPath =  XPathFactory.newInstance().newXPath();
			String output_str = "";
			
			for (String projectAreaAlias : projectAreaAliases) {
				
				output_str = "Running command '" + command + "' in project area '" + projectAreaAlias + "'."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				if(output)
					System.out.println(output_str);
				LogUtils.logTrace(output_str);
				//Get the templates to 'fix' in this project area
				boolean templatesToModifyInThisPA = false;
				
				String resourceType = "template";
				String templateFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType});
								
				String queryString ="abbreviate=false";
				String templatesXmlFeed = APIUtils.toString(httpClient.get(templateFeedUri, queryString));
				doc = builder.parse(new ByteArrayInputStream(templatesXmlFeed.getBytes()));
				
				String expression = "//entry/content/template/sections/section[@name='"+ sectionNameToUnify +"']";
				NodeList sections = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
				//for each section named sectionNameToUnify
				for(int c=0; c<sections.getLength(); c++){
					Node section = sections.item(c);
					String id = section.getAttributes().getNamedItem("id").getNodeValue();
					if (!id.equals(desiredSectionId)){
						//Id is not right so add template identifier to TemplatesToModify
						String templateIdentifier = null;
						String templateName = null;
						NodeList templateChildren = section.getParentNode().getParentNode().getChildNodes();
						for (int x=0; x<templateChildren.getLength(); x++){
							//got the field values, no need to keep iterating
							if( templateIdentifier != null && templateName != null){ 
								break;
							}
							Node child = templateChildren.item(x);
							if(child.getNodeName().contains("title")){
								templateName = child.getTextContent();
							}
							if(child.getNodeName().contains("identifier")){
								templateIdentifier = child.getTextContent();
							}
						}
						templateIdsToModify.add(templateIdentifier);
						output_str = "Template: '" + templateName + "' has a section named '"+ sectionNameToUnify + "' that needs to be updated. Template ID is: (" + templateIdentifier + ")." ;
						if(output)
							System.out.println(output_str);
						LogUtils.logTrace(output_str);
						numTemplatesToModify++;
						templatesToModifyInThisPA = true;
					}
				}
				
				if (!templatesToModifyInThisPA){
					//no templates to modify on this PA, skip
					output_str = "No templates to modify in this project area, skipping";
					if(output)
						System.out.println(output_str);
					LogUtils.logTrace(output_str);
					continue;
				}
			}//end for each project area
			output_str = "Starting update of " + numTemplatesToModify + " templates";
			if(output)
				System.out.println(output_str);
			LogUtils.logTrace(output_str);
			for(String tempToModify: templateIdsToModify){
				output_str = "Modifying template: '" + tempToModify + "'";
				if(output)
					System.out.println(output_str);
				LogUtils.logTrace(output_str);
				String templateXmlFeed = APIUtils.toString(httpClient.get(tempToModify, ""));
				doc = builder.parse(new ByteArrayInputStream(templateXmlFeed.getBytes()));
				String expression = "//template/sections/section[@name='"+ sectionNameToUnify +"']";
				org.w3c.dom.Element sectionToModify = (org.w3c.dom.Element) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);
				//should have it, but check for null just in case
				if(sectionToModify == null){
					output_str = "Template did not have a section named: " + sectionNameToUnify;
					if(output)
						System.out.println(output_str);
					LogUtils.logTrace(output_str);
					continue;
				}
				
				String currentSectionId = sectionToModify.getAttribute("id");
				if(!currentSectionId.equals(desiredSectionId)){
					System.out.println("Changing section id from '" + currentSectionId + "' to '" + desiredSectionId + "'.");
					sectionToModify.setAttribute("id", desiredSectionId);
					String modifiedTemplate = transformDocToString(doc);
					if(!test){
						httpClient.put(tempToModify, modifiedTemplate);
						output_str = "SUCCESS! - updated template: " + tempToModify;
						if(output)
							System.out.println(output_str);
						LogUtils.logTrace(output_str);
						numTemplatesModified++;
					}
				}else{
					output_str = "Template's section already had the right section id, skipping.";
					if(output)
						System.out.println(output_str);
					LogUtils.logTrace(output_str);
					continue;
				}
			}//end for each temp to modify
			output_str = command + " results: " + numTemplatesModified + "/" + numTemplatesToModify + " templates updated";
			if(output)
				System.out.println(output_str);
			LogUtils.logTrace(output_str);
			
		}
		else if ("permanentlydelete".equals(command.toLowerCase())) { //$NON-NLS-1$

			final List<String> supportedResourceTypes = new ArrayList<String>();
			supportedResourceTypes.add("executionresult"); //$NON-NLS-1$

			//Validate the supported resource types:
			//Assumption: All resource types are used, if the -rt/-resourceTypes argument is not specified.
			if(resourceTypes.equals(Arrays.asList(APIUtility.SUPPORTED_RESOURCE_TYPES))) {
				throw new IllegalArgumentException((test ? "Test c" : "C") + "ommand '" + command + "' requires the " + CmdLineArg.RESOURCE_TYPES.toString() + " argument containing only supported resource types.  Supported resource types: " + APIUtils.toString(supportedResourceTypes)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}
			
			List<String> unsupportedResourceTypes = new ArrayList<String>(resourceTypes);
			unsupportedResourceTypes.removeAll(supportedResourceTypes);

			if(!unsupportedResourceTypes.isEmpty()) {
				throw new IllegalArgumentException((test ? "Test c" : "C") + "ommand '" + command + "' does not support resource type" + (unsupportedResourceTypes.size() != 1 ? "s" : "") + ": " + APIUtils.toString(unsupportedResourceTypes) + ".  Supported resource types: " + APIUtils.toString(supportedResourceTypes)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}

			int totalPermanentlyDeletedResourceCount = 0;

			List<String> permanentlyDeletedMessages = new ArrayList<String>();

			//Iterate the project areas:
			for (String projectAreaAlias : projectAreaAliases) {

				int projectAreaPermanentlyDeletedResourceCount = 0;

				if(output){
					System.out.println("Running " + (test ? "test " : "") + "command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				}

				//Iterate the resource types in the project area:
				for (String resourceType : resourceTypes) {
					
					if(output){
						System.out.println("Running " + (test ? "test " : "") + "command '" + command + "' for resource type '" + resourceType + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					}
					
					String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType});

					List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, resourceType, ignoreReadErrors, Include.ARCHIVED_AND_PURGED);	

					//Iterate the resources of the archived/purged resources:
					for (String resourceId : resourceIds) {

						String resourceUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, resourceType, resourceId});

						try {
							
							int statusCode = HttpURLConnection.HTTP_OK;

							//Permanently delete the archived/purged resource:
							if(!test){
								statusCode = httpClient.delete(resourceUri, "deleteArchived=true"); //$NON-NLS-1$
							}

							//Capture the output message:
							if(statusCode == HttpURLConnection.HTTP_OK) {
								permanentlyDeletedMessages.add((test ? "Test p" : "P") + "ermanently deleted '" + resourceType + "' resource '" + resourceUri + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
							}
							else {
								throw new Exception("HTTP request 'DELETE " + resourceUri + "?deleteArchived=true' returned status code '" + statusCode + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$						
							}
							
							projectAreaPermanentlyDeletedResourceCount++;
						}
						catch(Exception e) {

							String message = "Unable to " + (test ? "test " : "") + "permanently delete '" + resourceType + "' resource '" + resourceUri + "'. Skipping resource."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

							if(output) {
								System.err.println(message); 
							}

							LogUtils.logError(message);
							LogUtils.logError(e.toString(), e);
						}
					}			

					if(output){
						System.out.println((test ? "Test p" : "P") + "ermanently deleted " + projectAreaPermanentlyDeletedResourceCount + " resource" + (projectAreaPermanentlyDeletedResourceCount != 1 ? "s" : "") + " of resource type '" + resourceType + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
					}
				}

				if((output) && (resourceTypes.size() > 1)){
					System.out.println((test ? "Test p" : "P") + "ermanently deleted " + projectAreaPermanentlyDeletedResourceCount + " resource" + (projectAreaPermanentlyDeletedResourceCount != 1 ? "s" : "") + " of resource type" + (resourceTypes.size() != 1 ? "s" : "") + " '" + APIUtils.toString(resourceTypes) + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$
				}

				totalPermanentlyDeletedResourceCount += projectAreaPermanentlyDeletedResourceCount;
			}

			if(output){

				System.out.println("Summary:"); //$NON-NLS-1$
				System.out.println("    " + (test ? "Test p" : "P") + "ermanently deleted " + totalPermanentlyDeletedResourceCount + " resource" + (totalPermanentlyDeletedResourceCount != 1 ? "s" : "") + " of resource type" + (resourceTypes.size() != 1 ? "s" : "") + " '" + APIUtils.toString(resourceTypes) + "' in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$

				for(String permanentlyDeleteMessage : permanentlyDeletedMessages){
					System.out.println("    " + permanentlyDeleteMessage); //$NON-NLS-1$
				}
			}
		}
		else if ("createmanyattachments".equals(command.toLowerCase())) { //$NON-NLS-1$

			for (String projectAreaAlias : projectAreaAliases) {

				if(output){
					System.out.println("Running command '" + command + "' in project area '" + projectAreaAlias + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}

				//Setup 1: Resolve the current count of test cases.		
				String attachmentType = "attachment"; //$NON-NLS-1$
				
				String attachmentsFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, attachmentType});
				
				List<String> attachmentIds = FeedReader.getIds(httpClient, attachmentsFeedUri, attachmentType, ignoreReadErrors);	

				int attachmentCount = (attachmentIds.size() + 1);

				//Setup 2: Resolve the current count of test cases.		
				String testCaseType = "testcase"; //$NON-NLS-1$

				String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAlias, testCaseType});

				List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, testCaseType, ignoreReadErrors);	

				int testCaseCount = (resourceIds.size() + 1);

				//Setup 3: Create a temporary file for the attachment:
				File temporaryAttachmentFile = File.createTempFile((attachmentType + "_" + attachmentCount), ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
				temporaryAttachmentFile.deleteOnExit();
				
				PrintWriter temporaryAttachmentFileWriter = null;
				
				try{
					
					temporaryAttachmentFileWriter = new PrintWriter(temporaryAttachmentFile);			
					temporaryAttachmentFileWriter.println("Attachment file content"); //$NON-NLS-1$
				}
				finally{
					
					if(temporaryAttachmentFileWriter != null){
						temporaryAttachmentFileWriter.close();
					}
				}
				
				if(!test){

					for (int counter = 0; counter < count; counter++) {

						//Step 1: Create the attachment.					
						String attachmentGenerateId = httpClient.postAttachment(attachmentsFeedUri, Files.readAllBytes(Paths.get(temporaryAttachmentFile.getAbsolutePath())), (attachmentType + "_" + attachmentCount + "_" + (counter + 1) + ".txt")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

						//Step 2: Create the test case with the attachment.
						String testCaseUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAlias, testCaseType, (testCaseType + "_" + testCaseCount + "_" + (counter + 1))}); //$NON-NLS-1$ //$NON-NLS-2$

						StringBuilder testCaseXml = new StringBuilder();
						testCaseXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
						testCaseXml.append("<ns2:testcase xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n"); //$NON-NLS-1$
						testCaseXml.append("  <ns4:title>Test Case " + testCaseCount + "." + (counter + 1) + "</ns4:title>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						testCaseXml.append("  <ns2:attachment href=\"" + attachmentsFeedUri + "/" + attachmentGenerateId + "\"/>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						testCaseXml.append("</ns2:testcase>"); //$NON-NLS-1$

						httpClient.put(testCaseUri, testCaseXml.toString());
					}	
				}
				
				if(output){

					System.out.println("Summary:"); //$NON-NLS-1$
					System.out.println("    " + (test ? "Test c" : "C") + "reated " + count + " attachments in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'.");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
				}
			}
		}
		else if ("createcategorywithvaluesets".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Setup: Resolve the current count of parent category types.		
			int currentParentCategoryTypeCount = 0;

			String categoryTypeFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), "categoryType"}); //$NON-NLS-1$

			String categoryTypeFeedXml = APIUtils.toString(httpClient.get(categoryTypeFeedUri));
			
			SAXBuilder saxBuilder = new SAXBuilder();
			
			Document categoryTypeFeedDocument = saxBuilder.build(new ByteArrayInputStream(categoryTypeFeedXml.trim().getBytes()));

			List<?> categoryTypeFeedEntries = categoryTypeFeedDocument.getRootElement().getChildren("entry", Namespace.getNamespace(NAMESPACE_URI_ATOM)); //$NON-NLS-1$
			
			for(Object categoryTypeFeedEntry : categoryTypeFeedEntries){

				String categoryTypeFeedEntryTitle = ((Element)(categoryTypeFeedEntry)).getChild("title", Namespace.getNamespace(NAMESPACE_URI_ATOM)).getValue(); //$NON-NLS-1$

				if((APIUtils.isSet(categoryTypeFeedEntryTitle)) && (categoryTypeFeedEntryTitle.startsWith("Parent Category Type"))){
					currentParentCategoryTypeCount++;
				}
			}
			
			currentParentCategoryTypeCount++;
			
			//Step 1: Create the parent category type.
			String parentCategoryTypeUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), "categoryType", "parentCategoryType" + currentParentCategoryTypeCount}); //$NON-NLS-1$ //$NON-NLS-2$
 
			StringBuilder parentCategoryTypeXml = new StringBuilder();
			parentCategoryTypeXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			parentCategoryTypeXml.append("<ns2:categoryType xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
			parentCategoryTypeXml.append("  <ns4:title>Parent Category Type " + currentParentCategoryTypeCount + "</ns4:title>\n");
			parentCategoryTypeXml.append("  <ns2:scope>TestPlan</ns2:scope>\n");
			parentCategoryTypeXml.append("  <ns2:required>true</ns2:required>\n");
			parentCategoryTypeXml.append("  <ns2:multiSelectable>false</ns2:multiSelectable>\n");
			parentCategoryTypeXml.append("</ns2:categoryType>");

			httpClient.put(parentCategoryTypeUri, parentCategoryTypeXml.toString());
			
			//Step 2: Create the subcategory type.
			String subcategoryTypeUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), "categoryType", "subcategoryType" + currentParentCategoryTypeCount}); //$NON-NLS-1$ //$NON-NLS-2$
 
			StringBuilder subcategoryTypeXml = new StringBuilder();
			subcategoryTypeXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			subcategoryTypeXml.append("<ns2:categoryType xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
			subcategoryTypeXml.append("  <ns4:title>Subcategory Type " + currentParentCategoryTypeCount + "</ns4:title>\n");
			subcategoryTypeXml.append("  <ns2:scope>TestPlan</ns2:scope>\n");
			subcategoryTypeXml.append("  <ns2:required>true</ns2:required>\n");
			subcategoryTypeXml.append("  <ns2:multiSelectable>false</ns2:multiSelectable>\n");
			subcategoryTypeXml.append("  <ns2:dependsOn href=\"" + parentCategoryTypeUri + "\"/>\n");
			subcategoryTypeXml.append("</ns2:categoryType>");

			httpClient.put(subcategoryTypeUri, subcategoryTypeXml.toString());

			//Step 3: Create the parent category values.
			List<String> parentcategoryValueUris = new ArrayList<String>();

			for (int counter = 0; counter < count; counter++) {

				String parentCategoryValueUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), "category", "parentCategoryValue" + currentParentCategoryTypeCount + "" + (counter + 1)}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder parentCategoryValueXml = new StringBuilder();
				parentCategoryValueXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				parentCategoryValueXml.append("<ns2:category xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				parentCategoryValueXml.append("  <ns4:title>Parent Category Value " + currentParentCategoryTypeCount + " " + (counter + 1) + "</ns4:title>\n");
				parentCategoryValueXml.append("  <ns2:categoryType href=\"" + parentCategoryTypeUri + "\"/>\n");
				parentCategoryValueXml.append("</ns2:category>");

				httpClient.put(parentCategoryValueUri, parentCategoryValueXml.toString());

				parentcategoryValueUris.add(parentCategoryValueUri);
			}	

			//Step 4: Create the subcategory values.
			List<String> subcategoryValueUris = new ArrayList<String>();
			
			for (int counter = 0; counter < count; counter++) {

				String subcategoryValueUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), "category", ("subcategoryValue" + currentParentCategoryTypeCount + "" + (counter + 1))}); //$NON-NLS-1$ //$NON-NLS-2$
				 
				StringBuilder subcategoryValueXml = new StringBuilder();
				subcategoryValueXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				subcategoryValueXml.append("<ns2:category xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				subcategoryValueXml.append("  <ns4:title>Subcategory Value " + currentParentCategoryTypeCount + " " + (counter + 1) + "</ns4:title>\n");
				subcategoryValueXml.append("  <ns2:categoryType href=\"" + subcategoryTypeUri + "\"/>\n");
				subcategoryValueXml.append("</ns2:category>");

				httpClient.put(subcategoryValueUri, subcategoryValueXml.toString());
				
				subcategoryValueUris.add(subcategoryValueUri);
			}		
					
			//Step 5: Resolve the subcategory type and resolve the RQM namespace prefix.
			subcategoryTypeXml = new StringBuilder(APIUtils.toString(httpClient.get(subcategoryTypeUri)));

			Pattern rqmNamespacePrefixPattern = Pattern.compile(".*xmlns\\:(ns[\\d]+)" + Pattern.quote("=\"http://jazz.net/xmlns/alm/qm/v0.1/\"") + ".*");
			
			Matcher rqmNamespacePrefixMatcher = rqmNamespacePrefixPattern.matcher(subcategoryTypeXml);
			
			rqmNamespacePrefixMatcher.find();
			
			String rqmNamespacePrefix = rqmNamespacePrefixMatcher.group(1);
			
			//Step 6: Create the value sets.
			StringBuilder valueSetXml = new StringBuilder();
			
			int counter = 0;
			
			for(String subcategoryValueUri : subcategoryValueUris){

				valueSetXml.append("  <" + rqmNamespacePrefix + ":valueset>\n");
				valueSetXml.append("    <" + rqmNamespacePrefix + ":key href=\"" + parentcategoryValueUris.get(counter) + "\"/>\n");
				valueSetXml.append("    <" + rqmNamespacePrefix + ":value href=\"" + subcategoryValueUri + "\"/>\n");
				valueSetXml.append("  </" + rqmNamespacePrefix + ":valueset>\n");
				
				counter++;
			}
			
			//Step 7: Append the value set to the subcategory type.
			subcategoryTypeXml.insert(subcategoryTypeXml.lastIndexOf("</" + rqmNamespacePrefix + ":categoryType>"), valueSetXml);
			
			//Step 8: Update the subcategory type.
			httpClient.put(subcategoryTypeUri, subcategoryTypeXml.toString());			
		}
		else if ("createcategorywithmanyvalues".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Setup: Resolve the current count of sample category types.		
			int currentSampleCategoryTypeCount = 0;

			final String sampleCategoryTypeFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), "categoryType"}); //$NON-NLS-1$

			final String sampleCategoryTypeFeedXml = APIUtils.toString(httpClient.get(sampleCategoryTypeFeedUri));

			SAXBuilder saxBuilder = new SAXBuilder();

			Document sampleCategoryTypeFeedDocument = saxBuilder.build(new ByteArrayInputStream(sampleCategoryTypeFeedXml.trim().getBytes()));

			List<?> sampleCategoryTypeFeedEntries = sampleCategoryTypeFeedDocument.getRootElement().getChildren("entry", Namespace.getNamespace(NAMESPACE_URI_ATOM)); //$NON-NLS-1$

			for(Object sampleCategoryTypeFeedEntry : sampleCategoryTypeFeedEntries){

				final String sampleCategoryTypeFeedEntryTitle = ((Element)(sampleCategoryTypeFeedEntry)).getChild("title", Namespace.getNamespace(NAMESPACE_URI_ATOM)).getValue(); //$NON-NLS-1$

				if((APIUtils.isSet(sampleCategoryTypeFeedEntryTitle)) && (sampleCategoryTypeFeedEntryTitle.startsWith("Sample Category Type"))){
					currentSampleCategoryTypeCount++;
				}
			}

			currentSampleCategoryTypeCount++;

			//Step 1: Create the sample category type.
			final String sampleCategoryTypeUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), "categoryType", "sampleCategoryType" + currentSampleCategoryTypeCount}); //$NON-NLS-1$ //$NON-NLS-2$

			StringBuilder sampleCategoryTypeXml = new StringBuilder();
			sampleCategoryTypeXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			sampleCategoryTypeXml.append("<ns2:categoryType xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
			sampleCategoryTypeXml.append("  <ns4:title>Sample Category Type " + currentSampleCategoryTypeCount + "</ns4:title>\n");
			sampleCategoryTypeXml.append("  <ns2:scope>TestPlan</ns2:scope>\n");
			sampleCategoryTypeXml.append("  <ns2:required>false</ns2:required>\n");
			sampleCategoryTypeXml.append("  <ns2:multiSelectable>false</ns2:multiSelectable>\n");
			sampleCategoryTypeXml.append("</ns2:categoryType>");

			httpClient.put(sampleCategoryTypeUri, sampleCategoryTypeXml.toString());

			//Step 2: Create the sample category values.
			for (int counter = 0; counter < count; counter++) {

				final String sampleCategoryValueUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), "category", ("sampleCategoryValue" + currentSampleCategoryTypeCount + "" + (counter + 1))}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder sampleCategoryValueXml = new StringBuilder();
				sampleCategoryValueXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				sampleCategoryValueXml.append("<ns2:category xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				sampleCategoryValueXml.append("  <ns4:title>Sample Category Value " + currentSampleCategoryTypeCount + " " + (counter + 1) + "</ns4:title>\n");
				sampleCategoryValueXml.append("  <ns2:categoryType href=\"" + sampleCategoryTypeUri + "\"/>\n");
				sampleCategoryValueXml.append("</ns2:category>");

				httpClient.put(sampleCategoryValueUri, sampleCategoryValueXml.toString());
			}		
		}
		else if ("createmanyusers".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Resolve the current count of users:	
			int currentUserCount = 0;

			final String contributorFeedUri = httpClient.getServerUrl() + "service/com.ibm.team.repository.service.internal.IAdminRestService/contributors?searchTerm=%25&pageSize=50&sortBy=name&sortOrder=ascpageNum=0";

			final String contributorXmlFeed = APIUtils.toString(httpClient.get(contributorFeedUri));

			final Pattern countPattern = Pattern.compile(".*" + Pattern.quote("<count>") + "(\\d+)" + Pattern.quote("</count>") + ".*");
			
			Matcher countMatcher = countPattern.matcher(contributorXmlFeed);
			
			if(countMatcher.find()){
				currentUserCount = Integer.parseInt(countMatcher.group(1));

				System.out.println("The current count of users is " + currentUserCount + ".");
			}
			else{
				throw new Exception("Could not resolve current user count.");
			}

			currentUserCount++;

			List<String> newContributorUuids = new ArrayList<String>();
			
			//Create the new users:
			for (int counter = currentUserCount; counter < (currentUserCount + count); counter++) {

				final String contributorUri = httpClient.getServerUrl() + "service/com.ibm.team.repository.service.internal.IAdminRestService/contributor";

				final String username = ("User" + counter);
				
				StringBuilder postContent = new StringBuilder();
				postContent.append("itemId=new");
				postContent.append("&name=" + username);
				postContent.append("&userId=" + username);
				postContent.append("&emailAddress=");
				postContent.append(URLEncoder.encode(username + "@home.com", ENCODING_UTF8));
				postContent.append("&jsonRoles=");
				postContent.append(URLEncoder.encode("[\"JazzUsers\"]", ENCODING_UTF8));
				postContent.append("&jsonLicenses=");
				postContent.append(URLEncoder.encode("{\"add\":[" /*\"com.ibm.rqm.tester\"*/ + "],\"remove\":[]}", ENCODING_UTF8));

				String soapXmlResponse = httpClient.post(contributorUri, postContent.toString(), MEDIA_TYPE_FORM_URL_ENCODED, null);
				
				final Pattern contributorIdPattern = Pattern.compile(".*" + Pattern.quote("<contributorId>") + "([^\\\"]+)" + Pattern.quote("</contributorId>") + ".*");

				Matcher contributorIdMatcher = contributorIdPattern.matcher(soapXmlResponse);
				
				if(contributorIdMatcher.find()){
					newContributorUuids.add(contributorIdMatcher.group(1));
				}
				else{
					throw new Exception("Could not resolve new contributor ID.");
				}
			}		
			
			//Resolve the new count of users:	
			countMatcher = countPattern.matcher(APIUtils.toString(httpClient.get(contributorFeedUri)));
			
			if(countMatcher.find()){
				System.out.println("The new count of users is " + countMatcher.group(1) + ".");
			}
			else{
				throw new Exception("Could not resolve current user count.");
			}

			//Create a new project area with the new users:
			final String projectAreaUri = httpClient.getServerUrl() + "service/com.ibm.team.process.internal.service.web.IProcessWebUIService/projectArea";

			StringBuilder postContent = new StringBuilder();
			postContent.append("itemId=new");
			postContent.append("&jsonAdmins=" + URLEncoder.encode("{}", ENCODING_UTF8));
				
			StringBuilder jsonMembers = new StringBuilder();
			jsonMembers.append("{\"add\":[");
			
			for (int index = 0; index < newContributorUuids.size(); index++) {
				
				if(index > 0){
					jsonMembers.append(",");					
				}
				
				jsonMembers.append("\"");
				jsonMembers.append(newContributorUuids.get(index));
				jsonMembers.append("\"");					
			}

			jsonMembers.append("]}");
//			jsonMembers.append("],\"roles\":{");
//
//			for (int index = 0; index < newContributorUuids.size(); index++) {
//
//				if(index > 0){
//					jsonMembers.append(",");					
//				}
//
//				jsonMembers.append("\"");
//				jsonMembers.append(newContributorUuids.get(index));
//				jsonMembers.append("\":[\"contributor\",\"data_migrator_admin\",\"tester\",\"default\"]");					
//			}
//
//			jsonMembers.append("}}");
			
			postContent.append("&jsonMembers=" + URLEncoder.encode(jsonMembers.toString(), ENCODING_UTF8));
				
			if(projectAreaAliases.size() == 1){
				postContent.append("&name=" + URLEncoder.encode(projectAreaAliases.get(0), ENCODING_UTF8));
			}
			else{
				throw new Exception("Requires one project area alias to create a new project area with the new users.");
			}
				
			postContent.append("&owningApplicationKey=JTS-Sentinel-Id");
			postContent.append("&processLocale=en-us");
			
			//Note: The process area UUID is specific to the local RQM development environment (see https://localhost:9443/jazz/service/com.ibm.team.process.internal.service.web.IProcessWebUIService/allProcessDefinitions?owningApplicationKey=JTS-Sentinel-Id).
			System.out.println("Input the UUID for the 'Quality Management Default Process' process area in the ETM server (see https://localhost:9443/jazz/service/com.ibm.team.process.internal.service.web.IProcessWebUIService/allProcessDefinitions?owningApplicationKey=JTS-Sentinel-Id):");

			Scanner scanner = new Scanner(System.in);

			String processAreaUuid = scanner.nextLine().trim();			
			
			scanner.close();
			
			postContent.append("&processUuid=");
			postContent.append(processAreaUuid);
				
			httpClient.post(projectAreaUri, postContent.toString(), MEDIA_TYPE_FORM_URL_ENCODED, null);
			
			System.out.println("REMINDER: Add the TestJazzAdmin1 user to the new project area and add all process roles to the new users in the new project area.");
		}
		else if ("createmanyuserscsvfile".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Resolve the current count of users:
			int currentUserCount = 0;

			final String contributorFeedUri = httpClient.getServerUrl() + "service/com.ibm.team.repository.service.internal.IAdminRestService/contributors?searchTerm=%25&pageSize=50&sortBy=name&sortOrder=ascpageNum=0";

			final String contributorFeedXml = APIUtils.toString(httpClient.get(contributorFeedUri));

			final Pattern countPattern = Pattern.compile(".*" + Pattern.quote("<count>") + "(\\d+)" + Pattern.quote("</count>") + ".*");
			
			Matcher countMatcher = countPattern.matcher(contributorFeedXml);
			
			if(countMatcher.find()){
				currentUserCount = Integer.parseInt(countMatcher.group(1));
			}
			else{
				throw new Exception("Could not resolve current user count.");
			}

			currentUserCount++;

			//Create the users CSV file (users.csv):
			File csvFile = new File("users.csv");
			PrintWriter printWriter = new PrintWriter(csvFile);
			
			for (int counter = currentUserCount; counter < (currentUserCount + count); counter++) {

				final String username = ("User" + counter);
			
				//userid,name,email@example.org,[com.ibm.team.foundation.user],[JazzUser],0
			    //Where:
				//userid is the user ID of the user.
			    //name is the name of the user.
			    //email@example.org is the email address of the user.
			    //[com.ibm.team.foundation.user] is the list of licenses.
			    //[JazzUser] is the list of repository groups.
			    //0 means that the user is not archived. If the user is archived, this value is 1.

				StringBuilder userContent = new StringBuilder();
				userContent.append(username);
				userContent.append(",");
				userContent.append(username);
				userContent.append(",");
				userContent.append(username);
				userContent.append("@home.com");
				userContent.append(",");
				userContent.append("[");
				//userContent.append("com.ibm.rqm.tester");
				userContent.append("]");
				userContent.append(",");
				userContent.append("[JazzUsers]");
				userContent.append(",");
				userContent.append("0");

				printWriter.println(userContent.toString());
			}
			
			printWriter.close();
			
			System.out.println("Users CSV file created at: " + csvFile.getAbsolutePath());
			System.out.println("See https://jazz.net/help-dev/clm/topic/com.ibm.jazz.install.doc/topics/r_repotools_importusers.html to import the users CSV file.");
		}
		else if ("createmanytestcases".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Setup: Resolve the current count of test cases.		
			final String testCaseType = "testcase"; //$NON-NLS-1$

			String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseType});

			List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, testCaseType, ignoreReadErrors);	

			int currentTestCaseCount = (resourceIds.size() + 1);
						
			//Step 1: Create the test cases.
			for (int counter = 0; counter < count; counter++) {

				String testCaseUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseType, testCaseType + "_" + currentTestCaseCount + "_" + (counter + 1)}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder testCaseXml = new StringBuilder();
				testCaseXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				testCaseXml.append("<ns2:testcase xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				testCaseXml.append("  <ns4:title>Test Case " + currentTestCaseCount + "." + (counter + 1) + "</ns4:title>\n");
				testCaseXml.append("</ns2:testcase>");

				httpClient.put(testCaseUri, testCaseXml.toString());
			}	
		}
		else if ("createmanytestscripts".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Setup: Resolve the current count of test scripts.		
			final String testScriptType = "testscript"; //$NON-NLS-1$

			String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testScriptType});

			List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, testScriptType, ignoreReadErrors);	

			int currentTestScriptCount = (resourceIds.size() + 1);
						
			//Step 1: Create the test scripts.
			for (int counter = 0; counter < count; counter++) {

				String testScriptUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testScriptType, testScriptType + "_" + currentTestScriptCount + "_" + (counter + 1)}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder testScriptXml = new StringBuilder();
				testScriptXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				testScriptXml.append("<ns2:testscript xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				testScriptXml.append("  <ns4:title>Test Script " + currentTestScriptCount + "." + (counter + 1) + "</ns4:title>\n");
				testScriptXml.append("  <ns2:steps>");
				testScriptXml.append("    <ns8:step stepIndex=\"1\" type=\"execution\">");
				testScriptXml.append("      <ns8:name>Test Script Step " + currentTestScriptCount + "." + (counter + 1) + "</ns8:name>");
				testScriptXml.append("      <ns8:title>Test Script Step " + currentTestScriptCount + "." + (counter + 1) + "</ns8:title>");
				testScriptXml.append("      <ns8:description>");
				testScriptXml.append("        <div>Test Script Step " + currentTestScriptCount + "." + (counter + 1) + "</div>");
				testScriptXml.append("      </ns8:description>");
				testScriptXml.append("    </ns8:step>");
				testScriptXml.append("  </ns2:steps>");
				testScriptXml.append("</ns2:testscript>");

				httpClient.put(testScriptUri, testScriptXml.toString());
			}	
		}
		else if ("createmanytestcaseexecutionrecords".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Setup: Resolve the current count of test cases.		
			final String testCaseType = "testcase"; //$NON-NLS-1$
			final String testCaseExecutionRecordType = "executionworkitem"; //$NON-NLS-1$

			String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseType});

			List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, testCaseType, ignoreReadErrors);	

			int currentTestCaseCount = (resourceIds.size() + 1);
						
			//Step 1: Create the test cases and test case execution records.
			for (int counter = 0; counter < count; counter++) {

				//Step 1a: Create the test case.
				String testCaseUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseType, testCaseType + "_" + currentTestCaseCount + "_" + (counter + 1)}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder testCaseXml = new StringBuilder();
				testCaseXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				testCaseXml.append("<ns2:testcase xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				testCaseXml.append("  <ns4:title>Test Case " + currentTestCaseCount + "." + (counter + 1) + "</ns4:title>\n");
				testCaseXml.append("</ns2:testcase>");

				httpClient.put(testCaseUri, testCaseXml.toString());
				
				//Step 1b: Create the test case execution record.
				String testCaseExecutionRecordUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseExecutionRecordType, testCaseExecutionRecordType + "_" + currentTestCaseCount + "_" + (counter + 1)}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder testCaseExecutionRecordXml = new StringBuilder();
				testCaseExecutionRecordXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				testCaseExecutionRecordXml.append("<ns2:executionworkitem xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				testCaseExecutionRecordXml.append("  <ns4:title>Test Case Execution Record " + currentTestCaseCount + "." + (counter + 1) + "</ns4:title>\n");
				testCaseExecutionRecordXml.append("  <ns2:testcase href=\"" + testCaseUri + "\"/>\n");
				testCaseExecutionRecordXml.append("</ns2:executionworkitem>");

				httpClient.put(testCaseExecutionRecordUri, testCaseExecutionRecordXml.toString());
			}	
		}
		else if ("createmanytestsuiteresults".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Setup: Resolve the current count of test cases and test suites.		
			final String testCaseType = "testcase"; //$NON-NLS-1$
			final String testSuiteType = "testsuite"; //$NON-NLS-1$
			final String testCaseExecutionRecordType = "executionworkitem"; //$NON-NLS-1$
			final String testSuiteExecutionRecordType = "suiteexecutionrecord"; //$NON-NLS-1$
			final String testCaseResultType = "executionresult"; //$NON-NLS-1$
			final String testSuiteResultType = "testsuitelog"; //$NON-NLS-1$

			String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseType});

			List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, testCaseType, ignoreReadErrors);	

			int currentTestCaseCount = (resourceIds.size() + 1);

			resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testSuiteType});

			resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, testCaseType, ignoreReadErrors);	

			int currentTestSuiteCount = (resourceIds.size() + 1);

			//Step 1: Create the test case.
			String testCaseUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseType, testCaseType + "_" + currentTestCaseCount}); //$NON-NLS-1$ //$NON-NLS-2$

			StringBuilder testCaseXml = new StringBuilder();
			testCaseXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			testCaseXml.append("<ns2:testcase xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
			testCaseXml.append("  <ns4:title>Test Case " + currentTestCaseCount + ".</ns4:title>\n");
			testCaseXml.append("</ns2:testcase>");

			httpClient.put(testCaseUri, testCaseXml.toString());
			
			//Step 2: Create the test suite.
			String testSuiteUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testSuiteType, testSuiteType + "_" + currentTestSuiteCount}); //$NON-NLS-1$

			StringBuilder testSuiteXml = new StringBuilder();
			testSuiteXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			testSuiteXml.append("<ns2:testsuite xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
			testSuiteXml.append("  <ns4:title>Test Suite " + currentTestSuiteCount + ".</ns4:title>\n");
			testSuiteXml.append("  <ns2:suiteelements>\n");
			testSuiteXml.append("  	<ns2:suiteelement elementindex=\"0\">\n");
			testSuiteXml.append("  		<ns2:testcase href=\"" + testCaseUri + "\"/>\n");
			testSuiteXml.append("  	</ns2:suiteelement>\n");
			testSuiteXml.append("  </ns2:suiteelements>\n");
			testSuiteXml.append("</ns2:testsuite>");

			httpClient.put(testSuiteUri, testSuiteXml.toString());
			
			//Step 3: Create the test case execution record.
			String testCaseExecutionRecordUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseExecutionRecordType, testCaseExecutionRecordType + "_" + currentTestCaseCount}); //$NON-NLS-1$ //$NON-NLS-2$

			StringBuilder testCaseExecutionRecordXml = new StringBuilder();
			testCaseExecutionRecordXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			testCaseExecutionRecordXml.append("<ns2:executionworkitem xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
			testCaseExecutionRecordXml.append("  <ns4:title>Test Case Execution Record " + currentTestCaseCount + "</ns4:title>\n");
			testCaseExecutionRecordXml.append("  <ns2:testcase href=\"" + testCaseUri + "\"/>\n");
			testCaseExecutionRecordXml.append("</ns2:executionworkitem>");

			httpClient.put(testCaseExecutionRecordUri, testCaseExecutionRecordXml.toString());

			//Step 4: Create the test suite execution record.
			String testSuiteExecutionRecordUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testSuiteExecutionRecordType, testSuiteExecutionRecordType + "_" + currentTestSuiteCount}); //$NON-NLS-1$ //$NON-NLS-2$

			StringBuilder testSuiteExecutionRecordXml = new StringBuilder();
			testSuiteExecutionRecordXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			testSuiteExecutionRecordXml.append("<ns2:suiteexecutionrecord xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
			testSuiteExecutionRecordXml.append("  <ns4:title>Test Suite Execution Record " + currentTestSuiteCount + "</ns4:title>\n");
			testSuiteExecutionRecordXml.append("  <ns2:testsuite href=\"" + testSuiteUri + "\"/>\n");
			testSuiteExecutionRecordXml.append("</ns2:suiteexecutionrecord>");

			httpClient.put(testSuiteExecutionRecordUri, testSuiteExecutionRecordXml.toString());

			//Step 5: Create the test case results and test suite results.
			for (int counter = 0; counter < count; counter++) {

				//Step 5a: Create the test case result.
				String testCaseResultUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testCaseResultType, testCaseResultType + "_" + currentTestCaseCount + "_" + (counter + 1)}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder testCaseResultXml = new StringBuilder();
				testCaseResultXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				testCaseResultXml.append("<ns2:executionresult xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				testCaseResultXml.append("  <ns4:title>Test Case Result " + currentTestCaseCount + "." + (counter + 1) + "</ns4:title>\n");
				testCaseResultXml.append("  <ns2:testcase href=\"" + testCaseUri + "\"/>\n");
				testCaseResultXml.append("  <ns2:executionworkitem href=\"" + testCaseExecutionRecordUri + "\"/>\n");
				testCaseResultXml.append("</ns2:executionresult>");

				httpClient.put(testCaseResultUri, testCaseResultXml.toString());
				
				//Step 5b: Create the test suite result.
				String testSuiteResultUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), testSuiteResultType, testSuiteResultType + "_" + currentTestSuiteCount + "_" + (counter + 1)}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder testSuiteResultXml = new StringBuilder();
				testSuiteResultXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				testSuiteResultXml.append("<ns2:testsuitelog xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				testSuiteResultXml.append("  <ns4:title>Test Suite Result " + currentTestSuiteCount + "." + (counter + 1) + "</ns4:title>\n");
				testSuiteResultXml.append("  <ns2:testsuite href=\"" + testSuiteUri + "\"/>\n");
				testSuiteResultXml.append("  <ns2:suiteexecutionrecord href=\"" + testSuiteExecutionRecordUri + "\"/>\n");
				testSuiteResultXml.append("  <ns2:executionresult href=\"" + testCaseResultUri + "\"/>\n");
				testSuiteResultXml.append("  <ns18:suiteelements>\n");
				testSuiteResultXml.append("  	<ns18:suiteelement>\n");
				testSuiteResultXml.append("  		<ns18:index>0</ns18:index>\n");
				testSuiteResultXml.append("  		<ns18:testcase href=\"" + testCaseUri + "\"/>\n");
				testSuiteResultXml.append("  		<ns18:executionworkitem href=\"" + testCaseExecutionRecordUri + "\"/>\n");
				testSuiteResultXml.append("  	</ns18:suiteelement>\n");
				testSuiteResultXml.append("  </ns18:suiteelements>\n");
				testSuiteResultXml.append("</ns2:testsuitelog>");

				httpClient.put(testSuiteResultUri, testSuiteResultXml.toString());
			}	
		}
		else if ("createmanycomponents".equals(command.toLowerCase())) { //$NON-NLS-1$

			//Setup: Resolve the current count of components.		
			final String componentType = "component"; //$NON-NLS-1$

			String resourceFeedUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), componentType});

			List<String> resourceIds = FeedReader.getIds(httpClient, resourceFeedUri, componentType, ignoreReadErrors);	

			int currentComponentCount = (resourceIds.size() + 1);
						
			//Step 1: Create the components.
			for (int counter = 0; counter < count; counter++) {

				String componentUri = MessageFormat.format(URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED, new Object[]{httpClient.getServerUrl(), projectAreaAliases.get(0), componentType}); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuilder componentXml = new StringBuilder();
				componentXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				componentXml.append("<ns2:component xmlns:ns2=\"http://jazz.net/xmlns/alm/qm/v0.1/\" xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:ns3=\"http://schema.ibm.com/vega/2008/\" xmlns:ns4=\"http://purl.org/dc/elements/1.1/\" xmlns:ns5=\"http://jazz.net/xmlns/prod/jazz/process/0.6/\" xmlns:ns6=\"http://jazz.net/xmlns/alm/v0.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/\" xmlns:ns9=\"http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1\" xmlns:ns10=\"http://open-services.net/ns/core#\" xmlns:ns11=\"http://open-services.net/ns/qm#\" xmlns:ns12=\"http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/\" xmlns:ns13=\"http://www.w3.org/2002/07/owl#\" xmlns:ns14=\"http://jazz.net/xmlns/alm/qm/qmadapter/v0.1\" xmlns:ns15=\"http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1\" xmlns:ns16=\"http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1\" xmlns:ns17=\"http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1\" xmlns:ns18=\"http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/\" xmlns:ns20=\"http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/\" xmlns:ns21=\"http://www.w3.org/1999/XSL/Transform\">\n");
				componentXml.append("  <ns4:title>Component " + currentComponentCount + "." + (counter + 1) + "</ns4:title>\n");
				componentXml.append("</ns2:component>");

				httpClient.post(componentUri, componentXml.toString(), MEDIA_TYPE_APPLICATION_XML, null);
			}	
		}
		else{
			System.out.println("Unknown " + (test ? "test " : "") + "command '" + command + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 			
		}

		System.out.println("Completed " + (test ? "test " : "") + "command '" + command + "' in project area" + (projectAreaAliases.size() == 1 ? "" : "s") + " '" + getProjectAreaAliasNames() + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
	}
	
	@SuppressWarnings("unchecked")
	private long resolveTestScriptLastAddDateTime(List<Element> historyEntries, String testScriptUri) throws Exception {

		try {

			SAXBuilder saxBuilder = new SAXBuilder();
			
			//Assumption: History feed is English.
			String testScriptXml = APIUtils.toString(httpClient.get(testScriptUri));
			
			Document testScriptDocument = saxBuilder.build(new ByteArrayInputStream(testScriptXml.trim().getBytes()));

			//Resolve the test script title and web ID:
			Element testScriptWebIdElement = testScriptDocument.getRootElement().getChild("webId", Namespace.getNamespace(NAMESPACE_URI_ALM_QM)); //$NON-NLS-1$
			Element testScriptTitleElement = testScriptDocument.getRootElement().getChild("title", Namespace.getNamespace(NAMESPACE_URI_DC_ELEMENTS)); //$NON-NLS-1$

			if ((testScriptWebIdElement != null) && (testScriptTitleElement != null)) {

				//Example: Test Script 42: PSMTS1
				final String historyMessage = ("Test Script " + testScriptWebIdElement.getValue() + ": " + testScriptTitleElement.getValue()); //$NON-NLS-1$ //$NON-NLS-2$

				long testScriptLastAddDateTime = -1;

				for(Element historyEntry : historyEntries){

					Element updatedElement = historyEntry.getChild("updated", Namespace.getNamespace(NAMESPACE_URI_ATOM)); //$NON-NLS-1$
					Element summaryElement = historyEntry.getChild("summary", Namespace.getNamespace(NAMESPACE_URI_ATOM)); //$NON-NLS-1$

					if((updatedElement != null) && (summaryElement != null)){

						long updated = DateTimeUtils.parseDateTime(updatedElement.getValue());

						if(updated != -1){

							for(Element trElement : resolveTrElements(summaryElement)){

								List<Element> tdElements = trElement.getChildren("td", Namespace.getNamespace(NAMESPACE_URI_XHTML)); //$NON-NLS-1$

								//Example:
								//<tr>
								//<td>Added</td>
								//<td colspan="2">Test Script 42: PSMTS1</td>
								//</tr>
								if(tdElements.size() == 2){

									String firstTdValue = tdElements.get(0).getValue();

									if((firstTdValue != null) && (firstTdValue.trim().equalsIgnoreCase("added"))){ //$NON-NLS-1$

										String secondTdValue = tdElements.get(1).getValue();

										if((secondTdValue != null) && (secondTdValue.trim().equalsIgnoreCase(historyMessage))){

											if((testScriptLastAddDateTime == -1) || (testScriptLastAddDateTime < updated)){
												testScriptLastAddDateTime = updated;
											}
										}
									}
								}
							}
						}
					}
				}
				
				return testScriptLastAddDateTime;
			}
		}
		catch(Exception ex) {
			if(ignoreReadErrors) {
				String output_str = "Unable to get test script content using: " + testScriptUri + ". Skipping."; //$NON-NLS-1$ //$NON-NLS-2$
				if(output) {
					System.out.println(output_str); 
				}
				LogUtils.logTrace(output_str);
				LogUtils.logError(ex.toString(), ex);
			}
			else {
				throw ex;
			}
		}
		
		return -1;
	}
	
	private List<Element> resolveTrElements(Element rootElement){
		
		List<Element> trElements = new ArrayList<Element>();
		
		if(rootElement != null){
		
			if(("tr".equals(rootElement.getName())) && (NAMESPACE_URI_XHTML.equals(rootElement.getNamespaceURI()))){ //$NON-NLS-1$
				trElements.add(rootElement);
			}
			else{

				for(Object child : rootElement.getChildren()){
					trElements.addAll(resolveTrElements((Element)(child)));				
				}
			}
		}

		return trElements;
	}

	private String encodeResouceName(String uri) throws Exception {
		Matcher integrationServiceUrlMatcher = INTEGRATION_SERVICE_URL_PATTERN.matcher(uri); 

		if(integrationServiceUrlMatcher.matches()){
			String resourceId = integrationServiceUrlMatcher.group(3);
			
			// located the resource id, the id can have forward slash, encode each
			// segments of the id.
			if((resourceId != null) && (!resourceId.trim().isEmpty())){
				String uri_seg = uri.substring(0, uri.indexOf(resourceId));
				return uri_seg + encodeSegmentedResourceId(resourceId);
			}
		}
		return uri;
	}
	
	private String encodeSegmentedResourceId(String resourceId) throws Exception {
		String[] segments = resourceId.split("/"); //$NON-NLS-1$
		StringBuffer encodedId = new StringBuffer();
		
		for(int i = 0; i < segments.length; i++) {
			encodedId.append(URLEncoder.encode(segments[i], ENCODING_UTF8));
			if(i < segments.length-1) {
				encodedId.append("/"); //$NON-NLS-1$
			}
		}
		return encodedId.toString();
	}
	
	public String getProjectAreaAliasNames(){

		if(projectAreaAliasNames == null){
			projectAreaAliasNames = APIUtils.toString(projectAreaAliases);
		}

		return projectAreaAliasNames;
	}
	private String transformDocToString(org.w3c.dom.Document doc){
		StringWriter writer = null;
		TransformerFactory transformerfactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = transformerfactory.newTransformer();
			Source source = new DOMSource(doc);
			writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			String output_str = "Not able to transform content.";
			if(output)
				System.out.println(output_str);
			LogUtils.logTrace(output_str);
			LogUtils.logError(e.toString(), e);
			return "";
		}
		return writer.toString();
	}
}