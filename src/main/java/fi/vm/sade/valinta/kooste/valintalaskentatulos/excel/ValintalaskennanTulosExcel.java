package fi.vm.sade.valinta.kooste.valintalaskentatulos.excel;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti.getTeksti;
import static java.util.Arrays.asList;

import com.codepoetics.protonpack.Indexed;
import com.codepoetics.protonpack.StreamUtils;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HyvaksynnanEhto;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.FunktioTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteeritulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public class ValintalaskennanTulosExcel {
  public static SXSSFWorkbook luoExcel(
      Map<String, HyvaksynnanEhto> hyvaksynnanEhdot,
      Map<String, Map<String, HyvaksynnanEhto>> hyvaksynnanEhdotValintatapajonoissa,
      Haku haku,
      AbstractHakukohde hakukohdeDTO,
      List<Organisaatio> tarjoajat,
      List<ValintatietoValinnanvaiheDTO> valinnanVaiheet,
      final List<HakemusWrapper> hakemukset) {
    final Map<String, HakemusWrapper> hakemusByOid =
        hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h));

    SXSSFWorkbook workbook = new SXSSFWorkbook(100);
    valinnanVaiheet.stream()
        .flatMap(
            vaihe ->
                vaihe.getValintatapajonot().stream()
                    .map(jono -> new ValintatapaJonoSheet(jono, vaihe)))
        .collect(
            Collectors.groupingBy(jonoSheet -> StringUtils.substring(jonoSheet.sheetName, 0, 31)))
        .entrySet()
        .stream()
        .flatMap(ValintalaskennanTulosExcel::toValintatapajonoStream)
        .sorted(ValintalaskennanTulosExcel::byReverseDateAndPriority)
        .forEach(
            (jonoSheet) -> {
              final SXSSFSheet sheet = workbook.createSheet(jonoSheet.sheetName);
              final ValinnanvaiheDTO vaihe = jonoSheet.vaihe;
              final ValintatietoValintatapajonoDTO jono = jonoSheet.jono;

              setColumnWidths(sheet);
              addRow(sheet, "Haku", getTeksti(haku.nimi));
              addRow(
                  sheet,
                  "Tarjoaja",
                  getTeksti(
                      tarjoajat.stream().map(Organisaatio::getNimi).collect(Collectors.toList()),
                      " - "));
              addRow(sheet, "Hakukohde", getTeksti(hakukohdeDTO.nimi));
              addRow(sheet, "Vaihe", vaihe.getNimi());
              addRow(sheet, "Päivämäärä", ExcelExportUtil.DATE_FORMAT.format(vaihe.getCreatedAt()));
              addRow(sheet, "Jono", jono.getNimi());
              addRow(sheet);
              if (jono.getJonosijat().isEmpty()) {
                addRow(sheet, "Jonolle ei ole valintalaskennan tuloksia");
              } else {
                final List<Column> dynamicColumns = dynamicColumns(jono);
                final List<String> allColumnHeaders =
                    Stream.concat(fixedColumns.stream(), dynamicColumns.stream())
                        .map(h -> h.name)
                        .collect(Collectors.toList());
                addRow(sheet, allColumnHeaders);
                addJonosijaRows(
                    hakemusByOid,
                    jono,
                    sheet,
                    dynamicColumns,
                    hyvaksynnanEhdot,
                    hyvaksynnanEhdotValintatapajonoissa);
              }
            });
    return workbook;
  }

  private static Stream<? extends ValintatapaJonoSheet> toValintatapajonoStream(
      Map.Entry<String, List<ValintatapaJonoSheet>> entry) {
    final Stream<ValintatapaJonoSheet> entriesSortedByCreationDateStream =
        entry.getValue().stream()
            .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
    return StreamUtils.zipWithIndex(entriesSortedByCreationDateStream)
        .map(ValintalaskennanTulosExcel::toValintatapaJonoSheet);
  }

  private static int byReverseDateAndPriority(ValintatapaJonoSheet o1, ValintatapaJonoSheet o2) {
    return new CompareToBuilder()
        .append(o2.getCreatedAt(), o1.getCreatedAt())
        .append(o1.jono.getPrioriteetti(), o2.jono.getPrioriteetti())
        .toComparison();
  }

  private static ValintatapaJonoSheet toValintatapaJonoSheet(
      Indexed<ValintatapaJonoSheet> indexedJonoSheet) {
    String sheetName = indexedJonoSheet.getValue().sheetName;
    if (indexedJonoSheet.getIndex() > 0L) {
      String indexSuffix = " " + (indexedJonoSheet.getIndex() + 1L);
      sheetName = (StringUtils.substring(sheetName, 0, 31 - indexSuffix.length())) + indexSuffix;
    }
    return new ValintatapaJonoSheet(indexedJonoSheet.getValue(), sheetName);
  }

  private static void addJonosijaRows(
      Map<String, HakemusWrapper> hakemusByOid,
      ValintatietoValintatapajonoDTO jono,
      SXSSFSheet sheet,
      List<Column> dynamicColumns,
      Map<String, HyvaksynnanEhto> hyvaksynnanEhdot,
      Map<String, Map<String, HyvaksynnanEhto>> hyvaksynnanEhdotValintatapajonoissa) {
    sortedJonosijat(jono)
        .map(
            hakija -> {
              final Map<String, HyvaksynnanEhto> ehdot =
                  hyvaksynnanEhdot.containsKey(hakija.getHakemusOid())
                      ? hyvaksynnanEhdot
                      : hyvaksynnanEhdotValintatapajonoissa.getOrDefault(jono.getOid(), Map.of());
              final HakemusRivi hakemusRivi =
                  new HakemusRivi(hakija, hakemusByOid.get(hakija.getHakemusOid()), ehdot);
              return Stream.concat(fixedColumns.stream(), dynamicColumns.stream())
                  .map(column -> column.extractor.apply(hakemusRivi))
                  .collect(Collectors.toList());
            })
        .forEach(v -> addRow(sheet, v));
  }

  private static List<Column> dynamicColumns(ValintatietoValintatapajonoDTO jono) {
    return jono.getJonosijat().stream()
        .flatMap(js -> js.getFunktioTulokset().stream())
        .map(DynamicColumnHeader::new)
        .distinct()
        .sorted()
        .map(header -> new Column(header.tunniste, 14, rivi -> extractValue(header.tunniste, rivi)))
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

    public Column(
        final String name,
        final int widthInCharacters,
        final Function<HakemusRivi, String> extractor) {
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
      if (StringUtils.isBlank(funktioTulosDTO.getTunniste())) {
        throw new IllegalArgumentException(
            "Tyhjä funktiotuloksen tunniste ei ole sallittu:"
                + ToStringBuilder.reflectionToString(funktioTulosDTO));
      }
      this.tunniste = funktioTulosDTO.getTunniste();
      this.nimiFi = funktioTulosDTO.getNimiFi();
      this.nimiSv = funktioTulosDTO.getNimiSv();
      this.nimiEn = funktioTulosDTO.getNimiEn();
      this.omaOpintopolku = funktioTulosDTO.isOmaopintopolku();
    }

    @Override
    public int hashCode() {
      return tunniste.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof DynamicColumnHeader)) {
        return false;
      }
      return ((DynamicColumnHeader) obj).tunniste.equals(tunniste);
    }

    @Override
    public int compareTo(DynamicColumnHeader o) {
      return tunniste.compareTo(o.tunniste);
    }
  }

  private static class HakemusRivi {
    public final JonosijaDTO hakija;
    public final HakemusWrapper hakemus;
    public final HyvaksynnanEhto hyvaksynnanEhto;

    HakemusRivi(
        final JonosijaDTO hakija,
        final HakemusWrapper hakemus,
        Map<String, HyvaksynnanEhto> hyvaksynnanEhdot) {
      this.hakija = hakija;
      this.hakemus =
          Objects.requireNonNull(
              hakemus, String.format("Hakemusta oidilla %s ei löytynyt", hakija.getHakemusOid()));
      this.hyvaksynnanEhto =
          hyvaksynnanEhdot.getOrDefault(hakija.getHakemusOid(), new HyvaksynnanEhto());
    }
  }

  private static final List<Column> fixedColumns =
      Arrays.asList(
          new Column("Jonosija", 14, rivi -> String.valueOf(rivi.hakija.getJonosija())),
          new Column("Sukunimi", 20, rivi -> rivi.hakemus.getSukunimi()),
          new Column("Etunimi", 20, rivi -> rivi.hakemus.getEtunimet()),
          new Column("Henkilötunnus", 20, rivi -> rivi.hakemus.getHenkilotunnus()),
          new Column("Sähköpostiosoite", 20, rivi -> rivi.hakemus.getSahkopostiOsoite()),
          new Column("Hakemus OID", 20, rivi -> rivi.hakemus.getOid()),
          new Column("Hakutoive", 14, rivi -> String.valueOf(rivi.hakija.getPrioriteetti())),
          new Column("Laskennan tulos", 20, rivi -> rivi.hakija.getTuloksenTila().toString()),
          new Column(
              "Selite", 30, rivi -> getTeksti(getJarjestyskriteeri(rivi.hakija).getKuvaus())),
          new Column(
              "Kokonaispisteet",
              14,
              rivi -> nullSafeToString(getJarjestyskriteeri(rivi.hakija).getArvo())),
          new Column("Hyväksynnän ehto (FI)", 30, rivi -> rivi.hyvaksynnanEhto.fi),
          new Column("Hyväksynnän ehto (SV)", 30, rivi -> rivi.hyvaksynnanEhto.sv),
          new Column("Hyväksynnän ehto (EN)", 30, rivi -> rivi.hyvaksynnanEhto.en));

  private static Stream<JonosijaDTO> sortedJonosijat(final ValintatietoValintatapajonoDTO jono) {
    return jono.getJonosijat().stream()
        .sorted(
            (o1, o2) ->
                new CompareToBuilder()
                    .append(o1.getJonosija(), o2.getJonosija())
                    .append(o1.getSukunimi(), o2.getSukunimi())
                    .append(o1.getEtunimi(), o2.getEtunimi())
                    .toComparison());
  }

  private static JarjestyskriteeritulosDTO getJarjestyskriteeri(final JonosijaDTO hakija) {
    return hakija.getJarjestyskriteerit().isEmpty()
        ? new JarjestyskriteeritulosDTO(
            null, hakija.getTuloksenTila(), Collections.emptyMap(), 1, "")
        : hakija.getJarjestyskriteerit().first();
  }

  private static String nullSafeToString(Object o) {
    return o == null ? "" : o.toString();
  }

  private static void setColumnWidths(final SXSSFSheet sheet) {
    for (int i = 0, columnCount = fixedColumns.size(); i < columnCount; i++) {
      sheet.setColumnWidth(i, fixedColumns.get(i).widthInCharacters * 256);
    }
  }

  private static void addRow(final SXSSFSheet sheet, String... values) {
    addRow(sheet, asList(values));
  }

  private static void addRow(final SXSSFSheet sheet, List<String> values) {
    final SXSSFRow row = sheet.createRow(sheet.getPhysicalNumberOfRows());
    for (int col = 0, count = values.size(); col < count; col++) {
      row.createCell(col).setCellValue(values.get(col));
    }
  }

  private static class ValintatapaJonoSheet {
    public final ValintatietoValintatapajonoDTO jono;
    public final ValinnanvaiheDTO vaihe;
    public final String sheetName;

    private String removeSpecialCharacters(String name) {
      return name.replaceAll("[^\\p{L}\\p{Digit}\\-\\s.,]+", "");
    }

    public ValintatapaJonoSheet(ValintatietoValintatapajonoDTO jono, ValinnanvaiheDTO vaihe) {
      this.jono = jono;
      this.vaihe = vaihe;
      this.sheetName = removeSpecialCharacters(jono.getNimi());
    }

    public ValintatapaJonoSheet(ValintatapaJonoSheet cloneSheetWithNewName, String sheetName) {
      this.jono = cloneSheetWithNewName.jono;
      this.vaihe = cloneSheetWithNewName.vaihe;
      this.sheetName = removeSpecialCharacters(sheetName);
    }

    public Date getCreatedAt() {
      return vaihe.getCreatedAt();
    }
  }
}
