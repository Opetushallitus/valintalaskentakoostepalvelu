package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class PistesyottoArvo {

	private final String arvo;
	private final String tila;
	private final boolean validi;
	private final boolean tyhja;
	private final String tunniste;
	private final String osallistuminenTunniste;

	public PistesyottoArvo(String arvo, String tila, boolean validi,
			String tunniste, String osallistuminenTunniste) {
		this.tila = tila;
		this.arvo = arvo;
		this.validi = validi;
		this.tyhja = StringUtils.isBlank(arvo) || StringUtils.isBlank(tila);
		this.tunniste = tunniste;
		this.osallistuminenTunniste = osallistuminenTunniste;
	}

	public String getOsallistuminenTunniste() {
		return osallistuminenTunniste;
	}

	public String getTunniste() {
		return tunniste;
	}

	public boolean isValidi() {
		return validi;
	}

	public boolean isTyhja() {
		return tyhja;
	}

	public String getArvo() {
		return arvo;
	}

	public String getTila() {
		return tila;
	}
}
