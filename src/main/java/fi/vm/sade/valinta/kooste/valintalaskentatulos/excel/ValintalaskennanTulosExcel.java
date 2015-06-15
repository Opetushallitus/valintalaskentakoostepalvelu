package fi.vm.sade.valinta.kooste.valintalaskentatulos.excel;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti.getTeksti;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.codepoetics.protonpack.StreamUtils;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteeritulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;

public class ValintalaskennanTulosExcel {
    public static XSSFWorkbook luoExcel(HakuV1RDTO haku, final HakukohdeV1RDTO hakukohdeDTO, List<ValintatietoValinnanvaiheDTO> valinnanVaiheet, final List<Hakemus> hakemukset) {
        final Map<String, Hakemus> hakemusByOid = hakemukset.stream().collect(Collectors.toMap(Hakemus::getOid, h -> h));

        XSSFWorkbook workbook = new XSSFWorkbook();
        valinnanVaiheet.stream()

                .flatMap(vaihe -> vaihe.getValintatapajonot().stream().map(jono -> new ValintatapaJonoSheet(jono, vaihe)))
                .collect(Collectors.groupingBy(jonoSheet -> jonoSheet.sheetName)).entrySet().stream()
                // Uudelleen nimetään saman nimiset sheetit (index) suffiksilla, esim Jokujono (5)
                .flatMap(entry -> StreamUtils.zipWithIndex(entry.getValue().stream().sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())))
                        .map(indexedJonoSheet -> new ValintatapaJonoSheet(indexedJonoSheet.getValue(),
                                indexedJonoSheet.getValue().sheetName + Optional.of(indexedJonoSheet.getIndex()).map(i -> {
                                    if (i == 0L) {
                                        return "";
                                    } else {
                                        return " (" + new Long(i + 1L).toString() + ")";
                                    }
                                }).get())))
                // Sorttaus ensin käänteisesti pvm:n mukaan ja sitten prioriteetin
                .sorted((o1, o2) -> new CompareToBuilder().append(o2.getCreatedAt(), o1.getCreatedAt())
                        .append(o1.jono.getPrioriteetti(), o2.jono.getPrioriteetti()).toComparison())
                .forEach((jonoSheet) -> {

                    final XSSFSheet sheet = workbook.createSheet(jonoSheet.sheetName);
                    setColumnWidths(sheet);
                    addRow(sheet, "Haku", getTeksti(haku.getNimi()));
                    addRow(sheet, "Tarjoaja", getTeksti(hakukohdeDTO.getTarjoajaNimet()));
                    addRow(sheet, "Hakukohde", getTeksti(hakukohdeDTO.getHakukohteenNimet()));
                    addRow(sheet, "Vaihe", jonoSheet.vaihe.getNimi());
                    addRow(sheet, "Päivämäärä", ExcelExportUtil.DATE_FORMAT.format(jonoSheet.vaihe.getCreatedAt()));
                    addRow(sheet, "Jono", jonoSheet.jono.getNimi());
                    addRow(sheet);
                    if (jonoSheet.jono.getJonosijat().isEmpty()) {
                        addRow(sheet, "Jonolle ei ole valintalaskennan tuloksia");
                    } else {
                        addRow(sheet, columnHeaders);
                        sortedJonosijat(jonoSheet.jono).forEach(hakija -> {
                            final HakemusRivi rivi = new HakemusRivi(hakija, hakemusByOid.getOrDefault(hakija.getHakemusOid(), emptyHakemus));
                            final List<String> columnValues = columns.stream()
                                    .map(column -> column.extractor.apply(rivi))
                                    .collect(Collectors.toList());
                            addRow(sheet, columnValues);
                        });
                    }
                });
        return workbook;
    }
    private static class ValintatapaJonoSheet {
        public final ValintatietoValintatapajonoDTO jono;
        public final ValinnanvaiheDTO vaihe;
        public final String sheetName;
        public ValintatapaJonoSheet(ValintatietoValintatapajonoDTO jono, ValinnanvaiheDTO vaihe) {
            this.jono = jono;
            this.vaihe = vaihe;
            this.sheetName = jono.getNimi();
        }
        public ValintatapaJonoSheet(ValintatapaJonoSheet cloneSheetWithNewName, String sheetName) {
            this.jono = cloneSheetWithNewName.jono;
            this.vaihe = cloneSheetWithNewName.vaihe;
            this.sheetName = sheetName;
        }
        public Date getCreatedAt() {
            return vaihe.getCreatedAt();
        }
    }

    private static Hakemus emptyHakemus = new Hakemus();

    private static class Column {
        public final String name;
        public final int widthInCharacters;
        public final Function<HakemusRivi, String> extractor;

        public Column(final String name, final int widthInCharacters, final Function<HakemusRivi, String> extractor) {
            this.name = name;
            this.widthInCharacters = widthInCharacters;
            this.extractor = extractor;
        }
    }

    private static class HakemusRivi {
        public final JonosijaDTO hakija;
        public final Hakemus hakemus;

        public HakemusRivi(final JonosijaDTO hakija, final Hakemus hakemus) {
            this.hakija = hakija;
            this.hakemus = hakemus;
        }

        public String getHetu() {
            return trimToEmpty(hakemus.getAnswers().getHenkilotiedot().get("Henkilotunnus"));
        }
    }

    private static List<Column> columns = Arrays.asList(
            new Column("Jonosija", 14, rivi -> String.valueOf(rivi.hakija.getJonosija())),
            new Column("Sukunimi", 20, rivi -> rivi.hakija.getSukunimi()),
            new Column("Etunimi", 20, rivi -> rivi.hakija.getEtunimi()),
            new Column("Henkilötunnus", 20, rivi -> rivi.getHetu()),
            new Column("Hakemus OID", 20, rivi -> rivi.hakija.getHakemusOid()),
            new Column("Hakutoive", 14, rivi -> String.valueOf(rivi.hakija.getPrioriteetti())),
            new Column("Laskennan tulos", 20, rivi -> rivi.hakija.getTuloksenTila().toString()),
            new Column("Selite", 30, rivi -> getTeksti(getJarjestyskriteeri(rivi.hakija).getKuvaus())),
            new Column("Kokonaispisteet", 14, rivi -> nullSafeToString(getJarjestyskriteeri(rivi.hakija).getArvo())));

    private final static List<String> columnHeaders = columns.stream().map(column -> column.name).collect(Collectors.toList());

    private static Stream<JonosijaDTO> sortedJonosijat(final ValintatietoValintatapajonoDTO jono) {
        return jono.getJonosijat().stream().sorted((o1, o2) ->
                new CompareToBuilder().append(o1.getJonosija(), o2.getJonosija()
                ).append(o1.getSukunimi(),o2.getSukunimi()
                ).append(o1.getEtunimi(), o2.getEtunimi()).toComparison()
        );
    }
    private static JarjestyskriteeritulosDTO getJarjestyskriteeri(final JonosijaDTO hakija) {
        return hakija.getJarjestyskriteerit().isEmpty()
            ? new JarjestyskriteeritulosDTO(null, hakija.getTuloksenTila(), Collections.EMPTY_MAP, 1, "")
            : hakija.getJarjestyskriteerit().first();
    }

    private static String nullSafeToString(Object o) {
        if (o == null) return "";
        return o.toString();
    }

    private static void setColumnWidths(final XSSFSheet sheet) {
        for (int i = 0; i < columns.size(); i++) {
            sheet.setColumnWidth(i, columns.get(i).widthInCharacters * 256);
        }
    }

    private static void addRow(final XSSFSheet sheet, String... values) {
        addRow(sheet, asList(values));
    }

    private static void addRow(final XSSFSheet sheet, List<String> values) {
        final XSSFRow row = sheet.createRow(sheet.getPhysicalNumberOfRows());
        for (int col = 0; col < values.size(); col++) {
            row.createCell(col).setCellValue(values.get(col));
        }
    }
}
