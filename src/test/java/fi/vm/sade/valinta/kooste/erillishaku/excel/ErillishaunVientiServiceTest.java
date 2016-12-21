package fi.vm.sade.valinta.kooste.erillishaku.excel;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ImportedErillisHakuExcel;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunVientiService;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import org.mockito.Mockito;
import rx.Observable;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ErillishaunVientiServiceTest {
    final MockApplicationAsyncResource mockApplicationAsyncResource = new MockApplicationAsyncResource();
    final MockSijoitteluAsyncResource mockSijoitteluAsyncResource = new MockSijoitteluAsyncResource();
    final MockTarjontaAsyncService mockTarjontaAsyncService = new MockTarjontaAsyncService();
    final MockDokumenttiResource mockDokumenttiResource = new MockDokumenttiResource();
    final ValintaTulosServiceAsyncResource mockTilaAsyncResource = Mockito.mock(ValintaTulosServiceAsyncResource.class);
    final MockKoodistoCachedAsyncResource mockKoodistoCachedAsyncResource = new MockKoodistoCachedAsyncResource(mock(KoodistoAsyncResource.class));

    @Test
    public void suoritaVientiTest() {
        when(mockTilaAsyncResource.findValintatulokset(anyString(), anyString())).thenReturn(Observable.just(Lists.newArrayList()));
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "1", "1", "1", "varsinainen jono");
        final KirjeProsessi prosessi = mock(KirjeProsessi.class);
        final ErillishaunVientiService erillishaunVientiService =
                new ErillishaunVientiService(mockTilaAsyncResource, mockApplicationAsyncResource, mockSijoitteluAsyncResource, mockTarjontaAsyncService, mockDokumenttiResource, mockKoodistoCachedAsyncResource);

        erillishaunVientiService.vie(prosessi, erillishaku);
        verify(prosessi, timeout(10000).times(1)).valmistui(anyString());
    }

    @Test
    public void suoritaEpaonnistunutVientiTest() {
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "1", "1", "1", "varsinainen jono");
        final KirjeProsessi prosessi = mock(KirjeProsessi.class);
        final ApplicationAsyncResource failingResource = mock(ApplicationAsyncResource.class);
        final ErillishaunVientiService erillishaunVientiService =
                new ErillishaunVientiService(mockTilaAsyncResource, failingResource, mockSijoitteluAsyncResource, mockTarjontaAsyncService, mockDokumenttiResource, mockKoodistoCachedAsyncResource);

        erillishaunVientiService.vie(prosessi, erillishaku);
        verify(prosessi, timeout(10000).times(1)).keskeyta();
    }

    @Test
    public void suoriteVientiIlmanhakemuksia() throws IOException {
        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "2", "1", "1", "varsinainen jono");
        final ErillishakuProsessiDTO prosessi = spy(new ErillishakuProsessiDTO(1));

        ApplicationAsyncResource applicationMock = mock(ApplicationAsyncResource.class);
        when(applicationMock.getApplicationsByOid(anyString(), anyString())).thenReturn(Observable.just(Lists.newArrayList()));

        SijoitteluAsyncResource sijoitteluMock = mock(SijoitteluAsyncResource.class);
        when(mockTilaAsyncResource.findValintatulokset(anyString(), anyString())).thenReturn(Observable.just(Lists.newArrayList()));
        when(sijoitteluMock.getLatestHakukohdeBySijoittelu(anyString(), anyString())).thenReturn(mockSijoitteluAsyncResource.getLatestHakukohdeBySijoittelu("sjfhaskdjhfa", "dskfasadkjhf"));

        final ErillishaunVientiService erillishaunVientiService =
                new ErillishaunVientiService(mockTilaAsyncResource, applicationMock, sijoitteluMock, mockTarjontaAsyncService, mockDokumenttiResource, mockKoodistoCachedAsyncResource);
        erillishaunVientiService.vie(prosessi, erillishaku);

        verify(prosessi, timeout(5000).times(1)).valmistui(anyString());

        ImportedErillisHakuExcel excel = new ImportedErillisHakuExcel(Hakutyyppi.KORKEAKOULU, MockDokumenttiResource.getStoredDocument(prosessi.getDokumenttiId()));
        assertEquals(1, excel.rivit.size());
        ErillishakuRivi erillishakuRivi = excel.rivit.get(0);
        assertEquals("123456-7890", erillishakuRivi.getHenkilotunnus());
        assertEquals("Rivi", erillishakuRivi.getEtunimi());
        assertEquals("Esimerkki", erillishakuRivi.getSukunimi());
        assertEquals(false, erillishakuRivi.isJulkaistaankoTiedot());
        assertEquals(Maksuvelvollisuus.NOT_CKECKED, erillishakuRivi.getLukuvuosiMaksuvelvollisuus());
    }
}
