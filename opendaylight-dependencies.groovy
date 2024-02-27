/** Determines all dependencies of customized OpenDaylight modules used by NETEMU.

    <p> This should be used to update ./opendaylight-dependencies.txt -- whose contents are dynamically added as dependencies in
    ./build.gradle -- so the customized OpenDaylight modules themselves can be bundled in netemu-*.jar library releases.
*/

final DEPENDENCIES = new HashSet<String>()


def startConsidering = false
def considerLine = false

for (final line in ['gradle', '--quiet', 'dependencies'].execute().inputStream.readLines()) {

    /*
    if (!startConsidering) {
        if (line =~ /^compileClasspath/) {
            startConsidering = true
        }

        continue
    }
    */

    if (line =~ /^\+.+-ADVA$/) {
        considerLine = true // ==> Following lines are applicable
        continue
    }

    if (line =~ /^\+/) {
        considerLine = false // ==> Following lines are inapplicable
        continue
    }

    if (line =~ /^\s*$/) { // ==> Finished compileClasspath
        considerLine = false // ==> Following lines are inapplicable
        continue
    }

    // Collect non-customized dependencies from applicable lines ...
    if (considerLine && !(line =~ /-ADVA\W*$/)) {
        // DEPENDENCIES.add line.find(/\w\S+/)

        final matcher = line =~ /(?<name>\w\S+):(\S+\s+->\s+)?(?<version>\S+)/
        if (matcher.find()) {
            DEPENDENCIES.add "${matcher.group 'name'}:${matcher.group 'version'}"
        }
    }
}

DEPENDENCIES.sort() forEach { println it }
