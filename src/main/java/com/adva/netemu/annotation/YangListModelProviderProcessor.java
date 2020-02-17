package com.adva.netemu.annotation;

import java.lang.annotation.Annotation;

import javax.annotation.Nonnull;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;

import com.google.auto.service.AutoService;

import com.adva.netemu.YangListModelProvider;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangListModelProvider"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangListModelProviderProcessor extends YangListProviderProcessor {

    public YangListModelProviderProcessor() {
        super(YangListModelProvider.class);
    }

    @Nonnull @Override
    protected String provideBindingClassSuffix() {
        return "$YangListModelBinding";
    }

    @Nonnull @Override
    protected String provideUtilityClassSuffix() {
        return "$YangListModel";
    }

    @Nonnull @Override
    protected String provideUtilityClassTemplateName() {
        return "$YangListModel.java";
    }

    @Nonnull @Override
    protected TypeElement provideOriginClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangListModelProvider) annotation).origin();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected TypeElement provideYangClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangListModelProvider) annotation).value();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected TypeElement provideYangKeyClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangListModelProvider) annotation).key();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }
}
