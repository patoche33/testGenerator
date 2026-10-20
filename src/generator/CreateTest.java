package generator;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.StringUtils;

import generator.TestClassGenerator;
import generator.config.Config;

public class CreateTest
{

    public static void main(String[] args) throws IOException
    {
        // interface graphique swing
        // loadJframe();

        System.out.println("*** Début traitement ***");
        try
        {
            String sourceProjet = Config.getSOURCEPROJECT();
            String out = Config.getOUTPUT();
            List<String> classes = Config.getCLASSES();
    
            if (sourceProjet != null && out != null && !classes.isEmpty() )
            {
                /* Lister les classe set méthodes */
                TestClassGenerator testClassGenerator = new TestClassGenerator(sourceProjet);
        
                /* générer les classes de test */
                System.out.println("*** Création des classes de test ***");
                for (String classe : classes)
                {
                    // System.out.println("CLASSE=" +classe);
                    String source =  classe;
                    testClassGenerator.generate(new File(source), Paths.get(out));
                }
            }
           
        }
        catch (Exception e)
        {
            System.out.println("Traitement interrompu. "+e.getMessage());
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
