package generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.squareup.javapoet.ClassName;

public  class Mocks
{

    public static final Map<String,String> mapMocks = new HashMap<>();
    
    /* lister les classe à mocker */
    private static void initMocks()
    {
        mapMocks.put("HttpServletRequest", "jakarta.servlet.http.HttpServletRequest");
        mapMocks.put("HttpSession", "jakarta.servlet.http.HttpSession");
        mapMocks.put("DataSource", "jakarta.activation.DataSource");
        mapMocks.put("TimeZone", "java.util.TimeZone");
       /* 
        mapMocks.put("Calendar", "java.util.Calendar");
        mapMocks.put("LocalDate", "java.time.LocalDate");
        mapMocks.put("LocalDateTime", "java.time.LocalDateTime");
       */
    }

    public static Map<String,String> getListmocks()
    {
        initMocks();
        
        return mapMocks;
    }
    
    
}
