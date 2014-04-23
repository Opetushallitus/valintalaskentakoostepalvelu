package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class BooleanDataArvo extends TilaDataArvo {

	private final Map<String, String> konvertteri;
	private final String tunniste;
	private final String osallistuminenTunniste;

	public BooleanDataArvo(Map<String, String> konvertteri,
			Map<String, String> tilaKonvertteri, String tunniste,
			String osallistuminenTunniste) {
		super(tilaKonvertteri);
		this.konvertteri = konvertteri;
		this.tunniste = tunniste;
		this.osallistuminenTunniste = osallistuminenTunniste;
	}

	protected boolean isValidi(String arvo) {
		return StringUtils.isBlank(arvo) || konvertteri.containsKey(arvo);
	}

	protected String konvertoi(String arvo) {
		if (konvertteri.containsKey(arvo)) {
			return konvertteri.get(arvo);
		} else {
			return null;
		}
	}

	public PistesyottoArvo asPistesyottoArvo(String arvo, String tila) {
		return new PistesyottoArvo(konvertoi(arvo), konvertoiTila(tila),
				isValidi(arvo) && isValidiTila(tila), tunniste,
				osallistuminenTunniste);
	}
}
