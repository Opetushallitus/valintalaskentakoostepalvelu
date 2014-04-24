package fi.vm.sade.valinta.kooste.pistesyotto.excel;

public class DataArvo extends PistesyottoDataArvo {

	private final String tunniste;
	private final String osallistuminenTunniste;

	public DataArvo(String tunniste, String osallistuminenTunniste) {
		this.tunniste = tunniste;
		this.osallistuminenTunniste = osallistuminenTunniste;
	}

	public PistesyottoArvo asPistesyottoArvo(String arvo, String tila) {
		return new PistesyottoArvo(arvo, tila, true, tunniste,
				osallistuminenTunniste);
	}
}
