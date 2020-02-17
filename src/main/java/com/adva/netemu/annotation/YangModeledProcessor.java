package com.adva.netemu.annotation;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;

import com.google.auto.service.AutoService;
import one.util.streamex.EntryStream;

import com.adva.netemu.YangModeled;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangModeled"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangModeledProcessor extends YangBoundProcessor {

    public YangModeledProcessor() {
        super(YangModeled.class);
    }

    @Nonnull @Override
    protected String provideBindingClassSuffix() {
        return "$YangModelBinding";
    }

    @Nonnull @Override
    protected String provideBindingClassAnnotationName() {
        return "YangModelProvider";
    }

    @Nonnull @Override
    protected String provideUtilityClassSuffix() {
        return "$YangModel.Yang";
    }

    /*
    @Nonnull @Override
    protected Optional<Map<String, Object>> provideTemplateContextFrom(@Nonnull TypeElement annotatedClass) {
        return super.provideTemplateContextFrom(annotatedClass).map(context -> EntryStream.of(context)
                .append("class", String.format("%s$YangModel", annotatedClass.getSimpleName()))
                .toMap((oldValue, newValue) -> newValue));
    }
    */

    @Nonnull @Override
    protected TypeElement provideCompileTimeContextFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangModeled) annotation).context();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected String provideYangNamespaceFrom(@Nonnull final Annotation annotation) {
        return ((YangModeled) annotation).namespace();
    }

    @Nonnull @Override
    protected String provideYangPathFrom(@Nonnull final Annotation annotation) {
        return ((YangModeled) annotation).value();
    }
}
