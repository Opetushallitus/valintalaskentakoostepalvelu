package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class DiskreettiDataArvo extends TilaDataArvo {

	private final Logger LOG = LoggerFactory.getLogger(DiskreettiDataArvo.class);
	private final Set<String> arvot;
	private final String tunniste;
	private final String osallistuminenTunniste;

	public DiskreettiDataArvo(Collection<String> arvot,
			Map<String, String> tilaKonvertteri, String tunniste,
			String asetettuTila, String osallistuminenTunniste) {
		super(tilaKonvertteri, asetettuTila);
		this.arvot = Sets.newHashSet(arvot);
		this.tunniste = tunniste;
		this.osallistuminenTunniste = osallistuminenTunniste;
	}

	protected boolean isValidi(String arvo) {
		return StringUtils.isBlank(arvo) || tarkistaArvo(arvo);
	}

	private boolean tarkistaArvo(String arvo) {
		return arvot.contains(arvo);
	}

	private boolean isAsetettu(String arvo) {
		return tarkistaArvo(arvo);
	}

	public PistesyottoArvo asPistesyottoArvo(String arvo, String tila) {
		// LOG.error("{}", arvo);
		String lopullinenTila;
		if (isAsetettu(arvo)) {
			lopullinenTila = getAsetettuTila();
		} else {
			lopullinenTila = konvertoiTila(tila);
			if (getAsetettuTila().equals(lopullinenTila)) {
				lopullinenTila = PistesyottoExcel.VAKIO_MERKITSEMATTA;
			}
		}
		return new PistesyottoArvo(arvo, lopullinenTila, isValidi(arvo)
				&& isValidiTila(tila), tunniste, osallistuminenTunniste);
	}

}