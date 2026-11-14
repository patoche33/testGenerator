package fr.testgenerator.ui.controller;


import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.testgenerator.core.TestGenerationService;
import fr.testgenerator.core.config.Config;
import fr.testgenerator.core.bean.GenerationProgressListener;
import fr.testgenerator.core.logging.GuiAppender;
import fr.testgenerator.ui.view.MainWindow;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MainController {

    private final MainWindow view;
   
    private final TestGenerationService service;
   
    private static Logger logger = LogManager.getLogger(MainController.class);
    
    public MainController(MainWindow view, TestGenerationService service) {
        this.view = view;
        this.service = service;
        initListeners();
    }

    /**
     * 
     * Evenement des boutons
     *
     */
    private void initListeners() {
        //ouverture boite dialogue pour chemin des sources du projet ,...
        view.getButtonSrcProj().addActionListener(e -> chooseDirectory(view.getFieldScrProj()));
        view.getButtonSrcClas().addActionListener(e -> chooseDirectory(view.getFieldScrClas()));
        //execution de la genertion
        view.getButtonGo().addActionListener(e -> startGeneration());
        
        //Appender log message
        GuiAppender.setLogConsumer(messageDeLog -> {
            // Assure la mise à jour dans le Thread Graphique (EDT)
            SwingUtilities.invokeLater(() -> {
                view.getLogTextArea().append(messageDeLog);
                // Défilement automatique
                view.getLogTextArea().setCaretPosition(view.getLogTextArea().getDocument().getLength());
                });
             });
        
        
    }

    
    // Ouvre la boîte de dialogue et met à jour le champ texte
    private void chooseDirectory( JTextField textField) {
        JFileChooser dirChooser = new JFileChooser();
    
        // Force la sélection de dossiers uniquement
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        /// Vérifie le texte du JTextField (et non du JButton)
        String currentPath = textField.getText();
         if (currentPath != null && !currentPath.isEmpty()) {
           File dir = new File(currentPath);
             if (dir.exists()) {
                dirChooser.setCurrentDirectory(dir); // Ouvre directement dans le dossier
            }
        }

        int resultat = dirChooser.showOpenDialog(textField);
        if (resultat == JFileChooser.APPROVE_OPTION) {
            File dossierChoisi = dirChooser.getSelectedFile();
            textField.setText(dossierChoisi.getAbsolutePath());
        }
    }
    
    
    /**
     * 
     * execution de la generation des clesses de tests
     *
     */
   private void startGeneration() {
       /*
        String srcPath = view.getSourceDirField().getText().trim();
        String outPath = view.getOutputDirField().getText().trim();

        if (srcPath.isEmpty() || outPath.isEmpty()) {
            JOptionPane.showMessageDialog(view, 
                "Veuillez sélectionner les dossiers source et sortie.", 
                "Champs manquants", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        GenerationOptions options = new GenerationOptions(
            Path.of(srcPath),
            Path.of(outPath),
            view.getUseMockitoCheck().isSelected()
        );

        */

        view.getButtonGo().setEnabled(false);
        view.getFieldScrProj().setText("");
       
        // SwingWorker exécute le travail lourd dans un thread séparé
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // "publish" envoie les messages de manière sécurisée vers le thread graphique
                //service.generateTests(options, this::publish);
                service.startProgram(new GenerationProgressListener(){
                    @Override
                    public void onScanStarted(String message) {
                        publish(message);
                        SwingUtilities.invokeLater(() -> {
                            view.getProgressBar().setIndeterminate(true); // Animation de chargement
                            view.getProgressBar().setString("Scan des fichiers en cours...");
                        });
                    }
                    
                    @Override
                    public void onScanCompleted(int totalFiles) {
                        publish(totalFiles + " classes trouvées.");
                        SwingUtilities.invokeLater(() -> {
                            view.getProgressBar().setIndeterminate(false);
                            view.getProgressBar().setMaximum(totalFiles);
                            view.getProgressBar().setValue(0);
                            view.getProgressBar().setString("0 / " + totalFiles);
                        });
                    }
                    
                    @Override
                    public void onClassProcessed(int currentIndex, int totalFiles, String className) {
                        publish("[" + currentIndex + "/" + totalFiles + "] Traitement de " + className);
                        SwingUtilities.invokeLater(() -> {
                            view.getProgressBar().setValue(currentIndex);
                            int percent = (int) ((currentIndex * 100.0) / totalFiles);
                            view.getProgressBar().setString(percent + "% (" + currentIndex + "/" + totalFiles + ")");
                        });
                    }

                    @Override
                    public void onGenerationCompleted() {
                        SwingUtilities.invokeLater(() -> {
                            view.getProgressBar().setString("Terminé !");
                        });
                    }    
                });
                
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                // Reçoit les messages publiés et met à jour le JTextArea
                for (String message : chunks) {
                    logger.info(message);
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Vérifie s'il y a eu une exception pendant doInBackground()
                  
                } 
                catch (Exception ex) 
                {
                    logger.error("ERREUR : " + ex.getCause().getMessage());
                    /*
                    JOptionPane.showMessageDialog(view, 
                        "Une erreur est survenue : " + ex.getCause().getMessage(), 
                        "Erreur", 
                        JOptionPane.ERROR_MESSAGE);
                        */
                } 
                finally 
                {
                    view.getButtonGo().setEnabled(true);
                }
            }
        };

        worker.execute();
    }

   /*
    private void appendLog(String message) {
        view.getLogTextArea().append(message + "\n");
        // Fait défiler automatiquement vers le bas
        view.getLogTextArea().setCaretPosition(view.getLogTextArea().getDocument().getLength());
        
       
    }
    */
    
    
}
