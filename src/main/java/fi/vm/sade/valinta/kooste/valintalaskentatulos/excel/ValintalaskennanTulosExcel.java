package fi.vm.sade.valinta.kooste.valintalaskentatulos.excel;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti.getTeksti;
import static java.util.Arrays.asList;
import com.codepoetics.protonpack.Indexed;
import com.codepoetics.protonpack.StreamUtils;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.FunktioTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteeritulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ValintalaskennanTulosExcel {
    public static XSSFWorkbook luoExcel(HakuV1RDTO haku, final HakukohdeV1RDTO hakukohdeDTO, List<ValintatietoValinnanvaiheDTO> valinnanVaiheet, final List<HakemusWrapper> hakemukset) {
        final Map<String, HakemusWrapper> hakemusByOid = hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h));

        XSSFWorkbook workbook = new XSSFWorkbook();
        valinnanVaiheet.stream()
                .flatMap(vaihe -> vaihe.getValintatapajonot().stream().map(jono -> new ValintatapaJonoSheet(jono, vaihe)))
                .collect(Collectors.groupingBy(jonoSheet -> jonoSheet.sheetName)).entrySet().stream()
                        .flatMap(ValintalaskennanTulosExcel::toValintatapajonoStream)
                        .sorted(ValintalaskennanTulosExcel::byReverseDateAndPriority)
                        .forEach((jonoSheet) -> {
                            final XSSFSheet sheet = workbook.createSheet(jonoSheet.sheetName);
                            final ValinnanvaiheDTO vaihe = jonoSheet.vaihe;
                            final ValintatietoValintatapajonoDTO jono = jonoSheet.jono;

                            setColumnWidths(sheet);
                            addRow(sheet, "Haku", getTeksti(haku.getNimi()));
                            addRow(sheet, "Tarjoaja", getTeksti(hakukohdeDTO.getTarjoajaNimet()));
                            addRow(sheet, "Hakukohde", getTeksti(hakukohdeDTO.getHakukohteenNimet()));
                            addRow(sheet, "Vaihe", vaihe.getNimi());
                            addRow(sheet, "Päivämäärä", ExcelExportUtil.DATE_FORMAT.format(vaihe.getCreatedAt()));
                            addRow(sheet, "Jono", jono.getNimi());
                            addRow(sheet);
                            if (jono.getJonosijat().isEmpty()) {
                                addRow(sheet, "Jonolle ei ole valintalaskennan tuloksia");
                            } else {
                                final List<String> fixedColumnHeaders = fixedColumnHeaders();
                                final List<DynamicColumnHeader> dynamicColumnHeaders = dynamicColumnHeaders(jono);
                                final List<String> allColumnHeaders = Stream
                                    .concat(
                                        fixedColumnHeaders.stream(),
                                        dynamicColumnHeaders.stream().map(h -> h.tunniste))
                                    .collect(Collectors.toList());
                                addRow(sheet, allColumnHeaders);
                                addJonosijaRows(hakemusByOid, jono, sheet, dynamicColumnHeaders);
                            }
                        }
                );
        return workbook;
    }

    private static Stream<? extends ValintatapaJonoSheet> toValintatapajonoStream(Map.Entry<String, List<ValintatapaJonoSheet>> entry) {
        final Stream<ValintatapaJonoSheet> entriesSortedByCreationDateStream = entry.getValue().stream().sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        return StreamUtils.zipWithIndex(entriesSortedByCreationDateStream).map(ValintalaskennanTulosExcel::toValintatapaJonoSheet);
    }

    private static int byReverseDateAndPriority(ValintatapaJonoSheet o1, ValintatapaJonoSheet o2) {
        return new CompareToBuilder().append(o2.getCreatedAt(), o1.getCreatedAt()).append(o1.jono.getPrioriteetti(), o2.jono.getPrioriteetti()).toComparison();
    }

    private static ValintatapaJonoSheet toValintatapaJonoSheet(Indexed<ValintatapaJonoSheet> indexedJonoSheet) {
        final String index = indexedJonoSheet.getIndex() == 0L ? "" : " (" + (indexedJonoSheet.getIndex() + 1L) + ")";
        final String sheetName = indexedJonoSheet.getValue().sheetName + index;
        final String truncatedSheetName = StringUtils.substring(sheetName, 0, 31 - " (0)".length());
        return new ValintatapaJonoSheet(indexedJonoSheet.getValue(), truncatedSheetName);
    }

    private static void addJonosijaRows(Map<String,HakemusWrapper> hakemusByOid,
                                        ValintatietoValintatapajonoDTO jono,
                                        XSSFSheet sheet,
                                        List<DynamicColumnHeader> dynamicColumnHeaders) {
        sortedJonosijat(jono)
                .map(hakija -> {
                    final Stream<Column> fixedColumnValuesStream = fixedColumns.stream();
                    final Stream<Column> dynamicColumnValuesStream = dynamicColumnHeaders.stream()
                        .map(header ->
                            new Column(header.tunniste, 14, rivi ->
                                extractValue(header.tunniste, rivi)));
                    final HakemusRivi hakemusRivi = new HakemusRivi(hakija, hakemusByOid.get(hakija.getHakemusOid()));
                    return Stream.concat(fixedColumnValuesStream, dynamicColumnValuesStream)
                            .map(column -> column.extractor.apply(hakemusRivi))
                            .collect(Collectors.toList());
                })
                .forEach(v -> addRow(sheet, v));
    }

    private static List<String> fixedColumnHeaders() {
        return fixedColumns.stream().map(column -> column.name).collect(Collectors.toList());
    }

    private static List<DynamicColumnHeader> dynamicColumnHeaders(ValintatietoValintatapajonoDTO jono) {
        return jono
            .getJonosijat()
            .stream()
            .flatMap(js -> js.getFunktioTulokset().stream())
            .map(DynamicColumnHeader::new)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    private static String extractValue(String tunniste, HakemusRivi rivi) {
        return rivi.hakija.getFunktioTulokset().stream()
                .filter(x -> x.getTunniste().equals(tunniste))
                .findFirst()
                .map(FunktioTulosDTO::getArvo)
                .orElse("");
    }

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

    private static class DynamicColumnHeader implements Comparable<DynamicColumnHeader> {
        public final String tunniste;
        public final String nimiFi;
        public final String nimiSv;
        public final String nimiEn;
        public final boolean omaOpintopolku;

        public DynamicColumnHeader(FunktioTulosDTO funktioTulosDTO) {
            this.tunniste = funktioTulosDTO.getTunniste();
            this.nimiFi = funktioTulosDTO.getNimiFi();
            this.nimiSv = funktioTulosDTO.getNimiSv();
            this.nimiEn = funktioTulosDTO.getNimiEn();
            this.omaOpintopolku = funktioTulosDTO.isOmaopintopolku();
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public int compareTo(DynamicColumnHeader o) {
            if (tunniste == null) {
                return -1;
            }
            return tunniste.compareTo(o.tunniste);
        }
    }

    private static class HakemusRivi {
        public final JonosijaDTO hakija;
        public final HakemusWrapper hakemus;

        HakemusRivi(final JonosijaDTO hakija, final HakemusWrapper hakemus) {
            this.hakija = hakija;
            this.hakemus = Objects.requireNonNull(hakemus, String.format("Hakemusta oidilla %s ei löytynyt", hakija.getHakemusOid()));
        }
    }

    private static final List<Column> fixedColumns = Arrays.asList(
            new Column("Jonosija", 14, rivi -> String.valueOf(rivi.hakija.getJonosija())),
            new Column("Sukunimi", 20, rivi -> rivi.hakemus.getSukunimi()),
            new Column("Etunimi", 20, rivi -> rivi.hakemus.getEtunimet()),
            new Column("Henkilötunnus", 20, rivi -> rivi.hakemus.getHenkilotunnus()),
            new Column("Sähköpostiosoite", 20, rivi -> rivi.hakemus.getSahkopostiOsoite()),
            new Column("Hakemus OID", 20, rivi -> rivi.hakemus.getOid()),
            new Column("Hakutoive", 14, rivi -> String.valueOf(rivi.hakija.getPrioriteetti())),
            new Column("Laskennan tulos", 20, rivi -> rivi.hakija.getTuloksenTila().toString()),
            new Column("Selite", 30, rivi -> getTeksti(getJarjestyskriteeri(rivi.hakija).getKuvaus())),
            new Column("Kokonaispisteet", 14, rivi -> nullSafeToString(getJarjestyskriteeri(rivi.hakija).getArvo()))
    );

    private static Stream<JonosijaDTO> sortedJonosijat(final ValintatietoValintatapajonoDTO jono) {
        return jono.getJonosijat().stream().sorted((o1, o2) ->
                new CompareToBuilder()
                        .append(o1.getJonosija(), o2.getJonosija()).append(o1.getSukunimi(),o2.getSukunimi())
                        .append(o1.getEtunimi(), o2.getEtunimi())
                        .toComparison()
        );
    }
    private static JarjestyskriteeritulosDTO getJarjestyskriteeri(final JonosijaDTO hakija) {
        return hakija.getJarjestyskriteerit().isEmpty()
            ? new JarjestyskriteeritulosDTO(null, hakija.getTuloksenTila(), Collections.emptyMap(), 1, "")
            : hakija.getJarjestyskriteerit().first();
    }

    private static String nullSafeToString(Object o) {
         return o == null ? "" : o.toString();
    }

    private static void setColumnWidths(final XSSFSheet sheet) {
        for (int i = 0, columnCount = fixedColumns.size(); i < columnCount; i++) {
            sheet.setColumnWidth(i, fixedColumns.get(i).widthInCharacters * 256);
        }
    }

    private static void addRow(final XSSFSheet sheet, String... values) {
        addRow(sheet, asList(values));
    }

    private static void addRow(final XSSFSheet sheet, List<String> values) {
        final XSSFRow row = sheet.createRow(sheet.getPhysicalNumberOfRows());
        for (int col = 0, count = values.size(); col < count; col++) {
            row.createCell(col).setCellValue(values.get(col));
        }
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
}
