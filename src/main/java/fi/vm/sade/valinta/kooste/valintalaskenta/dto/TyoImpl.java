package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeMultiset;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class TyoImpl extends Tyo {

	private volatile int kokonaismaara = -1;
	private volatile int ohitettu = 0;
	private final String nimi;
	private final Collection<Long> kestot;
	private final Collection<Long> epaonnistuneet;
	@JsonIgnore
	private final Collection<Exception> poikkeukset;

	// Collections.synchronizedList(Lists.<Long> newArrayList());

	public TyoImpl(String nimi) {

		this.kestot = Collections.synchronizedCollection(TreeMultiset
				.<Long> create());
		this.epaonnistuneet = Collections.synchronizedCollection(TreeMultiset
				.<Long> create());
		this.poikkeukset = Collections.synchronizedCollection(Lists
				.<Exception> newArrayList());
		this.nimi = nimi;
	}

	@Override
	public long getYksittaisenTyonArvioituKesto() {
		if (kestot.isEmpty()) {
			return 0L;
		}
		return Iterables.get(kestot, (kestot.size() - 1) / 2);
	}

	public int getOhitettu() {
		return ohitettu;
	}

	@Override
	public long getArvioituJaljellaOlevaKokonaiskesto() {
		return getJaljellaOlevienToidenMaara()
				* getYksittaisenTyonArvioituKesto();
	}

	public int getJaljellaOlevienToidenMaara() {
		if (kokonaismaara == -1) { // kokonaismaaraa ei tiedeta
			return -1;
		}
		return kokonaismaara - kestot.size();
	}

	@Override
	public long getKesto() {
		long kokonaiskesto = 0L;
		for (Long kesto : kestot) {
			kokonaiskesto += kesto;
		}
		return kokonaiskesto;
	}

	public void setKokonaismaara(int kokonaismaara) {
		this.kokonaismaara = kokonaismaara;
	}

	public int getKokonaismaara() {
		return kokonaismaara;
	}

	public String getNimi() {
		return nimi;
	}

	public int getTehty() {
		return kestot.size();
	}

	public void tyoValmistui(long kesto) {
		kestot.add(kesto);
	}

	public void tyoOhitettu() {
		kestot.add(0L);
		++ohitettu;
	}

	public boolean isValmis() {
		return kokonaismaara == kestot.size();
	}

	@JsonIgnore
	public Collection<Exception> getPoikkeukset() {
		return poikkeukset;
	}

	public void tyoEpaonnistui(long kesto, Exception e) {
		epaonnistuneet.add(kesto);
		poikkeukset.add(e);
	}
}
