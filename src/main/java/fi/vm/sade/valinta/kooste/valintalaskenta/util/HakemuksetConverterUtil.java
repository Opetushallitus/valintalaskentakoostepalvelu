package fi.vm.sade.valinta.kooste.valintalaskenta.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.apache.commons.lang.StringUtils;
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
			List<Hakemus> hakemukset, List<Oppija> oppijat,
			ParametritDTO parametritDTO) {
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
		Map<String, Exception> errors = Maps.newHashMap();
		try {
			if (oppijat != null) {
				Map<String, Oppija> oppijaNumeroJaOppija = oppijat.stream()
						.collect(
								Collectors.toMap(o -> o.getOppijanumero(),
										o -> o, (o1, o2) -> o2));

				hakemusDtot.stream().filter(h -> h.getHakemusoid() != null).forEach(h -> {
					try {
						String personOid = hakemusOidToPersonOid.get(h
								.getHakemusoid());
						if (personOid != null
								&& oppijaNumeroJaOppija.containsKey(personOid)) {
							Oppija oppija = oppijaNumeroJaOppija.get(personOid);

							Map<String, AvainArvoDTO> arvot = Optional.ofNullable(h.getAvaimet())
									.orElse(Collections.emptyList()).stream().filter(Objects::nonNull).filter(a -> StringUtils.isNotBlank(a.getAvain()))
									.collect(Collectors.groupingBy(a -> a.getAvain(), Collectors.mapping(a -> a, Collectors.toList())))
									.entrySet().stream()
									.map(a -> {
										if (a.getValue().size() != 1) {
											LOG.error("Duplikaattiavain {} hakemuksella {} hakukohteessa {}", a.getKey(), h.getHakemusoid(), hakukohdeOid);
											errors.put(h.getHakemusoid(), new RuntimeException("Duplikaattiavain "+a.getKey()+" hakemuksella "+ h.getHakemusoid()+" hakukohteessa " + hakukohdeOid));
										}
										return a.getValue().iterator().next();
									})
									.collect(Collectors.toMap(a -> a.getAvain(), a -> a));

							Map<String, AvainArvoDTO> sureArvot = OppijaToAvainArvoDTOConverter.convert(oppija, parametritDTO).stream().collect(Collectors.toMap(a -> a.getAvain(), a -> a));
							Map<String, AvainArvoDTO> merge = Maps.newHashMap();
							merge.putAll(arvot);
							merge.putAll(sureArvot);
							h.setAvaimet(merge.entrySet().stream().map(s -> s.getValue()).collect(Collectors.toList()));
						}
					} catch (Exception e) {
						errors.put(h.getHakemusoid(), e);
					}
				});
			}
		} catch (Exception e) {
			LOG.error(
					"\r\n###\r\n### SURE YO-arvosanojen konversiossa (hakukohde={}) odottamaton virhe {}\r\n{}\r\n###",
					hakukohdeOid, e.getMessage(), Arrays.toString(e.getStackTrace()));
			throw e;
		}
		if(!errors.isEmpty()) {
			errors.entrySet().forEach(err -> {
				Exception e = err.getValue();
				LOG.error(
						"\r\n###\r\n### SURE YO-arvosanojen konversiossa (hakukohde={}, hakemus={}) odottamaton virhe {}\r\n{}\r\n###",
						hakukohdeOid, err.getKey(), e.getMessage(), Arrays.toString(e.getStackTrace()));
			});
			throw new RuntimeException(errors.entrySet().iterator().next().getValue());
		}
		return hakemusDtot;

	}
}
