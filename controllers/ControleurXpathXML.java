/**
 *
 * @author Thomas Léchaire
 * @author Cyril Balboni
 * @version 1.0
 */

package controllers;

import models.GlobalData;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.ProcessingInstruction;

import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import views.MainGUI;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 *
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

                    // Ajout de la ligne pour la dtd
                    doc.addContent(new DocType("projections", "plex_admin.dtd"));

                    // Ajout de la feuille de style
                    ProcessingInstruction pI = new ProcessingInstruction("xml-stylesheet");
                    HashMap<String,String> hm = new HashMap<String, String>();
                    hm.put("type","text/xsl");
                    hm.put("href","projections.xsl");
                    pI.setData(hm);
                    doc.addContent(pI);

                    doc.setRootElement(new Element("plex"));    //Elément racine
                    XPathFactory xPathFactory = XPathFactory.instance();
                    XPathExpression xPathExpression = xPathFactory.compile("/projections//projection", Filters.element());
                    List<Element> resultat = (List<Element>) (xPathExpression.evaluate(docToRead));

                    HashMap<String, String> lienFilmTitre = new HashMap<>(); //Lien entre les id des films et leur titres
                    HashMap<String, String> lienActeurRole = new HashMap<>(); //Lien entre les acteur et les rôles
                    HashMap<String, String> lienLangageFilm = new HashMap<>(); //Lien entre langage et film

                    // Elément contenant l'ensemble des projections
                    Element projections = new Element("projections");

                    int i = 0;  // Incrément pour les identifants
                    for (Element elem: resultat) {
                        elem.detach();  //détache la racine de l'élément (par sécurité)
                        String film_id;

                        // Plus besoin
                        //Attribute titre = elem.getChild("film").getAttribute("titre").clone();

                        String titreToString = elem.getChild("film").getAttributeValue("titre");
                        Element projection = new Element(elem.getName());

                        // Vérifier que le titre n'as pas déjà d'identifiant
                        if(!lienFilmTitre.containsValue(titreToString)){
                            //si le titre n'existe pas on l'ajoute
                            film_id = "F" + i;
                            lienFilmTitre.put(titreToString, film_id);
                            i++; //on augmente i qui sont on a ajouté un film
                        }else{
                            //sinon on récupère l'id qui lui est déjà attribué
                            film_id = lienFilmTitre.get(titreToString);
                        }

                        projection.setAttribute("film_id", film_id);
                        projection.setAttribute(elem.getChild("film").getAttribute("titre").clone());
                        projection.addContent(populateSalle(elem));
                        projection.addContent(populateDateHeure(elem));
                        projections.addContent(projection);
                    }

                    // Construit la liste des films et des acteurs
                    Element films = new Element("films");
                    Element acteurs = new Element("acteurs");
                    Element langages = new Element("langages");
                    Element genres = new Element("genres");
                    Element motCles = new Element("motCles");

                    int acteur_id = 0;
                    int langue_id = 0;
                    for (Element elem : resultat){
                        elem.detach();
                        Element film = elem.getChild("film").clone();
                        Element acteur = elem.getChild("film").getChild("acteurs").clone();
                        Element langues = elem.getChild("film").getChild("langages").clone();
                        String titre = film.getAttributeValue("titre");
                        String nom = acteur.getAttributeValue("nom");

                        ArrayList<ArrayList<Element>> acteurs_roles = populateActeurs(acteur, lienActeurRole, acteur_id);
                        acteurs.addContent(acteurs_roles.get(0));
                        //création de la liste des langues avec id
                        langages.addContent(populateLangages(langues, lienLangageFilm, langue_id));

                        // Création des films avec id
                        films.addContent(populateFilms(film, lienFilmTitre.get(titre), acteurs_roles.get(1)));

                        // Création de la liste des langues avec id
                        langages.addContent(populateLangages(elem.getChild("film").getChild("langages").getChildren()));
                        acteur_id = acteur.getContentSize();
                        langue_id = langues.getContentSize();

                    }

                    projections.addContent(films); //ajoute la liste des films
                    projections.addContent(acteurs); //ajoute la liste des acteurs
                    projections.addContent(langages);

                    //TODO ajouter la liste des MotClef
                    //TODO ajouter la liste des acteurs
                    //TODO ajouter la liste des roles

                    doc.getRootElement().addContent(projections);   // Ajoute les projections à PLEX
                    writeToFile(xpathFileName,doc);

                    // Retour vers l'interface
                    mainGUI.setAcknoledgeMessage("XML from Xpath cree en "+ displaySeconds(currentTime, System.currentTimeMillis()) );
                }catch (Exception e){
                    mainGUI.setErrorMessage("Construction Xpath impossible", e.toString());
                }
            }
        }).start();
    }

    private List<Element> populateLangages(List<Element> listLangages) {
        ArrayList<Element> langages = new ArrayList<>();

        for (Element langue: listLangages){

        }

        return langages;
    }

    private Element populateFilms(Element movie, String film_id, ArrayList<Element> roles) {
        Element film = new Element("film").setAttribute("no", film_id);
        film.addContent(new Element("titre").setText(movie.getAttributeValue("titre")));
        film.addContent(new Element("duree").setText(movie.getAttributeValue("duree")).setAttribute("format", "minutes"));
        film.addContent(movie.getChild("synopsis").clone());
        film.addContent(new Element("photo").setAttribute("url", "http://docr.iict.ch/imdb/" + movie.getAttributeValue("film_id")+".jpg"));

        //les critiques doivent être dans le bon format
        List<Element> listCritique = movie.getChild("critiques").getChildren();
        film.addContent(populateCritiques(listCritique));
        List<Element> listLangages = movie.getChild("langages").getChildren();
        //film.addContent(movie.getChild("langages").clone());
        List<Element> listGenres = movie.getChild("genres").getChildren();
        //film.addContent(movie.getChild("genres").clone());
        List<Element> listMotCles = movie.getChild("motCles").getChildren();
        //film.addContent(movie.getChild("motCles").clone());

        film.addContent(roles);
        //film.addContent(langages);

        return film;
    }

    private List<Element> populateLangages(Element listLangages, HashMap<String, String> lienLangageFilm, int id) {
        ArrayList<Element> langages = new ArrayList<>();
        String langue_id;
        String langue;


        for (Element sprache: listLangages.getChildren()){

            langue = sprache.getAttributeValue("label");


            //Vérifier que le titre n'as pas déjà d'identifiant
            if(!lienLangageFilm.containsValue(langue)){
                //si le titre n'existe pas on l'ajoute
                langue_id = "L" + id;
                lienLangageFilm.put(langue, langue_id);
                System.out.println(langue);
                langages.add(new Element("langue").setAttribute("no",langue_id).setText(langue));
                id++; //on augmente i qui sont on a ajouté un film
            }
        }

        return langages;
    }

    private ArrayList<ArrayList<Element>> populateActeurs(Element act, HashMap<String, String> lienIdActeur, int id){
        ArrayList<Element> acteurs  = new ArrayList<>();
        ArrayList<Element> roles = new ArrayList<>();
        ArrayList<ArrayList<Element>> acteur_roles = new ArrayList<>();

        String formatDate = act.getAttributeValue("formatDate");
        for (Element element: act.getChildren()){
            String nom = element.getAttributeValue("nom");
            String acteur_id;

            //Vérifier que le titre n'as pas déjà d'identifiant
            if(!lienIdActeur.containsValue(nom)){
                //si le titre n'existe pas on l'ajoute
                acteur_id = "A" + id;
                lienIdActeur.put(nom, acteur_id);
                id++; //on augmente i qui sont on a ajouté un film
            }else{
                //sinon on récupère l'id qui lui est déjà attribué
                acteur_id = lienIdActeur.get(nom);
            }

            acteurs.add(new Element("acteur")
                            .setAttribute("no", acteur_id)
                            .addContent(
                                    new Element("nom")
                                            .setText(nom)
                            )
                            .addContent(
                                    new Element("nom_naissance")
                                            .setText(element.getAttributeValue("nomNaissance"))
                            )
                            .addContent(
                                    new Element("sexe")
                                            .setAttribute("valeur", element.getAttributeValue("sexe"))
                            )
                            .addContent(
                                    new Element("date_naissance")
                                            .setText(element.getAttributeValue("dateNaissance"))
                                            .setAttribute("format",formatDate)
                            )
                            .addContent(
                                    new Element("date_deces")
                                            .setText(element.getAttributeValue("dateDeces"))
                                            .setAttribute("format",formatDate)
                            )
                            .addContent(
                                    new Element("biographie")
                                            .setText(element.getAttributeValue("biographie")))
                            );

            roles.add(element.getChild("role").setAttribute("acteur_id", acteur_id).clone());
        }
        acteur_roles.add(acteurs);
        acteur_roles.add(roles);
        return acteur_roles;
    }


    private Element populateCritiques(List<Element> listeCritiques){
        Element critiques = new Element("critiques");

        //List<Element> listCritique = listeCritiques.getChildren();
        for (Element critique: listeCritiques){
            critiques.addContent(new Element("critique")
                    .setText(critique.getAttributeValue("texte"))
                    .setAttribute("note", critique.getAttributeValue("note")));
        }

        return critiques;
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
