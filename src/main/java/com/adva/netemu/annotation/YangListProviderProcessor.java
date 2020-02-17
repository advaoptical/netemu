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

import com.google.auto.service.AutoService;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import com.adva.netemu.YangListProvider;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangListProvider"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangListProviderProcessor extends YangProviderProcessor {

    protected YangListProviderProcessor(@Nonnull final Class<? extends Annotation> annotationClass) {
        super(annotationClass);
    }

    public YangListProviderProcessor() {
        super(YangListProvider.class);
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
    protected String provideUtilityClassTemplateName() {
        return "$Yang.List-extended.java";
    }

    @Nonnull @Override
    protected Optional<Map<String, Object>> provideTemplateContextFrom(@Nonnull final Annotation annotation) {
        @Nonnull final TypeElement yangKeyClass = this.provideYangKeyClassFrom(annotation);
        @Nonnull final Optional<ExecutableElement> keyGetter = StreamEx
                .of(ElementFilter.methodsIn(yangKeyClass.getEnclosedElements()))
                .findFirst(method -> method.getSimpleName().toString().startsWith("get"));

        if (keyGetter.isEmpty()) {
            super.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, "Missing get*() method in *Key class", yangKeyClass);

            return Optional.empty();
        }

        final var keyClass = (TypeElement) super.processingEnv.getTypeUtils().asElement(keyGetter.get().getReturnType());
        return super.provideTemplateContextFrom(annotation).map(context -> EntryStream.of(context)
                .append("keyClass", keyClass.getQualifiedName().toString())
                .toImmutableMap());
    }

    @Nonnull @Override
    protected TypeElement provideOriginClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangListProvider) annotation).origin();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull @Override
    protected TypeElement provideYangClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangListProvider) annotation).value();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull
    protected TypeElement provideYangKeyClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangListProvider) annotation).key();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }
}
