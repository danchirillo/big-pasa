ETM API Utility, version 1.0
============================

The ETM API Utility is an open-source stand-alone Java application for working with IBM Engineering Test Management resources using the IBM Engineering Test Management Reportable REST API (see https://jazz.net/wiki/bin/view/Main/RqmApi).  Commands include:

1) findProjectsWithRequirements: Finds all project areas that contain local requirements (created in IBM Engineering Test Management 2.x) and/or links to IBM Rational Requirements Composer/IBM Engineering Requirements Management DOORS Next requirements (created IBM Engineering Test Management 3.x+) and outputs the project area aliases and number of requirements.  Note, IBM Rational RequisitePro and/or IBM Engineering Requirements Management DOORS are ignored.  Useful for determining the required IBM Rational Requirements Composer/IBM Engineering Requirements Management DOORS Next project areas when migrating IBM Engineering Test Management.  The user must have read access for all project areas.  

2) readAll<resource type>Resources: Reads all (active) <resource type> resources and outputs the (formatted) XML representation (ATOM XML feed, each entry containing a resource XML) to the resources file or console.  Note, the ATOM XML feed is not valid input to the IBM Engineering Test Management Reportable REST API (see https://jazz.net/wiki/bin/view/Main/RqmApi).  The <resource type> (case sensitive) MUST be a supported resource type (see https://jazz.net/wiki/bin/view/Main/RqmApi#Resources_and_their_Supported_Op).  Supports the -qs/-queryString argument for querying the resources and reading each resource.  The user must have read access for all project areas.  

3) readAll<resource type>ResourcesHistory: Reads all (active) <resource type> resource's history and outputs the (formatted) XML representation (ATOM feeds, each feed containing a resource and each entry containing a resource history XHTML) to the resources file or console.  The <resource type> (case sensitive) MUST be a supported resource type (see https://jazz.net/wiki/bin/view/Main/RqmApi#historyUrl).  Supports the -qs/-queryString argument for querying the resources.  The user must have read access for all project areas.    

4) addMissingAdapterId: Reads all remote script (qm:remotescript) resources contained in the list of resource ID(s) (qm:webId) specified in the (optional) -ri/-resourceId argument and remote script type(s) (qm:type) specified in the -rst/-remoteScriptType argument.  Remote scripts managed by an adapter (qm:manageadapter = true) but missing an adapter ID (qm:adapterid) will be updated with the adapter ID specified in the -ai/-adapterId argument.  Requires the -rst/-remoteScriptType and -ai/-adapterId arguments.  Supports the -ri/-resourceId argument.  The user must have read/update access for all project areas.

5) completeExecutionTasks: Change the state to com.ibm.rqm.executionframework.common.requeststate.complete for the execution tasks (qm:adaptertask) whose resource ID(s), creation date, state(s), execution progress and(or) associated result state(s) is(are) specified in the -ri/-resourceId, -cd/-creationDate, -es/-executionStates, -ep/-executionProgress and -rs/-resultStates argument respectively. The user can provide the combination of these arguments. The execution tasks will be filtered based on the provided arguments and only filtered out execution task's state will be changed to complete. The -pa/-projectArea argument is optional, if resource ID(s) is(are) not provided. The user must have read/update access for all project areas.

6) autoAssignDefaultScript: Reads all test suites and sets the default test script to the first (see the default table sort of the Test Scripts section in the test case editor) manual (qm:testscript) or non-manual (qm:remotescript) script for each test case in the test suite. The user must have read access for all project areas and permissions to edit test suites.  

7) repairManualExecutionScripts: Reads all the Manual TestScripts and repairs those script steps having Null Delta Predecessor for the specified list of Project Areas. The user must have read access for all project areas and permissions to edit Manual test scripts. Optionally, Web IDs can be provided to fix specific Manual Test Scripts.

8) removeOrphanRootIterations: Reads all test plans and looks for parentIteration with no corresponding test phase, once found, the parentIteration is removed  from the test plan. The user must have read access for all project areas and permissions to edit test plans.     

9) convertInlineImages: Reads all resources of the resource type(s) specified in the (optional) -rt/-resourceTypes argument and converts all inline (base64) images to attachments.  Each resource is read, inline (base64) images are removed from the resource, inline (base64) images are converted to attachments, attachment URLs are added to the resource, and the resource is updated.  Supports the -rt/-resourceTypes argument.  All resource types are used, if the -rt/-resourceTypes argument is not specified. The user must have read/update access for all project areas.

10) removeHTMLTagsFromScriptSteps: Reads all the Manual TestScripts and corrects HTML <br/> tags (transforms "&lt;br/&gt;" into "<br/>"), or mal-formed tags of the form "&lt;br/gt;" (those are also converted into "<br/>"). This command corrects those errors coming from TM Migration.

11) unifyCustomSections: Unifies custom rich text sections in the test case editor with the same name (set in the -si/-sectionId argument) to have the same ID (set in the -sn/-sectionName argument). Requires the -sn/-sectionName and -si/-sectionId arguments.

12) unifyCustomSections2: Updates IDs (set in the -si/-sectionId argument) of the custom rich text sections in the test artifact template with a given name (set in the -sn/-sectionName argument).  Intended to be executed after the unifyCustomSections command.

13) permanentlyDelete: Reads all the archived resources of the resource type(s) specified in the (required) -rt/-resourceTypes argument and permanently deletes those resources including associated attachments.  NOTE: Permanently deleted resources can NOT be accessed, restored, or recovered.  Perform a test operation (see the -t/-test argument) and verify the archived resources that will be permanently deleted.  Permanently deleted resources will be permanently deleted from all configurations (baselines and streams) containing versions of these resources.  Permanently deleted attachments associated with other resources (active or archived) will also be permanently deleted from those resources.  Archived resources include resources in the Trash view or deleted from the Trash view.  Requires the -rt/-resourceTypes argument containing only supported resource types.  Supported resource types include: executionresult.  The user must have read/update access for all project areas.  The user must have a role with the Quality Management/XML Import/Delete and Quality Management/Save <resourceType>/Delete permissions.  This command is only supported for Rational Quality Manager 6.0.6.1 or later.  For more information, see https://jazz.net/wiki/bin/view/Main/RqmApi#deleteArchived.

Experimental (unsupported) commands:

1) createManyAttachments: For each project area, creates one or more (specified in the (optional) -ct/-count argument) sample attachments, each associated with a new sample test case.  Supports the -ct/-count argument.

2) createCategoryWithValueSets: For for specified project area alias (or first resolved project area, if not specified), creates a sample category and subcategory with one or more (specified in the (optional) -ct/-count argument) sample value sets.  Supports the -ct/-count argument.

3) createCategoryWithManyValues: For for specified project area alias (or first resolved project area, if not specified), creates a sample category with one or more (specified in the (optional) -ct/-count argument) sample values.  Supports the -ct/-count argument.

4) createManyUsers: Creates a one or more (specified in the (optional) -ct/-count argument) sample users and creates a new project area using the specified project area alias and adds the new users to the new project area.  Supports the -ct/-count argument.

5) createManyUsersCSVFile: Creates a one or more (specified in the (optional) -ct/-count argument) sample users and outputs the user information (ID, name, email, license(s), repository group(s), and archived) to a CSV file (users.csv) for import into the Jazz Team Server.  Supports the -ct/-count argument.

6) createManyTestCases: For for specified project area alias (or first resolved project area, if not specified), creates one or more (specified in the (optional) -ct/-count argument) sample test cases.  Supports the -ct/-count argument.

7) createManyTestCaseExecutionRecords: For for specified project area alias (or first resolved project area, if not specified), creates one or more (specified in the (optional) -ct/-count argument) sample test cases with test case execution records.  Supports the -ct/-count argument.

8) createManyTestSuiteResults: For for specified project area alias (or first resolved project area, if not specified), creates one or more (specified in the (optional) -ct/-count argument) sample test suite results including the required test case, test suite, test case execution record, test suite execution record, and test case result(s).  Supports the -ct/-count argument.

9) createManyTestScripts: For for specified project area alias (or first resolved project area, if not specified), creates one or more (specified in the (optional) -ct/-count argument) sample test scripts.  Each sample test script will have one sample test script step.  Supports the -ct/-count argument.

10) createManyComponents: For for specified project area alias (or first resolved project area, if not specified), creates one or more (specified in the (optional) -ct/-count argument) sample components.  Note, the project area(s) must be enabled for Configuration Management.  Each sample component will have no test artifacts.  Note, a new configuration (stream) will be created for each new component.  Supports the -ct/-count argument.

Compatibility
=============
The ETM API Utility is compatible with the following products:

    * IBM Rational Quality Manager 4.0 or later

Note: Product names have changed in the 7.0 release (2Q2019):

    * IBM Engineering Test Management replaces IBM Rational Quality Management
    * Engineering Test Management replaces Rational Quality Management
    * ETM replaces RQM

    Some product name references will not change, such as references in ZIP files, JAR files, and Wiki URLs.


Recommended Usage Pattern
=========================

Before using this utility in production you should consider the following suggestions:

    * The log option is recommended to output verbose information including the updated resource XML (pre/post-update) to a file, useful for debugging any failures and restoring updated resources.
    * Perform a test operation (see the -t/-test argument) and verify the results (see the -o/-output and -l/-log arguments).
    * Requires Java 6.0 (Sun JRE 6.0 Update 23 or IBM JRE 6.0 Service Refresh 8) or later (<java.home>).
    * If an OutOfMemoryError is thrown (e.g. memory intensive commands), increase the amount of available JVM memory by adding the -Xmxnm argument where n is the maximum amount of available memory (MB) in multiples of 1024 (recommendation: 2048 MB or 2 GB).
    * When running from a Windows command prompt, some special characters (e.g. ( ) = ; , ` ' % " * ? & \ < > ^ |) in the command are interpreted by the command prompt and require escaping with a caret character (e.g. ^( ^) ^= ^; ^, ^` ^' ^% ^" ^* ^? ^& ^\ ^< ^> ^^ ^|).
    * When running from an Unix shell, some special characters (e.g. ~ ` # $ & * ( ) \ | [ ] { } ; ' " < > / ? *) in the command are interpreted by the shell and require escaping with a backslash character (e.g. \~ \` \# \$ \& \* \( \) \\ \| \[ \] \{ \} \; \' \" \< \> \/ \? \*).
	* Attempting to start the ETM API utility when TLS 1.2 is used and the -Dcom.ibm.team.repository.transport.client.protocol="TLSv1.2" argument is not set will result in a connection handshake failure. If using TLS 1.2, ensure the -Dcom.ibm.team.repository.transport.client.protocol="TLSv1.2" argument is included in order to ensure a successful connection.
	
Argument Reference
==================

Note: Argument values containing whitespace must be enclosed in double quote characters.

<java.home>/bin/java.exe -jar RQMAPIUtility.jar -h

-h, -help
    Prints this help message.

<java.home>/bin/java.exe -jar RQMAPIUtility.jar -v

-v, -version
    Prints the version of the API Utility.

-qm, -qualityManagerURL=<URL>
    The fully specified URL (including a trailing forward slash character) to the IBM Engineering Test Management server: https://<server>:<port>/<context root>/

-u, -username=<username>
    The username for a valid user in the IBM Engineering Test Management server.
    
-pw, -password=<password>
    The password for the username in the IBM Engineering Test Management server.

-pa, -projectArea=<project area alias>
    [Optional] A single project area alias or list of project area aliases in the IBM Engineering Test Management server.
    The list of project area aliases is a single comma-separated (no whitespace) list: <project area alias 1>,<project area alias 2>,<project area alias 3>
    When the -pa/-projectArea argument is omitted, the command processes all project areas resolved from the ETM Reportable REST API's Project Feed Service (see https://jazz.net/wiki/bin/view/Main/RqmApi#Project_Feed_Service).
	For additional information on obtaining the project area alias, see https://jazz.net/wiki/bin/view/Main/RqmApi#projectAlias.

-cfg, -configContext=<configuration UUID>
	Starting on ETM version 6.0, the configuration context UUID parameter is required for specific commands such as repairManualExecutionScripts so the tool can correctly identify the configuration to work with.
	LIMITATIONS: - Currently, the tool supports only 1 configuration value. This means the command is limited to run on 1 project and 1 configuration at a time.
	             - The RepairManualExecutionScripts command REQUIRES this parameter in ETM version 6.0 and above even if Config Management is not enabled (use the default Configuration UUID).
	             - Commands that perform data repair will NOT work on Baseline configurations, as those are immutable.

-c, -command=<command>
    A supported command (see above).

-qs, -queryString=<query string>
	[Optional] The ETM Reportable REST API request query string, composed of one or more ampersand-delimited name-value pairs of supported ETM Reportable REST API request parameters (<name 1>=<value 1>&<name 2>=<value 2>&<name 3>=<value 3>).
    The entire query string MUST be URL-encoded.
    By default, the query string is abbreviate=false&calmlinks=true.
    The -qs/-queryString argument is not supported by all commands (see above).

-l, -log=<file>
    [Optional] Logs verbose information to the specified log file (relative file name or absolute file path).
    When a log file is specified, all (trace, informational, warning, and error) log messages are logged.  Otherwise, only error log messages are logged to the console.
    Note: The log file is rotated (deleted) on every execution of the ETM API Utility.

-rt, -resourceTypes=<resource type>
	[Optional] A single resource type or list of resource types.
	Supported case sensitive values include:
	 - adapter
	 - attachment
	 - builddefinition
	 - buildrecord
	 - catalog
	 - category
	 - categoryType
	 - channel
	 - configuration
	 - datapool
	 - executionresult
	 - executionsequence
	 - executionsequenceresult
	 - executionvariable
	 - executionvariablevalue
	 - executionworkitem
	 - jobscheduler
	 - keyword
	 - labresource
	 - labresourceattribute
	 - objective
	 - remotescript
	 - request
	 - reservation
	 - resourcegroup
	 - suiteexecutionrecord
	 - tasks
	 - template
	 - testcase
	 - testcell
	 - testphase
	 - testplan
	 - testscript
	 - testsuite
	 - testsuitelog
	The list of resource types is a single comma-separated (no whitespace) list: <resource type 1>,<resource type 2>,<resource type 3>
    The -rt/-resourceTypes argument is not supported by all commands (see above).

-r, -resources=<file>
    [Optional] Writes the XML representation of resources to the specified resources file (relative file name or absolute file path).
    When a resources file is not specified, the XML representation of resources are written to the console.
    Note: The resources file is rotated (deleted) on every execution of the ETM API Utility.
    
-ri, -resourceId=<resource web ID>
    [Optional] A single resource web ID (qm:webId) or list of resource web IDs.
    The list of resource web IDs is a single comma-separated (no whitespace) list: <resource web ID 1>,<resource web ID 2>,<resource web ID 3>
    The -ri/-resourceId argument is not supported by all commands (see above).
    
-cd, -creationDate=<creation date>
	[Optional] The date/time or number of seconds since a resource was created.
    Only resources created before (lass than) the creation date/time are processed.
    When a date/time is specified, a W3C/ISO8601 date/time (http://www.w3.org/TR/NOTE-datetime) with second-level precision expressed in UTC (Coordinated Universal Time) using the 'Z' UTC designator (e.g. 2013-09-17T17:18:16Z) is required.
    When a number of seconds is specified, a positive numerical value (greater than 0) is required to calculate the creation date/time from the date/time the ETM API Utility is executed.
    Unit of Time | Number of Seconds
    --------------------------------
    1 week       | 604800
    1 day        | 86400
    1 hour       | 3600
    1 minute     | 60 
	
-es, -executionStates=<execution task state>
	[Optional] A single execution task state or list of execution task states.
	Supported case sensitive values include:
	 - com.ibm.rqm.executionframework.common.requeststate.nottaken
	 - com.ibm.rqm.executionframework.common.requeststate.taken
	 - com.ibm.rqm.executionframework.common.requeststate.paused
	 - com.ibm.rqm.executionframework.common.requeststate.cancelled
	The list of execution task states is a single comma-separated (no whitespace) list: <execution task state 1>,<execution task state 2>,<execution task state 3>
    The -es/-executionStates argument is not supported by all commands (see above).
	
-ep, -executionProgress=<execution task progress>
	[Optional] A single execution task progress.
	Only resources with same execution progress are processed.
	Supported values include 0 to 100.
	The -ep/-executionProgress argument is not supported by all commands (see above).
	
-rs, -resultStates=<execution result state>
	[Optional] A single execution result state or list of execution result states.
	Supported case sensitive values include:
	 - com.ibm.rqm.execution.common.state.paused
	 - com.ibm.rqm.execution.common.state.inprogress
	 - com.ibm.rqm.execution.common.state.notrun
	 - com.ibm.rqm.execution.common.state.passed
	 - com.ibm.rqm.execution.common.state.perm_failed
	 - com.ibm.rqm.execution.common.state.incomplete
	 - com.ibm.rqm.execution.common.state.inconclusive
	 - com.ibm.rqm.execution.common.state.part_blocked
	 - com.ibm.rqm.execution.common.state.deferred
	 - com.ibm.rqm.execution.common.state.failed
	 - com.ibm.rqm.execution.common.state.error
	 - com.ibm.rqm.execution.common.state.blocked
	The list of execution result states is a single comma-separated (no whitespace) list: <execution result state 1>,<execution result state 2>,<execution result state 3>
    The -rs/-resultStates argument is not supported by all commands (see above).
	
-rst, -remoteScriptType=<remote script type>
    [Optional] A single remote script type (qm:type) or list of remote script types.
    Supported case sensitive values (<adapter name>) include:
     -RQM-KEY-CMD-APTR-TYPE-NAME (Sample: Command Line Adapter)
     -RQM-KEY-SEL-APTR-TYPE-NAME (JUnit Selenium)
     -RQM-KEY-APPSCAN-APTR-TYPE-NAME (Rational AppScan Tester Edition)
     -RQM-KEY-RFT-APTR-TYPE-NAME (Rational Functional Tester)
     -RQM-KEY-RPT-APTR-TYPE-NAME (Rational Performance Tester)
     -RQM-KEY-RPT-SERVICE-APTR-TYPE-NAME (Rational Service Tester)
     -RQM-KEY-RIT-APTR-TYPE-NAME (Rational Integration Tester)
     -RQM-KEY-RTW-APTR-TYPE-NAME (Rational Test Workbench)
     -RQM-KEY-TRT-APTR-TYPE-NAME (Rational Test RealTime)
     -RQM-KEY-ROBOT-APTR-TYPE-NAME (Rational Robot)
    The list of remote script types is a single comma-separated (no whitespace) list: <remote script type 1>,<remote script type 2>,<remote script type 3>
    The -rst/-remoteScriptType argument is not supported by all commands (see above).

-ai, -adapterId=<adapter web ID>
    [Optional] A single adapter web ID (qm:webId).
    The -ai/-adapterId argument is not supported by all commands (see above).

-ct, -count=<count>
    [Optional] The number of iterations performed by an operation.
    By default, the count is 1.
    The -ct/-count argument is not supported by all commands (see above).
		
-o, -output
    [Optional] Outputs verbose execution and summary information.

-t, -test
    [Optional] Executes the ETM API Utility without updating resources.
    
-ire, -ignoreReadErrors
    [Optional] Ignores errors when reading XML feeds and resources from the IBM Engineering Test Management server.  Read errors are logged.

-sn, -sectionName
	[Optional] The name of the sections to be updated.
	The -sn/-sectionName argument is not supported by all commands (see above).

-si, -sectionId
	[Optional] The new ID to be set on the sections specified in the -sn/-sectionName argument.
	The -si/-sectionId argument is not supported by all commands (see above).
	
Usage Examples
==============

Run the findProjectsWithRequirements:
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=findProjectsWithRequirements -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -o

Run the readAlltestplanResources on all project areas:
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=readAlltestplanResources -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -r=testplans.xml -o

Run the readAlltestplanResources on project area 'projectAlias1':
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=readAlltestplanResources -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -pa=projectAlias1 -l=apilog.txt -r=testplans.xml -o

Run the readAlltestplanResourcesHistory on all project areas:
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=readAlltestplanResourcesHistory -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -r=testplansHistory.xml -o

Run the readAlltestplanResourcesHistory on project area 'projectAlias1':
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=readAlltestplanResourcesHistory -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -pa=projectAlias1 -l=apilog.txt -r=testplansHistory.xml -o

Run the addMissingAdapterId:
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=addMissingAdapterId -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -ri=1,2,3 -rst=RQM-KEY-RFT-APTR-TYPE-NAME,RQM-KEY-RPT-APTR-TYPE-NAME -ai=1 -o -t

Run the completeExecutionTasks: (for execution tasks with ids [1,2,3])
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=completeExecutionTasks -qm=https://myhost:9443/qm/ -pa=projectAlias1 -u=ADMIN -pw=ADMIN -l=apilog.txt -ri=1,2,3 -o
	
Run the completeExecutionTasks: (for incomplete execution tasks that are created before 18 December, 2013)
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=completeExecutionTasks -qm=https://myhost:9443/qm/ -pa=projectAlias1 -u=ADMIN -pw=ADMIN -l=apilog.txt -cd=2013-12-18T00:00:00Z -es=com.ibm.rqm.executionframework.common.requeststate.taken,com.ibm.rqm.executionframework.common.requeststate.nottaken,com.ibm.rqm.executionframework.common.requeststate.paused -o
	
Run the completeExecutionTasks: (for incomplete execution tasks that are created before 18 December, 2013 across all project areas)
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=completeExecutionTasks -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -cd=2013-12-18T00:00:00Z -es=com.ibm.rqm.executionframework.common.requeststate.taken,com.ibm.rqm.executionframework.common.requeststate.nottaken,com.ibm.rqm.executionframework.common.requeststate.paused -o
	
Run the completeExecutionTasks: (for incomplete execution tasks with progress 100%)
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=completeExecutionTasks -qm=https://myhost:9443/qm/ -pa=projectAlias1 -u=ADMIN -pw=ADMIN -l=apilog.txt -ep=100 -es=com.ibm.rqm.executionframework.common.requeststate.taken,com.ibm.rqm.executionframework.common.requeststate.nottaken,com.ibm.rqm.executionframework.common.requeststate.paused -o
	
Run the completeExecutionTasks: (for incomplete execution tasks with progress 100% across all project areas)
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=completeExecutionTasks -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -ep=100 -es=com.ibm.rqm.executionframework.common.requeststate.taken,com.ibm.rqm.executionframework.common.requeststate.nottaken,com.ibm.rqm.executionframework.common.requeststate.paused -o
	
Run the completeExecutionTasks: (for incomplete execution tasks with ids [1,2,3] and progress 100%, that are created before 18 December, 2013)
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=completeExecutionTasks -qm=https://myhost:9443/qm/ -pa=projectAlias1 -u=ADMIN -pw=ADMIN -l=apilog.txt -ri=1,2,3 -cd=2013-12-18T00:00:00Z -ep=100 -es=com.ibm.rqm.executionframework.common.requeststate.taken,com.ibm.rqm.executionframework.common.requeststate.nottaken,com.ibm.rqm.executionframework.common.requeststate.paused -o
	
Run the completeExecutionTasks: (for incomplete execution tasks whose associated result is complete)
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=completeExecutionTasks -qm=https://myhost:9443/qm/ -pa=projectAlias1 -u=ADMIN -pw=ADMIN -l=apilog.txt -es=com.ibm.rqm.executionframework.common.requeststate.taken,com.ibm.rqm.executionframework.common.requeststate.nottaken,com.ibm.rqm.executionframework.common.requeststate.paused -rs=com.ibm.rqm.execution.common.state.passed,com.ibm.rqm.execution.common.state.perm_failed,com.ibm.rqm.execution.common.state.incomplete,com.ibm.rqm.execution.common.state.inconclusive,com.ibm.rqm.execution.common.state.part_blocked,com.ibm.rqm.execution.common.state.deferred,com.ibm.rqm.execution.common.state.failed,com.ibm.rqm.execution.common.state.error,com.ibm.rqm.execution.common.state.blocked -o
	
Run the completeExecutionTasks: (for incomplete execution tasks whose associated result is complete across all project areas)
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=completeExecutionTasks -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -es=com.ibm.rqm.executionframework.common.requeststate.taken,com.ibm.rqm.executionframework.common.requeststate.nottaken,com.ibm.rqm.executionframework.common.requeststate.paused -rs=com.ibm.rqm.execution.common.state.passed,com.ibm.rqm.execution.common.state.perm_failed,com.ibm.rqm.execution.common.state.incomplete,com.ibm.rqm.execution.common.state.inconclusive,com.ibm.rqm.execution.common.state.part_blocked,com.ibm.rqm.execution.common.state.deferred,com.ibm.rqm.execution.common.state.failed,com.ibm.rqm.execution.common.state.error,com.ibm.rqm.execution.common.state.blocked -o
	
Run the autoAssignDefaultScript:
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=autoAssignDefaultScript -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -o

Run the repairManualExecutionScripts:
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=repairManualExecutionScripts -qm=https://localhost:9443/jazz/ -u=ADMIN -pw=ADMIN -l=apilog.txt -pa="PA1,PA2" -o -cfg="_J8STAbDtEeWHFJfsQmdaOg"

Run the removeOrphanRootIterations:
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=removeorphanrootiterations -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -o

Run the convertInlineImages:
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=convertInlineImages -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -rt=testcase,testplan -l=apilog.txt -o
    
Run the removeHTMLTagsFromScriptSteps:
    <java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=removeHTMLTagsFromScriptSteps -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -o -pa=projectAlias1
    
Run the unifyCustomSections:
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=unifyCustomSections -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -si=com.ibm.rqm.planning.editor.section.dynamicSection_1234567890123 -sn="Custom Section Name"

Run the unifyCustomSections2:
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=unifyCustomSections2 -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -si=com.ibm.rqm.planning.editor.section.dynamicSection_1234567890123 -sn="Custom Section Name"

Run the permanentlyDelete:
	<java.home>/bin/java.exe -jar RQMAPIUtility.jar -c=permanentlyDelete -qm=https://myhost:9443/qm/ -u=ADMIN -pw=ADMIN -l=apilog.txt -o -pa=projectAlias1 -rt=executionresult 

Accessing the Source
====================

All source files for the utility are included in the 'src' directory of RQMAPIUtility.jar. In order to build the source, the libraries in the 'lib' directory also need to be unpacked from the jar.