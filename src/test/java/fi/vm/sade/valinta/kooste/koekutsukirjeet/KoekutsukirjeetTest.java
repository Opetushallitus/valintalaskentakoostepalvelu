package fi.vm.sade.valinta.kooste.koekutsukirjeet;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.hakukohdeJaValintakoe;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintakoe;
import com.google.gson.GsonBuilder;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Jussi Jartamo
 */
public class KoekutsukirjeetTest {
    final static Logger LOG = LoggerFactory.getLogger(KoekutsukirjeetTest.class);
    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResourceBuilder.WebClientExposingHttpResource koekutsukirjeResource = new HttpResourceBuilder()
            .address(root + "/viestintapalvelu/koekutsukirjeet/aktivoi")
            .buildExposingWebClientDangerously();

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void kaikkiKutsutaanHakijanValinta() {
        try {
            final String HAKUKOHDE1 = "HAKUKOHDE1";
            final String HAKUKOHDE2 = "HAKUKOHDE2";
            final String TUNNISTE1 = "TUNNISTE1";
            final String SELVITETTY_TUNNISTE1 = "SELVITETTY_TUNNISTE1";
            final String HAKEMUS1 = "HAKEMUS1";
            final String HAKEMUS2 = "HAKEMUS2";
            HakukohdeDTO HAKUKOHDEDTO1 = new HakukohdeDTO();
            HAKUKOHDEDTO1.setOpetuskielet(Arrays.asList("FI", "SV"));
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(Observable.just(Collections.emptyList()));
            ViestintapalveluAsyncResource viestintapalveluAsyncResource =
                    Mocks.getViestintapalveluAsyncResource();
            ArgumentCaptor<LetterBatch> letterBatchArgumentCaptor = ArgumentCaptor.forClass(LetterBatch.class);
            Mockito.when(viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(Mockito.any(LetterBatch.class))).thenReturn(Observable.empty());
            Mockito.when(Mocks.getHakukohdeResource().getByOID(Mockito.anyString())).thenReturn(HAKUKOHDEDTO1);
            MockValintaperusteetAsyncResource.setHakukohdeResult(
                    Arrays.asList(
                            hakukohdeJaValintakoe()
                                    .setHakukohdeOid(HAKUKOHDE1)
                                    .addValintakoe(TUNNISTE1)
                                    .build()
                    )
            );
            MockValintaperusteetAsyncResource.setValintakokeetResult(
                    Arrays.asList(
                            valintakoe().setTunniste(TUNNISTE1).setSelvitettyTunniste(SELVITETTY_TUNNISTE1).build()));
            MockValintalaskentaValintakoeAsyncResource.setResult(
                    Arrays.asList(
                            osallistuminen()
                                    .setHakemusOid(HAKEMUS1)
                                    .hakutoive()
                                    .setHakukohdeOid(HAKUKOHDE1)
                                    .valinnanvaihe()
                                    .valintakoe()
                                    .setOsallistuu()
                                    .setValintakoeTunniste(SELVITETTY_TUNNISTE1)
                                    .build()
                                    .build()
                                    .build()
                                    .build(),
                            osallistuminen()
                                    .setHakemusOid(HAKEMUS2)
                                    .hakutoive()
                                    .setHakukohdeOid(HAKUKOHDE1)
                                    .valinnanvaihe()
                                    .valintakoe()
                                    .setOsallistuu()
                                    .setValintakoeTunniste(SELVITETTY_TUNNISTE1)
                                    .build()
                                    .build()
                                    .build()
                                    .build()));

            MockApplicationAsyncResource.setResult(
                    Arrays.asList(
                            hakemus()
                                    .setOid(HAKEMUS1)
                                    .addHakutoive(HAKUKOHDE1)
                                    .build()
                    )
            );
            MockApplicationAsyncResource.setResultByOid(
                    Arrays.asList(
                            hakemus()
                                    .setOid(HAKEMUS2)
                                    .addHakutoive(HAKUKOHDE2)
                                    .build()
                    )
            );

            Response r =
                    koekutsukirjeResource.getWebClient()
                            .query("hakuOid", "H0")
                            .query("hakukohdeOid", HAKUKOHDE1)
                            .query("tarjoajaOid", "T0")
                            .query("templateName", "tmpl")
                            .query("valintakoeTunnisteet", SELVITETTY_TUNNISTE1)
                            .post(Entity.json(new DokumentinLisatiedot(Collections.emptyList(), "tag", "Letterbodytext", "FI", Collections.emptyList())));
            Assert.assertEquals(200, r.getStatus());


            Mockito.verify(viestintapalveluAsyncResource, Mockito.timeout(1000).times(1)).viePdfJaOdotaReferenssiObservable(letterBatchArgumentCaptor.capture());
            LetterBatch batch = letterBatchArgumentCaptor.getValue();
            Assert.assertEquals("Odotetaan kahta kirjettä. Yksi hakukohteessa olevalle hakijalle ja toinen osallistumistiedoista saadulle hakijalle.", 2, batch.getLetters().size());
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(batch));
        } finally {
            MockApplicationAsyncResource.clear();
        }
    }

}
