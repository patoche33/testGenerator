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
        CONSTRUCTOR, FACTORY_METHOD, BUILDER, INTERFACE_PROXY
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
        InstantiationStrategy strategy = new InstantiationStrategy();

        // Cas 0 : Interfaces ou Classes Abstraites
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()))
        {
            strategy.type = StrategyType.INTERFACE_PROXY;
            return strategy;
        }

        // Cas 1 : Constructeur public sans argument (Le plus simple)
        try
        {
            Constructor<?> emptyConstructor = clazz.getConstructor();
            if (Modifier.isPublic(emptyConstructor.getModifiers()))
            {
                strategy.type = StrategyType.CONSTRUCTOR;
                strategy.parameters = new Parameter[0];
                return strategy;
            }
        }
        catch (NoSuchMethodException ignored)
        {
        }

        // Cas 2 : Constructeurs publics avec arguments
        Constructor<?>[] constructors = clazz.getConstructors();
        Optional<Constructor<?>> publicConstructor = Arrays.stream(constructors)
            .filter(c -> Modifier.isPublic(c.getModifiers()))
            .findFirst(); // On prend le premier disponible pour l'exemple

        if (publicConstructor.isPresent())
        {
            strategy.type = StrategyType.CONSTRUCTOR;
            strategy.parameters = publicConstructor.get().getParameters();
            return strategy;
        }

        // Cas 3 : Pas de constructeur public -> Recherche de Factory Methods (ex: LocalDate.now())
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
            return strategy;
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
            return strategy;
        }

        // Cas 4 : Recherche d'un Builder interne (Pattern Lombok / Builder classique)
        Optional<Method> builderMethod = Arrays.stream(methods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getName().equals("builder"))
            .findFirst();

        if (builderMethod.isPresent())
        {
            strategy.type = StrategyType.BUILDER;
            strategy.methodName = "builder";
            return strategy;
        }

        throw new IllegalArgumentException("Impossible de trouver un moyen d'instancier automatiquement : " + clazz.getName());
    }

    // Assigne un score de priorité aux noms de méthodes d'instanciation courantes
    private static int scoreFactoryName(String name)
    {
        return switch (name)
        {
            case "now", "current" -> 1; // Priorité maximale (LocalDate, Instant)
            case "getInstance" -> 2; // Commun (Calendar, Signatures)
            case "getDefault", "system" -> 3; // Fallbacks fréquents
            default -> 10;
        };
    }

}
