package com.adva.netemu.service;

import javax.annotation.Nonnull;

import jep.Jep;
import jep.JepException;
import jep.MainInterpreter;
import jep.SharedInterpreter;

import com.adva.netemu.YangPool;

import java.io.File;
import java.io.IOException;


public abstract class EmuPythonService extends EmuService {

    protected static class Python {

        @Nonnull
        private final Jep interpreter;

        @Nonnull
        public Jep interpreter() {
            return this.interpreter;
        }

        private Python() throws JepException {
            this.interpreter = new SharedInterpreter();
        }

        @Nonnull
        public static Python withArguments(@Nonnull final String... argv) throws JepException {
            MainInterpreter.setSharedModulesArgv(argv);
            return new Python();
        }

        @Nonnull
        public String createReferenceTo(@Nonnull final Object value) throws JepException {
            @Nonnull final var variable = String.format(
                    "JAVA_%s_%s", value.getClass().getCanonicalName().replaceAll("[.$]", "_"), value.hashCode());

            this.createReference(variable, value);
            return variable;
        }

        @Nonnull
        public Python createReference(@Nonnull final String variable, @Nonnull final Object value) throws JepException {
            this.interpreter.set(variable, value);
            return this;
        }

        @Nonnull
        public Python execute(@Nonnull final String code) throws JepException {
            this.interpreter.eval(code);
            return this;
        }

        @Nonnull
        public Python executeFile(@Nonnull final File pythonFile) throws IOException, JepException {
            return this.execute(String.format(
                    "with open('%s') as python_file:\n    exec(compile(python_file.read(), '%s', mode='exec'))",
                    pythonFile.getCanonicalPath().replaceAll("\\\\", "\\\\\\\\"),
                    pythonFile.getCanonicalPath().replaceAll("\\\\", "\\\\\\\\")));
        }

        @Nonnull
        public Python importModule(@Nonnull final String module) throws JepException {
            return this.execute(String.format("import %s", module));
        }

        @Nonnull
        public Python importFromModule(@Nonnull final String module, @Nonnull final String... members) throws JepException {
            return this.execute(String.format("from %s import %s", module, String.join(", ", members)));
        }

        @Nonnull
        public Python launchKernelApp() throws JepException {
            return this.execute("__import__('ipykernel.kernelapp').kernelapp.launch_new_instance()");
        }
    }

    public interface Settings<S extends EmuPythonService> extends EmuService.Settings<S> {}

    public EmuPythonService(@Nonnull final YangPool pool, @Nonnull final Settings<? extends EmuPythonService> settings) {
        super(pool, settings);
    }

    @Override
    public void run() {
        try {
            @Nonnull final var python = Python.withArguments("netemu");
            @Nonnull final var yangPoolVariable = python.importModule("jep").importModule("java.lang")
                    .execute("jep.PyJObject = type(java.lang.Object())")
                    .execute("jep.PyJClass = type(jep.PyJObject)")
                    .importFromModule("netemu", "YANGPool")
                    .createReferenceTo(super.yangPool());

            this.launchAppFrom(python.execute(String.format("type(YANGPool).INSTANCE = YANGPool(%s)", yangPoolVariable)));

    } catch (final JepException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void launchAppFrom(@Nonnull final Python python) throws JepException;
}
