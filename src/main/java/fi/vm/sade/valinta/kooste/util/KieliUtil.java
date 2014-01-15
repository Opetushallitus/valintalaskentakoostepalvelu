package fi.vm.sade.valinta.kooste.util;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public class KieliUtil {

    private static final String SUOMI_KIELI = "SUOMI";
    private static final String RUOTSI_KIELI = "RUOTSI";
    private static final String ENGLANTI_KIELI = "ENGLANTI";

    public static final String SUOMI = "FI";
    public static final String RUOTSI = "SE";
    public static final String ENGLANTI = "EN";

    /**
     * Kielikoodit pitaisi tulla muodossa "FI","SE", "EN". Kaytannossa voi tulla
     * vaikka "fi,se,en" tai "fi_FI" tai "ruotsi".
     * 
     * @param kielikoodi
     *            kielikoodi palveluntarjoajan omalla enkoodauksella
     * @return normalisoitu kielikoodi muotoa "FI","SE","EN"
     */
    public static String normalisoiKielikoodi(String kielikoodi) {
        if (kielikoodi == null) {
            return SUOMI; // Oletuksena suomi
        } else if (SUOMI_KIELI.equalsIgnoreCase(kielikoodi)) {
            return SUOMI;
        } else if (RUOTSI_KIELI.equalsIgnoreCase(kielikoodi)) {
            return RUOTSI;
        } else if (ENGLANTI_KIELI.equalsIgnoreCase(kielikoodi)) {
            return ENGLANTI;
        }
        // Ei tarkkaa osumaa. Kokeillaan nimenosilla ja preferoidaan suomea,
        // sitten ruotsia ja lopuksi englanti
        String uppercaseKielikoodi = kielikoodi.toUpperCase();
        if (uppercaseKielikoodi.contains(SUOMI)) {
            return SUOMI;
        } else if (uppercaseKielikoodi.contains(RUOTSI)) {
            return RUOTSI;
        }
        return ENGLANTI; // Tuntematon ulkomaa => englanti
    }

}
