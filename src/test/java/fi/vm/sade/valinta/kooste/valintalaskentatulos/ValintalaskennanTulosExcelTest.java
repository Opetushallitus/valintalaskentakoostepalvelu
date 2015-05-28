package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import fi.vm.sade.valinta.kooste.valintalaskentatulos.excel.ValintalaskennanTulosExcel;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.Tasasijasaanto;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class ValintalaskennanTulosExcelTest {

    @Test
    public void test() {
        XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(Arrays.asList(
                valinnanvaihe(1),
                valinnanvaihe(2)
        ));
        assertEquals(2, workbook.getNumberOfSheets());
        assertEquals("Vaihe 1 - Jono 1", workbook.getSheetName(0));
        assertEquals("Vaihe 2 - Jono 1", workbook.getSheetName(1));
    }

    private ValintatietoValinnanvaiheDTO valinnanvaihe(int jarjestysnumero) {
        return new ValintatietoValinnanvaiheDTO(
                jarjestysnumero,
                "vaiheOid" + jarjestysnumero,
                "hakuOid",
                "Vaihe " + jarjestysnumero,
                new Date(),
                Arrays.asList(valintatapajono(1)),
                Collections.EMPTY_LIST
        );
    }

    private ValintatietoValintatapajonoDTO valintatapajono(int jonoNumero) {
        return new ValintatietoValintatapajonoDTO(
                "jonoOid" + jonoNumero,
                "Jono " + jonoNumero,
                jonoNumero,
                10,
                true,
                Tasasijasaanto.YLITAYTTO,
                false,
                true,
                true,
                true,
                true,
                Collections.EMPTY_LIST,
                true,
                Collections.EMPTY_LIST,
                2,
                10,
                new DateTime().plusDays(1).toDate(),
                new DateTime().plusDays(2).toDate(),
                "tayttojono", 100L
        );
    }
}
