package fr.testgenerator.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.testgenerator.core.bean.GenerationProgressListener;
import fr.testgenerator.core.config.Config;
import fr.testgenerator.core.generator.TestClassGenerator;
import fr.testgenerator.ui.controller.MainController;
import fr.testgenerator.ui.view.MainWindow;

public class TestGenerationService
{
    private static Logger logger = LogManager.getLogger(TestGenerationService.class);
   
    private static TestClassGenerator testClassGenerator;
    
    /**
     * 
     * mode ligne de commande
     *
     * @param listener
     */
    public static void startCli(GenerationProgressListener listener)
    {
        Properties config = new Properties();
        config.put("sourceClasses", Config.getROOTCLASSES());
        config.put("out", Config.getOUTPUT());
        config.put("classes", Config.getCLASSES());
        config.put("xml", Config.getXMLINJECTION());
        startProgram(listener,   config );
    }
    
    /**
     * 
     * mode interface graphique
     *
     * @param listener
     */
    public static void startGui(MainWindow view, GenerationProgressListener listener) throws IOException
    {
        Properties config = new Properties();
        config.put("sourceClasses", Config.getROOTCLASSES());
       
        config.put("classes", Config.getCLASSES());
        config.put("xml", Config.getXMLINJECTION());

       ;
        //propertie output
        File fileSrc = new File(view.getFieldScrCOut().getText());
        if (!fileSrc.exists() || !fileSrc.isDirectory())
        {
                throw new IOException("Répertoire des classes de test (paramètre output) incrorect ou inexistant !\n");
        }
        else {
            config.put("out", view.getFieldScrCOut().getText());
            logger.info("   -> Répertoire des classes de test: " + config.getProperty("out") + " Ok.");
        }
        startProgram(listener,   config );
    
    }
    //public static void startProgram(GenerationProgressListener listener)
    public static void startProgram(GenerationProgressListener listener,  Properties config )
    {
        try
        {
            
            /*String sourceClasses = Config.getROOTCLASSES();
            String out = Config.getOUTPUT();
            List<String> classes = Config.getCLASSES();
            String xml = Config.getXMLINJECTION();
    */
            String sourceClasses = (String) config.get("sourceClasses");
            String out = (String) config.get("out");
            List<String> classes = (List<String>) config.get("classes");
            String xml = (String) config.get("xml");
            
            if (sourceClasses != null && out != null && !classes.isEmpty() )
            {
                logger.info("Démarrage de l'analyse...");

                /* Lister les classes et méthodes */
                 testClassGenerator = new TestClassGenerator(sourceClasses, xml);
        
                /* générer les classes de test */
                listener.onScanCompleted(classes.size());
                                
                logger.info("*** Création des classes de test - " + classes.size() + " classes à traiter ***");
                int indice=0;
                for (String classe : classes)
                 {
                    try
                    {
                        String source =  classe;
                        testClassGenerator.generate(new File(source), Paths.get(out));
                        
                        listener.onClassProcessed(indice +1, classes.size(), classe);
                        indice ++;
                        
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
    public static void startForClassSelected(String sourceClasses, String out ) 
    {
        
        try
        {
            String xml = Config.getXMLINJECTION();
    
            if (sourceClasses != null && out != null )
            {
                logger.info("Scan du projet");
    
                /* Lister les classes et méthodes */
                testClassGenerator = new TestClassGenerator(sourceClasses, xml);
                logger.info("Scan terminé");
            }    
        }
        catch (Exception e)
        {
            logger.error("Erreur générale d'initialisation : " + e.getMessage());
            
        }
    }
    
    public static void generateTestFor(File source, String out )
    {
        try
        {
            testClassGenerator.generate(source, Paths.get(out));
        }
        catch (Exception e)
        {
            logger.error("Erreur lors du traitement du fichier '" + source + "' : " + e.getMessage());
            }
    }
}
