package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValinnanvaiheDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValintatapajonoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import static java.util.Collections.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

/**
 * @author Jussi Jartamo
 *
 *
 */
public class ErillishakuProxyResourceTest {
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    String hakuOid = "1.2.246.562.5.2013080813081926341928";
    String hakukohdeOid = "1.2.246.562.5.72607738902";
    final HttpResource proxyResource = new HttpResource(root + "/proxy/erillishaku/haku/"+hakuOid+"/hakukohde/" + hakukohdeOid);
    final Gson GSON = DateDeserializer.GSON;

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }
    private static String classpathResourceAsString(String path) throws Exception {
        return IOUtils.toString(new ClassPathResource(path).getInputStream());
    }

    @Test
    public void testaaProxyResurssiIlmanLaskentaaHakukohteelle() throws Exception {
        List<ValintatietoValinnanvaiheDTO> valintatieto = emptyList(); // ei valinnanvaiheita
        List<Hakemus> hakemukset = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/ilmanlaskentaa/listfull.json"), new TypeToken<List<Hakemus>>() {}.getType());
        List<Valintatulos> valintatulokset = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/ilmanlaskentaa/tila.json"), new TypeToken<List<Valintatulos>>() {}.getType());
        List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla = GSON.fromJson(
                classpathResourceAsString("/proxy/erillishaku/data/ilmanlaskentaa/valinnanvaihe.json"),
                new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType()
        );
        HakukohdeDTO hakukohde = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/ilmanlaskentaa/hakukohde.json"), HakukohdeDTO.class);
        initMocks(hakemukset, hakukohde, valintatieto, valintatulokset, valinnanvaihejonoilla);
        List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = callErillishakuProxy();
        mergeValinnanvaiheDTOs.forEach(valinnanvaihe -> valinnanvaihe.getValintatapajonot().forEach(valintatapajono -> assertFalse(valintatapajono.isKaytetaanValintalaskentaa())));
    }

    @Test
    public void testaaProxyResurssiHakukohteelleLaskennalla() throws Exception {
        List<ValintatietoValinnanvaiheDTO> valintatieto = GSON.fromJson(
                classpathResourceAsString("/proxy/erillishaku/data/laskennalla/laskenta_valinnanvaihe.json"),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {}.getType()
        );
        List<Hakemus> hakemukset = GSON.fromJson(
                classpathResourceAsString("/proxy/erillishaku/data/laskennalla/listfull.json"),
                new TypeToken<List<Hakemus>>() {}.getType()
        );
        List<Valintatulos> valintatulokset = GSON.fromJson(
                classpathResourceAsString("/proxy/erillishaku/data/laskennalla/tila.json"),
                new TypeToken<List<Valintatulos>>() {}.getType()
        );
        List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla = GSON.fromJson(
                classpathResourceAsString("/proxy/erillishaku/data/laskennalla/valinnanvaihe.json"),
                new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType()
        );
        HakukohdeDTO hakukohde = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/hakukohde.json"), HakukohdeDTO.class);
        HakukohdeDTO hakukohde_1422533823300 = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/hakukohde_1422533823300.json"), HakukohdeDTO.class);
        initMocks(hakemukset, hakukohde, valintatieto, valintatulokset, valinnanvaihejonoilla);
        MockSijoitteluAsyncResource.getResultMap().put(1422533823300L, hakukohde_1422533823300);
        List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = callErillishakuProxy();
        mergeValinnanvaiheDTOs.forEach(valinnanvaihe -> valinnanvaihe.getValintatapajonot().forEach(valintatapajono -> assertTrue(valintatapajono.isKaytetaanValintalaskentaa())));
    }

    @Test
    public void testaaProxyResurssinJononGenerointiKunValintaperusteetPuuttuu() throws Exception {
        initMocks(emptyList(), new HakukohdeDTO(), emptyList(), emptyList(), emptyList());
        List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = callErillishakuProxy();
        assertEquals(1, mergeValinnanvaiheDTOs.size());
        MergeValinnanvaiheDTO vv = mergeValinnanvaiheDTOs.iterator().next();
        assertEquals(1, vv.getValintatapajonot().size());
    }

    @Test
    public void excludesLaskentasIfValinnanvaiheDoesNotExist() throws Exception {
        List<ValintatietoValinnanvaiheDTO> valintatieto = GSON.fromJson(
                classpathResourceAsString("/proxy/erillishaku/data/laskennalla/laskenta_valinnanvaihe.json"),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {}.getType()
        );
        List<Hakemus> hakemukset = GSON.fromJson(
                classpathResourceAsString("/proxy/erillishaku/data/laskennalla/listfull.json"),
                new TypeToken<List<Hakemus>>() {}.getType()
        );
        List<Valintatulos> valintatulokset = GSON.fromJson(
                classpathResourceAsString("/proxy/erillishaku/data/laskennalla/tila.json"),
                new TypeToken<List<Valintatulos>>() {}.getType()
        );
        HakukohdeDTO hakukohde = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/hakukohde.json"), HakukohdeDTO.class);
        HakukohdeDTO hakukohde_1422533823300 = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/hakukohde_1422533823300.json"), HakukohdeDTO.class);
        initMocks(hakemukset, hakukohde, valintatieto, valintatulokset, emptyList());
        MockSijoitteluAsyncResource.getResultMap().put(1422533823300L, hakukohde_1422533823300);
        List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = callErillishakuProxy();
        assertEquals(1, mergeValinnanvaiheDTOs.size());
        List<MergeValintatapajonoDTO> valintatapajonot = mergeValinnanvaiheDTOs.get(0).getValintatapajonot();
        assertEquals(valintatapajonot.size(), 1);
        assertFalse(valintatapajonot.get(0).isKaytetaanValintalaskentaa());
    }

    @After
    public void resetMocks() {
        MockApplicationAsyncResource.clear();
        MockSijoitteluAsyncResource.clear();
        MockTilaAsyncResource.clear();
        MockValintalaskentaAsyncResource.clear();
        MockValintaperusteetAsyncResource.clear();
    }

    private void initMocks(
            List<Hakemus> hakemukset,
            HakukohdeDTO hakukohde,
            List<ValintatietoValinnanvaiheDTO> valintatieto,
            List<Valintatulos> valintatulokset,
            List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla
    ) {
        MockApplicationAsyncResource.setResult(hakemukset);
        MockSijoitteluAsyncResource.setResult(hakukohde);
        MockValintalaskentaAsyncResource.setResult(valintatieto);
        MockTilaAsyncResource.setResult(valintatulokset);
        MockValintaperusteetAsyncResource.setResult(valinnanvaihejonoilla);
    }

    public List<MergeValinnanvaiheDTO> callErillishakuProxy() throws Exception {
        return GSON.fromJson(
                IOUtils.toString((InputStream) proxyResource.getWebClient().get().getEntity()),
                new TypeToken<List<MergeValinnanvaiheDTO>>() {}.getType()
        );
    }
}
