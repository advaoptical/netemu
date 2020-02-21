package com.adva.netemu.annotation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;

import com.adva.netemu.YangModelProvider;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangModelProvider"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangModelProviderProcessor extends YangProviderProcessor {

    public YangModelProviderProcessor() {
        super(YangModelProvider.class);
    }

    @Nonnull @Override
    protected String provideBindingClassSuffix() {
        return "$YangModelBinding";
    }

    @Nonnull @Override
    protected String provideUtilityClassSuffix() {
        return "$YangModel";
    }

    @Nonnull @Override
    protected String provideUtilityClassTemplateName() {
        return "$YangModel.java";
    }

    @Nonnull @Override
    protected Optional<String> providePythonClassTemplateName() {
        return Optional.of("_YangModel.py");
    }

    @Nonnull @Override
    protected TypeElement provideOriginClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangModelProvider) annotation).origin();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected TypeElement provideYangClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangModelProvider) annotation).value();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected TypeElement provideYangBuilderClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangModelProvider) annotation).builder();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected Optional<TypeElement> providePythonizerClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangModelProvider) annotation).pythonizer();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return Optional.of((TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror()));
        }
    }
}
