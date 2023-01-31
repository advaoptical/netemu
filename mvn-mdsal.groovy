import java.lang.management.ManagementFactory

import java.nio.file.Files
import java.nio.file.Paths


final IS_WINDOWS = ManagementFactory.operatingSystemMXBean.name.startsWith "Windows"

final PROJECT_HOME = Paths.get 'opendaylight-mdsal' toAbsolutePath()

final JAVA_FILES = Files.walk PROJECT_HOME filter { path -> path.fileName.toString().endsWith '.java' } toList()

for (final file in JAVA_FILES) {
    file.text = file.text.replace "\r\n", "\n"
}

final status = new ProcessBuilder((IS_WINDOWS ? ['cmd', '/c'] : []) + ['mvn', *args]).directory(PROJECT_HOME.toFile())
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor()

/*
for (final file in JAVA_FILES) {
    file.text = file.text.replace "\n", "\r\n"
}
*/

System.exit status
