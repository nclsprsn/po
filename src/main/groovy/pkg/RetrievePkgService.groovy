package pkg

import com.sforce.soap.metadata.*
import com.sforce.ws.ConnectionException
import config.ConfigModel
import force.ForceService
import groovy.util.logging.Slf4j
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

@Slf4j
class RetrievePkgService {

    private ForceService forceService

    private ConfigModel config

    private PackageManifestParser manifestParser = new PackageManifestParser()

    RetrievePkgService(ForceService forceService, ConfigModel config) {
        this.forceService = forceService
        this.config = config
    }

    // manifest file that controls which components get retrieved
    private static final String MANIFEST_FILE = "package.xml"

    // one second in milliseconds
    private static final long ONE_SECOND = 1000

    // maximum number of attempts to deploy the zip file
    private static final int MAX_NUM_POLL_REQUESTS = 50


    byte[] retrieveZip() throws Exception {
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        // The version in package.xml overrides the version in RetrieveRequest
        retrieveRequest.setApiVersion(Double.valueOf(forceService.apiVersion))
        retrieveRequest.setSinglePackage(true)
        setUnpackaged(retrieveRequest)

        AsyncResult asyncResult = forceService.metadataConnection.retrieve(retrieveRequest);
        RetrieveResult result = waitForRetrieveCompletion(asyncResult)

        handleResponse(forceService.metadataConnection, result)

        return result.getZipFile()
    }



    private RetrieveResult waitForRetrieveCompletion(AsyncResult asyncResult) throws Exception {
        // Wait for the retrieve to complete
        int poll = 0
        long waitTimeMilliSecs = ONE_SECOND
        String asyncResultId = asyncResult.getId()
        RetrieveResult result = null
        Boolean isDone = false
        while(!isDone) {
            Thread.sleep(waitTimeMilliSecs);
            // Double the wait time for the next iteration
            waitTimeMilliSecs *= 2
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception("Request timed out.  If this is a large set " +
                        "of metadata components, check that the time allowed " +
                        "by MAX_NUM_POLL_REQUESTS is sufficient.")
            }
            result = forceService.metadataConnection.checkRetrieveStatus(
                    asyncResultId, true)
            isDone = result.isDone()
            logCurrentStatus(result)
        }

        return result
    }

    private void setUnpackaged(RetrieveRequest request) throws Exception {
        // Edit the path, if necessary, if your package.xml file is located elsewhere
        File unpackedManifest = new File(config.getPackageDir(), MANIFEST_FILE)
        log.info "Read manifest file: ${unpackedManifest.getAbsolutePath()}"

        if (!unpackedManifest.exists() || !unpackedManifest.isFile()) {
            throw new Exception("Should provide a valid retrieve manifest " +
                    "for unpackaged content. Looking for " +
                    unpackedManifest.getAbsolutePath())
        }

        // Note that we use the fully quualified class name because
        // of a collision with the java.lang.Package class
        Package p = parsePackage(unpackedManifest)
        log.debug p.toString()
        request.setUnpackaged(p)
        log.info "Manifest file read: ${unpackedManifest.getAbsolutePath()}"
    }

    Package parsePackage(File file) {
        try {
            Element root = this.manifestParser.parse(file);
            Package p = new Package();
            List<PackageTypeMembers> packageMembers = new ArrayList();

            for(Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                if(n instanceof Element) {
                    Element e = (Element)n;
                    if(e.getNodeName().equals("version")) {
                        p.setVersion(e.getTextContent());
                    }

                    NodeList names = e.getElementsByTagName("name");
                    if(names.getLength() != 0) {
                        String name = names.item(0).getTextContent();
                        NodeList m = e.getElementsByTagName("members");
                        List<String> members = new ArrayList();

                        for(int i = 0; i < m.getLength(); ++i) {
                            members.add(m.item(i).getTextContent());
                        }

                        PackageTypeMembers pdi = new PackageTypeMembers();
                        pdi.setName(name);
                        pdi.setMembers((String[])members.toArray(new String[members.size()]));
                        packageMembers.add(pdi);
                    }
                }
            }

            p.setTypes((PackageTypeMembers[])packageMembers.toArray(new PackageTypeMembers[packageMembers.size()]));
            return p;
        } catch (FileNotFoundException var12) {
            log.error var12.toString()
            System.exit(1)
        }
    }

    public static class PackageManifestParser {
        public PackageManifestParser() {
        }

        public Element parse(File file) throws FileNotFoundException {
            FileInputStream is = new FileInputStream(file);

            Element var4;
            try {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                var4 = db.parse(is).getDocumentElement();
            } catch (ParserConfigurationException var15) {
                log.error "Cannot create XML parser ${var15.toString()}"
                System.exit(1)
            } catch (IOException var16) {
                log.error var16.toString()
                System.exit(1)
            } catch (SAXException var17) {
                log.error var17.toString()
                System.exit(1)
            } finally {
                try {
                    is.close();
                } catch (IOException var14) {
                    ;
                }

            }

            return var4;
        }
    }

    void logCurrentStatus(RetrieveResult result) {
        if(result != null) {
            log.info "Request Status: " + result.getStatus()
        }

    }

    public void handleResponse(MetadataConnection metadataConnection, RetrieveResult result) throws ConnectionException {

        if(result.getMessages() != null && result.getMessages().length > 0) {
            StringBuilder buf = new StringBuilder();

            buf.append("Retrieve warnings (" + result.getMessages().length + "):");
            RetrieveMessage[] var11 = result.getMessages();
            int var6 = var11.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                RetrieveMessage rm = var11[var7];
                buf.append('\n' + rm.getFileName() + " - " + rm.getProblem());
            }

            buf.insert(0, "\n-----------------------------------------------------------------------------------\n");
            buf.append("\n-----------------------------------------------------------------------------------");
            log.warn buf.toString()
        }

    }
}
