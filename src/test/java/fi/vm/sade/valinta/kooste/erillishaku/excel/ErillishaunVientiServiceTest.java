package fi.vm.sade.valinta.kooste.erillishaku.excel;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ImportedErillisHakuExcel;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockDokumenttiResource;
import fi.vm.sade.valinta.kooste.mocks.MockSijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTarjontaAsyncService;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunVientiServiceImpl;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ErillishaunVientiServiceTest {


    final MockApplicationAsyncResource mockApplicationAsyncResource = new MockApplicationAsyncResource();
    final MockSijoitteluAsyncResource mockSijoitteluAsyncResource = new MockSijoitteluAsyncResource();
    final MockTarjontaAsyncService mockTarjontaAsyncService = new MockTarjontaAsyncService();
    final MockDokumenttiResource mockDokumenttiResource = new MockDokumenttiResource();

    @Test
    public void suoritaVientiTest() {
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "1", "1", "1", "varsinainen jono");
        final KirjeProsessi prosessi = mock(KirjeProsessi.class);
        final ErillishaunVientiServiceImpl erillishaunVientiService =
                new ErillishaunVientiServiceImpl(mockApplicationAsyncResource, mockSijoitteluAsyncResource, mockTarjontaAsyncService, mockDokumenttiResource);

        erillishaunVientiService.vie(prosessi, erillishaku);
        verify(prosessi, timeout(10000).times(1)).valmistui(anyString());
    }

    @Test
    public void suoritaEpaonnistunutVientiTest() {
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "1", "1", "1", "varsinainen jono");
        final KirjeProsessi prosessi = mock(KirjeProsessi.class);
        final ApplicationAsyncResource failingResource = mock(ApplicationAsyncResource.class);
        final ErillishaunVientiServiceImpl erillishaunVientiService =
                new ErillishaunVientiServiceImpl(failingResource, mockSijoitteluAsyncResource, mockTarjontaAsyncService, mockDokumenttiResource);

        erillishaunVientiService.vie(prosessi, erillishaku);
        verify(prosessi, timeout(10000).times(1)).keskeyta();
    }

    @Test
    public void suoriteVientiIlmanhakemuksia() throws IOException {
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "2", "1", "1", "varsinainen jono");
        final ErillishakuProsessiDTO prosessi = spy(new ErillishakuProsessiDTO(1));

        ApplicationAsyncResource applicationMock = mock(ApplicationAsyncResource.class);
        when(applicationMock.getApplicationsByOid(anyString(), anyString())).thenReturn(Futures.immediateFuture(ImmutableList.of()));

        SijoitteluAsyncResource sijoitteluMock = mock(SijoitteluAsyncResource.class);
        when(sijoitteluMock.getValintatuloksetHakukohteelle(anyString(), anyString())).thenReturn(Futures.immediateFuture(ImmutableList.of()));
        when(sijoitteluMock.getLatestHakukohdeBySijoittelu(anyString(), anyString())).thenReturn(mockSijoitteluAsyncResource.getLatestHakukohdeBySijoittelu("1", "2"));

        final ErillishaunVientiServiceImpl erillishaunVientiService =
                new ErillishaunVientiServiceImpl(applicationMock, sijoitteluMock, mockTarjontaAsyncService, mockDokumenttiResource);
        erillishaunVientiService.vie(prosessi, erillishaku);

        verify(prosessi, timeout(10000).times(1)).valmistui(anyString());

        ImportedErillisHakuExcel excel = new ImportedErillisHakuExcel(Hakutyyppi.KORKEAKOULU, MockDokumenttiResource.getStoredDocument(prosessi.getDokumenttiId()));
        assertEquals(1, excel.hetuToRivi.size());
        ErillishakuRivi erillishakuRivi = excel.hetuToRivi.get("123456-7890");
        assertEquals("Rivi", erillishakuRivi.getEtunimi());
        assertEquals("Esimerkki", erillishakuRivi.getSukunimi());
        assertEquals(false, erillishakuRivi.isJulkaistaankoTiedot());
    }
}