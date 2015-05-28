package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class ValintalaskennanTulosExcelTest {
    HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
    {
        hakukohdeDTO.setHakukohdeNimi(map("fi", "Hakukohde 1"));
        hakukohdeDTO.setTarjoajaNimi(map("fi", "Tarjoaja 1"));


    }

    private HashMap<String, String> map(final String key, final String value) {
        return new HashMap<String, String>() {{
            put(key, value);
        }};
    }

    XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(hakukohdeDTO, Arrays.asList(
        valinnanvaihe(1, 2),
        valinnanvaihe(2, 1)
    ));

    @Test
    public void sheetNames() {
        assertEquals(3, workbook.getNumberOfSheets());
        assertEquals("Vaihe 1 - Jono 1", workbook.getSheetName(0));
        assertEquals("Vaihe 1 - Jono 2", workbook.getSheetName(1));
        assertEquals("Vaihe 2 - Jono 1", workbook.getSheetName(2));
    }

    @Test
    public void headerDataOnEachSheet() {
        assertEquals("Tarjoaja 1", getStringCellValue(0, 0, 0));
        assertEquals("Hakukohde 1", getStringCellValue(0, 1, 0));
        assertEquals("Vaihe 1", getStringCellValue(0, 2, 0));
        assertEquals("Jono 1", getStringCellValue(0, 3, 0));
    }

    private String getStringCellValue(final int sheet, final int row, final int col) {
        return workbook.getSheetAt(sheet).getRow(row).getCell(col).getStringCellValue();
    }

    private ValintatietoValinnanvaiheDTO valinnanvaihe(int jarjestysnumero, int jonoja) {
        return new ValintatietoValinnanvaiheDTO(
                jarjestysnumero,
                "vaiheOid" + jarjestysnumero,
                "hakuOid",
                "Vaihe " + jarjestysnumero,
                new Date(),
                valintatapajonot(jonoja),
                Collections.EMPTY_LIST
        );
    }

    private List<ValintatietoValintatapajonoDTO> valintatapajonot(int jonoja) {
        return IntStream.rangeClosed(1, jonoja).boxed().map(this::valintatapajono).collect(Collectors.toList());
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
                Collections.EMPTY_LIST, // TODO populoi
                true,
                Collections.EMPTY_LIST, // TODO populoi
                2,
                10,
                new DateTime().plusDays(1).toDate(),
                new DateTime().plusDays(2).toDate(),
                "tayttojono", 100L
        );
    }
}
