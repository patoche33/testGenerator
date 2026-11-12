package generator.bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public enum Mocks
{
    HttpServletRequest( "jakarta.servlet.http.HttpServletRequest"),
    HttpSession("jakarta.servlet.http.HttpSession"),
    DataSource("jakarta.activation.DataSource");
    
    private String type;

    private Mocks(String type)
    {
        this.type = type;
    }
    
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public static Map<String,String> getListmocks()
    {
        Map<String,String> mapMocks = new HashMap<>();
        for (Mocks mock : Mocks.values()) {
            mapMocks.put(mock.name(), mock.getType());
        }
        
        return mapMocks;
    }
}
