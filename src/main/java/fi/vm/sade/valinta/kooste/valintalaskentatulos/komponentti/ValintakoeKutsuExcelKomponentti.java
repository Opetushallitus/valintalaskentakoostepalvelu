package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.hakemus.dto.Yhteystiedot;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.dto.ValintakoeNimi;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.dto.ValintakoeRivi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valintalaskenta.domain.dto.OsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintakoeOsallistuminenDTO;

/**
 * KOEKUTSUEXCEL
 *
 * @author Jussi Jartamo
 *         <p/>
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 *
 *  /haku-app/applications/listfull?appState=ACTIVE&appState=INCOMPLETE&rows=100000&asId={hakuOid}&aoOid={hakukohdeOid}
 *  @POST [oids] /valintaperusteet-service/resources/valintakoe
 *  @POST [oids] /valintalaskenta-laskenta-service/resources/valintatieto/hakukohde/{hakukohdeOid}
 *
 */
@Component("luoTuloksetXlsMuodossa")
public class ValintakoeKutsuExcelKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintakoeKutsuExcelKomponentti.class);

	public InputStream luoTuloksetXlsMuodossa(
			String haunNimi,
			String hakukohteenNimi,
			String hakukohdeOid,
			final Map<String, Koodi> maatJaValtiot1,
			final Map<String, Koodi> posti,
			List<HakemusOsallistuminenDTO> tiedotHakukohteelle,
			Map<String, ValintakoeDTO> valintakokeet,
			List<Hakemus> haetutHakemukset,
			Set<String> whiteList
	) throws Exception {
		final Map<String, String> nivelvaiheenKoekutsut = Maps.newHashMap();
		List<ValintakoeNimi> tunnisteet = Lists.newArrayList();
		for (Map.Entry<String, ValintakoeDTO> valintakoeEntry : valintakokeet.entrySet()) {
			ValintakoeDTO koe = valintakoeEntry.getValue();
			tunnisteet.add(new ValintakoeNimi(koe.getNimi(), koe.getSelvitettyTunniste()));
			if (Boolean.TRUE.equals(koe.getKutsutaankoKaikki())) {
				nivelvaiheenKoekutsut.put(valintakoeEntry.getKey(), "Kutsutaan");
			}
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
			final boolean useWhitelist = !whiteList.isEmpty();
			Function<String,Boolean> onkoHakemusWhiteListilla = hakemusOid -> {
				return !useWhitelist || whiteList.contains(hakemusOid);
			};
			Map<String, Hakemus> mapping = haetutHakemukset.stream().collect(Collectors.toMap(a -> a.getOid(), a -> a));
			Map<String, ValintakoeRivi> hakemusJaRivi = Maps.newHashMap();
			BiFunction<ValintakoeRivi, ValintakoeRivi, ValintakoeRivi> remappingFunction = (v1,v2) -> {
				return v1.merge(v2);
			};
			{

				for (HakemusOsallistuminenDTO tieto : tiedotHakukohteelle) {
					if (!onkoHakemusWhiteListilla.apply(tieto.getHakemusOid())) {
						// If whitelist in use then skip every hakemus that is
						// not
						// in whitelist
						continue;
					}
					if(mapping.containsKey(tieto.getHakemusOid())) {
						final ValintakoeRivi v0 = muodostaValintakoeRivi(
								posti, maatJaValtiot1,
								mapping.get(tieto.getHakemusOid()), tieto, tunnisteet);
						hakemusJaRivi.merge(tieto.getHakemusOid(), v0, remappingFunction);
					}
					//
				}
				if (!nivelvaiheenKoekutsut.isEmpty()) {
					for (Hakemus hakemus : haetutHakemukset) {
						if (!onkoHakemusWhiteListilla.apply(hakemus.getOid())) {
							continue;
						}
						HakemusWrapper wrapper = new HakemusWrapper(hakemus);
						Osoite osoite = OsoiteHakemukseltaUtil
								.osoiteHakemuksesta(hakemus, null, null);

						ValintakoeRivi v = new ValintakoeRivi(
								wrapper.getSukunimi(), wrapper.getEtunimi(),
								KoodistoCachedAsyncResource.haeKoodistaArvo(posti.get(wrapper.getSuomalainenPostinumero()), KieliUtil.SUOMI, wrapper.getSuomalainenPostinumero()),
								KoodistoCachedAsyncResource.haeKoodistaArvo(maatJaValtiot1.get(wrapper.getAsuinmaa()), KieliUtil.ENGLANTI, wrapper.getAsuinmaa()),
								wrapper,
								hakemus.getOid(), null, nivelvaiheenKoekutsut,
								osoite,
								Yhteystiedot.yhteystiedotHakemukselta(hakemus), true);
						hakemusJaRivi.merge(hakemus.getOid(), v, remappingFunction);
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
			otsikot.addAll(Arrays.asList("Sukunimi", "Etunimi",

					"Henkilötunnus",
					"Syntymäaika",
					"Sukupuoli",
					"Lähiosoite",
					"Postinumero",
					"Postitoimipaikka",
					"Osoite (ulkomaa)",
					"Postinumero (ulkomaa)",
					"Kaupunki (ulkomaa)",
					"Asuinmaa",
					"Kansalaisuus",
					"Kansallinen ID",
					"Passin numero",
					"Sähköpostiosoite",
					"Puhelinnumero", "Hakemus", "Laskettu pvm"));
			List<String> oids = Lists.newArrayList();
			for (ValintakoeNimi n : tunnisteet) {
				otsikot.add(n.getNimi());
				oids.add(n.getSelvitettyTunniste());
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

	private String suomenna(OsallistuminenDTO osallistuminen) {
		if (osallistuminen != null) {
			if (OsallistuminenDTO.EI_OSALLISTU.equals(osallistuminen)) {
				return "Ei kutsuta";
			} else if (OsallistuminenDTO.OSALLISTUU.equals(osallistuminen)) {
				return "Kutsutaan";
			} else if (OsallistuminenDTO.VIRHE.equals(osallistuminen)) {
				return "Virheellinen";
			}
		}
		return StringUtils.EMPTY;
	}

	private ValintakoeRivi muodostaValintakoeRivi(
			Map<String, Koodi> posti,
			Map<String, Koodi> maatJaValtiot1,
			Hakemus h,
			HakemusOsallistuminenDTO o, List<ValintakoeNimi> tunnisteet) {
		Date date = o.getLuontiPvm();
		Map<String, ValintakoeOsallistuminenDTO> osallistumiset = new HashMap<>();
		for (ValintakoeOsallistuminenDTO v : o.getOsallistumiset()) {
			osallistumiset.put(v.getValintakoeTunniste(), v);
		}
		ArrayList<String> rivi = new ArrayList<>();
		StringBuilder b = new StringBuilder();
		b.append(o.getSukunimi()).append(", ").append(o.getEtunimi());
		rivi.addAll(Arrays.asList(b.toString(), o.getHakemusOid(),
				date == null ? "" : ExcelExportUtil.DATE_FORMAT.format(date)));
		boolean osallistuuEdesYhteen = false;
		Map<String, String> osallistumistiedot = Maps.newHashMap();
		for (ValintakoeNimi tunniste : tunnisteet) {
			if (osallistumiset.containsKey(tunniste.getSelvitettyTunniste())) {
				ValintakoeOsallistuminenDTO vodto = osallistumiset.get(
						tunniste.getSelvitettyTunniste());
                OsallistuminenDTO osallistuminen = vodto.getOsallistuminen();
				if (OsallistuminenDTO.OSALLISTUU.equals(osallistuminen)) {
					osallistuuEdesYhteen = true;
				}
				osallistumistiedot.put(tunniste.getSelvitettyTunniste(),
						suomenna(vodto.getOsallistuminen()));
			} else {
				osallistumistiedot.put(tunniste.getSelvitettyTunniste(), "Määrittelemätön");
			}
		}
		//Hakemus h = applicationResource.getApplicationByOid(o.getHakemusOid());
		Osoite osoite = OsoiteHakemukseltaUtil
				.osoiteHakemuksesta(h, null, null);
		HakemusWrapper wrapper = new HakemusWrapper(h);
		return new ValintakoeRivi(o.getSukunimi(), o.getEtunimi(),
				KoodistoCachedAsyncResource.haeKoodistaArvo(posti.get(wrapper.getSuomalainenPostinumero()), KieliUtil.SUOMI, wrapper.getSuomalainenPostinumero()),
				KoodistoCachedAsyncResource.haeKoodistaArvo(maatJaValtiot1.get(wrapper.getAsuinmaa()), KieliUtil.ENGLANTI, wrapper.getAsuinmaa()),
				wrapper,
				o.getHakemusOid(), date, osallistumistiedot, osoite,
				Yhteystiedot.yhteystiedotHakemukselta(h), osallistuuEdesYhteen);
	}
}
