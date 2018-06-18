package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.MockAtaruAsyncResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.excel.ValintalaskennanTulosExcel;
import fi.vm.sade.valintalaskenta.domain.dto.FunktioTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteeritulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.Tasasijasaanto;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;
import org.apache.commons.io.FileUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ValintalaskennanTulosExcelTest {
    private HakukohdeV1RDTO hakukohdeDTO = new HakukohdeV1RDTO();

    {
        hakukohdeDTO.setHakukohteenNimet(map("fi", "Hakukohde 1"));
        hakukohdeDTO.setTarjoajaNimet(map("fi", "Tarjoaja 1"));
    }

    private HakuV1RDTO haku = new HakuV1RDTO();

    {
        haku.setNimi(map("fi", "Haku 1"));
    }

    private DateTime nyt = DateTime.now();

    @Test
    public void sheetNames() {
        XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(haku, hakukohdeDTO, asList(
                valinnanvaihe(1, nyt.toDate(), asList(
                        valintatapajono(1, jonosijat()),
                        valintatapajono(2, Collections.emptyList())
                )),
                valinnanvaihe(2, nyt.minusMonths(12).toDate(), Collections.singletonList(valintatapajono(1, Collections.emptyList()))
                )), hakemukset());

        assertEquals(3, workbook.getNumberOfSheets());
        assertEquals("Jono 1", workbook.getSheetName(0));
        assertEquals("Jono 2", workbook.getSheetName(1));
        assertEquals("Jono 1 (2)", workbook.getSheetName(2));
    }

    @Test
    public void sheetContents() {
        XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(haku, hakukohdeDTO, asList(
                valinnanvaihe(1, nyt.toDate(), asList(
                        valintatapajono(1, jonosijat()),
                        valintatapajono(2, Collections.emptyList())
                )),
                valinnanvaihe(2, nyt.minusMonths(12).toDate(), Collections.singletonList(valintatapajono(1, Collections.emptyList()))
                )), hakemukset());

        assertEquals(
                asList(
                        asList("Haku", "Haku 1"),
                        asList("Tarjoaja", "Tarjoaja 1"),
                        asList("Hakukohde", "Hakukohde 1"),
                        asList("Vaihe", "Vaihe 1"),
                        asList("Päivämäärä", ExcelExportUtil.DATE_FORMAT.format(nyt.toDate())), // "01.01.1970 02.00"
                        asList("Jono", "Jono 1"),
                        Collections.emptyList(),
                        asList("Jonosija", "Sukunimi", "Etunimi", "Henkilötunnus", "Sähköpostiosoite", "Hakemus OID", "Hakutoive", "Laskennan tulos", "Selite", "Kokonaispisteet", "pääsykoetulos", "keskiarvo"),
                        asList("2", "Suku 1", "Etu 1", "010101-123N", "sukuetu1@testi.fi", "Hakemus 1", "1", "HYVAKSYTTAVISSA", "", "666", "10", "9")
                ), getWorksheetData(workbook.getSheetAt(0)));
    }

    @Test
    public void ataruSheetContents() {
        XSSFWorkbook ataruWorkbook = ValintalaskennanTulosExcel.luoExcel(haku, hakukohdeDTO, asList(
                valinnanvaihe(1, nyt.toDate(), asList(
                        valintatapajono(1, ataruJonosijat()),
                        valintatapajono(2, Collections.emptyList())
                )),
                valinnanvaihe(2, nyt.minusMonths(12).toDate(), Collections.singletonList(valintatapajono(1, Collections.emptyList()))
                )), Collections.singletonList(MockAtaruAsyncResource.getAtaruHakemusWrapper("1.2.246.562.11.00000000000000000063")));

        assertEquals(
                asList(
                        asList("Haku", "Haku 1"),
                        asList("Tarjoaja", "Tarjoaja 1"),
                        asList("Hakukohde", "Hakukohde 1"),
                        asList("Vaihe", "Vaihe 1"),
                        asList("Päivämäärä", ExcelExportUtil.DATE_FORMAT.format(nyt.toDate())), // "01.01.1970 02.00"
                        asList("Jono", "Jono 1"),
                        Collections.emptyList(),
                        asList("Jonosija", "Sukunimi", "Etunimi", "Henkilötunnus", "Sähköpostiosoite", "Hakemus OID", "Hakutoive", "Laskennan tulos", "Selite", "Kokonaispisteet", "pääsykoetulos", "keskiarvo"),
                        asList("2", "TAUsuL4BQc", "Zl2A5", "020202A0202", "ukhBW@example.com", "1.2.246.562.11.00000000000000000063", "1", "HYVAKSYTTAVISSA", "", "666", "10", "9")
                ), getWorksheetData(ataruWorkbook.getSheetAt(0)));
    }

    @Test
    public void emptySheet() {
        XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(haku, hakukohdeDTO, asList(
                valinnanvaihe(1, nyt.toDate(), asList(
                        valintatapajono(1, jonosijat()),
                        valintatapajono(2, Collections.emptyList())
                )),
                valinnanvaihe(2, nyt.minusMonths(12).toDate(), Collections.singletonList(valintatapajono(1, Collections.emptyList()))
                )), hakemukset());

        assertEquals(
                asList(
                        asList("Haku", "Haku 1"),
                        asList("Tarjoaja", "Tarjoaja 1"),
                        asList("Hakukohde", "Hakukohde 1"),
                        asList("Vaihe", "Vaihe 1"),
                        asList("Päivämäärä", ExcelExportUtil.DATE_FORMAT.format(nyt.toDate())),
                        asList("Jono", "Jono 2"),
                        Collections.emptyList(),
                        Collections.singletonList("Jonolle ei ole valintalaskennan tuloksia")
                ), getWorksheetData(workbook.getSheetAt(1))
        );
    }

    @Test(expected = NullPointerException.class)
    public void sheetGenerationFailsNoApplication() {
        List<JonosijaDTO> jonosijatToisellaEiHakemusta = Arrays.asList(
                new JonosijaDTO(1, "Hakemus 2", "Hakija 2",
                        jarjestyskriteerit(JarjestyskriteerituloksenTila.VIRHE, map("fi", "Puuttuu"), null),
                        2, "Suku 2", "Etu 2", false, JarjestyskriteerituloksenTila.VIRHE, Collections.emptyList(), Collections.emptyList(), Arrays.asList(new FunktioTulosDTO("pääsykoetulos", null), new FunktioTulosDTO("keskiarvo", "8")), false, false),
                new JonosijaDTO(2, "Hakemus 1", "Hakija 1",
                        jarjestyskriteerit(HYVAKSYTTAVISSA, Collections.emptyMap(), new BigDecimal(666)),
                        1, "Suku 1", "Etu 1", false, HYVAKSYTTAVISSA, Collections.emptyList(), Collections.emptyList(), Arrays.asList(new FunktioTulosDTO("pääsykoetulos", "10"), new FunktioTulosDTO("keskiarvo", "9")), false, false)
        );
        ValintalaskennanTulosExcel.luoExcel(haku, hakukohdeDTO, asList(
                valinnanvaihe(1, nyt.toDate(), asList(
                        valintatapajono(1, jonosijatToisellaEiHakemusta),
                        valintatapajono(2, Collections.emptyList())
                )),
                valinnanvaihe(2, nyt.minusMonths(12).toDate(), Collections.singletonList(valintatapajono(1, Collections.emptyList()))
                )), hakemukset());
    }

    @Test
    public void generoiTiedosto() throws IOException {
        XSSFWorkbook workbook = ValintalaskennanTulosExcel.luoExcel(haku, hakukohdeDTO, asList(
                valinnanvaihe(1, nyt.toDate(), asList(
                        valintatapajono(1, jonosijat()),
                        valintatapajono(2, Collections.emptyList())
                )),
                valinnanvaihe(2, nyt.minusMonths(12).toDate(), Collections.singletonList(valintatapajono(1, Collections.emptyList()))
                )), hakemukset());

        File outputFile = new File("valintatulokset.xlsx");
        try {
            StreamUtils.copy(Excel.export(workbook), new FileOutputStream(outputFile.getName()));
            assertThat(outputFile.length(), is(greaterThan(0L)));
        } finally {
            FileUtils.forceDelete(outputFile);
        }
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

    private ValintatietoValinnanvaiheDTO valinnanvaihe(int jarjestysnumero, Date d, List<ValintatietoValintatapajonoDTO> jonot) {
        return new ValintatietoValinnanvaiheDTO(
                jarjestysnumero,
                "vaiheOid" + jarjestysnumero,
                "hakuOid",
                "Vaihe " + jarjestysnumero,
                d,
                jonot,
                Collections.emptyList()
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
                Collections.emptyList(),
                2,
                10,
                new DateTime().plusDays(1).toDate(),
                new DateTime().plusDays(2).toDate(),
                "tayttojono", 100L
        );
    }

    private List<JonosijaDTO> jonosijat() {
        return Collections.singletonList(
                new JonosijaDTO(2, "Hakemus 1", "Hakija 1",
                        jarjestyskriteerit(HYVAKSYTTAVISSA, Collections.emptyMap(), new BigDecimal(666)),
                        1, "Suku 1", "Etu 1", false, HYVAKSYTTAVISSA, Collections.emptyList(), Collections.emptyList(), Arrays.asList(new FunktioTulosDTO("pääsykoetulos", "10"), new FunktioTulosDTO("keskiarvo", "9")), false, false)
        );
    }

    private List<JonosijaDTO> ataruJonosijat() {
        return Collections.singletonList(
                new JonosijaDTO(2, "1.2.246.562.11.00000000000000000063", "1.2.246.562.24.86368188549",
                        jarjestyskriteerit(HYVAKSYTTAVISSA, Collections.emptyMap(), new BigDecimal(666)),
                        1, "Suku 1", "Etu 1", false, HYVAKSYTTAVISSA, Collections.emptyList(), Collections.emptyList(), Arrays.asList(new FunktioTulosDTO("pääsykoetulos", "10"), new FunktioTulosDTO("keskiarvo", "9")), false, false)
        );
    }

    private List<HakemusWrapper> hakemukset() {
        final Answers answersWithHetu = new Answers();
        answersWithHetu.getHenkilotiedot().put("Henkilotunnus", "010101-123N");
        answersWithHetu.getHenkilotiedot().put("Sähköposti", "sukuetu1@testi.fi");
        answersWithHetu.getHenkilotiedot().put("Etunimet", "Etu 1");
        answersWithHetu.getHenkilotiedot().put("Sukunimi", "Suku 1");

        Hakemus hakemus = new Hakemus("", "", answersWithHetu, Collections.emptyMap(), Collections.emptyList(), "Hakemus 1", "", "Hakija 1");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);
        return Collections.singletonList(wrapper);
    }


    private TreeSet<JarjestyskriteeritulosDTO> jarjestyskriteerit(final JarjestyskriteerituloksenTila tila, final Map<String, String> kuvaus, final BigDecimal arvo) {
        final TreeSet<JarjestyskriteeritulosDTO> kriteerit = new TreeSet<>();
        kriteerit.add(new JarjestyskriteeritulosDTO(arvo, tila, kuvaus, 1, "Yhteispisteet"));
        return kriteerit;
    }
}
