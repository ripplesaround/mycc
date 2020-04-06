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


// 需要在config中填写路径
public class main {
    public static void main(String[] args) throws Exception {
        readConfig();

    }

    private static void readConfig() throws Exception {
        // 声明新方法
        scan myscan = new scan();

        String temp_str = "";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse("./config.xml");
        NodeList nodeList = doc.getElementsByTagName("phase");
        System.out.println(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++){
            Element temp = (Element) nodeList.item(i);
            String name = temp.getAttribute("name");
            if(name.equals("scan")) {
                if(temp.getAttribute("skip").equals("false")) {
                    temp_str = temp.getAttribute("path").split("\\.")[0];
                    myscan.readfile(temp.getAttribute("path"));
                }
                else{
                    System.out.println("skip scanning step");
                }
            }
            else if(name.equals("parse")){
                if(temp.getAttribute("skip").equals("false")) {
                    System.out.println("parse");
                    if(temp.getAttribute("path").length()==0){
                        System.out.println(temp_str.concat(".tokens"));
                    }
                    else{
                        System.out.println(temp.getAttribute("path"));
                    }
                }
                else{
                    System.out.println("skip parsing step");
                }
            }

        }
    }


}
