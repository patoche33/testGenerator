package fr.testgenerator.config;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.testgenerator.TestClassGenerator;

public final class Config
{
    final static Logger logger = LogManager.getLogger(Config.class);
    
    private static Properties PROPERTIES = new Properties();

    private static List<String> SOURCEDEPENDENCIES = new ArrayList<>();

    private static String ROOTPROJECT;

    private static String ROOTCLASSES;

    private static String OUTPUT;
    
    private static String XMLINJECTION;

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

            logger.info("Analyse du fichier config.properties!");
           
            FileInputStream fileInput = new FileInputStream("config.properties");
            PROPERTIES.load(fileInput);

            loadROOTPROJECT();

            loadSourceDependencies();
            
            loadXmlInjection();

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

   

    public static String getXMLINJECTION()
    {
        return XMLINJECTION;
    }



    public static void setXMLINJECTION(String xMLINJECTION)
    {
        XMLINJECTION = xMLINJECTION;
    }



    private static void loadClasses() throws IOException
    {

        if (!PROPERTIES.containsKey("classes") && !PROPERTIES.containsKey("directories"))
        {
            logger.error("Aucune classes à tester. veuillez préciser au moins une classe avec le paramètre 'classes=...'\n"
                + " ou avec le paramètre 'directories='...'"
                + " (exemple: classes=fr/gouv/finances/monprojet/utils/MailUtils.java,fr/gouv/finances/monprojet/webservice/impl/TableauDeBordWebServiceImpl.java\n"
                + " directories=fr/gouv/finances/monprojet/webdao/utils/,fr/gouv/finances/monprojet/webservice/)");
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
                            logger.warn("Classe " + getROOTCLASSES() + classe + " inexistante.");
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
                        logger.warn("Répertoire " + getROOTCLASSES() + dir + " inexistant.");
                        System.out.println("Répertoire " + getROOTCLASSES() + dir + " inexistant.");
                    }
                    else
                    {
                        logger.info("   -> Recherche des classes dans '" + getROOTCLASSES() + dir + "'");
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
            logger.error("Le répertoire des classes de test (paramètre output) n'est pas précisé dans config.properties !");
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
                logger.info("   -> Répertoire des classes de test: " + get("output") + " Ok.");
                //System.out.println("   -> Répertoire des classes de test: " + get("output") + " Ok.");
                setOUTPUT(get("output"));
            }
        }
    }
    /**
     * 
     * Cherher propriété source du projet
     *
     * @throws IOException
     */
    private static void loadROOTPROJECT() throws IOException
    {
        //arret si non présence de la propriété "project.root"
        if (!PROPERTIES.containsKey("project.root"))
        {
            logger.error("    -> Le répertoire racine du projet n'est pas précisé dans config.properties (paramètre project.root) !\n"
                + " Il doit être de la forme '/chemin/vers/projet/.");
            throw new IOException("    -> Le répertoire racine du projet n'est pas précisé dans config.properties (paramètre project.root) !\n"
                + " Il doit être de la forme '/chemin/vers/projet/.");

        }
        /* vérifier presence propriété project.classes. Si non présente => valeur pr défaut =/src/main/java
         */
        
        File fileSrcClasses ;
        if (!PROPERTIES.containsKey("project.classes"))
        {
            fileSrcClasses = new File(get("project.root") + "src/main/java/" );
            setROOTCLASSES(get("project.root" ) + "src/main/java/");
        }
        else
        {
            fileSrcClasses = new File(get("project.classes") );
            setROOTCLASSES(get("project.classes" ));
        }

        // chemin racine du projet 
        File fileSrc = new File(get("project.root") );
        if (!fileSrc.exists() || !fileSrc.isDirectory())
        {
            logger.error("    ->Répertoire racine du projet (paramètre project.root) incorrect ou inexistant !\n"
                + " Il doit être de la forme '/chemin/vers/projet'.");
            throw new IOException("    ->Répertoire racine du projet (paramètre project.root) incorrect ou inexistant !\n"
                + " Il doit être de la forme '/chemin/vers/projet'.");
        }
        else
        {
            logger.info("    -> Répertoire racine du projet: " + get("project.root") + " Ok.");
            //System.out.println("    -> Répertoire racine du projet: " + get("project.root") + " Ok.");
            setROOTPROJECT(get("project.root"));
        }
        
        // chemin racine des classes 
        if (!fileSrcClasses.exists() || !fileSrcClasses.isDirectory())
        {
            logger.error(
                "    -> Source des classes introuvables. Répertoire 'src/main/java/' inexistant, et/ou "
                    + " aucune source des classes n'est pas précisé dans config.properties (paramètre project.classes) !\n"
                    + " Il doit être de la forme '/chemin/vers/sources/.'");
            throw new IOException(
                "    -> Source des classes introuvables. Répertoire 'src/main/java/' inexistant, et/ou "
                    + " aucune source des classes n'est pas précisé dans config.properties (paramètre project.classes) !\n"
                    + " Il doit être de la forme '/chemin/vers/sources/.'"
                    );
        }
        else
        {
            logger.info("    -> Répertoire racine des classes: " + getROOTCLASSES() + " Ok."); 
            //System.out.println("    -> Répertoire racine des classes: " + get("project.classes") + " Ok.");
            
        }

    }

    private static void loadSourceDependencies()
    {
        if (!PROPERTIES.containsKey("sourceDependencies"))
        {
            logger.warn("    -> ATTENTION!  Répertoire des dépendances  non précisé dans config.properties."
                + "S'il existe des dépendances, veuillez le préciser.");
           // System.out.println("    -> ATTENTION!  Répertoire des dépendances  non précisé dans config.properties."
           //     + "S'il existe des dépendances, veuillez le préciser.");
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
                    logger.warn("    ->ATTENTION!  Répertoire des dépendances incorrect ou inexistant."
                        + "S'il existe des dépendances, veuillez le préciser.");
                  //  System.out.println("    ->ATTENTION!  Répertoire des dépendances incorrect ou inexistant."
                  //      + "S'il existe des dépendances, veuillez le préciser.");
                }
                else
                {
                    logger.info("    -> Répertoire des dépendances : ");
                    //System.out.println("    -> Répertoire des dépendances : ");
                    for (String source : listSource)
                    {
                        File fileSrc = new File(source);
                        if (!fileSrc.exists() || !fileSrc.isDirectory())
                        {
                            logger.warn("    - " + source + " incorrect.");
                            //System.out.println("    - " + source + " incorrect.");
                        }
                        else
                        {
                            logger.info("    - " + source + " ok.");
                            //System.out.println("    - " + source + " ok.");
                            getSourceDependencies().add(source);
                        }

                    }
                }
            }
            else
            {
                logger.warn("    -> ATTENTION! Aucun répertoire des dépendances présent dans config.properties.\n"
                    + "S'il existe des dépendances, veuillez le préciser.");
                //System.out.println("    -> ATTENTION! Aucun répertoire des dépendances présent dans config.properties.\n"
                //    + "S'il existe des dépendances, veuillez le préciser.");
            }

        }
    }
    
   
    private static void loadXmlInjection()
    {
        if (PROPERTIES.containsKey("project.xml"))
        {
            File fileSrc = new File(get("project.xml"));
            if (!fileSrc.exists() || !fileSrc.isFile())
            {
                logger.warn("    -> Fichier applicationContext-service.xml non trouvé");
               // System.out.println("    -> Fichier applicationContext-service.xml non trouvé");
            }
            
            else
            {
                logger.warn("     -> Fichier applicationContext-service.xml ok.");
                //System.out.println("     -> Fichier applicationContext-service.xml ok.");
                setXMLINJECTION(get("project.xml"));
            }
           
  
        }
    }

}
