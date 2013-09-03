package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface HyvaksymiskirjeBatchAktivointiProxy {

    // halutaanko riippuvuus viestintapalveluun? tassa voisi kayttaa oikeaa
    // hyvaksymiskirjebatch-tyyppia merkkijonon sijaan!
    String hyvaksymiskirjeBatchAktivointi(String hyvaksymiskirjeBatchJson);

    String addressLabelBatchAktivointi(String hakukohdeOid, List<String> valintakoeOid);
}
