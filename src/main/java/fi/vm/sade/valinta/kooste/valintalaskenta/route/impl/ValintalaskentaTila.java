package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import java.util.concurrent.atomic.AtomicReference;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Säilöö valintalaskennan tilan että voidaan varmistaa vain yhden
 *         valintalaskennan olevan kerrallaan käynnissä
 */
public class ValintalaskentaTila {

	private final AtomicReference<ValintalaskentaMuistissaProsessi> kaynnissaOlevaValintalaskenta;

	public ValintalaskentaTila() {
		this.kaynnissaOlevaValintalaskenta = new AtomicReference<>(
				null);
	}

	public AtomicReference<ValintalaskentaMuistissaProsessi> getKaynnissaOlevaValintalaskenta() {
		return kaynnissaOlevaValintalaskenta;
	}
}
