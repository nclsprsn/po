package file

import spock.lang.Specification

class FileNameUtilsSpecification extends Specification {

    def 'Remove filename extension'(String a, String b) {
        expect:
        FileNameUtils.removeFilenameExtension(a) == b

        where:
        a | b
        'CustomObject__c.object' | 'CustomObject__c'
        'Home.page-meta.xml' | 'Home.page-meta'
        'Origin Of Clients.report' | 'Origin Of Clients'
        'Account' | 'Account'
    }
}
