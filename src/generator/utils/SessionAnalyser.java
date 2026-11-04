package generator.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import generator.bean.MethodCallInfo;
import generator.bean.MethodInfo;
import generator.bean.SessionAttributeInfo;

public class SessionAnalyser
{
    List<SessionAttributeInfo> sessionGetAttributes = new ArrayList<>();

    List<SessionAttributeInfo> sessionSetAttributes = new ArrayList<>();

    public Map<String, List> analyse(MethodInfo methodInfo)
    {
        Map<String, List> sessionAttributes = new HashMap<>();
        
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
                
                analyseExpression(v.getMethodCallExpr(),  methodInfo.getVariables() );
              
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
    private void analyseExpression(MethodCallExpr expr, List<VariableDeclarator> variables)
    {
       
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
        if (expr.getArgument(0).isStringLiteralExpr()) {
            sessionAttrInfo.setAttibuteName( expr.getArgument(0).asStringLiteralExpr().getValue());
        } 
        if (expr.getArgument(1).isNameExpr()) {
            // C'est un NameExpr (ex: nbrDelai)
            sessionAttrInfo.setValue( expr.getArgument(1).asNameExpr().getNameAsString());
        } else {
            // Au cas où c'est autre chose (un chiffre direct, un appel de méthode, etc.)
            sessionAttrInfo.setValue( expr.getArgument(1).toString());
        }
        //alimente le type de l'attribut qui correspond à une varaible dans la methode
        variables.stream().filter(
           v->v.getNameAsString().equals(expr.getArgument(1).asNameExpr().getNameAsString())).findAny().ifPresent(
                variable -> {
                    sessionAttrInfo.setVariableType(  variable.getTypeAsString());
                   
                }
            );
       
      
        sessionSetAttributes.add(sessionAttrInfo);
        

    }
}
