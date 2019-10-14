package config

import com.sforce.soap.metadata.TestLevel

class ConfigModel {

    private File packageDir = null

    private Boolean checkOnly = true

    private TestLevel testLevel = TestLevel.RunAllTestsInOrg

    private final Boolean rollbackOnError = true

    private final Boolean ignoreWarnings = false

    private final Boolean allowMissingFiles = false

    private final Boolean autoUpdatePackage = false

    ConfigModel(File packageDir, TestLevel testLevel, Boolean checkOnly) {
        this.packageDir = packageDir
        this.testLevel = testLevel
        this.checkOnly = checkOnly
    }

    File getPackageDir() {
        return packageDir
    }

    void setPackageDir(File packageDir) {
        this.packageDir = packageDir
    }

    Boolean getCheckOnly() {
        return checkOnly
    }

    void setCheckOnly(Boolean checkOnly) {
        this.checkOnly = checkOnly
    }

    TestLevel getTestLevel() {
        return testLevel
    }

    void setTestLevel(TestLevel testLevel) {
        this.testLevel = testLevel
    }

    Boolean getRollbackOnError() {
        return rollbackOnError
    }

    Boolean getIgnoreWarnings() {
        return ignoreWarnings
    }

    Boolean getAllowMissingFiles() {
        return allowMissingFiles
    }

    Boolean getAutoUpdatePackage() {
        return autoUpdatePackage
    }

    @Override
    public String toString() {
        return "ConfigModel{" +
                "packageDir=" + packageDir +
                ", checkOnly=" + checkOnly +
                ", testLevel=" + testLevel +
                ", rollbackOnError=" + rollbackOnError +
                ", ignoreWarnings=" + ignoreWarnings +
                ", allowMissingFiles=" + allowMissingFiles +
                ", autoUpdatePackage=" + autoUpdatePackage +
                '}';
    }
}
