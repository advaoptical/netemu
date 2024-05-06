package org.opendaylight.yangtools.yang2sources.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import org.opendaylight.mdsal.binding.java.api.generator.NetEmuFileGenerator;


public class NetEmuYangToSourcesProcessor extends YangToSourcesProcessor {

    @Nonnull
    public static final Path SERVICE_META_PATH = Paths.get("META-INF", "services");

    @Nonnull
    public static final Path YANG_META_PATH = Paths.get("META-INF", "yang");

    @Nonnull
    public static final String FILE_GENERATOR_IDENTIFIER = NetEmuFileGenerator.class.getName(); // NetEmuCodeGenerator.class.getName();

    public NetEmuYangToSourcesProcessor(
            @Nonnull final Path resourcesPath, @Nonnull final Path mdSalOutputPath, @Nonnull final String yangPackageName,
            @Nonnull final MavenProject project) {

        super(new DefaultBuildContext(), /// seems redundant, but super() w/implicit buildContext does not forward fileGenerators
                resourcesPath.resolve(YANG_META_PATH).resolve(yangPackageName).toFile(), List.of(), /// no exclusions
                List.of(new FileGeneratorArg(FILE_GENERATOR_IDENTIFIER)),
                project, true);

        /// Tell next factory-service-created generator instance the Java package to process YANG modules for:
        NetEmuFileGenerator.YANG_PACKAGE_NAME.set(yangPackageName);

        /// And reset the generator-parsed YANG modules:
        NetEmuFileGenerator.YANG_MODULES.set(Set.of());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
    }
}
