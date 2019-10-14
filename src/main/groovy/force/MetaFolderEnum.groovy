package force

enum MetaFolderEnum {

    /** Email. */
    Email('email'),
    /** Report. */
    Report('reports'),
    /** Dashboard. */
    Dashboard('dashboards'),
    /** Document. */
    Document('documents')

    /** Folder name. */
    String folderName

    MetaFolderEnum(String folderName) {
        this.folderName = folderName
    }

    static List<String> getFolderNames() {
        values().collect { it.folderName }
    }
}
