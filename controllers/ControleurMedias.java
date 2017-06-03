package controllers;

import ch.heigvd.iict.ser.imdb.models.Data;
import ch.heigvd.iict.ser.rmi.IClientApi;
import ch.heigvd.iict.ser.rmi.IServerApi;
import com.google.gson.*;
import models.*;
import views.*;

import java.io.FileWriter;
import java.io.Serializable;
import java.io.Writer;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControleurMedias extends Observable implements IServerApi{

	private ControleurGeneral ctrGeneral;
	private static MainGUI mainGUI;
	private ORMAccess ormAccess;
	private GlobalData globalData;
	private JsonObject output;

	// JSON output is for media purpose, so we limit to the 5 first items
	private static final int LIMITE_GENRE = 5;
	private static final int LIMITE_CRITIQUE = 5;
	private static final int LIMITE_MOTSCLE = 5;
	private static final String JSON_FILENAME = "projections.json";

	public ControleurMedias(ControleurGeneral ctrGeneral, MainGUI mainGUI, ORMAccess ormAccess){
	    this.ctrGeneral=ctrGeneral;
		ControleurMedias.mainGUI=mainGUI;
		this.ormAccess=ormAccess;

		try {
		    //démarrage du serveur RMI sur pour la partie MEDIA
            Registry rmiRegistry = LocateRegistry.createRegistry(999);
            IServerApi rmiService = (IServerApi) UnicastRemoteObject.exportObject(this,999);
            rmiRegistry.bind("RmiService", rmiService);
        }catch (Exception e){
		    e.printStackTrace();
        }
    }

	public void sendJSONToMedia(){
		new Thread(){
			public void run(){
				mainGUI.setAcknoledgeMessage("Envoi JSON ... WAIT");
				long currentTime = System.currentTimeMillis();
				try {

				    // Fetch data
					globalData = ormAccess.GET_GLOBAL_DATA();
                    List<Projection> liste_projections = globalData.getProjections();

                    // Setup json ouput
                    GsonBuilder builder = new GsonBuilder()
                            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                            .setPrettyPrinting();

					Gson gson = null;
                    gson = builder.create();

                    // Build JSON
                    output = buildJson(liste_projections);

                    // Write output to file
                    try (Writer writer = new FileWriter(JSON_FILENAME)) {
                        writer.write(gson.toJson(output));
                    }

					mainGUI.setAcknoledgeMessage("Envoi JSON: Créé avec succès en " + ControleurWFC.displaySeconds(currentTime, System.currentTimeMillis()) );
				}
				catch (Exception e){
					mainGUI.setErrorMessage("Construction JSON impossible", e.toString());
				}
			}
		}.start();
	}

    /**
     * Génére le flux JSON correspondant à une collection de projection
     * Appelle les méthodes spécialisées pour chaque type d'objet et complète le flux
     *
     * @param collectionProjections une List d'objet Projection
     * @return un JsonObject avec le label "projections"
     */
	private JsonObject buildJson(List<Projection> collectionProjections) {

        // Vars
        JsonArray jArrayProjection = new JsonArray();
        JsonObject output = new JsonObject();
        JsonArray sortedjArrayProjection = new JsonArray();

        // Utilisation d'un ArrayList pour pouvoir trier les JsonObject
        ArrayList<JsonObject> jArray = new ArrayList<JsonObject>();

        Iterator<Projection> it = collectionProjections.iterator();
        while (it.hasNext()) {

            // Local vars
            Projection projection = it.next();
            JsonObject jObjectProjection = projectionToJson(projection);

            Film filmSeance = projection.getFilm();
            JsonObject jObjectFilm = filmToJson(filmSeance);                        // 1 film par séance

            // Setup array elements
            JsonArray jArrayActeursRole = roleActeurToJson(filmSeance.getRoles());  // Plusieurs acteurs / film
            JsonArray jArrayGenre = genresToJson(filmSeance.getGenres());           // Plusieurs genres / film
            JsonArray jArrayLangage = langagesToJson(filmSeance.getLangages());     // Plusieurs langages / film
            JsonArray jArrayMotsCle = motsClesToJson(filmSeance.getMotcles());      // Plusieurs mots-clés / film
            JsonArray jArrayCritiques = critiquesToJson(filmSeance.getCritiques()); // Plusieurs critiques / film

            // Put all together
            jObjectProjection.add("film", jObjectFilm);
            jObjectFilm.add("acteurs", jArrayActeursRole);
            jObjectFilm.add("genres", jArrayGenre);
            jObjectFilm.add("langages", jArrayLangage);
            jObjectFilm.add("motCles", jArrayMotsCle);
            jObjectFilm.add("critiques", jArrayCritiques);

            // Array_push projection
            jArrayProjection.add(jObjectProjection);
            jArray.add((JsonObject) jObjectProjection);
        }

        // Anonyme method to sort projection by date & heure
        Collections.sort(jArray, new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject j1, JsonObject j2) {

                // Merge date & heure of j1
                StringBuilder sbDateHeure1 = new StringBuilder();
                sbDateHeure1.append(j1.get("date").getAsString() + " " + j1.get("heure").getAsString());

                // Merge date & heure of j2
                StringBuilder sbDateHeure2 = new StringBuilder();
                sbDateHeure2.append(j2.get("date").getAsString() + " " + j2.get("heure").getAsString());

                StringBuilder sbDate1 = new StringBuilder();
                StringBuilder sbDate2 = new StringBuilder();

                // Match a date time like : DD-MM-YYYY HH:MM
                String pattern = "(\\d{1,2})-(\\d{1,2})-(\\d{2,4}) (\\d\\d):(\\d\\d){0,1}";
                Matcher matcher1 = Pattern.compile(pattern).matcher(sbDateHeure1.toString());
                Matcher matcher2 = Pattern.compile(pattern).matcher(sbDateHeure2.toString());

                // Build date as a unified string YYYYMMDDHHMM formated, so we can easily sort by date & heure
                while(matcher1.find() && matcher2.find()){
                    // j1
                    sbDate1.append(matcher1.group(3)).append(matcher1.group(2)).append(matcher1.group(1));  // YYYYMMDD
                    sbDate1.append(matcher1.group(4)).append(matcher1.group(5));    // HHMM
                    // j2
                    sbDate2.append(matcher2.group(3)).append(matcher2.group(2)).append(matcher2.group(1));  // YYYYMMDD
                    sbDate2.append(matcher2.group(4)).append(matcher2.group(5));    // HHMM
                }

                // Compare YYYYMMDDHHMM
                return sbDate1.toString().compareTo(sbDate2.toString());
            }
        });

        // Back from ArrayList to JsonArray
        for (int i = 0; i < jArray.size(); ++i) {
            sortedjArrayProjection.add(jArray.get(i));
        }

        // Ajout du format de la date au tout début
        JsonObject jFormatDate = new JsonObject();
        output.addProperty("formatDate", "DD-MM-YYYY");
        output.addProperty("formatHeure", "HH:MM /24h");
        output.add("projections", sortedjArrayProjection);

        return output;
    }

    /**
     * Une projection est décrite dans le fichier JSON par la salle, la date et l'heure
     * A noter que la date et l'heure son volontairement séparé pour offrir une plus grande flexibilité
     * dans l'interprétation du fichier
     *
     * @param pro un objet Projection
     * @return un JsonObject contenant les détails de la projection
     */
	private JsonObject projectionToJson(Projection pro) {

        // Vars
        JsonObject jObjectProjection = new JsonObject();
        String sDate = null;
        String sHeure = null;

        // Salle
        jObjectProjection.add("salle", new JsonPrimitive(String.valueOf(pro.getSalle())));

        // Date
        Calendar calDateHeure = pro.getDateHeure();
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
        sDate = date.format(calDateHeure.getTime());

        // Heure
        SimpleDateFormat heure = new SimpleDateFormat("HH:mm");
        sHeure = heure.format(calDateHeure.getTime());

        // Put in projection
        jObjectProjection.add("date", new JsonPrimitive(sDate));
        jObjectProjection.add("heure", new JsonPrimitive(sHeure));

        return jObjectProjection;
    }

	private JsonObject filmToJson(Film film) {

	    JsonObject jObjectFilm = new JsonObject();
        jObjectFilm.add("titre", new JsonPrimitive(film.getTitre()));
        jObjectFilm.add("photo", new JsonPrimitive(film.getPhoto()));
        jObjectFilm.add("duree", new JsonPrimitive(film.getDureeToString()));

	    return jObjectFilm;
    }

    private JsonArray roleActeurToJson(Set<RoleActeur> collectionRoleActeur) {

	    // Collection d'acteurs
        JsonArray jArrayRoleActeur = new JsonArray();
        JsonArray sortedjArrayRoleActeur = new JsonArray();
        jArrayRoleActeur.add(new JsonPrimitive("formatDate"));

        // Utilisation d'un ArrayList pour pouvoir trier les JsonObject
        ArrayList<JsonObject> jArray = new ArrayList<JsonObject>();

	    for (RoleActeur coupleRoleActeur : collectionRoleActeur) {
	        long place = coupleRoleActeur.getPlace();

	        // Seul les acteurs ayant le premier ou le second rôle
	        if (place == 1 || place == 2) {

                // Chaque acteur est représenté par un JsonObject
                JsonObject jObjectActeur = new JsonObject();

                // Chaque rôle est également un JsonObject
                JsonObject jsonObjectRole = new JsonObject();

	        	// Détails de l'acteur
	        	jObjectActeur.add("nom", new JsonPrimitive(coupleRoleActeur.getActeur().getNom()));
	            jObjectActeur.add("dateNaissance", new JsonPrimitive(coupleRoleActeur.getActeur().getDateNaissanceToString()));

	            // Rôle joué
	            jsonObjectRole.add("personnage", new JsonPrimitive(coupleRoleActeur.getPersonnage()));
	            jsonObjectRole.add("place", new JsonPrimitive(coupleRoleActeur.getPlaceToString()));

	            // 					<acteur ...>
				// <role ... /> || __>
				// 					</acteur>
	            jObjectActeur.add("role", jsonObjectRole);

				// <acteur ...>			||	<acteurs>
				// 		<role ... />	|| __>
				// </acteur>			|| 	</acteurs>
	            jArrayRoleActeur.add(jObjectActeur);
                jArray.add((JsonObject) jObjectActeur);
            }
        }	// End for

        // On veut que les acteurs soient affichés dans l'ordre des places jouées (1ère place en 1er, 2ème en 2ème, etc..)
        Collections.sort(jArray, new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject j1, JsonObject j2) {

                // Comparaison basée sur l'attribut "place" pour un rôle donné
                String v1 = ((JsonObject) j1.get("role")).get("place").getAsString();
                String v2 = ((JsonObject) j2.get("role")).get("place").getAsString();

                return v1.compareTo(v2);
            }
        });

	    // ArrayList _> JsonArray
        for (int i = 0; i < jArray.size(); ++i) {
            sortedjArrayRoleActeur.add(jArray.get(i));
        }

		return sortedjArrayRoleActeur;
    }

    /**
     * Sérialise une collection de genre vers un tableau JSON
     * Chaque élément du tableau est un JsonObject correspondant à un genre
     *
     * @param collectionGenre un Set de Genre
     * @return jArrayGenres un tableau JSON
     */
	private JsonArray genresToJson(Set<Genre> collectionGenre) {

	    // Local vars
        int count = 0;
		JsonArray jArrayGenres = new JsonArray();
		Iterator<Genre> it = collectionGenre.iterator();

		while (it.hasNext() && count < LIMITE_GENRE ) {
            Genre genre = it.next();
			JsonObject jObjectGenre = new JsonObject();
			jObjectGenre.add("label", new JsonPrimitive(genre.getLabel()));

            // Push genre to array
			jArrayGenres.add(jObjectGenre);
			count++;
		}

		return jArrayGenres;
	}

    /**
     * Sérialise une collection de mots-clés vers un tableau JSON
     * Chaque élément du tableau est un JsonObject correspondant à un mot-clé
     *
     * @param collectionMotCles un Set de Motcle
     * @return jArrayMotsCle un tableau JSON
     */
	private JsonArray motsClesToJson(Set<Motcle> collectionMotCles) {

        // Local vars
        int count = 0;
        JsonArray jArrayMotsCle = new JsonArray();
        Iterator<Motcle> it = collectionMotCles.iterator();

	    while (it.hasNext() && count < LIMITE_MOTSCLE) {
            Motcle motcle = it.next();
	        JsonObject jObjectMotCle = new JsonObject();
	        jObjectMotCle.add("label", new JsonPrimitive(motcle.getLabel()));

            // Push motclé to array
	        jArrayMotsCle.add(jObjectMotCle);
	        count++;
        }

        return jArrayMotsCle;
    }

    /**
     * Sérialise un collection de langages vers un tableau JSON
     * Chaque élément du tableau JSON et un JsonObject correspondant à une langue
     *
     * @param collectionLangages un Set de Langage
     * @return jArrayLangages un tableau JSON
     */
    private JsonArray langagesToJson(Set<Langage> collectionLangages) {

	    JsonArray jArrayLangages = new JsonArray();
	    for (Langage langage : collectionLangages) {
	        JsonObject jObjectLangage = new JsonObject();
	        jObjectLangage.add("label", new JsonPrimitive(langage.getLabel()));

	        // Push langage to array
	        jArrayLangages.add(jObjectLangage);
        }

        return jArrayLangages;
    }

    /**
     * Sérialise un collection de critiques vers un tableau JSON
     * Chaque élément du tableau JSON et un JsonObject correspondant à une critique
     *
     * @param collectionCritiques un Set de Critique
     * @return jArrayCritiques un tableau JSON
     */
    private JsonArray critiquesToJson(Set<Critique> collectionCritiques) {

        // Local vars
        int count = 0;
        JsonArray jArrayCritiques = new JsonArray();
        Iterator<Critique> it = collectionCritiques.iterator();

        // LIMITE_CRITIQUE could have been passed as function param
        while (it.hasNext() && count < LIMITE_CRITIQUE ) {
            Critique critique = it.next();
            JsonObject jObjectCritique = new JsonObject();
            jObjectCritique.add("note", new JsonPrimitive(critique.getNote()));
            jObjectCritique.add("texte", new JsonPrimitive(critique.getTexte()));

            // Push crtique to array
            jArrayCritiques.add(jObjectCritique);
            count++;
        }

        return jArrayCritiques;
    }

    @Override
    public void addObserver(IClientApi client) throws RemoteException {
        WrappedObserver wo = new WrappedObserver(client);
        addObserver(wo);
        System.out.println("Added observer: " + client);
    }

    @Override
    public boolean isStillConnected() throws RemoteException {
        return true;
    }

    @Override
    public Data getData() throws RemoteException {
        return null;
    }

    private class WrappedObserver implements Observer, Serializable {

        private static final long serialVersionUID = -2067345842536415833L;

        private IClientApi ro = null;

        public WrappedObserver(IClientApi ro) {
            this.ro = ro;
        }

        public void update(Observable o, Object arg) {
            try {
                ro.update(o.toString(), IClientApi.Signal.UPDATE_REQUESTED, arg.toString());
            } catch (RemoteException e) {
                System.out.println("Remote exception removing observer: " + this);
                o.deleteObserver(this);
            }
        }
    }

}