package fr.testgenerator.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import fr.testgenerator.core.TestGenerationService;
import fr.testgenerator.ui.controller.MainController;
import fr.testgenerator.ui.view.MainWindow;
/**
 * 
 * GuiLauncher.java
 * Démarrage interface graphique
 * @author psiron01
 * Date: 13 nov. 2026
 */
public class GuiLauncher
{
    public static void launch(TestGenerationService generationService) {
        // Appliquer l'apparence native du système d'exploitation (Windows, Mac, etc.)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Garde le thème par défaut en cas d'erreur
        }

        // Toujours démarrer Swing dans l'Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainWindow view = new MainWindow();
            new MainController(view, generationService);
            view.setVisible(true);
        });
    }
}
