package fi.vm.sade.valinta.kooste.valvomo.dto;

import java.util.Collection;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public abstract class Tyo {

	public double getProsentteina() {
		int kokonaismaara = getKokonaismaara();
		if (kokonaismaara == 0) {
			return 0d;
		}
		return ((double) getTehty()) / ((double) kokonaismaara);
	}

	/**
	 * 
	 * @return Suoritettujen töiden kokonaiskesto millisekunteina
	 */
	abstract public long getKesto();

	abstract public Collection<Exception> getPoikkeukset();

	//
	abstract public long getArvioituJaljellaOlevaKokonaiskesto();

	/**
	 * 
	 * @return yksittaisen tyon arvioitu kesto, eli mediaani kestoista.
	 */
	abstract public long getYksittaisenTyonArvioituKesto();

	abstract public int getTehty();

	abstract public int getKokonaismaara();

	abstract public String getNimi();

}
