package generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.squareup.javapoet.ClassName;

public final class Imports
{
    private Imports()
    {
    }

    // JUnit
    public static final ClassName TEST =
        ClassName.get("org.junit.jupiter.api", "Test");

    public static final ClassName ASSERTIONS =
        ClassName.get("org.junit.jupiter.api", "Assertions");
    
   
    public static final ClassName BEFORE_EACH =
        ClassName.get("org.junit.jupiter.api", "BeforeEach");

    public static final ClassName AFTER_EACH =
        ClassName.get("org.junit.jupiter.api", "AfterEach");

    public static final ClassName PARAMETERIZED_TEST =
        ClassName.get("org.junit.jupiter.params", "ParameterizedTest");

    public static final ClassName METHOD_SOURCE =
        ClassName.get("org.junit.jupiter.params.provider", "MethodSource");

    // Mockito
    public static final ClassName MOCK =
        ClassName.get("org.mockito", "Mock");

    public static final ClassName INJECT_MOCKS =
        ClassName.get("org.mockito", "InjectMocks");

    public static final ClassName EXTEND_WITH =
        ClassName.get("org.junit.jupiter.api.extension", "ExtendWith");

    public static final ClassName MOCKITO_EXTENSION =
        ClassName.get("org.mockito.junit.jupiter", "MockitoExtension");
   
    /* cas when, doNothing, verify */
    public static final ClassName MOCKITO =
        ClassName.get("org.mockito", "Mockito");
    
    public static final ClassName MOCKITOSTATIC =
        ClassName.get("org.mockito", "MockedStatic");
   
   public static final ClassName ARG_MATCHERS =
        ClassName.get("org.mockito", "ArgumentMatchers");
   
   private static Map<ClassName,List<String>> listStaticImport = new HashMap<>();
   
   public static Map<ClassName, List<String>> getListStaticImport()
   {
       return listStaticImport;
   }
   
   /* ajoute dans la Map des static imports */
   public static void addStaticImport(ClassName classeName, String imp)
   {
       List<String> lisMatchers = listStaticImport.get(classeName);
       if (lisMatchers == null)
       {
           lisMatchers =  new ArrayList<String>();
       }
      
       if (! lisMatchers.contains(imp))
       {
           lisMatchers.add(imp);
       }
           
       listStaticImport.put(classeName,  lisMatchers);
   }
    
   /* ajoute dans la Map des static imports */
   public static void clearListStaticImport()
   {
       listStaticImport.clear();
   }
}
