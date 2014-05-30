package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.camel.Header;
import org.apache.camel.Property;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetValintakoeResource;
import fi.vm.sade.valinta.kooste.hakemus.dto.Yhteystiedot;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.dto.ValintakoeNimi;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.dto.ValintakoeRivi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

/**
 * KOEKUTSUEXCEL
 * 
 * @author Jussi Jartamo
 *         <p/>
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 */
@Component("luoTuloksetXlsMuodossa")
public class ValintalaskentaTulosExcelKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaTulosExcelKomponentti.class);

	@Resource(name = "valintatietoService")
	private ValintatietoService valintatietoService;

	@Autowired
	private ValintaperusteetValintakoeResource valintaperusteetValintakoeResource;
	@Autowired
	private HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti;
	@Autowired
	private ApplicationResource applicationResource;

	public InputStream luoTuloksetXlsMuodossa(
			@Header("haunNimi") String haunNimi,
			@Header("hakukohteenNimi") String hakukohteenNimi,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property("valintakoeOid") List<String> valintakoeOids,
			@Property("hakemusOids") List<String> hakemusOids) throws Exception {
		if (valintakoeOids == null || valintakoeOids.isEmpty()) {
			LOG.error("Ei voida luoda exceliä ilman valintakoeoideja!");
			throw new RuntimeException(
					"Ei voida luoda valintakokeista exceliä ilman että syötetään vähintään yksi valintakoeOid!");
		}
		// Map<String, ValintakoeNimi> tunnisteet = Maps.newHashMap();
		final Map<String, String> nivelvaiheenKoekutsut = Maps.newHashMap();
		List<ValintakoeNimi> tunnisteet = Lists.newArrayList();
		for (String oid : valintakoeOids) {
			ValintakoeDTO koe = valintaperusteetValintakoeResource
					.readByOid(oid);
			tunnisteet.add(new ValintakoeNimi(koe.getNimi(), koe.getOid()));
			if (Boolean.TRUE.equals(koe.getKutsutaankoKaikki())) {
				nivelvaiheenKoekutsut.put(oid, "Kutsutaan");
			}
		}
		if (tunnisteet.isEmpty()) {
			return ExcelExportUtil
					.exportGridAsXls(new Object[][] {
							new Object[] { "Hakukohteelle ei löytynyt tuloksia annetuilla syötteillä!" },
							new Object[] { "Hakukohde OID", hakukohdeOid },
							new Object[] { "Valintakoe OID:it",
									Arrays.toString(valintakoeOids.toArray()) } });
		}
		try {
			Collections.sort(tunnisteet, new Comparator<ValintakoeNimi>() {
				@Override
				public int compare(ValintakoeNimi o1, ValintakoeNimi o2) {
					if (o1 == null || o2 == null || o1.getNimi() == null
							|| o2.getNimi() == null) {
						LOG.error("Valintaperusteista palautui null nimisiä hakukohteita!");
						return 0;
					}
					return o1.getNimi().compareTo(o2.getNimi());
				}
			});
			boolean useWhitelist = hakemusOids != null
					&& !hakemusOids.isEmpty();
			Set<String> whiteList = Collections.emptySet();
			if (useWhitelist) {
				whiteList = Sets.newHashSet(hakemusOids);
			}
			Map<String, ValintakoeRivi> hakemusJaRivi = Maps.newHashMap();
			{
				List<HakemusOsallistuminenTyyppi> tiedotHakukohteelle = valintatietoService
						.haeValintatiedotHakukohteelle(valintakoeOids,
								hakukohdeOid);
				for (HakemusOsallistuminenTyyppi tieto : tiedotHakukohteelle) {
					if (useWhitelist) {
						// If whitelist in use then skip every hakemus that is
						// not
						// in whitelist
						if (!whiteList.contains(tieto.getHakemusOid())) {
							continue;
						}
					}
					hakemusJaRivi.put(tieto.getHakemusOid(),
							muodostaValintakoeRivi(tieto, tunnisteet));
					//
				}
				if (!nivelvaiheenKoekutsut.isEmpty()) {

					List<SuppeaHakemus> hakemukset = haeHakukohteenHakemuksetKomponentti
							.haeHakukohteenHakemukset(hakukohdeOid);
					for (SuppeaHakemus hakemus : hakemukset) {
						if (useWhitelist) {
							// If whitelist in use then skip every hakemus that
							// is
							// not
							// in whitelist
							if (!whiteList.contains(hakemus.getOid())) {
								continue;
							}
						}
						Hakemus h = applicationResource
								.getApplicationByOid(hakemus.getOid());
						Osoite osoite = OsoiteHakemukseltaUtil
								.osoiteHakemuksesta(h, null, null);

						ValintakoeRivi v = new ValintakoeRivi(
								hakemus.getLastName(), hakemus.getFirstNames(),
								hakemus.getOid(), null, nivelvaiheenKoekutsut,
								osoite,
								Yhteystiedot.yhteystiedotHakemukselta(h), true);

						ValintakoeRivi v2 = hakemusJaRivi.get(hakemus.getOid());
						if (v2 == null) {
							hakemusJaRivi.put(hakemus.getOid(), v);
						} else {
							hakemusJaRivi.put(hakemus.getOid(), v.merge(v2));
						}

					}
				}
			}
			List<ValintakoeRivi> rivit = Lists.newArrayList(hakemusJaRivi
					.values());
			Collections.sort(rivit);

			List<Object[]> rows = new ArrayList<Object[]>();
			rows.add(new Object[] { haunNimi });
			rows.add(new Object[] { hakukohteenNimi });
			rows.add(new Object[] {});

			LOG.debug("Creating rows for Excel file!");
			ArrayList<String> otsikot = new ArrayList<String>();
			otsikot.addAll(Arrays.asList("Nimi", "Osoite", "Sähköpostiosoite",
					"Puhelinnumero", "Hakemus", "Laskettu pvm"));
			List<String> oids = Lists.newArrayList();
			for (ValintakoeNimi n : tunnisteet) {
				otsikot.add(n.getNimi());
				oids.add(n.getOid());
			}
			rows.add(otsikot.toArray());
			for (ValintakoeRivi rivi : rivit) {
				if (rivi.isOsallistuuEdesYhteen()) {
					rows.add(rivi.toArray(oids));
				}

			}

			return ExcelExportUtil.exportGridAsXls(rows
					.toArray(new Object[][] {}));
		} catch (Exception e) {
			LOG.error("Jotain meni pieleen!");
			LOG.error("{}\r\n{}", e.getMessage(),
					Arrays.toString(e.getStackTrace()));
			throw e;
		}
	}

	private String suomenna(Osallistuminen osallistuminen) {
		if (osallistuminen != null) {
			if (Osallistuminen.EI_OSALLISTU.equals(osallistuminen)) {
				return "Ei kutsuta";
			} else if (Osallistuminen.OSALLISTUU.equals(osallistuminen)) {
				return "Kutsutaan";
			} else if (Osallistuminen.VIRHE.equals(osallistuminen)) {
				return "Virheellinen";
			}
		}
		return StringUtils.EMPTY;
	}

	private ValintakoeRivi muodostaValintakoeRivi(
			HakemusOsallistuminenTyyppi o, List<ValintakoeNimi> tunnisteet) {

		XMLGregorianCalendar calendar = o.getLuontiPvm();
		Date date = calendar.toGregorianCalendar().getTime();
		Map<String, ValintakoeOsallistuminenTyyppi> osallistumiset = new HashMap<String, ValintakoeOsallistuminenTyyppi>();
		for (ValintakoeOsallistuminenTyyppi v : o.getOsallistumiset()) {
			osallistumiset.put(v.getValintakoeOid(), v);
		}
		ArrayList<String> rivi = new ArrayList<String>();
		StringBuilder b = new StringBuilder();
		b.append(o.getSukunimi()).append(", ").append(o.getEtunimi());
		rivi.addAll(Arrays.asList(b.toString(), o.getHakemusOid(),
				ExcelExportUtil.DATE_FORMAT.format(date)));
		boolean osallistuuEdesYhteen = false;
		Map<String, String> osallistumistiedot = Maps.newHashMap();
		for (ValintakoeNimi tunniste : tunnisteet) {
			if (osallistumiset.containsKey(tunniste.getOid())) {
				Osallistuminen osallistuminen = osallistumiset.get(
						tunniste.getOid()).getOsallistuminen();
				if (Osallistuminen.OSALLISTUU.equals(osallistuminen)) {
					osallistuuEdesYhteen = true;
				}
				osallistumistiedot.put(tunniste.getOid(),
						suomenna(osallistumiset.get(tunniste.getOid())
								.getOsallistuminen()));

			} else {
				osallistumistiedot.put(tunniste.getOid(), "----");
			}
		}
		Hakemus h = applicationResource.getApplicationByOid(o.getHakemusOid());
		Osoite osoite = OsoiteHakemukseltaUtil
				.osoiteHakemuksesta(h, null, null);
		return new ValintakoeRivi(o.getSukunimi(), o.getEtunimi(),
				o.getHakemusOid(), date, osallistumistiedot, osoite,
				Yhteystiedot.yhteystiedotHakemukselta(h), osallistuuEdesYhteen);
	}
}
