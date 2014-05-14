package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

public abstract class TilaDataArvo extends PistesyottoDataArvo {

	private final Map<String, String> tilaKonvertteri;
	private final String asetettuTila;

	public TilaDataArvo(Map<String, String> tilaKonvertteri, String asetettuTila) {
		this.tilaKonvertteri = tilaKonvertteri;
		this.asetettuTila = asetettuTila;
	}

	protected boolean isValidiTila(String tila) {
		return StringUtils.isBlank(tila) || tilaKonvertteri.containsKey(tila);
	}

	protected String getAsetettuTila() {
		return asetettuTila;
	}

	protected String konvertoiTila(String tila) {

		if (tilaKonvertteri.containsKey(tila)) {
			return tilaKonvertteri.get(tila);
		} else {
			return StringUtils.EMPTY;
		}
	}
}
