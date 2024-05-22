package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;

public class ExcelTestData {
  public static InputStream kkHakuToisenAsteenValintatuloksella() {
    return getInputStream("kkhaku_toisenasteen_valintatuloksella.xlsx");
  }

  public static InputStream kkHakuPuuttuviaPakollisiaTietoja() {
    return getInputStream("kkhaku_puuttuvia_pakollisia_tietoja.xlsx");
  }

  public static InputStream puutteellisiaTietojaAutotayttoaVarten() {
    return getInputStream("puutteellisiaTietojaAutotayttoaVarten.xlsx");
  }

  public static InputStream erillisHakuHakemusOidilla() {
    return getInputStream("erillishaku_oidilla.xlsx");
  }

  public static InputStream erillisHakuSyntymaAjalla() {
    return getInputStream("erillishaku_syntymaajalla.xlsx");
  }

  public static InputStream erillisHakuTuntemattomallaKielella() {
    return getInputStream("erillishaku_tuntemattomalla_aidinkielella.xlsx");
  }

  public static InputStream toisenAsteenErillisHaku() {
    return getInputStream("erillishaku_toinen_aste.xlsx");
  }

  private static InputStream getInputStream(final String filename) {
    try {
      return new ClassPathResource(filename).getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
