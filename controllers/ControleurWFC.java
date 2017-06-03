package controllers;

import ch.heigvd.iict.ser.imdb.models.Data;
import ch.heigvd.iict.ser.rmi.IClientApi;
import ch.heigvd.iict.ser.rmi.IServerApi;
import views.*;


import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.DecimalFormat;

public class ControleurWFC extends UnicastRemoteObject implements IClientApi {

	private ControleurGeneral ctrGeneral;
	private static MainGUI mainGUI;
	private Data data;
	private IServerApi remoteConnexion = null;

	public ControleurWFC(ControleurGeneral ctrGeneral, MainGUI mainGUI) throws RemoteException{
		super(); //appelle du controleur de la super classe
		this.ctrGeneral=ctrGeneral;
		ControleurWFC.mainGUI=mainGUI;

		try {
			//Ajout de la connexion au server
			this.remoteConnexion = (IServerApi) Naming.lookup("//localhost:9999/RmiService");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Problème de connexion au service WFC");
		}

		//Vérification de la connexion distante et enregistrement comme observer
		if(remoteConnexion.isStillConnected()){
			this.remoteConnexion.addObserver(this); //demande d'observation du serveur par le client
		}else{
			System.out.println("Erreur de connexion");
		}
	}

	private static final DecimalFormat doubleFormat = new DecimalFormat("#.#");
	public static final String displaySeconds(long start, long end) {
		long diff = Math.abs(end - start);
		double seconds = ((double) diff) / 1000.0;
		return doubleFormat.format(seconds) + " s";
	}

	public Data getLastData() {
		Data lastData = new Data();

		return lastData;
	}

	@Override
	public void update(Object observable, Signal signalType, String updateMsg) throws RemoteException {
		System.out.println("Signal Reçu : " + signalType.name() + " : " + updateMsg);
		this.data = remoteConnexion.getData();
		if(ctrGeneral != null && data != null){
			ctrGeneral.initBaseDeDonneesAvecNouvelleVersion(data);
		}
	}
}