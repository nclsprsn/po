package archive

import force.model.EnvironmentModel

import java.text.SimpleDateFormat

/**
 * Archive.
 */
class Archive {

    /** Archive directory.*/
    File dir

    /** Credentials. */
    EnvironmentModel environment

    /**
     * Constructor.
     * @param dir Archive directory
     * @param environment Environment
     */
    Archive(File dir, EnvironmentModel environment) {
        this.dir = dir
        this.environment = environment
    }

    /**
     * Create archive.
     * @param bytes content
     */
    def create(byte[] bytes) {
        dir.mkdirs()
        String current = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm").format(Calendar.getInstance().getTime())
        File archive = new File(dir, "${environment.getUsername()}_${current}.zip")
        FileOutputStream fos = new FileOutputStream(archive)
        fos.write(bytes)
        fos.close()
        archive.getAbsolutePath()
    }
}
