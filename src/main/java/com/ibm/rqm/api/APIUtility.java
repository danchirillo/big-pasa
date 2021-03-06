/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2020. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ibm.rqm.api.internal.APIUtilities;
import com.ibm.rqm.api.internal.client.APIHttpClientFactory;
import com.ibm.rqm.api.internal.client.qm.APIHttpClient;
import com.ibm.rqm.api.internal.util.APIUtils;
import com.ibm.rqm.api.internal.util.DateTimeUtils;
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
public class APIUtility implements IAPIConstants {

	private static final String VERSION = "1.0"; //$NON-NLS-1$

	public static final String[] SUPPORTED_RESOURCE_TYPES = new String[]{"adapter", "attachment", "builddefinition", "buildrecord", "catalog", "category", "categoryType", "channe", "configuration", "datapool", "executionresult", "executionsequence", "executionsequenceresult", "executionvariable", "executionvariablevalue", "executionworkitem", "jobscheduler", "keyword", "labresource", "labresourceattribute", "objective", "remotescript", "request", "reservation", "resourcegroup", "suiteexecutionrecord", "tasks", "template", "testcase", "testcell", "testphase", "testplan", "testscript", "testsuite", "testsuitelog"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$ //$NON-NLS-20$ //$NON-NLS-21$ //$NON-NLS-22$ //$NON-NLS-23$ //$NON-NLS-24$ //$NON-NLS-25$ //$NON-NLS-26$ //$NON-NLS-27$ //$NON-NLS-28$ //$NON-NLS-29$ //$NON-NLS-30$ //$NON-NLS-31$ //$NON-NLS-32$ //$NON-NLS-33$ //$NON-NLS-34$ //$NON-NLS-35$

	public static enum CmdLineArg {
		QUALITY_MANAGER_URL("-qm", "-qualityManagerURL"), //$NON-NLS-1$ //$NON-NLS-2$
		USER_NAME("-u", "-username"), //$NON-NLS-1$ //$NON-NLS-2$
		PASSWORD("-pw", "-password"), //$NON-NLS-1$ //$NON-NLS-2$
		PROJECT_AREA("-pa", "-projectArea"), //$NON-NLS-1$ //$NON-NLS-2$
		LOG("-l", "-log"), //$NON-NLS-1$ //$NON-NLS-2$
		RESOURCES("-r", "-resources"), //$NON-NLS-1$ //$NON-NLS-2$
		QUERY_STRING("-qs", "-queryString"), //$NON-NLS-1$ //$NON-NLS-2$
		RESOURCE_TYPES("-rt", "-resourceTypes"), //$NON-NLS-1$ //$NON-NLS-2$
		RESOURCE_ID("-ri", "-resourceId"), //$NON-NLS-1$ //$NON-NLS-2$
		REMOTE_SCRIPT_TYPE("-rst", "-remoteScriptType"), //$NON-NLS-1$ //$NON-NLS-2$
		ADAPTER_ID("-ai", "-adapterId"), //$NON-NLS-1$ //$NON-NLS-2$
		OUTPUT("-o", "-output", false), //$NON-NLS-1$ //$NON-NLS-2$
		TEST("-t", "-test", false), //$NON-NLS-1$ //$NON-NLS-2$
		HELP("-h", "-help", false), //$NON-NLS-1$ //$NON-NLS-2$
		IGNORE_READ_ERRORS("-ire", "-ignoreReadErrors", false), //$NON-NLS-1$ //$NON-NLS-2$
		VERSION("-v", "-version", false), //$NON-NLS-1$ //$NON-NLS-2$
		COMMAND("-c", "-command"), //$NON-NLS-1$ //$NON-NLS-2$
		CREATION_DATE("-cd", "-creationDate"), //$NON-NLS-1$ //$NON-NLS-2$
		EXECUTION_STATE("-es", "-executionStates"), //$NON-NLS-1$ //$NON-NLS-2$
		EXECUTION_PROGRESS("-ep", "-executionProgress"), //$NON-NLS-1$ //$NON-NLS-2$
		RESULT_STATE("-rs", "-resultStates"), //$NON-NLS-1$ //$NON-NLS-2$
		COUNT("-ct", "-count"), //$NON-NLS-1$ //$NON-NLS-2$
		SECTION_ID("-si", "-sectionId"),//$NON-NLS-1$ //$NON-NLS-2$
		SECTION_NAME("-sn", "-sectionName"), //$NON-NLS-1$ //$NON-NLS-2$
		CONFIG_CONTEXT("-cfg", "-configContext"); //$NON-NLS-1$ //$NON-NLS-2$

		private String shortName;
		private String longName;
		private boolean isValueRequired;
		private String value;
		private CmdLineArg(String shortName, String longName) { this(shortName, longName, true); }
		private CmdLineArg(String shortName, String longName, boolean isValueRequired) { 
			this.shortName=shortName; 
			this.longName=longName; 
			this.isValueRequired = isValueRequired; 
		};
		public boolean isEqual(String name) {
			return shortName.equalsIgnoreCase(name) || longName.equalsIgnoreCase(name);
		}
		public boolean isValueRequired() { return isValueRequired; }
		public void setValue(String value) { 

			if(value != null){
				this.value = value.trim();
			}
			else{
				this.value = value;
			}
		}
		public String getValue() { return value; }
		public String getShortName() { return shortName; }
		public String getLongName() { return longName; }
		
		@Override
		public String toString() {
			
			StringBuilder string = new StringBuilder();
			
			if(APIUtils.isSet(shortName)){				
				string.append(shortName);
			}
			
			if(APIUtils.isSet(longName)){				
				
				if(string.length() > 0) {
					string.append("/"); //$NON-NLS-1$
				}
				
				string.append(longName);
			}
			
			return (string.toString());
		}
	}

	public static void main(String[] args) {
		
		APIHttpClient apiHttpClient = null;
		int systemExitCode = 0;
		try {                  

			List<CmdLineArg> cmdArgs = processArgs(args);

			if ((cmdArgs.contains(CmdLineArg.HELP)) || (cmdArgs.size() == 0)) {

				BufferedReader readmeReader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("readme.txt"))); //$NON-NLS-1$
				String line = null;

				while((line = readmeReader.readLine()) != null){
					System.out.println(line);
				}

				System.exit(0);
			}

			if (cmdArgs.contains(CmdLineArg.VERSION)) {

				System.out.println(getVersion());
				System.exit(0);
			}

			System.out.println("Configuring the ETM API Utility."); //$NON-NLS-1$

			//Turn off Apache Commons Logging for dependencies:
			//Note: Turn off Apache Commons Logging BEFORE configuration/execution.
			String[] loggerNames = new String[]{HttpClient.class.getPackage().getName(), "httpclient.wire"}; //$NON-NLS-1$

			for(String loggerName : loggerNames){

				//Note: Apache Commons Logging defaults to Log4j.
				Logger logger = Logger.getLogger(loggerName);

				//Preserve Apache Commons Logging, if configured (e.g. properties file):
				if(logger.getLevel() == null){				
					logger.setLevel(Level.OFF); 
				}
			}
			
			if (cmdArgs.contains(CmdLineArg.LOG)) {
				LogUtils.setLogFile(CmdLineArg.LOG.getValue());
			}
			else{
				LogManager.getRootLogger().setLevel(Level.OFF);
			}

			URL serverUrl = null;
			String username = null;
			String password = null;
			String command = null;

			if (cmdArgs.contains(CmdLineArg.QUALITY_MANAGER_URL)) {

				String url = CmdLineArg.QUALITY_MANAGER_URL.getValue();

				if(!url.endsWith(FORWARD_SLASH)){
					url = (url + FORWARD_SLASH);
				}

				serverUrl = new URL(url);

				//Validate the server URL by resolving the context root without the leading/trailing slash characters and checking for slash characters:
				String path = serverUrl.getPath();
				path = path.substring(1);
				path = path.substring(0, (path.length() - 1));

				if(path.contains(FORWARD_SLASH)){
					throw new IllegalArgumentException(CmdLineArg.QUALITY_MANAGER_URL.toString() + " requires a valid IBM Engineering Test Management server URL"); //$NON-NLS-1$
				}
			} 
			else {
				throw new IllegalArgumentException(CmdLineArg.QUALITY_MANAGER_URL.toString() + " is required"); //$NON-NLS-1$
			}

			if (cmdArgs.contains(CmdLineArg.USER_NAME)) {
				username = CmdLineArg.USER_NAME.getValue();
			} 
			else {
				throw new IllegalArgumentException(CmdLineArg.USER_NAME.toString() + " is required"); //$NON-NLS-1$
			}

			if (cmdArgs.contains(CmdLineArg.PASSWORD)) {
				password = CmdLineArg.PASSWORD.getValue();
			} 
			else {
				throw new IllegalArgumentException(CmdLineArg.PASSWORD.toString() + " is required"); //$NON-NLS-1$
			}

			if (cmdArgs.contains(CmdLineArg.COMMAND)) {
				command = CmdLineArg.COMMAND.getValue();
			} 
			else {
				throw new IllegalArgumentException(CmdLineArg.COMMAND.toString() + " is required"); //$NON-NLS-1$
			}

			System.out.println("Connecting to the IBM Engineering Test Management server."); //$NON-NLS-1$

			//Connect to the server:
			apiHttpClient = APIHttpClientFactory.getClient(serverUrl, username, password); 
			
			// Tool updated for 6.0.x and above: By adding the config context parameter, all requests will be performed on
			// the configuration specified. So let's be careful to match projects with its respective configurations.
			if (cmdArgs.contains(CmdLineArg.CONFIG_CONTEXT)) {

				String configContext = CmdLineArg.CONFIG_CONTEXT.getValue(); //$NON-NLS-1$
				apiHttpClient.setConfigContext(configContext);
			}

			List<String> projectAreaAliases = null;

			if (cmdArgs.contains(CmdLineArg.PROJECT_AREA)) {

				String[] projectAreaNames = CmdLineArg.PROJECT_AREA.getValue().split(","); //$NON-NLS-1$

				if(projectAreaNames.length == 0){
					throw new IllegalArgumentException(CmdLineArg.PROJECT_AREA.toString() + " requires one (or more) valid project area names"); //$NON-NLS-1$
				}

				projectAreaAliases = Arrays.asList(projectAreaNames);
			}
			else{
				projectAreaAliases = apiHttpClient.getProjectAreaAliases();
			}

			if(projectAreaAliases.size() == 0){
				throw new IllegalArgumentException(CmdLineArg.QUALITY_MANAGER_URL.toString() + " requires a valid IBM Engineering Test Management server with one (or more) valid project areas"); //$NON-NLS-1$
			}

			PrintStream resourcesPrintStream = null;
			
			if (cmdArgs.contains(CmdLineArg.RESOURCES)) {
				
				File resourcesFile = new File(CmdLineArg.RESOURCES.getValue());
				
				if(resourcesFile.exists()){
					resourcesFile.delete();
				}
				
				resourcesPrintStream = new PrintStream(resourcesFile);
			}
			else{
				resourcesPrintStream = System.out;
			}
			
			String queryString = null;
			
			if (cmdArgs.contains(CmdLineArg.QUERY_STRING)) {
				
				StringBuilder queryStringBuilder = new StringBuilder();
				
				for(String queryStringNameValue : URLDecoder.decode(CmdLineArg.QUERY_STRING.getValue(), ENCODING_UTF8).split("&")){ //$NON-NLS-1$

					if(queryStringBuilder.length() > 0){
						queryStringBuilder.append("&"); //$NON-NLS-1$						
					}
					
					String[] nameValue = queryStringNameValue.split("="); //$NON-NLS-1$		
					
					queryStringBuilder.append(nameValue[0]);
					
					if(nameValue.length > 1){

						queryStringBuilder.append("="); //$NON-NLS-1$
						queryStringBuilder.append(URLEncoder.encode(nameValue[1], ENCODING_UTF8));
					}
				}
				
				queryString = queryStringBuilder.toString();
			}
			else{
				queryString = "abbreviate=false&calmlinks=true"; //$NON-NLS-1$
			}

			List<String> resourceTypes = null;

			if (cmdArgs.contains(CmdLineArg.RESOURCE_TYPES)) {

				String[] resourceTypesValues = CmdLineArg.RESOURCE_TYPES.getValue().split(","); //$NON-NLS-1$

				if(resourceTypesValues.length == 0){
					throw new IllegalArgumentException(CmdLineArg.RESOURCE_TYPES.toString() + " requires one (or more) valid resource types"); //$NON-NLS-1$
				}

				for(String resourceType : resourceTypesValues){

					if(!APIUtils.contains(SUPPORTED_RESOURCE_TYPES, resourceType)){
						throw new IllegalArgumentException(CmdLineArg.RESOURCE_TYPES.toString() + " contains an invalid resource type '" + resourceType + "'"); //$NON-NLS-1$ //$NON-NLS-2$						
					}
				}

				resourceTypes = new ArrayList<String>(Arrays.asList(resourceTypesValues));
			}
			else{
				resourceTypes = new ArrayList<String>(Arrays.asList(SUPPORTED_RESOURCE_TYPES));
			}

			List<String> resourceWebIds = null;

			if (cmdArgs.contains(CmdLineArg.RESOURCE_ID)) {

				String[] resourceIds = CmdLineArg.RESOURCE_ID.getValue().split(","); //$NON-NLS-1$

				if(resourceIds.length == 0){
					throw new IllegalArgumentException(CmdLineArg.RESOURCE_ID.toString() + " requires one (or more) valid resource web IDs"); //$NON-NLS-1$
				}

				resourceWebIds = new ArrayList<String>(Arrays.asList(resourceIds));
			}
			
			long longCreationDate = -1;
			
			if (cmdArgs.contains(CmdLineArg.CREATION_DATE)) {

				//Parse the date/time:
				longCreationDate = DateTimeUtils.parseDateTime(CmdLineArg.CREATION_DATE.getValue());

				if(longCreationDate == -1){

					try {

						//Parse the number of seconds:
						long longDateTime = Long.parseLong(CmdLineArg.CREATION_DATE.getValue());

						//Note: A positive numerical value (greater than 0) is required.
						//Note: 1 second = 1000 milliseconds
						if(longDateTime > 0){
							longCreationDate = (System.currentTimeMillis() - (longDateTime * 1000));
						}
					} 
					catch (NumberFormatException n) {
						//Ignore since an invalid value.
					}
				}

				if(longCreationDate == -1){
					throw new IllegalArgumentException(CmdLineArg.CREATION_DATE.toString() + " requires a valid creation date/time or number of seconds"); //$NON-NLS-1$					
				}
			}
			
			List<String> executionStates = null;

			if (cmdArgs.contains(CmdLineArg.EXECUTION_STATE)) {

				String[] execStates = CmdLineArg.EXECUTION_STATE.getValue().split(","); //$NON-NLS-1$

				if(execStates.length == 0){
					throw new IllegalArgumentException(CmdLineArg.EXECUTION_STATE.toString() + " requires one (or more) valid execution states"); //$NON-NLS-1$
				}

				executionStates = Arrays.asList(execStates);
			}
			
			int executionProgress = -1;
			
			if (cmdArgs.contains(CmdLineArg.EXECUTION_PROGRESS)) {
				int execProgress = Integer.parseInt(CmdLineArg.EXECUTION_PROGRESS.getValue());
				
				if(execProgress < 0 || execProgress > 100) {
					throw new IllegalArgumentException(CmdLineArg.EXECUTION_PROGRESS.toString() + " requires a valid execution progress"); //$NON-NLS-1$
				}
				
				executionProgress = execProgress;
			}
			
			List<String> resultStates = null;

			if (cmdArgs.contains(CmdLineArg.RESULT_STATE)) {

				String[] resStates = CmdLineArg.RESULT_STATE.getValue().split(","); //$NON-NLS-1$

				if(resStates.length == 0){
					throw new IllegalArgumentException(CmdLineArg.RESULT_STATE.toString() + " requires one (or more) valid execution result states"); //$NON-NLS-1$
				}

				resultStates = Arrays.asList(resStates);
			}
				
			List<String> remoteScriptTypeNames = null;

			if (cmdArgs.contains(CmdLineArg.REMOTE_SCRIPT_TYPE)) {

				String[] remoteScriptTypes = CmdLineArg.REMOTE_SCRIPT_TYPE.getValue().split(","); //$NON-NLS-1$

				if(remoteScriptTypes.length == 0){
					throw new IllegalArgumentException(CmdLineArg.REMOTE_SCRIPT_TYPE.toString() + " requires one (or more) valid remote script types"); //$NON-NLS-1$
				}

				remoteScriptTypeNames = Arrays.asList(remoteScriptTypes);
			}
			
			String adapterId = CmdLineArg.ADAPTER_ID.getValue();
			
			boolean output = cmdArgs.contains(CmdLineArg.OUTPUT);

			boolean test = cmdArgs.contains(CmdLineArg.TEST);

			String sectionId = null;
			
			if(cmdArgs.contains(CmdLineArg.SECTION_ID)) {
				sectionId = CmdLineArg.SECTION_ID.getValue();
			}
			else if((COMMAND_UNIFY_CUSTOM_SECTIONS.toLowerCase().equals(command.toLowerCase())) || ((COMMAND_UNIFY_CUSTOM_SECTIONS_2.toLowerCase().equals(command.toLowerCase())))) {
				throw new IllegalArgumentException(CmdLineArg.SECTION_ID.toString() + " requires a valid custom rich text section ID"); //$NON-NLS-1$
			}
			
			String sectionName = null;
			
			if(cmdArgs.contains(CmdLineArg.SECTION_NAME)) {
				sectionName = CmdLineArg.SECTION_NAME.getValue();
			}
			else if((COMMAND_UNIFY_CUSTOM_SECTIONS.toLowerCase().equals(command.toLowerCase())) || ((COMMAND_UNIFY_CUSTOM_SECTIONS_2.toLowerCase().equals(command.toLowerCase())))) {
				throw new IllegalArgumentException(CmdLineArg.SECTION_NAME.toString() + " requires a valid custom rich text section name"); //$NON-NLS-1$
			}
			
			boolean ignoreReadErrors = cmdArgs.contains(CmdLineArg.IGNORE_READ_ERRORS);

			int count = 1;
			
			if(cmdArgs.contains(CmdLineArg.COUNT)){
				count = Integer.parseInt(CmdLineArg.COUNT.getValue());
			}

			System.out.println("Starting the ETM API Utility."); //$NON-NLS-1$

			//Run the API utilities for each project area:			
			APIUtilities apiUtilities = new APIUtilities(apiHttpClient, projectAreaAliases, resourcesPrintStream, queryString, resourceWebIds, remoteScriptTypeNames, adapterId, output, test, ignoreReadErrors, longCreationDate, executionStates, executionProgress, resultStates, resourceTypes, sectionId, sectionName, count);
			apiUtilities.run(command);

			System.out.println("ETM API Utility has completed " + (test ? "test " : "") + "command '" + command + "' in project area" + (projectAreaAliases.size() > 1 ? "s" : "") + " '" + apiUtilities.getProjectAreaAliasNames() + "' on '" + serverUrl.toString() + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
		} 
		catch (Throwable t) {

			LogUtils.logError(t.toString(), t);

			System.out.println("ETM API Utility has terminated due to an error."); //$NON-NLS-1$

			systemExitCode = 1;
		}
		finally {
			
			attemptLogout(apiHttpClient);
			
        	System.exit(systemExitCode);
		}
	}
	
    /**
     * Attempts a logout of the provided client.
     * @param client The client object to handle the logout
     */
    private static void attemptLogout(final APIHttpClient client) {
    	// Exit early if not provided a client
    	if (client == null)
    		return;
    	
    	System.out.println("Disconnecting from the IBM Engineering Test Management server."); //$NON-NLS-1$
    	
    	final int sourceReturnCode = client.logout();
    	
		if ((sourceReturnCode != HttpURLConnection.HTTP_OK) && 
			(sourceReturnCode != HttpURLConnection.HTTP_MOVED_TEMP)) {
			System.out.println("ETM API Utility failed to disconnect from the server!"); //$NON-NLS-1$
		}
    }

	private static List<CmdLineArg> processArgs(String[] args) throws IllegalArgumentException {
		ArrayList<CmdLineArg> argList = new ArrayList<CmdLineArg>();
		for (String arg : args) {
			boolean found = false;
			for (CmdLineArg cmd : CmdLineArg.values()) {
				String[] nameVal = arg.split("="); //$NON-NLS-1$
				if (cmd.isEqual(nameVal[0])) {
					found = true;
					if (nameVal.length > 1 && !cmd.isValueRequired()) {
						throw new IllegalArgumentException(cmd.toString() + " requires no value"); //$NON-NLS-1$
					} else if (nameVal.length > 1) {
						cmd.setValue(nameVal[1]);
					} else if (cmd.isValueRequired()) {
						throw new IllegalArgumentException(cmd.toString() + " requires a value"); //$NON-NLS-1$
					}
					argList.add(cmd);
					break;
				}
			}
			if (!found) {
				throw new IllegalArgumentException(arg + " is an invalid argument"); //$NON-NLS-1$
			}
		}
		return argList;
	}

	private static String getVersion() {
		return ("ETM API Utility, version " + VERSION); //$NON-NLS-1$
	}
}
