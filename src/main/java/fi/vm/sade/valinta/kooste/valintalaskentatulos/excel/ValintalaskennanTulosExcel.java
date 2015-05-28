package fi.vm.sade.valinta.kooste.valintalaskentatulos.excel;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti.getTeksti;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

public class ValintalaskennanTulosExcel {
    public static List<Column> columns = Arrays.asList(
        new Column("Jonosija",        14, hakija -> String.valueOf(hakija.getJonosija())),
        new Column("Sukunimi",        20, HakijaDTO :: getSukunimi),
        new Column("Etunimi",         20, HakijaDTO :: getEtunimi),
        /*new Column("Henkilötunnus", hakija -> ""), // TODO: hetu ei saatavilla*/
        new Column("Hakemus OID",     20, HakijaDTO :: getHakemusOid),
        new Column("Laskennan tulos", 20, hakija -> hakija.getTila().toString()),
        /*new Column("Selite", hakija -> ""), // TODO: mites tähän?*/
        new Column("Kokonaispisteet", 20, hakija -> nullSafeToString(hakija.getPisteet()))
    );

    private final static List<String> columnHeaders = columns.stream().map(column -> column.name).collect(Collectors.toList());

    public static XSSFWorkbook luoExcel(final HakukohdeDTO hakukohdeDTO, List<ValintatietoValinnanvaiheDTO> valinnanVaiheet) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        valinnanVaiheet.stream().forEach(vaihe -> {
            vaihe.getValintatapajonot().forEach(jono -> {
                final XSSFSheet sheet = workbook.createSheet(vaihe.getNimi() + " - " + jono.getNimi());
                setColumnWidths(sheet);
                addRow(sheet, asList("Tarjoaja", getTeksti(hakukohdeDTO.getTarjoajaNimi())));
                addRow(sheet, asList("Hakukohde", getTeksti(hakukohdeDTO.getHakukohdeNimi())));
                addRow(sheet, asList("Vaihe", vaihe.getNimi()));
                addRow(sheet, asList("Jono", jono.getNimi()));
                addRow(sheet, asList());
                addRow(sheet, columnHeaders);
                for (HakijaDTO hakija : jono.getHakija()) {
                    addRow(sheet, columns.stream().map(column -> column.extractor.apply(hakija)).collect(Collectors.toList()));
                }
            });
        });
        return workbook;
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
        public final Function<HakijaDTO, String> extractor;

        public Column(final String name, final int widthInCharacters, final Function<HakijaDTO, String> extractor) {
            this.name = name;
            this.widthInCharacters = widthInCharacters;
            this.extractor = extractor;
        }
    }
}
