/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <nicolas.a.pierson@capgemini.com> wrote this file. As long as you retain
 * this notice you can do whatever you want with this stuff. If we meet some
 * day, and you think this stuff is worth it, you can buy me a beer in return.
 * Nicolas Pierson
 *
 * Contributors:
 *      - Jean-Lucas Tran <jean-lucas.tran@capgemini.com>
 * ----------------------------------------------------------------------------
 */

import archive.Archive
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.sforce.soap.metadata.TestLevel
import config.ConfigModel
import file.FileUtils
import force.ForceService
import force.ForceServiceFactory
import force.model.EnvironmentModel
import groovy.util.logging.Slf4j
import manifest.DeployManifestService
import manifest.DiffEnum
import manifest.DiffManifestService
import manifest.Manifest
import manifest.RetrieveManifestService
import org.slf4j.LoggerFactory
import pkg.DeployPkgService
import pkg.RetrievePkgService
import scm.GitServiceImpl
import scm.utils.Banner
import scm.utils.ScmUtils
import utils.Utils
import zip.ZipUtils

/**
 * Po
 */
@Slf4j
class Po {

    /**
     * Main.
     * @param args arguments
     */
    static void main(String... args) {
        def cli = new CliBuilder(usage: 'Po [options] <arguments>',
                header: 'Options:',
                footer: Utils.horizontalBar(Utils.MAX_LENGTH_HORIZONTAL_BAR))

        cli.with {
            // Credentials
            e longOpt: 'environment', 'Defined in configuration file', args: 1
            u longOpt: 'username', 'Salesforce username', args: 1
            p longOpt: 'password', 'Salesforce password', args: 1
            a longOpt: 'url', 'Salesforce url', args: 1

            // Config credentials
            f longOpt: 'root', 'Package root directory', args: 1, required: true

            // I/O
            r longOpt: 'retrieve', 'Retrieve source from Org'
            d longOpt: 'deploy', 'Deploy source to Org'
            g longOpt: 'force', 'Deactivate simulation lock'

            // Manifest
            o longOpt: 'org-manifest', 'Generate manifest from Org'
            s longOpt: 'source-manifest', 'Generate manifest from source'
            x longOpt: 'destructive', 'Generate destructive manifest'
            c longOpt: 'compare', 'Compare the source against Org'

            // Source control management
            m longOpt: 'git', 'Diff between two git references', args: 2, valueSeparator: ','

            // Test
            t longOpt: 'test-level', 'Test level: NoTestRun, RunSpecifiedTests, RunLocalTests, RunAllTestsInOrg', args: 1

            // Log
            l longOpt: 'log-level', 'Log level: TRACE, DEBUG, INFO (default), WARN, ERROR', args: 1

            // Help
            h longOpt: 'help', 'Usage information'
        }

        def options
        def isCli = false

        if ('-h' in args || '--help' in args) {
            cli.usage()
            return
        } else {
            options = cli.parse(args)
            if (options == null) {
                return
            }
        }

        if (!(options.e || (options.u && options.p && options.a))) {
            log.error 'Environment must be defined through configuration file or cli parameters.'
            cli.usage()
            return
        }

        // Set log level
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        root.setLevel(options.l instanceof Boolean ? Level.INFO : Level.toLevel((String) options.l, Level.INFO))

        // Load config
        ConfigObject config
        File envFile = new File('config/environment.groovy')
        if (envFile.exists()) {
            config = new ConfigSlurper().parse(envFile.toURI().toURL())
        }

        EnvironmentModel environment

        // Environment configuration
        if (options.u instanceof String && options.p instanceof String && options.a instanceof String) {
            environment = new EnvironmentModel((String) options.a, (String) options.u, (String) options.p, false)
            isCli = true
        } else if (config != null) {
            if (options.e instanceof String && config.getProperty('environment') != null && config.getProperty('environment').containsKey(options.e)) {
                ConfigObject env = config.getProperty('environment').getProperty(options.e)
                String url = env.getProperty('url')
                String username = env.getProperty('username')
                String password = env.getProperty('password')
                boolean isConfirmationRequired = (env.getProperty('isConfirmationRequired') == null || env.getProperty('isConfirmationRequired')) ? true : false
                environment = new EnvironmentModel(url, username, password, isConfirmationRequired)
            } else {
                log.error "Unknown environment ${options.e}. Check your configuration file ${envFile.getAbsolutePath()}."
            }
        } else {
            log.error 'Environment must be defined through configuration file or cli parameters.'
        }

        if (environment == null) {
            System.exit 1
        }

        File packageDir = new File((String) options.f)

        ConfigModel conf = new ConfigModel(packageDir, options.t instanceof Boolean ? TestLevel.RunAllTestsInOrg : TestLevel.valueOf((String) options.t), options.t ? false : true)
        conf.setCheckOnly(!(boolean) options.g)

        log.debug conf.toString()

        Banner.generate(options, conf, environment)

        ForceService forceService = ForceServiceFactory.create(environment)

        if (options.r) {
            if (packageDir.exists()) {
                FileUtils.delete(packageDir, [new File(packageDir, 'package.xml').getAbsolutePath()])
            }
        }

        if (!packageDir.isDirectory()) {
            packageDir.mkdirs()
        }

        if (options.o) {
            RetrieveManifestService retrieveManifestService = new RetrieveManifestService(forceService, conf)
            retrieveManifestService.writeBuildXml()
        }

        if (options.r) {
            RetrievePkgService retrievePkgService = new RetrievePkgService(forceService, conf)
            byte[] zip = retrievePkgService.retrieveZip()
            ZipUtils.unZipToFolder(new ByteArrayInputStream(zip), conf.getPackageDir())
        }

        if (options.m) {
            GitServiceImpl git = new GitServiceImpl(conf)
            List<String> keepFiles = ScmUtils.absolutePath(ScmUtils.addMetaFile(git.modifiedFile((String) options.ms.get(0), (String) options.ms.get(1))), conf)
            FileUtils.delete(conf.getPackageDir(), keepFiles)
        }

        if (options.s) {
            DeployManifestService deployManifestService = new DeployManifestService(forceService, conf)
            deployManifestService.writeBuildXml()
        }

        if (options.x) {
            DiffManifestService diffManifestService = new DiffManifestService(forceService, conf)
            diffManifestService.writeBuildXml()
        }

        if (options.c) {
            DiffManifestService diffManifestService = new DiffManifestService(forceService, conf)
            log.info 'Diff Org'
            Manifest.print(diffManifestService.buildXml(DiffEnum.ORG))
            log.info 'Diff Src'
            Manifest.print(diffManifestService.buildXml(DiffEnum.SRC))
        }

        if (options.d) {
            boolean isContinue = true

            if (Manifest.isDestructiveManifestDetected(conf) && !isCli) {
                if (!Utils.isConfirmed('Destructive detected. Do you want to continue ?')) {
                    isContinue = false
                }
            }

            if (isContinue && environment.getIsConfirmationRequired()) {
                if (!Utils.isConfirmed('Deployment processing. Do you want to continue ?')) {
                    isContinue = false
                }
            }

            if (isContinue) {
                log.info 'Start deployment'
                byte[] zip = ZipUtils.zipFromFolder(conf.getPackageDir())

                // Archive
                Archive archive = new Archive(new File('archives'), environment)
                String archivePath = archive.create(zip)

                DeployPkgService deployPkgService = new DeployPkgService(forceService, conf)
                deployPkgService.deployZip(zip)

                log.info "Deployed -- $archivePath"
            } else {
                log.info 'Deployment canceled'
            }

        }

    }

}
