package fr.testgenerator.core.bean;

public interface GenerationProgressListener
{
    // Appelé au début du scan (barre indéterminée)
    void onScanStarted(String message);

    // Appelé une fois la liste des fichiers trouvée (définit le maximum de la barre)
    void onScanCompleted(int totalFiles);

    // Appelé à chaque classe traitée (incrémente la barre)
    void onClassProcessed(int currentIndex, int totalFiles, String className);

    // Appelé à la fin
    void onGenerationCompleted();
}
