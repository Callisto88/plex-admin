package controllers;

import com.thoughtworks.xstream.XStream;
import models.GlobalData;
import org.jaxen.dom.DocumentNavigator;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import views.MainGUI;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Created by Thomas on 05.05.2017.
 */
public class ControleurXpathXML {

    private Document xml;
    private GlobalData globalData;
    private MainGUI mainGUI;

    private final static String xpathFileName = "XpathProjection.xml";

    ControleurXpathXML(Document doc, MainGUI mainGUI){
        xml = doc;
        this.mainGUI = mainGUI;
        //createXmlWithXpath();
    }

    public void createXmlWithXpath(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                mainGUI.setAcknoledgeMessage("Creation XML from Xpath... WAIT");
                long currentTime = System.currentTimeMillis();
                try{
                    Document doc = new Document();

                    DocumentNavigator documentNavigator = new DocumentNavigator();

                    documentNavigator.getAttributeName(xml);

                    //ajout de la ligne pour la dtd
                    doc.addContent(new DocType("projections", "plex_admin.dtd"));
                    //ajout de la feuille de style
                    ProcessingInstruction pI = new ProcessingInstruction("xml-stylesheet");
                    HashMap<String,String> hm = new HashMap<String, String>();
                    hm.put("type","text/xsl");
                    hm.put("href","projections.xsl");
                    pI.setData(hm);
                    doc.addContent(pI);

                    writeToFile(xpathFileName,doc);

                    mainGUI.setAcknoledgeMessage("XML from Xpath cree en "+ displaySeconds(currentTime, System.currentTimeMillis()) );


                }catch (Exception e){
                    mainGUI.setErrorMessage("Construction Xpath impossible", e.toString());
                }
            }
        }).start();
    }

    private static void writeToFile(String filename, Document doc){
        try {
            XMLOutputter fichierXml = new XMLOutputter(Format.getPrettyFormat());
            fichierXml.output(doc,new FileOutputStream(filename));
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private static final DecimalFormat doubleFormat = new DecimalFormat("#.#");
    private static final String displaySeconds(long start, long end) {
        long diff = Math.abs(end - start);
        double seconds = ((double) diff) / 1000.0;
        return doubleFormat.format(seconds) + " s";
    }
}
