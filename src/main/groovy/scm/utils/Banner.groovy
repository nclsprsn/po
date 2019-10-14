package scm.utils

import config.ConfigModel
import force.model.EnvironmentModel
import utils.Utils

class Banner {

    static generate(options, ConfigModel conf, EnvironmentModel environment) {
        List<String> msg = ["Org -> ${environment.username}",
                            "Endpoint -> ${environment.endpoint}",
                            "Folder -> ${conf.getPackageDir().getPath()}"]
        if (options.d) {
            msg.addAll(["Destructive -> ${options.x ? 'ON' : 'OFF'}",
                        "Simulate mode -> ${conf.checkOnly ? 'ON' : 'OFF'}",
                        "Test Level -> ${conf.testLevel}"])
        }

        Utils.banner(msg)
    }

}
