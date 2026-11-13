package fr.testgenerator.cli;


import fr.testgenerator.core.bean.GenerationProgressListener;

public class ConsoleProgressListener implements GenerationProgressListener {

    @Override
    public void onScanStarted(String message) {
        System.out.println("[SCAN] " + message);
    }

    @Override
    public void onScanCompleted(int totalFiles) {
        System.out.println("[INFO] " + totalFiles + " classes trouvées. Démarrage de la génération...\n");
    }

    @Override
    public void onClassProcessed(int currentIndex, int totalFiles, String className) {
        int percent = (int) ((currentIndex * 100.0) / totalFiles);
        
        // Dessine une barre visuelle en ASCII : [=====>    ] 50% (12/24)
        int barWidth = 20;
        int filled = (percent * barWidth) / 100;
        String bar = "=".repeat(filled) + " ".repeat(barWidth - filled);

        // "\r" permet de réécrire sur la même ligne dans le terminal
        System.out.printf("\r[%s] %3d%% (%d/%d) - %-40s", bar, percent, currentIndex, totalFiles, className);
    }

    @Override
    public void onGenerationCompleted() {
        System.out.println("\n\n[SUCCÈS] Génération terminée !");
    }
}
