package generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
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
import generator.utils.ParseUtils;
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
        try (Stream<Path> stream = Files.walk(start))
        {
            stream.filter(f -> f.toString().endsWith(".java"))
                .forEach(f -> {
                    try
                    {
                        parse(f);
                    }
                    catch (Exception e)
                    {
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

        javaParser.parse(file).getResult().ifPresent(cu -> {
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

               
                if (className.equals("EvenementServiceImpl" ))
                  {
                      System.out.println("DEBUG CLASSE==>" + className);
                }
                ResolvedReferenceTypeDeclaration resolvedChild = clazz.resolve();
                // si c'est une classe
                if (resolvedChild.isClass()) {
                    // Récupère récursivement toutes les super-classes
                    resolvedChild.asClass().getAllAncestors().forEach(ancestor -> {
                        ResolvedReferenceTypeDeclaration resolvedParent = ancestor.getTypeDeclaration().get();
                        // On ignore la classe Object et l'interface directe implémentée (implements...)
                        // pour ne pas surcharger
                        if (!resolvedParent.getQualifiedName().equals("java.lang.Object")
                            && ! resolvedParent.isInterface())
                        {
                            extractMethods(resolvedParent, classInfo);
                        }
                    });
                } 
                else if (resolvedChild.isInterface()) {
                    // Récupère récursivement toutes les interfaces parentes
                    resolvedChild.asInterface().getAllAncestors().forEach(ancestor -> {
                        ResolvedReferenceTypeDeclaration resolvedParent = ancestor.getTypeDeclaration().get();
                        extractMethods(resolvedParent, classInfo);
                    });
                }

                for (MethodDeclaration method : clazz.getMethods())
                {
                    ResolvedMethodDeclaration resolvedMethod = method.resolve();
                    
                    if (className.equals("EvenementServiceImpl" )
                        && method.getNameAsString().equals("creerEvenement")) //RegieDao
                    {
                          System.out.println("DEBUG CLASSE==>" + className);
                    }
                    
                    MethodInfo methodInfo = new MethodInfo();
                    methodInfo.setName(method.getNameAsString());
                    methodInfo.setReturnType(method.getType());
                    methodInfo.setPrivate(method.isPrivate());
                    methodInfo.setStatic(method.isStatic());
                    methodInfo.setInherited(false);
                    methodInfo.setParameters(method.getParameters());
                    if (method.getBody().isPresent())
                    {
                        methodInfo.setBody(method.getBody().get().toString());
                    }

                    methodInfo.setVariables(method.findAll(VariableDeclarator.class));
                    methodInfo.setAssignExpressions(method.findAll(AssignExpr.class));
                    methodInfo.setThrownExceptions(method.getThrownExceptions());
                    methodInfo.setIfStmts(method.findAll(IfStmt.class));
                    methodInfo.setReturnStmts(method.findAll(ReturnStmt.class));
                    methodInfo.setComplex(ParseUtils.isMethodComplex(method));
                    boolean isGetOrSet = (ParseUtils.isSetter(method) ||  ParseUtils.isGetter(method));
                    methodInfo.setGetterOrSetter(isGetOrSet);
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
                // toutes les methodes sont elles static
                boolean uniquementStatic =
                    classInfo.getMethods().values().stream().allMatch(m -> m.isStatic());
                classInfo.setAllMethodStatic(uniquementStatic);

                // if (clazz.getNameAsString().contains("HistorisationWebService"))
                // { System.out.println("2222=>" + clazz.getNameAsString()+ ">>>>" +classInfo.getQualifiedName());

                // }
               
                List<String> props = new ArrayList<String>();
                if (xmlProperties.containsKey(clazz.getFullyQualifiedName().get()))
                {
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

        /* ajoute la methode setUp avec annotation BEFORE_EACH pour execution avant chaque test */
        methodTestGenerator.generateSetup(testClass);

        /* ajoute les methodes de test */
        methodTestGenerator.generateMethods(classes, classInfo, testClass);

        /* ajoute les mocks utilisés par les methodes de test */
        methodTestGenerator.getListMocks().forEach(mock -> MockitoGenerator.addMockitoField(testClass, mock, classInfo));

        /* ecrit la classe de test */
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
     * Méthode utilitaire factorisée pour extraire et stocker les méthodes
     */
    private void extractMethods(ResolvedReferenceTypeDeclaration resolvedParent, ClassInfo classInfo) {

        for (ResolvedMethodDeclaration resolvedMethod : resolvedParent.getDeclaredMethods()) {
            ResolvedType resolvedReturnType = resolvedMethod.getReturnType();
            Type returnType = StaticJavaParser.parseType(resolvedReturnType.describe());
            
            NodeList<Parameter> parametersList= new NodeList<Parameter>();
            for (int i = 0; i < resolvedMethod.getNumberOfParams(); i++) {
                ResolvedParameterDeclaration resolvedParam = resolvedMethod.getParam(i);
                // Conversion du type du paramètre en nœud AST Type
                Type paramType = StaticJavaParser.parseType(resolvedParam.getType().describe());
                // On récupère le nom de la variable du paramètre
                String paramName = resolvedParam.getName();
                if (paramName == null || paramName.trim().isEmpty()) {
                    // Si le nom est vide (cas du JDK / JAR compilé), on génère un nom générique : p0, p1, p2...
                    paramName = "p" + i; 
                }
                //crée un nouveau nœud AST de type Parameter(Type, String)
                Parameter astParameter = new Parameter(paramType, paramName);
                
                parametersList.add(astParameter);
            }
         
                  
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.setSignature(resolvedMethod.getSignature());
            methodInfo.setName(resolvedMethod.getName());
            methodInfo.setInherited(true);
            methodInfo.setReturnType(returnType);
            methodInfo.setParameters(parametersList);
            methodInfo.setComplex(ParseUtils.isMethodComplex(resolvedMethod));
           
            boolean isGetOrSet = (ParseUtils.isSetter(resolvedMethod) ||  ParseUtils.isGetter(resolvedMethod));
            methodInfo.setGetterOrSetter(isGetOrSet);
            // putIfAbsent évite qu'une classe grand-parente n'écrase 
            // la redéfinition d'une classe parente plus proche
            classInfo.getMethods().putIfAbsent(resolvedMethod.getSignature(), methodInfo);
        }
    }

}
