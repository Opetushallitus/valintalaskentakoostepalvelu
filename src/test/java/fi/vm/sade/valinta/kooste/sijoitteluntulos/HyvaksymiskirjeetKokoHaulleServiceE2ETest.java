package fi.vm.sade.valinta.kooste.sijoitteluntulos;

import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import rx.functions.Action0;

import javax.ws.rs.client.Entity;
import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToNoContent;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockServer;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKU1;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKUKOHDE1;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKUKOHDE2;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.hakukohdeviite;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintaperusteet;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;

/**
 * @author Jussi Jartamo
 */
@Ignore
public class HyvaksymiskirjeetKokoHaulleServiceE2ETest {
    @Before
    public void startServer() throws Throwable{
        mockToNoContent(GET, "/seuranta-service/resources/seuranta/laskenta/otaSeuraavaLaskentaTyonAlle");
        startShared();
    }

    public static class Result<T> {
        private T result;
public Result(T result){
 this.result = result;
}

        public T getResult() {
            return result;
        }
    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulle() {
        try {
            HttpResource http = new HttpResource(resourcesAddress + "/sijoitteluntuloshaulle/hyvaksymiskirjeet");
            HakuV1RDTO h = new HakuV1RDTO();
            h.setHakukohdeOids(Arrays.asList(HAKUKOHDE1,HAKUKOHDE2));
            mockToReturnJson(GET, "/tarjonta-service/rest/v1/haku/HAKU1/", new Result(h));
            {
            HakukohdeV1RDTO hk = new HakukohdeV1RDTO();
            hk.setTarjoajaOids(Sets.newHashSet("T1","T2"));
            mockToReturnJson(GET, "/tarjonta-service/rest/v1/hakukohde/HAKUKOHDE1/", new Result(hk));
            }
            {
                HakukohdeV1RDTO hk = new HakukohdeV1RDTO();
                hk.setTarjoajaOids(Sets.newHashSet("T1","T2"));
                mockToReturnJson(GET, "/tarjonta-service/rest/v1/hakukohde/HAKUKOHDE2/", new Result(hk));
            }
            HakijaDTO hakija1 = new HakijaDTO();
            HakutoiveDTO hakutoiveDTO = new HakutoiveDTO();
            hakutoiveDTO.setHakukohdeOid(HAKUKOHDE1);
            HakutoiveenValintatapajonoDTO jono = new HakutoiveenValintatapajonoDTO();
            jono.setTila(HakemuksenTila.HYVAKSYTTY);
            hakutoiveDTO.setHakutoiveenValintatapajonot(Arrays.asList(jono));
            hakija1.setHakutoiveet(Sets.newTreeSet(Arrays.asList(hakutoiveDTO)));
            HakijaPaginationObject hp = new HakijaPaginationObject();
            hp.setResults(Arrays.asList(hakija1));
            mockToReturnJson(GET, "/sijoittelu-service/resources/sijoittelu/HAKU1/sijoitteluajo/latest/hakemukset.*", hp);

            TemplateHistory templateHistory = new TemplateHistory();
            templateHistory.setName("default");
            TemplateDetail detail = new TemplateDetail();
            detail.setName("sisalto");
            templateHistory.setTemplateReplacements(Arrays.asList(detail));
            mockToReturnJson(GET, "/viestintapalvelu/api/v1/template/getHistory.*", Arrays.asList(templateHistory));


            mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList());

            CyclicBarrier barrier = new CyclicBarrier(2);
            Action0 waitRequestForMax7Seconds = () ->{
                try {
                    barrier.await(17L, TimeUnit.SECONDS);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            };
             /*
            MockServer fakeValintalaskenta = new MockServer();


            AtomicBoolean first = new AtomicBoolean(true);
            mockForward(
                    fakeValintalaskenta.addHandler("/valintalaskenta-laskenta-service/resources/valintalaskenta/valintakokeet", exchange -> {
                        try {
                            // failataan tarkoituksella ensimmainen laskenta
                            if (first.getAndSet(false)) {
                                String resp = "ERR!";
                                exchange.sendResponseHeaders(505, resp.length());
                                exchange.getResponseBody().write(resp.getBytes());
                                exchange.getResponseBody().close();
                                return;
                            }
                            waitRequestForMax7Seconds.call();
                            String resp = "OK!";
                            exchange.sendResponseHeaders(200, resp.length());
                            exchange.getResponseBody().write(resp.getBytes());
                            exchange.getResponseBody().close();
                        } catch (Throwable t) {
                            throw new RuntimeException(t);
                        }
                    }));
            */
            Assert.assertEquals(200, http.getWebClient()
                    .query("hakuOid", HAKU1)
                    .post(Entity.json(Arrays.asList(HAKUKOHDE1, HAKUKOHDE2))).getStatus());
            waitRequestForMax7Seconds.call();
        } finally {
            mockServer.reset();
        }
    }
}
