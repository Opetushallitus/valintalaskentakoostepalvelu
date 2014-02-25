package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class KokonaisTyo extends Tyo {
	private final Collection<? extends Tyo> tyot;

	public KokonaisTyo(Collection<? extends Tyo> tyot) {
		this.tyot = tyot;
	}

	@com.fasterxml.jackson.annotation.JsonIgnore
	public Collection<Exception> getPoikkeukset() {
		Collection<Collection<Exception>> c = Lists.newArrayList();
		for (Tyo t : tyot) {
			c.add(t.getPoikkeukset());
		}
		return Lists.newArrayList(Iterables.concat(c));
	}

	public int getKokonaismaara() {
		int k = 0;
		for (Tyo t : tyot) {
			k += t.getKokonaismaara();
		}
		return k;
	}

	@Override
	public long getArvioituJaljellaOlevaKokonaiskesto() {
		int k = 0;
		for (Tyo t : tyot) {
			k += t.getArvioituJaljellaOlevaKokonaiskesto();
		}
		return k;
	}

	@Override
	public long getKesto() {
		int k = 0;
		for (Tyo t : tyot) {
			k += t.getKesto();
		}
		return k;
	}

	/**
	 * Palauttaa töiden mediaanikestojen keskiarvon
	 */
	@Override
	public long getYksittaisenTyonArvioituKesto() {
		int k = 0;
		for (Tyo t : tyot) {
			k += t.getYksittaisenTyonArvioituKesto();
		}
		return k / tyot.size();
	}

	public String getNimi() {
		return "kokonaistyo";
	}

	public int getTehty() {
		int k = 0;
		for (Tyo t : tyot) {
			k += t.getTehty();
		}
		return k;
	}
}
