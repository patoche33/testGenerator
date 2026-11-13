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



    public void run(String[] args)
    {
        
        generationService.startProgram();
    }
}
