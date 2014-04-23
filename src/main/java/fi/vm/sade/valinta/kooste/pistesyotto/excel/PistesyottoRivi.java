package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;

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

	public Map<String, String> asAdditionalData() {
		Map<String, String> data = Maps.newHashMap();
		for (PistesyottoArvo arvo : arvot) {
			data.put(arvo.getTunniste(), arvo.getArvo());
			data.put(arvo.getOsallistuminenTunniste(), arvo.getTila());
		}
		return data;
	}

	public boolean isValidi() {
		for (PistesyottoArvo a : arvot) {
			if (!a.isValidi()) {
				return false;
			}
		}
		return true;
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
