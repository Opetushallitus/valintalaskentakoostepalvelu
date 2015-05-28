package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.excel.ValintalaskennanTulosExcel;
import fi.vm.sade.valintalaskenta.domain.dto.HakijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteerituloksenTilaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.Tasasijasaanto;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import jdk.nashorn.internal.ir.annotations.Ignore;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.util.StreamUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class ValintalaskennanTulosExcelTest {
    HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
    {
        hakukohdeDTO.setHakukohdeNimi(map("fi", "Hakukohde 1"));
        hakukohdeDTO.setTarjoajaNimi(map("fi", "Tarjoaja 1"));
    }

    XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(hakukohdeDTO, asList(valinnanvaihe(1, 2), valinnanvaihe(2, 1)));

    @Test
    public void sheetNames() {
        assertEquals(3, workbook.getNumberOfSheets());
        assertEquals("Vaihe 1 - Jono 1", workbook.getSheetName(0));
        assertEquals("Vaihe 1 - Jono 2", workbook.getSheetName(1));
        assertEquals("Vaihe 2 - Jono 1", workbook.getSheetName(2));
    }

    @Test
    public void sheetContents() {
        assertEquals(
            asList(
                asList("Tarjoaja", "Tarjoaja 1"),
                asList("Hakukohde", "Hakukohde 1"),
                asList("Vaihe", "Vaihe 1"),
                asList("Päivämäärä", "01.01.1970 02.00"),
                asList("Jono", "Jono 1"),
                asList(),
                asList("Jonosija", "Sukunimi", "Etunimi", "Hakemus OID", "Laskennan tulos", "Kokonaispisteet"),
                asList("1",        "Suku 1",   "Etu 1",   "Hakemus 1",   "HYVAKSYTTAVISSA", "10")
            ), getWorksheetData(workbook.getSheetAt(0)));
    }

    @Test
    public void generoiTiedosto() throws IOException {
        StreamUtils.copy(Excel.export(workbook), new FileOutputStream("valintatulokset.xlsx"));
    }


    private <T> HashMap<String, T> map(final String key, final T value) {
        return new HashMap<String, T>() {{
            put(key, value);
        }};
    }

    private Map<String, List<List<String>>> getWorkbookData(XSSFWorkbook workbook) {
        Map<String, List<List<String>>> data = new HashMap<>();
        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            data.put(workbook.getSheetName(sheetNum), getWorksheetData(workbook.getSheetAt(sheetNum)));
        }
        return data;
    }

    private List<List<String>> getWorksheetData(final XSSFSheet sheet) {
        List<List<String>> sheetData = new ArrayList<>();
        for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
            sheetData.add(getRowData(sheet.getRow(rowNum)));
        }
        return sheetData;
    }

    private List<String> getRowData(final XSSFRow row) {
        final ArrayList<String> rowData = new ArrayList<>();
        if (row != null) {
            for (int col = 0; col < row.getLastCellNum(); col++) {
                rowData.add(row.getCell(col).getStringCellValue());
            }
        }
        return rowData;
    }

    private ValintatietoValinnanvaiheDTO valinnanvaihe(int jarjestysnumero, int jonoja) {
        return new ValintatietoValinnanvaiheDTO(
                jarjestysnumero,
                "vaiheOid" + jarjestysnumero,
                "hakuOid",
                "Vaihe " + jarjestysnumero,
                new Date(0),
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
                hakijat(), // TODO populoi
                2,
                10,
                new DateTime().plusDays(1).toDate(),
                new DateTime().plusDays(2).toDate(),
                "tayttojono", 100L
        );
    }

    private List<HakijaDTO> hakijat() {
        int i = 1;
        return Arrays.asList(
          new HakijaDTO(
              1,
              "Hakija " + i,
              "Hakemus " + i,
              JarjestyskriteerituloksenTilaDTO.HYVAKSYTTAVISSA,
              0,
              "Etu " + i,
              "Suku " + i,
              1,
              new BigDecimal(10),
              Collections.EMPTY_LIST,
              Collections.EMPTY_LIST,
              false,
              false
          )
        );
    }
}
