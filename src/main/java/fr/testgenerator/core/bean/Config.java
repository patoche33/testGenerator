package fr.testgenerator.core.bean;

import java.util.ArrayList;
import java.util.List;

public class Config
{
    private  List<String> sourceDependencies = new ArrayList<>();

    private  String rootProjet;

    private  String rootClasses;

    private  String output;
    
    private  String xmlInjection = "";

    private  List<String> classes = new ArrayList<>();

    public List<String> getSourceDependencies()
    {
        return sourceDependencies;
    }

    public void setSourceDependencies(List<String> sourceDependencies)
    {
        this.sourceDependencies = sourceDependencies;
    }

    public String getRootProjet()
    {
        return rootProjet;
    }

    public void setRootProjet(String rootProjet)
    {
        this.rootProjet = rootProjet;
    }

    public String getRootClasses()
    {
        return rootClasses;
    }

    public void setRootClasses(String rootClasses)
    {
        this.rootClasses = rootClasses;
    }

    public String getOutput()
    {
        return output;
    }

    public void setOutput(String output)
    {
        this.output = output;
    }

    public String getXmlInjection()
    {
        return xmlInjection;
    }

    public void setXmlInjection(String xmlInjection)
    {
        this.xmlInjection = xmlInjection;
    }

    public List<String> getClasses()
    {
        return classes;
    }

    public void setClasses(List<String> classes)
    {
        this.classes = classes;
    }

   
    
}
