package fi.vm.sade.valinta.kooste.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Eligibility;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class ConverterMappingsTest {

    @Test
    public void testaaEligibilitiesOikeallaDatalla()
            throws JsonSyntaxException, IOException {
        List<Hakemus> hakemukset = new Gson().fromJson(IOUtils
                .toString(new ClassPathResource("listfull2_eligibilities.json")
                        .getInputStream()), new TypeToken<List<Hakemus>>() {
        }.getType());
        Hakemus hakemus = hakemukset.stream()
                .filter(h -> "1.2.246.562.11.00000977230".equals(h.getOid()))
                .distinct().iterator().next();
        HakemusDTO dto = Converter.hakemusToHakemusDTO(hakemus, Maps.newHashMap());
        // LOG.error("\r\n{}", new GsonBuilder().setPrettyPrinting().create()
        // .toJson(dto));
        assertTrue(dto
                .getAvaimet()
                .stream()
                .filter(pari -> "preference1-Koulutus-id-eligibility"
                        .equals(pari.getAvain())
                        && "NOT_CHECKED".equals(pari.getArvo())).distinct()
                .iterator().hasNext());
    }

    @Test
    public void testaaHakukohderyhmienLisaysOikeallaDatalla()
            throws JsonSyntaxException, IOException {
        List<Hakemus> hakemukset = new Gson().fromJson(IOUtils
                .toString(new ClassPathResource("listfull2_eligibilities.json")
                        .getInputStream()), new TypeToken<List<Hakemus>>() {
        }.getType());
        Hakemus hakemus = hakemukset.stream()
                .filter(h -> "1.2.246.562.11.00000977230".equals(h.getOid()))
                .distinct().iterator().next();


        ArrayList<String> a = Lists.newArrayList("ryhmaOid1", "ryhmaOid2");
        Map<String, List<String>> hakukohdeRyhmasForHakukohdes = ImmutableMap.of("1.2.246.562.20.49132232288", a);

        HakemusDTO dto = Converter.hakemusToHakemusDTO(hakemus, hakukohdeRyhmasForHakukohdes);
        assertEquals(a, dto.getHakukohteet().get(0).getHakukohdeRyhmatOids());
    }

    @Test
    public void testaaEligibilitiesMappaustaNullArvoilla() {
        assertTrue(Collections.emptyMap().equals(
                Converter.mapEligibilityAndStatus(null, null)));
        assertTrue(Converter.mapEligibilityAndStatus(
                Arrays.asList(new Eligibility("", "", "", "")), null).isEmpty());
        Map<String, String> m = Maps.newHashMap();
        m.put("preference1-Koulutus-id", "hk1");
        assertTrue(Converter.mapEligibilityAndStatus(null, m).isEmpty());
    }

    @Test
    public void testaaEligibilitiesMappaustaEiMatsaa() {
        Map<String, String> m = Maps.newHashMap();
        m.put("preference1-Koulutus-id", "hk1");
        assertTrue(Converter.mapEligibilityAndStatus(
                Arrays.asList(new Eligibility("", "", "", "")), m).isEmpty());
    }

    @Test
    public void testaaEligibilitiesMappaustaMatsaa() {
        Map<String, String> m = Maps.newHashMap();
        m.put("preference1-Koulutus-id", "hk1");
        Map<String, String> ans = Converter.mapEligibilityAndStatus(
                Arrays.asList(new Eligibility("hk1", "status1", "", "")), m);
        assertFalse(ans.isEmpty());
        assertTrue(ans.size() == 1);
    }

    @Test
    public void testaaEligibilitiesParsintaa() {
        Map<String, String> m = Maps.newHashMap();
        m.put("preference1-Koulutus-id", "hk1");
        Map<String, String> ans = Converter.mapEligibilityAndStatus(
                Arrays.asList(new Eligibility("hk1", "AUTOMATICALLY_CHECKED_ELIGIBLE", "", "")), m);
        assertFalse(ans.isEmpty());
        assertTrue(ans.size() == 1);
        assertTrue(ans.entrySet().iterator().next().getValue().equals("ELIGIBLE"));
    }

    @Test
    public void testaaEligibilitiesMappaustaMatsaakoKunYlimaaraisiaAvaimia() {
        Map<String, String> m = Maps.newHashMap();
        m.put("preference1-Koulutus-id", "hk1");
        m.put("preference2-Koulutus-id", "hk2");
        Map<String, String> ans = Converter.mapEligibilityAndStatus(
                Arrays.asList(new Eligibility("hk1", "status1", "", "")), m);
        assertFalse(ans.isEmpty());
        assertTrue(ans.size() == 1);
        assertTrue(ans.entrySet().iterator().next().getKey()
                .equals("preference1-Koulutus-id-eligibility"));
    }

    @Test
    public void testaaEligibilitiesMappaustaMatsaakoKunYlimaaraisiaEligibilityja() {
        Map<String, String> m = Maps.newHashMap();
        m.put("preference1-Koulutus-id", "hk1");
        Map<String, String> ans = Converter.mapEligibilityAndStatus(Arrays
                .asList(new Eligibility("hk1", "status1", "", ""), new Eligibility(
                        "hk2", "status2", "", "")), m);
        assertFalse(ans.isEmpty());
        assertTrue(ans.size() == 1);
        assertTrue(ans.entrySet().iterator().next().getValue()
                .equals("status1"));
    }

    @Test
    public void testaaArvosanaFiletrointi()
            throws JsonSyntaxException, IOException {
        List<Hakemus> hakemukset = new Gson().fromJson(IOUtils
                .toString(new ClassPathResource("osaaminen_ilman_suorituksia.json")
                        .getInputStream()), new TypeToken<List<Hakemus>>() {
        }.getType());
        Hakemus hakemus = hakemukset.stream()
                .filter(h -> "1.2.246.562.11.00003000803".equals(h.getOid()))
                .distinct().iterator().next();
        HakemusDTO dto = Converter.hakemusToHakemusDTO(hakemus, Maps.newHashMap());

        final int prefixes = dto.getAvaimet().stream()
                .filter(a -> a.getAvain().startsWith("PK_") || a.getAvain().startsWith("LK_"))
                .collect(toList())
                .size();

        final int paattoToditusVuosi = dto.getAvaimet()
                .stream()
                .filter(h -> h.getAvain().equals("PK_PAATTOTODISTUSVUOSI"))
                .collect(toList())
                .size();

        assertEquals(1, prefixes);

        // PK_PAATTOTODISTUSVUOSI
        assertEquals(paattoToditusVuosi, 1);

        final AvainArvoDTO yleinen_kielitutkinto_sv = dto.getAvaimet().stream()
                .filter(a -> a.getAvain().equals("yleinen_kielitutkinto_sv"))
                .findFirst().get();

        Assert.assertNotNull(yleinen_kielitutkinto_sv);
    }

}
