package com.adva.netemu.annotation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.annotation.Annotation;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import com.adva.netemu.YangProvider;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangProvider"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangProviderProcessor extends AbstractProcessor {

    @Nonnull
    private static Handlebars HANDLEBARS = new Handlebars().with(new ClassPathTemplateLoader("/templates"))
            .registerHelpers(StringHelpers.class);

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
    protected Optional<String> providePythonClassTemplateName() {
        return Optional.empty();
    }

    @Nonnull
    protected Optional<Map<String, Object>> provideTemplateContextFrom(@Nonnull final Annotation annotation) {
        @Nonnull final var originClassName = ClassName.get(this.provideOriginClassFrom(annotation));
        @Nonnull final var yangClassName = ClassName.get(this.provideYangClassFrom(annotation));

        @Nonnull final var yangBuilderClass =  this.provideYangBuilderClassFrom(annotation);
        @Nonnull final var yangDataGetters = EntryStream.of(StreamEx
                .of(ElementFilter.methodsIn(yangBuilderClass.getEnclosedElements()))
                .mapToEntry(method -> method.getSimpleName().toString(), Function.identity())
                .filterKeys(name -> name.matches("^(get|is)[A-Z].*$"))
                .mapKeyValue((name, method) -> new SimpleImmutableEntry<>(name, Map.of(
                        "valueClass", ((TypeElement) this.processingEnv.getTypeUtils().asElement(method.getReturnType()))
                                .getQualifiedName().toString(),

                        "reprefixedName", name.replaceFirst("^is", "get"),
                        "setterName", name.replaceFirst("^(get|is)", "set"),

                        "pythonName", name.replaceFirst("^(get|is)", "").replaceAll("[A-Z]", "_$0").toLowerCase()
                                .substring(1)))));

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

    @Nonnull
    protected TypeElement provideYangBuilderClassFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangProvider) annotation).builder();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull
    protected Optional<TypeElement> providePythonizerClassFrom(@Nonnull final Annotation annotation) {
        return Optional.empty();
    }

    @Override
    public boolean process(@Nonnull final Set<? extends TypeElement> annotations, @Nonnull final RoundEnvironment roundEnv) {
        for (@Nonnull final var annotatedElement : StreamEx.of(annotations).map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Set::stream).toSet()) {

            if (annotatedElement.getKind() != ElementKind.CLASS) {
                super.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("@%s annotation supports only classes", this.annotationClass.getSimpleName()),
                        annotatedElement);

                return true;
            }

            this.generateUtilitiesFrom((TypeElement) annotatedElement, annotatedElement.getAnnotation(this.annotationClass));
        }

        return true;
    }

    protected void generateUtilitiesFrom(@Nonnull final TypeElement annotatedClass, @Nonnull final Annotation annotation) {
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

            this.providePythonizerClassFrom(annotation)
                    .ifPresent(pythonizerClass -> this.generatePythonizationsFrom(pythonizerClass, context));
        });
    }

    protected void generatePythonizationsFrom(@Nonnull final TypeElement pythonizerClass, @Nonnull final Map<String, Object> templateContext) {
        @Nullable String pythonYangModelsFilePath = null;
        for (@Nonnull final var member: pythonizerClass.getEnclosedElements()) {
            if (member.getSimpleName().toString().equals("YANG_MODELS_FILE")) {
                pythonYangModelsFilePath = (String) ((VariableElement) member).getConstantValue();
                break;
            }
        }

        if (pythonYangModelsFilePath == null) {
            super.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, "Missing static 'YANG_MODELS_FILE' value", pythonizerClass);

            return;
        }

        @Nonnull final var pythonYangModelsFile = new File(pythonYangModelsFilePath);
        this.providePythonClassTemplateName().ifPresent(templateName -> {
            try (@Nonnull final var writer = new FileWriter(pythonYangModelsFile, true)) { // true -> append to file
                HANDLEBARS.compile(templateName).apply(templateContext, writer);

            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}