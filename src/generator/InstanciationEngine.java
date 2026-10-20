package generator;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ClassName;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Optional;

public class InstanciationEngine
{

    public enum StrategyType
    {
        CONSTRUCTOR, FACTORY_METHOD, BUILDER, INTERFACE_PROXY, NULL
    }

    public static class InstantiationStrategy
    {
        public StrategyType type;

        public String methodName; // Nom de la méthode si FACTORY_METHOD ou BUILDER

        public Parameter[] parameters; // Paramètres nécessaires

        @Override
        public String toString()
        {
            return "Stratégie: " + type + (methodName != null ? " (" + methodName + ")" : "");
        }
    }

    public static InstantiationStrategy discoverStrategy(Class<?> clazz)
    {
       

        // Cas Interfaces (Elles ne peuvent jamais avoir de Factory Methods retournant leur propre
        //type concret directement dans la classe même avant Java 8, et s'appuient généralement sur des Proxies)
        if (clazz.isInterface() )
        {
            return stategyProxy();
        }
        else if (!Modifier.isAbstract(clazz.getModifiers())) {  //CLASSES CONCRÈTES
          
            return stategyConstructor(clazz);
            
        }
        else
        {
            return strategyFactory(clazz); //classe abstraite
        }
        
        //throw new IllegalArgumentException("Impossible de trouver un moyen d'instancier automatiquement : " + clazz.getName());
    }

    // Assigne un score de priorité aux noms de méthodes d'instanciation courantes
    public static int scoreFactoryName(String name)
    {
        return switch (name)
        {
            case "now", "current" -> 1; // Priorité maximale (LocalDate, Instant)
            case "getInstance" -> 2; // Commun (Calendar, Signatures)
            case "getDefault", "system" -> 3; // Fallbacks fréquents
            default -> 10;
        };
    }
    
    public static InstantiationStrategy strategyFactory(Class<?> clazz)
    {
        InstantiationStrategy strategy = new InstantiationStrategy();
        // Cas TOUTES CLASSES (Abstraites et Concrètes sans constructeur public) : 
        //Recherche de Factory Methods (ex: TimeZone.getDefault(), LocalDate.now())
        
        Method[] methods = clazz.getMethods();

        // Priorité aux fabriques sans arguments typées "temps réel" ou globales (now, getInstance)
        Optional<Method> factoryNoArg = Arrays.stream(methods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getReturnType().equals(clazz))
            .filter(m -> m.getParameterCount() == 0)
            .min((m1, m2) -> scoreFactoryName(m1.getName()) - scoreFactoryName(m2.getName()));

        if (factoryNoArg.isPresent())
        {
            strategy.type = StrategyType.FACTORY_METHOD;
            strategy.methodName = factoryNoArg.get().getName();
            strategy.parameters = new Parameter[0];
        }

        // Recherche de fabriques avec arguments (ex: LocalDate.of(y, m, d) ou valueOf)
        Optional<Method> factoryWithArgs = Arrays.stream(methods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getReturnType().equals(clazz))
            .filter(m -> m.getName().equals("of") || m.getName().equals("valueOf"))
            .findFirst();

        if (factoryWithArgs.isPresent())
        {
            strategy.type = StrategyType.FACTORY_METHOD;
            strategy.methodName = factoryWithArgs.get().getName();
            strategy.parameters = factoryWithArgs.get().getParameters();
        }

        // Cas  Recherche d'un Builder interne (Pattern Lombok / Builder classique)
        Optional<Method> builderMethod = Arrays.stream(methods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getName().equals("builder"))
            .findFirst();

        if (builderMethod.isPresent())
        {
            strategy.type = StrategyType.BUILDER;
            strategy.methodName = "builder";
        }
        
        return strategy;

    }
    
    public static InstantiationStrategy stategyConstructor(Class<?> clazz)
    {
        InstantiationStrategy strategy = new InstantiationStrategy();
        try
        {
            Constructor<?> emptyConstructor = clazz.getConstructor();
            if (Modifier.isPublic(emptyConstructor.getModifiers()))
            {
                strategy.type = StrategyType.CONSTRUCTOR;
                strategy.parameters = new Parameter[0];
            }
        }
        catch (NoSuchMethodException ignored)
        {
            strategy.type = StrategyType.NULL;
        }

        // Cas Constructeurs publics avec arguments
        Constructor<?>[] constructors = clazz.getConstructors();
        Optional<Constructor<?>> publicConstructor = Arrays.stream(constructors)
            .filter(c -> Modifier.isPublic(c.getModifiers()))
            .findFirst(); 

        if (publicConstructor.isPresent())
        {
            strategy.type = StrategyType.CONSTRUCTOR;
            strategy.parameters = publicConstructor.get().getParameters();
        }
        
        return strategy;
    }
    
    public static InstantiationStrategy stategyProxy() {
        
        InstantiationStrategy strategy = new InstantiationStrategy();
        // Cas Interfaces (Elles ne peuvent jamais avoir de Factory Methods retournant leur propre
        //type concret directement dans la classe même avant Java 8, et s'appuient généralement sur des Proxies)
         strategy.type = StrategyType.INTERFACE_PROXY;
        return strategy;
       
    }
}
