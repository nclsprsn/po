import ch.qos.logback.classic.encoder.PatternLayoutEncoder

displayStatusOnConsole()
scan('5 minutes')
setupAppenders()
setupLoggers()

def displayStatusOnConsole() {
    statusListener OnConsoleStatusListener
}

def setupAppenders() {
    def logfileDate = timestamp('yyyy-MM-dd')
    def filePatternFormat = "%d{HH:mm:ss.SSS} %-5level [${hostname}] %logger - %msg%n"
    appender('logfile', FileAppender) {
        file = "log/po.${logfileDate}.log"
        encoder(PatternLayoutEncoder) {
            pattern = filePatternFormat
        }
    }

    appender('systemOut', ConsoleAppender) {
        encoder(PatternLayoutEncoder) {
            pattern = filePatternFormat
        }
    }
}

def setupLoggers() {
    root TRACE, ['systemOut', 'logfile']
}
