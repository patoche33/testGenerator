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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class MainController
{

    private final MainWindow view;
    
    private Properties config;
    
    private final TestGenerationService service;

    private static Logger logger = LogManager.getLogger(MainController.class);

    public MainController(MainWindow view, TestGenerationService service)
    {
        this.view = view;
        this.service = service;
        this.config = new Properties();
        initListeners();
        loadConfiguration();
    }

    /**
     * Evenement des boutons
     */
    private void initListeners()
    {
        // ouverture boite dialogue pour chemin des sources du projet ,...
        view.getButtonSrcProj().addActionListener(e -> chooseDirectory(view.getFieldScrProj()));
        view.getButtonSrcClas().addActionListener(e -> chooseDirectory(view.getFieldScrClas()));
        view.getButtonSrcOut().addActionListener(e -> chooseDirectory(view.getFieldScrOut()));
        view.getButtonSrcXml().addActionListener(e -> chooseDirectory(view.getFieldScrXml()));
        view.getButtonAddClasses().addActionListener(e -> addPath(view.getListModelClasses(), "Classe à tester", JFileChooser.FILES_ONLY));
        view.getButtonRemClasses().addActionListener(e -> removePath(view.getListModelClasses(), view.getPathListClasses()));
        view.getButtonAddDirs()
            .addActionListener(e -> addPath(view.getListModelDirs(), "Dossier des classes à tester", JFileChooser.DIRECTORIES_ONLY));
        view.getButtonRemDirs().addActionListener(e -> removePath(view.getListModelDirs(), view.getPathListDirs()));
        view.getButtonAddDeps().addActionListener(
            e -> addPath(view.getListModelDeps(), "Dossier des dépendances (Maven ou autres jars)", JFileChooser.DIRECTORIES_ONLY));
        view.getButtonRemDeps().addActionListener(e -> removePath(view.getListModelDeps(), view.getPathListDeps()));
        // execution de la genertion
        view.getButtonGo().addActionListener(e -> startGeneration());

        // Appender log message
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
    private void chooseDirectory(JTextField textField)
    {
        JFileChooser dirChooser = new JFileChooser();

        // Force la sélection de dossiers uniquement
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        /// Vérifie le texte du JTextField (et non du JButton)
        String currentPath = textField.getText();
        if (currentPath != null && !currentPath.isEmpty())
        {
            File dir = new File(currentPath);
            if (dir.exists())
            {
                dirChooser.setCurrentDirectory(dir); // Ouvre directement dans le dossier
            }
        }

        int resultat = dirChooser.showOpenDialog(textField);
        if (resultat == JFileChooser.APPROVE_OPTION)
        {
            File dossierChoisi = dirChooser.getSelectedFile();
            textField.setText(dossierChoisi.getAbsolutePath());
        }
    }

    private void addPath(DefaultListModel<String> listModel, String title, int type)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        // Limite la sélection aux dossiers ou aux fichiers uniquement
        chooser.setFileSelectionMode(type);
        chooser.setAcceptAllFileFilterUsed(false);
        // ctiver le mode multi-sélection
        chooser.setMultiSelectionEnabled(true);

        int resultat = chooser.showOpenDialog(this.view);
        if (resultat == JFileChooser.APPROVE_OPTION)
        {
            StringBuilder pathIn = new StringBuilder();
            File[] files = chooser.getSelectedFiles();
            for (File file : files)
            {
                String path = file.getAbsolutePath();
                // Évite les doublons dans la liste
                if (!listModel.contains(path))
                {
                    listModel.addElement(path);
                }
                else
                {
                    pathIn.append(path + "\n");
                }
            }
            if (! pathIn.isEmpty())
            {
                JOptionPane.showMessageDialog(this.view,  pathIn + "\ndéjà dans la liste.",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);

            }

        }
    }

    private void removePath(DefaultListModel<String> listModel, JList<String> pathList)
    {
        int indexSelectionne = pathList.getSelectedIndex();
        if (indexSelectionne != -1)
        {
            listModel.remove(indexSelectionne);
        }
        else
        {
            JOptionPane.showMessageDialog(this.view,
                "Veuillez d'abord choisir dans la liste pour le retirer.",
                "Aucune sélection",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * execution de la generation des clesses de tests
     */
    private void startGeneration()
    {
        /*
         * String srcPath = view.getSourceDirField().getText().trim(); String outPath =
         * view.getOutputDirField().getText().trim(); if (srcPath.isEmpty() || outPath.isEmpty()) {
         * JOptionPane.showMessageDialog(view, "Veuillez sélectionner les dossiers source et sortie.", "Champs manquants",
         * JOptionPane.WARNING_MESSAGE); return; } GenerationOptions options = new GenerationOptions( Path.of(srcPath),
         * Path.of(outPath), view.getUseMockitoCheck().isSelected() );
         */

        view.getButtonGo().setEnabled(false);
      

        // SwingWorker exécute le travail lourd dans un thread séparé
        SwingWorker<Void, String> worker = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                // "publish" envoie les messages de manière sécurisée vers le thread graphique
               
                verifyConfig();
                
                service.startGui(config, new GenerationProgressListener()
                {
                    @Override
                    public void onScanStarted(String message)
                    {
                        publish(message);
                        SwingUtilities.invokeLater(() -> {
                            view.getProgressBar().setIndeterminate(true); // Animation de chargement
                            view.getProgressBar().setString("Scan des fichiers en cours...");
                        });
                    }

                    @Override
                    public void onScanCompleted(int totalFiles)
                    {
                        publish(totalFiles + " classes trouvées.");
                        SwingUtilities.invokeLater(() -> {
                            view.getProgressBar().setIndeterminate(false);
                            view.getProgressBar().setMaximum(totalFiles);
                            view.getProgressBar().setValue(0);
                            view.getProgressBar().setString("0 / " + totalFiles);
                        });
                    }

                    @Override
                    public void onClassProcessed(int currentIndex, int totalFiles, String className)
                    {
                        publish("[" + currentIndex + "/" + totalFiles + "] Traitement de " + className);
                        SwingUtilities.invokeLater(() -> {
                            view.getProgressBar().setValue(currentIndex);
                            int percent = (int) ((currentIndex * 100.0) / totalFiles);
                            view.getProgressBar().setString(percent + "% (" + currentIndex + "/" + totalFiles + ")");
                        });
                    }

                    @Override
                    public void onGenerationCompleted()
                    {
                        SwingUtilities.invokeLater(() -> {
                            view.getProgressBar().setString("Terminé !");
                        });
                    }
                });

                return null;
            }

            @Override
            protected void process(List<String> chunks)
            {
                // Reçoit les messages publiés et met à jour le JTextArea
                for (String message : chunks)
                {
                    logger.info(message);
                }
            }

            @Override
            protected void done()
            {
                try
                {
                    get(); // Vérifie s'il y a eu une exception pendant doInBackground()

                }
                catch (Exception ex)
                {
                    logger.error("ERREUR : " + ex.getCause().getMessage());
                    /*
                     * JOptionPane.showMessageDialog(view, "Une erreur est survenue : " + ex.getCause().getMessage(), "Erreur",
                     * JOptionPane.ERROR_MESSAGE);
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

    /**
     * 
     * charger le paramètrage du fichier config.propeties si il existe
     *
     */
    private void loadConfiguration() {
        try 
        {
            
            File file = new File("config.properties");
            if (!file.exists() || !file.isFile())
            {
                return;
            }
            FileInputStream fileInput = new FileInputStream("config.properties");
            config.load(fileInput);
            
            String rootProject = (String) config.get("project.root");
            String rootClasses= (String) config.get("project.classes");
            String out= (String) config.get("output");

            // Mise à jour de la MainWindow
            view.setFieldScrProj(rootProject);
            view.setFieldScrClas(rootClasses);
            view.setFieldScrOut(out);
            loadListModel("sourceDependencies", view.getListModelDeps());
            loadListModel("classes", view.getListModelClasses());
            loadListModel("directories", view.getListModelDirs());

        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier config : " + e.getMessage());
        }
    }

    public void loadListModel(String prop, DefaultListModel<String> listModel)
    {
        String property= (String) config.get(prop);
        if (property != null && !property.trim().isEmpty()) {
            // Découpage par la virgule
            String[] items = property.split(",");
            for (String item : items) {
                String cleanItem = item.trim();
                // On ignore les éléments vides (par exemple s'il y a une virgule finale)
                if (!cleanItem.isEmpty()) {
                    listModel.addElement(cleanItem);
                }
            }
        }    
    }
    
    private void verifyConfig() throws IOException
    {
        //reset du Properties config pour le recharger avec les données de la vue
        config.clear();
        config.put("project.root",  view.getFieldScrProj().getText());
        config.put("project.classes", view.getFieldScrClas().getText());
        config.put("output",  view.getFieldScrOut().getText());
        config.put("project.xml", view.getFieldScrXml().getText());
        //recuperer toutes les ligne de la liste dans une String
        String classes = String.join(", ", Collections.list(view.getListModelClasses().elements()));
        config.put("classes", classes);
        //recuperer toutes les ligne de la liste dans une String
        String directories = String.join(", ", Collections.list(view.getListModelDirs().elements()));
        config.put("directories", directories);
        String dependencies = String.join(", ", Collections.list(view.getListModelDirs().elements()));
        config.put("sourceDependencies", dependencies );
        
        //verifier la config
        Config.loadConfig(config);
       
      
    }
}
