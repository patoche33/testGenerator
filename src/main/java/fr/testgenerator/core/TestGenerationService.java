package fr.testgenerator.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.testgenerator.core.bean.Config;
import fr.testgenerator.core.bean.GenerationProgressListener;
import fr.testgenerator.core.config.ConfigLoader;
import fr.testgenerator.core.generator.TestClassGenerator;
import fr.testgenerator.ui.controller.MainController;
import fr.testgenerator.ui.view.MainWindow;
import fr.testgenerator.core.bean.Config;
public class TestGenerationService
{
    private static Logger logger = LogManager.getLogger(TestGenerationService.class);
   
    private static TestClassGenerator testClassGenerator;
    
    /**
     * 
     * mode ligne de commande
     *
     * @param listener
     * @throws IOException 
     */
    public static void startCli(GenerationProgressListener listener) throws IOException
    {
        //lecture du fichier config.properties et charhgement dans bean Config
        Config config =  ConfigLoader.loadConfigFile();
       
        startProgram(listener,   config );
    }
    
    /**
     * 
     * mode interface graphique
     *
     * @param listener
     */
    public static void startGui(Config config, GenerationProgressListener listener) throws IOException
    {
      
        startProgram(listener,   config );
    
    }
    
    //public static void startProgram(GenerationProgressListener listener)
    public static void startProgram(GenerationProgressListener listener,  Config config )
    {
        try
        {
            
            /*String sourceClasses = Config.getROOTCLASSES();
            String out = Config.getOUTPUT();
            List<String> classes = Config.getCLASSES();
            String xml = Config.getXMLINJECTION();
    */
            String sourceClasses = config.getRootClasses();
            String out = (String) config.getOutput();
            List<String> classes = (List<String>) config.getClasses();
            String xml = (String) config.getXmlInjection();
            
            if (sourceClasses != null && out != null && !classes.isEmpty() )
            {
                logger.info("Démarrage de l'analyse...");

                /* Lister les classes et méthodes */
                 testClassGenerator = new TestClassGenerator(config);
        
                /* générer les classes de test */
                listener.onScanCompleted(classes.size());
                                
                //logger.info("*** Création des classes de test - " + classes.size() + " classes à traiter ***");
                int indice=1;
                for (String classe : classes)
                 {
                    listener.onClassProcessed(indice , classes.size(), classe);
                    indice ++;
                    try
                    {
                        String source =  classe;
                        testClassGenerator.generate(new File(source), Paths.get(out), config);
                    }
                    catch (Exception e)
                    {
                        logger.error("Erreur lors du traitement du fichier '" + classe + "' : " + e.getMessage());
                    }
                    
                }
                
                listener.onGenerationCompleted();
                logger.info("Génération terminée  !");
            }
           
        }
        catch (Exception e)
        {
            logger.error("Erreur générale d'initialisation : " + e.getMessage());
            
        }
        
    }
    
    /**
     * 
     * Appelé depuis le plugin sur chois d'une classe
     *
     * @param sourceClasses
     * @param out
     * @throws IOException
     */
    public static void startForClassSelected(String sourceClasses, String out, Config config ) 
    {
        
        try
        {
            String xml = config.getXmlInjection();
    
            if (sourceClasses != null && out != null )
            {
                logger.info("Scan du projet");
    
                /* Lister les classes et méthodes */
                testClassGenerator = new TestClassGenerator(config);
                logger.info("Scan terminé");
            }    
        }
        catch (Exception e)
        {
            logger.error("Erreur générale d'initialisation : " + e.getMessage());
            
        }
    }
    
    public static void generateTestFor(File source, String out, Config config  )
    {
        try
        {
            testClassGenerator.generate(source, Paths.get(out), config);
        }
        catch (Exception e)
        {
            logger.error("Erreur lors du traitement du fichier '" + source + "' : " + e.getMessage());
            }
    }
}
