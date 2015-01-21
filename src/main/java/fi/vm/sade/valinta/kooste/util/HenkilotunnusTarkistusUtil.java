package fi.vm.sade.valinta.kooste.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jussi Jartamo
 */
public class HenkilotunnusTarkistusUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HenkilotunnusTarkistusUtil.class);
    private final static char[] TARKISTUSMERKKI =
            {'0','1','2','3','4','5','6','7','8','9',
                    'A','B','C','D','E','F','H','J','K','L',
                    'M','N','P','R','S','T','U','V','W','X','Y'};

    /**
     * @param henkilotunnus
     * @return true jos henkilötunnus on rakenteellisesti oikein ja tarkistusmerkki täsmää
     */
    public static boolean tarkistaHenkilotunnus(String henkilotunnus) {
        if(henkilotunnus == null || henkilotunnus.length() != 11) {
            return false; // ei vastaa henkilötunnusta sisällön koolta
        }
        String syntymaaika = henkilotunnus.substring(0,6);
        String numero = henkilotunnus.substring(7,10);
        int tarkistettavaNumero;
        try {
            tarkistettavaNumero = Integer.parseInt(syntymaaika + numero);
        }catch(NumberFormatException e) {
            return false;
        }
        return TARKISTUSMERKKI[(tarkistettavaNumero%31)]==henkilotunnus.charAt(10);
    }
}
