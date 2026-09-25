package generator;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import generator.bean.ClassInfo;

import java.util.HashMap;
import java.util.Map;

public class TypeNameFactory {

    private  Map<String, String> imports = new HashMap<>();

    public TypeNameFactory(ClassInfo classInfo) {

        imports = classInfo.getImports();
        // package courant
        String currentPackage = classInfo.getPackageName();
    }

    public TypeName toTypeName(Type type) {

        if (type.isPrimitiveType()) {
            return primitive(type.asPrimitiveType());
        }

        if (type.isVoidType()) {
            return TypeName.VOID;
        }

        if (type.isClassOrInterfaceType()) {
            return classType(type.asClassOrInterfaceType());
        }

        return TypeName.OBJECT;
    }

    private TypeName primitive(PrimitiveType primitive) {

        return switch (primitive.getType()) {
            case BOOLEAN -> TypeName.BOOLEAN;
            case BYTE -> TypeName.BYTE;
            case SHORT -> TypeName.SHORT;
            case INT -> TypeName.INT;
            case LONG -> TypeName.LONG;
            case FLOAT -> TypeName.FLOAT;
            case DOUBLE -> TypeName.DOUBLE;
            case CHAR -> TypeName.CHAR;
        };
    }

    private TypeName classType(ClassOrInterfaceType type) {

        ClassName raw = resolve(type.getNameAsString());

        if (type.getTypeArguments().isEmpty()) {
            return raw;
        }

        TypeName[] arguments =
                type.getTypeArguments()
                        .get()
                        .stream()
                        .map(this::toTypeName)
                        .toArray(TypeName[]::new);

        return ParameterizedTypeName.get(raw, arguments);
    }

    private ClassName resolve(String simpleName) {

        // Types java.lang
        switch (simpleName) {

            case "String":
                return ClassName.get("java.lang", "String");

            case "Integer":
                return ClassName.get("java.lang", "Integer");

            case "Long":
                return ClassName.get("java.lang", "Long");

            case "Boolean":
                return ClassName.get("java.lang", "Boolean");

            case "Double":
                return ClassName.get("java.lang", "Double");

            case "Float":
                return ClassName.get("java.lang", "Float");

            case "Object":
                return ClassName.get("java.lang", "Object");
        }

       // Collections Java
        switch (simpleName) {

            case "List":
                return ClassName.get("java.util", "List");
                
            case "ArrayList":
                return ClassName.get("java.util", "ArrayList");    
                
            case "Set":
                return ClassName.get("java.util", "Set");
             
            case "HashSet":
                return ClassName.get("java.util", "HashSet");     

            case "Map":
                return ClassName.get("java.util", "Map");
                
            case "Hashmap":
                return ClassName.get("java.util", "Hashmap"); 

            case "Optional":
                return ClassName.get("java.util", "Optional");
        }
        
        // import trouvé
        if (imports.containsKey(simpleName)) {

            String fqcn = imports.get(simpleName);

            int idx = fqcn.lastIndexOf('.');

            return ClassName.get(
                    fqcn.substring(0, idx),
                    fqcn.substring(idx + 1));
        }
        
        // Dernier recours
        return ClassName.bestGuess(simpleName);
    }
}