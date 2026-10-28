package generator;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.squareup.javapoet.*;

import generator.bean.ClassInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.apache.commons.lang3.StringUtils;

/**
 * MockitoGenerator.java Génére tous les attributs de classe et leur import
 * 
 * @author psiron01 Date: 22 juil. 2026
 */

public class MockitoGenerator
{

    public static void addMockitoFields(
        ClassInfo classInfo,
        TypeSpec.Builder builder)
    {

        Map<String, String> importMap = getImportMap(classInfo);

        builder.addAnnotation(
            AnnotationSpec.builder(
                ClassName.get(
                    "org.junit.jupiter.api.extension",
                    "ExtendWith"))
                .addMember("value",
                    "$T.class",
                    ClassName.get(
                        "org.mockito.junit.jupiter",
                        "MockitoExtension"))
                .build());

        boolean uniquementStatic = classInfo.isAllMethodStatic();
        // System.out.println(classInfo.getQualifiedName()+" uniquement Static="+uniquementStatic);

        /*
         * on ajoutye injectMocks si au moins une method n'est pas static. si toutes les methodes sont static , pas d@injects
         * mocks
         */
        if (!uniquementStatic)
        {
            builder.addField(
                FieldSpec.builder(
                    ClassName.bestGuess(classInfo.getClassName()),
                    StringUtils.uncapitalize(classInfo.getClassName()), // "service",
                    Modifier.PRIVATE)
                    .addAnnotation(
                        ClassName.get("org.mockito", "InjectMocks"))
                    .build());
        }

        classInfo.getFields().forEach((name, field) -> {
            // clazz.getFields().forEach(field -> {

            // String type = field.getElementType().asString();
            String type = field.getType();
            /* just field with annotation @Autowired */

            /* Si anotation @Autowired ou Parametrage xml pour injection spring */
            if (!type.startsWith("java.lang") && (field.getAnnotations().contains("@Autowired") ||
                field.getXmlInject()))
            {
                ClassName className;
                if (importMap.containsKey(type))
                {
                    className = ClassName.get(importMap.get(type), type);
                }
                else
                {
                    className = ClassName.bestGuess(type);
                }

                builder.addField(
                    FieldSpec.builder(
                        className,
                        // "mock" + StringUtils.capitalize(field.getVariable(0).getNameAsString()),
                        "mock" + StringUtils.capitalize(field.getName()),
                        Modifier.PRIVATE)
                        .addAnnotation(
                            ClassName.get("org.mockito", "Mock"))
                        .build());
            }

        });

    }

    /* ajouter un attribut de la classe (qui est utilisé dans ls méthodes) et son import */
    public static void addMockitoField(TypeSpec.Builder builder, String type, ClassInfo classInfo)
    {
        Map<String, String> importMap = getImportMap(classInfo);

        ClassName className;
        if (importMap.containsKey(type))
        {
            className = ClassName.get(importMap.get(type), type);
        }
        else
        {
            className = ClassName.bestGuess(type);
        }

        builder.addField(
            FieldSpec.builder(
                className,
                "mock" + StringUtils.capitalize(type),
                Modifier.PRIVATE)
                .addAnnotation(
                    ClassName.get("org.mockito", "Mock"))
                .build());

    }

    private static Map<String, String> getImportMap(ClassInfo classInfo)
    {
        /* créer une map des imports */
        Map<String, String> importMap = new HashMap<>();
        classInfo.getImports().forEach((key, val) -> {
            String importName = val.substring(0, val.lastIndexOf('.'));
            importMap.put(key, importName);
        });

        return importMap;
    }

}
