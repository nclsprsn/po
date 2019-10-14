package force

import com.sforce.soap.metadata.ListMetadataQuery
import com.sforce.soap.metadata.MetadataConnection
import com.sforce.soap.partner.LoginResult
import com.sforce.soap.partner.PartnerConnection
import com.sforce.soap.tooling.ToolingConnection
import com.sforce.ws.ConnectionException
import com.sforce.ws.ConnectorConfig
import com.sforce.ws.SoapFaultException
import groovy.util.logging.Slf4j
/**
 * Force service.
 */
@Slf4j
class ForceService {

    /** Salesforce Org metadata. */
    def metadata
    /** Salesforce Org metadata types. */
    def metadataTypes
    /** Salesforce API version. */
    public String apiVersion

    MetadataConnection metadataConnection

    ToolingConnection toolConnection

    /**
     * Constructor.
     * @param serverUrl server url
     * @param username username
     * @param password password
     * @param apiVersion api version
     */
    ForceService(String serverUrl, String username, String password) {

        this.apiVersion = '40.0'
        def url = "${serverUrl}/services/Soap/u/${apiVersion}"

        final LoginResult loginResult = loginToSalesforce(username, password, url);
        metadataConnection = createMetadataConnection(loginResult)
        toolConnection = createToolingConnection(loginResult)

        metadataConnection.setCallOptions("AntMigrationTool/${apiVersion}")
    }

    private static MetadataConnection createMetadataConnection(
            final LoginResult loginResult) throws ConnectionException {
        final ConnectorConfig config = new ConnectorConfig()
        config.setServiceEndpoint(loginResult.getMetadataServerUrl())
        config.setSessionId(loginResult.getSessionId())
        return new MetadataConnection(config)
    }

    private static ToolingConnection createToolingConnection(
            final LoginResult loginResult) throws ConnectionException {
        final ConnectorConfig partnerConfig = new ConnectorConfig()
        partnerConfig.setManualLogin(true)
        ConnectorConfig toolingConfig = new ConnectorConfig()
        toolingConfig.setSessionId(loginResult.getSessionId())
        toolingConfig.setServiceEndpoint(loginResult.getServerUrl().replace('u', 'T'))
        return new ToolingConnection(toolingConfig)
    }

    private static LoginResult loginToSalesforce(
            final String username,
            final String password,
            final String loginUrl) throws ConnectionException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setAuthEndpoint(loginUrl);
        config.setServiceEndpoint(loginUrl);
        config.setManualLogin(true);
        return (new PartnerConnection(config)).login(username, password);
    }

    /**
     * Is a valid metadata type.
     * @param type metadata type
     * @return true if valid, otherwise false
     */
    def isValidMetadataType(type) {
        if (metadata == null) {
            metadata = basicMetadata()

            metadataTypes = []
            metadataTypes << metadata.keySet()
            // Add child names metadata
            metadata.each { k, v ->
                v.childNames.each {
                    if (it) {
                        metadataTypes << it
                    }
                }
            }
            metadataTypes = metadataTypes.flatten() as Set
        }

        metadataTypes.contains(type)
    }

    /**
     * Valid metadata clojure mode.
     * @param type metadata type
     * @param closure closure
     * @return execute closure if valid, otherwise return null.
     */
    def withValidMetadataType(type, Closure closure) {
        if (isValidMetadataType(type)) {
            closure(type)
        } else {
            log.warn "WARNING: $type is an invalid metadata type for this Organisation"
            null
        }
    }

    /**
     * SOQL Query.
     * @param soql soql.
     * @return result list of the soql query
     */
    def query(soql) {
        def result = []

        def queryResult = connection.query soql

        if (queryResult.size > 0) {
            for (; ;) {
                queryResult.records.each { result << it }

                if (queryResult.isDone()) {
                    break
                }

                queryResult = connection.queryMore queryResult.queryLocator
            }
        }

        result
    }

    /**
     * Retrieve basic metadata.
     * @return Map of metadata and theirs child names
     */
    def basicMetadata() {
        def metadata = [:]

        def result = metadataConnection.describeMetadata(apiVersion.toDouble())
        if (result) {
            result.metadataObjects.each { obj ->
                def name = obj.xmlName

                metadata[name] = [
                        name         : name,
                        childNames   : obj.childXmlNames.collect { it } as Set,
                        inFolder     : obj.inFolder,
                        directoryName: obj.directoryName,
                        suffix       : obj.suffix
                ]
            }
        }

        metadata
    }

    /**
     * List metadata.
     * @param type type
     * @return list of file properties
     */
    def listMetadata(String type) {
        listMetadata(type, null)
    }

    /**
     * List metadata.
     * @param type type
     * @param folder folder
     * @return list of file properties
     */
    def listMetadata(String type, String folder) {
        def query = new ListMetadataQuery()
        query.type = type
        query.folder = folder

        listMetadata([query])
    }

    /**
     * List metadata.
     * @param queries queries
     * @return list of file properties
     */
    def listMetadata(List<ListMetadataQuery> queries) {
        final MAX_QUERIES_PER_REQUEST = 3

        def numQueries = queries.size
        def isLastQuery = false
        def index = 0
        def apiVersion = this.apiVersion.toDouble()

        def fileProperties = []
        while (numQueries > 0 && !isLastQuery) {
            def start = index * MAX_QUERIES_PER_REQUEST

            def end = start + MAX_QUERIES_PER_REQUEST
            if (end > numQueries) {
                end = numQueries
            }

            def requestQueries = queries.subList(start, end) as ListMetadataQuery[]
            def result = null
            try {
                result = metadataConnection.listMetadata(requestQueries, apiVersion)
            } catch (SoapFaultException e) {
                if (e.faultCode.localPart == 'INVALID_TYPE') {
                    log.warn "WARNING: ${e.message}"
                } else {
                    throw e
                }
            }

            if (result != null) {
                fileProperties.addAll(result.toList())
            }

            isLastQuery = (numQueries - (++index * MAX_QUERIES_PER_REQUEST)) < 1
        }
        fileProperties
    }

    /**
     * List metadata for specific types
     * @param types types
     * @return list of file properties
     */
    def listMetadataForTypes(types) {
        def queries = types.collect { type ->
            withValidMetadataType(type) {
                def query = new ListMetadataQuery()
                query.type = it
                query
            }
        }
        queries.removeAll([null])

        listMetadata(queries)
    }

    /**
     * Build connection url.
     */
    private buildConnectionUrl = { serverUrl, username, password ->
        def encode = { URLEncoder.encode(it, 'UTF-8') }

        def host = new URI(serverUrl).host
        def query = [
                user    : username,
                password: password
        ].collect { k, v -> "${encode k}=${encode v}" }.join('&')

        "force://$host?$query"
    }
}