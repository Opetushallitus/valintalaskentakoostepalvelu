package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import org.apache.commons.lang.StringUtils;

public class PistesyottoArvo {

	private final String arvo;
	private final String tila;
	private final boolean validi;
	private final boolean tyhja;

	public PistesyottoArvo(String arvo, String tila, boolean validi) {
		this.tila = tila;
		this.arvo = arvo;
		this.validi = validi;
		this.tyhja = StringUtils.isBlank(arvo) || StringUtils.isBlank(tila);
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
