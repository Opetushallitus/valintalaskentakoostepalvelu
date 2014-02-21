package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
		if (hakemusOids == null || hakemusOids.isEmpty()) {
			throw new RuntimeException(
					"Tyhjää hakemusoidjoukkoa yritettiin syöttää cacheen!");
		}
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

	public int getHakemattomienHakemustenMaara() {
		return hakemattomatHakemusOids.size();
	}

	/**
	 * @return list of ready works or null if none is ready
	 */
	public List<HakukohdeKey> putHakemus(HakemusTyyppi v) {
		if (v == null) {
			throw new RuntimeException(
					"Tyhjää hakemustyyppiä yritettiin syöttää cacheen!");
		}
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
		if (v == null) {
			throw new RuntimeException(
					"Valintaperusteet lista ei saa olla null!");
		}
		List<HakukohdeKey> valmiit = Lists.newArrayList();
		try {
			for (HakukohdeKey h : hakukohdeTyot.remove(hakukohdeOid)) {
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
		private final Collection<String> oids;
		private final List<HakemusTyyppi> hakemukset;
		// private final AtomicInteger tyotaJaljella;
		private final AtomicReference<List<ValintaperusteetTyyppi>> valintaperusteet;

		public List<HakemusTyyppi> getHakemukset() {
			return hakemukset;
		}

		public List<ValintaperusteetTyyppi> getValintaperusteet() {
			return valintaperusteet.get();
		}

		public HakukohdeKey(String hakukohdeOid, Collection<String> hakemusOids) {
			if (hakukohdeOid == null) {
				throw new RuntimeException("Hakukohde oiditon hakukohdeKey!");
			}
			if (hakemusOids == null || hakemusOids.isEmpty()) {
				throw new RuntimeException("Hakemukseton hakukohde "
						+ hakukohdeOid);
			}
			this.hakukohdeOid = hakukohdeOid;
			this.hakukohdeHakijaMaara = hakemusOids.size();

			List<String> l = Lists.newArrayList(hakemusOids);
			l.add(hakukohdeOid);
			this.oids = Collections.synchronizedCollection(l);

			// this.tyotaJaljella = new AtomicInteger(hakemusOids.size() + 1);
			this.hakemukset = Collections.synchronizedList(Lists
					.<HakemusTyyppi> newArrayList());
			this.valintaperusteet = new AtomicReference<List<ValintaperusteetTyyppi>>(
					null);
		}

		public boolean markkaaTyoValmiiksi(List<ValintaperusteetTyyppi> v) {
			if (v == null) {
				throw new RuntimeException(
						"Valintaperusteet lista ei saa olla null!");
			}
			boolean didRemove;
			boolean wasEmpty;
			synchronized (oids) {
				didRemove = oids.remove(hakukohdeOid);
				wasEmpty = oids.isEmpty();
			}
			if (didRemove) {
				if (!valintaperusteet.compareAndSet(null, v)) {
					throw new RuntimeException(
							"HakukohdeKey:n oids listan synkronointi pettää!");
				}
				return wasEmpty;
			} else {
				throw new RuntimeException(
						"Samaa valintaperustetta syötettiin hakukohteeseen useaan kertaan! Tarkista ettei useampi säie työstä samaa hakukohdetta!");
			}
		}

		public boolean markkaaTyoValmiiksi(HakemusTyyppi hakemus) {
			if (hakemus == null) {
				throw new RuntimeException(
						"Yritetään merkata null hakemusta valmiiksi työksi hakukohteeseen "
								+ hakukohdeOid);
			}
			boolean didRemove;
			boolean wasEmpty;
			synchronized (oids) {
				didRemove = oids.remove(hakemus.getHakemusOid());
				wasEmpty = oids.isEmpty();
			}
			if (didRemove) {
				hakemukset.add(hakemus);
				return wasEmpty;
			} else {

				throw new RuntimeException(
						"Hakukohteelle "
								+ hakukohdeOid
								+ " yritettiin tuoda hakemusta joka ei kuulu kyseiseen hakukohteeseen tai se oli jo tuotu tähän hakukohteeseen!");
			}
		}

		public String getHakukohdeOid() {
			return hakukohdeOid;
		}

		public Collection<String> getHakemusOids() {
			return oids;
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
