package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import fi.vm.sade.integrationtest.tomcat.SharedTomcat;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteTomcat;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.lang.reflect.Type;
import static java.util.Collections.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Jussi Jartamo
 *
 *
 */
public class ErillishakuProxyResourceTest {

    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final static Logger LOG = LoggerFactory.getLogger(ErillishakuProxyResourceTest.class);
    final String root = "http://localhost:" + SharedTomcat.port + "/valintalaskentakoostepalvelu/resources";
    String hakuOid = "1.2.246.562.5.2013080813081926341928";
    String hakukohdeOid = "1.2.246.562.5.72607738902";
    String tarjoajaOid = "1.2.246.562.10.591352080610";
    String valintatapajonoOid = "14090336922663576781797489829886";
    final HttpResource proxyResource = new HttpResource(root + "/proxy/erillishaku/haku/"+hakuOid+"/hakukohde/" + hakukohdeOid);
    final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new JsonDeserializer() {
                @Override
                public Object deserialize(JsonElement json, Type typeOfT,
                                          JsonDeserializationContext context)
                        throws JsonParseException {
                    return new Date(json.getAsJsonPrimitive().getAsLong());
                }
            })
            .create();

    @Before
    public void startServer() {
        ValintaKoosteTomcat.startShared();
    }
    private static String classpathResourceAsString(String path) throws Exception {
        return IOUtils.toString(new ClassPathResource(path).getInputStream());
    }
    @Test
    public void testaaProxyResurssiIlmanLaskentaaHakukohteelle() throws Exception {
        LOG.error("{}",root + "/proxy/erillishaku/haku/"+hakuOid+"/hakukohde/" + hakukohdeOid);
        List<ValintatietoValinnanvaiheDTO> valintatieto = emptyList(); // ei valinnanvaiheita
        //
        List<Hakemus> hakemukset = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/ilmanlaskentaa/listfull.json"), new TypeToken<List<Hakemus>>() {
        }.getType());
        List<Valintatulos> valintatulokset = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/ilmanlaskentaa/tila.json"), new TypeToken<List<Valintatulos>>() {
                }.getType());
        List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla =
                GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/ilmanlaskentaa/valinnanvaihe.json"), new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {
                }.getType());
        HakukohdeDTO hakukohde = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/ilmanlaskentaa/hakukohde.json"), HakukohdeDTO.class);
        try {
            MockApplicationAsyncResource.setResult(hakemukset);
            MockSijoitteluAsyncResource.setResult(hakukohde);
            MockValintalaskentaAsyncResource.setResult(valintatieto);
            MockTilaAsyncResource.setResult(valintatulokset);
            MockValintaperusteetAsyncResource.setResult(valinnanvaihejonoilla);
            List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = GSON.fromJson(IOUtils.toString((InputStream) proxyResource.getWebClient().get().getEntity()), new TypeToken<List<MergeValinnanvaiheDTO>>() {}.getType());
            mergeValinnanvaiheDTOs.forEach(valinnanvaihe->

                {
                    valinnanvaihe.getValintatapajonot().forEach(
                            valintatapajono -> {
                                Assert.assertFalse(valintatapajono.isKaytetaanValintalaskentaa());
                            }
                    );
                }

                );
            //LOG.info("{}",new GsonBuilder().setPrettyPrinting().create().toJson(mergeValinnanvaiheDTOs));
            }finally {
            MockApplicationAsyncResource.clear();
            MockSijoitteluAsyncResource.clear();
            MockTilaAsyncResource.clear();
            MockValintalaskentaAsyncResource.clear();
            MockValintaperusteetAsyncResource.clear();
        }
    }
    @Test
    public void testaaProxyResurssiHakukohteelleLaskennalla() throws Exception {
        LOG.error("{}",root + "/proxy/erillishaku/haku/"+hakuOid+"/hakukohde/" + hakukohdeOid);
        List<ValintatietoValinnanvaiheDTO> valintatieto = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/laskenta_valinnanvaihe.json"), new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {
        }.getType());
        //
        List<Hakemus> hakemukset = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/listfull.json"), new TypeToken<List<Hakemus>>() {
        }.getType());
        List<Valintatulos> valintatulokset = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/tila.json"), new TypeToken<List<Valintatulos>>() {
        }.getType());
        List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla =
                GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/valinnanvaihe.json"), new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {
                }.getType());
        HakukohdeDTO hakukohde = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/hakukohde.json"),HakukohdeDTO.class);
        HakukohdeDTO hakukohde_1422533823300 = GSON.fromJson(classpathResourceAsString("/proxy/erillishaku/data/laskennalla/hakukohde_1422533823300.json"),HakukohdeDTO.class);
        try {
            MockApplicationAsyncResource.setResult(hakemukset);
            MockSijoitteluAsyncResource.setResult(hakukohde);
            MockSijoitteluAsyncResource.getResultMap().put(1422533823300L, hakukohde_1422533823300);
            MockValintalaskentaAsyncResource.setResult(valintatieto);
            MockTilaAsyncResource.setResult(valintatulokset);
            MockValintaperusteetAsyncResource.setResult(valinnanvaihejonoilla);
            List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = GSON.fromJson(IOUtils.toString((InputStream) proxyResource.getWebClient().get().getEntity()), new TypeToken<List<MergeValinnanvaiheDTO>>() {
            }.getType());
            mergeValinnanvaiheDTOs.forEach(valinnanvaihe -> {
                valinnanvaihe.getValintatapajonot().forEach(
                        valintatapajono -> {
                            Assert.assertTrue(valintatapajono.isKaytetaanValintalaskentaa());
                        }
                );
            });
            //LOG.info("{}",new GsonBuilder().setPrettyPrinting().create().toJson(mergeValinnanvaiheDTOs));
        } finally {
            MockApplicationAsyncResource.clear();
            MockSijoitteluAsyncResource.clear();
            MockTilaAsyncResource.clear();
            MockValintalaskentaAsyncResource.clear();
            MockValintaperusteetAsyncResource.clear();
        }
    }

    @Test
    public void testaaProxyResurssinJononGenerointiKunValintaperusteetPuuttuu() throws Exception {
        try {
            MockApplicationAsyncResource.setResult(emptyList());
            MockSijoitteluAsyncResource.setResult(new HakukohdeDTO());
            MockValintalaskentaAsyncResource.setResult(emptyList());
            MockTilaAsyncResource.setResult(emptyList());
            MockValintaperusteetAsyncResource.setResult(emptyList());
            List<MergeValinnanvaiheDTO> mergeValinnanvaiheDTOs = GSON.fromJson(IOUtils.toString((InputStream) proxyResource.getWebClient().get().getEntity()), new TypeToken<List<MergeValinnanvaiheDTO>>() {
            }.getType());
            Assert.assertEquals(1, mergeValinnanvaiheDTOs.size());//
            MergeValinnanvaiheDTO vv = mergeValinnanvaiheDTOs.iterator().next();
            Assert.assertEquals(1, vv.getValintatapajonot().size());
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(mergeValinnanvaiheDTOs));
        } finally {
            MockApplicationAsyncResource.clear();
            MockSijoitteluAsyncResource.clear();
            MockTilaAsyncResource.clear();
            MockValintalaskentaAsyncResource.clear();
            MockValintaperusteetAsyncResource.clear();
        }


    }

}
