package manifest.reader

import groovy.util.slurpersupport.GPathResult

class CustomLabels {

    static List<String> readLabelFields(String text) {
        List<String> fields = []
        GPathResult xml = new XmlSlurper().parseText(text)

        xml.each { customLabel ->
            customLabel.labels.each { field ->
                fields << field.fullName.toString()
            }
        }

        fields
    }

}
