package fr.testgenerator.ui.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

//"extends JFrame" fournit automatiquement la méthode setVisible(true)

public class MainWindow extends JFrame
{
    private JButton buttonSrcProj;

    private JButton buttonGo;

    private JTextField fieldScrProj;

    private JTextArea logTextArea;

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
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        buttonSrcProj = new JButton("Sélectionner le dossier source du projet");
        buttonSrcProj.setPreferredSize(new Dimension(160, 30)); // Taille fixe du bouton

        fieldScrProj = new JTextField();
        fieldScrProj.setEditable(false); // Empêche la saisie manuelle erronée
        fieldScrProj.setPreferredSize(new Dimension(400, 30));

        // ajout au panneau topPanel
        topPanel.add(new JLabel("Fichier sélectionné : "), BorderLayout.WEST);
        topPanel.add(fieldScrProj, BorderLayout.CENTER);
        topPanel.add(buttonSrcProj, BorderLayout.EAST);

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

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));

        // au panneau, puis le panneau à la fenêtre
        middlePanel.add(buttonGo, BorderLayout.NORTH);
        middlePanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(middlePanel, BorderLayout.CENTER);

        //Ajout du panneau à la fenêtre (setVisible(true) est géré par GuiLauncher)
        setContentPane(mainPanel);

    }

    public JButton getButtonSrcProj()
    {
        return buttonSrcProj;
    }

    public JButton getButtonGo()
    {
        return buttonGo;
    }

    public JTextField getFieldScrProj()
    {
        return fieldScrProj;
    }

    public JTextArea getLogTextArea()
    {
        return logTextArea;
    }

    /**
     * Définir la font globale DOCUMENTEZ_MOI
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

}
