package file

import spock.lang.Specification

class FileWriteFactorySpecification extends Specification {

    File tmpDir = new File(System.getProperty('java.io.tmpdir'))

    String testFolder = "${this.class.getSimpleName()}Folder"

    def 'Create file'() {
        setup:
        def folder = new File(tmpDir,  testFolder)
        def level1File = new File(folder, 'level1File')

        when:
        FileWriterFactory.create(level1File.getAbsolutePath())

        then:
        level1File.exists()

        cleanup:
        folder.delete()
    }
}
