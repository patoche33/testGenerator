package fr.testgenerator.logging;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;
import java.util.function.Consumer;

@Plugin(name = "GuiAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class GuiAppender extends AbstractAppender {

    // Le callback qui recevra le message textuel formaté
    private static Consumer<String> logConsumer;

    protected GuiAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, null);
    }

    public static void setLogConsumer(Consumer<String> consumer) {
        logConsumer = consumer;
    }

    @Override
    public void append(LogEvent event) {
        if (logConsumer != null) {
            // Utilise le layout configuré dans le log4j2.xml (ex: pattern)
            String message = new String(getLayout().toByteArray(event));
            logConsumer.accept(message);
        }
    }

    // La factory requise par Log4j2 pour instancier l'appender depuis le XML
    @PluginFactory
    public static GuiAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        
        if (name == null) {
            LOGGER.error("Aucun nom spécifié pour GuiAppender");
            return null;
        }
        if (layout == null) {
            LOGGER.error("Aucun layout spécifié pour GuiAppender");
            return null;
        }
        return new GuiAppender(name, filter, layout, ignoreExceptions);
    }
}
