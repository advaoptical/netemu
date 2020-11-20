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

Main Classes

* NetEmu
    - main class
* Owned
* YangPool
* YangProvider

**Installation and usage**

- [ ] install submodules:
        `.\netemu> git submodule update --init --recursive`
        
- [ ] run groovy install scripts
        `groovy mvn-yang-data-util.groovy install`
        `groovy mvn-yang-data-codec-xml.groovy install`
        `groovy mvn-netconf-netty-util.groovy install`

 



Features
- Independent of yang revisions
- 

Limitations / Bugs / Workaround
- AOS bugs
- OpenConfig bugs
- 
- OpenDaylight bugs / fixes / workaround

-

