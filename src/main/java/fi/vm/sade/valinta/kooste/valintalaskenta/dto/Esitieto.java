package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Laskentaan tarvittava esitieto: valintaperuste tai hakemustyyppi
 * 
 *         Esitiedon luova säie vastaa esitiedon välittämisestä työjonoon
 * 
 *         Synkronoitu kuuntelijat listan kautta
 */
public class Esitieto<A, T> extends AbstraktiTyo {

	private final AtomicReference<T> esitieto;
	private final AtomicBoolean ohitettu;
	private final Collection<EsitiedonKuuntelija<A, T>> kuuntelijat;
	private final String oid;

	public Esitieto(String oid) {
		this.ohitettu = new AtomicBoolean(false);
		this.kuuntelijat = Collections.synchronizedCollection(Lists
				.<EsitiedonKuuntelija<A, T>> newArrayList());
		this.esitieto = new AtomicReference<T>(null);
		this.oid = oid;
	}

	public String getOid() {
		return oid;
	}

	/**
	 * HAKUKOHDETYO VOI VALMISTUA TASSA
	 */
	public A rekisteroiKuuntelija(EsitiedonKuuntelija<A, T> kuuntelija) {
		T tieto = null;
		// synkronoidaan kuuntelijat listan kautta
		synchronized (this.kuuntelijat) {
			if (!this.ohitettu.get()) {
				tieto = this.esitieto.get();
				if (tieto == null) {
					this.kuuntelijat.add(kuuntelija);
				}
			} else {
				return kuuntelija.esitietoOhitettu();
			}
		}
		if (tieto != null) {
			return kuuntelija.esitietoSaatavilla(tieto);
		}
		return null;
	}

	/**
	 * HAKUKOHDETYO VOI VALMISTUA TASSA
	 */
	public Collection<A> setEsitieto(T esitieto) {
		Collection<EsitiedonKuuntelija<A, T>> k = Lists.newArrayList();
		synchronized (this.kuuntelijat) {
			for (EsitiedonKuuntelija<A, T> k0 : this.kuuntelijat) {
				k.add(k0);
			}
			if (!this.esitieto.compareAndSet(null, esitieto)) {
				throw new RuntimeException(
						"Esitiedot: Samaa esitietoa haettiin useaan otteeseen! "
								+ oid);
			}
		}
		Collection<A> valmistuneet = Lists.newArrayList();
		for (EsitiedonKuuntelija<A, T> e : k) {
			A t = e.esitietoSaatavilla(esitieto);
			if (t != null) {
				valmistuneet.add(t);
			}
		}
		return valmistuneet;
	}

	/**
	 * HAKUKOHDETYO VOI VALMISTUA TASSA. Joissain virhetilanteissa halutaan
	 * jatkaa valintalaskennan suorittamista. Merkataan esitieto valmistuneeksi.
	 */
	public Collection<A> setEsitietoOhitettu() {
		Collection<EsitiedonKuuntelija<A, T>> k = Lists.newArrayList();
		synchronized (this.kuuntelijat) {
			for (EsitiedonKuuntelija<A, T> k0 : this.kuuntelijat) {
				k.add(k0);
			}

			if (!this.ohitettu.compareAndSet(false, true)) {
				throw new RuntimeException(
						"Esitiedot: Samaa esitietoa yritettiin ohittaa useaan otteeseen! "
								+ oid);
			}
		}
		Collection<A> valmistuneet = Lists.newArrayList();
		for (EsitiedonKuuntelija<A, T> e : k) {
			A t = e.esitietoOhitettu();
			if (t != null) {
				valmistuneet.add(t);
			}
		}
		return valmistuneet;
	}
}
