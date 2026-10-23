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
       private static Properties PROPERTIES = new Properties();

    private static List<String> SOURCEDEPENDENCIES = new ArrayList<>();

    private static String ROOTPROJECT;

    private static String ROOTCLASSES;

    private static String OUTPUT;
    
    private static String MAVEN;

    private static List<String> CLASSES = new ArrayList<>();

    static
    {
        try
        {
            File file = new File("config.properties");
            if (!file.exists() || !file.isFile())
            {
                throw new IOException("Fichier config.properties inexistant");

            }

            System.out.println("Analyse du fichier config.properties!");

            FileInputStream fileInput = new FileInputStream("config.properties");
            PROPERTIES.load(fileInput);

            loadROOTPROJECT();

            loadSourceDependencies();
            
            loadMaven();

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

    public static String getROOTPROJECT()
    {
        return ROOTPROJECT;
    }

    public static void setROOTPROJECT(String rootProject)
    {
        ROOTPROJECT = rootProject;
    }

    public static String getOUTPUT()
    {
        return OUTPUT;
    }

    public static void setOUTPUT(String oUTPUT)
    {
        OUTPUT = oUTPUT;
    }

    public static String get(String cle)
    {
        return PROPERTIES.getProperty(cle);
    }

    public static String getROOTCLASSES()
    {
        return ROOTCLASSES;
    }

    public static void setROOTCLASSES(String rOOTCLASSES)
    {
        ROOTCLASSES = rOOTCLASSES;
    }

    public static String getMAVEN()
    {
        return MAVEN;
    }

    public static void setMAVEN(String mAVEN)
    {
        MAVEN = mAVEN;
    }

    private static void loadClasses() throws IOException
    {

        if (!PROPERTIES.containsKey("classes") && !PROPERTIES.containsKey("directories"))
        {
            throw new IOException("Aucune classes à tester. veuillez préciser au moins une classe avec le paramètre 'classes=...'\n"
                + " ou avec le paramètre 'directories='...'"
                + " (exemple: classes=fr/gouv/finances/monprojet/utils/MailUtils.java,fr/gouv/finances/monprojet/webservice/impl/TableauDeBordWebServiceImpl.java\n"
                + " directories=fr/gouv/finances/monprojet/webdao/utils/,fr/gouv/finances/monprojet/webservice/)");
        }

        if (PROPERTIES.containsKey("classes"))
        {
            
            String[] listClasses = get("classes").trim().split(",");
            Set<String> set = Stream.of(listClasses).collect(Collectors.toSet());
            if (set.size() > 0)
            {
                for (String classe : set)
                {
                    if (!getCLASSES().contains(classe))
                    {
                        File fileSrc = new File(getROOTCLASSES() + classe);
                        if (!fileSrc.exists() || !fileSrc.isFile())
                        {
                            System.out.println("Classe " + getROOTCLASSES() + classe + " inexistante.");
                        }
                        else
                        {
                            getCLASSES().add(getROOTCLASSES() + classe);
                        }
                    }

                }
            }
        }

        if (PROPERTIES.containsKey("directories"))
        {
            String[] listDir = get("directories").trim().split(",");
            Set<String> set = Stream.of(listDir).collect(Collectors.toSet());
            if (set.size() > 0)
            {
                for (String dir : set)
                {
                    File dirSrc = new File(getROOTCLASSES() + dir);
                    if (!dirSrc.exists() || !dirSrc.isDirectory())
                    {
                        System.out.println("Répertoire " + getROOTCLASSES() + dir + " inexistant.");
                    }
                    else
                    {
                        System.out.println("   -> Recherche des classes dans '" + getROOTCLASSES() + dir + "'");
                        Path start = Paths.get(getROOTCLASSES() + dir);
                        Files.walk(start).filter(f -> f.toString().endsWith(".java"))
                            .forEach(f -> {
                                if (!getCLASSES().contains(f.toString()))
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
        if (!PROPERTIES.containsKey("output"))
        {
            throw new IOException("Le répertoire des classes de test (paramètre output) n'est pas précisé dans config.properties !");
        }
        else
        {
            // chemin racine du projet + /src/main/test
            File fileSrc = new File(get("output"));
            if (!fileSrc.exists() || !fileSrc.isDirectory())
            {
                throw new IOException("Répertoire des classes de test (paramètre output) incrorect ou inexistant !\n");
            }
            else
            {
                System.out.println("   -> Répertoire des classes de test: " + get("output") + " Ok.");
                setOUTPUT(get("output"));
            }
        }
    }

    private static void loadROOTPROJECT() throws IOException
    {
        if (!PROPERTIES.containsKey("project.root"))
        {
            throw new IOException("    -> Le répertoire racine du projet n'est pas précisé dans config.properties (paramètre project.root) !\n"
                + " Il doit être de la forme '/chemin/vers/projet/.");

        }

        if (!PROPERTIES.containsKey("project.classes"))
        {
            throw new IOException(
                "    -> Le répertoire des classe du projet n'est pas précisé dans config.properties (paramètre project.classes) !\n"
                    + " Il doit être de la forme '/chemin/vers/projet/src/main/java.");

        }

        // chemin racine du projet 
        File fileSrc = new File(get("project.root") );
        if (!fileSrc.exists() || !fileSrc.isDirectory())
        {
            throw new IOException("    ->Répertoire racine du projet (paramètre project.root) incorrect ou inexistant !\n"
                + " Il doit être de la forme '/chemin/vers/projet'.");
        }
        else
        {
            System.out.println("    -> Répertoire racine du projet: " + get("project.root") + " Ok.");
            setROOTPROJECT(get("project.root"));
        }
        
       // chemin racine des classes 
         fileSrc = new File(get("project.classes") );
        if (!fileSrc.exists() || !fileSrc.isDirectory())
        {
            throw new IOException("    -> Répertoire racine des classes (paramètre project.classes) incorrect ou inexistant !\n"
                + " Il doit être de la forme '/chemin/vers/projet/src/main/java'.");
        }
        else
        {
            System.out.println("    -> Répertoire racine des classes: " + get("project.classes") + " Ok.");
            setROOTCLASSES(get("project.classes"));
        }

    }

    private static void loadSourceDependencies()
    {
        if (!PROPERTIES.containsKey("sourceDependencies"))
        {
            System.out.println("    -> ATTENTION!  Répertoire des dépendances  non précisé dans config.properties."
                + "S'il existe des dépendances, veuillez le préciser.");
        }
        else
        {

            String[] listSource = get("sourceDependencies").trim().split(",");
            if (listSource.length > 0)
            {
                boolean notDir = Arrays.stream(listSource).allMatch(source -> {
                    File fileSrc = new File(source);
                    return (!fileSrc.exists() || !fileSrc.isDirectory());
                });
                if (notDir)
                {
                    System.out.println("    ->ATTENTION!  Répertoire des dépendances incorrect ou inexistant."
                        + "S'il existe des dépendances, veuillez le préciser.");
                }
                else
                {
                    System.out.println("    -> Répertoire des dépendances : ");
                    for (String source : listSource)
                    {
                        File fileSrc = new File(source);
                        if (!fileSrc.exists() || !fileSrc.isDirectory())
                        {
                            System.out.println("    - " + source + " incorrect.");
                        }
                        else
                        {
                            System.out.println("    - " + source + " ok.");
                            getSourceDependencies().add(source);
                        }

                    }
                }
            }
            else
            {
                System.out.println("    -> ATTENTION! Aucun répertoire des dépendances présent dans config.properties.\n"
                    + "S'il existe des dépendances, veuillez le préciser.");
            }

        }
    }
    
   
    private static void loadMaven()
    {
        if (PROPERTIES.containsKey("maven"))
        {
            File fileSrc = new File(get("maven"));
            if (!fileSrc.exists() || !fileSrc.isDirectory())
            {
                System.out.println("    -> Répertoire des dépendances maven (.m2) " + get("maven") + " incorrecte.");
            }
            else
            {
                System.out.println("     -> Répertoire des dépendances maven"  +  get("maven") + " ok.");
                setMAVEN(get("maven"));
            }
           
  
        }
    }

}
