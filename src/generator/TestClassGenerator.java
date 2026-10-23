package generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
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
import generator.config.Config;
import generator.utils.XmlInjection;

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
import java.util.Set;
import java.util.stream.Stream;

public class TestClassGenerator
{
    /* Javaparser configuré avec JavaSymbolResolver */
    private static JavaParser javaParser;
    
    private Map<String, ClassInfo> classes = new HashMap<>();
    private Map<String, List<String>> xmlProperties = new HashMap<>();
    
    public TestClassGenerator(String rootProject, String xml) throws IOException
    {
        
        try
        {
           if (xml != null)
           {
              xmlProperties = XmlInjection.extraireNomsProprietesDepuisXml(xml);
           } 
        }
        catch (Exception e)
        {
            System.err
            .println("Impossible de résoudre les propriétes du fichier applicationContext-service.xml : " + e.getMessage());
        }
        
        Path start = Paths.get(rootProject);

        javaParser = ConfigSolver.getJavaParser();
        
        // Utilisation d'un try-with-resources pour fermer le flux de fichiers proprement
        try (Stream<Path> stream = Files.walk(start)) {
            stream.filter(f -> f.toString().endsWith(".java"))
                  .forEach(f -> {
                      try {
                          parse(f);
                      } catch (Exception e) {
                          // Log 
                      }
                  });
        }
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
                
                
                
                
                for (ImportDeclaration imp : cu.getImports())
                {
                    String qualifiedName = imp.getNameAsString();
                    String simpleName = imp.getName().getIdentifier();
                    classInfo.getImports().put(simpleName, qualifiedName);
                }

               if (className.equals("RegieDaoImpl"))
               {
                   System.out.println("RegieDaoImpl");
               }
                
                for (MethodDeclaration method : clazz.getMethods())
                {
                    ResolvedMethodDeclaration resolvedMethod = method.resolve();
                    
                    MethodInfo methodInfo = new MethodInfo();
                    methodInfo.setName(method.getNameAsString());
                    methodInfo.setReturnType(method.getType());
                    // System.out.println("Declaration methode: "+ method.getTypeAsString()+":::"+method.getType());
                    //methodInfo.setDeclaration(method);
                    //methodInfo.setCalls(method.findAll(MethodCallExpr.class));
                    methodInfo.setPrivate(method.isPrivate());
                    methodInfo.setStatic(method.isStatic());
                    methodInfo.setParameters(method.getParameters());
                    if (method.getBody().isPresent()) {
                        methodInfo.setBody(method.getBody().get().toString());
                    }
                    
                    methodInfo.setVariables(method.findAll(VariableDeclarator.class));
                    methodInfo.setAssignExpressions(method.findAll(AssignExpr.class));
                    methodInfo.setThrownExceptions(method.getThrownExceptions());
                    methodInfo.setIfStmts(method.findAll(IfStmt.class));
                    methodInfo.setReturnStmts(method.findAll(ReturnStmt.class));
                    methodInfo.setComplex(isMethodComplex(method));
                    
                    Map<String, MethodCallInfo> mapCalls = new HashMap<>();
                    method.findAll(MethodCallExpr.class).forEach(methodCall -> {
                        try
                        {
                            // cretion d'un nouveau MethoCallInfo
                            MethodCallInfo methodCallInfo = new MethodCallInfo();
                            // Résolution du symbole de l'appel
                            ResolvedMethodDeclaration resolved = methodCall.resolve();
                            methodCallInfo.setResolvedMethodDeclaration(resolved);
                            methodCallInfo.setMethodCallExpr(methodCall);
                          
                            mapCalls.put(resolved.getSignature(), methodCallInfo);
                            // System.out.println("-> Appel trouvé vers : " + resolved.getQualifiedName());
                        }
                        catch (Exception e)
                        {
                            System.err
                                .println("Impossible de résoudre l'appel : " + methodCall.getNameAsString() + " (" + e.getMessage() + ")");
                        }
                    });
                    methodInfo.setCalls(mapCalls);
                                      
                    
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
                if (className.equals("RegieServiceImpl"))
                {
                    System.out.println("RegieServiceImpl");
                    System.out.println(clazz.getFullyQualifiedName().get());
                } 
                List<String> props = new ArrayList<String>();
                if (xmlProperties.containsKey(clazz.getFullyQualifiedName().get())) {
                    props.addAll(xmlProperties.get(clazz.getFullyQualifiedName().get()));
                }
                
                clazz.getFields().forEach(field -> {
                    FieldInfo fieldInfo = new FieldInfo();
                    fieldInfo.setType(field.getElementType().asString());
                    fieldInfo.setName(field.getVariable(0).getNameAsString());
                    fieldInfo.setXmlInject(false);
                    if (props.contains(field.getVariable(0).getNameAsString()))
                    {
                        fieldInfo.setXmlInject(true);
                    }
                    

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
        
        
    }

    public void generate(File sourceFile, Path output) throws IOException
    {

        /* retrouver la classe dans la liste des classes */
        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
        ClassOrInterfaceDeclaration clazz = cu.findFirst(ClassOrInterfaceDeclaration.class)
            .orElse(null);
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
        MethodTestGenerator methodTestGenerator = new MethodTestGenerator();
        methodTestGenerator.init();
        
        /*ajoute la  methode setUp avec annotation BEFORE_EACH pour execution avant chaque test */
        methodTestGenerator.generateSetup(testClass);
        
        /* ajoute les methodes de test */
        methodTestGenerator.generateMethods(classes, classInfo, testClass);
        
        /*ajoute les mocks utilisés par les methodes de test */
        methodTestGenerator.getListMocks().forEach(mock -> MockitoGenerator.addMockitoField(testClass, mock, classInfo));
        
        /* ecrit la classe de test*/
        Builder fileBuilder = JavaFile.builder(
            cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse(""),
            testClass.build()).skipJavaLangImports(true);

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
    
    /**
     * Détermine si un getter ou setter contient de la logique métier ou des règles complexes.
     * 
     * @param method La déclaration de la méthode à analyser.
     * @return true si la méthode est complexe, false si elle est simple (ou vide).
     */
    private static boolean isMethodComplex(MethodDeclaration method) {
        // Si la méthode n'a pas de corps (ex: interface, méthode abstraite), elle n'est pas complexe
        if (method.getBody().isEmpty()) {
            return false;
        }

        BlockStmt body = method.getBody().get();

        //Plus d'une seule instruction de base = Logique ou effets de bord
        if (body.getStatements().size() > 1) {
            return true;
        }

        //Présence de structures conditionnelles (if, switch, guard clauses avec throw)
        boolean contientConditions = !body.findAll(IfStmt.class).isEmpty() 
                || !body.findAll(SwitchStmt.class).isEmpty()
                || !body.findAll(ThrowStmt.class).isEmpty();
        if (contientConditions) {
            return true;
        }

        // 3. Présence de boucles (for, foreach, while, do-while)
        boolean contientBoucles = !body.findAll(ForStmt.class).isEmpty() 
                || !body.findAll(ForEachStmt.class).isEmpty() 
                || !body.findAll(WhileStmt.class).isEmpty()
                || !body.findAll(DoStmt.class).isEmpty();
        if (contientBoucles) {
            return true;
        }

        // 4. Présence d'opérateurs ternaires (ex: return (val != null) ? val : "default";)
        boolean contientTernaire = !body.findAll(ConditionalExpr.class).isEmpty();
        if (contientTernaire) {
            return true;
        }

        // Si aucun critère de complexité n'est détecté, la méthode est considérée comme simple
        return false;
    }

}
