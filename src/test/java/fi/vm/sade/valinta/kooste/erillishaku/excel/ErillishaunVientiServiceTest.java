package fi.vm.sade.valinta.kooste.erillishaku.excel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunVientiService;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ImportedErillisHakuExcel;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockAtaruAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockDokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockKoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTarjontaAsyncService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import io.reactivex.Observable;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

public class ErillishaunVientiServiceTest {
    final MockApplicationAsyncResource mockApplicationAsyncResource = new MockApplicationAsyncResource();
    final MockAtaruAsyncResource mockAtaruAsyncResource = new MockAtaruAsyncResource();
    final MockTarjontaAsyncService mockTarjontaAsyncService = new MockTarjontaAsyncService();
    final MockDokumenttiAsyncResource mockDokumenttiAsyncResource = new MockDokumenttiAsyncResource();
    final ValintaTulosServiceAsyncResource mockTilaAsyncResource = Mockito.mock(ValintaTulosServiceAsyncResource.class);
    final MockKoodistoCachedAsyncResource mockKoodistoCachedAsyncResource = new MockKoodistoCachedAsyncResource(mock(KoodistoAsyncResource.class));

    @Test
    public void suoritaVientiTest() {
        AuditSession auditSession = new AuditSession();
        when(mockTilaAsyncResource.getErillishaunValinnantulokset(anyObject(), anyString())).thenReturn(Observable.just(Lists.newArrayList()));
        when(mockTilaAsyncResource.fetchLukuvuosimaksut(anyString(),any())).thenReturn(Observable.just(Lists.newArrayList()));
        when(mockTilaAsyncResource.getErillishaunValinnantulokset(any(), anyString())).thenReturn(Observable.just(Lists.newArrayList()));
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "1", "1", "1");
        final KirjeProsessi prosessi = mock(KirjeProsessi.class);
        final ErillishaunVientiService erillishaunVientiService =
                new ErillishaunVientiService(mockTilaAsyncResource, mockApplicationAsyncResource, mockAtaruAsyncResource, mockTarjontaAsyncService, mockDokumenttiAsyncResource, mockKoodistoCachedAsyncResource);

        erillishaunVientiService.vie(auditSession, prosessi, erillishaku);
        verify(prosessi, timeout(10000).times(1)).valmistui(anyString());
    }

    @Test
    public void suoritaEpaonnistunutVientiTest() {
        when(mockTilaAsyncResource.getErillishaunValinnantulokset(anyObject(), anyString())).thenReturn(Observable.just(Lists.newArrayList()));
        when(mockTilaAsyncResource.fetchLukuvuosimaksut(anyString(),any())).thenReturn(Observable.just(Lists.newArrayList()));
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "1", "1", "1");
        final KirjeProsessi prosessi = mock(KirjeProsessi.class);
        final ApplicationAsyncResource failingResource = mock(ApplicationAsyncResource.class);
        final ErillishaunVientiService erillishaunVientiService =
                new ErillishaunVientiService(mockTilaAsyncResource, failingResource, mockAtaruAsyncResource, mockTarjontaAsyncService, mockDokumenttiAsyncResource, mockKoodistoCachedAsyncResource);
        when(failingResource.getApplicationsByOid(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("Vienti epäonnistuu tarkoituksella")));

        erillishaunVientiService.vie(new AuditSession(), prosessi, erillishaku);
        verify(prosessi, timeout(10000).times(1)).keskeyta();
    }

    @Test
    public void suoriteVientiIlmanhakemuksia() {
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "2", "1", "1");
        final ErillishakuProsessiDTO prosessi = spy(new ErillishakuProsessiDTO(1));

        ApplicationAsyncResource applicationMock = mock(ApplicationAsyncResource.class);
        when(applicationMock.getApplicationsByOid(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(Lists.newArrayList()));

        when(mockTilaAsyncResource.getErillishaunValinnantulokset(anyObject(), anyString())).thenReturn(Observable.just(Lists.newArrayList()));
        when(mockTilaAsyncResource.fetchLukuvuosimaksut(anyString(),any())).thenReturn(Observable.just(Lists.newArrayList()));

        final ErillishaunVientiService erillishaunVientiService =
                new ErillishaunVientiService(mockTilaAsyncResource, applicationMock, mockAtaruAsyncResource, mockTarjontaAsyncService, mockDokumenttiAsyncResource, mockKoodistoCachedAsyncResource);
        erillishaunVientiService.vie(new AuditSession(), prosessi, erillishaku);

        verify(prosessi, timeout(5000).times(1)).valmistui(anyString());

        ImportedErillisHakuExcel excel = new ImportedErillisHakuExcel(Hakutyyppi.KORKEAKOULU, MockDokumenttiAsyncResource.getStoredDocument(prosessi.getDokumenttiId()), mockKoodistoCachedAsyncResource);
        assertEquals(1, excel.rivit.size());
        ErillishakuRivi erillishakuRivi = excel.rivit.get(0);
        assertEquals("123456-7890", erillishakuRivi.getHenkilotunnus());
        assertEquals("Rivi", erillishakuRivi.getEtunimi());
        assertEquals("Esimerkki", erillishakuRivi.getSukunimi());
        assertEquals(false, erillishakuRivi.isJulkaistaankoTiedot());
        assertEquals(Maksuvelvollisuus.NOT_CHECKED, erillishakuRivi.getMaksuvelvollisuus());
    }
}
