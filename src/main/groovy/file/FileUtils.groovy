package file

import groovy.util.logging.Slf4j

/**
 * File utils.
 */
@Slf4j
class FileUtils {

    /**
     * Delete files and folders recursively.
     * @param file file or directory
     * @throws IOException IOException
     */
    static void delete(File file) throws IOException {

        if (file.isDirectory()) {
            //directory is empty, then delete it
            if (file.list().length == 0) {
                file.delete()
                log.debug "Directory is deleted : ${file.getAbsolutePath()}"
            } else {
                //list all the directory contents
                String[] files = file.list()
                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp)

                    //recursive delete
                    delete(fileDelete)
                }
                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete()
                    log.debug "Directory is deleted : ${file.getAbsolutePath()}"
                }
            }
        } else {
            //if file, then delete it
            file.delete()
            log.debug "File is deleted : ${file.getAbsolutePath()}"
        }
    }

    static void delete(File file, List<String> exclude) throws IOException {

        if (file.isDirectory()) {
            //directory is empty, then delete it
            if (file.list().length == 0) {
                file.delete()
                log.debug "Directory is deleted : ${file.getAbsolutePath()}"
            } else {
                //list all the directory contents
                String[] files = file.list()
                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp)

                    if (!exclude.contains(fileDelete.getAbsolutePath())) {
                        //recursive delete
                        delete(fileDelete, exclude)
                    }
                }
                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete()
                    log.debug "Directory is deleted : ${file.getAbsolutePath()}"
                }
            }
        } else {
            if (!exclude.contains(file.getAbsolutePath())) {
                //if file, then delete it
                file.delete()
                log.debug "File is deleted : ${file.getAbsolutePath()}"
            }
        }
    }
}
