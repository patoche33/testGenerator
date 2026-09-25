package generator.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Config
{
    
    private static  Properties PROPERTIES = new Properties();
    
    private static List<String> SOURCEDEPENDENCIES = new ArrayList<>();
    
    private static String SOURCEPROJECT;
    
    private static String OUTPUT;
    
    private static  List<String> CLASSES = new ArrayList<>();
    
    static {
        try
        {
            File file = new File("config.properties");
            if (!file.exists() || !file.isFile())
            {
                throw new IOException("Fichier config.properties inexistant");

            }
           
            System.out.println("Lecture du fichier config.properties!"); 
            
            FileInputStream fileInput = new FileInputStream("config.properties");
            PROPERTIES.load(fileInput);
            
            loadSourceDependencies();
            
            loadSourceProject();
            
            loadOutput();
            
            loadClasses();
            
            
                
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage()); 
           // ex.printStackTrace();
        }
    }   
    
    
    public static List<String> getCLASSES()
    {
        return CLASSES;
    }

    public static void setCLASSES(List<String> cLASSES)
    {
        CLASSES = cLASSES;
    }

    public static List<String> getSourceDependencies()
    {
        return SOURCEDEPENDENCIES;
    }

    public static String getSOURCEPROJECT()
    {
        return SOURCEPROJECT;
    }

    public static void setSOURCEPROJECT(String sOURCEPROJECT)
    {
        SOURCEPROJECT = sOURCEPROJECT;
    }

   public static String getOUTPUT()
    {
        return OUTPUT;
    }

    public static void setOUTPUT(String oUTPUT)
    {
        OUTPUT = oUTPUT;
    }

    public static String get(String cle) {
        return PROPERTIES.getProperty(cle);
    }
    
    private static void loadClasses() throws IOException
    {
        if ( !PROPERTIES.containsKey("classes") && !PROPERTIES.containsKey("directories"))
        {
            throw new IOException("Aucune classes à tester. veuillez préciser au moins une classe avec le paramètre 'classes=...'\n"
                + " ou avec le paramètre 'directories='...'"
                + " (exemple: classes=fr/gouv/finances/monprojet/utils/MailUtils.java,fr/gouv/finances/monprojet/webservice/impl/TableauDeBordWebServiceImpl.java\n"
                + " directories=fr/gouv/finances/monprojet/webdao/utils/,fr/gouv/finances/monprojet/webservice/)"); 
        } 
        
        if ( PROPERTIES.containsKey("classes"))
        {
            String[] listClasses = get("classes").trim().split(",");
            Set<String> set = Stream.of(listClasses).collect(Collectors.toSet());
            if (set.size() > 0)
            {
              for(String classe: set)
              {    
                  if ( ! getCLASSES().contains(classe))
                  {
                      File fileSrc = new File(getSOURCEPROJECT() + classe);
                      if (! fileSrc.exists() || ! fileSrc.isFile())
                      {
                          System.out.println("Classe " + getSOURCEPROJECT() + classe + " inexistante.");
                      }
                      else
                      {
                          getCLASSES().add(getSOURCEPROJECT() + classe);
                      }
                  }
                 
               }
            } 
         }
        
        if ( PROPERTIES.containsKey("directories"))
        {
            String[] listDir= get("directories").trim().split(",");
            Set<String> set = Stream.of(listDir).collect(Collectors.toSet());
            if (set.size() > 0)
            {
              for(String dir: set)
              {    
                   File dirSrc = new File(getSOURCEPROJECT() + dir);
                   if (! dirSrc.exists() || ! dirSrc.isDirectory())
                   {
                       System.out.println("Répertoire " + getSOURCEPROJECT() + dir + " inexistant.");
                   }
                   else
                   {
                       System.out.println("Recherche des classes dans '" + getSOURCEPROJECT() + dir + "'" );
                       Path start = Paths.get( getSOURCEPROJECT() + dir);
                       Files.walk(start).filter(f -> f.toString().endsWith(".java"))
                       .forEach(f -> {
                           if ( ! getCLASSES().contains(f.toString()))
                           {
                               getCLASSES().add(f.toString());
                           }
                           
                       });
                       
                   }
               }
            } 
         }
    }
    
    private static void loadOutput() throws IOException
    {
        if ( !PROPERTIES.containsKey("output"))
        {
            throw new IOException("Le répertoire des classes de test n'est pas précisé dans config.properties !"); 
        } 
        else
        {
            File fileSrc = new File(get("output"));
            if (!fileSrc.exists() || !fileSrc.isDirectory())
            {
                throw new IOException("Répertoire des classes de test incrorect ou inexistant !\n");
            }
            else
            {
                System.out.println("Répertoire des classes de test: " + get("output")); 
                setOUTPUT(get("output"));
            }          
         }
    }
    
    private static void loadSourceProject() throws IOException
    {
        if ( !PROPERTIES.containsKey("sourceProject"))
        {
            throw new IOException("Le répertoire racine du projet n'est pas précisé dans config.properties !\n"
                +" Il doit être de la forme '/chemin/vers/projet/src/main/java/'."); 
           
        }
        else
        {
            File fileSrc = new File(get("sourceProject"));
            if (!fileSrc.exists() || !fileSrc.isDirectory())
            {
                throw new IOException("Répertoire racine du projet incrorect ou inexistant !\n"
                    +" Il doit être de la forme '/chemin/vers/projet/src/main/java/'.");
            }
            else
            {
                System.out.println("Répertoire racine du projet: " + get("sourceProject")); 
                setSOURCEPROJECT( get("sourceProject"));
            }
         
        }
    }
    
    private static void loadSourceDependencies()
    {
        if ( !PROPERTIES.containsKey("sourceDependencies"))
        {
            System.out.println("ATTENTION!  Répertoire des dépendances (maven ou autres) non précisé dans config.properties."
                + "S'il existe des dépendances, veuillez le préciser."); 
        }
        else
        {
           
            String[] listSource = get("sourceDependencies").trim().split(",");
            if (listSource.length > 0)
            {
               boolean notDir =Arrays.stream(listSource).allMatch(source->{
                   File fileSrc = new File(source);
                   return (!fileSrc.exists() || !fileSrc.isDirectory());
               });
               if (notDir)
               {
                   System.out.println("ATTENTION!  Répertoire des dépendances (maven ou autres) incorrect ou inexistant."
                       + "S'il existe des dépendances, veuillez le préciser.");
               }
               else
               {
                    System.out.println("Répertoire des dépendances (maven ou autres): ");
                    for (String source: listSource) {
                        File fileSrc = new File(source);
                        if (!fileSrc.exists() || !fileSrc.isDirectory())
                        {
                            System.out.println("    " + source + " incorrect.");
                        }
                        else
                        {
                            System.out.println("    " + source + " ok.");
                            getSourceDependencies().add(source);
                        }
                       
                    } 
               }     
            }
            else
            {
                System.out.println("ATTENTION! Aucun répertoire des dépendances (maven ou autres) présent dans config.properties.\n"
                    + "S'il existe des dépendances, veuillez le préciser."); 
            }
           
        }
    }
}
