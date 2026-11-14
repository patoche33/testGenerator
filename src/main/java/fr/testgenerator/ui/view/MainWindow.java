package fr.testgenerator.ui.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Enumeration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

//"extends JFrame" fournit automatiquement la méthode setVisible(true)

public class MainWindow extends JFrame
{
    private JButton buttonSrcProj;

    private JButton buttonSrcClas;

    private JButton buttonSrcOut;
    
    private JButton buttonGo;

    private JTextField fieldScrProj;
    
    private JTextField fieldScrClas;

    private JTextArea logTextArea;

    private JTextField fieldScrOut;
    
    private JProgressBar progressBar;

    public MainWindow()
    {
        super("Générateur de tests unitaires");
        applyGlobalFont(new Font("SansSerif", Font.PLAIN, 16));
        init();

    }

    private void init()
    {
        setFont(new Font("SansSerif", Font.PLAIN, 20));
        System.out.println("*** Démarrage intefrace graphique ***");
        // Lancer l'interface en toute sécurité sur le fil d'exécution Swing

        // Configuration fenêtre principale
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrer la fenêtre
        
        // panneau principal avec marges de 10px
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // panneau des zones de texte et boutons de sélecion
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Configuration des répertoires"));

        // HAUT GridBagLayout pour un alignement parfait)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Espacement autour de chaque élément
        gbc.fill = GridBagConstraints.HORIZONTAL;


        buttonSrcProj = new JButton();
        //buttonSrcProj.setPreferredSize(new Dimension(160, 30)); // Taille fixe du bouton
        fieldScrProj = new JTextField();
        //fieldScrProj.setEditable(false); // Empêche la saisie manuelle erronée
        //fieldScrProj.setPreferredSize(new Dimension(400, 30));

        buttonSrcClas= new JButton();
        //buttonSrcClas.setPreferredSize(new Dimension(160, 30)); // Taille fixe du bouton
        fieldScrClas = new JTextField();
        //fieldScrClas.setEditable(false); // Empêche la saisie manuelle erronée
        //fieldScrClas.setPreferredSize(new Dimension(400, 30));

        buttonSrcOut= new JButton();
        //buttonSrcClas.setPreferredSize(new Dimension(160, 30)); // Taille fixe du bouton
        fieldScrOut = new JTextField();
        //fieldScrClas.setEditable(false); // Empêche la saisie manuelle erronée
        //fieldScrClas.setPreferredSize(new Dimension(400, 30));

        // Ajout des 3 lignes simples (lignes 0, 1, 2)
        addSingleRow(topPanel, gbc, 0, "Dossier racine projet :", fieldScrProj, buttonSrcProj);
        addSingleRow(topPanel, gbc, 1, "Dossier des classes :", fieldScrClas, buttonSrcClas);
        addSingleRow(topPanel, gbc, 2, "Dossier de sortie tests :", fieldScrOut, buttonSrcOut);

        areaMulti1 = new JTextArea(3, 30); // 3 lignes visibles par défaut
        buttonMulti1 = new JButton("Ajouter dossier(s)...");

        areaMulti2 = new JTextArea(3, 30);
        buttonMulti2 = new JButton("Ajouter dossier(s)...");

        areaMulti3 = new JTextArea(3, 30);
        buttonMulti3 = new JButton("Ajouter dossier(s)...");

        // Ajout des 3 lignes simples (lignes 0, 1, 2)
        addSingleRow(topPanel, gbc, 0, "Dossier racine projet :", fieldSingle1, buttonSingle1);
        addSingleRow(topPanel, gbc, 1, "Dossier des classes :", fieldSingle2, buttonSingle2);
        addSingleRow(topPanel, gbc, 2, "Dossier de sortie tests :", fieldSingle3, buttonSingle3);

        // Ajout des 3 lignes multi-sélections (lignes 3, 4, 5)
        addMultiRow(topPanel, gbc, 3, "Dépendances / Libs :", areaMulti1, buttonMulti1);
        addMultiRow(topPanel, gbc, 4, "Sources externes :", areaMulti2, buttonMulti2);
        addMultiRow(topPanel, gbc, 5, "Dossiers à exclure :", areaMulti3, buttonMulti3);


        // ajout au panneau principal
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // panneau du bouton generate et logs
        JPanel middlePanel = new JPanel(new BorderLayout(5, 5));

        // Bouton generate
        buttonGo = new JButton("Generate");
        buttonGo.setFont(new Font("SansSerif", Font.BOLD, 18));
        buttonGo.setPreferredSize(new Dimension(200, 50));

        // Configuration de la zone de texte pour les logs
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        // logTextArea.setBackground(Color.BLACK);
        // logTextArea.setForeground(Color.GREEN); // Style console rétro !

        //barre de progression
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true); // Affiche le pourcentage (ex: "45%")
        progressBar.setString("En attente...");
        progressBar.setPreferredSize(new Dimension(500, 25));
       
        
        
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));

        //ajouter au panneau middle
        middlePanel.add(buttonGo, BorderLayout.NORTH);
        middlePanel.add(scrollPane, BorderLayout.CENTER);
        middlePanel.add(progressBar, BorderLayout.SOUTH);

        //ajouter au panneau principal
        mainPanel.add(middlePanel, BorderLayout.CENTER);

        //Ajout du panneau à la fenêtre (setVisible(true) est géré par GuiLauncher)
        setContentPane(mainPanel);

    }
    
    //getter des composants
    public JButton getButtonSrcProj()
    {
        return buttonSrcProj;
    }

    public JButton getButtonSrcClas()
    {
        return buttonSrcClas;
    }

    public JButton getButtonSrcOut()
    {
        return buttonSrcOut;
    }

    public JButton getButtonGo()
    {
        return buttonGo;
    }

    public JTextField getFieldScrProj()
    {
        return fieldScrProj;
    }

    public JTextField getFieldScrClas()
    {
        return fieldScrProj;
    }

     public JTextField getFieldScrCOut()
    {
        return fieldScrOut;
    }

    public JTextArea getLogTextArea()
    {
        return logTextArea;
    }
    
    public JProgressBar getProgressBar()
    {
        return progressBar;
    }
    
    /**
     * Définir la font globale 
     *
     * @param police
     */
    private void applyGlobalFont(Font police)
    {
        Enumeration<Object> cles = UIManager.getDefaults().keys();
        while (cles.hasMoreElements())
        {
            Object cle = cles.nextElement();
            Object valeur = UIManager.get(cle);
            if (valeur instanceof javax.swing.plaf.FontUIResource)
            {
                UIManager.put(cle, new javax.swing.plaf.FontUIResource(police));
            }
        }
    }

     /**
     * Ajoute une ligne avec un JLabel, un JTextField simple et un JButton.
     */
    private void addSingleRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextField textField, JButton button)
    {
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

         // Colonne 0 : Label
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(190, 30));
        panel.add(label, gbc);
        
        // Colonne 1 : Bouton
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        button.setPreferredSize(new Dimension(210, 30));
        panel.add(button, gbc);

      

        // Colonne 2 : Champ texte (prend toute la largeur disponible)
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        textField.setEditable(false);
        textField.setPreferredSize(new Dimension(400, 30));
        panel.add(textField, gbc);

        
    }

     /**
     * Ajoute une ligne avec un JLabel, un JTextArea multi-lignes avec ascenseur et un JButton.
     */
    private void addMultiRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextArea textArea, JButton button)
    {
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.NORTHWEST; // Aligne le label et le bouton en haut de la zone multi-ligne

        // Colonne 0 : Label
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(190, 30));
        panel.add(label, gbc);

        // Colonne 1 : Zone de texte multi-lignes dans un JScrollPane
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 65)); // Hauteur suffisante pour 3 lignes
        panel.add(scrollPane, gbc);

        // Colonne 2 : Bouton
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        button.setPreferredSize(new Dimension(210, 30));
        panel.add(button, gbc);
    }

}
