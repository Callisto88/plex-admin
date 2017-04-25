package controllers;

import com.google.gson.*;
import models.*;
import views.*;
import java.io.FileWriter;
import java.io.Writer;
import java.util.*;
import java.util.Comparator;

public class ControleurMedia {

	private ControleurGeneral ctrGeneral;
	private static MainGUI mainGUI;
	private ORMAccess ormAccess;
	private GlobalData globalData;

	// JSON output is for media purpose, so we limit to the 5 first items
	private static final int LIMITE_GENRE = 5;
	private static final int LIMITE_CRITIQUE = 5;
	private static final int LIMITE_MOTSCLE = 5;
	private static final String JSON_FILENAME = "projections.json";

	public ControleurMedia(ControleurGeneral ctrGeneral, MainGUI mainGUI, ORMAccess ormAccess){
		this.ctrGeneral=ctrGeneral;
		ControleurMedia.mainGUI=mainGUI;
		this.ormAccess=ormAccess;
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
                    JsonObject output = buildJson(liste_projections);

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

        Collections.sort(jArray, new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject j1, JsonObject j2) {

                String v1 = j1.get("date").getAsString();
                String v2 = j2.get("date").getAsString();

                return v1.compareTo(v2);
            }
        });

        // ArrayList _> JsonArray
        for (int i = 0; i < jArray.size(); ++i) {
            sortedjArrayProjection.add(jArray.get(i));
        }

        output.add("projections", sortedjArrayProjection);

        return output;
    }

	private JsonObject projectionToJson(Projection pro) {

        // Objets JSON
        JsonObject jObjectProjection = new JsonObject();

        // Date et heure
        String sDate = null;
        String sHeure = null;
        Character dateSeparator = new Character('-');
        Character hourSeparator = new Character(':');

        // Salle
        jObjectProjection.add("salle", new JsonPrimitive(String.valueOf(pro.getSalle())));

        // Formatage de la date telle qu'on les trouve en Suisse
        StringBuilder sbDate = new StringBuilder();
        sbDate.append(pro.getDateHeure().get(Calendar.DAY_OF_MONTH)).append(dateSeparator);
        sbDate.append(pro.getDateHeure().get(Calendar.MONTH)).append(dateSeparator);
        sbDate.append(pro.getDateHeure().get(Calendar.YEAR));
        sDate = sbDate.toString();

        // Formatage de l'heure au format HH:MM
        StringBuilder sbHeure = new StringBuilder();
        sbHeure.append(pro.getDateHeure().get(Calendar.HOUR_OF_DAY)).append(":");

        // Fixe les cas où : minutes = 3 force le format à _> 03
        int minutes = pro.getDateHeure().get(Calendar.MINUTE);
        String minutesFormat = String.valueOf((minutes < 10) ? "0" + minutes : minutes);
        sbHeure.append(minutesFormat);
        sHeure = sbHeure.toString();

        // Ajout de la date et de l'heure séparement
        jObjectProjection.add("date", new JsonPrimitive(sDate));
        jObjectProjection.add("heure", new JsonPrimitive(sHeure));

        return jObjectProjection;
    }

	private JsonObject filmToJson(Film film) {

	    JsonObject jObjectFilm = new JsonObject();
        jObjectFilm.add("titre", new JsonPrimitive(film.getTitre()));
        // jObjectFilm.add("photo", new JsonPrimitive(film.getPhoto()));
        jObjectFilm.add("duree", new JsonPrimitive(film.getDureeToString()));

	    return jObjectFilm;
    }

    private JsonArray roleActeurToJson(Set<RoleActeur> collectionRoleActeur) {

	    // Collection d'acteurs
        JsonArray jArrayRoleActeur = new JsonArray();
        JsonArray sortedjArrayRoleActeur = new JsonArray();

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
	            jObjectActeur.add("date_naissance", new JsonPrimitive(coupleRoleActeur.getActeur().getDateNaissanceToString()));

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

		while (it.hasNext() && count <= LIMITE_GENRE ) {
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

	    while (it.hasNext() && count <= LIMITE_MOTSCLE) {
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
        while (it.hasNext() && count <= LIMITE_CRITIQUE ) {
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

    /*private JsonArray sortJsonArray(JsonArray jsonArray) {

        JsonArray sortedJsonArray = new JsonArray();
        List<JsonElement> jsonValues = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); ++i) {
            jsonValues.add(jsonArray.get(i));
        }

        Collections.sort( jsonValues, new Comparator<JsonElement>() {

            // Tri par place occupée dans le film
            private static final String KEY_NAME = "place";

            @Override
            public int compare(JsonElement a, JsonElement b) {
                String valA = new String();
                String valB = new String();

                try {
                    valA = (String) a.get(KEY_NAME);
                    valB = (String) b.get(KEY_NAME);
                }
                catch (JSONException e) {
                    //do something
                }

                return valA.compareTo(valB);
                //if you want to change the sort order, simply use the following:
                //return -valA.compareTo(valB);
            }
        });

        for (int i = 0; i < jsonArray.size(); i++) {
            sortedJsonArray.put(jsonValues.get(i));
        }

        return sortedJsonArray;
    }*/

	/*

	GsonBuilder builder = new GsonBuilder()
                        .registerTypeAdapter(models.Projection.class, new ProjectionHandler())
                        .registerTypeAdapter(models.Film.class, new FilmHandler())
                        .registerTypeAdapter(models.Genre.class, new GenreHandler())
                        .registerTypeAdapter(models.Acteur.class, new ActeurHandler());

                    builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                            .setPrettyPrinting();


	private class ProjectionHandler implements JsonSerializer<Projection> {

		public JsonElement serialize(Projection pro, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jobj = new JsonObject();
			jobj.addProperty("id", pro.getId());
			jobj.addProperty("salle", String.valueOf(pro.getSalle()));
			jobj.addProperty("dateHeure", pro.getDateHeureString());
			jobj.addProperty("film", gson.toJson(pro.getFilm()));
			jobj.addProperty("genres", gson.toJson(pro.getFilm().getGenres()));

			return jobj;
		}
	} */
}