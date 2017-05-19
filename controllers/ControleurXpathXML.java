/**
 * Phase 3 du laboratoire de SER
 *
 * @author Thomas Léchaire
 * @author Cyril Balboni
 * @version 1.0
 */

package controllers;

/**
 * Imports
 */

import models.GlobalData;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import views.MainGUI;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

public class ControleurXpathXML {

    /**
     * Membres
     */
    private final static String xpathFileName = "XpathProjection.xml";  // fichier de sortie
    private static final DecimalFormat doubleFormat = new DecimalFormat("#.#");
    private GlobalData globalData;
    private final MainGUI mainGUI;

    /**
     * Constructeur
     *
     * @param doc
     * @param mainGUI
     */
    ControleurXpathXML(Document doc, MainGUI mainGUI) {
        Document xml = doc;
        this.mainGUI = mainGUI;
    }

    /**
     * Méthode permettant d'écrire dans un document
     *
     * @param filename nom du fichier de destination
     * @param doc      le contenu qui va être inséré dans le fichier
     */
    private static void writeToFile(String filename, Document doc) {

        try {
            XMLOutputter fichierXml = new XMLOutputter(Format.getPrettyFormat());
            fichierXml.output(doc, new FileOutputStream(filename));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode qui chronomètres la durée dans un intervall donné
     *
     * @param start début du chronométrage
     * @param end   fin du chronométrage
     * @return le nombre de secondes écoulées entre start et end
     */
    private static String displaySeconds(long start, long end) {

        long diff = Math.abs(end - start);
        double seconds = ((double) diff) / 1000.0;

        return doubleFormat.format(seconds) + " s";
    }

    /**
     * Méthode principale, point d'entrée lors du clic sur le bouton
     */
    public void createXmlWithXpath() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mainGUI.setAcknoledgeMessage("Creation XML from Xpath... WAIT");
                long currentTime = System.currentTimeMillis();
                try {
                    SAXBuilder builder = new SAXBuilder();
                    Document docToRead = builder.build("projections.xml");
                    Document doc = new Document();

                    // Ajout de la ligne pour la DTD
                    doc.addContent(new DocType("plex", "projections.dtd"));

                    // Ajout de la feuille de style
                    ProcessingInstruction pI = new ProcessingInstruction("xml-stylesheet");
                    HashMap<String, String> hm = new HashMap<String, String>();
                    hm.put("type", "text/xsl");
                    hm.put("href", "projections.xsl");
                    pI.setData(hm);
                    doc.addContent(pI);

                    // Définition de l'élément racine
                    Element root = new Element("plex");

                    // Requête et expression XPath sur la collection de projections
                    XPathFactory xPathFactory = XPathFactory.instance();
                    XPathExpression xPathExpression = xPathFactory.compile("//projections/projection",
                            Filters.element());
                    List<Element> projections = (List<Element>) (xPathExpression.evaluate(docToRead));

                    // Utilisation de HashMap pour éviter les doublons dans les références par ID
                    HashMap<String, String> lienFilmTitre = new HashMap<>();   // Lien film ID  <=> titre
                    HashMap<String, String> lienActeurRole = new HashMap<>();  // Lien acteur   <=> rôles
                    HashMap<String, String> lienLangageFilm = new HashMap<>(); // Lien langage  <=> film
                    HashMap<String, String> lienMotsCleFilm = new HashMap<>(); // Lien mot-clé  <=> film
                    HashMap<String, String> lienGenresFilm = new HashMap<String, String>(); // Lien genre    <=> film

                    // Ensembles situés à la racine
                    Element colProjections = new Element("projections");
                    Element colFilms = new Element("films");
                    Element colActeurs = new Element("acteurs");
                    Element colLangues = new Element("liste_langages");
                    Element colGenres = new Element("liste_genres");
                    Element colMotsCle = new Element("liste_mots_cles");

                    // Auto-incrément pour obtenir des cles unique pour chaque elements de la liste
                    int filmID, acteurID, langueID, genreID, motCleID;
                    filmID = acteurID = langueID = genreID = motCleID = 1;

                    // Boucle principale, traite chaque projection
                    for (Element pro : projections) {

                        /**
                         * FILM
                         */
                        Element elFilm = new Element("film");

                        // Le titre est la clé du hasmap
                        String filmTitle = pro.getChild("film").getAttributeValue("titre");

                        // Un film n'est généré que si il n'existe pas déjà
                        String sFilmID = "";
                        if (!lienFilmTitre.containsValue(filmTitle)) {

                            sFilmID = "F" + filmID;
                            lienFilmTitre.put(filmTitle, sFilmID);
                            filmID++;

                            // Attributs
                            elFilm.setAttribute("no", sFilmID);

                            // Noeuds
                            elFilm.addContent(new Element("titre").setText(pro.getChild("film").
                                    getAttributeValue("titre")));
                            elFilm.addContent(new Element("duree").setText(pro.getChild("film").
                                    getAttributeValue("duree")).setAttribute("format", "minutes"));
                            elFilm.addContent(new Element("synopsys").setText(pro.getChild("film").
                                    getChild("synopsis").getText()));
                            elFilm.addContent(new Element("photo")
                                    .setAttribute("url", "http://docr.iict.ch/imdb/" + pro.getChild("film")
                                            .getAttributeValue("film_id") + ".jpg"));

                            /**
                             * CRITIQUES
                             */
                            Element eCritiques = new Element("critiques");
                            for (Element critique : pro.getChild("film").getChild("critiques").
                                    getChildren("critique")) {
                                Element eCritique = new Element("critique");
                                eCritique.setAttribute("note", critique.getAttributeValue("note"));
                                eCritique.setText(critique.getAttributeValue("texte"));
                                eCritiques.addContent(eCritique);
                            }
                            elFilm.addContent(eCritiques);

                            /**
                             * LANGUES
                             */
                            Element elLangues = new Element("langages");
                            String languagesList = "";
                            String languageRef;

                            for (Element langue : pro.getChild("film").getChild("langages").getChildren()) {

                                String sLangue = langue.getAttributeValue("label");

                                // Une langue n'est générée que s'il elle n'existe pas déjà
                                if (!lienLangageFilm.containsKey(sLangue)) {
                                    languageRef = "L" + langueID;   // Reference unique
                                    lienLangageFilm.put(sLangue, languageRef); //Ajout de la langue au hashmap pour eviter les doublons

                                    Element elLangue = new Element("langage");
                                    elLangue.setAttribute("no", languageRef);
                                    elLangue.setText(sLangue);
                                    colLangues.addContent(elLangue);
                                    langueID++;
                                } else {
                                    // Obtention de la référence existante
                                    languageRef = lienLangageFilm.get(sLangue);
                                }

                                languagesList += languageRef + " ";
                            }

                            // Référence vers la liste des langues liées
                            elLangues.setAttribute(new Attribute("liste", languagesList, Attribute.IDREFS_TYPE));
                            elFilm.addContent(elLangues);

                            /**
                             * GENRES
                             */
                            Element elGenres = new Element("genres");
                            String genresList = "";
                            String genreRef;

                            for (Element genre : pro.getChild("film").getChild("genres").getChildren()) {
                                // Le nom du genre est la clé du hashmap
                                String sGenre = genre.getAttributeValue("label");

                                // Ajout uniquement si le genre n'est pas encore référencé
                                if (!lienGenresFilm.containsKey(sGenre)) {
                                    genreRef = "G" + genreID;   // Référence unique
                                    lienGenresFilm.put(sGenre, genreRef);
                                    genreID++;

                                    Element eGenre = new Element("genre");
                                    eGenre.setAttribute("no", genreRef);
                                    eGenre.setText(sGenre);
                                    colGenres.addContent(eGenre);
                                } else {
                                    // Obtention de la référence existante
                                    genreRef = lienGenresFilm.get(sGenre);
                                }
                                genresList += genreRef + " ";
                            }
                            // Références aux genres liés
                            elGenres.setAttribute(new Attribute("liste", genresList, Attribute.IDREFS_TYPE));
                            elFilm.addContent(elGenres);

                            /**
                             * MOTS-CLES
                             */
                            Element elMotsCles = new Element("mots_cles");
                            String motClesList = "";
                            String motCleRef;
                            for (Element motCle : pro.getChild("film").getChild("motCles").getChildren()) {

                                // Le label du mot-clé est la clé du hashmap
                                String motCleValue = motCle.getAttributeValue("label");

                                // Ajout uniquement si le mot clé n'est pas encore référencé
                                if (!lienMotsCleFilm.containsKey(motCleValue)) {
                                    motCleRef = "M" + motCleID;     // Référence unique
                                    lienMotsCleFilm.put(motCleValue, motCleRef);
                                    motCleID++;

                                    Element eMotCle = new Element("mot_cle");
                                    eMotCle.setAttribute("no", motCleRef);
                                    eMotCle.setText(motCleValue);
                                    colMotsCle.addContent(eMotCle);
                                } else {
                                    // Obtention de la référence existante
                                    motCleRef = lienMotsCleFilm.get(motCleValue);
                                }
                                motClesList += motCleRef + " ";
                            }
                            // Références aux mots-clés liés
                            elMotsCles.setAttribute(new Attribute("liste", motClesList, Attribute.IDREFS_TYPE));
                            elFilm.addContent(elMotsCles);

                            /**
                             * ACTEURS & ROLES
                             */
                            String formatDate = pro.getChild("film").getChild("acteurs").
                                    getAttributeValue("formatDate");
                            List<Element> acteurs = pro.getChild("film").getChild("acteurs").getChildren();
                            Element elRoles = new Element("roles");

                            for (Element acteur : acteurs) {

                                String nomActeur = acteur.getAttributeValue("nom");
                                String sActeurID = "";

                                // Ajout uniquemement si l'acteur n'est pas déjà référencé
                                if (!lienActeurRole.containsValue(nomActeur)) {
                                    sActeurID = "A" + acteurID; // Référence unique
                                    lienActeurRole.put(nomActeur, sActeurID);
                                    acteurID++;
                                } else {
                                    // Obtention de la référence existante
                                    sActeurID = lienActeurRole.get(nomActeur);
                                }

                                Element elActeur = new Element("acteur");
                                elActeur.setAttribute("no", sActeurID);
                                elActeur.addContent(new Element("nom").setText(nomActeur));
                                elActeur.addContent(new Element("nom_naissance")
                                        .setText(acteur.getAttributeValue("nomNaissance")));
                                elActeur.addContent(new Element("sexe")
                                        .setAttribute("valeur", acteur.getAttributeValue("sexe")));
                                elActeur.addContent(new Element("date_naissance")
                                        .setText(acteur.getAttributeValue("dateNaissance"))
                                        .setAttribute("format", formatDate));
                                elActeur.addContent(new Element("date_deces")
                                        .setText(acteur.getAttributeValue("dateDeces"))
                                        .setAttribute("format", formatDate));
                                elActeur.addContent(new Element("biographie")
                                        .setText(acteur.getAttributeValue("biographie")));

                                colActeurs.addContent(elActeur);

                                // Roles
                                Element elRole = new Element("role");
                                elRole.setAttribute("place", acteur.getChild("role").getAttributeValue("place"));
                                elRole.setAttribute("personnage", acteur.getChild("role").getAttributeValue("personnage"));
                                elRole.setAttribute("acteur_id", sActeurID);
                                elRoles.addContent(elRole);
                            }
                            elFilm.addContent(elRoles);

                        } else {
                            // Obtention de la référence existante
                            sFilmID = lienFilmTitre.get(filmTitle);
                        }
                        colFilms.addContent(elFilm);

                        /**
                         * FIN FILM
                         */

                        /**
                         * PROJECTION
                         */
                        pro.detach();  // Détache la racine de l'élément (par sécurité)
                        Element elProjection = new Element(pro.getName());

                        // Attributs
                        elProjection.setAttribute(new Attribute("film_id", sFilmID, Attribute.IDREF_TYPE));
                        elProjection.setAttribute("titre", filmTitle);

                        // Noeuds
                        Element salle = new Element("salle");
                        salle.addContent(pro.getAttributeValue("salle"));
                        salle.setAttribute(pro.getAttribute("taille").clone());
                        elProjection.addContent(salle);

                        Element date_heure = new Element("date_heure");
                        date_heure.addContent(pro.getAttributeValue("dateHeure"));
                        date_heure.setAttribute("format", "dd-MM-yyyy - HH:mm /24h");
                        elProjection.addContent(date_heure);

                        colProjections.addContent(elProjection);
                    }
                    // Fin de la boucle principale

                    // Regroupement des collections sous l'élément racine "plex"
                    root.addContent(colProjections);
                    root.addContent(colFilms);
                    root.addContent(colActeurs);
                    root.addContent(colLangues);
                    root.addContent(colGenres);
                    root.addContent(colMotsCle);
                    doc.addContent(root);

                    // Ecriture vers le fichier de sortie
                    writeToFile(xpathFileName, doc);

                    // Retour vers l'interface
                    mainGUI.setAcknoledgeMessage("XML from Xpath cree en " + displaySeconds(currentTime, System.currentTimeMillis()));
                } catch (Exception e) {
                    mainGUI.setErrorMessage("Construction Xpath impossible", e.toString());
                }
            }
        }).start();
    }
}