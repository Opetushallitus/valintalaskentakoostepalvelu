package fi.vm.sade.valinta.kooste.pistesyotto.excel;

public class DataArvo extends PistesyottoDataArvo {

	public PistesyottoArvo asPistesyottoArvo(String arvo, String tila) {
		return new PistesyottoArvo(arvo, tila, true);
	}
}
