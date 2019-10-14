package pkg

import com.sforce.soap.metadata.*
import com.sforce.ws.ConnectionException
import config.ConfigModel
import force.ForceService
import groovy.util.logging.Slf4j

@Slf4j
class DeployPkgService {

    private ForceService forceService

    private ConfigModel config

    private static final String ZIP_FILE = "components.zip"

    // one second in milliseconds
    private static final long ONE_SECOND = 1000

    // maximum number of attempts to deploy the zip file
    private static final int MAX_NUM_POLL_REQUESTS = 1800

    Set<DeployMessageKey> printedInProgressDeployFailures
    Set<RunTestsResultKey> printedInProgressTestFailures
    int inProgressDeployFailuresIndex
    int inProgressTestFailuresIndex


    DeployPkgService(ForceService forceService, ConfigModel config) {
        this.forceService = forceService
        this.config = config

        this.printedInProgressDeployFailures = new HashSet()
        this.printedInProgressTestFailures = new HashSet()
        this.inProgressDeployFailuresIndex = 1
        this.inProgressTestFailuresIndex = 1
    }

    void deployZip(byte[] zipBytes) throws Exception {
        //byte[] zipBytes = readZipFile()
        DeployOptions deployOptions = new DeployOptions()
        deployOptions.setPerformRetrieve(false)
        deployOptions.setRollbackOnError(config.getRollbackOnError())
        deployOptions.setCheckOnly(config.getCheckOnly())
        deployOptions.setTestLevel(config.getTestLevel())
        deployOptions.setAllowMissingFiles(config.getAllowMissingFiles())
        deployOptions.setIgnoreWarnings(config.getIgnoreWarnings())
        deployOptions.setAutoUpdatePackage(config.getAutoUpdatePackage())
        deployOptions.setSinglePackage(true)

        log.info 'Submit request for a deploy.'
        AsyncResult asyncResult = forceService.metadataConnection.deploy(zipBytes, deployOptions)
        log.info 'Request for a deploy submitted successfully.'
        log.info "Request ID for the current deploy task: ${asyncResult.getId()}"
        DeployResult result = waitForDeployCompletion(asyncResult.getId())

        handleResponse(forceService.metadataConnection, result)
        /*
        if (!result.isSuccess()) {
            printErrors(result, "Final list of failures:\n")
            throw new Exception("The files were not successfully deployed")
        }*/
    }

    /*
    * Read the zip file contents into a byte array.
    */

    private byte[] readZipFile() throws Exception {
        byte[] result = null
        // We assume here that you have a deploy.zip file.
        // See the retrieve sample for how to retrieve a zip file.
        File zipFile = new File(ZIP_FILE)
        if (!zipFile.exists() || !zipFile.isFile()) {
            throw new Exception("Cannot find the zip file for deploy() on path:"
                    + zipFile.getAbsolutePath())
        }

        FileInputStream fileInputStream = new FileInputStream(zipFile)
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream()
            byte[] buffer = new byte[4096]
            int bytesRead = 0
            while (-1 != (bytesRead = fileInputStream.read(buffer))) {
                bos.write(buffer, 0, bytesRead)
            }

            result = bos.toByteArray()
        } finally {
            fileInputStream.close()
        }
        return result
    }

    /*
    * Print out any errors, if any, related to the deploy.
    * @param result - DeployResult
    */

    private void printErrors(DeployResult result, String messageHeader) {
        DeployDetails details = result.getDetails()
        StringBuilder stringBuilder = new StringBuilder()
        if (details != null) {
            DeployMessage[] componentFailures = details.getComponentFailures()
            for (DeployMessage failure : componentFailures) {
                String loc = "(" + failure.getLineNumber() + ", " + failure.getColumnNumber()
                if (loc.length() == 0 && !failure.getFileName().equals(failure.getFullName())) {
                    loc = "(" + failure.getFullName() + ")";
                }
                stringBuilder.append(failure.getFileName() + loc + ":"
                        + failure.getProblem()).append('\n')
            }
            RunTestsResult rtr = details.getRunTestResult()
            if (rtr.getFailures() != null) {
                for (RunTestFailure failure : rtr.getFailures()) {
                    String n = (failure.getNamespace() == null ? "" :
                            (failure.getNamespace() + ".")) + failure.getName()
                    stringBuilder.append("Test failure, method: " + n + "." +
                            failure.getMethodName() + " -- " + failure.getMessage() +
                            " stack " + failure.getStackTrace() + "\n\n")
                }
            }
            if (rtr.getCodeCoverageWarnings() != null) {
                for (CodeCoverageWarning ccw : rtr.getCodeCoverageWarnings()) {
                    stringBuilder.append("Code coverage issue")
                    if (ccw.getName() != null) {
                        String n = (ccw.getNamespace() == null ? "" :
                                (ccw.getNamespace() + ".")) + ccw.getName()
                        stringBuilder.append(", class: " + n)
                    }
                    stringBuilder.append(" -- " + ccw.getMessage() + "\n")
                }
            }
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.insert(0, messageHeader)
            log.info stringBuilder.toString()
        }
    }

    private DeployResult waitForDeployCompletion(String asyncResultId) throws Exception {
        log.info 'Waiting for server to finish processing the request...'
        int poll = 0
        long waitTimeMilliSecs = ONE_SECOND * 5
        DeployResult deployResult = null
        Boolean isDone = false
        while (!isDone) {
            Thread.sleep(waitTimeMilliSecs)
            // double the wait time for the next iteration

            //waitTimeMilliSecs *= 2
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception(
                        "Request timed out. If this is a large set of metadata components, " +
                                "ensure that MAX_NUM_POLL_REQUESTS is sufficient.")
            }

            deployResult = forceService.metadataConnection.checkDeployStatus(asyncResultId, true);
            /*if (deployResult.getDetails().getComponentSuccesses().length > 0) {
                log.info "Request Status: ${deployResult.status} (${deployResult.numberComponentsDeployed}/${deployResult.numberComponentsTotal})  -- Processing Type: ${deployResult.getDetails().getComponentSuccesses()[deployResult.getDetails().getComponentSuccesses().length - 1].componentType}"
            } else {
                log.info "Request Status: ${deployResult.status}"
            }*/
            logCurrentStatus(deployResult)

            isDone = deployResult.isDone()
            if (!deployResult.isDone()) {
                //printErrors(deployResult, "Failures for deployment in progress:\n")
            }
        }

        if (deployResult != null && !deployResult.isSuccess() && deployResult.getErrorStatusCode() != null) {
            throw new Exception(deployResult.getErrorStatusCode().name() + " msg: " +
                    deployResult.getErrorMessage())
        }

        return deployResult
    }


    void logCurrentStatus(DeployResult deployResult) {
        if (deployResult != null) {
            String statusTextToPrint = "Request Status: " + deployResult.getStatus();
            String stateDetail = deployResult.getStateDetail();
            if (stateDetail != null) {
                if (stateDetail.startsWith("Processing")) {
                    statusTextToPrint = statusTextToPrint + " (" + (deployResult.getNumberComponentsDeployed() + deployResult.getNumberComponentErrors()) + "/" + deployResult.getNumberComponentsTotal() + ")";
                } else if (stateDetail.startsWith("Running")) {
                    statusTextToPrint = statusTextToPrint + " (" + (deployResult.getNumberTestErrors() + deployResult.getNumberTestsCompleted()) + "/" + deployResult.getNumberTestsTotal() + ")";
                }

                statusTextToPrint = statusTextToPrint + "  -- " + stateDetail;
            }

            StringBuilder currentStatusLog = new StringBuilder();
            if (deployResult.getStatus() == DeployStatus.InProgress) {
                boolean hasDeployFailuresToPrint = false;
                boolean hasTestFailuresToPrint = false;
                DeployDetails inProgressDeployDetails = deployResult.getDetails();
                if (inProgressDeployDetails != null) {
                    StringBuilder deployFailuresLog = new StringBuilder();
                    DeployMessage[] componentFailures = inProgressDeployDetails.getComponentFailures();
                    if (componentFailures != null && componentFailures.length > 0) {
                        DeployMessage[] var11 = componentFailures;
                        int var12 = componentFailures.length;

                        for (int var13 = 0; var13 < var12; ++var13) {
                            DeployMessage componentFailure = var11[var13];
                            DeployMessageKey messageKey = new DeployMessageKey(componentFailure);
                            if (!this.printedInProgressDeployFailures.contains(messageKey)) {
                                hasDeployFailuresToPrint = true;
                                String numberedPrefix = this.formatIndexToPrint(this.inProgressDeployFailuresIndex);
                                ++this.inProgressDeployFailuresIndex;
                                this.appendDeployProblemLog(componentFailure, deployFailuresLog, numberedPrefix);
                                this.printedInProgressDeployFailures.add(messageKey);
                            }
                        }
                    }

                    if (hasDeployFailuresToPrint) {
                        deployFailuresLog.insert(0, "Component Failures:\n");
                        currentStatusLog.append(deployFailuresLog.toString());
                    }

                    RunTestsResult runTestsResult = inProgressDeployDetails.getRunTestResult();
                    if (runTestsResult != null) {
                        RunTestFailure[] testFailures = runTestsResult.getFailures();
                        StringBuilder testFailuresLog = new StringBuilder();
                        if (testFailures != null && testFailures.length > 0) {
                            RunTestFailure[] var23 = testFailures;
                            int var24 = testFailures.length;

                            for (int var25 = 0; var25 < var24; ++var25) {
                                RunTestFailure testFailure = var23[var25];
                                RunTestsResultKey testResultKey = new RunTestsResultKey(testFailure);
                                if (!this.printedInProgressTestFailures.contains(testResultKey)) {
                                    hasTestFailuresToPrint = true;
                                    String numberedPrefix = this.formatIndexToPrint(this.inProgressTestFailuresIndex);
                                    ++this.inProgressTestFailuresIndex;
                                    this.appendFailureLog(testFailure, testFailuresLog, numberedPrefix);
                                    this.printedInProgressTestFailures.add(testResultKey);
                                }
                            }
                        }

                        if (hasTestFailuresToPrint) {
                            testFailuresLog.insert(0, "Test Failures:\n");
                            currentStatusLog.append(testFailuresLog.toString());
                        }
                    }
                }

                if (hasDeployFailuresToPrint || hasTestFailuresToPrint) {
                    currentStatusLog.insert(0, "\n-----------------------------------------------------------------------------------\n");
                    currentStatusLog.append("-----------------------------------------------------------------------------------");
                }
            }

            currentStatusLog.insert(0, statusTextToPrint);
            log.info currentStatusLog.toString()
        }

    }

    private String formatIndexToPrint(int index) {
        return index + "." + "  ";
    }

    private void appendFailureLog(RunTestFailure failure, StringBuilder failuresLog, String prefix) {
        failuresLog.append(prefix);
        String name = (failure.getNamespace() == null ? "" : failure.getNamespace() + ".") + failure.getName();
        String spacesToIndent = this.constructSpacesToIndent(prefix.length());
        String failureMessage = failure.getMessage();
        failureMessage = this.appendToNewLines(failureMessage, spacesToIndent);
        String stacktrace = failure.getStackTrace();
        stacktrace = this.appendToNewLines(stacktrace, spacesToIndent);
        failuresLog.append(name + "." + failure.getMethodName() + " -- " + failureMessage + "\n" + spacesToIndent + "Stack trace: " + stacktrace + "\n");
    }

    private void appendDeployProblemLog(DeployMessage deployMessage, StringBuilder log, String prefix) {
        log.append(prefix);
        String fileName = deployMessage.getFileName();
        String fullName = deployMessage.getFullName();
        log.append(fileName);
        if (fileName != null && fullName != null && !fileName.contains(fullName)) {
            log.append(" (" + fullName + ")");
        }

        log.append(" -- " + deployMessage.getProblemType() + ": ");
        String problemMessage = deployMessage.getProblem();
        problemMessage = this.appendToNewLines(problemMessage, this.constructSpacesToIndent(prefix.length()));
        log.append(problemMessage);
        String loc = deployMessage.getLineNumber() == 0 ? "" : " (line " + deployMessage.getLineNumber() + ", column " + deployMessage.getColumnNumber() + ")";
        log.append(loc);
        log.append("\n");
    }

    private String appendToNewLines(String sourceStr, String appendStr) {
        return sourceStr == null ? null : sourceStr.replaceAll("\\n", "\n" + appendStr);
    }

    private String constructSpacesToIndent(int numberOfSpaces) {
        StringBuilder spaceForIndent = new StringBuilder();

        for (int i = 0; i < numberOfSpaces; ++i) {
            spaceForIndent.append(" ")
        }

        return spaceForIndent.toString()
    }

    private static class RunTestsResultKey {
        private final String namespace
        private final String className
        private final String methodName
        private final int _hashCode

        RunTestsResultKey(RunTestFailure failure) {
            this.namespace = failure.getNamespace();
            this.className = failure.getName();
            this.methodName = failure.getMethodName();
            this._hashCode = (this.namespace == null ? 0 : this.namespace.hashCode()) + (this.className == null ? 0 : this.className.hashCode()) + (this.methodName == null ? 0 : this.methodName.hashCode());
        }

        public final boolean equals(Object obj) {
            if (!(obj instanceof RunTestsResultKey)) {
                return false;
            } else {
                RunTestsResultKey that = (RunTestsResultKey) obj;
                return stringEquals(this.namespace, that.namespace) && stringEquals(this.className, that.className) && stringEquals(this.methodName, that.methodName);
            }
        }

        public final int hashCode() {
            return this._hashCode;
        }
    }

    private static class DeployMessageKey {
        private final String fileName;
        private final String fullName;
        private final int _hashCode;

        DeployMessageKey(DeployMessage message) {
            this.fileName = message.getFileName();
            this.fullName = message.getFullName();
            this._hashCode = (this.fileName == null ? 0 : this.fileName.hashCode()) + (this.fullName == null ? 0 : this.fullName.hashCode());
        }

        public final boolean equals(Object obj) {
            if (!(obj instanceof DeployMessageKey)) {
                return false;
            } else {
                DeployMessageKey that = (DeployMessageKey) obj;
                return stringEquals(this.fileName, that.fileName) && stringEquals(this.fullName, that.fullName);
            }
        }

        public final int hashCode() {
            return this._hashCode;
        }
    }

    private static boolean stringEquals(String str1, String str2) {
        return str1 == str2 ? true : str1 != null && str1.equals(str2);
    }

    public void handleResponse(MetadataConnection metadataConnection, DeployResult response) throws ConnectionException {
        DebuggingHeader_element debugHeader = new DebuggingHeader_element();
        //debugHeader.setDebugLevel(this.readLogType());
        metadataConnection.__setDebuggingHeader(debugHeader);
        DeployResult result = metadataConnection.checkDeployStatus(response.getId(), true);
        String debug = metadataConnection.getDebuggingInfo() != null ? metadataConnection.getDebuggingInfo().getDebugLog() : "";
        if (debug != null && debug.length() > 0) {
            log.debug("Debugging Information:\n" + debug);
        }

        DeployDetails details = result.getDetails();
        if (details == null) {
            details = new DeployDetails();
            details.setComponentSuccesses(new DeployMessage[0]);
            details.setComponentFailures(new DeployMessage[0]);
        }

        if (!result.isSuccess()) {
            this.logFailedDeploy(result, details);
        } else if (result.getStatus() == DeployStatus.SucceededPartial) {
            this.logPartiallySucceededDeploy(details);
        } else {
            this.logSucceededDeploy(details);
        }

    }

    private void logFailedDeploy(DeployResult result, DeployDetails details) {
        DeployMessage[] errorMessages = details.getComponentFailures();
        StringBuilder failuresLog = new StringBuilder("\n");
        failuresLog.append("*********** DEPLOYMENT FAILED ***********\n");
        failuresLog.append("Request ID: " + result.getId() + "\n");
        if (result.getErrorStatusCode() != null) {
            failuresLog.append("Failure code - " + result.getErrorStatusCode() + ", error message - " + result.getErrorMessage());
            log.error failuresLog.toString()
        } else {
            this.logComponentFailures(errorMessages, failuresLog);
            this.logTestFailuresAndCodeCoverage(details, failuresLog);
            if (result.getStatus() == DeployStatus.Canceled) {
                failuresLog.append("\nRequest canceled!\n");
            }

            failuresLog.append("*********** DEPLOYMENT FAILED ***********");
            log.error failuresLog.toString()
        }
        System.exit(1)
    }

    private void logPartiallySucceededDeploy(DeployDetails details) {
        DeployMessage[] errorMessages = details.getComponentFailures();
        StringBuilder warningLog = new StringBuilder();
        this.logComponentFailures(errorMessages, warningLog);
        this.logTestFailuresAndCodeCoverage(details, warningLog);
        log.info("*********** DEPLOYMENT PARTIALLY SUCCEEDED ***********\n" + warningLog.toString() + "*********** DEPLOYMENT PARTIALLY SUCCEEDED ***********\n");
    }

    private void logSucceededDeploy(DeployDetails details) {
        StringBuilder warningLog = new StringBuilder();
        int warningIndex = 1;
        DeployMessage[] var4 = details.getComponentSuccesses();
        int var5 = var4.length;

        int var6;
        DeployMessage message;
        for (var6 = 0; var6 < var5; ++var6) {
            message = var4[var6];
            if (message.getProblemType() == DeployProblemType.Warning) {
                this.appendDeployProblemLog(message, warningLog, this.formatIndexToPrint(warningIndex));
                ++warningIndex;
            }
        }

        var4 = details.getComponentFailures();
        var5 = var4.length;

        for (var6 = 0; var6 < var5; ++var6) {
            message = var4[var6];
            if (message.getProblemType() == DeployProblemType.Warning) {
                this.appendDeployProblemLog(message, warningLog, this.formatIndexToPrint(warningIndex));
                ++warningIndex;
            }
        }

        if (warningIndex > 1) {
            warningLog.insert(0, "All warnings:\n");
        }

        this.logTestFailuresAndCodeCoverage(details, warningLog);
        if (warningLog.length() > 0) {
            warningLog.append("\n*********** DEPLOYMENT SUCCEEDED ***********");
        }

        log.info("*********** DEPLOYMENT SUCCEEDED ***********" + warningLog.toString());
    }

    private void logComponentFailures(DeployMessage[] errorMessages, StringBuilder failuresLog) {
        StringBuilder deployFailuresLog = new StringBuilder("\nAll Component Failures:\n");
        int deployMessageIndex = 1;
        DeployMessage[] var5 = errorMessages;
        int var6 = errorMessages.length;

        for (int var7 = 0; var7 < var6; ++var7) {
            DeployMessage errorMessage = var5[var7];
            String prefix = this.formatIndexToPrint(deployMessageIndex);
            this.appendDeployProblemLog(errorMessage, deployFailuresLog, prefix);
            ++deployMessageIndex;
        }

        if (errorMessages.length > 0) {
            failuresLog.append(deployFailuresLog.toString() + "\n");
        }

    }

    private void logTestFailuresAndCodeCoverage(DeployDetails details, StringBuilder failuresLog) {
        RunTestsResult rtr = details.getRunTestResult();
        RunTestFailure[] testFailures = rtr.getFailures();
        StringBuilder codeCoverageWarningsLog;
        int warningIndex;
        int var8;
        int var9;
        String n;
        if (testFailures != null) {
            codeCoverageWarningsLog = new StringBuilder("\nAll Test Failures:\n");
            warningIndex = 1;
            RunTestFailure[] var7 = testFailures;
            var8 = testFailures.length;

            for (var9 = 0; var9 < var8; ++var9) {
                RunTestFailure failure = var7[var9];
                n = this.formatIndexToPrint(warningIndex);
                this.appendFailureLog(failure, codeCoverageWarningsLog, n);
                ++warningIndex;
            }

            if (testFailures.length > 0) {
                failuresLog.append(codeCoverageWarningsLog.toString());
            }
        }

        if (rtr.getCodeCoverageWarnings() != null) {
            codeCoverageWarningsLog = new StringBuilder("\nCode Coverage Failures:\n");
            warningIndex = 1;
            CodeCoverageWarning[] var12 = rtr.getCodeCoverageWarnings();
            var8 = var12.length;

            for (var9 = 0; var9 < var8; ++var9) {
                CodeCoverageWarning ccw = var12[var9]
                codeCoverageWarningsLog.append(this.formatIndexToPrint(warningIndex));
                if (ccw.getName() != null) {
                    n = (ccw.getNamespace() == null ? "" : ccw.getNamespace() + ".") + ccw.getName();
                    codeCoverageWarningsLog.append("Class: " + n + " -- ");
                }

                codeCoverageWarningsLog.append(ccw.getMessage() + "\n");
                ++warningIndex;
            }

            if (warningIndex > 1) {
                failuresLog.append(codeCoverageWarningsLog.toString());
            }
        }

    }
}
