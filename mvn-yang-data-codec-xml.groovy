import java.nio.file.Files
import java.nio.file.Paths

def YANG_BINDING_HOME = Paths.get(
    "opendaylight-yangtools", "yang", "yang-data-codec-xml")

def JAVA_FILES = Files.walk(YANG_BINDING_HOME).filter { path ->
    path.fileName.toString().endsWith ".java"
}.toList()

for (file in JAVA_FILES) {
    file.text = file.text.replace "\r\n", "\n"
}

new ProcessBuilder("mvn", *args)
    .directory(YANG_BINDING_HOME.toFile())
    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
    .redirectError(ProcessBuilder.Redirect.INHERIT)
    .start()
    .waitFor()

for (file in JAVA_FILES) {
    file.text = file.text.replace "\n", "\r\n"
}
