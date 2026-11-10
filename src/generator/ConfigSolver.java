package generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import generator.config.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class ConfigSolver
{

    public static JavaSymbolSolver javaSymbolSolver;

    private static ParserConfiguration instance;

    private ConfigSolver()
    {
    }

    public static ParserConfiguration getParserConfiguration() throws IOException
    {
        if (instance == null)
        {
            instance = creerParserConfiguration();
        }
        return instance;
    }

    public static ParserConfiguration creerParserConfiguration() throws IOException
    {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();

        // Permet de résoudre les classes de base du JDK (String, File, Exception...)
        typeSolver.add(new ReflectionTypeSolver());

        // Permet de résoudre les classes du projet (fichiers .java locaux)
        // (ex: "src/main/java")
        File srcDirectory = new File(Config.getROOTCLASSES());
        if (srcDirectory.exists())
        {
            typeSolver.add(new JavaParserTypeSolver(srcDirectory));
        }

        // dependency autres
        for (String dependencie : Config.getSourceDependencies())
        {
            System.out.println("PARCOURS DE " + dependencie);
            Path m2 = Paths.get(dependencie); // source maven par exemple
            try (Stream<Path> flux = Files.walk(m2))
            {
                flux.filter(Files::isRegularFile) // Garder uniquement les fichiers
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .forEach(path -> {
                        try
                        {
                            typeSolver.add(new JarTypeSolver(path));
                            // System.out.println("MAVEn dependance " +path.toString());
                        }
                        catch (IOException e)
                        {
                            System.out.println("ERREUR dans ajout Type Resolver de MAVEN dependance ");
                        }
                    }); // Afficher le chemin

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration()
            .setAttributeComments(false);
        // .setAttributeComments(false)=> DESACTIVE L'INSERTION DES COMMENTAIRES DANS L'AST.
        // Sinon risque de tomber dans boucle infini et pb de memoire
        config.setSymbolResolver(symbolSolver);

        return config;

    }

    public static JavaParser getJavaParser() throws IOException
    {
        // ParserConfiguration config = creerParserConfiguration();

        ParserConfiguration config = ConfigSolver.getParserConfiguration();

        // Retourner l'instance configurée de JavaParser
        return new JavaParser(config);
    }
}
