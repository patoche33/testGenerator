package generator.utils;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import generator.bean.MethodInfo;
import generator.bean.RequestAttributeInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RequestAnalyser {

 // Permet de stocker les noms des variables qui contiennent req.getParameterMap()
    private Set<String> parameterMapVariables = new HashSet<>();
    
    public List<RequestAttributeInfo> analyse(MethodInfo methodInfo) {
        List<RequestAttributeInfo> requestAttributes = new ArrayList<>();
        //parcours des variables déclares dans la méthode
        for (VariableDeclarator var : methodInfo.getVariables()) {
            var.getInitializer().ifPresent(expr -> {
                analyseExpression(expr, var.getNameAsString(), var.getTypeAsString(), requestAttributes);
            });
        }
        //parcours des assignations de variables non déclarées dans la méthode
        for (AssignExpr expr : methodInfo.getAssignExpressions()) {
           if ( !methodInfo.getVariables().stream().map(v->v.getNameAsString()).toList().contains(expr.getTarget().toString()))
           {
               analyseExpression(expr.getValue(), expr.getTarget().toString(), "Map",requestAttributes);
           }
        }
        
      //trairement parameterMapVariables
        List<Expression> listMethodCall = methodInfo.getExpressions().stream().filter(e->e.isMethodCallExpr()).collect(Collectors.toList());
        
        for (String paramVar : parameterMapVariables)
        {
           
                
            
           listMethodCall.forEach((v)->{
                 if (v.asMethodCallExpr().getNameAsString().equals("get") &&
                     v.asMethodCallExpr().getScope().get().toString().equals(paramVar))
                 {
                    
                     RequestAttributeInfo info = new RequestAttributeInfo();
                     info.setVariableName( v.asMethodCallExpr().getArgument(0).toString());
                     info.setMethodCalled("get");
                     info.setRequestVariable(paramVar);
                     requestAttributes.add(info);
                 }
                 
            });
        }
        
        return requestAttributes;
    }
    
    private void analyseExpression(Expression expr, String varName,String varType, List<RequestAttributeInfo> requestAttributes) {
        // Gestion récursive des Casts (ex: (String) req.getParameter(...))
        if (expr.isCastExpr()) {
            analyseExpression(expr.asCastExpr().getExpression(), varName, varType, requestAttributes);
            return; // On s'arrête ici pour cette itération car la récursion gère la suite
        }
        //Gestion des expressions du type "req.getParameterMap().get("localniveau2")[0]"
        if (expr.isArrayAccessExpr()) {
            analyseExpression(expr.asArrayAccessExpr().getName(), varName, varType, requestAttributes);
            return;
        }
        
        if (!expr.isMethodCallExpr()) {
            return;
        }
        
        //expressio du type "req.getParameter("valeur");
        MethodCallExpr call = expr.asMethodCallExpr();
        String methodName = call.getNameAsString();
        String scopeName = call.getScope().map(Expression::toString).orElse("");
        
     // Vérifications des deux scénarios cibles
        boolean isDirectRequestCall = "getParameter".equals(methodName) || 
                                     //"getAttribute".equals(methodName) || 
                                     "getParameterMap".equals(methodName);
                                     
        boolean isMapGetCall = "get".equals(methodName) && parameterMapVariables.contains(scopeName);

        if (!isDirectRequestCall && !isMapGetCall) {
         // Si la méthode possède au moins un argument, on analyse le premier argument (le flux de données)
            if (!call.getArguments().isEmpty()) {
                analyseExpression(call.getArgument(0), varName, varType, requestAttributes);
            }
            return;
        }

        if (isDirectRequestCall && "getParameterMap".equals(methodName))
        {
            parameterMapVariables.add(varName);
            return;
        }
        
        RequestAttributeInfo info = new RequestAttributeInfo();
        info.setVariableName(varName);
        info.setVariableType(varType);
        if (isDirectRequestCall) {
            info.setMethodCalled(methodName);
            info.setRequestVariable(scopeName.isEmpty() ? "request" : scopeName);
            
            // Si c'est getParameterMap, on retient le nom de l'attribut (ex: paramReq) pour les futures lignes
            if ("getParameterMap".equals(methodName)) {
                parameterMapVariables.add(varName);
                info.setAttributeOrParamName(null);
            } else if (!call.getArguments().isEmpty()) {
                info.setAttributeOrParamName(getArgumentValue(call.getArgument(0)));
            }
        } 
        else if (isMapGetCall) {
            // On simule un getParameter classique pour le résultat final
            info.setMethodCalled("getParameter (via Map)");
            info.setRequestVariable(scopeName); 
            if (!call.getArguments().isEmpty()) {
                info.setAttributeOrParamName(getArgumentValue(call.getArgument(0)));
            }
        }
        
        requestAttributes.add(info);
    }
    
    private String getArgumentValue(Expression argument) {
        return argument.isStringLiteralExpr() 
            ? argument.asStringLiteralExpr().getValue() 
            : argument.toString();
    }

}
