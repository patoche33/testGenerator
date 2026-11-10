package fr.testgenerator.bean;

import com.github.javaparser.ast.body.VariableDeclarator;

public class SessionAttributeInfo
{
    private String requestVariable;
    
    private String attibuteName;
    
    private String variableType;
    
    private VariableDeclarator variable;
    
    private String value;
    
    private boolean IsLitteralValue;

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

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public boolean isIsLitteralValue()
    {
        return IsLitteralValue;
    }

    public void setIsLitteralValue(boolean isLitteralValue)
    {
        IsLitteralValue = isLitteralValue;
    }

    
    
    
}
