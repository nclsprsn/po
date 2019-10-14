package scm.utils

import config.ConfigModel
import force.MetaFolderEnum

class ScmUtils {

    static List<String> addMetaFile(List<String> list) {
        List<String> res = []
        list.findAll { !it.endsWith('-meta.xml') }.collect ({ it ->
            res.addAll([it, "$it-meta.xml".toString()])
            // Add folders meta.xml if necessary
            if (MetaFolderEnum.getFolderNames().contains(new File(it).getParentFile().getParentFile().getName())) {
                res.add("${new File(it).getParent()}-meta.xml".toString())
            }
            res
        })
        res
    }

    static List<String> absolutePath(List<String> list, ConfigModel configModel) {
        List<String> res = []
        list.each { file ->
            res.add(new File(configModel.getPackageDir().getParent(), file).getAbsolutePath())
        }
        res
    }
}
