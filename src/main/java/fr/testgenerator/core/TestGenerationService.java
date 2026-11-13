package fr.testgenerator.core;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.testgenerator.core.config.Config;
import fr.testgenerator.core.generator.TestClassGenerator;
import fr.testgenerator.ui.controller.MainController;

public class TestGenerationService
{
    private static Logger logger = LogManager.getLogger(TestGenerationService.class);

    public static void startProgram()
    {
        try
        {
            String sourceClasses = Config.getROOTCLASSES();
            String out = Config.getOUTPUT();
            List<String> classes = Config.getCLASSES();
            String xml = Config.getXMLINJECTION();
    
            if (sourceClasses != null && out != null && !classes.isEmpty() )
            {
                logger.info("Démarrage de l'analyse...");

                /* Lister les classes et méthodes */
                TestClassGenerator testClassGenerator = new TestClassGenerator(sourceClasses, xml);
        
                /* générer les classes de test */
                logger.info("*** Création des classes de test ***");
                 for (String classe : classes)
                {
                    try
                    {
                        String source =  classe;
                        testClassGenerator.generate(new File(source), Paths.get(out));
                    }
                    catch (Exception e)
                    {
                        logger.error("Erreur lors du traitement du fichier '" + classe + "' : " + e.getMessage());
                        }
                    
                }
                 logger.info("Génération terminée  !");
            }
           
        }
        catch (Exception e)
        {
            logger.error("Erreur générale d'initialisation : " + e.getMessage());
            
        }
        
    }
}
