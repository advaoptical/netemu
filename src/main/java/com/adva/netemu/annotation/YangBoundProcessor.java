package com.adva.netemu.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import one.util.streamex.StreamEx;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import org.opendaylight.yangtools.yang.binding.ResourceYangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

// import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.model.util.BindingGeneratorUtil;
import org.opendaylight.mdsal.binding.spec.naming.BindingMapping;

import com.adva.netemu.YangBound;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangBound"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangBoundProcessor extends AbstractProcessor {

    private static class CompileTimeClassFieldMap extends AbstractMap<String, String> {

        @Nonnull
        private final Set<Entry<String, String>> entries;

        public CompileTimeClassFieldMap(@Nonnull final TypeElement element) {
            this.entries = StreamEx.of(element.getEnclosedElements()).filter(member -> member.getKind() == ElementKind.FIELD)
                    .map(VariableElement.class::cast)
                    .mapToEntry(member -> member.getSimpleName().toString(), member -> member.getConstantValue().toString())
                    .toImmutableSet();
        }

        @Nonnull @Override
        public Set<Entry<String, String>> entrySet() {
            return this.entries;
        }
    }

    @SuppressWarnings({"UnstableApiUsage", "unused"})
    private static class CompileTimeYangModuleInfo extends ResourceYangModuleInfo {

        @Nonnull @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        private final CompileTimeClassFieldMap infoFields;

        public CompileTimeYangModuleInfo(@Nonnull final TypeElement element) {
            this.infoFields = new CompileTimeClassFieldMap(element);
        }

        @Nonnull @Override @SuppressWarnings({"UnstableApiUsage"})
        protected String resourceName() {
            return this.infoFields.get("RESOURCE");
        }

        @Nonnull @Override
        public QName getName() {
            return QName.create(this.infoFields.get("NAMESPACE"), this.infoFields.get("REVISION"), this.infoFields.get("NAME"));
        }
    }

    @Nonnull
    private static Handlebars HANDLEBARS = new Handlebars().with(new ClassPathTemplateLoader("/templates"))
            .registerHelpers(StringHelpers.class);

    @Nonnull @SuppressWarnings({"unused"})
    private static Map<TypeElement, SchemaContext> YANG_CONTEXT_CACHE = Collections.synchronizedMap(new HashMap<>());

    @Nonnull
    private final Class<? extends Annotation> annotationClass;

    protected YangBoundProcessor(@Nonnull final Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public YangBoundProcessor() {
        this(YangBound.class);
    }

    @Nonnull
    protected String provideBindingClassSuffix() {
        return "$YangBinding";
    }

    @Nonnull
    protected String provideBindingClassAnnotationName() {
        return "YangProvider";
    }

    @Nonnull
    protected String provideUtilityClassSuffix() {
        return "$Yang";
    }

    @Nonnull
    protected String provideBindingClassTemplateName() {
        return "$YangBinding.java";
    }

    protected Optional<TypeElement> resolveInnerClass(
            @Nonnull final TypeElement outerClass, @Nonnull final String... innerClassNames) {

        @Nonnull TypeElement currentClass = outerClass;
        for (final var memberName: innerClassNames) {

            boolean foundMember = false;
            for (@Nonnull final var member: currentClass.getEnclosedElements()) {
                if (member.getSimpleName().toString().equals(memberName)) {
                    if (member.getKind() != ElementKind.CLASS) {
                        super.processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR, String.format("'%s' is not a class", memberName), member);

                        return Optional.empty();
                    }

                    currentClass = (TypeElement) member;
                    foundMember = true;
                    break;
                }
            }

            if (!foundMember) {
                super.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR, String.format("Missing inner class '%s'", memberName), currentClass);

                return Optional.empty();
            }
        }

        return Optional.of(currentClass);
    }

    @Nonnull
    protected Optional<Map<String, Object>> provideTemplateContextFrom(
            @Nonnull final TypeElement annotatedClass, @Nonnull final Annotation annotation) {

        @Nonnull final var className = ClassName.get(annotatedClass);

        @Nonnull final TypeElement compileTimeContext = this.provideCompileTimeContextFrom(annotation);
        @Nonnull final String yangNamespace = this.provideYangNamespaceFrom(annotation);
        @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var yangNorevPackage = BindingMapping.getRootPackageName(
                QNameModule.create(URI.create(yangNamespace)));

        @Nonnull final Optional<TypeElement> yangNorevLookup = this.resolveInnerClass(
                compileTimeContext, "CompileTime", "YangModules", "NorevLookup");

        if (yangNorevLookup.isEmpty()) {
            return Optional.empty();
        }

        @Nonnull final Optional<TypeElement> yangModuleCompileTimeInfo = this.resolveInnerClass(
                yangNorevLookup.get(), yangNorevPackage.replace('.', '$'));

        if (yangModuleCompileTimeInfo.isEmpty()) {
            return Optional.empty();
        }

        // TODO: @Nonnull final var yangModuleInfos = new HashSet<YangModuleInfo>();
        /*
        for (final var yangModuleElement : StreamEx.of(yangNorevLookup.get().getEnclosedElements())
                .filter(element -> element.getKind() == ElementKind.CLASS)) {

            // TODO: yangModuleInfos.add(new CompileTimeYangModuleInfo((TypeElement) yangModuleElement));

            if (yangModuleElement.getSimpleName().toString().equals(yangNorevPackage.replace('.', '$'))) {
                yangNorevLookup = (TypeElement) yangModuleElement;
            }
        }
        */

        // TODO: @Nonnull final var yangRegistry = ModuleInfoBackedContext.create();
        // TODO: yangRegistry.addModuleInfos(yangModuleInfos);

        @Nullable String yangModulePackage = null;
        for (@Nonnull final var member: yangModuleCompileTimeInfo.get().getEnclosedElements()) {
            if (member.getSimpleName().toString().equals("PACKAGE")) {
                yangModulePackage = (String) ((VariableElement) member).getConstantValue();
                break;
            }
        }

        if (yangModulePackage == null) {
            super.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, "Missing static 'PACKAGE' value", yangModuleCompileTimeInfo.get());

            return Optional.empty();
        }

        // TODO: final var yangContext = yangRegistry.getSchemaContext();

        @Nonnull final var yangPath = SchemaPath.create(
                StreamEx.of(this.provideYangPathFrom(annotation).split("/")).map(name -> QName.create(yangNamespace, name))
                        //  Needed to avoid "java.lang.IllegalStateException: stream has already been operated upon or closed"
                        .toList(), // Because SchemaPath.create checks Iterables.isEmpty before iteration

                true); // true -> SchemaPath is absolute

        @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var yangClassName = ClassName.get(
                BindingGeneratorUtil.packageNameForGeneratedType(yangModulePackage, yangPath),
                BindingMapping.getClassName(yangPath.getLastComponent()));

        return Optional.of(Map.of(
                "package", className.packageName(),
                "class", className.simpleName(),

                "yangPackage", yangClassName.packageName(),
                "yangClass", yangClassName.simpleName(),

                "bindingClassSuffix", this.provideBindingClassSuffix(),
                "utilityClassSuffix", this.provideUtilityClassSuffix(),
                "providerAnnotation", this.provideBindingClassAnnotationName()));
    }

    @Nonnull
    protected TypeElement provideCompileTimeContextFrom(@Nonnull final Annotation annotation) {
        try {
            @Nonnull @SuppressWarnings({"unused"}) final var provokeException = ((YangBound) annotation).context();
            throw new Error();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    @Nonnull
    protected String provideYangNamespaceFrom(@Nonnull final Annotation annotation) {
        return ((YangBound) annotation).namespace();
    }

    @Nonnull
    protected String provideYangPathFrom(@Nonnull final Annotation annotation) {
        return ((YangBound) annotation).value();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (@Nonnull final var annotatedElement : StreamEx.of(annotations).map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Set::stream).toSet()) {

            if (annotatedElement.getKind() != ElementKind.CLASS) {
                super.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("@%s annotation supports only classes", this.annotationClass.getSimpleName()),
                        annotatedElement);

                return true;
            }

            @Nonnull final var annotatedClass = (TypeElement) annotatedElement;
            @Nonnull final var className = ClassName.get(annotatedClass);
            @Nonnull final var bindingClassName = ClassName.get(
                    className.packageName(), className.simpleName() + this.provideBindingClassSuffix());

            super.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE, String.format("Generating class %s", bindingClassName));

            this.provideTemplateContextFrom(annotatedClass, annotatedClass.getAnnotation(this.annotationClass))
                    .ifPresent(context -> {
                        try (@Nonnull final var writer = super.processingEnv.getFiler()
                                .createSourceFile(bindingClassName.canonicalName(), annotatedClass)
                                .openWriter()) {

                            HANDLEBARS.compile(this.provideBindingClassTemplateName()).apply(context, writer);

                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        return true;
    }
}
