package fi.vm.sade.valinta.kooste.valintalaskenta.excel;

import fi.vm.sade.valinta.seuranta.dto.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TestLaskentaDtoAsExcel {

  @Test
  public void testLaskentaDtoAsExcel() throws IOException {
    // uuid, hakuOid, luotu, tila, hakukohteet, valinnanvaihe,
    // valintakoelaskenta
    String uuid = null;
    String hakuOid = null;
    LaskentaTila tila = null;
    List<HakukohdeDto> hakukohteet = null;
    Integer valinnanvaihe = null;
    Boolean valintakoelaskenta = null;
    LaskentaDto l =
        new LaskentaDto(
            uuid,
            "",
            "",
            "",
            hakuOid,
            new Date().getTime(),
            tila,
            LaskentaTyyppi.HAKUKOHDE,
            new IlmoitusDto(IlmoitusTyyppi.VIRHE, "Joku virhe", Arrays.asList("A", "B", "C")),
            hakukohteet,
            false,
            valinnanvaihe,
            valintakoelaskenta,
            null,
            true);
    LaskentaDtoAsExcel.laskentaDtoAsExcel(l);
  }
}
