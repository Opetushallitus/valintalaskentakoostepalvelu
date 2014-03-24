package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.camel.Property;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.dto.ValintakoeNimi;

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

	public InputStream luoTuloksetXlsMuodossa(
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@Property("valintakoeOid") List<String> valintakoeOids,
			@Property("hakemusOids") List<String> hakemusOids) {
		boolean useWhitelist = hakemusOids != null && !hakemusOids.isEmpty();
		Set<String> whiteList = Collections.emptySet();
		if (useWhitelist) {
			whiteList = Sets.newHashSet(hakemusOids);
		}
		List<HakemusOsallistuminenTyyppi> tiedotHakukohteelle = valintatietoService
				.haeValintatiedotHakukohteelle(valintakoeOids, hakukohdeOid);

		List<ValintakoeNimi> tunnisteet = getTunnisteet(tiedotHakukohteelle);
		if (tunnisteet.isEmpty()) {
			return ExcelExportUtil
					.exportGridAsXls(new Object[][] {
							new Object[] { "Hakukohteelle ei löytynyt tuloksia annetuilla syötteillä!" },
							new Object[] { "Hakukohde OID", hakukohdeOid },
							new Object[] { "Valintakoe OID:it",
									Arrays.toString(valintakoeOids.toArray()) } });
		} else {
			List<Object[]> rows = new ArrayList<Object[]>();
			LOG.debug("Creating rows for Excel file!");
			ArrayList<String> otsikot = new ArrayList<String>();
			otsikot.addAll(Arrays.asList("Nimi", "Hakemus", "Laskettu pvm"));
			for (ValintakoeNimi n : tunnisteet) {
				otsikot.add(n.getNimi());
			}
			rows.add(otsikot.toArray());
			for (HakemusOsallistuminenTyyppi o : tiedotHakukohteelle) {
				if (useWhitelist) {
					// If whitelist in use then skip every hakemus that is not
					// in whitelist
					if (!whiteList.contains(o.getHakemusOid())) {
						continue;
					}
				}
				XMLGregorianCalendar calendar = o.getLuontiPvm();
				Date date = calendar.toGregorianCalendar().getTime();
				Map<String, ValintakoeOsallistuminenTyyppi> osallistumiset = new HashMap<String, ValintakoeOsallistuminenTyyppi>();
				for (ValintakoeOsallistuminenTyyppi v : o.getOsallistumiset()) {
					osallistumiset.put(v.getValintakoeTunniste(), v);
				}
				ArrayList<String> rivi = new ArrayList<String>();
				StringBuilder b = new StringBuilder();
				b.append(o.getSukunimi()).append(", ").append(o.getEtunimi());
				rivi.addAll(Arrays.asList(b.toString(), o.getHakemusOid(),
						ExcelExportUtil.DATE_FORMAT.format(date)));
				for (ValintakoeNimi tunniste : tunnisteet) {
					if (osallistumiset.containsKey(tunniste.getTunniste())) {
						rivi.add(suomenna(osallistumiset.get(
								tunniste.getTunniste()).getOsallistuminen()));
					} else {
						rivi.add("----");
					}
				}
				rows.add(rivi.toArray());
			}

			return ExcelExportUtil.exportGridAsXls(rows
					.toArray(new Object[][] {}));
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

	private List<ValintakoeNimi> getTunnisteet(
			List<HakemusOsallistuminenTyyppi> osallistujat) {
		Map<String, ValintakoeNimi> tunnisteet = Maps.newHashMap();
		for (HakemusOsallistuminenTyyppi osallistuja : osallistujat) {
			for (ValintakoeOsallistuminenTyyppi valintakoe : osallistuja
					.getOsallistumiset()) {
				if (!tunnisteet.containsKey(valintakoe.getValintakoeTunniste())) {
					String nimi = valintakoe.getNimi();
					if (nimi == null) {
						nimi = valintakoe.getValintakoeTunniste();
					}
					tunnisteet.put(
							valintakoe.getValintakoeTunniste(),
							new ValintakoeNimi(nimi, valintakoe
									.getValintakoeTunniste()));
				}
			}
		}
		return Lists.newArrayList(tunnisteet.values());
	}
}
