package generator.bean;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.type.Type;

public class FieldInfo
{
    private String name;

    private String type;
    
    private List<String> annotations = new ArrayList<String>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public List<String> getAnnotations()
    {
        return annotations;
    }
    
    
}
