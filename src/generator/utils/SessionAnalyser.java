package generator.utils;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import generator.bean.MethodInfo;
import generator.bean.SessionAttributeInfo;

public class SessionAnalyser
{
    public List<SessionAttributeInfo> analyse(MethodInfo methodInfo)
    {
        List<SessionAttributeInfo> sessionAttributes = new ArrayList<>();
                
        for (VariableDeclarator var : methodInfo.getVariables())
        {
            var.getInitializer().ifPresent(expr ->{
                analyseExpression(expr, var, sessionAttributes);
            });
        
        }
        return sessionAttributes;
        
     }
    
    private void  analyseExpression(Expression expr, VariableDeclarator var,  List<SessionAttributeInfo> sessionAttributes)
    {
        if (expr.isCastExpr())
        {
            analyseExpression(expr.asCastExpr().getExpression(), var, sessionAttributes);
        }
        
        if (!expr.isMethodCallExpr())
                return;
        
        MethodCallExpr call = expr.asMethodCallExpr();
        
        if (! "getAttribute".equals(call.getNameAsString()))
            return;
        
        if (call.getArguments().isEmpty())
            return;
        
        Expression scope = call.getScope().orElse(null);
        
        if (!(scope instanceof MethodCallExpr))
            return;
        
        MethodCallExpr sessionCall = (MethodCallExpr) scope;
        
        if (! "getSession".equals(sessionCall.getNameAsString()))
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
        sessionAttributes.add(sessionAttrInfo);
    }
}
