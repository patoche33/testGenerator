package generator.bean;

import com.github.javaparser.ast.body.VariableDeclarator;

public class RequestAttributeInfo {
    private String variableName;
    private String variableType;
    private String requestVariable;
    private String methodCalled;        // "getParameter", "getAttribute" ou "getParameterMap"
    private String attributeOrParamName; // Le nom de la clé recherchée (ex: "niveau_strat")

    // Getters et Setters
    public String getVariableName() { return variableName; }
    public void setVariableName(String variable) { this.variableName = variable; }

    public String getVariableType() { return variableType; }
    public void setVariableType(String variableType) { this.variableType = variableType; }

    public String getRequestVariable() { return requestVariable; }
    public void setRequestVariable(String requestVariable) { this.requestVariable = requestVariable; }

    public String getMethodCalled() { return methodCalled; }
    public void setMethodCalled(String methodCalled) { this.methodCalled = methodCalled; }

    public String getAttributeOrParamName() { return attributeOrParamName; }
    public void setAttributeOrParamName(String attributeOrParamName) { this.attributeOrParamName = attributeOrParamName; }
}

