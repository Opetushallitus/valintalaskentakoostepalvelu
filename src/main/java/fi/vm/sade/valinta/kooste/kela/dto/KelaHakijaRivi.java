package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Date;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaHakijaRivi {

	private final String henkilotunnus;
	private final String etunimi;
	private final String sukunimi;
	private final Date lukuvuosi;
	private final Date poimintapaivamaara;
	private final Date valintapaivamaara;
	private final String linjakoodi;
	private final String oppilaitos;
	private final String syntymaaika; // 04.05.1965

	public KelaHakijaRivi(String etunimi, String sukunimi,
			String henkilotunnus, Date lukuvuosi, Date poimintapaivamaara,
			Date valintapaivamaara, String linjakoodi, String oppilaitos,
			String syntymaaika) {
		this.etunimi = etunimi;
		this.sukunimi = sukunimi;
		this.henkilotunnus = henkilotunnus;
		this.lukuvuosi = lukuvuosi;
		this.poimintapaivamaara = poimintapaivamaara;
		this.valintapaivamaara = valintapaivamaara;
		this.linjakoodi = linjakoodi;
		this.oppilaitos = oppilaitos;
		this.syntymaaika = syntymaaika;
	}

	public String getSyntymaaika() {
		return syntymaaika;
	}

	public boolean hasHenkilotunnus() {
		return henkilotunnus != null;
	}

	public Date getValintapaivamaara() {
		return valintapaivamaara;
	}

	public String getHenkilotunnus() {
		return henkilotunnus;
	}

	public String getEtunimi() {
		return etunimi;
	}

	public String getSukunimi() {
		return sukunimi;
	}

	public Date getPoimintapaivamaara() {
		return poimintapaivamaara;
	}

	public Date getLukuvuosi() {
		return lukuvuosi;
	}

	public String getLinjakoodi() {
		return linjakoodi;
	}

	public String getOppilaitos() {
		return oppilaitos;
	}
}
