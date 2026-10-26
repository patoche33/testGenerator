package generator.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

public class MethodInfo {

    private String name;

    private Type returnType;
    
    private String signature;

    private MethodDeclaration declaration;
    
    private String body;
    
    private  Map<String,MethodCallInfo> calls;
    
    private  NodeList<Parameter> parameters;
    
    private boolean isPrivate;
    
    private boolean isStatic;
    
    private boolean isComplex;
    
    private boolean isInherited;
   
    private List<VariableDeclarator> variables = new ArrayList<>();
    private List<AssignExpr> assignExpressions = new ArrayList<>();
    private List<IfStmt> ifStmts = new ArrayList<>();
    private List<ReturnStmt> returnStmts = new ArrayList<>();

    private NodeList<ReferenceType> thrownExceptions;
    
    private Map<String, List<MethodCallInfo>> exceptions;
    
   
    public MethodInfo()
    {
       
        exceptions = new HashMap<String,  List<MethodCallInfo>>();
        
    }

    public String getSignature()
    {
        return signature;
    }

    public void setSignature(String signature)
    {
        this.signature = signature;
    }
    
    
    public NodeList<Parameter> getParameters()
    {
        return parameters;
    }

    public void setParameters(NodeList<Parameter> parameters)
    {
        this.parameters = parameters;
    }

    public boolean isPrivate()
    {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate)
    {
        this.isPrivate = isPrivate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public MethodDeclaration getDeclaration() {
        return declaration;
    }

    public void setDeclaration(MethodDeclaration declaration) {
        this.declaration = declaration;
    }

    public  Map<String,MethodCallInfo> getCalls()
    {
        return calls;
    }

    public void setCalls( Map<String,MethodCallInfo>calls)
    {
        this.calls = calls;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public Map<String, List<MethodCallInfo>> getExceptions()
    {
        return exceptions;
    }

    public void setExceptions(Map<String, List<MethodCallInfo>> exceptions)
    {
        this.exceptions = exceptions;
    }

    public boolean isStatic()
    {
        return isStatic;
    }

    public void setStatic(boolean isStatic)
    {
        this.isStatic = isStatic;
    }

    public List<VariableDeclarator> getVariables()
    {
        return variables;
    }

    public void setVariables(List<VariableDeclarator> variables)
    {
        this.variables = variables;
    }

    public List<AssignExpr> getAssignExpressions()
    {
        return assignExpressions;
    }

    public void setAssignExpressions(List<AssignExpr> assignExpressions)
    {
        this.assignExpressions = assignExpressions;
    }

    public List<IfStmt> getIfStmts()
    {
        return ifStmts;
    }

    public void setIfStmts(List<IfStmt> ifStmts)
    {
        this.ifStmts = ifStmts;
    }

    public NodeList<ReferenceType> getThrownExceptions()
    {
        return thrownExceptions;
    }

    public void setThrownExceptions(NodeList<ReferenceType> thrownExceptions)
    {
        this.thrownExceptions = thrownExceptions;
    }

    public List<ReturnStmt> getReturnStmts()
    {
        return returnStmts;
    }

    public void setReturnStmts(List<ReturnStmt> returnStmts)
    {
        this.returnStmts = returnStmts;
    }

    public boolean isComplex()
    {
        return isComplex;
    }

    public void setComplex(boolean isComplex)
    {
        this.isComplex = isComplex;
    }

    public boolean isInherited()
    {
        return isInherited;
    }

    public void setInherited(boolean isInherited)
    {
        this.isInherited = isInherited;
    }
    
}