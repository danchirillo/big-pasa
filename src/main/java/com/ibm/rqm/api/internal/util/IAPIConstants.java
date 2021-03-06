/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2018. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.util;

/**
 * <p>API constants.</p>
 * 
 * <p>Note: Copied from <code>com.ibm.rqm.oslc.service</code> and <code>com.ibm.rqm.oslc.common</code> plug-ins.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 1.0
 * @since   0.9
 */
public interface IAPIConstants {

	//Commands:
	String COMMAND_UNIFY_CUSTOM_SECTIONS = "unifyCustomSections"; //$NON-NLS-N$ //$NON-NLS-1$
	String COMMAND_UNIFY_CUSTOM_SECTIONS_2 = "unifyCustomSections2"; //$NON-NLS-N$ //$NON-NLS-1$
	
	//Encodings:
	String ENCODING_BASE_64 = "base64"; //$NON-NLS-1$

	//RRC:
	String RRC_ARTIFACTCONVERTER_URL = "artifactConverter"; //$NON-NLS-1$
	String RRC_BASE_URL = "resources"; //$NON-NLS-1$
	String RRC_QUERY_URL = "query"; //$NON-NLS-1$
	String RRC_MULTI_FETCH_URL = "multi-fetch"; //$NON-NLS-1$
	String RRC_STOREDQUERY_URL = "storedquery"; //$NON-NLS-1$
	String RRC_CALM_QUERY_URL = "calmquery"; //$NON-NLS-1$
	String RRC_SPARQL_QUERY_URL = "sparqlquery"; //$NON-NLS-1$
	String RRC_FOLDERS_URL = "folders"; //$NON-NLS-1$
	String RRC_TAGS_URL = "tags"; //$NON-NLS-1$
	String RRC_PROJECTS_URL = "projects"; //$NON-NLS-1$
	String RRC_FRIENDS_URL = "friends"; //$NON-NLS-1$
	String RRC_PROJECTRESOURCES_URL = "project-resources"; //$NON-NLS-1$
	String RRC_PROJECTTEMPLATES_URL = "projectTemplates"; //$NON-NLS-1$
	String RRC_COMMENTS_URL = "comments"; //$NON-NLS-1$
	String RRC_WRAPPER_RESOURCE_URL = "wrapper-resources"; //$NON-NLS-1$
	String RRC_WRAPPED_RESOURCE_URL = "wrappedResources"; //$NON-NLS-1$
	String RRC_BINARY_RESOURCE_URL = "binary"; //$NON-NLS-1$
	String RRC_REVISIONS_URL = "revisions"; //$NON-NLS-1$
	String RRC_LINK_TYPES_URL = "linkTypes"; //$NON-NLS-1$
	String RRC_LINKS_URL = "links"; //$NON-NLS-1$
	String RRC_LINKS_20_URL = "links"; //$NON-NLS-1$
	String RRC_TEMPLATES_URL = "templates"; //$NON-NLS-1$
	String RRC_RECENTFEEDS_URL = "recentfeeds"; //$NON-NLS-1$
	String RRC_DISCOVERY_URL = "discovery"; //$NON-NLS-1$
	String RRC_REVIEWS_URL = "reviews"; //$NON-NLS-1$
	String RRC_REVIEW_RESULTS_URL = "reviews/results"; //$NON-NLS-1$
	String RRC_OPERATIONS_URL = "operations"; //$NON-NLS-1$
	String RRC_PROCESS_SECURITY_URL = "process-security"; //$NON-NLS-1$
	String RRC_BASELINES_URL = "baselines"; //$NON-NLS-1$
	String RRC_MULTI_REQUEST_URL = "multi-request"; //$NON-NLS-1$
	String RRC_MAIL_URL = "mail"; //$NON-NLS-1$
	String RRC_LOGS_URL = "logs"; //$NON-NLS-1$
	String RRC_IMPORT_URL = "import"; //$NON-NLS-1$
	String RRC_EXPORT_URL = "export"; //$NON-NLS-1$
	String RRC_TYPES_URL = "types"; //$NON-NLS-1$
	String RRC_VIEWS_URL = "views"; //$NON-NLS-1$
	String RRC_PROXY_URL = "proxy?uri="; //$NON-NLS-1$
	String RRC_REQUEST_VALIDATION_URL = "validation/request"; //$NON-NLS-1$
	String RRC_RESPONSE_VALIDATION_URL = "validation/response"; //$NON-NLS-1$
	String RRC_MODULES_URL = "modules"; //$NON-NLS-1$
	String RRC_GLOSSARY_URL = "glossary"; //$NON-NLS-1$
	
	//Media types:
	String MEDIA_TYPE_APPLICATION_XML = "application/xml"; //$NON-NLS-1$
	String MEDIA_TYPE_APPLICATION_XHTML = "application/xhtml+xml"; //$NON-NLS-1$
	String MEDIA_TYPE_RDF_XML = "application/rdf+xml"; //$NON-NLS-1$
	String MEDIA_TYPE_FORM_URL_ENCODED = "application/x-www-form-urlencoded"; //$NON-NLS-1$
	
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Quality Management V1.0 service provider is not supported or tested.  Please use the OSLC Quality Management V2.0 service provider.
	 */
	String MEDIA_TYPE_OSLC_TEST_CASE_V1 = "application/x-oslc-qm-testcase-1.0+xml"; //$NON-NLS-1$
	
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Quality Management V1.0 service provider is not supported or tested.  Please use the OSLC Quality Management V2.0 service provider.
	 */
	String MEDIA_TYPE_OSLC_TEST_PLAN_V1 = "application/x-oslc-qm-testplan-1.0+xml"; //$NON-NLS-1$
	
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Quality Management V1.0 service provider is not supported or tested.  Please use the OSLC Quality Management V2.0 service provider.
	 */
	String MEDIA_TYPE_OSLC_QM_SERVICE_PROVIDER_CATALOG_V1 = "application/x-oslc-disc-service-provider-catalog+xml"; //$NON-NLS-1$
	
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Quality Management V1.0 service provider is not supported or tested.  Please use the OSLC Quality Management V2.0 service provider.
	 */
	String MEDIA_TYPE_OSLC_QM_SERVICE_DESCRIPTION_V1 = "application/x-oslc-qm-service-description+xml"; //$NON-NLS-1$
	String MEDIA_TYPE_OSLC_QM_RESOURCE_LEGACY = "application/x-oslc-qm-resource+xml"; //$NON-NLS-1$
	String MEDIA_TYPE_UNKNOWN_PROXY = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"; //$NON-NLS-1$	
	String MEDIA_TYPE_WILDCARD = "*/*"; //$NON-NLS-1$
	String MEDIA_TYPE_APPLICATION_WILDCARD = "application/*"; //$NON-NLS-1$
	String MEDIA_TYPE_TEXT_WILDCARD = "text/*"; //$NON-NLS-1$
	String MEDIA_TYPE_TEXT_JSON = "text/json"; //$NON-NLS-1$
	String MEDIA_TYPE_TEXT_XML = "text/xml"; //$NON-NLS-1$
	String MEDIA_TYPE_TEXT_HTML = "text/html"; //$NON-NLS-1$
	String MEDIA_TYPE_ATOM = "application/atom+xml"; //$NON-NLS-1$
	String MEDIA_TYPE_JAZZ_COMPACT_RENDERING = "application/x-jazz-compact-rendering"; //$NON-NLS-1$
	String MEDIA_TYPE_OSLC_COMPACT = "application/x-oslc-compact+xml"; //$NON-NLS-1$
	
	//Formatting:
	String LINE_SEPARATOR = System.getProperty("line.separator", "\\n"); //$NON-NLS-1$ //$NON-NLS-2$
    String FORWARD_SLASH = "/"; //$NON-NLS-1$

	//OSLC:
	String OSLC_VERSION = "2.0"; //$NON-NLS-1$
	String OSLC_CONFIG_PARAM_NAME = "oslc_config.context"; //$NON-NLS-1$

	//HTTP (methods):	
	String HTTP_METHOD_HEAD = "HEAD"; //$NON-NLS-1$
	String HTTP_METHOD_GET = "GET"; //$NON-NLS-1$
	String HTTP_METHOD_PUT = "PUT"; //$NON-NLS-1$
	String HTTP_METHOD_POST = "POST"; //$NON-NLS-1$
	String HTTP_METHOD_DELETE = "DELETE"; //$NON-NLS-1$
	
	//HTTP (headers):
	String HTTP_HEADER_ETAG = "ETag"; //$NON-NLS-1$
	String HTTP_HEADER_IF_MATCH = "If-Match"; //$NON-NLS-1$
	String HTTP_HEADER_ACCEPT = "Accept"; //$NON-NLS-1$
	String HTTP_HEADER_REFERER = "Referer"; //$NON-NLS-1$
	String HTTP_HEADER_CONTENT_TYPE = "Content-Type"; //$NON-NLS-1$
	String HTTP_HEADER_LOCATION = "Location"; //$NON-NLS-1$
	String HTTP_HEADER_OSLC_CORE_VERSION = "OSLC-Core-Version"; //$NON-NLS-1$
    String HTTP_HEADER_SINGLE_COOKIE_HEADER = "http.protocol.single-cookie-header"; //$NON-NLS-1$
    String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding"; //$NON-NLS-1$
    String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding"; //$NON-NLS-1$
    String HTTP_HEADER_CONTENT_LOCATION = "content-location"; //$NON-NLS-1$
    String HTTP_HEADER_AUTHENTICATION = "X-com-ibm-team-repository-web-auth-msg"; //$NON-NLS-1$
    String HTTP_HEADER_NO_RETRY = "X-com-ibm-team-repository.common.remoteaccess.noRetry"; //$NON-NLS-1$
    String HTTP_HEADER_OAUTH_TOKEN = "oauth_token"; //$NON-NLS-1$
    String HTTP_HEADER_OAUTH_CALLBACK = "oauth_callback"; //$NON-NLS-1$
    String HTTP_HEADER_OAUTH_AUTHORIZE = "authorize"; //$NON-NLS-1$

    //HTTP (constants):
    String HTTP_CONSTANTS_FORM = "FORM"; //$NON-NLS-1$
    String HTTP_CONSTANTS_BASIC = "BASIC"; //$NON-NLS-1$

    //HTTP (header values):
	String HTTP_HEADER_VALUE_AUTHENTICATION_REQUIRED = "authrequired"; //$NON-NLS-1$
	String HTTP_HEADER_VALUE_AUTHENTICATION_FAILED = "authfailed"; //$NON-NLS-1$
    
	//Protocols:
	String PROTOCOL_HTTPS = "https"; //$NON-NLS-1$
	
    //Jazz (repository roles):
    String JAZZ_REPOSITORY_ROLE_GUESTS = "JazzGuests"; //$NON-NLS-1$
    String JAZZ_REPOSITORY_ROLE_DWADMINS = "JazzDWAdmins"; //$NON-NLS-1$
    String JAZZ_REPOSITORY_ROLE_USERS = "JazzUsers"; //$NON-NLS-1$
    String JAZZ_REPOSITORY_ROLE_PROJECTADMINS = "JazzProjectAdmins"; //$NON-NLS-1$
    String JAZZ_REPOSITORY_ROLE_ADMINS = "JazzAdmins"; //$NON-NLS-1$

    //Jazz (uri):
    String JAZZ_URI_FORM_AUTHENTICATION = "j_security_check"; //$NON-NLS-1$
    String JAZZ_URI_FORM_OAUTHENTICATION = "oauth-authorize"; //$NON-NLS-1$
    String JAZZ_URI_FORM_LOGOUT = "/auth/logout"; //$NON-NLS-1$
    String JAZZ_URI_FORM_AUTHENTICATION_FAILED = "/auth/authfailed"; //$NON-NLS-1$
    String JAZZ_URI_FORM_AUTHENTICATION_REQUIRED = "/auth/authrequired"; //$NON-NLS-1$
    String JAZZ_URI_FORM_IDENITY = "authenticated/identity"; //$NON-NLS-1$
    
    //Jazz (fields):
    String JAZZ_FIELD_USERNAME = "j_username"; //$NON-NLS-1$
    String JAZZ_FIELD_PASSWORD = "j_password"; //$NON-NLS-1$

	//Encodings:
	String ENCODING_UTF8 = "UTF-8"; //$NON-NLS-1$
	String ENCODING_UTF16 = "UTF-16"; //$NON-NLS-1$
	String ENCODING_GZIP = "gzip";   //$NON-NLS-1$

	//XML:
	/**
	 * <p>The template for the XML declaration.</p>
	 * 
	 * <p>Substitution tokens (<code>{&lt;zero-based token index&gt;}</code>) mappings:</p>
	 * 
	 * <ul>
	 * <li>{0}: The encoding.</li>
	 * </ul>
	 * 
	 * <p>For example:</p>
	 * 
	 * <p><code>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;</code></p>
	 * 
	 * @see java.text.MessageFormat#format(String, Object[])
	 * @see #ENCODING_UTF8
	 */
	String XML_DECLARATION_ENCODING = "<?xml version=\"1.0\" encoding=\"{0}\"?>"; //$NON-NLS-1$
	
	//RDF (languages):
	String RDF_XML_ABBREVIATED = "RDF/XML-ABBREV"; //$NON-NLS-1$

	//RDF (properties):
	/**
	 * <p>If <code>false</code>, an XML declaration is not included with the serialized RDF model.  
	 * If <code>true</code>, the XML declaration without the encoding is included with the serialized 
	 * RDF model.  If not set (default) or <code>true</code> and the {@link java.io.OutputStreamWriter} uses an 
	 * encoding other than UTF-8/UTF-16, the XML declaration with the encoding is included with the 
	 * serialized RDF model.<p> 
	 */
	String RDF_PROPERTY_SHOW_XML_DECLARATION = "showXmlDeclaration"; //$NON-NLS-1$
	
	/**
	 * <p>Number of indent spaces of the serialized RDF model (default: 0).</p>
	 * 
	 * <p>Note: This property in only supported when using the abbreviated XML 
	 * (see {@link #RDF_XML_ABBREVIATED}) syntax.</p>
	 */
	String RDF_PROPERTY_INDENT = "indent"; //$NON-NLS-1$

	/**
	 * <p>Number of tab spaces of the serialized RDF model (default: 2).</p>
	 */
	String RDF_PROPERTY_TAB = "tab"; //$NON-NLS-1$
	
	/**
	 * <p>Number of characters before inserting a newline character (default: 60).</p>
	 */
	String RDF_PROPERTY_WIDTH = "width"; //$NON-NLS-1$	
	
	//Prefixes:
	String PREFIX_DC = "dc"; //$NON-NLS-1$
	String PREFIX_DCTERMS = "dcterms"; //$NON-NLS-1$
	String PREFIX_FOAF = "foaf"; //$NON-NLS-1$
	String PREFIX_RQM_QM = "rqm_qm"; //$NON-NLS-1$
	String PREFIX_OSLC = "oslc"; //$NON-NLS-1$
	String PREFIX_OSLC_CM = "oslc_cm"; //$NON-NLS-1$
	String PREFIX_OSLC_QM = "oslc_qm"; //$NON-NLS-1$
	String PREFIX_OSLC_RM = "oslc_rm"; //$NON-NLS-1$
	String PREFIX_OSLC_DISC = "oslc_disc"; //$NON-NLS-1$
	String PREFIX_CALM = "calm"; //$NON-NLS-1$
	String PREFIX_RDF = "rdf"; //$NON-NLS-1$
	String PREFIX_RDFS = "rdfs"; //$NON-NLS-1$
	String PREFIX_JFS = "jfs"; //$NON-NLS-1$
	String PREFIX_JPRES = "jpres"; //$NON-NLS-1$
	String PREFIX_JPROC = "jproc"; //$NON-NLS-1$
	String PREFIX_XML = "xml"; //$NON-NLS-1$
	String PREFIX_XMLNS = "xmlns"; //$NON-NLS-1$
	
	//Properties:
	String PROPERTY_TITLE = "title"; //$NON-NLS-1$
	String PROPERTY_SERVICE_PROVIDER = "serviceProvider"; //$NON-NLS-1$
	String PROPERTY_VALIDATES_REQUIREMENT_COLLECTION = "validatesRequirementCollection"; //$NON-NLS-1$
	String PROPERTY_VALIDATES_REQUIREMENT = "validatesRequirement"; //$NON-NLS-1$
	String PROPERTY_NEXT_PAGE = "nextPage"; //$NON-NLS-1$
	String PROPERTY_CREATION_FACTORY = "creationFactory"; //$NON-NLS-1$
	String PROPERTY_CREATION = "creation"; //$NON-NLS-1$
	String PROPERTY_QUERY_CAPABILITY = "queryCapability"; //$NON-NLS-1$
	String PROPERTY_QUERY_BASE = "queryBase"; //$NON-NLS-1$
	String PROPERTY_RESOURCE_TYPE = "resourceType"; //$NON-NLS-1$
	String PROPERTY_UPDATED = "updated"; //$NON-NLS-1$
	String PROPERTY_IDENTIFIER = "identifier"; //$NON-NLS-1$
	String PROPERTY_WEB_ID = "webId"; //$NON-NLS-1$
	String PROPERTY_TYPE = "type"; //$NON-NLS-1$
	String PROPERTY_STATE = "state"; //$NON-NLS-1$
	String PROPERTY_CREATION_DATE = "creationDate"; //$NON-NLS-1$
	String PROPERTY_PROGRESS = "progress"; //$NON-NLS-1$
	String PROPERTY_MANAGED_ADAPTER = "manageadapter"; //$NON-NLS-1$
	String PROPERTY_ADAPTER_ID = "adapterid"; //$NON-NLS-1$

	//Resources:
	String RESOURCE_TEST_PLAN = "TestPlan"; //$NON-NLS-1$
	String RESOURCE_TEST_CASE = "TestCase"; //$NON-NLS-1$
	String RESOURCE_TEST_SCRIPT = "TestScript"; //$NON-NLS-1$
	String RESOURCE_TEST_RESULT = "TestResult"; //$NON-NLS-1$
	String RESOURCE_TEST_EXECUTION_RECORD = "TestExecutionRecord"; //$NON-NLS-1$
	String RESOURCE_RESPONSE_INFO = "ResponseInfo"; //$NON-NLS-1$

	//Prefixed (oslc_qm:) resources:		
	String RESOURCE_OSLC_QM_TEST_PLAN = (PREFIX_OSLC_QM + ':' + RESOURCE_TEST_PLAN); 
	String RESOURCE_OSLC_QM_TEST_CASE = (PREFIX_OSLC_QM + ':' + RESOURCE_TEST_CASE); 
	String RESOURCE_OSLC_QM_TEST_SCRIPT = (PREFIX_OSLC_QM + ':' + RESOURCE_TEST_SCRIPT); 
	String RESOURCE_OSLC_QM_TEST_RESULT = (PREFIX_OSLC_QM + ':' + RESOURCE_TEST_RESULT); 
	String RESOURCE_OSLC_QM_TEST_EXECUTION_RECORD = (PREFIX_OSLC_QM + ':' + RESOURCE_TEST_EXECUTION_RECORD); 
	
	//Namespaces:
	String NAMESPACE_URI_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"; //$NON-NLS-1$
	String NAMESPACE_URI_RDFS = "http://www.w3.org/2000/01/rdf-schema#"; //$NON-NLS-1$
	String NAMESPACE_URI_DC_TERMS = "http://purl.org/dc/terms/"; //$NON-NLS-1$
	String NAMESPACE_URI_OSLC = "http://open-services.net/ns/core#"; //$NON-NLS-1$

	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Change Management V1.0 consumer is not supported or tested.  Please use the OSLC Change Management V1.0 consumer.
	 */
	String NAMESPACE_URI_OSLC_CM_V1 = "http://open-services.net/xmlns/cm/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_OSLC_CM = "http://open-services.net/ns/cm#"; //$NON-NLS-1$
	
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Quality Management V1.0 service provider is not supported or tested.  Please use the OSLC Quality Management V2.0 service provider.
	 */
	String NAMESPACE_URI_OSLC_QM_V1 = "http://open-services.net/xmlns/qm/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_OSLC_QM = "http://open-services.net/ns/qm#"; //$NON-NLS-1$
	
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Requirements Management V1.0 consumer is not supported or tested.  Please use the OSLC Requirements Management V1.0 consumer.
	 */
	String NAMESPACE_URI_OSLC_RM_V1 = "http://open-services.net/xmlns/rm/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_OSLC_RM = "http://open-services.net/ns/rm#"; //$NON-NLS-1$
	String NAMESPACE_URI_OSLC_DISC = "http://open-services.net/xmlns/discovery/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_OSLC_DISC_JAZZ = "http://jazz.net/xmlns/prod/jazz/discovery/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_JAZZ_JFS = "http://jazz.net/xmlns/prod/jazz/jfs/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_JAZZ_CALM = "http://jazz.net/xmlns/prod/jazz/calm/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_JAZZ_PRESENTATION = "http://jazz.net/xmlns/prod/jazz/presentation/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_JAZZ_PROCESS = "http://jazz.net/xmlns/prod/jazz/process/1.0/"; //$NON-NLS-1$
	String NAMESPACE_URI_JAZZ_QM = "http://jazz.net/ns/qm/rqm#"; //$NON-NLS-1$
	String NAMESPACE_URI_XML = "http://www.w3.org/XML/1998/namespace"; //$NON-NLS-1$
	String NAMESPACE_URI_DC_ELEMENTS = "http://purl.org/dc/elements/1.1/"; //$NON-NLS-1$
	String NAMESPACE_URI_FOAF = "http://xmlns.com/foaf/0.1/"; //$NON-NLS-1$
	String NAMESPACE_URI_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema#"; //$NON-NLS-1$
	String NAMESPACE_URI_ALM_QM = "http://jazz.net/xmlns/alm/qm/v0.1/"; //$NON-NLS-1$
	String ALM_NAMESPACE = "http://jazz.net/xmlns/alm/v0.1/"; //$NON-NLS-1$
	String NAMESPACE_URI_ALM_QM_ADAPTER_TASK = "http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1"; //$NON-NLS-1$
	String NAMESPACE_URI_ATOM = "http://www.w3.org/2005/Atom"; //$NON-NLS-1$
	String NAMESPACE_URI_XHTML = "http://www.w3.org/1999/xhtml"; //$NON-NLS-1$
	
	//Ranges:
	//Note: The RQM implementation of the OSLC Quality Management V 2.0 specification is more restrictive of target resource ranges (typed compared to 'any').
	String RANGE_URI_TEST_PLAN = (NAMESPACE_URI_OSLC_QM + RESOURCE_TEST_PLAN);
	String RANGE_URI_TEST_CASE = (NAMESPACE_URI_OSLC_QM + RESOURCE_TEST_CASE);
	String RANGE_URI_TEST_SCRIPT = (NAMESPACE_URI_OSLC_QM + RESOURCE_TEST_SCRIPT);
	String RANGE_URI_TEST_RESULT = (NAMESPACE_URI_OSLC_QM + RESOURCE_TEST_RESULT);
	String RANGE_URI_TEST_EXECUTION_RECORD = (NAMESPACE_URI_OSLC_QM + RESOURCE_TEST_EXECUTION_RECORD);
	
	//Service providers:
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Quality Management V1.0 service provider is not supported or tested.  Please use the OSLC Quality Management V2.0 service provider.
	 */
	String SERVICE_PROVIDERS_URI_QM = (NAMESPACE_URI_OSLC_QM_V1 + "qmServiceProviders"); //$NON-NLS-1$
	
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Change Management V1.0 consumer is not supported or tested.  Please use the OSLC Change Management V1.0 consumer.
	 */
	String SERVICE_PROVIDERS_URI_CM = (NAMESPACE_URI_OSLC_CM_V1 + "cmServiceProviders"); //$NON-NLS-1$
	
	/**
	 * @deprecated As of Rational Quality Manager 5.0, the OSLC Requirements Management V1.0 consumer is not supported or tested.  Please use the OSLC Requirements Management V1.0 consumer.
	 */
	String SERVICE_PROVIDERS_URI_RM = (NAMESPACE_URI_OSLC_RM_V1 + "rmServiceProviders"); //$NON-NLS-1$

	//Context roots:
	String CONTEXT_ROOT_QM = "qm"; //$NON-NLS-1$
	String CONTEXT_ROOT_CM = "cm"; //$NON-NLS-1$
	String CONTEXT_ROOT_RM = "rm"; //$NON-NLS-1$
	String CONTEXT_ROOT_RDM = "rdm"; //$NON-NLS-1$
	String CONTEXT_ROOT_JAZZ = "jazz"; //$NON-NLS-1$

	//Templates (URIs):
	/**
	 * <p>The template for root services URIs.</p>
	 * 
	 * <p>Substitution tokens (<code>{&lt;zero-based token index&gt;}</code>) mappings:</p>
	 * 
	 * <ul>
	 * <li>{0}: The server URL (including context root) with the trailing separator character.</li>
	 * </ul>
	 * 
	 * <p>For example:</p>
	 * 
	 * <p><code>https://localhost:9443/jazz/rootservices</code></p>
	 * 
	 * @see java.text.MessageFormat#format(String, Object[])
	 */
	String URI_TEMPLATE_ROOT_SERVICES = "{0}rootservices";  //$NON-NLS-1$
	
	String CONFIG_CONTEXT_PARAMETER = OSLC_CONFIG_PARAM_NAME + "={0}"; //$NON-NLS-1$

	String INTEGRATION_SERVICE_URL = "service/com.ibm.rqm.integration.service.IIntegrationService/";  //$NON-NLS-1$
	
	String MANUALEXECUTIONSCRIPT_RESTSERVICE_URL = "service/com.ibm.rqm.planning.common.service.rest.IManualExecutionScriptRestService/";  //$NON-NLS-1$
	
	String INTEGRATION_SERVICE_RESOURCES_URL = INTEGRATION_SERVICE_URL + "resources/";  //$NON-NLS-1$

	String URI_TEMPLATE_INTEGRATION_SERVICE_FEED = "{0}" + INTEGRATION_SERVICE_URL + "{1}";  //$NON-NLS-1$ //$NON-NLS-2$

	String URI_TEMPLATE_INTEGRATION_SERVICE_ALL_PROJECTS_RESOURCES_FEED = "{0}" + INTEGRATION_SERVICE_RESOURCES_URL + "{1}";  //$NON-NLS-1$ //$NON-NLS-2$

	String URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED = "{0}" + INTEGRATION_SERVICE_RESOURCES_URL + "{1}/{2}";  //$NON-NLS-1$ //$NON-NLS-2$

	String URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE = URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCES_FEED + "/{3}";  //$NON-NLS-1$

	String URI_TEMPLATE_INTEGRATION_SERVICE_RESOURCE_HISTORY = "{0}" + INTEGRATION_SERVICE_URL + "history?resourceId=resources/{1}/{2}/{3}"; //$NON-NLS-1$ //$NON-NLS-2$
	
	String URI_TEMPLATE_MANUALEXECUTIONSCRIPT_RESTSERVICE_REPAIRMANUALEXECUTIONSCRIPT = "{0}" + MANUALEXECUTIONSCRIPT_RESTSERVICE_URL + "RepairManualScript?projectAreaName={1}";  //$NON-NLS-1$ //$NON-NLS-2$

	String URI_TEMPLATE_ATTACHMENT_SERVICE_RESOURCE = "{0}service/com.ibm.rqm.planning.service.internal.rest.IAttachmentRestService/{1}";  //$NON-NLS-1$

	//Templates (resources):
	/**
	 * <p>The template for a query resource name or URI.</p>
	 * 
	 * <p>Substitution tokens (<code>{&lt;zero-based token index&gt;}</code>) mappings:</p>
	 * 
	 * <ul>
	 * <li>{0}: The (optional) resource type namespace URI and resource name.</li>
	 * </ul>
	 * 
	 * <p>For example:</p>
	 * 
	 * <p><code>TestPlanQuery</code></p>
	 * <p><code>http://open-services.net/ns/qm#TestPlanQuery</code></p>
	 * 
	 * @see java.text.MessageFormat#format(String, Object[])
	 */
	String RESOURCE_TEMPLATE_QUERY = "{0}Query"; //$NON-NLS-1$
}