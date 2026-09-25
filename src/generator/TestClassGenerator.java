package generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.squareup.javapoet.*;
import com.squareup.javapoet.JavaFile.Builder;

import generator.bean.ClassInfo;
import generator.bean.FieldInfo;
import generator.bean.MethodCallInfo;
import generator.bean.MethodInfo;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestClassGenerator
{
    /* Javaparser configuré avec JavaSymbolResolver */
    private static JavaParser javaParser;
    
    private Map<String, ClassInfo> classes = new HashMap<>();

    public TestClassGenerator(String sourceProject) throws IOException
    {
        
        Path start = Paths.get(sourceProject);

        javaParser = ConfigSolver.getJavaParser();
        
        Files.walk(start).filter(f -> f.toString().endsWith(".java"))
            .forEach(f -> {
                try
                {
                    parse(f);
                }
                catch (IOException e)
                {
                }
            });

    }

    /*
     * Parser chaque classe du projet afin de lister les classes et les méthodes de chaque classe
     */
    private void parse(Path file) throws IOException
    {
       
    
        
        javaParser.parse(file).getResult().ifPresent(cu ->  {
            try
            {
                ClassOrInterfaceDeclaration clazz = cu.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new FileClassException());

                if (clazz == null)
                return;
                ClassInfo classInfo = new ClassInfo();
                String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
                String className = clazz.getNameAsString();
                classInfo.setPackageName(pkg);
                classInfo.setClassName(className);
                classInfo.setQualifiedName(pkg + "." + className);
                classInfo.setFile(file);
                
              //  if (clazz.getNameAsString().contains("HistorisationWebService"))
              //  { System.out.println("OUUUUUUUU=>" + clazz.getNameAsString()+ ">>>>" +classInfo.getQualifiedName());

              //  }
                
                for (ImportDeclaration imp : cu.getImports())
                {
                    String qualifiedName = imp.getNameAsString();
                    String simpleName = imp.getName().getIdentifier();
                    classInfo.getImports().put(simpleName, qualifiedName);
                }

               
                
                for (MethodDeclaration method : clazz.getMethods())
                {
                    ResolvedMethodDeclaration resolvedMethod = method.resolve();
                    
                    MethodInfo methodInfo = new MethodInfo();
                    methodInfo.setName(method.getNameAsString());
                    methodInfo.setReturnType(method.getType());
                    // System.out.println("Declaration methode: "+ method.getTypeAsString()+":::"+method.getType());
                    methodInfo.setDeclaration(method);
                    //methodInfo.setCalls(method.findAll(MethodCallExpr.class));
                    methodInfo.setPrivate(method.isPrivate());
                    methodInfo.setStatic(method.isStatic());
                    methodInfo.setParameters(method.getParameters());
                    if (method.getBody().isPresent()) {
                        methodInfo.setBody(method.getBody().get().toString());
                    }
                    
                   // methodInfo.setSignature(method.getSignature().asString());
                    methodInfo.setSignature(method.getSignature().asString());
            
                                   
                    /* la clé de la map est la signature car possibilité de redefinition de methodes */
                    classInfo.getMethods().put(resolvedMethod.getSignature(), methodInfo);
                }
                //toutes les methodes sont elles static
                boolean uniquementStatic =
                    classInfo.getMethods().values().stream().allMatch(m->m.isStatic());
                classInfo.setAllMethodStatic(uniquementStatic);
                
                
                //if (clazz.getNameAsString().contains("HistorisationWebService"))
                //{ System.out.println("2222=>" + clazz.getNameAsString()+ ">>>>" +classInfo.getQualifiedName());

                //}
                
                clazz.getFields().forEach(field -> {
                    FieldInfo fieldInfo = new FieldInfo();
                    fieldInfo.setType(field.getElementType().asString());
                    fieldInfo.setName(field.getVariable(0).getNameAsString());

                    field.getAnnotations().forEach(anno -> fieldInfo.getAnnotations().add(anno.toString()));
                    classInfo.getFields().put(fieldInfo.getName(), fieldInfo);

                });
               
                classes.put(classInfo.getQualifiedName(), classInfo);
    
                
            }
            catch (Exception e)
            {
                System.out.println("File " + file + " :" + e.getMessage());
            } 
            
         });
        
        
        
        
        
        
        /*
        CompilationUnit cu = StaticJavaParser.parse(file);
       

        try
        {
            ClassOrInterfaceDeclaration clazz = cu.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new FileClassException());

            if (clazz == null)
                return;
            ClassInfo classInfo = new ClassInfo();
            String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
            String className = clazz.getNameAsString();
            classInfo.setPackageName(pkg);
            classInfo.setClassName(className);
            classInfo.setQualifiedName(pkg + "." + className);
            classInfo.setFile(file);
            
          //  if (clazz.getNameAsString().contains("HistorisationWebService"))
          //  { System.out.println("OUUUUUUUU=>" + clazz.getNameAsString()+ ">>>>" +classInfo.getQualifiedName());

          //  }
            
            for (ImportDeclaration imp : cu.getImports())
            {
                String qualifiedName = imp.getNameAsString();
                String simpleName = imp.getName().getIdentifier();
                classInfo.getImports().put(simpleName, qualifiedName);
            }

           
            
            for (MethodDeclaration method : clazz.getMethods())
            {
                MethodInfo methodInfo = new MethodInfo();
                methodInfo.setName(method.getNameAsString());
                methodInfo.setReturnType(method.getType());
                // System.out.println("Declaration methode: "+ method.getTypeAsString()+":::"+method.getType());
                methodInfo.setDeclaration(method);
                //methodInfo.setCalls(method.findAll(MethodCallExpr.class));
                methodInfo.setPrivate(method.isPrivate());
                methodInfo.setParameters(method.getParameters());
                if (method.getBody().isPresent()) {
                    methodInfo.setBody(method.getBody().get().toString());
                }
                
                methodInfo.setSignature(method.getSignature().asString());
        
                               
                // la clé de la map est la signature car possibilité de redefinition de methodes 
                classInfo.getMethods().put(method.getSignature().asString(), methodInfo);
            }
            
            //if (clazz.getNameAsString().contains("HistorisationWebService"))
            //{ System.out.println("2222=>" + clazz.getNameAsString()+ ">>>>" +classInfo.getQualifiedName());

            //}
            
            clazz.getFields().forEach(field -> {
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.setType(field.getElementType().asString());
                fieldInfo.setName(field.getVariable(0).getNameAsString());

                field.getAnnotations().forEach(anno -> fieldInfo.getAnnotations().add(anno.toString()));
                classInfo.getFields().put(fieldInfo.getName(), fieldInfo);

            });
           
            classes.put(classInfo.getQualifiedName(), classInfo);

        }
        catch (Exception e)
        {
            System.out.println("File " + file + " :" + e.getMessage());
        }

         */
    }

    public void generate(File sourceFile, Path output) throws IOException
    {

        /* retrouver la classe dans la liste des classes */
        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
        ClassOrInterfaceDeclaration clazz = cu.findFirst(ClassOrInterfaceDeclaration.class)
            .orElseThrow(null);
        if (clazz == null)
            return;

        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
        String className = clazz.getNameAsString();
        ClassInfo classInfo = classes.get(pkg + "." + className);

        System.out.println("Traitement de " + pkg + "." + className);

        /* Ecriture de la classe de test */

        TypeSpec.Builder testClass = TypeSpec.classBuilder(className + "Test")
            .addModifiers(Modifier.PUBLIC);

        MockitoGenerator.addMockitoFields(classInfo, testClass);
        
        /* initialisation */
        MethodTestGenerator.init();
        
        /*ajoute la  methode setUp avec annotation BEFORE_EACH pour execution avant chaque test */
        MethodTestGenerator.generateSetup(testClass);
        
        /* ajoute les methodes de test */
        MethodTestGenerator.generateMethods(classes, classInfo, testClass);
        
        /*ajoute les mocks utilisés par les methodes de test */
        MethodTestGenerator.getListMocks().forEach(mock -> MockitoGenerator.addMockitoField(testClass, mock, classInfo));
        
        /* ecrit la classe de test*/
        Builder fileBuilder = JavaFile.builder(
            cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse(""),
            testClass.build());

        /*
         * Ajout des imports Static du genre import static org.junit.jupiter.api.Assertions.assertTrue; import static
         * org.mockito.ArgumentMachers.any; à partir de la map créé dans la classe MethodTestGenerator Remplace tous les
         * "fileBuilder.addStaticImport(Imports.ASSERTIONS, "assertTrue")" ...
         */

        Imports.getListStaticImport().forEach((imp, liste) -> {
            liste.forEach(arg -> fileBuilder.addStaticImport(imp, arg));

        });

        fileBuilder.build()
            .writeTo(output);

    }

}
