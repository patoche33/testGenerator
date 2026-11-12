package generator.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlInjection
{
    /**
     * Parse le XML pour lister les attributs 'name' des balises <property> pour un bean donné.
     */
    public static Map<String, List<String>> extraireNomsProprietesDepuisXml(String xmlPath ) throws Exception
    {
        Map<String, List<String>> xmlProperties = new HashMap<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(xmlPath));

        NodeList beanNodes = doc.getElementsByTagName("bean");
        for (int i = 0; i < beanNodes.getLength(); i++)
        {
            Element beanElement = (Element) beanNodes.item(i);
            if (!beanElement.getAttribute("class").isEmpty())
            {   
                List<String> prop = new ArrayList<String>();
                NodeList propertyNodes = beanElement.getElementsByTagName("property");
                for (int j = 0; j < propertyNodes.getLength(); j++)
                {
                    Element propElement = (Element) propertyNodes.item(j);
                    prop.add(propElement.getAttribute("name"));
                }

                // Optionnel : Gérer aussi l'injection par constructeur <constructor-arg name="..."/>
                NodeList constNodes = beanElement.getElementsByTagName("constructor-arg");
                for (int k = 0; k < constNodes.getLength(); k++)
                {
                    Element constElement = (Element) constNodes.item(k);
                    if (constElement.hasAttribute("name"))
                    {
                        prop.add(constElement.getAttribute("name"));
                    }  
                }  
                
                xmlProperties.put(beanElement.getAttribute("class"), prop);
            }
        }
        return xmlProperties;
    }
//    public static Set<String> extraireNomsProprietesDepuisXml(String xmlPath, String targetClass) throws Exception
//    {
//        Set<String> nomsProprietes = new HashSet<>();
//        
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        Document doc = builder.parse(new File(xmlPath));
//
//        NodeList beanNodes = doc.getElementsByTagName("bean");
//        for (int i = 0; i < beanNodes.getLength(); i++)
//        {
//            Element beanElement = (Element) beanNodes.item(i);
//
//            if (targetClass.equals(beanElement.getAttribute("class")))
//            {
//                NodeList propertyNodes = beanElement.getElementsByTagName("property");
//                for (int j = 0; j < propertyNodes.getLength(); j++)
//                {
//                    Element propElement = (Element) propertyNodes.item(j);
//                    nomsProprietes.add(propElement.getAttribute("name"));
//                }
//
//                // Optionnel : Gérer aussi l'injection par constructeur <constructor-arg name="..."/>
//                NodeList constNodes = beanElement.getElementsByTagName("constructor-arg");
//                for (int k = 0; k < constNodes.getLength(); k++)
//                {
//                    Element constElement = (Element) constNodes.item(k);
//                    if (constElement.hasAttribute("name"))
//                    {
//                        nomsProprietes.add(constElement.getAttribute("name"));
//                    }
//                }
//            }
//        }
//        return nomsProprietes;
//    }
}
