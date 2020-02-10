package com.adva.netemu.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;

import one.util.streamex.StreamEx;

import com.google.auto.service.AutoService;

import com.squareup.javapoet.ClassName;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import com.adva.netemu.YangProvider;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangProvider"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangProviderProcessor extends AbstractProcessor {

    @Nonnull
    private static Handlebars HANDLEBARS = new Handlebars()
            .with(new ClassPathTemplateLoader("/templates"));

    @Nonnull
    private final Class<? extends Annotation> _annotationClass;

    @Nonnull
    protected final String UTILITY_CLASS_SUFFIX = "$Yang";

    protected YangProviderProcessor(
            @Nonnull final Class<? extends Annotation> annotationClass) {

        this._annotationClass = annotationClass;
    }

    public YangProviderProcessor() {
        this(YangProvider.class);
    }

    @Nonnull
    protected String provideUtilityClassTemplateName() {
        return "$Yang.java";
    }

    @Nonnull
    protected Map<String, String> provideTemplateContextFrom(
            @Nonnull final Annotation annotation) {

        final var originClassName = ClassName.get(
                this.provideOriginClassFrom(annotation));

        final var yangClassName = ClassName.get(
                this.provideYangClassFrom(annotation));

        return Map.of(
                "package", originClassName.packageName(),
                "class", originClassName.simpleName(),

                "yangPackage", yangClassName.packageName(),
                "yangClass", yangClassName.simpleName(),

                "bindingClassSuffix", this.provideBindingClassSuffix(),
                "bindingGetter", this.provideBindingGetterName());
    }

    @Nonnull
    protected String provideBindingClassSuffix() {
        return "$YangBinding";
    }

    @Nonnull
    protected String provideBindingGetterName() {
        return "getYangBinding";
    }

    @Nonnull
    protected TypeElement provideOriginClassFrom(
            @Nonnull final Annotation annotation) {

        try {
            final var __ = ((YangProvider) annotation).origin();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils()
                    .asElement(e.getTypeMirror());
        }

        throw new Error();
    }

    @Nonnull
    protected TypeElement provideYangClassFrom(
            @Nonnull final Annotation annotation) {

        try {
            final var __ = ((YangProvider) annotation).value();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils()
                    .asElement(e.getTypeMirror());
        }

        throw new Error();
    }

    @Override
    public boolean process(
            @Nonnull final Set<? extends TypeElement> annotations,
            @Nonnull final RoundEnvironment roundEnv) {

        for (final var annotatedElement: StreamEx.of(annotations)
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Set::stream).toSet()) {

            final var annotation = annotatedElement.getAnnotation(
                    this._annotationClass);

            if (annotatedElement.getKind() != ElementKind.CLASS) {
                super.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR, String.format(
                                "@%s annotation supports only classes",
                                this._annotationClass.getSimpleName()),

                        annotatedElement);

                return true;
            }

            final var utilityClassName =
                    this.provideOriginClassFrom(annotation).getQualifiedName()
                            + UTILITY_CLASS_SUFFIX;

            super.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE, String.format(
                            "Generating class %s", utilityClassName));

            try (final var writer = this.processingEnv.getFiler()
                    .createSourceFile(utilityClassName).openWriter()) {

                HANDLEBARS.compile(this.provideUtilityClassTemplateName())
                        .apply(
                                this.provideTemplateContextFrom(annotation),
                                writer);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }
}
