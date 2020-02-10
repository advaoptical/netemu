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
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangListBoundProcessor  extends YangBoundProcessor {

    public YangListBoundProcessor() {
        super(YangListBound.class);
    }

    @Nonnull @Override
    protected String provideBindingClassSuffix() {
        return "$YangListBinding";
    }

    @Nonnull @Override
    protected TypeElement provideCompileTimeContextFrom(
            @Nonnull final Annotation annotation) {

        try {
            final var __ = ((YangListBound) annotation).context();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils()
                    .asElement(e.getTypeMirror());
        }

        throw new Error();
    }

    @Nonnull @Override
    protected String provideYangNamespaceFrom(
            @Nonnull final Annotation annotation) {

        return ((YangListBound) annotation).namespace();
    }

    @Nonnull @Override
    protected String provideYangPathFrom(
            @Nonnull final Annotation annotation) {

        return ((YangListBound) annotation).value();
    }
}
