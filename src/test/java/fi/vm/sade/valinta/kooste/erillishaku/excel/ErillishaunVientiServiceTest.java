package fi.vm.sade.valinta.kooste.erillishaku.excel;

import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.erillishaku.excel.mocks.MockDokumenttiResource;
import fi.vm.sade.valinta.kooste.erillishaku.excel.mocks.MockSijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.erillishaku.excel.mocks.MockTarjontaAsyncService;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunVientiServiceImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import org.junit.Test;
import org.mockito.Mockito;

public class ErillishaunVientiServiceTest {

    @Test
    public void suoritaVientiTest() {

        final MockApplicationAsyncResource mockApplicationAsyncResource = new MockApplicationAsyncResource();
        final MockSijoitteluAsyncResource mockSijoitteluAsyncResource = new MockSijoitteluAsyncResource();
        final MockTarjontaAsyncService mockTarjontaAsyncService = new MockTarjontaAsyncService();
        final MockDokumenttiResource mockDokumenttiResource = new MockDokumenttiResource();

        final ErillishakuDTO erillishaku = new ErillishakuDTO(Hakutyyppi.KORKEAKOULU, "1", "1", "1", "1", "varsinainen jono");
        final KirjeProsessi prosessi = Mockito.mock(KirjeProsessi.class);

        final ErillishaunVientiServiceImpl erillishaunVientiService = new ErillishaunVientiServiceImpl(mockApplicationAsyncResource, mockSijoitteluAsyncResource, mockTarjontaAsyncService, mockDokumenttiResource);
        erillishaunVientiService.vie(prosessi, erillishaku);

        Mockito.verify(prosessi, Mockito.timeout(1000).times(1)).valmistui(Mockito.anyString());
    }

}