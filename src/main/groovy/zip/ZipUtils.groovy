package zip

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Zip library.
 */
@Slf4j
class ZipUtils {

    final static Integer ZIP_BUFFER = 2048

    static unZipToFolder(InputStream inputStream, File outputDir) {
        def zipStream = new ZipInputStream(inputStream)

        byte[] buffer = new byte[ZIP_BUFFER]

        try {
            ZipEntry entry
            while ((entry = zipStream.getNextEntry()) != null) {
                log.debug String.format("Read entry: %s", entry.getName())

                String outpath = outputDir.getAbsolutePath() + File.separator + entry.getName()
                log.debug "Write entry: ${outpath}"
                new File(outpath).parentFile.mkdirs()
                new File(outpath).createNewFile()
                FileOutputStream output = null
                try {
                    output = new FileOutputStream(outpath)
                    int len = 0
                    while ((len = zipStream.read(buffer)) > 0) {
                        output.write(buffer, 0, len)
                    }
                } finally {
                    if (output != null) {
                        output.close()
                    }
                }
            }
        }
        finally {
            zipStream.close()
        }
    }

    /**
     * Unzip.
     * @param path unzip path
     * @param inputStream zip data
     */
    static Map<String, GPathResult> unZipBuffered(InputStream inputStream) {
        Map<String, GPathResult> res = new HashMap<String, GPathResult>()
        def zipStream = new ZipInputStream(inputStream)

        byte[] buffer = new byte[ZIP_BUFFER]

        try {
            ZipEntry entry
            while ((entry = zipStream.getNextEntry()) != null) {
                log.debug String.format("Read entry: %s", entry.getName())

                ByteArrayOutputStream output = null
                try {
                    output = new ByteArrayOutputStream()
                    int len = 0
                    while ((len = zipStream.read(buffer)) > 0) {
                        output.write(buffer, 0, len)
                    }
                } finally {
                    if (output != null) {
                        output.close()
                    }
                }

                res.put(entry.getName(), new XmlSlurper().parse(new ByteArrayInputStream(output.toByteArray())))
            }
        }
        finally {
            zipStream.close()
        }
        res
    }


    static byte[] zipFromFolder(File inputDir) {
        log.info "Compress folder: ${inputDir.getPath()}"

        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        ZipOutputStream result = new ZipOutputStream(bos)
        result.withStream {zipOutStream ->

            def buffer = new byte[ZIP_BUFFER]

            inputDir.eachFileRecurse { f ->
                if(!f.isDirectory()) {

                    def filePathZip = (f.getPath().replace('\\', '/') - (inputDir.getPath().replace('\\', '/') + '/'))
                    log.debug filePathZip

                    zipOutStream.putNextEntry(new ZipEntry(filePathZip))
                    new FileInputStream(f).withStream { inStream ->
                        def count = 0
                        while((count = inStream.read(buffer)) > 0) {
                            zipOutStream.write(buffer, 0, count)
                        }
                    }
                    zipOutStream.closeEntry()
                }
            }
        }
        log.info "Folder compressed: ${inputDir.getPath()}"

        bos.toByteArray()
    }
}
