package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.excel.ValintalaskennanTulosExcel;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteeritulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.Tasasijasaanto;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.StreamUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import static org.junit.Assert.assertEquals;

public class ValintalaskennanTulosExcelTest {
    HakukohdeV1RDTO hakukohdeDTO = new HakukohdeV1RDTO();
    {
        hakukohdeDTO.setHakukohteenNimet(map("fi", "Hakukohde 1"));
        hakukohdeDTO.setTarjoajaNimet(map("fi", "Tarjoaja 1"));
    }

    private HakuV1RDTO haku = new HakuV1RDTO();
    {
        haku.setNimi(map("fi", "Haku 1"));
    }
    XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(haku, hakukohdeDTO, asList(
        valinnanvaihe(1, asList(
            valintatapajono(1, jonosijat()),
            valintatapajono(2, EMPTY_LIST)
        )),
        valinnanvaihe(2, asList(
            valintatapajono(1, EMPTY_LIST))
        )), hakemukset());

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
                asList("Haku", "Haku 1"),
                asList("Tarjoaja", "Tarjoaja 1"),
                asList("Hakukohde", "Hakukohde 1"),
                asList("Vaihe", "Vaihe 1"),
                asList("Päivämäärä", "01.01.1970 02.00"),
                asList("Jono", "Jono 1"),
                asList(),
                asList("Jonosija", "Sukunimi",  "Etunimi", "Henkilötunnus",  "Hakemus OID",  "Hakutoive",    "Laskennan tulos",  "Selite",   "Kokonaispisteet"),
                asList("1",        "Suku 2",    "Etu 2",   "",               "Hakemus 2",    "2",            "VIRHE",            "Puuttuu",  ""),
                asList("2",        "Suku 1",    "Etu 1",   "010101-123N",    "Hakemus 1",    "1",            "HYVAKSYTTAVISSA",  "",         "666")
            ), getWorksheetData(workbook.getSheetAt(0)));
    }

    @Test
    public void emptySheet() {
        assertEquals(
            asList(
                asList("Haku", "Haku 1"),
                asList("Tarjoaja", "Tarjoaja 1"),
                asList("Hakukohde", "Hakukohde 1"),
                asList("Vaihe", "Vaihe 1"),
                asList("Päivämäärä", "01.01.1970 02.00"),
                asList("Jono", "Jono 2"),
                asList(),
                asList("Jonolle ei ole valintalaskennan tuloksia")
            ), getWorksheetData(workbook.getSheetAt(1))
        );
    }

    @Test
    @Ignore
    public void generoiTiedosto() throws IOException {
        StreamUtils.copy(Excel.export(workbook), new FileOutputStream("valintatulokset.xlsx"));
    }


    private <T> HashMap<String, T> map(final String key, final T value) {
        return new HashMap<String, T>() {{
            put(key, value);
        }};
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

    private ValintatietoValinnanvaiheDTO valinnanvaihe(int jarjestysnumero, List<ValintatietoValintatapajonoDTO> jonot) {
        return new ValintatietoValinnanvaiheDTO(
                jarjestysnumero,
                "vaiheOid" + jarjestysnumero,
                "hakuOid",
                "Vaihe " + jarjestysnumero,
                new Date(0),
                jonot,
                EMPTY_LIST
        );
    }

    private ValintatietoValintatapajonoDTO valintatapajono(int jonoNumero, final List<JonosijaDTO> jonosijat) {
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
                jonosijat,
                true,
                EMPTY_LIST,
                2,
                10,
                new DateTime().plusDays(1).toDate(),
                new DateTime().plusDays(2).toDate(),
                "tayttojono", 100L
        );
    }

    private List<JonosijaDTO> jonosijat() {
        return Arrays.asList(
            new JonosijaDTO(2, "Hakemus 1", "Hakija 1",
                jarjestyskriteerit(HYVAKSYTTAVISSA, EMPTY_MAP, new BigDecimal(666)),
                1, "Suku 1", "Etu 1", false, HYVAKSYTTAVISSA, EMPTY_LIST, EMPTY_LIST, EMPTY_LIST, false, false),
            new JonosijaDTO(1, "Hakemus 2", "Hakija 2",
                jarjestyskriteerit(JarjestyskriteerituloksenTila.VIRHE, map("fi", "Puuttuu"), null),
                2, "Suku 2", "Etu 2", false, JarjestyskriteerituloksenTila.VIRHE, EMPTY_LIST, EMPTY_LIST, EMPTY_LIST, false, false)
        );
    }

    private List<Hakemus> hakemukset() {
        final Answers answersWithHetu = new Answers();
        answersWithHetu.getHenkilotiedot().put("Henkilotunnus", "010101-123N");
        return Arrays.asList(
            new Hakemus("", "", answersWithHetu, EMPTY_MAP, EMPTY_LIST, "Hakemus 1", "", "Hakija 1")
        );
    }


    private TreeSet<JarjestyskriteeritulosDTO> jarjestyskriteerit(final JarjestyskriteerituloksenTila tila, final Map<String, String> kuvaus, final BigDecimal arvo) {
        final TreeSet<JarjestyskriteeritulosDTO> kriteerit = new TreeSet<>();
        kriteerit.add(new JarjestyskriteeritulosDTO(arvo, tila, kuvaus, 1, "Yhteispisteet"));
        return kriteerit;
    }
}
