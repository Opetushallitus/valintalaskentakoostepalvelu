package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Property;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.tulos.dto.TilaHistoriaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.hakemus.dto.Yhteystiedot;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;
import fi.vm.sade.valinta.kooste.sijoittelu.exception.SijoittelultaEiSisaltoaPoikkeus;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.TilaResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.util.HakemusUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti luo sijoittelun tulokset excel tiedostoksi!
 */
@Component("sijoittelunTulosXlsKomponentti")
public class SijoittelunTulosExcelKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(SijoittelunTulosExcelKomponentti.class);

	@Autowired
	private SijoitteluResource sijoitteluajoResource;

	@Autowired
	private TilaResource tilaResource;

	@Autowired
	private ApplicationResource applicationResource;

	public InputStream luoXls(List<Valintatulos> tilat,
			@Simple("${property.sijoitteluajoId}") String sijoitteluajoId,
			@Property("preferoitukielikoodi") String preferoitukielikoodi,
			@Property("hakukohdeNimi") String hakukohdeNimi,
			@Property("tarjoajaNimi") String tarjoajaNimi,
			@Simple("${property.hakukohdeOid}") String hakukohdeOid,
			@Simple("${property.hakuOid}") String hakuOid) {
		Map<String, List<Valintatulos>> valintatulosCache = new HashMap<String, List<Valintatulos>>();
		HakukohdeDTO hakukohde;
		try {
			hakukohde = sijoitteluajoResource
					.getHakukohdeBySijoitteluajoPlainDTO(hakuOid,
							sijoitteluajoId, hakukohdeOid);
		} catch (Exception e) {
			LOG.error(
					"Sijoittelulta ei saa tuloksia. Tarkista että sijoittelu on ajettu. {} {}",
					e.getMessage(), e.getCause());
			throw new RuntimeException(
					"Sijoittelulta ei saa tuloksia. Tarkista että sijoittelu on ajettu.");
		}
		if (hakukohde == null) {
			LOG.error("Hakukohteessa ei hakijoita tai hakukohdetta ei ole olemassa!");
			throw new SijoittelultaEiSisaltoaPoikkeus(
					"Hakukohteessa ei hakijoita tai hakukohdetta ei ole olemassa!");
		}
		List<Object[]> rivit = new ArrayList<Object[]>();
		Collections.sort(hakukohde.getValintatapajonot(),
				new Comparator<ValintatapajonoDTO>() {
					@Override
					public int compare(ValintatapajonoDTO o1,
							ValintatapajonoDTO o2) {
						if (o1.getPrioriteetti() == null
								|| o2.getPrioriteetti() == 0) {
							return 0;
						}
						return o1.getPrioriteetti().compareTo(
								o2.getPrioriteetti());
					}
				});

		Map<String, Map<String, IlmoittautumisTila>> valintatapajononTilat = valintatapajononTilat(tilat);

		Map<String, Hakemus> hakemukset = haeHakemukset(hakukohdeOid);
		rivit.add(new Object[] { tarjoajaNimi });
		rivit.add(new Object[] { hakukohdeNimi });
		rivit.add(new Object[] {});
		for (ValintatapajonoDTO jono : hakukohde.getValintatapajonot()) {
			if (jono == null || jono.getHakemukset() == null
					|| jono.getHakemukset().isEmpty()) {
				continue;
			}
			Map<String, IlmoittautumisTila> hakemusTilat = Collections
					.emptyMap();
			if (valintatapajononTilat.containsKey(jono.getOid())) {
				hakemusTilat = valintatapajononTilat.get(jono.getOid());
				if (hakemusTilat == null) {
					hakemusTilat = Collections.emptyMap();
				}
			}
			// rivit.add(new Object[] { "Valintatapajono", jono.getOid() });
			rivit.add(new Object[] { "Jonosija", "Hakemus", "Hakija",
					//
					"Osoite", "Sähköposti", "Puhelinnumero", "Lupa julkaisuun",
					//
					"Hakutoive", "Pisteet", "Sijoittelun tila",
					"Vastaanottotieto", "Ilmoittautumistieto", "Muokattu" });
			// ComparatorChain jonosijaAndHakijaNameComparator = new
			// ComparatorChain(
			// compare by jonosija

			// jonosijaAndHakijaNameComparator
			// .addComparator(HakemusComparator.DEFAULT);
			Collections.sort(jono.getHakemukset(),
					new Comparator<HakemusDTO>() {
						private int ordinal(HakemusDTO h) {
							// harkinnanvaraisesti hyväksytyt, hyväksytyt,
							// varalla, peruuntuneet, hylätyt
							switch (h.getTila()) {
							case HYLATTY:
								return 6;
							case VARALLA:
								return 2;
							case PERUUNTUNUT:
								return 3;
							case HYVAKSYTTY:
								if (h.isHyvaksyttyHarkinnanvaraisesti()) {
									return 0;
								} else {
									return 1;
								}
							case HARKINNANVARAISESTI_HYVAKSYTTY:
								return 0;
							case PERUNUT:
								return 4;
							case PERUUTETTU:
								return 5;
							default:
								return 7;
							}
						}

						@Override
						public int compare(HakemusDTO o1, HakemusDTO o2) {
							return new Integer(ordinal(o1))
									.compareTo(ordinal(o2));
						}
					});
			for (HakemusDTO hakemus : jono.getHakemukset()) {
				// Jonosija Tasasijan jonosija Hakija Hakemus Hakutoive
				// Sijoittelun tila Vastaanottotieto
				StringBuilder nimi = new StringBuilder();
				String hakemusOid = hakemus.getHakemusOid();
				//
				// OVT-6335
				//
				List<Valintatulos> valintaTulos = Collections
						.<Valintatulos> emptyList();
				if (valintatulosCache.containsKey(hakemusOid)) {
					valintaTulos = valintatulosCache.get(hakemusOid);
				} else {
					try {
						valintaTulos = tilaResource.hakemus(hakemusOid);
						if (valintaTulos == null) {
							LOG.error(
									"Hakemukselle {} ei saatu valintatuloksia sijoittelusta!",
									new Object[] { hakemusOid });
							valintatulosCache.put(hakemusOid,
									Collections.<Valintatulos> emptyList());
						} else {
							valintatulosCache.put(hakemusOid, valintaTulos);
						}
					} catch (Exception e) {
						e.printStackTrace();
						LOG.error(
								"Hakemukselle {} ei saatu valintatuloksia sijoittelusta! {}",
								new Object[] { hakemusOid, e.getMessage() });
						valintatulosCache.put(hakemusOid,
								Collections.<Valintatulos> emptyList());
					}
				}
				String valintaTieto = StringUtils.EMPTY; // "--"
				for (Valintatulos valinta : valintaTulos) {
					if (jono.getOid().equals(valinta.getValintatapajonoOid())) {
						if (valinta.getTila() != null) {
							valintaTieto = HakemusUtil.tilaConverter(
									valinta.getTila(), preferoitukielikoodi);
						}
						break;
					}
				}
				nimi.append(hakemus.getSukunimi()).append(", ")
						.append(hakemus.getEtunimi());
				Hakemus application = hakemukset.get(hakemusOid);
				HakemusWrapper wrapper = new HakemusWrapper(application);
				boolean lupaJulkaisuun = wrapper.getLupaJulkaisuun();
				Yhteystiedot yhteystiedot = Yhteystiedot
						.yhteystiedotHakemukselta(application);
				String ilmoittautumistieto = StringUtils.EMPTY;
				try {
					ilmoittautumistieto = HakemusUtil.tilaConverter(
							hakemusTilat.get(hakemusOid), preferoitukielikoodi);
				} catch (Exception e) {
				}
				rivit.add(new Object[] {
						hakemus.getJonosija(),
						hakemusOid,
						nimi.toString(),
						//
						OsoiteHakemukseltaUtil.osoiteHakemuksesta(application,
								null, null),
						yhteystiedot.getSahkoposti(),
						yhteystiedot.getPuhelinnumerotAsString(),
						//
						HakemusUtil.lupaJulkaisuun(lupaJulkaisuun),
						//
						hakemus.getPrioriteetti(),
						Formatter.suomennaNumero(hakemus.getPisteet()),
						HakemusUtil.tilaConverter(hakemus.getTila(),
								preferoitukielikoodi,
								hakemus.isHyvaksyttyHarkinnanvaraisesti(),
								true, hakemus.getVarasijanNumero()),
						valintaTieto, ilmoittautumistieto,
						muokattu(hakemus.getTilaHistoria()) });
			}
			rivit.add(new Object[] {});
		}
		return ExcelExportUtil
				.exportGridAsXls(rivit.toArray(new Object[][] {}));
	}

	private Map<String, Hakemus> haeHakemukset(String hakukohdeOid) {
		Map<String, Hakemus> tmp = Maps.newHashMap();
		for (Hakemus h : applicationResource.getApplicationsByOid(hakukohdeOid,
				ApplicationResource.ACTIVE_AND_INCOMPLETE, Integer.MAX_VALUE)) {
			tmp.put(h.getOid(), h);
		}
		return tmp;
	}

	private String muokattu(List<TilaHistoriaDTO> h) {
		if (h == null || h.isEmpty()) {
			return StringUtils.EMPTY;
		} else {
			Collections.sort(h, new Comparator<TilaHistoriaDTO>() {
				@Override
				public int compare(TilaHistoriaDTO o1, TilaHistoriaDTO o2) {
					if (o1 == null || o2 == null || o1.getLuotu() == null
							|| o2.getLuotu() == null) {
						return 0;
					}
					return -1 * o1.getLuotu().compareTo(o2.getLuotu());

				}
			});
			return Formatter.paivamaara(h.get(0).getLuotu());
		}
	}

	private Map<String, Map<String, IlmoittautumisTila>> valintatapajononTilat(
			List<Valintatulos> tilat) {
		Map<String, Map<String, IlmoittautumisTila>> t = Maps.newHashMap();
		try {
			for (Valintatulos tulos : tilat) {
				Map<String, IlmoittautumisTila> jono;
				if (!t.containsKey(tulos.getValintatapajonoOid())) {
					t.put(tulos.getValintatapajonoOid(),
							jono = Maps
									.<String, IlmoittautumisTila> newHashMap());
				} else {
					jono = t.get(tulos.getValintatapajonoOid());
				}
				jono.put(tulos.getHakemusOid(), tulos.getIlmoittautumisTila());
			}
		} catch (Exception e) {
			LOG.error(
					"Ilmoittautumistiloja ei saatu luettua sijoittelusta! {}",
					Arrays.toString(e.getStackTrace()));
		}
		return t;
	}
}
