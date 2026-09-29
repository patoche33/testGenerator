package generator.bean;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;

public class ElementExpression {
    public enum TypeElement { OPERANDE, OPERATEUR }
    
    private final TypeElement type;
    private final Expression expression; // Rempli si OPERANDE
    private final BinaryExpr.Operator operateur; // Rempli si OPERATEUR

    public ElementExpression(Expression expression) {
        this.type = TypeElement.OPERANDE;
        this.expression = expression;
        this.operateur = null;
    }

    public ElementExpression(BinaryExpr.Operator operateur) {
        this.type = TypeElement.OPERATEUR;
        this.expression = null;
        this.operateur = operateur;
    }

    // Getters...
    public TypeElement getType()
    { 
        return type; 
    }
    
    public Expression getExpression() 
    { 
        return expression;
    }
    
    public BinaryExpr.Operator getOperateur()
    { 
        return operateur;
    }
    
    public boolean isExpression()
    {
        return (expression != null);
    }
    
    public boolean isOperator()
    {
        return (operateur != null);
    }
}
