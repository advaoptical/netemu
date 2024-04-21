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
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class YangModeledProcessor extends YangBoundProcessor {

    public YangModeledProcessor() {
        super(YangModeled.class);
    }

    @Nonnull @Override
    protected String provideBindingClassSuffix() {
        return "_YangModelBinding";
    }

    @Nonnull @Override
    protected String provideBindingClassAnnotationName() {
        return "YangModelProvider";
    }

    @Nonnull @Override
    protected String provideUtilityClassSuffix() {
        return "_YangModel";
    }

    @Nonnull @Override
    protected Optional<Map<String, Object>> provideTemplateContextFrom(
            @Nonnull final TypeElement annotatedClass, @Nonnull final Annotation annotation) {

        return this.resolveInnerClass(this.provideCompileTimeContextFrom(annotation), "CompileTime", "Pythonizer")
                .flatMap(pythonizerClass -> super.provideTemplateContextFrom(annotatedClass, annotation)
                        .map(context -> EntryStream.of(context).append("pythonizerClass", pythonizerClass.getQualifiedName())
                                .toMap()));
    }

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
