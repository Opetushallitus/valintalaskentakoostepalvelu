package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Suoritus {
	private String id;
	private String komo;
	private String myontaja;
	private String tila;
	private String valmistuminen;
	private String henkiloOid;
	private String yksilollistaminen;
	private String suoritusKieli;
	private String source;

	public String getHenkiloOid() {
		return henkiloOid;
	}

	public String getId() {
		return id;
	}

	public String getKomo() {
		return komo;
	}

	public String getMyontaja() {
		return myontaja;
	}

	public String getSource() {
		return source;
	}

	public String getSuoritusKieli() {
		return suoritusKieli;
	}

	public String getTila() {
		return tila;
	}

	public String getValmistuminen() {
		return valmistuminen;
	}

	public String getYksilollistaminen() {
		return yksilollistaminen;
	}

}
