package force.model

/**
 * Environment model.
 */
class EnvironmentModel {

    /** Endpoint. */
    String endpoint
    /** Username. */
    String username
    /** Password. */
    String password
    /** Is confirmation required. */
    boolean isConfirmationRequired

    /**
     * Constructor.
     * @param endpoint environment
     * @param username username
     * @param password password
     * @param isConfirmationRequired isConfirmationRequired
     * @param apiVersion api version
     */
    EnvironmentModel(String endpoint, String username, String password, boolean isConfirmationRequired) {
        this.endpoint = endpoint
        this.username = username
        this.password = password
        this.isConfirmationRequired = isConfirmationRequired
    }
}
