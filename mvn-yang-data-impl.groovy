import java.nio.file.Files
import java.nio.file.Paths


final PROJECT_HOME = Paths.get("opendaylight-yangtools", "yang", "yang-data-impl")

final JAVA_FILES = Files.walk(PROJECT_HOME).filter { path -> path.fileName.toString().endsWith ".java" }.toList()

for (final file in JAVA_FILES) {
    file.text = file.text.replace "\r\n", "\n"
}

new ProcessBuilder("mvn", *args)
        .directory(PROJECT_HOME.toFile())
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor()

for (final file in JAVA_FILES) {
    file.text = file.text.replace "\n", "\r\n"
}
