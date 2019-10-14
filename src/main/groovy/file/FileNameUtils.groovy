package file

/**
 * File name utils.
 */
class FileNameUtils {

    /**
     * Remove file extension.
     * @param filename filename
     * @return filename without extension name
     */
    static String removeFilenameExtension(String filename, String separator) {
        if(filename.lastIndexOf(separator) != -1) {
            filename.take(filename.lastIndexOf(separator))
        } else {
            filename
        }
    }

    /**
     * Remove file extension.
     * @param filename filename
     * @return filename without extension name
     */
    static String removeFilenameExtension(String filename) {
        return removeFilenameExtension(filename, '.')
    }
}
