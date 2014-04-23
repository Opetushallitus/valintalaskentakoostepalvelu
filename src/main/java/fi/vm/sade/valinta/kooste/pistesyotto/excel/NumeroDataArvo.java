package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumeroDataArvo extends TilaDataArvo {

	private final Logger LOG = LoggerFactory.getLogger(NumeroDataArvo.class);
	private final double min;
	private final double max;

	public NumeroDataArvo(double min, double max,
			Map<String, String> tilaKonvertteri) {
		super(tilaKonvertteri);
		this.min = min;
		this.max = max;
	}

	protected boolean isValidi(String arvo) {
		return StringUtils.isBlank(arvo) || tarkistaRajat(arvo);
	}

	private boolean tarkistaRajat(String arvo) {
		try {
			double d = Double.parseDouble(arvo);
			return min <= d && max >= d;
		} catch (Exception e) {

		}
		return false;
	}

	public PistesyottoArvo asPistesyottoArvo(String arvo, String tila) {
		// LOG.error("{}", arvo);
		return new PistesyottoArvo(arvo, konvertoiTila(tila), isValidi(arvo)
				&& isValidiTila(tila));
	}

	public Number getMax() {
		return max;
	}

	public Number getMin() {
		return min;
	}
}
