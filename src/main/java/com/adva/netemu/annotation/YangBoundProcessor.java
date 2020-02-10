package com.adva.netemu.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;

import static java.lang.String.format;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import javax.annotation.Nonnull;
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.squareup.javapoet.ClassName;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.ResourceYangModuleInfo;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import org.opendaylight.mdsal.binding.model.util.BindingGeneratorUtil;
import org.opendaylight.mdsal.binding.spec.naming.BindingMapping;

import com.adva.netemu.YangBound;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.adva.netemu.YangBound"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class YangBoundProcessor extends AbstractProcessor {

    private class CompileTimeClassFieldMap
            extends AbstractMap<String, String> {

        @Nonnull
        private final TypeElement _element;

        @Nonnull
        private final Set<Entry<String, String>> _entries;

        public CompileTimeClassFieldMap(@Nonnull final TypeElement element) {
            this._element = element;
            this._entries = element.getEnclosedElements().stream()
                    .filter(member -> member.getKind() == ElementKind.FIELD)
                    .map(member -> new AbstractMap.SimpleImmutableEntry<>(
                            ((VariableElement) member).getSimpleName()
                                    .toString(),

                            ((VariableElement) member).getConstantValue()
                                    .toString()))

                    .collect(toImmutableSet());
        }

        @Nonnull @Override
        public Set<Entry<String, String>> entrySet() {
            return this._entries;
        }
    }

    private class CompileTimeYangModuleInfo extends ResourceYangModuleInfo {

        @Nonnull
        private final CompileTimeClassFieldMap _infoFields;

        public CompileTimeYangModuleInfo(@Nonnull final TypeElement element) {
            this._infoFields = new CompileTimeClassFieldMap(element);
        }

        @Nonnull @Override
        protected String resourceName() {
            return this._infoFields.get("RESOURCE");
        }

        @Nonnull @Override
        public QName getName() {
            return QName.create(
                    this._infoFields.get("NAMESPACE"),
                    this._infoFields.get("REVISION"),
                    this._infoFields.get("NAME"));
        }
    }

    @Nonnull
    private static Handlebars HANDLEBARS = new Handlebars()
            .with(new ClassPathTemplateLoader("/templates"));

    @Nonnull
    private static Map<TypeElement, SchemaContext> YANG_CONTEXT_CACHE =
            synchronizedMap(new HashMap<>());

    @Nonnull
    private final Class<? extends Annotation> _annotationClass;

    protected YangBoundProcessor(
            @Nonnull final Class<? extends Annotation> annotationClass) {

        this._annotationClass = annotationClass;
    }

    public YangBoundProcessor() {
        this(YangBound.class);
    }

    @Nonnull
    protected String provideBindingClassSuffix() {
        return "$YangBinding";
    }

    @Nonnull
    protected TypeElement provideCompileTimeContextFrom(
            @Nonnull final Annotation annotation) {

        try {
            final var __ = ((YangBound) annotation).context();

        } catch (final MirroredTypeException e) {
            return (TypeElement) super.processingEnv.getTypeUtils()
                    .asElement(e.getTypeMirror());
        }

        throw new Error();
    }

    @Nonnull
    protected String provideYangNamespaceFrom(
            @Nonnull final Annotation annotation) {

        return ((YangBound) annotation).namespace();
    }

    @Nonnull
    protected String provideYangPathFrom(
            @Nonnull final Annotation annotation) {

        return ((YangBound) annotation).value();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {

        for (final var annotatedElement: annotations.stream()
                .map(roundEnv::getElementsAnnotatedWith).flatMap(Set::stream)
                .collect(toSet())) {

            if (annotatedElement.getKind() != ElementKind.CLASS) {
                super.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@YangBound annotation supports only classes",
                        annotatedElement);

                return true;
            }

            final var annotatedClass = (TypeElement) annotatedElement;
            final var annotation = annotatedClass.getAnnotation(
                    this._annotationClass);

            final TypeElement compileTimeContext =
                    this.provideCompileTimeContextFrom(annotation);

            final String yangNamespace = this.provideYangNamespaceFrom(
                    annotation);

            final var yangNorevPackage = BindingMapping.getRootPackageName(
                    QNameModule.create(URI.create(yangNamespace)));

            TypeElement yangNorevLookup = compileTimeContext;
            for (final var memberName: List.of(
                    "CompileTime", "YangModules", "NorevLookup")) {

                boolean foundMember = false;
                for (final var member: yangNorevLookup.getEnclosedElements()) {
                    if (member.getSimpleName().toString()
                            .equals(memberName)) {

                        if (member.getKind() != ElementKind.CLASS) {
                            super.processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    format("'%s' is not a class", memberName),
                                    member);

                            return true;
                        }

                        yangNorevLookup = (TypeElement) member;
                        foundMember = true;
                        break;
                    }
                }

                if (!foundMember) {
                    super.processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            format("Missing inner class '%s'", memberName),
                            yangNorevLookup);

                    return true;
                }
            }

            final var yangModuleInfos = new HashSet<YangModuleInfo>();
            for (final var yangModuleElement: yangNorevLookup
                    .getEnclosedElements().stream()
                    .filter(element -> element.getKind() == ElementKind.CLASS)
                    .collect(toList())) {

                /*
                yangModuleInfos.add(new CompileTimeYangModuleInfo(
                        (TypeElement) yangModuleElement));
                */

                if (yangModuleElement.getSimpleName().toString().equals(
                        yangNorevPackage.replace('.', '$'))) {

                    yangNorevLookup = (TypeElement) yangModuleElement;
                }
            }

            final var yangRegistry = ModuleInfoBackedContext.create();
            yangRegistry.addModuleInfos(yangModuleInfos);

            String yangModulePackage = null;
            for (final var member: yangNorevLookup.getEnclosedElements()) {
                if (member.getSimpleName().toString().equals("PACKAGE")) {
                    yangModulePackage = (String)
                            ((VariableElement) member).getConstantValue();

                    break;
                }
            }

            if (yangModulePackage == null) {
                super.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Missing static 'PACKAGE' value",
                        yangNorevLookup);

                return true;
            }

            // final var yangContext = yangRegistry.getSchemaContext();

            final List<QName> yangPathSegments =
                    stream(this.provideYangPathFrom(annotation).split("/"))
                            .map(name -> QName.create(yangNamespace, name))
                            .collect(toList());

            final var yangPath = SchemaPath.create(yangPathSegments, true);
            final var yangClassName = ClassName.get(
                    BindingGeneratorUtil.packageNameForGeneratedType(
                            yangModulePackage, yangPath),

                    BindingMapping.getClassName(yangPath.getLastComponent()));

            final var className = ClassName.get(annotatedClass);
            final var bindingClassName = ClassName.get(
                    className.packageName(), className.simpleName()
                            + this.provideBindingClassSuffix());

            super.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE, format(
                            "Generating class %s", bindingClassName));

            final Template bindingClassTemplate;
            try {
                bindingClassTemplate = HANDLEBARS.compile(
                        this.provideBindingClassSuffix() + ".java");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (final var writer = super.processingEnv.getFiler()
                    .createSourceFile(
                            bindingClassName.canonicalName(), annotatedClass)

                    .openWriter()) {

                bindingClassTemplate.apply(Map.of(
                        "package", className.packageName(),
                        "class", className.simpleName(),
                        "yangPackage", yangClassName.packageName(),
                        "yangClass", yangClassName.simpleName()

                ), writer);

            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }
}
