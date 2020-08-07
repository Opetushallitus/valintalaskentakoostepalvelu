package fi.vm.sade.valinta.kooste.valintalaskenta;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Eligibility;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/** @author Jussi Jartamo */
public class EligibilitiesKonvertointiTest {
  private final Logger LOG = LoggerFactory.getLogger(EligibilitiesKonvertointiTest.class);

  @Ignore
  @Test
  public void t() throws JsonSyntaxException, IOException {
    // List<String> sq = Arrays.asList("s", "s");
    // sq.stream().collect(Collectors.toMap(s -> s, s -> 6));
    Hakemus h =
        new Gson()
            .fromJson(
                IOUtils.toString(
                    new ClassPathResource("1.2.246.562.11.00001038440").getInputStream()),
                Hakemus.class);
    Map<String, String> s =
        Converter.mapEligibilityAndStatus(
            h.getPreferenceEligibilities(), h.getAnswers().getHakutoiveet());
    for (Entry<String, String> e : s.entrySet()) {
      System.err.println(e);
    }
  }

  @Ignore
  @Test
  public void te() throws JsonSyntaxException, IOException {
    String hakukohdeOid = "";
    List<Hakemus> hakemukset =
        new Gson()
            .fromJson(
                IOUtils.toString(new ClassPathResource("xxlistfull").getInputStream()),
                new TypeToken<List<Hakemus>>() {}.getType());
    Map<String, Exception> epaonnistuneetKonversiot = Maps.newConcurrentMap();
    try {
      hakemukset.parallelStream()
          .filter(Objects::nonNull)
          .map(
              h -> {
                try {
                  HakemusWrapper hakemus = new HakuappHakemusWrapper(h);
                  return hakemus.toHakemusDto(new Valintapisteet(), Maps.newHashMap(), false);
                } catch (Exception e) {
                  epaonnistuneetKonversiot.put(h.getOid(), e);
                  return null;
                }
              })
          .collect(Collectors.toList());
    } catch (Exception e) {
      LOG.error(
          "Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle "
              + hakukohdeOid
              + " +  ja null hakemukselle",
          e);
      throw e;
    }
    if (!epaonnistuneetKonversiot.isEmpty()) {
      LOG.error(
          "Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle {} ja hakemuksille {}. Esimerkiksi {}!",
          hakukohdeOid,
          Arrays.toString(epaonnistuneetKonversiot.keySet().toArray()),
          epaonnistuneetKonversiot.values().iterator().next().getMessage());
      throw new RuntimeException(
          "Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle "
              + hakukohdeOid
              + " ja hakemuksille "
              + Arrays.toString(epaonnistuneetKonversiot.keySet().toArray())
              + "!");
    }
  }

  @Ignore
  @Test
  public void testaaEligibilities() throws JsonSyntaxException, IOException {

    List<Eligibility> eligibilities =
        new Gson()
            .fromJson(
                IOUtils.toString(
                    new ClassPathResource("valintalaskenta/eligibilities.json").getInputStream()),
                new TypeToken<List<Eligibility>>() {}.getType());

    List<Hakemus> obj =
        new Gson()
            .fromJson(
                IOUtils.toString(new ClassPathResource("listfull.json").getInputStream()),
                new TypeToken<List<Hakemus>>() {}.getType());
    Map<String, String> hakutoiveet = obj.iterator().next().getAnswers().getHakutoiveet();
    Converter.mapEligibilityAndStatus(eligibilities, hakutoiveet);
  }
}
