package generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;

import generator.bean.ElementExpression;

public class Utils
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
  
    public static boolean isParameterMethod(MethodDeclaration method, NameExpr nameExpr)
    {
        String varName = nameExpr.getNameAsString();
        Optional<Parameter> param = method.getParameterByName(varName);
        if (param.isPresent()) {
            return true;
        }
        return false;
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
    public static List<String> getVariableValues(MethodDeclaration method, NameExpr nameExpr) 
    {
        List<String> values = new ArrayList<>();
        String varName = nameExpr.getNameAsString();
     
        List<VariableDeclarator> declarations = method.findAll(VariableDeclarator.class);
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
            List<AssignExpr> affectations = method.findAll(AssignExpr.class);
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
}
