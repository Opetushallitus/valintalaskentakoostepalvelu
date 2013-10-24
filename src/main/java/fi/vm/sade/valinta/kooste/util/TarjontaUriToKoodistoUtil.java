package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.util.KoodiServiceSearchCriteriaBuilder;

public class TarjontaUriToKoodistoUtil {

    /**
     * Siivoaa (tarjonta uri esim hakukohteet_123#3) versionumeron pois
     * (palauttaa esimerkiksi hakukohteet_123)
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

    public static Integer stripVersion(String tarjontaUri) {
        if (tarjontaUri.contains("#")) {
            try {
                return Integer.parseInt(tarjontaUri.split("#")[1]);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static SearchKoodisCriteriaType toSearchCriteria(String koodiUri, Integer koodiVersio) {
        if (koodiVersio == null) {
            return KoodiServiceSearchCriteriaBuilder.latestAcceptedKoodiByUri(koodiUri);
        } else {
            return KoodiServiceSearchCriteriaBuilder.koodiByUriAndVersion(koodiUri, koodiVersio);
        }
    }

    public static SearchKoodisCriteriaType toSearchCriteria(String tarjontaUri) {
        String koodiUri = cleanUri(tarjontaUri);
        Integer koodiVersio = stripVersion(tarjontaUri);
        return toSearchCriteria(koodiUri, koodiVersio);
    }
}
