package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;

import fi.vm.sade.valinta.kooste.util.Formatter;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Wrapperi sijoittelun HakutoiveDTO:lle jossa meta dataa liittyen
 *         kaikkiin hakukohteen hakijoihin
 */
public class MetaHakukohde {

    private final String hakukohdeNimi;
    private final String tarjoajaNimi;
    private BigDecimal alinHyvaksyttyPistemaara;
    private int hyvaksytyt;
    private int kaikkiHakeneet;

    public MetaHakukohde(String hakukohdeNimi, String tarjoajaNimi) {
        this.hakukohdeNimi = hakukohdeNimi;
        this.tarjoajaNimi = tarjoajaNimi;
        this.alinHyvaksyttyPistemaara = null;
        this.hyvaksytyt = 0;
        this.kaikkiHakeneet = 0;
    }

    private BigDecimal paivitaAlinHyvaksyttyPistemaara(BigDecimal kandidaatti) {
        if (kandidaatti == null) { // hakija on hyvaksytty pisteetta! esim
                                   // harkinnanvaraisesti!
            return alinHyvaksyttyPistemaara;
        }
        if (alinHyvaksyttyPistemaara == null) {
            return kandidaatti;
        }
        return alinHyvaksyttyPistemaara.min(kandidaatti);
    }

    /**
     * 
     * @param pisteet
     *            Hyvaksytyn hakukohteen pisteet! Saa olla null!
     */
    public void paivitaHyvaksytyt(BigDecimal pisteet) {
        ++hyvaksytyt;
        alinHyvaksyttyPistemaara = paivitaAlinHyvaksyttyPistemaara(pisteet);
    }

    public void paivitaKaikkiHakeneet() {
        ++kaikkiHakeneet;
    }

    public String getHakukohdeNimi() {
        return hakukohdeNimi;
    }

    public String getTarjoajaNimi() {
        return tarjoajaNimi;
    }

    public String getAlinHyvaksyttyPistemaara() {
        if (alinHyvaksyttyPistemaara == null) { // kohteella ei ole pisteilla
                                                // hyväksyttyjä hakijoita!
            return StringUtils.EMPTY;
        }
        return Formatter.NUMERO_FORMAATTI.format(alinHyvaksyttyPistemaara);
    }

    public String getHyvaksytyt() {
        return Formatter.NUMERO_FORMAATTI.format(hyvaksytyt);
    }

    public String getKaikkiHakeneet() {
        return Formatter.NUMERO_FORMAATTI.format(kaikkiHakeneet);
    }
}
