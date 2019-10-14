package manifest

import config.ConfigModel
import file.FileNameUtils
import file.FileWriterFactory
import force.ForceService
import groovy.xml.XmlUtil
import groovy.util.logging.Slf4j
import manifest.reader.CustomLabels
import manifest.reader.CustomObject
import utils.Utils

/**
 * Diff manifest service.
 */
@Slf4j
class DiffManifestService {

    /** Force Service. */
    private ForceService forceService

    /** Config. */
    private ConfigModel config

    /** Exclude conf. */
    Map<String, List<String>> excludeConf = [:]

    Map<String, List<String>> srcManifest = [:]

    Map<String, List<String>> orgManifest = [:]

    private static final List<String> metadataToDelete = ['ApexClass', 'ApexPage', 'CustomObject']

    private static final List<List<String>> metadataToDeleteFile = [
            ['CustomLabels', 'CustomLabel'],
            ['CustomObject', 'CustomField']
    ]

    /**
     * Constructor.
     * @param forceService forceService
     * @param config config
     */
    DiffManifestService(ForceService forceService, ConfigModel config) {
        this.forceService = forceService
        this.config = config
        loadConfiguration()

        // Load src manifest
        DeployManifestService deployManifestService = new DeployManifestService(forceService, config)
        srcManifest = Manifest.read(XmlUtil.serialize(deployManifestService.buildXml()))

        // Load org manifest
        RetrieveManifestService retrieveManifestService = new RetrieveManifestService(forceService, config)
        orgManifest = retrieveManifestService.getPackage()
    }

    /**
     * Loads configuration.
     */
    private loadConfiguration() {
        File exclude = new File(config.getPackageDir().getParent(), '.po' + File.separator + 'destructive.xml')

        if (exclude.exists()) {
            // Load exclusion config
            this.excludeConf = Manifest.read(exclude.getText())
            log.info "Use configuration file for destructive change: ${exclude.getCanonicalPath()}"
        } else {
            log.info 'No configuration file found for destructive change.'
        }
    }

    void writeBuildXml() {
        String filePath = new File(config.packageDir, 'destructiveChangesPost.xml').getAbsolutePath()
        FileWriterFactory.create(filePath)
        def xml = buildXml(DiffEnum.ORG)
        Manifest.print(xml)
        new OutputStreamWriter(new FileOutputStream(filePath), 'UTF-8') << xml
    }

    private Map<String, Collection<String>> diffByFilename(DiffEnum diff) {
        Map<String, Collection<String>> pkg = [:]

        for (String metadata : metadataToDelete) {
            // Source = source + exclude
            List<String> srcMetadata
            // Org = org
            List<String> orgMetadata
            switch (metadata) {
                case 'CustomObject':
                    srcMetadata = srcManifest.get(metadata).findAll { it.endsWith '__c' }
                    orgMetadata = orgManifest.get(metadata).findAll { it.endsWith '__c' }
                    break
                default:
                    srcMetadata = srcManifest.get(metadata)
                    orgMetadata = orgManifest.get(metadata)
                    break
            }

            if (excludeConf.containsKey(metadata)) {
                srcMetadata.addAll(excludeConf.get(metadata))
            }

            // Result
            if(diff == DiffEnum.ORG) {
                pkg.put(metadata, Utils.diffInDestinationNotInSource(srcMetadata, orgMetadata))
            } else {
                pkg.put(metadata, Utils.diffInSourceNotInDestination(srcMetadata, orgMetadata))
            }
        }

        pkg
    }

    private Map<String, Collection<String>> diffByFileContent(DiffEnum diff) {
        Map<String, Collection<String>> pkg = [:]

        def basicMetadata = forceService.basicMetadata()

        for (List<String> metadata : metadataToDeleteFile) {
            def customMetadata = basicMetadata.get(metadata.get(0))
            // Members
            def srcMetadata = []

            new File(config.packageDir, (String) customMetadata.getAt('directoryName')).eachFileRecurse { file ->
                // Exclude metadata xml files
                if (file.getCanonicalPath().endsWith((String) customMetadata.get('suffix'))) {
                    def res2
                    switch (metadata.get(0)) {
                        case 'CustomObject':
                            List<String> res = CustomObject.readObjectFields(file.text)
                            res2 = res.collect {
                                "${FileNameUtils.removeFilenameExtension(file.getName())}.$it".toString()
                            }.findAll { it.endsWith '__c' }
                            break
                        case 'CustomLabels':
                            res2 = CustomLabels.readLabelFields(file.text)
                            break
                        default:
                            throw new Exception()
                    }
                    srcMetadata.addAll(res2)
                }
            }

            if (excludeConf.containsKey(metadata.get(1))) {
                srcMetadata.addAll(excludeConf.get(metadata.get(1)))
            }

            // Source = source + exclude
            List<String> orgMetadata = orgManifest.get(metadata.get(1))

            // Result
            if(diff == DiffEnum.ORG) {
                pkg.put(metadata.get(1), Utils.diffInDestinationNotInSource(srcMetadata, orgMetadata))
            } else {
                pkg.put(metadata.get(1), Utils.diffInSourceNotInDestination(srcMetadata, orgMetadata))
            }
        }

        pkg
    }

    /**
     * Build xml.
     * @return stream
     */
    def buildXml(DiffEnum diff) {
        Map<String, Collection<String>> pkgOrg = [:]

        pkgOrg.putAll(diffByFilename(diff))
        pkgOrg.putAll(diffByFileContent(diff))

        Manifest.transformManifestXML(pkgOrg, forceService)
    }

}
