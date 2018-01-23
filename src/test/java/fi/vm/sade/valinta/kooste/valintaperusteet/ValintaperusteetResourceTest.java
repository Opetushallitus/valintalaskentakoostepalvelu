package fi.vm.sade.valinta.kooste.valintaperusteet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetValinnanVaiheDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.mocks.MockValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import fi.vm.sade.valinta.kooste.valintaperusteet.ValintaperusteetResource.ValintaperusteetResourceResult;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ValintaperusteetResourceTest {

    private final String hakukohdeOid = "1.2.246.562.5.28143628072";

    private final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources/valintaperusteet";
    private final HttpResourceBuilder.WebClientExposingHttpResource hakukohteenValintaperusteetResource = new HttpResourceBuilder()
            .address(String.format("%s/hakukohde/%s/kayttaaValintalaskentaa", root, hakukohdeOid))
            .buildExposingWebClientDangerously();

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
        Mocks.reset();
    }

    @Test
    public void testEiValintaperusteita() {
        assertFalse(kayttaaValintalaskentaa(Collections.emptyList()));
    }

    @Test
    public void onValinnanvaihtaEiValintalaskentaaMissaanJonossa() {
        List<ValintaperusteetDTO> valintaperusteetDTOs = createValintaperusteetDTOs(false, false, false, false);
        assertFalse(kayttaaValintalaskentaa(valintaperusteetDTOs));
    }

    @Test
    public void onValinnanvaihtaLaskentaaMuussaKuinViimeisessaVaiheessa() {
        List<ValintaperusteetDTO> valintaperusteetDTOs = createValintaperusteetDTOs(true, true, false, false);
        assertFalse(kayttaaValintalaskentaa(valintaperusteetDTOs));
    }

    @Test
    public void onValinnanvaihtaViimeisessaVaiheessaJonossaLaskenta() {
        List<ValintaperusteetDTO> valintaperusteetDTOs = createValintaperusteetDTOs(false, false, true, true);
        assertTrue(kayttaaValintalaskentaa(valintaperusteetDTOs));
    }

    private List<ValintaperusteetDTO> createValintaperusteetDTOs(boolean... kaytetaanValintalaskentaa) {
        ValintaperusteetDTO peruste1 = new ValintaperusteetDTO();
        ValintaperusteetDTO peruste2 = new ValintaperusteetDTO();

        peruste1.setViimeinenValinnanvaihe(1);
        peruste2.setViimeinenValinnanvaihe(1);

        ValintaperusteetValinnanVaiheDTO vaihe1 = new ValintaperusteetValinnanVaiheDTO();
        ValintaperusteetValinnanVaiheDTO vaihe2 = new ValintaperusteetValinnanVaiheDTO();

        vaihe1.setValinnanVaiheJarjestysluku(0);
        vaihe2.setValinnanVaiheJarjestysluku(1);

        ValintatapajonoJarjestyskriteereillaDTO vaihe1jono1 = new ValintatapajonoJarjestyskriteereillaDTO();
        ValintatapajonoJarjestyskriteereillaDTO vaihe1jono2 = new ValintatapajonoJarjestyskriteereillaDTO();
        ValintatapajonoJarjestyskriteereillaDTO vaihe2jono1 = new ValintatapajonoJarjestyskriteereillaDTO();
        ValintatapajonoJarjestyskriteereillaDTO vaihe2jono2 = new ValintatapajonoJarjestyskriteereillaDTO();

        vaihe1jono1.setKaytetaanValintalaskentaa(kaytetaanValintalaskentaa[0]);
        vaihe1jono2.setKaytetaanValintalaskentaa(kaytetaanValintalaskentaa[1]);
        vaihe2jono1.setKaytetaanValintalaskentaa(kaytetaanValintalaskentaa[2]);
        vaihe2jono2.setKaytetaanValintalaskentaa(kaytetaanValintalaskentaa[3]);

        peruste1.setValinnanVaihe(vaihe1);
        peruste2.setValinnanVaihe(vaihe2);

        vaihe1.setValintatapajono(Arrays.asList(vaihe1jono1, vaihe1jono2));
        vaihe2.setValintatapajono(Arrays.asList(vaihe2jono1, vaihe2jono2));

        return Arrays.asList(peruste1, peruste2);
    }

    private boolean kayttaaValintalaskentaa(List<ValintaperusteetDTO> valintapetusteetDTOs) {
        MockValintaperusteetAsyncResource.setHakukohteenValintaperusteetResult(valintapetusteetDTOs);
        Response r = hakukohteenValintaperusteetResource.getWebClient().get();
        assertEquals(200, r.getStatus());
        return r.readEntity(ValintaperusteetResourceResult.class).kayttaaValintalaskentaa;
    }
}
