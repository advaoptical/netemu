# NETEMU

## Library to generate Java and Python source code with utility functions from YANG models to build NETCONF APIs in an object-oriented manner

The library can be use to validate yang models and yang datastores (e.g. for AOS F8 or OpenConfig), build mediators for new APIs (e.g. OpenROADM Agent), or SDN applications (e.g. FlexProbe)

Used projects and libraries
- OpenDaylight
    - org.opendaylight.netconf.*
    - org.opendaylight.mdsal.*
    - org.opendaylight.yangtools.*
-  Gradle
    - annotation processor
    - YangToSources
    - Pythonizer
- Groovy

**Pre-requisites, installation and usage**
- **Java 13, Maven 3.6.x, Groovy 3.0 **
    - netemu (specifically spotbugs) requires Java 13
    - install Openjdk13 (via chocolate / package manager)
    - JAVA_HOME should be set to java13 and path should include %JAVA_HOME%\bin
    - Settings>Build, Execution, Deployment>Build Tools>Gradle --> set Gradle JVM to java 13 

    ```
    C:\Users\developer>java --version
        openjdk 13.0.2 2020-01-14
        OpenJDK Runtime Environment (build 13.0.2+8)
        OpenJDK 64-Bit Server VM (build 13.0.2+8, mixed mode, sharing)

    C:\Users\developer>mvn -version
        Apache Maven 3.6.2 (40f52333136460af0dc0d7232c0dc0bcf0d9e117; 2019-08-27T17:06:16+02:00)
        Maven home: C:\DEV\MAVEN\apache-maven-3.6.2\bin\..
        Java version: 13.0.2, vendor: Oracle Corporation, runtime: C:\Program Files\OpenJDK\jdk-13.0.2
        Default locale: de_DE, platform encoding: Cp1252
        OS name: "windows 10", version: "10.0", arch: "amd64", family: "windows"

    C:\Users\developer>groovy --version
        Groovy Version: 3.0.6 JVM: 13.0.2 Vendor: Oracle Corporation OS: Windows 10

    PS C:\Users\achima> Get-Command mvn
        CommandType     Name                                               Version    Source
        -----------     ----                                               -------    ------
        Application     mvn.cmd                                            0.0.0.0    C:\DEV\MAVEN\apache-maven-3.6.2\bin\mv...

    --> replace mvn with mvn.cmd in all three groovy scripts:
        `groovy mvn-yang-data-util.groovy install`
        `groovy mvn-yang-data-codec-xml.groovy install`
        `groovy mvn-netconf-netty-util.groovy install`   
    ```

- 
- jepDir: if no python is installed, deactivate jebDir from build.Gradle   
    - ~ line 23: comment /* ext.jepDir = [ ... ].execute().text.trim() */ 
    - ~ line 50: comment: /* flatDir {  dirs jepDir } */



- **install jsr173-ri-1.0.jar to local maven repository**
    - follow this guide: https://stackoverflow.com/a/14101884 
        - download file=jsr173-ri-1.0.jar file to a local folder:
            ` http://svn.apache.org/repos/asf/servicemix/m2-repo/com/bea/xml/jsr173-ri/1.0/`
        - create a file jsr173.pom in the same directory:
            `<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <groupId>com.bea.xml</groupId>
                <artifactId>jsr173-ri</artifactId>
                <version>1.0</version>
                <modelVersion>4.0.0</modelVersion> 
            </project> `
        - and install it to your local repo like this:
            `mvn install:install-file -Dfile=jsr173-ri-1.0.jar -DpomFile=jsr173.pom`

        - ATTENTION: use '' in Windows Powershell: 
            `mvn install:install-file -Dfile='jsr173-ri-1.0.jar' -DpomFile='jsr173.pom'`
- [ ] install submodules:
        `.\netemu> git submodule update --init --recursive`
        
- [ ] run groovy install scripts
        `groovy mvn-yang-data-util.groovy install`
        `groovy mvn-yang-data-codec-xml.groovy install`
        `groovy mvn-netconf-netty-util.groovy install`

 - [ ] Settings>Build, Execution, Deployment>Build Tools>Gradle noch die Gradle JVM auf 13 stellen
 - [ ]  in Intellij den Gradle-Task "install" ausfuehren


- --> emuports
    - create enc-sdn-driver-connection.xml in emuports-root folder 
        `
            <?xml version="1.0" encoding="UTF-8"?>
            <config xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
                <connection xmlns="http://yang.adva.com/enc-sdn-driver">
                    <name>main</name>

                    <protocol>http</protocol>
                    <!-- <host>192.168.127.110</host> -->
                    <host>10.20.6.49</host>
                    <port>8080</port>
                    <root-path>/advabase/sdn</root-path>

                </connection>
            </config> 
        `
    - generate Python-Codegen-Datei 


Main Classes

* NetEmu
    - main class
* Owned
* YangPool
* YangProvider



Features
- Independent of yang revisions
- 

Limitations / Bugs / Workaround
- AOS bugs
- OpenConfig bugs
- 
- OpenDaylight bugs / fixes / workaround

-

