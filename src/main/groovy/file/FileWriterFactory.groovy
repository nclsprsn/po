package file

/**
 * File writer factory.
 */
class FileWriterFactory {

    /**
     * Create file.
     * @param filePath file path
     * @return file
     */
    static create(String filePath) {
        def file = new File(filePath)
        def parentFile = file.parentFile

        if (parentFile) {
            parentFile.mkdirs()
        }

        new FileWriter(file)
    }
}