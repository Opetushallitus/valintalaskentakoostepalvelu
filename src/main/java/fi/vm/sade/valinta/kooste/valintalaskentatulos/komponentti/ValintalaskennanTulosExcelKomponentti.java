package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.camel.Header;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.resource.ValintatapajonoResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.JarjestyskriteerituloksenTila;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.JonosijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValinnanvaiheDTO;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 */
@Component("luoValintalaskennanTuloksetXlsMuodossa")
public class ValintalaskennanTulosExcelKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskennanTulosExcelKomponentti.class);

	@Autowired
	private HakukohdeResource hakukohdeResource;
	// @Autowired
	// private ValinnanVaiheResource valinnanVaiheResource;
	@Autowired
	private ValintatapajonoResource valintatapajonoResource;

	@Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}")
	private String valintalaskentaResourceUrl;
	@Value("${valintalaskentakoostepalvelu.valintaperusteet.rest.url}")
	private String valintaperusteetUrl;

	public InputStream luoXls(@Header("haunNimi") String haunNimi,
			@Header("hakukohteenNimi") String hakukohteenNimi,
			@Property(OPH.HAKUOID) String hakuOid,
			@Property(OPH.HAKUKOHDEOID) String hakukohdeOid) throws Exception {
		LOG.debug("Yhteys {} HakukohdeResource.hakukohde({})", new Object[] {
				valintalaskentaResourceUrl, hakukohdeOid });

		List<ValinnanvaiheDTO> valinnanVaiheet = hakukohdeResource
				.hakukohde(hakukohdeOid);
		Collections.sort(valinnanVaiheet, new Comparator<ValinnanvaiheDTO>() {
			@Override
			public int compare(ValinnanvaiheDTO o1, ValinnanvaiheDTO o2) {
				if (o1 == null || o2 == null || o1.getCreatedAt() == null) {
					LOG.error("Sijoittelulta palautuu null valinnanvaiheita!");
					return 0;
				}
				return -1 * o1.getCreatedAt().compareTo(o2.getCreatedAt());
			}
		});

		// -jonosija
		// -nimi
		// sukunimi, etunimi
		// -järjestyskriteerit[0].arvo
		// -prioriteetti
		// -tuloksen tila
		List<Object[]> rivit = new ArrayList<Object[]>();

		rivit.add(new Object[] { haunNimi });
		rivit.add(new Object[] { hakukohteenNimi });
		rivit.add(new Object[] {});
		// Valinnanvaihe OID: 13770670692632664634655521339392
		// Päivämäärä: 08.09.2013 15:23:40
		// Valintatapajono: Varsinaisen valinnanvaiheen valintatapajono (v.)
		for (ValinnanvaiheDTO vaihe : valinnanVaiheet) {

			// ValinnanVaiheDTO vdto;
			// try {
			// vdto = valinnanVaiheResource.read(vaihe.getValinnanvaiheoid());
			// } catch (Exception e) {
			// LOG.error("Yhteys epäonnistui: {}/valinnanvaihe/{}",
			// valintaperusteetUrl, vaihe.getValinnanvaiheoid());
			// throw e;
			// }
			rivit.add(new Object[] { vaihe.getNimi(), "Valinnanvaiheen OID ",
					vaihe.getValinnanvaiheoid() });
			rivit.add(new Object[] { "Päivämäärä:",
					ExcelExportUtil.DATE_FORMAT.format(vaihe.getCreatedAt()) });
			if (!vaihe.getValintatapajono().isEmpty()) {
				rivit.add(new Object[] { vaihe.getValintatapajono().get(0)
						.getNimi() });
				rivit.add(new Object[] { "Valintatapajonon numero",
						vaihe.getJarjestysnumero() });
				rivit.add(new Object[] { "Jonosija", "Hakija", "Yhteispisteet",
						"Hakutoive", "Valintatieto" });
				for (ValintatapajonoDTO jono : vaihe.getValintatapajono()) {
					// fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO
					// vjdto;
					// try {
					// vjdto = valintatapajonoResource.readByOid(jono.getOid());
					// } catch (Exception e) {
					// LOG.error("Yhteys epäonnistui: {}/valintatapajono/{}",
					// valintaperusteetUrl, jono.getOid());
					// throw e;
					// }
					Collections.sort(jono.getJonosijat(),
							new Comparator<JonosijaDTO>() {
								@Override
								public int compare(JonosijaDTO o1,
										JonosijaDTO o2) {
									String o1sukunimi = o1.getSukunimi();
									if (o1 == null || o2 == null
											|| o1sukunimi == null) {
										return 0;
									}
									int c = o1sukunimi.compareTo(o2
											.getSukunimi());
									if (c == 0) {
										String o1etunimi = o1.getEtunimi();
										if (o1etunimi == null) {
											return 0;
										}
										return o1etunimi.compareTo(o2
												.getEtunimi());
									} else {
										return c;
									}
								}
							});
					for (JonosijaDTO sija : jono.getJonosijat()) {
						StringBuilder hakija = new StringBuilder();
						hakija.append(sija.getSukunimi()).append(", ")
								.append(sija.getEtunimi());
						String yhteispisteet = "--";
						try {
							yhteispisteet = sija.getJarjestyskriteerit()
									.first().getArvo().toString();
							// sija.getJarjestyskriteerit().
							// yhteispisteet =
							// sija.getJarjestyskriteerit().firstEntry().getValue().getArvo().toString();
						} catch (Exception e) {
							LOG.error(
									"Hakemukselle {}, nimi {} ei löytynyt yhteispisteitä!",
									new Object[] { sija.getHakemusOid(),
											hakija.toString() });
						}
						rivit.add(new Object[] { sija.getJonosija(),
								hakija.toString(), yhteispisteet,
								sija.getPrioriteetti(),
								suomennaTila(sija.getTuloksenTila()) });
					}
				}
				rivit.add(new Object[] {});
			}
		}
		return ExcelExportUtil
				.exportGridAsXls(rivit.toArray(new Object[][] {}));
	}

	private static String suomennaTila(JarjestyskriteerituloksenTila tila) {
		if (tila == null) {
			return "--";
		} else {
			return tila.toString();
		}
	}
}
