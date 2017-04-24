package controllers;

import views.*;

import java.text.DecimalFormat;

public class ControleurWFC {

	private ControleurGeneral ctrGeneral;
	private static MainGUI mainGUI;

	public ControleurWFC(ControleurGeneral ctrGeneral, MainGUI mainGUI){
		this.ctrGeneral=ctrGeneral;
		ControleurWFC.mainGUI=mainGUI;
	}

	private static final DecimalFormat doubleFormat = new DecimalFormat("#.#");
	public static final String displaySeconds(long start, long end) {
		long diff = Math.abs(end - start);
		double seconds = ((double) diff) / 1000.0;
		return doubleFormat.format(seconds) + " s";
	}
}