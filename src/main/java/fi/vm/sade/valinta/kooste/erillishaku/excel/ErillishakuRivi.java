package fi.vm.sade.valinta.kooste.erillishaku.excel;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishakuRivi {

	private final String etunimi;
	private final String sukunimi;
	private final String henkilotunnus;
	private final String syntymaAika;
	
	private final String hakemuksenTila;
	private final String vastaanottoTila;
	private final String ilmoittautumisTila;
	
	private final boolean julkaistaankoTiedot;
	
	public ErillishakuRivi() {
		this.etunimi =  null;
		this.sukunimi = null;
		this.henkilotunnus = null;
		this.syntymaAika = null;
		this.hakemuksenTila = null;
		this.vastaanottoTila = null;
		this.ilmoittautumisTila = null;
		this.julkaistaankoTiedot = false;
	}
	
	public ErillishakuRivi(String sukunimi,String etunimi,  String henkilotunnus, String syntymaAika, String hakemuksenTila, String vastaanottoTila, String ilmoittautumisTila, boolean julkaistaankoTiedot) {
		this.etunimi = etunimi;
		this.sukunimi = sukunimi;
		this.henkilotunnus = henkilotunnus;
		this.syntymaAika = syntymaAika;
		
		this.hakemuksenTila = hakemuksenTila;
		this.vastaanottoTila = vastaanottoTila;
		this.ilmoittautumisTila = ilmoittautumisTila;
		
		this.julkaistaankoTiedot = julkaistaankoTiedot;
	}
	
	public boolean isJulkaistaankoTiedot() {
		return julkaistaankoTiedot;
	}
	public String getEtunimi() {
		return etunimi;
	}
	public String getHenkilotunnus() {
		return henkilotunnus;
	}
	public String getSukunimi() {
		return sukunimi;
	}
	public String getSyntymaAika() {
		return syntymaAika;
	}
	
	public String getHakemuksenTila() {
		return hakemuksenTila;
	}
	public String getIlmoittautumisTila() {
		return ilmoittautumisTila;
	}
	public String getVastaanottoTila() {
		return vastaanottoTila;
	}
	
}
