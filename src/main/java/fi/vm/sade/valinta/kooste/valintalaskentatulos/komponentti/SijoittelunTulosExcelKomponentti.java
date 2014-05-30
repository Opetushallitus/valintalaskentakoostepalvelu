package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.TilaHistoriaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.hakemus.dto.Yhteystiedot;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.TilaResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.Formatter;
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

	public InputStream luoXls(
			@Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
			@Simple("${property.hakukohdeOid}") String hakukohdeOid,
			@Simple("${property.hakuOid}") String hakuOid) {
		Map<String, List<Valintatulos>> valintatulosCache = new HashMap<String, List<Valintatulos>>();
		Map<String, Hakemus> hakemukset = Maps.newHashMap();
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
		for (ValintatapajonoDTO jono : hakukohde.getValintatapajonot()) {
			rivit.add(new Object[] { "Valintatapajono", jono.getOid() });
			rivit.add(new Object[] { "Jonosija", "Hakemus", "Hakija",
					//
					"Osoite", "Sähköposti", "Puhelinnumero",
					//
					"Hakutoive", "Pisteet", "Sijoittelun tila",
					"Vastaanottotieto", "Muokattu" });
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
				Hakemus application = haeHakemus(hakemusOid, hakemukset);
				Yhteystiedot yhteystiedot = Yhteystiedot
						.yhteystiedotHakemukselta(application);
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
						hakemus.getPrioriteetti(),
						Formatter.suomennaNumero(hakemus.getPisteet()),
						hakemus.getTila(), valintaTieto,
						muokattu(hakemus.getTilaHistoria()) });
			}
			rivit.add(new Object[] {});
		}
		return ExcelExportUtil
				.exportGridAsXls(rivit.toArray(new Object[][] {}));
	}

	private Hakemus haeHakemus(String hakemusOid,
			Map<String, Hakemus> hakemukset) {
		if (hakemukset.containsKey(hakemusOid)) {
			return hakemukset.get(hakemusOid);
		} else {
			Hakemus h = applicationResource.getApplicationByOid(hakemusOid);
			hakemukset.put(hakemusOid, h);
			return h;
		}
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
}
