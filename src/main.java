import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NameList;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.*;

public class main {
    public static void main(String[] args) throws Exception {
        readConfig();

    }

    private static void readConfig() throws Exception {
        scan myscan = new scan();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse("./config.xml");

        NodeList nodeList = doc.getElementsByTagName("phase");
        System.out.println(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++){
            Element temp = (Element) nodeList.item(i);
            String name = temp.getAttribute("name");
            if(name.equals("scan")) {
                if(temp.getAttribute("skip") == "false") {
                    myscan.readfile(temp.getAttribute("path"));
                }
                else{
                    System.out.println("skip scanning step");
                }

            }

        }
    }


}
