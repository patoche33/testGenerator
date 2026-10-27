package generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.squareup.javapoet.*;
import com.squareup.javapoet.MethodSpec.Builder;

import generator.bean.ClassInfo;
import generator.bean.ElementExpression;
import generator.bean.MethodCallInfo;
import generator.bean.MethodInfo;
import generator.bean.Mocks;
import generator.bean.SessionAttributeInfo;
import generator.utils.ParseUtils;
import generator.utils.SessionAnalyser;

import java.io.IOException;
import java.lang.reflect.TypeVariable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MethodTestGenerator
{

    private static String NEWT = "new $T()";

    private static String NEWPARAMETERIZEDT = "new $T<>()";

    private List<String> listMocks;

    private Map<ClassName, List<String>> listStaticImport;

    private List<String> listFields;

    /* code block declaration when donothing verify invocation */
    private List<CodeBlock> declarationBlocks;

    private List<CodeBlock> invocationBlocks;

    private List<CodeBlock> assertBlocks;

    private List<CodeBlock> verifyBlocks;

    private List<CodeBlock> mockitoBlocks;

    private List<String> commentBlocks;

    private List<String> methodGenererated;

    /* clé = fqn valeur= simple name de la variable */
    private Map<String, String> variableBlocks;

    private TypeNameFactory typeNameFactory;

    private Map<String, ClassInfo> classes;

    private ClassInfo classInfo;

    private TypeSpec.Builder builder;

    /* Javaparser configuré avec JavaSymbolResolver */
    private JavaParser javaParser;

    public void init()
    {
        listMocks = new ArrayList<String>();
        listStaticImport = new HashMap<>();
        listFields = new ArrayList<String>();
        declarationBlocks = new ArrayList<>();
        invocationBlocks = new ArrayList<>();
        assertBlocks = new ArrayList<>();
        verifyBlocks = new ArrayList<>();
        mockitoBlocks = new ArrayList<>();
        commentBlocks = new ArrayList<>();
        methodGenererated = new ArrayList<>();
        variableBlocks = new HashMap<>();
        Imports.clearListStaticImport();
    }

    public void generateSetup(TypeSpec.Builder builder)
    {

        MethodSpec setUp =
            MethodSpec.methodBuilder("setUp")
                .addAnnotation(
                    Imports.BEFORE_EACH
                // ClassName.get( "org.junit.jupiter.api", "Test")
                )
                .addModifiers(Modifier.PUBLIC)
                .addStatement("// code")
                .build();

        builder.addMethod(setUp);
    }

    public void generateMethods(Map<String, ClassInfo> pClasses, ClassInfo pClassInfo, TypeSpec.Builder pBuilder) throws IOException
    {

        classes = pClasses;
        classInfo = pClassInfo;
        builder = pBuilder;

        typeNameFactory = new TypeNameFactory(classInfo);

        // pour parser et rechercher structures particulières comme try cath, if else
        javaParser = ConfigSolver.getJavaParser();

        /* recherche des try catch */
        findExceptions();

        /*
         * Création d'un test pour chaque methode publique de la classe
         */
        for (MethodInfo methodInfo : classInfo.getMethods().values())
        {
           

            if (classInfo.getClassName().equals("EvenementServiceImpl")
                && methodInfo.getName().equals("creerEvenement"))
            {
                System.out.println("DEBUG CLASSE==>" + classInfo.getClassName() +
                    " et METHODE ===>" + methodInfo.getName());
            }
            
            //pas de tests pour methode telle que getter et setter  simple (non complexe)
            if (methodInfo.isGetterOrSetter() && ! methodInfo.isComplex())
            {
                continue;
            }
            
            System.out.println("METHODE==>" + methodInfo.getName());

           
            // pour rechercher erreur dans le code
            // getMessagerieParPeriode
          

            String nameMethod;
            Boolean surcharge;
            // pas de test pour les methodes privées
            if (methodInfo.isPrivate())
                continue;

            /* recherche de surcharge de methode et maj du nom de methode de test (avec comtpeur) si surcharge */
            if (classInfo.getMethods().values().stream().filter(m -> m.getName().equals(methodInfo.getName())).count() > 1)
            {
                long nbGenererated = methodGenererated.stream().filter(m -> m.equals(methodInfo.getName())).count();
                nameMethod = methodInfo.getName() + "_" + nbGenererated++;
                surcharge = true;
            }
            else
            {
                nameMethod = methodInfo.getName();
                surcharge = false;
            }
            /*
             * ajouter la methode dans la liste des methode générées. Permetta de verifier s'il s'agit d'une surcharge (la méthode
             * sera dèjà dans la liste
             */
            methodGenererated.add(methodInfo.getName());

            /* si la methode contient des try catch on génère une methode de test supplémentaire par exception */
            // Configuration de JavaParser avec le résolveur de symboles (requis pour lier méthode -> exception)

            methodInfo.getExceptions().forEach((k, v) -> {
                String simpleName = k.substring(k.lastIndexOf('.') + 1);
                generateMethod(methodInfo, "test" + ParseUtils.capitalize(nameMethod + "_" + simpleName), surcharge, k);
            });

            generateMethod(methodInfo, "test" + ParseUtils.capitalize(nameMethod), surcharge, "");
        }

    }

    /**
     * DOCUMENTEZ_MOI
     *
     * @param methodInfo methode à tester
     * @param testName   nom de la classe de test à creer
     * @param surcharge  booelean à vrai si surcharge
     * @param exception  nom de l'exception si il s'agit d"une methode de test d'exception, sinon chaine vide ""
     * @throws IOException
     */
    public void generateMethod(MethodInfo methodInfo, String testName, Boolean surcharge, String exception)
    {
        
        /* nom de la méthode */
        Builder methodeBuilder = MethodSpec.methodBuilder(testName);
        /*
         * Ajouter les exceptions dans la signature de la méthode de test exemple:
         * @Test void testSendMai() throws MessagingException, ConfigurationException {
         */
        if (!methodInfo.getThrownExceptions().isEmpty())
        { // exception présente dans la signature de la méthode (throws...)
          // exemple: public static String maMethode(Strin arg1, ...) throws ConfigurationException
            methodInfo.getThrownExceptions().forEach(refType -> {
                /*
                 * TypeName returnType = typeNameFactory.toTypeName((refType)); ClassName exceptionClass =
                 * ClassName.bestGuess(refType.asString()); methodeBuilder.addException(returnType);
                 */
                String fqn = refType.resolve().describe();
                methodeBuilder.addException(ClassName.bestGuess(fqn));
            });
        }
        else if (!methodInfo.getExceptions().isEmpty())
        {
            // exception non présente dans la signature de la méthode (throws...)
            // mais presence de try catch dans la méthode
            methodInfo.getExceptions().forEach((Excep, liste) -> {
                TypeName returnType = ClassName.bestGuess(Excep);
                methodeBuilder.addException(returnType);
            });
        }

        /* gestion when pour exception pouvant être déclenché dans bloc try catch du corps de la méthode à tester */
        if (!exception.isEmpty())
        {
            addWhenException(methodInfo, exception);
        }
        else
        {
            /*
             * Uniquement dans les methodes de test classique. Pas dans les test d'exceptions car déjà fait dans classique.
             * Rechercher les conditions il else
             */
            try
            {
                findIfElse(methodInfo.getIfStmts());
            }
            catch (IOException e)
            {
                System.out.println("Erreur dans le recherche des 'if then else' de la méthode à tester " + methodInfo.getName());
            }
        }

        /* gestion des annotations de la méthode */
        methodeBuilder.addAnnotation(Imports.TEST); // ClassName.get( "org.junit.jupiter.api", "Test")
        // .addModifiers(Modifier.PUBLIC).

        // Traitement des mehodes appelées dans la méthode à tester

        // System.out.println("");
        // System.out.println("***** Traitement Test " + methodInfo.getName() + " *****");
        // System.out.println("");

        Map<String, MethodCallInfo> calls = methodInfo.getCalls();
        for (Map.Entry entry : calls.entrySet())
        {
            MethodCallInfo methodCallInfo = (MethodCallInfo) entry.getValue();
            String signatureCall = methodCallInfo.getResolvedMethodDeclaration().getSignature();// (String) entry.getKey();

            MethodCallExpr call = (MethodCallExpr) methodCallInfo.getMethodCallExpr();

            /* call est une méthode de la classe à tester. */
            if (call.getScope().isEmpty())
            {
                continue;
            }

            String mock = call.getScope().get().toString(); // nom de l'instance de classe
            String methodName = call.getNameAsString(); // nom de la méthode à mocker

            /*
             * Génération du when ou doNothing si type void
             */

            // if (mock.equals("historisationwebservice"))
            // {
            // System.out.println("YYYSSSS!!!!");
            // }

            // System.out.println("Traitement méthode appelé " + mock + " . " + methodName);

            String returnValue;

            /* si la methode fait parti des declarations de la classe */
            if (classInfo.getFields().get(mock) != null)
            {
                String fqnMock = classInfo.getImports().get(classInfo.getFields().get(mock).getType());

                // System.out.println("Mock de " + mock + " de type " + fqnMock);

                /*
                 * recherche de la classe contenant la méthode appelée Si elle est trouvée on vérifie que la classe en import est en
                 * Autowired ou fait parti des propriétés (d'injection spring) du fichier spring applicationContext-service.xml 
                 * car on ne génère le "when" que pour les méthodes dont la classe est en autowired dans la classe de test
                 */

                if (classes.get(fqnMock) != null  && 
                            ( classInfo.getFields().get(mock).getAnnotations().contains("@Autowired") ||
                                classInfo.getFields().get(mock).getXmlInject()
                            
                                )
                    
                    )
                {

                    // la methode appelée est de type void . Gestion du doNoting et verify
                    if (classes.get(fqnMock).getMethods().get(signatureCall).getReturnType().isVoidType())
                    {
                        // Ajout du code block contenant doNothig.when(method(param))
                        addDoNothing(methodCallInfo);
                        // Ajout du code block contenant verify(service).method(param)
                        addVerify(methodCallInfo);
                    }
                    else
                    { /* Generation when */

                        // Ajout du code block contenant when(method(param)).thenReturn()
                        // CodeBlock.Builder block =
                        addWhen(methodCallInfo);
                    }
                }
            }
            else /*
                  * la methode ne fait pas parti des declarations de la classe. Méthode privée de la classe ou Méthode d'une autre classe
                  * (instanciée ou static)
                  */
            {
                // System.out.println("PRIVATE METHOD: " + call.getScope().get().toString() + "=>" + call.getNameAsString());

                List<MethodInfo> listMethodInfo = classInfo.getMethods().values().stream()
                    .filter(m -> m.getName().equals(call.getNameAsString())) // && m.getParameters().equals(call.getArguments())
                    .collect(Collectors.toList());

                // listMethodInfo.forEach(m -> System.out.println("PRIVATEDDDDD METHOD: " + m.getName()));
            }
        }

        /* ajout commentaire si surcharge */
        if (surcharge)
        {
            methodeBuilder.addComment("Surcharge de la méthode " + methodInfo.getSignature());
        }
        /* gestion des sessions et session.getAttribute(...) */
        buildSession(methodInfo);
        /* gestion du block contenant l'appel de la méthode et déclaration ds de paramètres de la methodes */
        if (exception.isEmpty())
        {
            buildInvocation(methodInfo);
        }
        else
        {
            buildInvocation(methodInfo, exception);
        }

        /* ecrire les commentaires */
        if (!commentBlocks.isEmpty())
        {
            commentBlocks.forEach(block -> methodeBuilder.addCode(block));
        }
        /* ecrire les declarations */
        if (!declarationBlocks.isEmpty())
        {
            declarationBlocks.forEach(block -> methodeBuilder.addStatement(block));
        }
        /* ecrire les mockito when, donothing ... */
        if (!mockitoBlocks.isEmpty())
        {
            mockitoBlocks.forEach(block -> methodeBuilder.addStatement(block));
        }
        /* ecrire l'appel de la methode à tester */
        if (!invocationBlocks.isEmpty())
        {
            invocationBlocks.forEach(block -> methodeBuilder.addStatement(block));
        }
        /* ecrire les assertions */
        if (!assertBlocks.isEmpty())
        {
            assertBlocks.forEach(block -> methodeBuilder.addStatement(block));
        }

        if (!verifyBlocks.isEmpty())
        {
            verifyBlocks.forEach(block -> methodeBuilder.addStatement(block));
        }

        MethodSpec test = methodeBuilder.build();

        builder.addMethod(test);

        /* raz des listes pour prochaine methode */
        invocationBlocks.removeAll(invocationBlocks);
        declarationBlocks.removeAll(declarationBlocks);
        mockitoBlocks.removeAll(mockitoBlocks);
        verifyBlocks.removeAll(verifyBlocks);
        assertBlocks.removeAll(assertBlocks);
        commentBlocks.removeAll(commentBlocks);
        variableBlocks.clear();
    }

    /**
     * creation du doNothing()
     *
     * @param builder
     * @param classes
     * @param classInfo
     * @param call
     */

    private void addDoNothing(MethodCallInfo methodCallInfo)
    {
        MethodCallExpr call = methodCallInfo.getMethodCallExpr();
        ResolvedMethodDeclaration resolvedMethod = methodCallInfo.getResolvedMethodDeclaration();
        String mock = call.getScope().get().toString(); // nom de l'instance de classe
        String methodName = call.getNameAsString(); // nom de la méthode à mocker
        String fqnMock = classInfo.getImports().get(classInfo.getFields().get(mock).getType());

        // ajouter dans la liste des statics import
        Imports.addStaticImport(Imports.MOCKITO, "doNothing");

        CodeBlock args = buildMatchers(classes.get(fqnMock).getMethods().get(resolvedMethod.getSignature()));

        CodeBlock.Builder block = CodeBlock.builder();
        block.add(
            "doNothing().when($L).$L($L)",
            "mock" + StringUtils.capitalize(mock),
            methodName,
            args);

        /* block when(methode(parametre).theReturn */
        mockitoBlocks.add(block.build());

    }

    /**
     * creation du doNothing()
     *
     * @param builder
     * @param classes
     * @param classInfo
     * @param call
     */

    private void addVerify(MethodCallInfo methodCallInfo)
    {
        MethodCallExpr call = methodCallInfo.getMethodCallExpr();
        ResolvedMethodDeclaration resolvedMethod = methodCallInfo.getResolvedMethodDeclaration();

        String mock = call.getScope().get().toString(); // nom de l'instance de classe
        String methodName = call.getNameAsString(); // nom de la méthode à mocker
        String fqnMock = classInfo.getImports().get(classInfo.getFields().get(mock).getType());

        // ajouter dans la liste des statics import
        Imports.addStaticImport(Imports.MOCKITO, "verify");

        CodeBlock args = buildMatchers(classes.get(fqnMock).getMethods().get(resolvedMethod.getSignature()));

        CodeBlock.Builder block = CodeBlock.builder();
        block.add(
            "verify($L).$L($L)",
            "mock" + StringUtils.capitalize(mock),
            methodName,
            args);

        /* block when(methode(parametre).theReturn */
        verifyBlocks.add(block.build());

    }

    /**
     * creation du when() pour variable de classe
     *
     * @param builder
     * @param classes
     * @param classInfo
     * @param call
     */
    private void addWhen(MethodCallInfo methodCallInfo)
    {

        MethodCallExpr call = methodCallInfo.getMethodCallExpr();
        ResolvedMethodDeclaration resolvedCall = methodCallInfo.getResolvedMethodDeclaration();
        String mock;
        String fqnMock;

        // ajouter dans la liste des statics import
        Imports.addStaticImport(Imports.MOCKITO, "when");

        // Soit le nom de l'instance de classe (pas de scope car la methode est dans la classe à tester)
        if (call.getScope().isEmpty())
        { // la classe de la méthode à tester
            mock = classInfo.getClassName();
            fqnMock = classInfo.getQualifiedName();
        }
        else // Soit le nom de l'instance d'une autre classe( presence de scope)
        {
            mock = call.getScope().get().toString();
            // la variable ne fait pas parti des variables d la clase à tester
            if (classInfo.getFields().get(mock) == null)
            {
                return;
            }
            fqnMock = classInfo.getImports().get(classInfo.getFields().get(mock).getType());
        }

        if (ParseUtils.isResolvedMethodPrivate(resolvedCall))
        {
            return;
        }

        String methodName = call.getNameAsString(); // nom de la méthode à mocker
        String methodSignature = resolvedCall.getSignature(); // signature fqn

        // Si la classe ne fait pas parti de la liste des classes à tester on quitte

        // arguments de la méthode
        CodeBlock args = buildMatchers(classes.get(fqnMock).getMethods().get(methodSignature));

        Type callType = classes.get(fqnMock).getMethods().get(methodSignature).getReturnType();

        String returnValue = defaultValue(callType);

        // System.out.println("Retourne " + returnValue);

        // classes.get(fqnMock).getMethods().get(methodSignature).getReturnType().getElementType();
        CodeBlock.Builder blockInvocation = CodeBlock.builder();
        CodeBlock.Builder blockDeclarations = CodeBlock.builder();

        // methode non static
        if (!resolvedCall.isStatic())
        {
            // methode non static
            String abbrev = abbreviate(callType);
            if (!abbrev.isEmpty())
            {

                TypeName returnType = typeNameFactory.toTypeName((callType));
                /*
                 * retourne le typname d'une implementatation ou retourne type List retournera ArrayList, Map => HashMap
                 */
                TypeName implType = typeNameFactory.toTypeName(ParseUtils.implemetOfType(callType));

                if (ParseUtils.isInterfaceCollection(callType) || returnValue.startsWith("new $T"))
                {
                    blockDeclarations.add("$T $L  = " + returnValue,
                        returnType,
                        abbrev,
                        implType);
                    returnValue = abbrev;
                    // ajout de la variable dans la map des variables de la methode
                    variableBlocks.put(returnType.toString(), abbrev);
                }

            }

            blockInvocation.add("when($L.$L($L)).thenReturn(" + returnValue + ")",
                "mock" + StringUtils.capitalize(mock),
                methodName,
                args);
        }
        else
        { // methode static => utilser MockedStatic de Mockito

            // ajouter dans la liste des statics import
            Imports.addStaticImport(Imports.MOCKITO, "mockStatic");
            blockDeclarations.add("$T<$L> $L = mockStatic($L.class)",
                ClassName.get("org.mockito", "MockedStatic"),
                resolvedCall.getClassName(),
                "mocked" + StringUtils.capitalize(mock),
                resolvedCall.getClassName());

            blockInvocation.add("mocked" + StringUtils.capitalize(mock) + ".when(() -> $L.$L($L)).thenCallRealMethod()",
                // + "thenReturn(" + returnValue + ")",
                resolvedCall.getClassName(),
                methodName,
                args);

        }

        /* block when(methode(parametre).theReturn */
        mockitoBlocks.add(blockInvocation.build());
        /* block declaration de variables utilisées en ant que paramètre de la méthode */
        if (!blockDeclarations.isEmpty())
        {
            declarationBlocks.add(blockDeclarations.build());
        }

    }

    private void addWhenException(MethodInfo methodInfo, String exception)
    {
        /* gestion when pour exception */
        CodeBlock.Builder blockInvocation = CodeBlock.builder();
        CodeBlock.Builder blockDeclaration = CodeBlock.builder();
        StringBuilder blockComment = new StringBuilder();

        if (!methodInfo.getExceptions().get(exception).isEmpty())
        {
            blockComment.append("/*\n");
            blockComment.append("ICI ajouter le code pour la gestion de l' exception " + exception + " qui peut être déclenché par:\n");

            methodInfo.getExceptions().get(exception).forEach((v) -> {
                blockComment.append("   - " + v.getResolvedMethodDeclaration().getQualifiedSignature() + "\n");
                /*
                 * cas methode d'une autre classe et methode non pas void (on ne génère pas de when pour les methodes de type void)
                 */
                if (!v.getMethodCallExpr().getScope().isEmpty()
                    &&
                    !v.getResolvedMethodDeclaration().getReturnType().describe().equals("void")
                    &&
                    !ParseUtils.isResolvedMethodPrivate(v.getResolvedMethodDeclaration())

                )
                {

                    String methodName = v.getMethodCallExpr().getNameAsString();

                    // HERE ERROR => dans le cas de parse il s'agit de la classe DateFormat (voir declaration DateFormat formatter)
                    // mock est egal à Formatter et devrait etre DateFormat mais ce est pas bon
                    // et il n'y a pas d'import Formatter

                    Expression scopeExpr = v.getMethodCallExpr().getScope().get();
                    ResolvedType resolvedType = scopeExpr.calculateResolvedType();

                    String mock = StringUtils.capitalize(v.getMethodCallExpr().getScope().get().toString());

                    // HERE ERROR car ne se trouve pas dans la liste des classes à tester
                    // car il s'agit de DateFormat squi instancier dan sla methode (DateFormat formatter = new SimpleDateFormat(format,
                    // Locale.FRENCH);

                    // Ne fait pas parti des variables de la classe , donc instancié dans la méthode.
                    // on ne fait rien
                    if (classInfo.getImports().get(mock) == null)
                    {
                        // System.out.println("HERE ERROR addWhenException =>" + mock);
                    }
                    else
                    {
                        CodeBlock args = buildMatchers(v.getMethodCallExpr());
                        String fqnMock = classInfo.getImports().get(mock);
                        String importMock = fqnMock.substring(0, fqnMock.lastIndexOf("."));
                        /* Méthode non static d'une autre classe */
                        if (!v.getResolvedMethodDeclaration().isStatic())
                        {
                            /* classe présente dans les imports et déclarations */
                            if (classInfo.getImports().get(classInfo.getFields().get(mock)) != null)
                            {
                                blockInvocation.add("when($L.$L($L)).thenThrow($L)",
                                    "mock" + mock,
                                    v.getMethodCallExpr().getNameAsString(),
                                    args,
                                    exception);
                            }

                        }
                        else
                        { /* Méthode static d'une autre classe */

                            // ajouter dans la liste des statics import
                            Imports.addStaticImport(Imports.MOCKITO, "mockStatic");
                            Imports.addStaticImport(Imports.MOCKITO, "when");

                            String mocked = "mocked" + mock;
                            blockDeclaration.add("$T<$T> $L = mockStatic($L.class)",
                                Imports.MOCKITOSTATIC,
                                ClassName.get(importMock, mock),
                                // mock,
                                mocked, mock);

                            blockInvocation.add("$L.when(()-> $L.$L($L).thenThrow(new $L(\"\"))", mocked, mock, methodName, args,
                                exception);
                        }

                    }

                }

            });
            blockComment.append("*/\n");

            commentBlocks.add(blockComment.toString());
            if (!blockDeclaration.isEmpty())
            {
                declarationBlocks.add(blockDeclaration.build());
            }
            if (!blockInvocation.isEmpty())
            {
                invocationBlocks.add(blockInvocation.build());
            }
        }

    }

    /**
     * cree le code d'invocation de la méthode
     *
     * @param methodInfo
     */
    private void buildInvocation(MethodInfo methodInfo) // MethodDeclaration method ,clazz
    {

        StringBuilder sb = new StringBuilder();

        CodeBlock.Builder blockFinal = CodeBlock.builder();
        CodeBlock.Builder blockInvocation = CodeBlock.builder();
        CodeBlock.Builder blockDeclarations = CodeBlock.builder();
        CodeBlock.Builder blockAssert = CodeBlock.builder();
        Type methodReturnType = methodInfo.getReturnType();
        if (!methodReturnType.isVoidType()) // method.getType()
        {
            blockInvocation.add(" result = ");
        }

        // si pas toutes les methodes static @injectMocks aura été créé et on l'utilise
        if (!classInfo.isAllMethodStatic())
        {
            blockInvocation.add(StringUtils.uncapitalize(classInfo.getClassName()) + "." + methodInfo.getName() + "(");

        }
        else
        {
            // toutes les methodes sont static donc pas d'@injectMocks et on utilse la classe
            blockInvocation.add(classInfo.getClassName() + "." + methodInfo.getName() + "(");
        }

//        if (methodInfo.getName().equals("datesInDaysBetween"))
//        {
//            // //isJourFerie
//            System.out.println("DEBUG");
//
//        }

        // parcours de parametres de la méthode
        for (int i = 0; i < methodInfo.getParameters().size(); i++) // method.getParameters()
        {
            // type du paramètre
            Type parameterType = methodInfo.getParameters().get(i).getType();

            if (i > 0)
                blockInvocation.add(",");

            //
            String valueParameter = defaultValue(parameterType);

            if (valueParameter.startsWith("new $T"))
            {
                String abbrev = methodInfo.getParameters().get(i).getNameAsString();

                // typeName du paramètre (pour import automatique avec $T)

                TypeName parameterTypeName = typeNameFactory.toTypeName(ParseUtils.implemetOfType(parameterType));
                TypeName returnType = typeNameFactory.toTypeName((parameterType));
                blockDeclarations.add("$T " + abbrev + " = " + defaultValue(parameterType), returnType, parameterTypeName);

                declarationBlocks.add(blockDeclarations.build());
                blockDeclarations.clear();
                blockInvocation.add(abbrev);

            }
            else if (valueParameter.startsWith("$T.$L("))
            {
                String fullyQualifiedName = parameterType.resolve().asReferenceType().getQualifiedName();
                try
                {
                    Class<?> clazz = Class.forName(fullyQualifiedName);
                    String abbrev = methodInfo.getParameters().get(i).getNameAsString();
                    TypeName returnType = typeNameFactory.toTypeName((parameterType));
                    InstanciationEngine.InstantiationStrategy strategy =
                        InstanciationEngine.strategyFactory(clazz);
                    blockDeclarations.add("$T " + abbrev + " = $T.$L()", returnType, returnType, strategy.methodName);
                    declarationBlocks.add(blockDeclarations.build());
                    blockDeclarations.clear();
                    blockInvocation.add(abbrev);
                }
                catch (ClassNotFoundException e)
                {
                    blockDeclarations.add("//");
                }

            }
            else
            {
                TypeName returnType = typeNameFactory.toTypeName((parameterType));

                if (ParseUtils.isPrimitiveOrWrapper(parameterType))
                {
                    // ecriture des parametres avec nom de la variable déclarée :mamethode(monInt ,maString ,...
                    // ajouter une déclaration de variable pour les paramèters de la méthode
                    String nameParameter = methodInfo.getParameters().get(i).getNameAsString();
                    // blockDeclarations.add("$T " + nameParameter + " = " + defaultValue(parameterType), returnType);
                    blockDeclarations.add(parameterType.asString() + " " + nameParameter + " = " + defaultValue(parameterType));

                    declarationBlocks.add(blockDeclarations.build());
                    blockDeclarations.clear();
                    // blockInvocation.add(nameParameter, returnType);
                    blockInvocation.add(nameParameter);
                }
                else
                {
                    // ecriture des parametres avec une valeur exemple :mamethode(10 ,"valeur1" ,...
                    blockInvocation.add(defaultValue(parameterType), returnType);

                }

                // methodInfo.getParameters().get(i).getType().isArrayType()

            }
        }

        blockInvocation.add(")");

        /* ecrire l'appel de la methode avec retour à tester */
        if (!methodReturnType.isVoidType())
        {
            // recherche du ou des "return ..."
            List<ReturnStmt> returnStatements = methodInfo.getReturnStmts();
            if (returnStatements.size() > 1)
            { // plusiers "return ..."
                commentBlocks.add("/*\nLa méthode contient " + returnStatements.size() + " déclarations 'return'\n");
                for (ReturnStmt returnStmt : returnStatements)
                {
                    commentBlocks.add("    " + returnStmt.getExpression().get().toString() + "\n");
                }
                commentBlocks.add("Prévoir des tests supplémentaires\n*/\n");
            }

            TypeName returnType = typeNameFactory.toTypeName(methodReturnType);
            blockFinal.add("$T" + blockInvocation.build(), returnType);

          
            // gestion assertion
            // la variable a été crée et peut etre utiisée pour assertion
            String attendu = "";
            if (variableBlocks.containsKey(returnType.toString()))
            {
                attendu = variableBlocks.get(returnType.toString());
            }
            else
            { /* creation de la variable resultat Attendu */

                // valorisation de la variable resultatAttendu
                String newValue = "";
                // plusieurs return => valorisation par defaut en fonction du type
                if (returnStatements.size() > 1)
                {
                    newValue = defaultValue(methodReturnType);
                }
                else
                { // 1 seul return => on decortique le return
                    Expression returnExpr = returnStatements.get(0).getExpression().get();
                    // Enlever les parenthèses de protection s'il y en a (ex: (a + b)
                    if (returnExpr.isEnclosedExpr())
                    {
                        returnExpr = returnExpr.asEnclosedExpr().getInner();
                    }
                    // Binary expression (exemple: return "select * from" + varTable + " where uid=" + varUid)
                    if (returnExpr.isBinaryExpr())
                    {
                        /*
                         * Lister toutes les expressions contenues dans le Binary expression (exemple: - Literral expression:
                         * "select * from" et " where uid=" - variables: varTable et varUid)
                         */

                        List<ElementExpression> expressions = ParseUtils.getAllExprInBinaryExpr(returnExpr);
                        for (int i = 0; i < expressions.size(); i++)
                        {
                            String val = "";
                            ElementExpression elmentExpr = expressions.get(i);
                            if (elmentExpr.isExpression())
                            {
                                Expression expr = elmentExpr.getExpression();
                                /* nom simple */
                                if (expr.isNameExpr())
                                {
                                    // est ce un paramètre de la méthode
                                    if (ParseUtils.isParameterMethod(methodInfo, expr.asNameExpr()))
                                    {
                                        val = expr.asNameExpr().toString();

                                    }
                                    else
                                    {
                                        // ce n'est pas un parametre de la methode
                                        List<String> values = ParseUtils.getVariableValues(methodInfo, expr.asNameExpr());
                                        if (!values.isEmpty())
                                        {
                                            val = values.get(values.size() - 1); // la dernière des valeurs
                                        }
                                    }

                                }
                                else if (expr.isStringLiteralExpr())
                                {
                                    val = "\"" + expr.asStringLiteralExpr().getValue() + "\"";
                                }
                                else if (expr.isIntegerLiteralExpr())
                                {
                                    val = expr.asIntegerLiteralExpr().getValue();
                                }
                                else if (expr.isFieldAccessExpr())
                                {
                                    // Un accès à une propriété / constante : Calendar.MINUTE
                                    FieldAccessExpr field = expr.asFieldAccessExpr();
                                    val = field.getNameAsString();
                                }
                                else if (expr.isMethodCallExpr())
                                {

                                    /* correspond au type de la méthode a tester */
                                    val = defaultValue(methodReturnType);
                                }

                            }
                            else
                            {
                                BinaryExpr.Operator op = elmentExpr.getOperateur();
                                val = " " + op.asString() + " ";
                            }

                            newValue += val;
                            /*
                             * if (i < expressions.size() - 1) { newValue += val + " + "; } else { newValue += val; }
                             */
                        }
                    }
                    // Cast (Transtypage)
                    else if (returnExpr.isCastExpr())
                    {
                        CastExpr castExpr = returnExpr.asCastExpr();
                        // System.out.println("Type du Cast détecté : " + castExpr.getType()); // "Date"

                        // Récupérer l'expression à l'intérieur du Cast
                        var innerExpr = castExpr.getExpression();
                        if (innerExpr.isMethodCallExpr())
                        {
                            MethodCallExpr methodCall = innerExpr.asMethodCallExpr();

                            newValue = defaultValue(methodReturnType);
                        }
                    }

                    /* un appel de methode */
                    else if (returnExpr.isMethodCallExpr())
                    {
                        /* géré dans addWhen car fait parti des calls */

                        /*
                         * MethodCallExpr methodCall = returnExpr.asMethodCallExpr(); // resolve pour obtenir signature de la methode
                         * ResolvedMethodDeclaration resolvedMethod = methodCall.resolve(); // signature String signature =
                         * resolvedMethod.getSignature(); if (methodInfo.getCalls().get(signature).getMethodCallExpr() != null) {
                         * MethodCallInfo methodCallInfo = methodInfo.getCalls().get(signature); if
                         * (!getListMocks().contains(classInfo.getClassName())) { getListMocks().add(classInfo.getClassName()); }
                         * addWhen(methodCallInfo); }
                         */
                        newValue = defaultValue(methodReturnType);

                    }
                    else if (returnExpr.isNameExpr()) /* un nom simple */
                    {
                        newValue = defaultValue(methodReturnType);
                    }
                    else /* autres cas */
                    {
                        newValue = defaultValue(methodReturnType);
                    }

                }

                // declaration de la variable resultatAttendu

                if (!methodReturnType.asString().toLowerCase().equals("optional") &&
                    !methodReturnType.asString().toLowerCase().equals("boolean"))
                {
                    String abbrev = "resultAttendu"; // abbreviate(methodReturnType);

                    if (newValue.startsWith("new $T"))
                    {
                        TypeName typeName = typeNameFactory.toTypeName(methodReturnType);
                        TypeName implTypeName = typeNameFactory.toTypeName(ParseUtils.implemetOfType(methodReturnType));
                        /*
                         * TypeName implTypeName = typeNameFactory.toTypeName(ParseUtils.implemetOfType(methodReturnType));
                         * blockDeclarations.add("$L " + abbrev + " = " + newValue, methodReturnType.asString(), implTypeName);
                         */
                        blockDeclarations.add("$T " + abbrev + " = " + newValue,
                            typeName,
                            implTypeName);
                    }
                    
                    else if (newValue.startsWith("$T.$L("))
                    {
                        String fullyQualifiedName = methodReturnType.resolve().asReferenceType().getQualifiedName();
                        try
                        {
                            Class<?> clazz = Class.forName(fullyQualifiedName);
                            TypeName typeName = typeNameFactory.toTypeName((methodReturnType));
                            InstanciationEngine.InstantiationStrategy strategy =
                                InstanciationEngine.strategyFactory(clazz);
                            blockDeclarations.add("$T " + abbrev + " = $T.$L()", typeName, typeName, strategy.methodName);
                            
                        }
                        catch (ClassNotFoundException e)
                        {
                            blockDeclarations.add("//");
                        }

                    }
                    
                    else
                    { // si primitive ou wrapper( int, String,...) pas besoin d'import (java.lanng est importé implicitement)
                        if (ParseUtils.isPrimitiveOrWrapper(methodReturnType))
                        {
                            blockDeclarations.add(methodReturnType.asString() + " " + abbrev + " = " + newValue);
                        }
                        else
                        {
                            blockDeclarations.add("$T " + abbrev + " = " + newValue, returnType);
                        }

                    }
                    declarationBlocks.add(blockDeclarations.build());
                    attendu = abbrev;
                }

            }

            /* Assertion */
            switch (methodReturnType.asString())
            {

                case "Optional" -> {
                    // ajouter dans la liste des statics import
                    Imports.addStaticImport(Imports.ASSERTIONS, "assertTrue");
                    blockAssert.add("assertTrue(result.isPresent())", Imports.ASSERTIONS);
                }
                case "int", "Integer", "float", "Float", "byte",
                    "Byte", "short", "Short",
                    "long", "Long", "double", "Double" -> {
                    Imports.addStaticImport(Imports.ASSERTIONS, "assertEquals");
                    blockAssert.add("assertEquals(result," + attendu + ")", Imports.ASSERTIONS);
                }

                case "boolean", "Boolean" -> {
                    Imports.addStaticImport(Imports.ASSERTIONS, "assertTrue");
                    blockAssert.add("assertTrue(result)", Imports.ASSERTIONS);
                }
                default -> {
                    Imports.addStaticImport(Imports.ASSERTIONS, "assertEquals");
                    blockAssert.add("assertEquals(result," + attendu + ")", Imports.ASSERTIONS);
                }
            }

            assertBlocks.add(blockAssert.build());

        }
        else
        { // Rien à vérifier en retour puisque la méthode est de type void . Utiliser verify si necessaire */
          // blockInvocation.add(blockInvocation);
            blockFinal.add(blockInvocation.build());
        }

        invocationBlocks.add(blockFinal.build());
    }

    /**
     * Generation du assertThrows
     *
     * @param methodInfo
     * @param exception
     */
    private void buildInvocation(MethodInfo methodInfo, String exception)
    {
        StringBuilder sb = new StringBuilder();

        CodeBlock.Builder blockInvocation = CodeBlock.builder();
        CodeBlock.Builder blockDeclarations = CodeBlock.builder();
        CodeBlock.Builder blockAssert = CodeBlock.builder();
        Type methodReturnType = methodInfo.getReturnType();
        /* nom de la methode */
        // si pas toutes les methodes static @injectMocks aura été créé et on l'utilise
        if (!classInfo.isAllMethodStatic())
        {
            blockInvocation.add(StringUtils.uncapitalize(classInfo.getClassName()) + "." + methodInfo.getName() + "(");

        }
        else
        {
            // toutes les methodes sont static donc pasd'@injectMocks et on utilse la classe
            blockInvocation.add(classInfo.getClassName() + "." + methodInfo.getName() + "(");

        }
        // parcours des parametres de la méthode
        for (int i = 0; i < methodInfo.getParameters().size(); i++) // method.getParameters()
        {
            // type du paramètre
            Type parameterType = methodInfo.getParameters().get(i).getType();

            if (i > 0)
                blockInvocation.add(",");

            String valueParameter = defaultValue(parameterType);
            if (valueParameter.startsWith("new $T"))
            {
                String abbrev = methodInfo.getParameters().get(i).getNameAsString();// abbreviate(parameterType);
                // typeName du paramètre (pour import automatique avec $T)
                TypeName parameterTypeName = typeNameFactory.toTypeName(ParseUtils.implemetOfType(parameterType));
                TypeName returnType = typeNameFactory.toTypeName((parameterType));
                blockDeclarations.add("$T " + abbrev + " = " + defaultValue(parameterType), returnType, parameterTypeName);
                declarationBlocks.add(blockDeclarations.build());
                blockDeclarations.clear();
                blockInvocation.add(abbrev);

            }
            else if (valueParameter.startsWith("$T.$L("))
            {
                String fullyQualifiedName = parameterType.resolve().asReferenceType().getQualifiedName();
                try
                {
                    Class<?> clazz = Class.forName(fullyQualifiedName);
                    String abbrev = methodInfo.getParameters().get(i).getNameAsString();
                    TypeName returnType = typeNameFactory.toTypeName((parameterType));
                    InstanciationEngine.InstantiationStrategy strategy =
                        InstanciationEngine.strategyFactory(clazz);
                    blockDeclarations.add("$T " + abbrev + " = $T.$L()", returnType, returnType, strategy.methodName);
                    declarationBlocks.add(blockDeclarations.build());
                    blockDeclarations.clear();
                    blockInvocation.add(abbrev);
                }
                catch (ClassNotFoundException e)
                {
                    blockDeclarations.add("//");
                }

            }
            else
            {
                TypeName returnType = typeNameFactory.toTypeName((parameterType));
                if (ParseUtils.isPrimitiveOrWrapper(parameterType))
                {
                    // ecriture des parametres avec nom de la variable déclarée :mamethode(monInt ,maString ,...
                    // ajouter une déclaration de variable pour les paramèters de la méthode
                    String nameParameter = methodInfo.getParameters().get(i).getNameAsString();
                    // blockDeclarations.add("$T " + nameParameter + " = " + defaultValue(parameterType), returnType);
                    blockDeclarations.add(parameterType.asString() + " " + nameParameter + " = " + defaultValue(parameterType));

                    declarationBlocks.add(blockDeclarations.build());
                    blockDeclarations.clear();

                    blockInvocation.add(nameParameter, returnType);
                }
                else
                {
                    // ecriture des parametres avec une valeur exemple :mamethode(10 ,"valeur1" ,...
                    blockInvocation.add(defaultValue(parameterType), returnType);

                }
            }
        }

        blockInvocation.add(")");

        blockAssert.add("assertThrows(" + exception + ".class, ()->");
        blockAssert.add(blockInvocation.build());
        blockAssert.add(")", Imports.ASSERTIONS);
        invocationBlocks.add(blockAssert.build());
        Imports.addStaticImport(Imports.ASSERTIONS, "assertThrows");
    }

    private String defaultValue(Type type)
    {
        String ret;

        String typeString = type.asString();
        if (ParseUtils.isAbstract(type))
        {
            return "$T.$L()";

        }
        else if (ParseUtils.isInterfaceCollection(type))
        {
            return NEWPARAMETERIZEDT;
        }
        else
        {
            switch (typeString)
            {

                case "String", "char", "Character" -> ret = "\"\"";

                case "int", "Integer", "float", "Float", "byte", "Byte", "short", "Short" -> ret = "0";

                case "long", "Long" -> ret = "0L";

                case "boolean", "Boolean" -> ret = "false";

                case "double", "Double" -> ret = "0D";

                default -> {
                    ret = getMockOrNewT(type);

                }

            }
            return ret;
        }

    }

    private void buildSession(MethodInfo methodInfo)
    {
        SessionAnalyser sessionAnalyser = new SessionAnalyser();
        List<SessionAttributeInfo> sessionAttributes = sessionAnalyser.analyse(methodInfo);

        if (!sessionAttributes.isEmpty())
        {
            if (!getListMocks().contains("HttpSession"))
            {
                getListMocks().add("HttpSession");
                // ajouter l'import (typpename le retorouvera dans les imports)
                classInfo.getImports().put("HttpSession", "jakarta.servlet.http.HttpSession");
            }
            CodeBlock.Builder block = CodeBlock.builder();
            block.add("when(mockHttpServletRequest.getSession()).thenReturn(mockHttpSession)");
            mockitoBlocks.add(block.build());
            /* ajouter l'import du when */
            Imports.addStaticImport(Imports.MOCKITO, "when");

        }
        sessionAttributes.forEach(attr -> {
            CodeBlock.Builder blockMockito = CodeBlock.builder();
            CodeBlock.Builder blockDeclarations = CodeBlock.builder();
            String value = defaultValue(attr.getVariable().getType());

            if (value.toString().startsWith("new $T"))
            {
                TypeName parameterType = typeNameFactory.toTypeName((attr.getVariable().getType()));
                String abbrev = abbreviate(attr.getVariable().getType());
                if (!abbrev.isEmpty())
                {
                    blockDeclarations.add("$T " + abbrev + " = " + value, parameterType, parameterType);
                    blockMockito.add("when(mockHttpSession.getAttribute(\"" + attr.getAttibuteName() + "\")).thenReturn(" +
                        abbrev + ")");
                    // ajout de la variable dans la map des variables de la methode
                    variableBlocks.put(parameterType.toString(), abbrev);
                }

            }
            else
            {
                blockMockito.add("when(mockHttpSession.getAttribute(\"" + attr.getAttibuteName() + "\")).thenReturn(" +
                    value + ")");
            }

            mockitoBlocks.add(blockMockito.build());
            if (!blockDeclarations.isEmpty())
            {
                declarationBlocks.add(blockDeclarations.build());
            }

        });

    }

    private CodeBlock buildMatchers(MethodInfo call)
    {
        CodeBlock.Builder block = CodeBlock.builder();

        for (int i = 0; i < call.getParameters().size(); i++)
        {
            if (i > 0)
                block.add(", ");
            block.add("$L", matcher(call.getParameters().get(i).getTypeAsString()));
        }
        return block.build();

    }

    private CodeBlock buildMatchers(MethodCallExpr call)
    {
        CodeBlock.Builder block = CodeBlock.builder();

        for (int i = 0; i < call.getArguments().size(); i++)
        {
            if (i > 0)
                block.add(", ");
            block.add("$L", matcher(call.getArgument(i).toString()));
        }
        return block.build();

    }

    private String matcher(String e)
    {

        String ret = switch (e.split("<")[0].trim())
        {
            case "String" -> "anyString";
            case "char", "Character" -> "anyChar";
            case "int", "Integer" -> "anyInt";
            case "long", "Long" -> "anyLong";
            case "double", "Double" -> "anyDouble";
            case "boolean", "Boolean" -> "anyBoolean";
            case "byte", "Byte" -> "anyByte";
            case "short", "Short" -> "anyShor";
            case "float", "Float" -> "anyFloat";
            case "List" -> "anyList";
            case "Map" -> "anyMap";
            case "Set" -> "anySet";
            case "Collection" -> "anyCollection";
            case "Iterable" -> "anyIterable";
            default -> "any";
        };

        Imports.addStaticImport(Imports.ARG_MATCHERS, ret);
        return ret + "()";

    }

    /**
     * Retourne un nom de variable en fonction du type ex si type=List<MonObjet> cela retourne lisTMonObjet
     *
     * @param type
     * @return string
     */
    private String abbreviate(Type type)
    {
        if (type instanceof ClassOrInterfaceType)
        {
            ClassOrInterfaceType ciType = (ClassOrInterfaceType) type;
            Optional<NodeList<Type>> args = ciType.getTypeArguments();
            if (args.isPresent())
            {
                return StringUtils.uncapitalize(ciType.getName().asString()) + args.get().get(0);
            }
            // return "";
            /*
             * args.ifPresent(typeList -> { System.out.println("ARGGGG pricipal " + ciType.getName()); for (Type t : typeList) {
             * System.out.println("ARGGGG " +t); } });
             */
        }

        return StringUtils.uncapitalize(type.toString());// StringUtils.abbreviate(str, 10);
    }

    public String getMockOrNewT(Type type)
    {
        String typeString = type.asString();
        // Soit on utilise un mock déclaré soit on instancie une classe
        if (Mocks.getListmocks().containsKey(typeString))
        {
            // import du mock
            String importMock = Mocks.getListmocks().get(typeString);

            if (!getListMocks().contains(typeString))
            {
                getListMocks().add(typeString);
                // ajouter dans les imports si necessaire
                if (!classInfo.getImports().containsKey(importMock))
                {
                    classInfo.getImports().put(typeString, importMock);
                }
            }
            return "mock" + typeString;
        }
        else
        {
            return NEWT;
        }

    }

    private void findIfElse(List<IfStmt> ifs) throws IOException
    {
        StringBuilder blockComment = new StringBuilder();

        if (!ifs.isEmpty())
        {
            blockComment.append("/*\n Prévoir un test pour les conditions suivantes: \n");
            ifs.forEach(ifStmt -> {
                blockComment.append("    ->  " + ifStmt.getCondition() + "\n");

                // parseCondition(ifStmt.getCondition());

                if (ifStmt.hasElseBranch())
                {
                    // System.out.println("Else : " + ifStmt.getElseStmt().get());
                }
            });
            blockComment.append("*/\n");
            commentBlocks.add(blockComment.toString());
        }

    }

    /**
     * DOCUMENTEZ_MOI
     *
     * @param pMethod Methode pour laquelle on recherche les calls
     * @return la map des calls clé= signature du call , valeur=call
     * @throws IOException
     */
    private Map<String, MethodCallInfo> findCalls(MethodDeclaration pMethod) throws IOException
    {
        Map<String, MethodCallInfo> mapCalls = new HashMap<>();

        // System.out.println("File:" + classInfo.getFile().toString());

        javaParser.parse(classInfo.getFile()).getResult().ifPresent(cu -> {
            cu.findFirst(MethodDeclaration.class, m -> m.getSignature().equals(pMethod.getSignature())).ifPresent(md -> {
                md.findAll(MethodCallExpr.class).forEach(methodCall -> {
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
            });

        });
        return mapCalls;
    }

    private void findExceptions() throws IOException
    {

        // System.out.println("FIND EXCEPTION File:" + classInfo.getFile().toString());

        javaParser.parse(classInfo.getFile()).getResult().ifPresent(cu -> {

            List<TryStmt> tryStatements = cu.findAll(TryStmt.class);
            if (tryStatements.isEmpty())
            {
                return;
            }

            tryStatements.forEach(tryStmt -> {

                // Trouver la VRAIE méthode parente du bloc try courant
                Optional<MethodDeclaration> parentMethodOpt = tryStmt.findAncestor(MethodDeclaration.class);
                if (!parentMethodOpt.isPresent())
                    return;
                MethodDeclaration targetParentMethod = parentMethodOpt.get();

                /*
                 * retrouver toutes les méthodes dans le bloc try et Filtrer les appels de méthodes : // on ignore ceux enfermés dans
                 * une classe anonyme ou un lambda
                 */
                List<MethodCallExpr> callsInTry = tryStmt.getTryBlock().findAll(MethodCallExpr.class, call -> {
                    // Le call doit avoir EXACTEMENT la même méthode parente directe que le bloc try
                    return call.findAncestor(MethodDeclaration.class)
                        .map(ancestor -> ancestor.equals(targetParentMethod))
                        .orElse(false);
                });

                // callsInTry.forEach(m->System.out.println(m.getNameAsString()));

                tryStmt.getCatchClauses().forEach(catchClause -> {
                    // retrouver l'exception
                    String nameException = catchClause.getParameter().getType().asString();
                    // pour chaque methode dans le bloc try on vérifie si celle ci lève l'exception
                    callsInTry.forEach(call -> {
                        try
                        {
                            ResolvedMethodDeclaration resolved = call.resolve();
                            for (int i = 0; i < resolved.getNumberOfSpecifiedExceptions(); i++)
                            {
                                ResolvedType exceptionType = resolved.getSpecifiedException(i);
                                // Vérifie si le nom simple ou complet correspond
                                if (exceptionType.describe().endsWith(nameException))
                                {

                                    // System.out.println("Declencheur:" + call.getNameAsString() + "/" + resolved.getSignature()
                                    // + " de l'exception " + nameException);

                                    // Retrouver la méthode parente contenant ce try-catch

                                    // tryStmt.findAncestor(MethodDeclaration.class).ifPresent(methodParent -> {
                                    // méthode parente
                                    // String nomMethodeParente = methodParent.getNameAsString();
                                    ResolvedMethodDeclaration resolvedParent = targetParentMethod.resolve();

                                    MethodCallInfo methodCallInfo = new MethodCallInfo();
                                    methodCallInfo.setMethodCallExpr(call);
                                    methodCallInfo.setResolvedMethodDeclaration(resolved);
                                    /*
                                     * maj de la methodInfo contenant des exceptions la map des exceptions contient l'exception en clé et la
                                     * méthode du try déclenchant l'exception
                                     */
                                    if (classInfo.getMethods()
                                        // .get(methodParent.getSignature().asString())
                                        .get(resolvedParent.getSignature())
                                        .getExceptions().isEmpty())
                                    {
                                        // List<ResolvedMethodDeclaration> listExceptions = new ArrayList<>();
                                        List<MethodCallInfo> listExceptions = new ArrayList<>();

                                        // listExceptions.add(resolved);
                                        listExceptions.add(methodCallInfo);

                                        classInfo.getMethods()
                                            .get(resolvedParent.getSignature())
                                            .getExceptions().put(exceptionType.describe(), listExceptions);
                                    }
                                    else
                                    {
                                        classInfo.getMethods()
                                            .get(resolvedParent.getSignature())
                                            .getExceptions().get(exceptionType.describe())
                                            // .add(resolved);
                                            .add(methodCallInfo);
                                    }

                                    // });

                                }
                            }
                        }
                        catch (Exception e)
                        {
                            System.out
                                .println("Échec de résolution (classe manquante dans le typeSolver) pour " + call.getNameAsString());
                        }

                    });

                });

            });

        });
    }

    public List<String> getListMocks()
    {
        return listMocks;
    }

    public void parseCondition(Expression condition)
    {
        // Supprime les parenthèses superflues s'il y en a
        if (condition instanceof EnclosedExpr enclosed)
        {
            parseCondition(enclosed.getInner());
            return;
        }

        // System.out.println("Type d'expression : " + condition.getClass().getSimpleName());

        switch (condition.getClass().getSimpleName())
        {
            case "BinaryExpr" -> {
                System.out.println("  -> Opérateur : " + condition.asBinaryExpr().getOperator());
                System.out.println("  -> Partie Gauche : " + condition.asBinaryExpr().getLeft());
                System.out.println("  -> Partie Droite : " + condition.asBinaryExpr().getRight());

                // On peut décortiquer récursivement les deux côtés
                parseCondition(condition.asBinaryExpr().getLeft());
                parseCondition(condition.asBinaryExpr().getRight());
            }
            case "UnaryExpr" -> {
                System.out.println("  -> Opérateur unaire : " + condition.asUnaryExpr().getOperator());
                System.out.println("  -> Expression modifiée : " + condition.asUnaryExpr().getExpression());
            }
            case "MethodCallExpr" -> {
                System.out.println("  -> Appel de méthode : " + condition.asMethodCallExpr().getNameAsString());
                System.out.println("  -> Sur l'objet : " + condition.asMethodCallExpr().getScope().orElse(null));
                System.out.println("  -> Arguments passés : " + condition.asMethodCallExpr().getArguments());
            }
            case "NameExpr" -> {
                System.out.println("  -> Variable booléenne directe : " + condition.asNameExpr().getNameAsString());
            }
            case "StringLiteralExpr" -> {
                System.out.println("  -> Littérale: " + condition.asLiteralExpr().toString());
            }
            default -> {
                System.out.println("  -> Autre type d'expression : " + condition.toString());
            }
        }

    }

    /**
     * décortiquer le type de variable
     *
     * @param type
     */
    // private void visitType(Type type)
    // {
    // // String code = "class Test { " + type.asString() + " verTest; }";
    // String code = "class Test { Map<String, List<Map<String, string>>> verTest; }";
    // CompilationUnit cu = StaticJavaParser.parse(code);
    //
    // cu.accept(new ModifierVisitor<Void>()
    // {
    // @Override
    // public Type visit(ClassOrInterfaceType n, Void arg)
    // {
    // n.getTypeArguments().ifPresent(typeArgs -> {
    // System.out.println("Type principal : " + n.getNameAsString());
    // System.out.println("Nombre d'arguments : " + typeArgs.size());
    //
    // // Parcourir chaque argument de type
    // for (Type t : typeArgs)
    // {
    // System.out.println(" -> Argument trouvé : " + t.toString());
    // }
    // });
    // return (Type) super.visit(n, arg);
    // }
    // }, null);
    // }

    /*
     * ArgumentCaptor<HistoParamDesStrates> histoCaptor = ArgumentCaptor.forClass(HistoParamDesStrates.class);
     * verify(mockStrateswebdao).nbrTitresRar(histoCaptor.capture()); HistoParamDesStrates histoConstruit =
     * histoCaptor.getValue(); assertEquals("", histoConstruit.getNiveau()); assertEquals("", histoConstruit.getEntite());
     */

}
