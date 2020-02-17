package com.adva.netemu.annotation;

import java.lang.annotation.Annotation;

import javax.annotation.Nonnull;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;

import com.adva.netemu.YangModelProvider;
import com.google.auto.service.AutoService;


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
}
