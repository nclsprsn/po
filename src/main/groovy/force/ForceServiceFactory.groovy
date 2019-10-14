package force

import groovy.util.logging.Slf4j
import force.model.EnvironmentModel

/**
 * Force service factory.
 */
@Slf4j
class ForceServiceFactory {

    static create(EnvironmentModel config) {

        log.debug("Connecting at ${config.endpoint}....")
        log.debug("Logged in as ${config.username}" )

        /**
         * Create force service instance.
         */
        new ForceService(
            config.endpoint,
            config.username,
            config.password
        )
    }
}
