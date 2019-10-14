package manifest

import config.ConfigModel
import force.ForceService
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

@Slf4j
class Manifest {

    private static final List<String> destructiveName = [
            'destructiveChangesPre.xml',
            'destructiveChanges.xml',
            'destructiveChangesPost.xml'
    ]

    static print(data) {
        log.debug '\n' + XmlUtil.serialize(data)
    }

    /**
     * Read configuration file
     * @param file configuration file
     * @return configuration
     */
    static Map<String, List<String>> read(String text) {
        Map<String, List<String>> pkg = [:]
        GPathResult xml = new XmlSlurper().parseText(text)
        def members

        xml.each { pkgElm ->
            pkgElm.types.each { type ->
                members = []
                type.members.each { member ->
                    members << member.toString()
                }
                pkg[type.name.toString()] = members
            }
        }

        pkg
    }

    static boolean isDestructiveManifestDetected(ConfigModel config) {
        File dir = config.getPackageDir()
        boolean isDetected = false
        for (String manifest : destructiveName) {
            if (new File(dir, manifest).exists()) {
                isDetected = true
                break
            }
        }
        isDetected
    }

    static transformManifestXML(Map<String, Collection<String>> pkg, ForceService forceService) {
        def builder = new StreamingMarkupBuilder()
        builder.encoding = 'UTF-8'

        builder.bind {
            mkp.xmlDeclaration()
            Package(xmlns: 'http://soap.sforce.com/2006/04/metadata') {

                pkg.each { entry ->
                    if (!entry.value.isEmpty()) {
                        types {
                            entry.value.each { fp ->
                                members fp
                            }
                            name entry.key
                        }
                    }
                }

                version forceService.apiVersion
            }
        }
    }
}
