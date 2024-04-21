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

import com.adva.netemu.YangListBound;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangListBound"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class YangListBoundProcessor extends YangBoundProcessor {

    protected YangListBoundProcessor(@Nonnull final Class<? extends Annotation> annotationClass) {
        super(annotationClass);
    }

    public YangListBoundProcessor() {
        super(YangListBound.class);
    }

    /*
    @Nonnull @Override
    protected String provideBindingClassSuffix() {
        return "_YangListBinding";
    }
    */

    @Nonnull @Override
    protected String provideBindingClassAnnotationName() {
        return "YangListProvider";
    }

    /*
    @Nonnull @Override
    protected String provideUtilityClassSuffix() {
        return "_YangList";
    }
    */

    @Nonnull @Override
    protected String provideBindingClassTemplateName() {
        return "$YangListBinding.java";
    }

    @Nonnull @Override
    protected TypeElement provideCompileTimeContextFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangListBound) annotation).context();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected String provideYangNamespaceFrom(@Nonnull final Annotation annotation) {
        return ((YangListBound) annotation).namespace();
    }

    @Nonnull @Override
    protected String provideYangPathFrom(@Nonnull final Annotation annotation) {
        return ((YangListBound) annotation).value();
    }
}
