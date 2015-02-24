package fi.vm.sade.valinta.kooste.valintalaskenta.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class HakemuksetConverterUtil {
	private static final Logger LOG = LoggerFactory
			.getLogger(HakemuksetConverterUtil.class);

	public static List<HakemusDTO> muodostaHakemuksetDTO(String hakukohdeOid,
			List<Hakemus> hakemukset, List<Oppija> oppijat) {
		Map<String, String> hakemusOidToPersonOid;
		List<String> virheellisetHakemukset = Lists.newArrayList();
		try {
			hakemusOidToPersonOid = hakemukset.stream()
			//
					.filter(Objects::nonNull)
					//
					.collect(Collectors.toMap(h -> h.getOid(), h -> {
						String personOid = h.getPersonOid();
						if (personOid == null) {
							virheellisetHakemukset.add(h.getOid());
							return "";
						}
						return personOid;
					}));
		} catch (Exception e) {
			LOG.error(
					"Hakemukset to personOid mappauksessa virhe hakukohteelle {}. Syy {}!",
					hakukohdeOid, e.getMessage());
			throw e;
		}
		if (!virheellisetHakemukset.isEmpty()) {
			LOG.error(
					"Hakukohteessa {} hakemuksilta {} puuttui personOid! Jalkikasittely ehka tekematta! Tarkista hakemusten tiedot!",
					hakukohdeOid,
					Arrays.toString(virheellisetHakemukset.toArray()));
			throw new RuntimeException(
					"Hakukohteessa "
							+ hakukohdeOid
							+ " hakemuksilta "
							+ Arrays.toString(virheellisetHakemukset.toArray())
							+ " puuttui personOid! Jalkikasittely ehka tekematta! Tarkista hakemusten tiedot!");
		}
		List<HakemusDTO> hakemusDtot;
		Map<String, Exception> epaonnistuneetKonversiot = Maps
				.newConcurrentMap();
		try {

			hakemusDtot = hakemukset.parallelStream()
			//
					.filter(Objects::nonNull)
					//
					.map(h -> {
						try {
							return Converter.hakemusToHakemusDTO(h);
						} catch (Exception e) {
							epaonnistuneetKonversiot.put(h.getOid(), e);
							return null;
						}
					}).collect(Collectors.toList());
		} catch (Exception e) {
			LOG.error(
					"Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle {} ja null hakemukselle. Syy {}!",
					hakukohdeOid, e.getMessage());
			throw e;
		}
		if (!epaonnistuneetKonversiot.isEmpty()) {
			LOG.error(
					"Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle {} ja hakemuksille {}. Esimerkiksi {}!",
					hakukohdeOid, Arrays.toString(epaonnistuneetKonversiot
							.keySet().toArray()), epaonnistuneetKonversiot
							.values().iterator().next().getMessage());
			throw new RuntimeException(
					"Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle "
							+ hakukohdeOid
							+ " ja hakemuksille "
							+ Arrays.toString(epaonnistuneetKonversiot.keySet()
									.toArray()) + "!");
		}

		try {
			if (oppijat != null) {
				Map<String, Oppija> oppijaNumeroJaOppija = oppijat.stream()
						.collect(
								Collectors.toMap(o -> o.getOppijanumero(),
										o -> o, (o1, o2) -> o2));
				hakemusDtot.forEach(h -> {
					String personOid = hakemusOidToPersonOid.get(h
							.getHakemusoid());
					if (personOid != null
							&& oppijaNumeroJaOppija.containsKey(personOid)) {
						Oppija oppija = oppijaNumeroJaOppija.get(personOid);
						Map<String, AvainArvoDTO> arvot = h.getAvaimet().stream().collect(Collectors.toMap(a -> a.getAvain(), a-> a));
						Map<String, AvainArvoDTO> sureArvot = OppijaToAvainArvoDTOConverter.convert(oppija).stream().collect(Collectors.toMap(a -> a.getAvain(), a -> a));
						Map<String, AvainArvoDTO> merge = Maps.newHashMap();
						merge.putAll(arvot);
						merge.putAll(sureArvot);
						h.setAvaimet(merge.entrySet().stream().map(s -> s.getValue()).collect(Collectors.toList()));
					}
				});
			}
		} catch (Exception e) {
			LOG.error(
					"\r\n###\r\n### SURE YO-arvosanojen konversiossa odottamaton virhe {}\r\n###",
					e.getMessage());
			throw e;
		}
		return hakemusDtot;

	}
}
