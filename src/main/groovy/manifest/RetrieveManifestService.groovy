package manifest

import com.sforce.soap.metadata.ListMetadataQuery
import config.ConfigModel
import file.FileWriterFactory
import force.ForceService
import groovy.util.logging.Slf4j
import groovy.xml.StreamingMarkupBuilder
import manifest.model.Component
import manifest.model.ComponentType
import manifest.utils.Flow

@Slf4j
class RetrieveManifestService {

    /** Force Service. */
    private ForceService forceService

    private ConfigModel config

    String packageFile = 'package.xml'

    /** Exclude conf. */
    Map<String, List<String>> excludeConf = [:]

    RetrieveManifestService(ForceService forceService, ConfigModel config) {
        this.forceService = forceService
        this.config = config
        loadConfiguration()
    }

    /**
     * Loads configuration.
     */
    private loadConfiguration() {
        File exclude = new File(config.getPackageDir().getParentFile(), '.po' + File.separator + 'retrieve.xml')

        if (exclude.exists()) {
            // Load exclusion config
            this.excludeConf = Manifest.read(exclude.getText())
            log.info "Use configuration file for retrieve: ${exclude.getCanonicalPath()}"
        } else {
            log.info "No configuration file found for retrieve in ${exclude.getCanonicalPath()}."
        }
    }

    Map<String, List<String>> getPackage() {

        ComponentType component_type_record
        Component component_record
        def component_type_records = []
        def component_records = []
        def component_list = []

        forceService.basicMetadata().each { component_type ->

            component_type.value.childNames.each { child_component ->
                component_type_record = new ComponentType()
                component_type_record.name = child_component
                component_type_records.push(component_type_record)
            }

            component_type_record = new ComponentType()
            component_type_record.name = component_type.value.name
            component_type_records.push(component_type_record)

            if (component_type.value.inFolder) {
                String component1
                if (component_type.value.name == 'EmailTemplate') {
                    component1 = 'EmailFolder'
                } else {
                    component1 = component_type.value.name + 'Folder'
                }

                forceService.listMetadata(component1).each { folder ->
                    component_record = new Component()
                    component_record.component_type = component_type_record
                    component_record.name = folder.fullName
                    component_record.include = true
                    component_records.push(component_record)

                    forceService.listMetadata(component_type.value.name, folder.fullName).each { folder_component ->
                        component_record = new Component()
                        component_record.component_type = component_type_record
                        component_record.name = folder_component.fullName
                        component_record.include = true
                        component_records.push(component_record)
                    }
                }
            } else {
                def queries = component_type.value.childNames.collect { child_component ->
                    forceService.withValidMetadataType(child_component) {
                        def query = new ListMetadataQuery()
                        query.type = it
                        query
                    }
                }
                forceService.listMetadata(queries).each { component ->
                    component_record = new Component()
                    component_record.component_type = (ComponentType)component_type_records.findAll{ it.name == component.type }[0]
                    component_record.name = component.fullName
                    component_record.include = true
                    component_records.push(component_record)
                }

                component_list.add(component_type.value.name)

            }
        }

        def queries = component_list.collect { child_component ->
            forceService.withValidMetadataType(child_component) {
                def query = new ListMetadataQuery()
                query.type = it
                query
            }
        }

        forceService.listMetadata(queries).each { component ->
            component_record = new Component()
            component_record.component_type = (ComponentType)component_type_records.findAll{ it.name == component.type }[0]
            component_record.name = component.fullName
            component_record.include = true
            if (component_record.component_type == null) {
                if (component.fileName.startsWith('globalValueSetTranslations')) {
                    component_record.component_type = (ComponentType)component_type_records.findAll{ it.name == 'GlobalValueSetTranslation' }[0]
                }
            }
            component_records.push(component_record)
        }

        def pkg = [:]

        component_records.each { component ->
            if (component.include) {
                if (component.component_type != null) {
                    if (pkg.containsKey(component.component_type.name)) {
                        pkg.get(component.component_type.name) << component.name
                    } else {
                        pkg.put(component.component_type.name, [component.name])
                    }
                } else {
                    log.error "Unknown component: $component"
                }
            }
        }

        pkg.each {
            it.value = it.value.sort { it }
        }

        Flow flow = new Flow(forceService)

        pkg['Flow'] = flow.getFlows()
        pkg['FlowDefinition'] = flow.getFlowDefinitions()

        pkg = pkg.sort { it.key }

        pkg = removeExclude(pkg)

        pkg
    }

    def removeExclude(Map<String, List<String>> pkg) {
        if (excludeConf != null ) {
            Map<String, List<String>> pkgCleaned = [:]
            List<String> metadata = []
            pkg.each { k, v ->
                if (excludeConf.get(k) == null || !excludeConf.get(k).contains('*')) {
                    metadata = []
                    v.each { it ->
                        if (excludeConf.get(k) == null || !excludeConf.get(k).contains(it)) {
                            metadata << it
                        }
                    }
                    if (metadata.size() > 0 ) {
                        pkgCleaned.put(k, metadata)
                    }
                }
            }
            pkgCleaned
        } else {
            pkg
        }
    }

    /**
     * Write and build xml.
     * @return null
     */
    def writeBuildXml() {
        String filePath = new File(config.packageDir, packageFile).getAbsolutePath()
        FileWriterFactory.create(filePath)
        def xml = buildXml()
        Manifest.print(xml)
        new OutputStreamWriter(new FileOutputStream(filePath), 'UTF-8') << xml
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

                getPackage().each { entry ->
                    types {
                        entry.value.each { fp ->
                            members fp
                        }
                        name entry.key
                    }
                }

                version forceService.apiVersion
            }
        }
    }

}