package generator.bean;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

public class MethodCallInfo
{
    private ResolvedMethodDeclaration resolvedMethodDeclaration;
    
    private MethodCallExpr methodCallExpr;

    public ResolvedMethodDeclaration getResolvedMethodDeclaration()
    {
        return resolvedMethodDeclaration;
    }

    public void setResolvedMethodDeclaration(ResolvedMethodDeclaration resolvedMethodDeclaration)
    {
        this.resolvedMethodDeclaration = resolvedMethodDeclaration;
    }

    public MethodCallExpr getMethodCallExpr()
    {
        return methodCallExpr;
    }

    public void setMethodCallExpr(MethodCallExpr methodCallExpr)
    {
        this.methodCallExpr = methodCallExpr;
    }
    
    
}
