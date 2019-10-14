# Po

Automate Salesforce deployement.

## Requirements

 * Gradle 3.4.1
 * Groovy 2.4.7
 * Java 8

## Build

### Build Po package

```bash
gradle buildPackage --no-daemon
```

The ZIP file can be found at `build/ditributions/`.

### Build JAR

```bash
gradle buildJar --no-daemon
cp -r config build/libs
```

The JAR file can be found at `build/libs/`.

## Usage

### Commands

```
usage: Po [options] <arguments>
Options:
 -a,--url <arg>           Salesforce url
 -c,--compare             Compare the source against Org
 -d,--deploy              Deploy source to Org
 -e,--environment <arg>   Defined in configuration file
 -f,--root <arg>          Package root directory
 -g,--force               Deactivate simulation lock
 -h,--help                Usage information
 -l,--log-level <arg>     Log level: TRACE, DEBUG, INFO (default), WARN,
                          ERROR
 -m,--git <arg>           Diff between two git references
 -o,--org-manifest        Generate manifest from Org
 -p,--password <arg>      Salesforce password
 -r,--retrieve            Retrieve source from Org
 -s,--source-manifest     Generate manifest from source
 -t,--test-level <arg>    Test level: NoTestRun, RunSpecifiedTests,
                          RunLocalTests, RunAllTestsInOrg
 -u,--username <arg>      Salesforce username
 -x,--destructive         Generate destructive manifest
-------------------------------------------------------------------------
```

## Distribution folder structure

The distrubution directory is structured as follows:

    .
    ├── archives                # Deployed packages
    ├── log                     # Log files
    ├── config                  # Configuration files
    └── po-<VERSION>.jar

## Examples

### Retrieve

#### Generate Manifest from Org

```
java -jar po-1.0-SNAPSHOT.jar -e env1 -f /tmp/pkg
```

#### Retrieve with Manifest from Org

```
java -jar po-1.0-SNAPSHOT.jar -r -e env1 -f /tmp/pkg
```

#### Generate Manifest and Retrieve from Org

```
java -jar po-1.0-SNAPSHOT.jar -or -e env1 -f /tmp/pkg
```

### Deploy

#### Generate Manifest from Source

```
java -jar po-1.0-SNAPSHOT.jar -s -e env1 -f /tmp/pkg
```

#### Simulate Deploy with Manifest

```
java -jar po-1.0-SNAPSHOT.jar -d -e env1 -f /tmp/pkg
```

#### Deploy with Manifest (no simulate)

```
java -jar po-1.0-SNAPSHOT.jar -d -e env1 -f /tmp/pkg -g
```

#### Generate Manifest and Deploy (no simulate)

```
java -jar po-1.0-SNAPSHOT.jar -sd -e env1 -f /tmp/pkg -g
```

#### Generate Manifest, Diff between 2 tags and Deploy (no simulate)

```
java -jar po-1.0-SNAPSHOT.jar -sd -m 1.12.0.0-RELEASE,1.13.0.0-RELEASE -e env1 -f /tmp/pkg -g
```

#### Generate Manifest, Allow destructive change and Deploy (no simulate)

```
java -jar po-1.0-SNAPSHOT.jar -sdx -e env1 -f /tmp/pkg -g
```

### Bonus

#### Compare the local package and the org package

```
java -jar po-1.0-SNAPSHOT.jar -c -e env1 -f /tmp/pkg
```