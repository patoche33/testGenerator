package generator.bean;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.body.FieldDeclaration;

public class ClassInfo {

    private String packageName;

    private String className;

    private String qualifiedName;
    
    private Path file;
    
    private boolean allMethodStatic;
   
    public Path getFile()
    {
        return file;
    }

    public void setFile(Path file)
    {
        this.file = file;
    }

    /**
     * clé = nom simple
     * valeur = nom qualifié
     */
    private final Map<String, String> imports = new HashMap<>();

    /**
     * clé = nom méthode
     */
    private final Map<String, MethodInfo> methods = new HashMap<>();

    /**
     * clé = nom field
     */
    private final  Map<String,FieldInfo> fields  = new HashMap<>();

    
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public Map<String, String> getImports() {
        return imports;
    }

    public Map<String,MethodInfo> getMethods() {
        return methods;
    }

    public  Map<String,FieldInfo> getFields()
    {
        return fields;
    }

    public boolean isAllMethodStatic()
    {
        return allMethodStatic;
    }

    public void setAllMethodStatic(boolean allMethodStatic)
    {
        this.allMethodStatic = allMethodStatic;
    }
  
}

