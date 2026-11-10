package fr.testgenerator.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import fr.testgenerator.bean.ClassInfo;
import fr.testgenerator.bean.FieldInfo;
import fr.testgenerator.bean.MethodCallInfo;
import fr.testgenerator.bean.MethodInfo;
import fr.testgenerator.bean.SessionAttributeInfo;

public class SessionAnalyser
{
    List<SessionAttributeInfo> sessionGetAttributes = new ArrayList<>();

    List<SessionAttributeInfo> sessionSetAttributes = new ArrayList<>();

    public Map<String, List<SessionAttributeInfo>> analyse(MethodInfo methodInfo)
    {
        Map<String, List<SessionAttributeInfo>> sessionAttributes = new HashMap<>();
        
      //gestion des getAttribute
        for (VariableDeclarator var : methodInfo.getVariables())
        {
            var.getInitializer().ifPresent(expr -> {
                analyseExpression(expr, var);
            });

        }
        sessionAttributes.put("get", sessionGetAttributes);
        
        //gestion des setAttribute
        Map<String, List<MethodCallInfo>> ListCalls = methodInfo.getCalls();
        ListCalls.forEach((k, list)->{
            list.forEach((v)->{
                
                analyseExpression(v.getMethodCallExpr(),  methodInfo);
              
            });
        });
        
        
        sessionAttributes.put("set", sessionSetAttributes);
        return sessionAttributes;

    }
    
    //gestion des getAttribute
    private void analyseExpression(Expression expr, VariableDeclarator var)
    {
        if (expr.isCastExpr())
        {
            analyseExpression(expr.asCastExpr().getExpression(), var);
        }

        if (!expr.isMethodCallExpr())
            return;

        MethodCallExpr call = expr.asMethodCallExpr();

        if (!"getAttribute".equals(call.getNameAsString()) )
            return;

        if (call.getArguments().isEmpty())
            return;

        Expression scope = call.getScope().orElse(null);

        if (!(scope instanceof MethodCallExpr))
            return;

        MethodCallExpr sessionCall = (MethodCallExpr) scope;

        if (!"getSession".equals(sessionCall.getNameAsString()) )
            return;

        SessionAttributeInfo sessionAttrInfo = new SessionAttributeInfo();
        sessionAttrInfo.setVariable(var);
        sessionAttrInfo.setVariableType(var.getTypeAsString());
        sessionAttrInfo.setRequestVariable(sessionCall.getScope().map(Expression::toString).orElse("request"));
        Expression attribute = call.getArgument(0);
        if (attribute.isStringLiteralExpr())
        {
            sessionAttrInfo.setAttibuteName(attribute.asStringLiteralExpr().asString());
        }
      
        sessionGetAttributes.add(sessionAttrInfo);
        

    }
    
    //gestion des setAttribute
    private void analyseExpression(MethodCallExpr expr, MethodInfo methodInfo)
    {
       
        List<VariableDeclarator> variables = methodInfo.getVariables();
        
        if (!"setAttribute".equals(expr.getNameAsString()) )
            return;

        if (expr.getArguments().isEmpty())
            return;

        Expression scope = expr.getScope().orElse(null);

        if (!(scope instanceof MethodCallExpr))
            return;

        MethodCallExpr sessionCall = (MethodCallExpr) scope;

        if (!"getSession".equals(sessionCall.getNameAsString()) )
            return;

        
        SessionAttributeInfo sessionAttrInfo = new SessionAttributeInfo();
        sessionAttrInfo.setRequestVariable(sessionCall.getScope().map(Expression::toString).orElse("request"));
        //gestion de la clé (nom de l'attribut)
        if (expr.getArgument(0).isStringLiteralExpr()) {
            sessionAttrInfo.setAttibuteName( expr.getArgument(0).asStringLiteralExpr().getValue());
           
        } 
        //gestion de la valeur associé à la clé
        Expression valueExpr = expr.getArgument(1);
        if (valueExpr.isNameExpr()) {
            // C'est un NameExpr (ex: nbrDelai)
            sessionAttrInfo.setValue( expr.getArgument(1).asNameExpr().getNameAsString());
            sessionAttrInfo.setIsLitteralValue(true);
            
            //alimente le type de l'attribut qui correspond à une variable dans la methode
           variables.stream().filter(
                 v->v.getNameAsString().equals(expr.getArgument(1).asNameExpr().getNameAsString()))
                 .findAny()
                 .ifPresentOrElse(
                    variable -> sessionAttrInfo.setVariableType(  variable.getTypeAsString()), 
                    () -> {
                         // Si pas trouvé dans les variables locales, on tente la résolution dynamique (ex: champ de classe)
                        try
                        {
                            String resolvedType = valueExpr.calculateResolvedType().describe();
                            sessionAttrInfo.setVariableType(resolvedType);
                        } 
                        catch (Exception e) {
                            sessionAttrInfo.setVariableType("Unknown");
                        }
                    }
            );
            
        } 
        else {
            // Au cas où c'est autre chose (un chiffre direct, un appel de méthode, etc.)
            sessionAttrInfo.setValue( expr.getArgument(1).toString());
            sessionAttrInfo.setIsLitteralValue(false);
            try {
                // Tente de résoudre le type dynamiquement grâce au Symbol Solver
                // calculateResolvedType() renvoie un ResolvedType, .describe() donne son nom complet (ex: "java.lang.String")
                String resolvedType = valueExpr.calculateResolvedType().describe();
                sessionAttrInfo.setVariableType(resolvedType);
            } 
            catch (Exception e) {
                // Si le SymbolSolver n'est pas configuré ou ne trouve pas le type (ex: classe manquante)
                sessionAttrInfo.setVariableType("Unknown");
            }
        }
        
       
      
        sessionSetAttributes.add(sessionAttrInfo);
        

    }
}
