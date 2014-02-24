package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

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

	private final AtomicInteger kokonaismaara;
	private final AtomicInteger ohitettu;
	private final String nimi;
	private final Collection<Long> kestot;
	private final Collection<Long> epaonnistuneet;
	@JsonIgnore
	private final Collection<Exception> poikkeukset;

	// Collections.synchronizedList(Lists.<Long> newArrayList());
	public TyoImpl(String nimi, int kokonaismaara) {
		this.kokonaismaara = new AtomicInteger(kokonaismaara);
		this.ohitettu = new AtomicInteger(0);
		this.kestot = Collections.synchronizedCollection(TreeMultiset
				.<Long> create());
		this.epaonnistuneet = Collections.synchronizedCollection(TreeMultiset
				.<Long> create());
		this.poikkeukset = Collections.synchronizedCollection(Lists
				.<Exception> newArrayList());
		this.nimi = nimi;
	}

	public TyoImpl(String nimi) {
		this(nimi, -1);
	}

	@Override
	public long getYksittaisenTyonArvioituKesto() {
		if (kestot.isEmpty()) {
			return 0L;
		}
		return Iterables.get(kestot, (kestot.size() - 1) / 2);
	}

	public int getOhitettu() {
		return ohitettu.get();
	}

	@Override
	public long getArvioituJaljellaOlevaKokonaiskesto() {
		return getJaljellaOlevienToidenMaara()
				* getYksittaisenTyonArvioituKesto();
	}

	public int getJaljellaOlevienToidenMaara() {
		if (kokonaismaara.get() == -1) { // kokonaismaaraa ei tiedeta
			return -1;
		}
		return kokonaismaara.get() - kestot.size();
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
		this.kokonaismaara.set(kokonaismaara);
	}

	public int getKokonaismaara() {
		return kokonaismaara.get();
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
		ohitettu.incrementAndGet();
	}

	public boolean isValmis() {
		if (kokonaismaara.get() == -1) {
			return false;
		}
		return kokonaismaara.get() == kestot.size();
	}

	public Collection<Exception> getPoikkeukset() {
		return poikkeukset;
	}

	public void tyoEpaonnistui(long kesto, Exception e) {
		epaonnistuneet.add(kesto);
		poikkeukset.add(e);
	}
}
