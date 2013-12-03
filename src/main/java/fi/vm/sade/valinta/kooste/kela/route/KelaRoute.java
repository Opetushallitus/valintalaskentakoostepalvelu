package fi.vm.sade.valinta.kooste.kela.route;

import java.util.Date;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * @author Jussi Jartamo
 */
public interface KelaRoute {

    /**
     * Aloittaa Kela-siirtodokumentin luonnin.
     */
    void aloitaKelaLuonti(@Property(OPH.HAKUOID) String hakuOid,
    //
            @Property(PROPERTY_LUKUVUOSI) Date lukuvuosi,
            //
            @Property(PROPERTY_POIMINTAPAIVAMAARA) Date poimintapaivamaara,
            //
            @Property(PROPERTY_AINEISTONNIMI) String aineistonNimi,
            //
            @Property(PROPERTY_ORGANISAATIONNIMI) String organisaationNimi);

    /**
     * Camel route description.
     */
    String DIRECT_KELA_LUONTI = "direct:kela_luonti";
    /**
     * Camel route description.
     */
    String DIRECT_KELA_SIIRTO = "direct:kela_siirto";
    /**
     * Camel route description.
     */
    String DIRECT_KELA_FAILED = "direct:kela_failed";
    /**
     * Property hakuOid
     */
    String PROPERTY_HAKUOID = "hakuOid";
    /**
     * Property lukuvuosi
     */
    String PROPERTY_LUKUVUOSI = "lukuvuosi";
    /**
     * Property poimintapaivamaara
     */
    String PROPERTY_POIMINTAPAIVAMAARA = "poimintapaivamaara";
    /**
     * Property aineistonNimi
     */
    String PROPERTY_AINEISTONNIMI = "aineistonNimi";
    /**
     * Property organisaationNimi
     */
    String PROPERTY_ORGANISAATIONNIMI = "organisaationNimi";
    /**
     * Property lukuvuosi
     */
    String PROPERTY_DOKUMENTTI_ID = "dokumenttiId";
}
