package com.adva.netemu.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

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
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import com.adva.netemu.YangProvider;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangProvider"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangProviderProcessor extends AbstractProcessor {

    @Nonnull
    private static Handlebars HANDLEBARS = new Handlebars().with(new ClassPathTemplateLoader("/templates"));

    @Nonnull
    private final Class<? extends Annotation> annotationClass;

    @Nonnull
    protected final String UTILITY_CLASS_SUFFIX = "$Yang";

    protected YangProviderProcessor(@Nonnull final Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public YangProviderProcessor() {
        this(YangProvider.class);
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
    protected String provideUtilityClassSuffix() {
        return "$Yang";
    }

    @Nonnull
    protected String provideUtilityClassTemplateName() {
        return "$Yang.java";
    }

    @Nonnull
    protected Optional<Map<String, Object>> provideTemplateContextFrom(@Nonnull final Annotation annotation) {
        @Nonnull final var originClassName = ClassName.get(this.provideOriginClassFrom(annotation));

        @Nonnull final var yangClass =  this.provideYangClassFrom(annotation);
        @Nonnull final var yangClassName = ClassName.get(yangClass);

        @Nonnull final var yangDataGetters = EntryStream.of(StreamEx.of(ElementFilter.methodsIn(yangClass.getEnclosedElements()))
                .mapToEntry(method -> method.getSimpleName().toString(), Function.identity())
                .filterKeys(name -> name.matches("^(get|is)[A-Z].*$"))
                .mapKeyValue((name, method) -> new SimpleImmutableEntry<>(name, Map.of(
                        "valueClass", ((TypeElement) this.processingEnv.getTypeUtils().asElement(method.getReturnType()))
                                .getQualifiedName().toString(),

                        "reprefixedName", name.replaceFirst("^is", "get"),
                        "setterName", name.replaceFirst("^get", "set")))));

        return Optional.of(Map.of(
                "package", originClassName.packageName(),
                "class", originClassName.simpleName(),

                "bindingClassSuffix", this.provideBindingClassSuffix(),
                "bindingGetter", this.provideBindingGetterName(),

                "yangPackage", yangClassName.packageName(),
                "yangClass", yangClassName.simpleName(),
                "yangDataGetters", yangDataGetters.toImmutableMap()));
    }

    @Nonnull
    protected TypeElement provideOriginClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangProvider) annotation).origin();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull
    protected TypeElement provideYangClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangProvider) annotation).value();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Override
    public boolean process(@Nonnull final Set<? extends TypeElement> annotations, @Nonnull final RoundEnvironment roundEnv) {
        for (@Nonnull final var annotatedElement: StreamEx.of(annotations).map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Set::stream).toSet()) {

            if (annotatedElement.getKind() != ElementKind.CLASS) {
                super.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("@%s annotation supports only classes", this.annotationClass.getSimpleName()),
                        annotatedElement);

                return true;
            }

            @Nonnull final var annotatedClass = (TypeElement) annotatedElement;
            @Nonnull final var annotation = annotatedClass.getAnnotation(this.annotationClass);
            @Nonnull final var utilityClassName = String.format("%s%s",
                    this.provideOriginClassFrom(annotation).getQualifiedName(), this.provideUtilityClassSuffix());

            super.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE, String.format("Generating class %s", utilityClassName));

            this.provideTemplateContextFrom(annotation).ifPresent(context -> {
                try (@Nonnull final var writer = this.processingEnv.getFiler()
                        .createSourceFile(utilityClassName, annotatedClass)
                        .openWriter()) {

                    HANDLEBARS.compile(this.provideUtilityClassTemplateName()).apply(context, writer);

                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return true;
    }
}
