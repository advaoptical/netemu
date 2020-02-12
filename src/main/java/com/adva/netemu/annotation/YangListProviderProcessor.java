package com.adva.netemu.annotation;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import com.google.auto.service.AutoService;

import com.adva.netemu.YangListProvider;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangListProvider"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangListProviderProcessor extends YangProviderProcessor {

    public YangListProviderProcessor() {
        super(YangListProvider.class);
    }

    @Nonnull @Override
    protected String provideUtilityClassTemplateName() {
        return "$Yang.List-extended.java";
    }

    @Nonnull @Override
    protected Map<String, Object> provideTemplateContextFrom(
            @Nonnull final Annotation annotation) {

        final TypeElement yangKeyClass;
        try {
            final var __ = ((YangListProvider) annotation).key();
            throw new Error();

        } catch (final MirroredTypeException e) {
            yangKeyClass = (TypeElement) super.processingEnv.getTypeUtils()
                    .asElement(e.getTypeMirror());
        }

        final Optional<ExecutableElement> keyGetter = StreamEx
                .of(ElementFilter.methodsIn(
                        yangKeyClass.getEnclosedElements()))

                .findFirst(method -> method.getSimpleName().toString()
                        .startsWith("get"));

        if (keyGetter.isEmpty()) {
            super.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Missing get*() method in *Key class",
                    yangKeyClass);

            return null;
        }

        final var keyClass = (TypeElement) super.processingEnv.getTypeUtils()
                .asElement(keyGetter.get().getReturnType());

        return EntryStream.of(super.provideTemplateContextFrom(annotation))
                .append("keyClass", keyClass.getQualifiedName().toString())
                .toImmutableMap();
    }

    @Nonnull @Override
    protected String provideBindingClassSuffix() {
        return "$YangListBinding";
    }

    @Nonnull @Override
    protected String provideBindingGetterName() {
        return "getYangListBinding";
    }

    @Nonnull @Override
    protected TypeElement provideOriginClassFrom(
            @Nonnull final Annotation annotation) {

        try {
            final var __ = ((YangListProvider) annotation).origin();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils()
                    .asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected TypeElement provideYangClassFrom(
            @Nonnull final Annotation annotation) {

        try {
            final var __ = ((YangListProvider) annotation).value();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils()
                    .asElement(e.getTypeMirror());
        }
    }
}
