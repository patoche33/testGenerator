package fr.testgenerator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.testgenerator.config.Config;
import fr.testgenerator.logging.GuiAppender;

public class CreateTest
{

    public static void main(String[] args) throws IOException
    {
       

        Boolean gui = false;
        for(String arg: args)
        {
             if ("--gui".equalsIgnoreCase(arg)) {
               gui = true;
            }
        }

        if (gui)
        {
            startGui();
        }
        else 
        {
            startProgram();
        }
    }
    
    private static void startGui()
    {
          setFont(new Font("SansSerif", Font.PLAIN, 20));
        System.out.println("*** Démarrage intefrace graphique ***");
        // Lancer l'interface en toute sécurité sur le fil d'exécution Swing
       
            //fenêtre principale
            JFrame mainFrame = new JFrame("Générateur de tests unitaires");
            mainFrame.setSize(1024, 768);
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setLocationRelativeTo(null); // Centrer la fenêtre

            //panneau principal avec marges de 10px
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            //panneau des zones de texte et boutons de sélecion
            JPanel topPanel = new JPanel(new BorderLayout(5, 5));
            JButton buttonSrcProj = new JButton("Sélectionner le dossier source du projet");
            buttonSrcProj.setPreferredSize(new Dimension(160, 30)); // Taille fixe du bouton
            
           
            JTextField fieldScrProj= new JTextField();
            fieldScrProj.setEditable(false); // Empêche la saisie manuelle erronée
            fieldScrProj.setPreferredSize(new Dimension(400, 30));

            buttonSrcProj.addActionListener(e -> choiceDir(fieldScrProj));

            //ajout au panneau topPanel
            topPanel.add(new JLabel("Fichier sélectionné : "), BorderLayout.WEST);
            topPanel.add(fieldScrProj, BorderLayout.CENTER);
            topPanel.add(buttonSrcProj, BorderLayout.EAST);

            //ajout au panneau principal
            mainPanel.add(topPanel, BorderLayout.NORTH);



            //panneau du bouton generate et logs
            JPanel middlePanel = new JPanel(new BorderLayout(5, 5));

            // Bouton generate
            JButton buttonGo = new JButton("Generate");
            buttonGo.setFont(new Font("SansSerif", Font.BOLD, 18));
            buttonGo.setPreferredSize(new Dimension(200, 50));

            // Configuration de la zone de texte pour les logs
            JTextArea logTextArea;
            logTextArea = new JTextArea();
            logTextArea.setEditable(false);
           // logTextArea.setBackground(Color.BLACK);
           // logTextArea.setForeground(Color.GREEN); // Style console rétro !

            JScrollPane scrollPane = new JScrollPane(logTextArea);
            scrollPane.setPreferredSize(new Dimension(500, 200));


            //  au panneau, puis le panneau à la fenêtre
            middlePanel.add(buttonGo,  BorderLayout.NORTH);
            middlePanel.add(scrollPane,  BorderLayout.CENTER);
                       
            mainPanel.add(middlePanel, BorderLayout.CENTER);

         mainFrame.add(mainPanel);
            // 5. Rendre la fenêtre visible
            mainFrame.setVisible(true);


            GuiAppender.setLogConsumer(messageDeLog -> {
            // Assure la mise à jour dans le Thread Graphique (EDT)
            SwingUtilities.invokeLater(() -> {
                logTextArea.append(messageDeLog);
                // Défilement automatique
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
                });
             });

            // Ajouter une action au clic du bouton
            buttonGo.addActionListener(e -> {
                // Désactiver le bouton pendant le traitement pour éviter les doubles clics
                buttonGo.setEnabled(false);
                new Thread(()->{
                    try 
                    {
                        startProgram(); 
                    }
                    finally
                    {
                        // Réactiver le bouton une fois le programme terminé
                        // SwingUtilities.invokeLater est requis car on modifie un composant GUI depuis un autre thread
                        SwingUtilities.invokeLater(() -> buttonGo.setEnabled(true));
                    }
                }).start();
                
            });

            
        
    }

    // Ouvre la boîte de dialogue et met à jour le champ texte
    private static void choiceDir( JTextField textField) {
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

    private static void setFont(Font police) {
        Enumeration<Object> cles = UIManager.getDefaults().keys();
        while (cles.hasMoreElements()) {
            Object cle = cles.nextElement();
            Object valeur = UIManager.get(cle);
            if (valeur instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(cle, new javax.swing.plaf.FontUIResource(police));
            }
        }
    }

    private static void startProgram()
    {
        System.out.println("*** Début traitement ***");
        try
        {
            String sourceClasses = Config.getROOTCLASSES();
            String out = Config.getOUTPUT();
            List<String> classes = Config.getCLASSES();
            String xml = Config.getXMLINJECTION();
    
            if (sourceClasses != null && out != null && !classes.isEmpty() )
            {
                /* Lister les classes et méthodes */
                TestClassGenerator testClassGenerator = new TestClassGenerator(sourceClasses, xml);
        
                /* générer les classes de test */
                System.out.println("*** Création des classes de test ***");
                for (String classe : classes)
                {
                    try
                    {
                        String source =  classe;
                        testClassGenerator.generate(new File(source), Paths.get(out));
                    }
                    catch (Exception e)
                    {
                        System.out.println("Erreur lors du traitement du fichier '" + classe + "' : " + e.getMessage());
                    }
                    
                }
            }
           
        }
        catch (Exception e)
        {
            System.out.println("Erreur générale d'initialisation : " + e.getMessage());
        }
        System.out.println("*** Fin traitement ***");
    }

    /*
     * private static void loadJframe() { // Création de la fenêtre principale JFrame frame = new
     * JFrame("Sélection de fichier"); frame.setSize(400, 200); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     * frame.setLayout(new FlowLayout()); // Alignement simple des composants frame.setLocationRelativeTo(null); // Centre
     * la fenêtre sur l'écran //bouton de sélection JButton bouton = new JButton("Fichier"); JLabel labelResultat = new
     * JLabel("Aucun fichier sélectionné"); bouton.addActionListener(new ActionListener() {
     * @Override public void actionPerformed(ActionEvent e) { // Création du sélecteur de fichiers JFileChooser explorateur
     * = new JFileChooser(); // Optionnel : Pour choisir uniquement des dossiers, décommentez la ligne suivante : //
     * explorateur.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Ouverture de la boîte de dialogue int resultat =
     * explorateur.showOpenDialog(frame); // Si l'utilisateur a validé un fichier if (resultat ==
     * JFileChooser.APPROVE_OPTION) { File fichierSelectionne = explorateur.getSelectedFile();
     * labelResultat.setText("Fichier : " + fichierSelectionne.getName()); } else {
     * labelResultat.setText("Sélection annulée."); } } }); frame.add(bouton); frame.add(labelResultat); // Rendre la
     * fenêtre visible frame.setVisible(true); }
     */
}
