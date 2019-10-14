package manifest.reader

import groovy.util.slurpersupport.GPathResult

class CustomObject {

    static List<String> readObjectFields(String text) {
        List<String> fields = []
        GPathResult xml = new XmlSlurper().parseText(text)

        xml.each { customObject ->
            customObject.fields.each { field ->
                fields << field.fullName.toString()
            }
        }

        fields
    }

}
