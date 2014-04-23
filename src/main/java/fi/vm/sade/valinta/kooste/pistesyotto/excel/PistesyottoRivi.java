package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Collection;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class PistesyottoRivi {

	private final String oid;
	private final String nimi;
	private final Collection<PistesyottoArvo> arvot;

	public PistesyottoRivi(String oid, String nimi,
			Collection<PistesyottoArvo> arvot) {
		this.oid = oid;
		this.nimi = nimi;
		this.arvot = arvot;
	}

	public Collection<PistesyottoArvo> getArvot() {
		return arvot;
	}

	public String getNimi() {
		return nimi;
	}

	public String getOid() {
		return oid;
	}

}
