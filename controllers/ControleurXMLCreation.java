package controllers;

import models.*;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import views.*;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//import org.dom4j.*;
//import org.dom4j.io.OutputFormat;
//import org.dom4j.io.XMLWriter;

import com.thoughtworks.xstream.XStream;

public class ControleurXMLCreation {

	//private ControleurGeneral ctrGeneral;
	private static MainGUI mainGUI;
	private ORMAccess ormAccess;
	private Document xmlDoc;

	private GlobalData globalData;

    private static final String XML_FILENAME = "projections.xml";

	public ControleurXMLCreation(ControleurGeneral ctrGeneral, MainGUI mainGUI, ORMAccess ormAccess){
		//this.ctrGeneral=ctrGeneral;
		ControleurXMLCreation.mainGUI=mainGUI;
		this.ormAccess=ormAccess;
	}

	public void createXML(){
		new Thread(){
			public void run(){
				mainGUI.setAcknoledgeMessage("Creation XML... WAIT");
				long currentTime = System.currentTimeMillis();
				try {

						/*Création du fichier XML*/
					Document doc = new Document();

					//récupération de la liste des projection
					globalData = ormAccess.GET_GLOBAL_DATA();
					List<Projection> liste_projections = globalData.getProjections();

					//élément racine
					Element element = new Element("projections");
					//indication du format date Heure pour les projections
					element.setAttribute("formatDateHeure", "dd-MM-yyyy - HH:mm /24h");


					//Parcours de la liste des projections
					for (Projection pro :liste_projections) {
						element.addContent(
								populatProjection(pro). //ajoute les projections
										addContent(
										populateFilm(pro)		//ajoute le film de la projection
								));
					}

					doc.setRootElement(element);
					writeToFile(XML_FILENAME, doc);
					xmlDoc = doc; //enregistrement du nouveau fichier
					mainGUI.setAcknoledgeMessage("XML cree en "+ displaySeconds(currentTime, System.currentTimeMillis()) );

				}
				catch (Exception e){
					mainGUI.setErrorMessage("Construction XML impossible", e.toString());
				}
			}
		}.start();
	}

	public void createXStreamXML(){
		new Thread(){
			public void run(){
				mainGUI.setAcknoledgeMessage("Creation XML... WAIT");
				long currentTime = System.currentTimeMillis();
				try {
					globalData = ormAccess.GET_GLOBAL_DATA();
					globalDataControle();
				}
				catch (Exception e){
					mainGUI.setErrorMessage("Construction XML impossible", e.toString());
				}

				XStream xstream = new XStream();
				writeToFile("global_data.xml", xstream, globalData);
				System.out.println("Done [" + displaySeconds(currentTime, System.currentTimeMillis()) + "]");
				mainGUI.setAcknoledgeMessage("XML cree en "+ displaySeconds(currentTime, System.currentTimeMillis()) );
			}
		}.start();
	}

	private static void writeToFile(String filename, XStream serializer, Object data) {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
			serializer.toXML(data, out);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	private void globalDataControle(){
		for (Projection p:globalData.getProjections()){
			System.out.println("******************************************");
			System.out.println(p.getFilm().getTitre());
			System.out.println(p.getSalle().getNo());
			System.out.println("Acteurs *********");
			for(RoleActeur role : p.getFilm().getRoles()) {
				System.out.println(role.getActeur().getNom());
			}
			System.out.println("Genres *********");
			for(Genre genre : p.getFilm().getGenres()) {
				System.out.println(genre.getLabel());
			}
			System.out.println("Mot-cles *********");
			for(Motcle motcle : p.getFilm().getMotcles()) {
				System.out.println(motcle.getLabel());
			}
			System.out.println("Langages *********");
			for(Langage langage : p.getFilm().getLangages()) {
				System.out.println(langage.getLabel());
			}
			System.out.println("Critiques *********");
			for(Critique critique : p.getFilm().getCritiques()) {
				System.out.println(critique.getNote());
				System.out.println(critique.getTexte());
			}
		}
	}

	public Document getXmlDocument(){
		return xmlDoc;
	}

	private Element populateFilm(Projection pro){
		Element film =  new Element("film");

		film.setAttribute("film_id", pro.getFilm().getId() + "");
		film.setAttribute("titre", pro.getFilm().getTitre());
		film.setAttribute("duree", pro.getFilm().getDureeToString());
		film.setAttribute("photo", pro.getFilm().getPhoto());

		film.addContent(populateSynopsis(pro)); //le film a un synopsis
		film.addContent(populateListGenre(pro)); //le film a une liste de genre
		film.addContent(populateListMotsCles(pro)); //le film a une liste de mot cles
		film.addContent(populateListLangue(pro)); //le film possède une liste de langues
		film.addContent(populateListActeursRoles(pro));//liste des acteurs du film
		film.addContent(populateListCritiques(pro));//un film reçoit des critiques

		return film;
	}

	private Element populateSynopsis(Projection pro){
		Element synopsys = new Element("synopsis");
		synopsys.setText(pro.getFilm().getSynopsis());
		return synopsys;
	}

	private Element populatProjection(Projection pro){
		Element elemProjections = new Element("projection");

		elemProjections.setAttribute("id", pro.getIdString());
		elemProjections.setAttribute("salle", pro.getSalle().getNo());
		elemProjections.setAttribute("taille", pro.getSalle().getTaille()+"");
		elemProjections.setAttribute("dateHeure", pro.getDateHeureString());

		return elemProjections;
	}

	private Element populateListGenre(Projection pro){
		Element genres = new Element("genres");
		for (Genre genre : pro.getFilm().getGenres()) {
			genres.addContent(populateGenre(genre));
		}
		return genres;
	}

	private Element populateGenre(Genre gen){
		Element genre = new Element("genre");
		genre.setAttribute("id",gen.getIdToString());
		genre.setAttribute("label", gen.getLabel());
		return genre;
	}


	private Element populateListMotsCles(Projection pro){
		Element motcles = new Element("motCles");
		for (Motcle mot : pro.getFilm().getMotcles()) {
			motcles.addContent(populateMotCle(mot));
		}
		return motcles;
	}

	private Element populateMotCle(Motcle mo){
		Element mot = new Element("motCle");
		mot.setAttribute("id",mo.getIdToString());
		mot.setAttribute("label", mo.getLabel());
		return mot;
	}

	private Element populateListLangue(Projection pro){
		Element langue = new Element("langages");
		for (Langage langage : pro.getFilm().getLangages()) {
			langue.addContent(populateLangue(langage));
		}
		return langue;
	}

	private Element populateLangue(Langage lang){
		Element langue = new Element("langage");
		langue.setAttribute("id",lang.getIdToString());
		langue.setAttribute("label", lang.getLabel());
		return langue;
	}

	private Element populateListActeursRoles(Projection pro){
		Element acteurs = new Element("acteurs");

		Set<RoleActeur> ra = pro.getFilm().getRoles(); //Les roles du film
		Iterator<RoleActeur> it = ra.iterator();

		acteurs.setAttribute("formatDate", "dd-MM-yyyy"); //format de la date

			while(it.hasNext()){
			acteurs.addContent(populateActeur(it.next()));
		}

		return acteurs;
	}

	private Element populateActeur(RoleActeur roleActeur){
		Acteur acteur = roleActeur.getActeur();

		Element elemActeur = new Element("acteur");
		elemActeur.setAttribute("acteur_id", acteur.getId() + "");
		elemActeur.setAttribute("nom", acteur.getNom());
		elemActeur.setAttribute("sexe", acteur.getSexe().toString());
		elemActeur.setAttribute("nomNaissance", acteur.getNomNaissance());
		elemActeur.setAttribute("dateNaissance", acteur.getDateNaissanceToString());
		elemActeur.setAttribute("dateDeces", acteur.getDateDecesToString());

		elemActeur.addContent(populateRole(roleActeur));

		return elemActeur;
	}

	public Element populateRole(RoleActeur role){
		Element elementRole = new Element("role");

		elementRole.setAttribute("personnage", role.getPersonnage());
		elementRole.setAttribute("place",role.getPlaceToString());

		return elementRole;
	}

	private Element populateListCritiques(Projection pro) {
		Element critiques = new Element("critiques");

		for(Critique critique : pro.getFilm().getCritiques()){
			critiques.addContent(populateCritique(critique));
		}

		return critiques;
	}

	private Element populateCritique(Critique critique){
		Element elemCritique = new Element("critique");
		elemCritique.setAttribute("id", critique.getIdToString());
		elemCritique.setAttribute("note",critique.getNoteToString());
		elemCritique.setAttribute("texte",critique.getTexte());

		return elemCritique;
	}
}



