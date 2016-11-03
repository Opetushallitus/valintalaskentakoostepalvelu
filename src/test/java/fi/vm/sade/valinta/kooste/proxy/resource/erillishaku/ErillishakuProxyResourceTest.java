package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.http.HttpResourceImpl;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValinnanvaiheDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValintatapajonoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    final HttpResourceImpl proxyResource = new HttpResourceImpl(root + "/proxy/erillishaku/haku/"+hakuOid+"/hakukohde/" + hakukohdeOid);
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
        Authentication auth = new UsernamePasswordAuthenticationToken("admin",null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        List<ValintatietoValinnanvaiheDTO> valintatieto = emptyList(); // ei valinnanvaiheita
        List<Hakemus> hakemukset = hakemuksetFromJson("/proxy/erillishaku/data/ilmanlaskentaa/listfull.json");
        List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla = valinnanvaihejonoillaFromJson("/proxy/erillishaku/data/ilmanlaskentaa/valinnanvaihe.json");
        HakukohdeDTO hakukohde = hakukohdeFromJson("/proxy/erillishaku/data/ilmanlaskentaa/hakukohde.json");
        initMocks(hakemukset, hakukohde, valintatieto, valinnanvaihejonoilla);
        List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = callErillishakuProxy();
        mergeValinnanvaiheDTOs.forEach(valinnanvaihe -> valinnanvaihe.getValintatapajonot().forEach(valintatapajono -> assertFalse(valintatapajono.isKaytetaanValintalaskentaa())));
    }

    @Test
    public void testaaProxyResurssiHakukohteelleLaskennalla() throws Exception {
        List<ValintatietoValinnanvaiheDTO> valintatieto = valintatietoFromJson("/proxy/erillishaku/data/laskennalla/laskenta_valinnanvaihe.json");
        List<Hakemus> hakemukset = hakemuksetFromJson("/proxy/erillishaku/data/laskennalla/listfull.json");
        List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla = valinnanvaihejonoillaFromJson("/proxy/erillishaku/data/laskennalla/valinnanvaihe.json");
        HakukohdeDTO hakukohdeWithSijoittelu = hakukohdeFromJson("/proxy/erillishaku/data/laskennalla/hakukohde_1422533823300.json");
        initMocksWithSijoittelu(hakemukset, hakukohdeWithSijoittelu, valintatieto, valinnanvaihejonoilla);
        List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = callErillishakuProxy();
        mergeValinnanvaiheDTOs.forEach(valinnanvaihe -> valinnanvaihe.getValintatapajonot().forEach(valintatapajono -> assertTrue(valintatapajono.isKaytetaanValintalaskentaa())));
    }

    @Test
    public void testaaProxyResurssinJononGenerointiKunValintaperusteetPuuttuu() throws Exception {
        initMocks(emptyList(), new HakukohdeDTO(), emptyList(),emptyList());
        List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = callErillishakuProxy();
        assertEquals(1, mergeValinnanvaiheDTOs.size());
        MergeValinnanvaiheDTO vv = mergeValinnanvaiheDTOs.iterator().next();
        assertEquals(1, vv.getValintatapajonot().size());
    }

    @Test
    public void excludesLaskentasIfValinnanvaiheDoesNotExist() throws Exception {
        List<ValintatietoValinnanvaiheDTO> valintatieto = valintatietoFromJson("/proxy/erillishaku/data/laskennalla/laskenta_valinnanvaihe.json");
        List<Hakemus> hakemukset = hakemuksetFromJson("/proxy/erillishaku/data/laskennalla/listfull.json");
        List<Valintatulos> valintatulokset = valintatuloksetFromJson("/proxy/erillishaku/data/laskennalla/tila.json");
        HakukohdeDTO hakukohdeWithSijoittelu = hakukohdeFromJson(("/proxy/erillishaku/data/laskennalla/hakukohde_1422533823300.json"));
        initMocksWithSijoittelu(hakemukset, hakukohdeWithSijoittelu, valintatieto, emptyList());
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
        MockValintalaskentaAsyncResource.clear();
        MockValintaperusteetAsyncResource.clear();
    }

    private List<ValintatietoValinnanvaiheDTO> valintatietoFromJson(String path) throws Exception {
        return GSON.fromJson(
                classpathResourceAsString(path),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {
                }.getType()
        );
    }

    private HakukohdeDTO hakukohdeFromJson(String path) throws Exception {
        return GSON.fromJson(classpathResourceAsString(path), HakukohdeDTO.class);
    }

    private List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoillaFromJson(String path) throws Exception {
        return GSON.fromJson(classpathResourceAsString(path), new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType());
    }

    private List<Valintatulos> valintatuloksetFromJson(String path) throws Exception {
        return GSON.fromJson(classpathResourceAsString(path), new TypeToken<List<Valintatulos>>() {}.getType());
    }

    private List<Hakemus> hakemuksetFromJson(String path) throws Exception {
        return GSON.fromJson(classpathResourceAsString(path), new TypeToken<List<Hakemus>>() {}.getType());
    }

    private void initMocks(
            List<Hakemus> hakemukset,
            HakukohdeDTO hakukohde,
            List<ValintatietoValinnanvaiheDTO> valintatieto,
            List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla
    ) {
        MockApplicationAsyncResource.setResult(hakemukset);
        MockSijoitteluAsyncResource.setResult(hakukohde);
        MockValintalaskentaAsyncResource.setResult(valintatieto);
        MockValintaperusteetAsyncResource.setResult(valinnanvaihejonoilla);
    }

    private void initMocksWithSijoittelu(List<Hakemus> hakemukset,
                                         HakukohdeDTO hakukohde,
                                         List<ValintatietoValinnanvaiheDTO> valintatieto,
                                         List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla
    ) {
        initMocks(hakemukset, hakukohde, valintatieto, valinnanvaihejonoilla);
        MockSijoitteluAsyncResource.getResultMap().put(hakukohde.getSijoitteluajoId(), hakukohde);
    }

    public List<MergeValinnanvaiheDTO> callErillishakuProxy() throws Exception {
        return GSON.fromJson(
                IOUtils.toString((InputStream) proxyResource.getWebClient().get().getEntity()),
                new TypeToken<List<MergeValinnanvaiheDTO>>() {}.getType()
        );
    }
}
