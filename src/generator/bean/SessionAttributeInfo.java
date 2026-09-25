package generator.bean;

import com.github.javaparser.ast.body.VariableDeclarator;

public class SessionAttributeInfo
{
    private String requestVariable;
    
    private String attibuteName;
    
    private String variableType;
    
    private VariableDeclarator variable;

    public String getRequestVariable()
    {
        return requestVariable;
    }

    public void setRequestVariable(String requestVariable)
    {
        this.requestVariable = requestVariable;
    }

    public String getAttibuteName()
    {
        return attibuteName;
    }

    public void setAttibuteName(String attibuteName)
    {
        this.attibuteName = attibuteName;
    }

    public String getVariableType()
    {
        return variableType;
    }

    public void setVariableType(String variableType)
    {
        this.variableType = variableType;
    }

    public VariableDeclarator getVariable()
    {
        return variable;
    }

    public void setVariable(VariableDeclarator variable)
    {
        this.variable = variable;
    }
    
    
}
