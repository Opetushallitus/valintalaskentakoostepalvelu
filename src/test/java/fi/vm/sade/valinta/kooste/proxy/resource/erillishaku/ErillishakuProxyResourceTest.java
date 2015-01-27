package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku;

import fi.vm.sade.integrationtest.tomcat.SharedTomcat;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteTomcat;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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

    @Before
    public void startServer() {
        ValintaKoosteTomcat.startShared();
    }

    @Test
    public void testaaProxyResurssi() {
        LOG.error("{}",root + "/proxy/erillishaku/haku/"+hakuOid+"/hakukohde/" + hakukohdeOid);
        List<Hakemus> hakemukset = Collections.emptyList();
        List<ValintatietoValinnanvaiheDTO> valintatieto = Collections.emptyList();
        List<Valintatulos> valintatulokset = Collections.emptyList();
        List<ValinnanVaiheJonoillaDTO> valinnanvaihejonoilla = Collections.emptyList();
        HakukohdeDTO hakukohde = null;
        try {
            MockApplicationAsyncResource.setResult(hakemukset);
            MockSijoitteluAsyncResource.setResult(hakukohde);
            MockValintalaskentaAsyncResource.setResult(valintatieto);
            MockTilaAsyncResource.setResult(valintatulokset);
            MockValintaperusteetAsyncResource.setResult(valinnanvaihejonoilla);
            proxyResource.getWebClient().get();
        } finally {
            MockApplicationAsyncResource.clear();
            MockSijoitteluAsyncResource.clear();
            MockTilaAsyncResource.clear();
            MockValintalaskentaAsyncResource.clear();
            MockValintaperusteetAsyncResource.clear();
        }
    }
}
