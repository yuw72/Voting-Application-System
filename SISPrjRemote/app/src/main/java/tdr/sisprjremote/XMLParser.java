package tdr.sisprjremote;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by zhenjiangfan on 2016/12/15.
 */

public class XMLParser {
    public static KeyValueList getMessagesFromXML(File file){
        //File file = new File(path);
        InputStream is = null;
        KeyValueList kvList = new KeyValueList();
        try {
            is = new FileInputStream(file);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);

            Element element=doc.getDocumentElement();
            element.normalize();

            NodeList nList = doc.getElementsByTagName("Item");

            for (int i=0; i<nList.getLength(); i++) {
                Node node = nList.item(i);
                Element element2 = (Element) node;
                String key = getValue("Key", element2);
                String value = getValue("Value", element2);
                kvList.putPair(key,value);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return kvList;
    }
    private static String getValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);
        return node.getNodeValue();
    }
}
