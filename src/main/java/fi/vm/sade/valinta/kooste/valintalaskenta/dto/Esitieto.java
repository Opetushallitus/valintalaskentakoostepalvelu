package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
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
public class Esitieto<T> extends AbstraktiTyo {

	private final AtomicReference<T> esitieto;
	private final Collection<EsitiedonKuuntelija<T>> kuuntelijat;
	private final String oid;

	public Esitieto(String oid) {
		this.kuuntelijat = Collections.synchronizedCollection(Lists
				.<EsitiedonKuuntelija<T>> newArrayList());
		this.esitieto = new AtomicReference<T>(null);
		this.oid = oid;
	}

	public String getOid() {
		return oid;
	}

	/**
	 * HAKUKOHDETYO VOI VALMISTUA TASSA
	 */
	public ValintalaskentaTyo rekisteroiKuuntelija(EsitiedonKuuntelija<T> kuuntelija) {
		T tieto = null;
		// synkronoidaan kuuntelijat listan kautta
		synchronized (this.kuuntelijat) {
			tieto = this.esitieto.get();
			if (tieto == null) {
				this.kuuntelijat.add(kuuntelija);
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
	public Collection<ValintalaskentaTyo> setEsitieto(T esitieto) {
		Collection<EsitiedonKuuntelija<T>> k = Lists.newArrayList();
		synchronized (this.kuuntelijat) {
			for (EsitiedonKuuntelija<T> k0 : this.kuuntelijat) {
				k.add(k0);
			}
			if (!this.esitieto.compareAndSet(null, esitieto)) {
				throw new RuntimeException(
						"Esitiedot: Samaa esitietoa haettiin useaan otteeseen! "
								+ esitieto);
			}
		}
		Collection<ValintalaskentaTyo> valmistuneet = Lists.newArrayList();
		for (EsitiedonKuuntelija<T> e : k) {
			ValintalaskentaTyo t = e.esitietoSaatavilla(esitieto);
			if (t != null) {
				valmistuneet.add(t);
			}
		}
		return valmistuneet;
	}

}
