package org.opendaylight.mdsal.binding.java.api.generator;

import java.util.Map;

import lombok.NonNull;

import org.kohsuke.MetaInfServices;

import org.opendaylight.yangtools.plugin.generator.api.FileGenerator;
// import org.opendaylight.yangtools.plugin.generator.api.FileGeneratorException;
import org.opendaylight.yangtools.plugin.generator.api.FileGeneratorFactory;

import org.opendaylight.yangtools.yang2sources.plugin.NetEmuYangToSourcesProcessor;


/** Allows NetEMU-specific {@code yang2sources} code generation in addition to {@link JavaFileGeneratorFactory}.

    <p> Auto-loaded by {@link org.opendaylight.yangtools.yang2sources.plugin.YangToSourcesProcessor}, which
    {@link NetEmuYangToSourcesProcessor} is derived from -- the code generation processor used by
    {@link com.adva.netemu.gradle.NetEmuPlugin}.
*/
@MetaInfServices(FileGeneratorFactory.class)
public class NetEmuFileGeneratorFactory implements FileGeneratorFactory {

    /** Returns as factory identifier the qualified class name of generator instances produced by this factory.

        @return the qualified class name of {@link NetEmuFileGenerator}
     */
    @NonNull @Override
    public String getIdentifier() {
        return NetEmuFileGenerator.class.getName();
    }

    /** Creates a new instance of {@link NetEmuFileGenerator} with given configuration properties.

        @param configuration the generator's configuration properties

        @return the new generator instance
    */
    @NonNull @Override
    public FileGenerator newFileGenerator(@NonNull final Map<String, String> configuration) { // throws FileGeneratorException {
        return new NetEmuFileGenerator();
    }
}
