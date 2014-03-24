package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.TilaResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.Formatter;

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

	public InputStream luoXls(
			@Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
			@Simple("${property.hakukohdeOid}") String hakukohdeOid,
			@Simple("${property.hakuOid}") String hakuOid) {
		Map<String, List<Valintatulos>> valintatulosCache = new HashMap<String, List<Valintatulos>>();
		HakukohdeDTO hakukohde;
		try {
			hakukohde = sijoitteluajoResource
					.getHakukohdeBySijoitteluajoPlainDTO(hakuOid,
							sijoitteluajoId.toString(), hakukohdeOid);
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(
					"Sijoittelulta ei saa tuloksia. Tarkista että sijoittelu on ajettu. {} {}",
					e.getMessage(), e.getCause());
			throw new RuntimeException(
					"Sijoittelulta ei saa tuloksia. Tarkista että sijoittelu on ajettu.");
		}

		List<Object[]> rivit = new ArrayList<Object[]>();
		for (ValintatapajonoDTO jono : hakukohde.getValintatapajonot()) {
			rivit.add(new Object[] { "Valintatapajono", jono.getOid() });
			rivit.add(new Object[] { "Jonosija", "Hakemus", "Hakija",
					"Hakutoive", "Pisteet", "Sijoittelun tila",
					"Vastaanottotieto" });
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
				String valintaTieto = "--";
				for (Valintatulos valinta : valintaTulos) {
					if (jono.getOid().equals(valinta.getValintatapajonoOid())) {
						if (valinta.getTila() != null) {
							valintaTieto = valinta.getTila().toString();
						}
						break;
					}
				}
				nimi.append(hakemus.getSukunimi()).append(", ")
						.append(hakemus.getEtunimi());
				rivit.add(new Object[] { hakemus.getJonosija(), hakemusOid,
						nimi.toString(), hakemus.getPrioriteetti(),
						Formatter.suomennaNumero(hakemus.getPisteet()),
						hakemus.getTila(), valintaTieto });
			}
			rivit.add(new Object[] {});
		}
		return ExcelExportUtil
				.exportGridAsXls(rivit.toArray(new Object[][] {}));
	}
}
