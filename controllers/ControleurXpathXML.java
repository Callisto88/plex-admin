package controllers;

import models.GlobalData;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import views.MainGUI;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

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
                    SAXBuilder builder = new SAXBuilder();
                    Document docToRead = builder.build("projections.xml");
                    Document doc = new Document();

                    //ajout de la ligne pour la dtd
                    doc.addContent(new DocType("projections", "plex_admin.dtd"));
                    //ajout de la feuille de style
                    ProcessingInstruction pI = new ProcessingInstruction("xml-stylesheet");
                    HashMap<String,String> hm = new HashMap<String, String>();
                    hm.put("type","text/xsl");
                    hm.put("href","projections.xsl");
                    pI.setData(hm);
                    doc.addContent(pI);

                    doc.setRootElement(new Element("plex"));
                    XPathFactory xPathFactory = XPathFactory.instance();
                    XPathExpression xPathExpression = xPathFactory.compile("/projections//projection", Filters.element());
                    List<Element> resultat = (List<Element>) (xPathExpression.evaluate(docToRead));

                    //XPathExpression xPathExpression_roles = xPathFactory.compile("/projections/projection/film//role");
                    //List<Element> resultatRole = (List<Element>) (xPathExpression.evaluate(docToRead));
                    String formatDate = "28-04-2017 - 00:33";
                    Element projections = new Element("projections");
                    //résultat des projections
                    int i = 0;
                    for (Element elem: resultat) {

                        elem.detach();

                        Element projection = new Element(elem.getName());
                        projection.setAttribute("film_id", "F" + i);
                        projection.setAttribute(elem.getChild("film").getAttribute("titre").clone());
                        projection.addContent(populateSalle(elem));
                        projection.addContent(populateDateHeure(elem));

                        projections.addContent(projection);
                        i++;
                    }

                    Element films = new Element("films");

                    //resultat des films
                    for (Element elem : resultat){
                        //Creation de la liste des films
                        elem.detach();
                        Element film = elem.getChild("film").clone();
                        films.addContent(populateFilms(film));
                        //System.out.println(elem.getAttribute("film").toString());

                    }

                    projections.addContent(films);

                    doc.getRootElement().addContent(projections);
                    writeToFile(xpathFileName,doc);

                    mainGUI.setAcknoledgeMessage("XML from Xpath cree en "+ displaySeconds(currentTime, System.currentTimeMillis()) );


                }catch (Exception e){
                    mainGUI.setErrorMessage("Construction Xpath impossible", e.toString());
                }
            }
        }).start();
    }

    private Element populateFilms(Element movie) {
        Element film = new Element("film");
        film.addContent(new Element("titre").setText(movie.getAttributeValue("titre")));
        film.addContent(new Element("duree").setText(movie.getAttributeValue("duree")));
        film.addContent(movie.getChild("synopsis").clone());
        film.addContent(new Element("photo").setText(movie.getAttributeValue("photo")));

        //les critiques sont déjà dans le bon format
        film.addContent(movie.getChild("critiques").clone());
        film.addContent(movie.getChild("langages").clone());
        film.addContent(movie.getChild("genres").clone());
        film.addContent(movie.getChild("motCles").clone());

        //TODO trouver un moyen d'ajouter le rôles peut-être avec Xpath
        //film.addContent(populateRoles(movie));

        return film;
    }

    private Element populateRoles(Element movie) {
        Element roles = new Element("roles");

        List<Element> listElement = movie.getChildren("acteur");

        for(Element acteur: listElement){
            acteur.detach();
            Element role = acteur.getChild("role").clone();
            //role.setAttribute("acteur_id", acteur.getAttributeValue("acteur_id"));
        }

        return roles;
    }

    private Element populateSalle(Element elem) {
        Element salle = new Element("salle");
        salle.addContent(elem.getAttributeValue("salle"));
        //.clone() detache le parent
        salle.setAttribute(elem.getAttribute("taille").clone());
        return salle;
    }

    private Element populateDateHeure(Element elem) {
        Element date_heure = new Element("date_heure");
        date_heure.addContent(elem.getAttributeValue("dateHeure"));
        date_heure.setAttribute("format","dd-MM-yyyy - HH:mm /24h");
        return date_heure;
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
