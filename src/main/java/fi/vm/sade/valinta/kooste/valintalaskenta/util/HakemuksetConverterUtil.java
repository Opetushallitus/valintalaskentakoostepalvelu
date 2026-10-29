package fi.vm.sade.valinta.kooste.valintalaskenta.util;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("HakemuksetConverterUtil")
public class HakemuksetConverterUtil {
  public static final String ATARU_POHJAKOULUTUS_VUOSI = "pohjakoulutus_vuosi";

  private static final Logger LOG = LoggerFactory.getLogger(HakemuksetConverterUtil.class);

  public List<HakemusDTO> muodostaHakemuksetDTOfromHakemukset(
      String hakukohdeOid,
      Map<String, List<String>> hakukohdeRyhmasForHakukohdes,
      List<HakemusWrapper> hakemukset,
      List<Valintapisteet> valintapisteet) {
    ensurePersonOids(hakemukset, hakukohdeOid);
    return hakemuksetToHakemusDTOs(
        hakukohdeOid,
        hakemukset,
        ofNullable(valintapisteet).orElse(emptyList()),
        hakukohdeRyhmasForHakukohdes);
  }

  private List<HakemusDTO> hakemuksetToHakemusDTOs(
      String hakukohdeOid,
      List<HakemusWrapper> hakemukset,
      List<Valintapisteet> valintapisteet,
      Map<String, List<String>> hakukohdeRyhmasForHakukohdes) {
    List<HakemusDTO> hakemusDtot;
    Map<String, Valintapisteet> hakemusOIDtoValintapisteet =
        valintapisteet.stream().collect(Collectors.toMap(Valintapisteet::getHakemusOID, v -> v));
    Map<String, Exception> epaonnistuneetKonversiot = Maps.newConcurrentMap();
    hakemusDtot =
        getHakemusDTOS(
            hakukohdeOid,
            hakemukset,
            hakukohdeRyhmasForHakukohdes,
            hakemusOIDtoValintapisteet,
            epaonnistuneetKonversiot);
    if (!epaonnistuneetKonversiot.isEmpty()) {
      RuntimeException e =
          new RuntimeException(
              String.format(
                  "Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle %s ja hakemuksille %s. Esimerkiksi %s!",
                  hakukohdeOid,
                  Arrays.toString(epaonnistuneetKonversiot.keySet().toArray()),
                  epaonnistuneetKonversiot.values().iterator().next().getMessage()));
      LOG.error("hakemuksetToHakemusDTOs", e);
      throw e;
    }
    return hakemusDtot;
  }

  private List<HakemusDTO> getHakemusDTOS(
      String hakukohdeOid,
      List<HakemusWrapper> hakemukset,
      Map<String, List<String>> hakukohdeRyhmasForHakukohdes,
      Map<String, Valintapisteet> hakemusOIDtoValintapisteet,
      Map<String, Exception> epaonnistuneetKonversiot) {
    List<HakemusDTO> hakemusDtot;
    try {
      hakemusDtot =
          hakemukset.parallelStream()
              .filter(Objects::nonNull)
              .map(
                  h -> {
                    try {
                      return h.toHakemusDto(
                          hakemusOIDtoValintapisteet.get(h.getOid()),
                          hakukohdeRyhmasForHakukohdes, // TODO there maybe come null pisteet here
                          true);
                    } catch (Exception e) {
                      epaonnistuneetKonversiot.put(h.getOid(), e);
                      return null;
                    }
                  })
              .collect(toList());
    } catch (Exception e) {
      LOG.error(
          String.format(
              "Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle %s ja null hakemukselle.",
              hakukohdeOid),
          e);
      throw e;
    }
    return hakemusDtot;
  }

  private void ensurePersonOids(List<HakemusWrapper> hakemukset, String hakukohdeOid) {
    final List<HakemusWrapper> noPersonOid =
        hakemukset.stream().filter(h -> StringUtils.isBlank(h.getPersonOid())).collect(toList());
    if (!noPersonOid.isEmpty()) {
      String hakemusOids =
          noPersonOid.stream().map(HakemusWrapper::getOid).collect(Collectors.joining(", "));
      RuntimeException e =
          new RuntimeException(
              String.format(
                  "Hakukohteessa %s hakemuksilta %s puuttui personOid! Jalkikasittely ehka tekematta! Tarkista hakemusten tiedot!",
                  hakukohdeOid, hakemusOids));
      LOG.error("ensurePersonOids", e);
      throw e;
    }
  }
}
