package fr.testgenerator.core.config;

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

import fr.testgenerator.core.bean.Config;

public final class ConfigLoader
{
    final static Logger logger = LogManager.getLogger(ConfigLoader.class);

    private static Properties PROPERTIES = new Properties();


    /*
     * static { try { File file = new File("config.properties"); if (!file.exists() || !file.isFile()) { throw new
     * IOException("Fichier config.properties inexistant"); } logger.info("Analyse du fichier config.properties!");
     * FileInputStream fileInput = new FileInputStream("config.properties"); PROPERTIES.load(fileInput); } catch
     * (IOException ex) { System.out.println(ex.getMessage()); // ex.printStackTrace(); } }
     */

    public static Config loadConfigFile() throws IOException
    {
        
            Config config = new Config();
            PROPERTIES = new Properties();
            File file = new File("config.properties");
            if (!file.exists() || !file.isFile())
            {
                throw new IOException("Fichier config.properties inexistant");
            }

            logger.info("Analyse du fichier config.properties!");

            try(FileInputStream fileInput = new FileInputStream("config.properties"))
            {
                PROPERTIES.load(fileInput);
                loadAndVerifyConfig(config);
            }
            catch (IOException ex)
            {
                logger.error(ex.getMessage());
                // ex.printStackTrace();
            }
      

        return config;
    }

    public static Config loadConfig(Properties properties)
    {
        Config config = new Config();
        try
        {
            
            PROPERTIES = new Properties(); 
            
            if (properties != null)
            {
                PROPERTIES.putAll(properties); 
            }
            logger.info("Analyse de la configuration!");
            loadAndVerifyConfig(config);

        }
        catch (IOException ex)
        {
            logger.error(ex.getMessage());
            // ex.printStackTrace();
        }

        return config;
    }

    private static void loadAndVerifyConfig(Config config) throws IOException
    {
        loadROOTPROJECT( config);

        loadSourceDependencies( config);

        loadXmlInjection( config);

        loadOutput( config);

        loadClasses( config);

    }

       

 
    public static String get(String cle)
    {
        return PROPERTIES.getProperty(cle);
    }


    /**
     * Cherher propriété source du projet
     *
     * @throws IOException
     */
    private static void loadROOTPROJECT(Config config) throws IOException
    {
        // arret si non présence de la propriété "project.root"
        if (!PROPERTIES.containsKey("project.root"))
        {
            logger.error("    -> Le répertoire racine du projet n'est pas précisé dans config.properties (paramètre project.root) !\n"
                + " Il doit être de la forme '/chemin/vers/projet/.");
            throw new IOException(
                "    -> Le répertoire racine du projet n'est pas précisé dans config.properties (paramètre project.root) !\n"
                    + " Il doit être de la forme '/chemin/vers/projet/.");

        }
        /*
         * vérifier presence propriété project.classes. Si non présente => valeur pr défaut =/src/main/java
         */

        File fileSrcClasses;
        // pas la proprité ou vide
        if (!PROPERTIES.containsKey("project.classes") || get("project.classes").isEmpty())
        {
            fileSrcClasses = new File(get("project.root").trim() + "src/main/java/");
            config.setRootClasses(get("project.root").trim() + "src/main/java/");
        }
        else
        {
            fileSrcClasses = new File(get("project.classes").trim());
            config.setRootClasses(get("project.classes").trim());
        }

        // chemin racine du projet
        File fileSrc = new File(get("project.root").trim());
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
            // System.out.println(" -> Répertoire racine du projet: " + get("project.root") + " Ok.");
            config.setRootProjet(get("project.root"));
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
                    + " Il doit être de la forme '/chemin/vers/sources/.'");
        }
        else
        {
            logger.info("    -> Répertoire racine des classes: " + config.getRootClasses() + " Ok.");
            // System.out.println(" -> Répertoire racine des classes: " + get("project.classes") + " Ok.");

        }

    }

    /**
     * Cherher propriété dependances
     */
    private static void loadSourceDependencies(Config config)
    {
        if (!PROPERTIES.containsKey("sourceDependencies"))
        {
            logger.warn("    -> ATTENTION!  Répertoire des dépendances  non précisé dans config.properties."
                + "S'il existe des dépendances, veuillez le préciser.");
            // System.out.println(" -> ATTENTION! Répertoire des dépendances non précisé dans config.properties."
            // + "S'il existe des dépendances, veuillez le préciser.");
        }
        else
        {

            String[] listSource = get("sourceDependencies").trim().split(",");
            if (listSource.length > 0)
            {
                boolean notDir = Arrays.stream(listSource).allMatch(source -> {
                    File fileSrc = new File(source.trim());
                    return (!fileSrc.exists() || !fileSrc.isDirectory());
                });
                if (notDir)
                {
                    logger.warn("    ->ATTENTION!  Répertoire des dépendances incorrect ou inexistant."
                        + "S'il existe des dépendances, veuillez le préciser.");
                    // System.out.println(" ->ATTENTION! Répertoire des dépendances incorrect ou inexistant."
                    // + "S'il existe des dépendances, veuillez le préciser.");
                }

                logger.info("    -> Répertoire des dépendances : ");
                // System.out.println(" -> Répertoire des dépendances : ");
                for (String source : listSource)
                {
                    File fileSrc = new File(source.trim());
                    if (!fileSrc.exists() || !fileSrc.isDirectory())
                    {
                        logger.warn("    - " + source + " incorrect.");
                        // System.out.println(" - " + source + " incorrect.");
                    }
                    else
                    {
                        logger.info("    - " + source + " ok.");
                        // System.out.println(" - " + source + " ok.");
                        config.getSourceDependencies().add(source.trim());
                    }

                }

            }
            else
            {
                logger.warn("    -> ATTENTION! Aucun répertoire des dépendances présent dans config.properties.\n"
                    + "S'il existe des dépendances, veuillez le préciser.");
                // System.out.println(" -> ATTENTION! Aucun répertoire des dépendances présent dans config.properties.\n"
                // + "S'il existe des dépendances, veuillez le préciser.");
            }

        }
    }

    /**
     * Cherher propriété xml infection (spring)
     */
    private static void loadXmlInjection(Config config)
    {
        if (PROPERTIES.containsKey("project.xml") && !get("project.xml").trim().isEmpty())
        {
            File fileSrc = new File(get("project.xml").trim());
            if (!fileSrc.exists() || !fileSrc.isFile())
            {
                logger.warn("    -> Fichier applicationContext-service.xml non trouvé");
                // System.out.println(" -> Fichier applicationContext-service.xml non trouvé");
            }

            else
            {
                logger.warn("     -> Fichier applicationContext-service.xml ok.");
                // System.out.println(" -> Fichier applicationContext-service.xml ok.");
                config.setXmlInjection(get("project.xml").trim());
            }

        }
    }

    /**
     * Cherher propriété output
     */
    private static void loadOutput(Config config) throws IOException
    {
        if (!PROPERTIES.containsKey("output"))
        {
            logger.error("Le répertoire des classes de test (paramètre output) n'est pas précisé dans config.properties !");
            throw new IOException("Le répertoire des classes de test (paramètre output) n'est pas précisé dans config.properties !");
        }
        else
        {
            // chemin racine du projet + /src/main/test
            File fileSrc = new File(get("output").trim());
            if (!fileSrc.exists() || !fileSrc.isDirectory())
            {
                throw new IOException("Répertoire des classes de test (paramètre output) incrorect ou inexistant !\n");
            }
            else
            {
                logger.info("   -> Répertoire des classes de test: " + get("output") + " Ok.");
                // System.out.println(" -> Répertoire des classes de test: " + get("output") + " Ok.");
                config.setOutput(get("output").trim());
            }
        }
    }

    private static void loadClasses(Config config) throws IOException
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
                    classe= classe.trim();
                    if (!config.getClasses().contains(classe))
                    {
                        File fileSrc = new File(config.getRootClasses() + classe);
                        if (!fileSrc.exists() || !fileSrc.isFile())
                        {
                            logger.warn("Classe " + config.getRootClasses() + classe + " inexistante.");
                            System.out.println("Classe " + config.getRootClasses() + classe + " inexistante.");
                        }
                        else
                        {
                            config.getClasses().add(config.getRootClasses() + classe);
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
                    dir = dir.trim();
                    File dirSrc = new File(config.getRootClasses() + dir);
                    if (!dirSrc.exists() || !dirSrc.isDirectory())
                    {
                        logger.warn("Répertoire " + config.getRootClasses() + dir + " inexistant.");
                        System.out.println("Répertoire " + config.getRootClasses() + dir + " inexistant.");
                    }
                    else
                    {
                        logger.info("   -> Recherche des classes dans '" + config.getRootClasses() + dir + "'");
                        System.out.println("   -> Recherche des classes dans '" + config.getRootClasses() + dir + "'");
                        Path start = Paths.get(config.getRootClasses() + dir);
                        Files.walk(start).filter(f -> f.toString().endsWith(".java"))
                            .forEach(f -> {
                                if (!config.getClasses().contains(f.toString()))
                                {
                                    config.getClasses().add(f.toString());
                                }

                            });

                    }
                }
            }
        }
    }

}
