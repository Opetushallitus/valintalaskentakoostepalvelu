package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static javafx.scene.input.KeyCode.M;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import com.sun.xml.internal.ws.util.CompletedFuture;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl.ViestintapalveluAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuProsessiImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import rx.Observable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class HyvaksymiskirjeetServiceImplTest {
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource = Mockito.mock(ViestintapalveluAsyncResource.class);
    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti = Mockito.mock(HyvaksymiskirjeetKomponentti.class);
    private final SijoitteluAsyncResource sijoitteluAsyncResource = Mockito.mock(SijoitteluAsyncResource.class);
    private final ApplicationAsyncResource applicationAsyncResource = Mockito.mock(ApplicationAsyncResource.class);
    private final OrganisaatioAsyncResource organisaatioAsyncResource = Mockito.mock(OrganisaatioAsyncResource.class);
    private final HaeOsoiteKomponentti haeOsoiteKomponentti = Mockito.mock(HaeOsoiteKomponentti.class);
    private final KoekutsuProsessiImpl koekutsuProsessi = Mockito.mock(KoekutsuProsessiImpl.class);

    private final HakuParametritService hakuParametritService = Mockito.mock(HakuParametritService.class);
    private final HyvaksymiskirjeetService service = new HyvaksymiskirjeetServiceImpl(viestintapalveluAsyncResource, hyvaksymiskirjeetKomponentti,
        sijoitteluAsyncResource, applicationAsyncResource, organisaatioAsyncResource, haeOsoiteKomponentti, hakuParametritService);

    @Test
    public void hyvaksymiskirjeetHakukohteelleSaadaanLahetettyaJosHakukohdeLoytyyHyvaksymiskirjeetKomponentista() throws Exception {
        Hakemus hakemus = new Hakemus();
        Mockito.when(applicationAsyncResource.getApplicationsByOidsWithPOST("hakuOid", Collections.singletonList("hakukohdeOid"))).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        Mockito.when(sijoitteluAsyncResource.getKoulutuspaikkalliset("hakuOid", "hakukohdeOid")).thenReturn(Observable.just(new HakijaPaginationObject()));
        Mockito.when(organisaatioAsyncResource.haeHakutoimisto("tarjoajaOid")).thenReturn(Observable.just(Optional.of(new HakutoimistoDTO(ImmutableMap.of("fi", "hakutoimisto"), Collections.emptyMap()))));

        Mockito.when(hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(Collections.emptyList())).thenReturn(Collections.emptyMap());

        Mockito.when(hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(Collections.emptyList())).
            thenReturn(ImmutableMap.of("hakukohdeOid", new MetaHakukohde("tarjoajaOid", new Teksti("hakukohdeNimi"), new Teksti("tarjoajaNimi"))));

        LetterResponse letterResponse = new LetterResponse();
        letterResponse.setBatchId("batchId");
        letterResponse.setStatus(LetterResponse.STATUS_SUCCESS);
        Mockito.when(viestintapalveluAsyncResource.viePdfJaOdotaReferenssi(Matchers.any(LetterBatch.class))).thenReturn(new CompletedFuture<>(letterResponse, null));
        LetterBatchStatusDto letterBatchStatusDto = new LetterBatchStatusDto();
        letterBatchStatusDto.setStatus("ready");
        Mockito.when(viestintapalveluAsyncResource.haeStatus("batchId")).thenReturn(new CompletedFuture<>(letterBatchStatusDto, null));

        HyvaksymiskirjeDTO hyvaksymiskirjeDTO = new HyvaksymiskirjeDTO("tarjoajaOid", "sisalto", "templateName", "tag", "hakukohdeOid", "hakuOid", -1L, "palautusPvm", "palautusAika");

        Mockito.when(koekutsuProsessi.isKeskeytetty()).thenReturn(false);
        service.hyvaksymiskirjeetHakukohteelle(koekutsuProsessi, hyvaksymiskirjeDTO);


        Thread.sleep(1001); // HyvaksymiskirjeetServiceImpl.letterBatchToViestintapalvelu consumes observable by polling with 1 second intervals
        Mockito.verify(koekutsuProsessi, Mockito.times(1)).vaiheValmistui();
        Mockito.verify(koekutsuProsessi, Mockito.never()).keskeyta();
    }

    @Test
    public void hyvaksymiskirjeetHakukohteelleSaadaanLahetettyaVaikkaHakukohdeEiLoydyHyvaksymiskirjeetKomponentista() throws Exception {
        Hakemus hakemus = new Hakemus();
        Mockito.when(applicationAsyncResource.getApplicationsByOidsWithPOST("hakuOid", Collections.singletonList("hakukohdeOid"))).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        Mockito.when(sijoitteluAsyncResource.getKoulutuspaikkalliset("hakuOid", "hakukohdeOid")).thenReturn(Observable.just(new HakijaPaginationObject()));
        Mockito.when(organisaatioAsyncResource.haeHakutoimisto("tarjoajaOid")).thenReturn(Observable.just(Optional.of(new HakutoimistoDTO(ImmutableMap.of("fi", "hakutoimisto"), Collections.emptyMap()))));

        Mockito.when(hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(Collections.emptyList())).
            thenReturn(Collections.emptyMap());

        LetterResponse letterResponse = new LetterResponse();
        letterResponse.setBatchId("batchId");
        letterResponse.setStatus(LetterResponse.STATUS_SUCCESS);
        Mockito.when(viestintapalveluAsyncResource.viePdfJaOdotaReferenssi(Matchers.any(LetterBatch.class))).thenReturn(new CompletedFuture<>(letterResponse, null));
        LetterBatchStatusDto letterBatchStatusDto = new LetterBatchStatusDto();
        letterBatchStatusDto.setStatus("ready");
        Mockito.when(viestintapalveluAsyncResource.haeStatus("batchId")).thenReturn(new CompletedFuture<>(letterBatchStatusDto, null));

        HyvaksymiskirjeDTO hyvaksymiskirjeDTO = new HyvaksymiskirjeDTO("tarjoajaOid", "sisalto", "templateName", "tag", "hakukohdeOid", "hakuOid", -1L, "palautusPvm", "palautusAika");
        Mockito.when(koekutsuProsessi.isKeskeytetty()).thenReturn(false);

        service.hyvaksymiskirjeetHakukohteelle(koekutsuProsessi, hyvaksymiskirjeDTO);

        Thread.sleep(1001); // HyvaksymiskirjeetServiceImpl.letterBatchToViestintapalvelu consumes observable by polling with 1 second intervals
        Mockito.verify(koekutsuProsessi, Mockito.times(1)).vaiheValmistui();
        Mockito.verify(koekutsuProsessi, Mockito.never()).keskeyta();
    }
}
