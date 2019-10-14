package manifest

import config.ConfigModel
import file.FileNameUtils
import file.FileWriterFactory
import force.ForceService
import force.MetaFolderEnum
import groovy.io.FileType
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

class DeployManifestService {

    /** Force Service. */
    def forceService
    /** Source path. */
    private String packageDir

    private ConfigModel config

    /** Metadata mapping. */
    def metadata = []

    DeployManifestService(ForceService forceService, ConfigModel config) {
        this.forceService = forceService
        this.config = config
        this.packageDir = config.getPackageDir().getAbsolutePath()
        def metadata = [:]
        forceService.basicMetadata().each { entry ->
            if (entry.value.directoryName != null) {
                metadata[entry.value.directoryName] = entry.key
            }
        }
        this.metadata = metadata
    }

    /**
     * Get a list of components to deploy based on "packageDir" folder.
     * @return list of components to deploy
     */
    private getComponents() {
        def list = []

        def dir = new File(packageDir)
        dir.eachFile (FileType.DIRECTORIES) { file ->
            list << file.getName()
        }
        list.sort()
    }

    /**
     * Generate list of members based on folder content.
     * @param folder folder name
     * @return list of members
     */
    private getSpecificMembers(folder) {
        def metadataDir = new File("$packageDir/$folder")
        // Metadata path length
        def metadataDirLength = metadataDir.getCanonicalPath().length() + 1

        // Members
        def list = []

        metadataDir.eachFileRecurse { file ->
            // Exclude metadata xml files
            if (!file.getCanonicalPath().endsWith('.xml')) {
                // Remove root path
                def metadata = file.getCanonicalPath().substring(metadataDirLength)
                // Replace path separator
                metadata = metadata.replace("\\", "/")
                // Remove extension
                metadata = FileNameUtils.removeFilenameExtension(metadata)
                // Only add custom
                if (metadata != 'unfiled$public') {
                    list << metadata
                }
            }
        }
        list
    }

    /**
     * Generate list of members based on folder content.
     * @param folder folder name
     * @return list of members
     */
    private getMembers(folder) {
        def metadataDir = new File("$packageDir/$folder")
        // Metadata path length
        def metadataDirLength = metadataDir.getCanonicalPath().length() + 1

        // Members
        def list = []

        metadataDir.eachFileRecurse { file ->
            // Exclude metadata xml files
            def metadata = file.getCanonicalPath().substring(metadataDirLength)
            metadata = metadata.replace("\\", "/")
            if (!file.getCanonicalPath().endsWith('.xml') && !metadata.contains('/') && !metadata.contains('\\')) {
                list << FileNameUtils.removeFilenameExtension(file.getName())
            }
        }
        list
    }

    /**
     * Write and build xml.
     * @return null
     */
    def writeBuildXml() {
        FileWriterFactory.create("$packageDir/package.xml")
        def xml = buildXml()
        Manifest.print(xml)
        new OutputStreamWriter(new FileOutputStream("$packageDir/package.xml"), 'UTF-8') << xml
    }

    /**
     * Build xml.
     * @return stream
     */
    def buildXml() {
        def builder = new StreamingMarkupBuilder()
        builder.encoding = 'UTF-8'

        builder.bind {
            mkp.xmlDeclaration()
            Package(xmlns: 'http://soap.sforce.com/2006/04/metadata') {

                components.each { type ->
                    if (MetaFolderEnum.getFolderNames().contains(type)) {
                        types {
                            getSpecificMembers(type).each { fp ->
                                members fp
                            }
                            name getMetadataName(type).capitalize()
                        }
                    } else {
                        types {
                            getMembers(type).each { fp ->
                                members fp
                            }

                            name getMetadataName(type).capitalize()
                        }
                    }
                }

                version forceService.apiVersion

            }
        }
    }

    /**
     * Get metadata.
     * @param  name metadata name
     * @return      metadata
     */
    def getMetadataName(name) {
        if (metadata.containsKey(name)) {
            metadata[name]
        } else {
            name
        }
    }

}
