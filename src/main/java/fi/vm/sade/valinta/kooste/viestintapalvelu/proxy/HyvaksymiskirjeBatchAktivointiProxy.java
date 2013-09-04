package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

/**
 * 
 * @author Jussi Jartamo
 * 
 */

public interface HyvaksymiskirjeBatchAktivointiProxy {

    // halutaanko riippuvuus viestintapalveluun? tassa voisi kayttaa oikeaa
    // hyvaksymiskirjebatch-tyyppia merkkijonon sijaan!
    String hyvaksymiskirjeetAktivointi(String hakukohdeOid, String hakuOid, Long sijoitteluajoId);

}
