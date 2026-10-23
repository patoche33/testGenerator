package generator.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;

import generator.bean.ElementExpression;
import generator.bean.MethodInfo;

public class ParseUtils
{
    
    /**
     * 
     * Rechercher toutes es expression contenu dans une Binary expression
     * exemple avec la Binary expression : "Select * from " + table + " where id" = id
     * donne 4 expressions: "Select * from " , table , " where id" et id
     * @param expr
     * @return
     */
    public static List<ElementExpression> getAllExprInBinaryExpr(Expression expr) 
    {
        List<ElementExpression> morceaux = new ArrayList<>();
        parcourirConcaténation(expr, morceaux);
        return morceaux;
    }

    /* decomposer une expression */
    private static void parcourirConcaténation(Expression expr, List<ElementExpression> morceaux) 
    {
     // Enlever les parenthèses de protection s'il y en a (ex: (a + b)
        if (expr.isEnclosedExpr()) {
            parcourirConcaténation(expr.asEnclosedExpr().getInner(), morceaux);
        } 
        // Si c'est un "+" binaire, on continue de descendre à gauche et à droite
        else if (expr.isBinaryExpr() ) //&& expr.asBinaryExpr().getOperator() == BinaryExpr.Operator.PLUS) {
        { 
            BinaryExpr binaryExpr = expr.asBinaryExpr();
            //coté gauche
            parcourirConcaténation(binaryExpr.getLeft(), morceaux);  
            // Capturer l'opérateur au milieu
            BinaryExpr.Operator op = binaryExpr.getOperator();
            morceaux.add(new ElementExpression(op));
            
            parcourirConcaténation(binaryExpr.getRight(), morceaux); // Descend à droite
        } 
        else {
            // Si ce n'est plus un "+", on a atteint un élément final (String, Variable, etc.)
            morceaux.add(new ElementExpression(expr));
        }
    }
  
    /**
     *  
     * Rechercher si la variable est un parametre de la methode 
     *
     * @param method MethodDeclaration dans laquelle rechercher si la variable est un parametre
     * @param nameExpr variable
     * @return crai ou faux
     */
  
    public static boolean isParameterMethod(MethodInfo method, NameExpr nameExpr)
    {
        String varName = nameExpr.getNameAsString();
        return method.getParameters().stream().anyMatch(p->p.getNameAsString().equals(varName));
       
    }
    
    /**
     * 
     * Retourne la liste des valeurs pour un variable déclarée dans la methode
     * plusieurs valeurs possibles si la variable a été réassigné plusieurs fois
     *
     * @param method
     * @param nameExpr
     * @return
     */
    public static List<String> getVariableValues(MethodInfo method, NameExpr nameExpr) 
    {
        List<String> values = new ArrayList<>();
        String varName = nameExpr.getNameAsString();
     
        List<VariableDeclarator> declarations = method.getVariables();
        for (VariableDeclarator decl : declarations) 
        {
            if (decl.getNameAsString().equals(varName)) 
            {
                //System.out.println("💡 '" + varName + "' est déclarée localement.");
                //System.out.println("Type détecté : " + decl.getType());
                
                if (decl.getInitializer().isPresent())
                {
                    values.add(decl.getInitializer().get().toString());
                   
                }
               
            }
            
            //  La variable a été réassignée plus bas (ex: tableName = config.get();)
            List<AssignExpr> affectations = method.getAssignExpressions();
            for (AssignExpr assign : affectations) 
            {
                if (assign.getTarget().isNameExpr() && assign.getTarget().asNameExpr().getNameAsString().equals(varName))
                {
                    //System.out.println("💡 '" + varName + "' a reçu une affectation plus tard dans le code.");
                    //System.out.println("Nouvelle valeur affectée : " + assign.getValue());
                    values.add(assign.getValue().toString());
                }
            }

            //System.out.println(" Origine de '" + varName + "' introuvable (variable globale / champ de classe ?)");
       
        }
        return values;
    }
    
    /**
     * 
     * Mettre majuscule à la premièere lettre de la chaine
     *
     * @param s
     * @return
     */
    public static  String capitalize(String s)
    {
        return Character.toUpperCase(s.charAt(0))
            + s.substring(1);
    }
    /**
     * 
     * Retourne vrai si le type est abstrait (classe abstraite)
     *
     * @param type Ast Type
     * @return true ou false
     */
    public static  boolean isAbstract(Type type)
    {
        try
        {
            // Résoudre le type
            ResolvedType resolvedType = type.resolve();
        
            // Vérifier si c'est une référence à une classe (et non un type primitif)
            if (resolvedType.isReferenceType())
            {
                ResolvedReferenceTypeDeclaration decl = resolvedType.asReferenceType().getTypeDeclaration().get();
                // Si c'est une classe -> on vérifie la présence du modificateur "ABSTRACT"
                if (decl.isClass())
                {
                    // via AST
                    Optional<Node> astNodeOpt = decl.asClass().toAst();
                    if (astNodeOpt.isPresent() && astNodeOpt.get() instanceof ClassOrInterfaceDeclaration)
                    {
                        ClassOrInterfaceDeclaration astClass = (ClassOrInterfaceDeclaration) astNodeOpt.get();
                        return astClass.isAbstract();
                    }
                    else
                    {   //via refect
                        String qualifiedName = decl.asClass().getQualifiedName();
                        try {
                            // Charge la structure de la classe via le ClassLoader
                            Class<?> clazz = Class.forName(qualifiedName);
                            
                            // Utilisation de l'API Reflection classique de Java
                            boolean isAbstract = java.lang.reflect.Modifier.isAbstract(clazz.getModifiers());
                            
                            /*
                            if (isAbstract) {
                                System.out.println(qualifiedName + " est une classe abstraite !");
                            }
                            */
                            return isAbstract;
                        } 
                        catch (ClassNotFoundException e) {
                            // Pour les JARs/JDK externes non chargés par la JVM courante,
                                                        
                            // on demande au résolveur s'il s'agit d'une interface ou d'une classe sans constructeur accessible.
                            // Malheureusement, l'accès aux modificateurs de ResolvedClassDeclaration varie selon les versions.
                            // Si votre version ne l'a pas, on ruse en inspectant ses ancêtres ou constructeurs :
                            
                            boolean aUnConstructeurInterneUniquement = decl.getConstructors().stream()
                                    .noneMatch(c -> c.accessSpecifier().toString().equals("PUBLIC"));
                                    
                            // Une classe externe sans constructeur public est soit abstraite (ex: Calendar), 
                            // soit finale typée Factory (ex: Joda LocalDate ou Java LocalDate).
                            // Dans l'esprit d'un générateur universel, les deux nécessitent de chercher une Factory Method !
                            return aUnConstructeurInterneUniquement;
                            
                            
                            
                            
                            
                        }
                    }
                   
                   
                    
                }

            }
        }
        catch (Exception e)
        {
            // Le Symbol Solver n'a pas réussi à trouver la classe (ex: dépendance manquante)
            System.err.println("Impossible de résoudre le type : " + type.asString());
        }
        return false;
    }
    
    /**
     * 
     * retourne vrai si leAst Type est un type primitif (int) ou un Wrapper (Integer)
     *
     * @param type
     * @return
     */
    public static  Boolean isPrimitiveOrWrapper(Type type)
    {
        if (type.isPrimitiveType())
            return true;

        return switch (type.asString())
        {
            case "String", "Character" -> true;
            case "Integer", "Float", "Byte", "Short", "Long", "Double" -> true;
            case "Boolean" -> true;
            default -> false;
        };

    }
    
    /**
     * Retourne vrai si Ast type =interface List, Map,...
     *
     * @param type
     * @return string
     */
    public static  Boolean isInterfaceCollection(Type type)
    {
        //type primitif 'int, boolean, ...)
        if (type.isPrimitiveType())
        {
            return false;
        }
        //  tableau (ex: BigDecimal[]), ce n'est pas une collection
        if (type.isArrayType())
        {
            return false;
        }
        // c'est bien une classe ou interface
        if (!type.isClassOrInterfaceType())
            return false;      
        
        ClassOrInterfaceType ciType = (ClassOrInterfaceType) type;
        return switch (ciType.getName().asString())
        {
            case "List", "Map", "Set" -> true;
            default -> false;
        };

    }

    /**
     * Retourne vrai si la methode est private
     *
     * @param type
     * @return string
     */
    public static  Boolean isResolvedMethodPrivate(ResolvedMethodDeclaration resolvedCall)
    {
        try
        {

            // Vérifier si c'est un nœud issu du code source (JavaParser)
            if (resolvedCall instanceof JavaParserMethodDeclaration)
            {
                // Récupérer le vrai MethodDeclaration de l'AST
                MethodDeclaration methodDecl = ((JavaParserMethodDeclaration) resolvedCall).getWrappedNode();

                // 3. Vérifier les modificateurs de portée
                if (methodDecl.isPrivate())
                {
                   // System.out.println("La méthode est PRIVÉE !");
                    return true;
                }
                else
                {
                    //System.out.println("La méthode n'est pas privée.");
                    return false;
                }
            }
            else
            {
                // Cas d'une méthode issue d'un JAR externe / JRE (pas de nœud AST disponible)
                // On passe par le ResolvedMethodDeclaration
                if (resolvedCall.accessSpecifier() == AccessSpecifier.PRIVATE)
                {
                    //System.out.println("La méthode externe est PRIVÉE !");
                    return true;
                }
                else
                {
                    //System.out.println("La méthode externe n'est pas privée.");
                    return false;
                }
            }
        }
        catch (UnsolvedSymbolException e)
        {
            System.err.println("Impossible de résoudre la méthode. Le SymbolSolver est-il configuré ?");
            return true;
        }
    }

    /**
     * Retourne une implementation du type ex si type=List<MonObjet> cela retourne lisTMonObjet
     *
     * @param type
     * @return string
     */
    public static  Type implemetOfType(Type type)
    {
        if (!(type instanceof ClassOrInterfaceType )) {
            return type;
        }
        
        ClassOrInterfaceType ciType = (ClassOrInterfaceType) type;
        return switch (ciType.getName().asString())
        {
            case "List" -> StaticJavaParser.parseType("ArrayList");
            case "Map" -> StaticJavaParser.parseType("HashMap");
            case "Set" -> StaticJavaParser.parseType("HashSet");
            case "Deque",  "Queue" -> StaticJavaParser.parseType("ArrayDeque");
            case "SortedMap", "NavigableMap" -> StaticJavaParser.parseType("TreeMap");
            case "SortedSet", "NavigableSet" -> StaticJavaParser.parseType("TreeSet");
            default -> type;
        };

    }


   }
