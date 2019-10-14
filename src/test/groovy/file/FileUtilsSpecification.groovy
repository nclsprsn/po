package file

import spock.lang.Specification

class FileUtilsSpecification extends Specification {

    File tmpDir = new File(System.getProperty('java.io.tmpdir'))

    String testFolder = "${this.class.getSimpleName()}Folder"

    def 'Delete folder'() {
        setup:
        def folder = new File(tmpDir,  testFolder)
        folder.mkdirs()

        when:
        FileUtils.delete(folder)

        then:
        !folder.exists()

        cleanup:
        folder.delete()
    }

    def 'Delete folders recursively'() {
        setup:
        def folder = new File(tmpDir,  testFolder)
        folder.mkdirs()
        def level1Folder = new File(folder,  'level1')
        level1Folder.mkdirs()
        def level1File = new File(level1Folder, 'level1File')
        level1File << 'Content'
        def level2Folder = new File(level1Folder,  'level2')
        level2Folder.mkdirs()
        def level2File = new File(level2Folder, 'level2File')
        level2File << 'Content'

        when:
        FileUtils.delete(folder)

        then:
        !folder.exists()
        !level1Folder.exists()
        !level2Folder.exists()
        !level1File.exists()
        !level2File.exists()

        cleanup:
        folder.delete()
    }
}
