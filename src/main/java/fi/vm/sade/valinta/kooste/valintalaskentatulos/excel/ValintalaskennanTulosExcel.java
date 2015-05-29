package fi.vm.sade.valinta.kooste.valintalaskentatulos.excel;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti.getTeksti;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;

public class ValintalaskennanTulosExcel {
    public static List<Column> columns = Arrays.asList(
        new Column("Jonosija",        14, hakija -> String.valueOf(hakija.getJonosija())),
        new Column("Sukunimi",        20, JonosijaDTO :: getSukunimi),
        new Column("Etunimi",         20, JonosijaDTO :: getEtunimi),
        new Column("Hakemus OID",     20, JonosijaDTO :: getHakemusOid),
        new Column("Hakutoive",       14, hakija -> String.valueOf(hakija.getPrioriteetti())),
        new Column("Laskennan tulos", 20, hakija -> hakija.getTuloksenTila().toString()),
        new Column("Kokonaispisteet", 14, hakija -> hakija.getJarjestyskriteerit().isEmpty() ? "" : nullSafeToString(hakija.getJarjestyskriteerit().first().getArvo()))
    );

    private final static List<String> columnHeaders = columns.stream().map(column -> column.name).collect(Collectors.toList());

    public static XSSFWorkbook luoExcel(final HakukohdeDTO hakukohdeDTO, List<ValintatietoValinnanvaiheDTO> valinnanVaiheet) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        valinnanVaiheet.stream()
            .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
            .forEach(vaihe -> vaihe.getValintatapajonot().forEach(jono -> {
                final XSSFSheet sheet = workbook.createSheet(vaihe.getNimi() + " - " + jono.getNimi());
                setColumnWidths(sheet);
                addRow(sheet, asList("Tarjoaja", getTeksti(hakukohdeDTO.getTarjoajaNimi())));
                addRow(sheet, asList("Hakukohde", getTeksti(hakukohdeDTO.getHakukohdeNimi())));
                addRow(sheet, asList("Vaihe", vaihe.getNimi()));
                addRow(sheet, asList("Päivämäärä", ExcelExportUtil.DATE_FORMAT.format(vaihe.getCreatedAt())));
                addRow(sheet, asList("Jono", jono.getNimi()));
                addRow(sheet, asList());
                addRow(sheet, columnHeaders);
                sortedJonosijat(jono).forEach(hakija ->
                    addRow(sheet, columns.stream().map(column -> column.extractor.apply(hakija)).collect(Collectors.toList()))
                );
            }));
        return workbook;
    }

    private static Stream<JonosijaDTO> sortedJonosijat(final ValintatietoValintatapajonoDTO jono) {
        return jono.getJonosijat().stream().sorted((o1, o2) ->
            (o1.getJonosija() - o2.getJonosija()) * 100 +
                (trimToNull(o1.getSukunimi()).compareTo(trimToNull(o2.getSukunimi()))) * 10 +
                (trimToNull(o1.getEtunimi()).compareTo(trimToNull(o2.getEtunimi())))
        );
    }

    private static void setColumnWidths(final XSSFSheet sheet) {
        for (int i = 0; i < columns.size(); i++) {
            sheet.setColumnWidth(i, columns.get(i).widthInCharacters * 256);
        }
    }

    private static String nullSafeToString(Object o) {
        if (o == null) return "";
        return o.toString();
    }

    private static void addRow(final XSSFSheet sheet, List<String> values) {
        final XSSFRow row = sheet.createRow(sheet.getPhysicalNumberOfRows());
        for (int col = 0; col < values.size(); col++) {
            row.createCell(col).setCellValue(values.get(col));
        }
    }

    static class Column {
        public final String name;
        public final int widthInCharacters;
        public final Function<JonosijaDTO, String> extractor;

        public Column(final String name, final int widthInCharacters, final Function<JonosijaDTO, String> extractor) {
            this.name = name;
            this.widthInCharacters = widthInCharacters;
            this.extractor = extractor;
        }
    }
}
