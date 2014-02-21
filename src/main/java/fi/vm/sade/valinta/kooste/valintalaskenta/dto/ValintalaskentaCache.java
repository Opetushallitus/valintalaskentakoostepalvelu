package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultiset;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Threadsafe cache for valintalaskennat
 */
public class ValintalaskentaCache {

	// HakuOidilla voi tarkistaa ettei cachea käytetä useassa eri haussa
	// yhtäaikaa
	private final String hakuOid;
	private final List<String> tarjonnanHakukohteet;
	private final List<String> hakukohdeMask;
	private final List<String> hakukohdeEmpty;
	private final Collection<HakukohdeKey> sizeComparableHakukohdeOids;
	private final Set<String> hakemattomatHakemusOids;
	// hakemusoids + hakukohdeoid => HakukohdeKey. this is used to keep track
	// when hakukohde becomes available for calculation
	private final ConcurrentHashMap<String, List<HakukohdeKey>> hakukohdeTyot;

	// private final Map<String, Collection<String>> hakukohdeHakemuksetOids;

	private ValintalaskentaCache(String hakuOid, List<String> hakukohdeMask) {
		this.hakuOid = hakuOid;
		this.tarjonnanHakukohteet = Collections.synchronizedList(Lists
				.<String> newArrayList());
		if (hakukohdeMask == null) {
			this.hakukohdeMask = Collections.emptyList();
		} else {
			this.hakukohdeMask = Collections.synchronizedList(hakukohdeMask);
		}
		this.hakukohdeEmpty = Collections.synchronizedList(Lists
				.<String> newArrayList());
		// this.hakukohdeHakemuksetOids = Collections.synchronizedMap(Maps
		// .<String, Collection<String>> newTreeMap());
		this.hakemattomatHakemusOids = Collections.synchronizedSet(Sets
				.<String> newHashSet());
		this.hakukohdeTyot = new ConcurrentHashMap<String, List<HakukohdeKey>>();

		this.sizeComparableHakukohdeOids = Collections
				.synchronizedCollection(TreeMultiset.<HakukohdeKey> create());

	}

	public static ValintalaskentaCache create(String hakuOid,
			List<String> hakukohdeMask) {
		return new ValintalaskentaCache(hakuOid, hakukohdeMask);
	}

	public boolean hasTarjonnanHakukohteet() {
		return !tarjonnanHakukohteet.isEmpty();
	}

	public void putHakemusOids(String hakukohdeOid,
			Collection<String> hakemusOids) {
		final HakukohdeKey hakukohdeKey = new HakukohdeKey(hakukohdeOid,
				hakemusOids);
		for (String hakemusOid : hakemusOids) {
			List<HakukohdeKey> h = hakukohdeTyot.putIfAbsent(hakemusOid,
					Collections.synchronizedList(Lists
							.newArrayList(hakukohdeKey)));
			if (h != null) {
				h.add(hakukohdeKey);
			}
		}
		List<HakukohdeKey> h = hakukohdeTyot.putIfAbsent(hakukohdeOid,
				Collections.synchronizedList(Lists.newArrayList(hakukohdeKey)));
		if (h != null) {
			h.add(hakukohdeKey);
		}
		hakemattomatHakemusOids.addAll(hakemusOids);
		sizeComparableHakukohdeOids.add(hakukohdeKey);
		/*
		 * hakukohdeHakemuksetOids.put(hakukohdeOid,
		 * Collections.synchronizedCollection(hakemusOids));
		 */
	}

	/**
	 * @return list of ready works or null if none is ready
	 */
	public List<HakukohdeKey> putHakemus(HakemusTyyppi v) {
		List<HakukohdeKey> valmiit = Lists.newArrayList();
		try {
			for (HakukohdeKey h : hakukohdeTyot.remove(v.getHakemusOid())) {
				if (h.markkaaTyoValmiiksi(v)) {
					valmiit.add(h);
				}
			}
		} catch (NullPointerException e) {

			e.printStackTrace();
			throw new RuntimeException(
					"Samaa hakemusta yritettiin merkata toistamiseen saapuneeksi hakukohteisiin joiden laskentaan hakemus liittyy!",
					e);
		}
		if (valmiit.isEmpty()) {
			return null;
		}
		return valmiit;
	}

	/**
	 * @return list of ready works or null if none is ready
	 */
	public List<HakukohdeKey> putValintaperusteet(String hakukohdeOid,
			List<ValintaperusteetTyyppi> v) {
		List<HakukohdeKey> valmiit = Lists.newArrayList();
		try {
			for (HakukohdeKey h : hakukohdeTyot.get(hakukohdeOid)) {
				if (h.markkaaTyoValmiiksi(v)) {
					valmiit.add(h);
				}
			}
		} catch (NullPointerException e) {

			e.printStackTrace();
			throw new RuntimeException(
					"Samaa valintaperustetta("
							+ hakukohdeOid
							+ ") yritettiin merkata toistamiseen saapuneeksi hakukohteeseen!",
					e);
		}
		if (valmiit.isEmpty()) {
			return null;
		}
		return valmiit;
	}

	public Collection<HakukohdeKey> getKasiteltavatHakukohteetOrderedByHakemustenMaaraAscending() {
		return sizeComparableHakukohdeOids;
	}

	/**
	 * 
	 * @return hakukohteet miinus maski
	 */
	public Collection<String> getFilteredHakukohteet() {
		List<String> kohteet = Lists.newArrayList(tarjonnanHakukohteet);
		kohteet.removeAll(hakukohdeMask);
		kohteet.removeAll(hakukohdeEmpty);
		return kohteet;
	}

	public List<String> getHakukohdeEmpty() {
		return hakukohdeEmpty;
	}

	public List<String> getTarjonnanHakukohteet() {
		return tarjonnanHakukohteet;
	}

	public Collection<String> takeFromHakemattomatHakemukset(
			Collection<String> hakemusOids) {
		List<String> notHaetut = Lists.newArrayList();
		for (String hakemusOid : hakemusOids) {
			if (hakemattomatHakemusOids.remove(hakemusOid)) {
				notHaetut.add(hakemusOid);
			}
		}
		return notHaetut;
	}

	public static class HakukohdeKey implements Comparable<HakukohdeKey> {
		private final String hakukohdeOid;
		private final int hakukohdeHakijaMaara;
		private final Collection<String> hakemusOids;
		private final List<HakemusTyyppi> hakemukset;
		private final AtomicInteger tyotaJaljella;
		private final AtomicReference<List<ValintaperusteetTyyppi>> valintaperusteet;

		public List<HakemusTyyppi> getHakemukset() {
			return hakemukset;
		}

		public List<ValintaperusteetTyyppi> getValintaperusteet() {
			return valintaperusteet.get();
		}

		public HakukohdeKey(String hakukohdeOid, Collection<String> hakemusOids) {
			this.hakukohdeOid = hakukohdeOid;
			this.hakukohdeHakijaMaara = hakemusOids.size();
			this.hakemusOids = Collections.synchronizedCollection(hakemusOids);
			this.tyotaJaljella = new AtomicInteger(hakemusOids.size() + 1);
			this.hakemukset = Collections.synchronizedList(Lists
					.<HakemusTyyppi> newArrayList());
			this.valintaperusteet = new AtomicReference<List<ValintaperusteetTyyppi>>(
					null);
		}

		public boolean markkaaTyoValmiiksi(List<ValintaperusteetTyyppi> v) {
			if (!valintaperusteet.compareAndSet(null, v)) {
				throw new RuntimeException(
						"Samaa valintaperustetta syötettiin hakukohteeseen useaan kertaan! Tarkista ettei useampi säie työstä samaa hakukohdetta!");
			}
			return markkaaTyoValmiiksi();
		}

		public boolean markkaaTyoValmiiksi(HakemusTyyppi hakemus) {
			hakemukset.add(hakemus);
			return markkaaTyoValmiiksi();
		}

		/**
		 * 
		 * @return true if all is done
		 */
		private boolean markkaaTyoValmiiksi() {
			int j = tyotaJaljella.decrementAndGet();
			if (j < 0) {
				throw new RuntimeException(
						"Työtä on merkattu enemmän valmiiksi kuin sitä voi olla valmiina! Synkronointi pettää ja joitain hakemuksia tai valintaperusteita haetaan useaan otteeseen!");
			}
			return j == 0;
		}

		public String getHakukohdeOid() {
			return hakukohdeOid;
		}

		public Collection<String> getHakemusOids() {
			return hakemusOids;
		}

		public int compareTo(HakukohdeKey o) {
			return new Integer(hakukohdeHakijaMaara)
					.compareTo(o.hakukohdeHakijaMaara);
		}

		public String toString() {
			return hakukohdeOid;
		}
	}

	public String getHakuOid() {
		return hakuOid;
	}
}
