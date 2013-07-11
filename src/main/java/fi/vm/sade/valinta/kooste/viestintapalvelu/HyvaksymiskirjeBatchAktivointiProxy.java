package fi.vm.sade.valinta.kooste.viestintapalvelu;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface HyvaksymiskirjeBatchAktivointiProxy {

    // halutaanko riippuvuus viestintapalveluun? tassa voisi kayttaa oikeaa
    // hyvaksymiskirjebatch-tyyppia merkkijonon sijaan!
    byte[] hyvaksymiskirjeBatchAktivointi(String hyvaksymiskirjeBatchJson);
}
