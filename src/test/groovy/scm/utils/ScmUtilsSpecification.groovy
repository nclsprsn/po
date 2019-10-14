package scm.utils

import spock.lang.Specification

class ScmUtilsSpecification extends Specification {

    def 'Add meta files to list'() {

        setup:
        List<String> givenFiles = ['src/classes/AgentInvoiceController.cls',
                                   'src/classes/AgentLog.cls',
                                   'src/classes/AgentLog.cls-meta.xml',
                                   'src/classes/AgentLogTest.cls-meta.xml',
                                   'src/classes/Agent-meta.xmlLogTest.cls',
                                   'src/dashboards/Service_Dashboards/KYC.dashboard']
        List<String> givenFilesFormatted = givenFiles.collect { new File(it).getPath() }

        when:
        def keepFiles = ScmUtils.addMetaFile(givenFilesFormatted)

        then:
        List<String> expectedFiles = ['src/classes/AgentInvoiceController.cls',
                                      'src/classes/AgentInvoiceController.cls-meta.xml',
                                      'src/classes/AgentLog.cls',
                                      'src/classes/AgentLog.cls-meta.xml',
                                      'src/classes/Agent-meta.xmlLogTest.cls',
                                      'src/classes/Agent-meta.xmlLogTest.cls-meta.xml',
                                      'src/dashboards/Service_Dashboards/KYC.dashboard',
                                      'src/dashboards/Service_Dashboards/KYC.dashboard-meta.xml',
                                      'src/dashboards/Service_Dashboards-meta.xml']
        List<String> expectedFilesFormatted = expectedFiles.collect{ new File(it).getPath() }
        keepFiles == expectedFilesFormatted

    }

}
