// Adapted from https://stackoverflow.com/a/1169196
final SCRIPT_FILE = new File((getClass() as Class).protectionDomain.codeSource.location.path)

final REPOSITORY_PATH = SCRIPT_FILE.toPath().parent.resolve 'repository'


/*  Creation of settings.xml content adapted from
    https://subscription.packtpub.com/book/cloud-and-networking/9781785286124/1/ch01lvl1sec16/changing-the-location-of-the-maven-repository
    */

println """
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <localRepository>${REPOSITORY_PATH}</localRepository>
</settings>

""".trim()
