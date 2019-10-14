package scm

import config.ConfigModel

class GitServiceImpl {

    private final static bin = 'git'

    private ConfigModel configModel

    GitServiceImpl(ConfigModel configModel) {
        this.configModel = configModel
    }

    List<String> modifiedFile(String ver1, String ver2) {
        List<String> files = []
        Process process = execute([bin, 'diff', '--name-only', ver1, ver2])

        process.inputStream.eachLine { line ->
            files.add(line)
        }
        process.waitFor()

        if (!process.exitValue()) {
            files
        } else {
            []
        }
    }

    Process execute(List<String> command) {
        new ProcessBuilder(command)
                .directory(configModel.getPackageDir())
                .redirectErrorStream(true)
                .start()
    }

}
