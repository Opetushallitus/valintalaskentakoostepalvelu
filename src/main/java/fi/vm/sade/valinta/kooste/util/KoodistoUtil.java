package fi.vm.sade.valinta.kooste.util;

public class KoodistoUtil {

  /**
   * Siivoaa (tarjonta uri esim hakukohteet_123#3) versionumeron pois (palauttaa esimerkiksi
   * hakukohteet_123)
   *
   * @param tarjontaUri
   * @return uri ilman versionumeroa
   */
  public static String cleanUri(String tarjontaUri) {
    if (tarjontaUri.contains("#")) {
      return tarjontaUri.split("#")[0];
    }
    return tarjontaUri;
  }
}
