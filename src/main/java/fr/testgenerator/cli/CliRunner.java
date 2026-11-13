package fr.testgenerator.cli;

import fr.testgenerator.core.TestGenerationService;

public class CliRunner
{
    TestGenerationService generationService ;
    
    
    
    public CliRunner(TestGenerationService generationService)
    {
        super();
        this.generationService = generationService;
    }



    public int run(String[] args)
    {
        try {
            generationService.startProgram(new ConsoleProgressListener());
            return 0; // Code de succès
        }
        catch (Exception e) {
            System.err.println("\n[ERREUR] " + e.getMessage());
            return 1; // Code d'erreur
        }
    }
}
