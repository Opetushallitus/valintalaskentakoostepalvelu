package fi.vm.sade.valinta.kooste.parametrit.service;

public interface ParametriService {

    /**
     * Onko pistesyöttö mahdollista haulle?
     */
    boolean pistesyottoEnabled(String hakuOid);

    /**
     * Onko hakeneiden listaaminen mahdollista haulle?
     */
    boolean hakeneetEnabled(String hakuOid);

    /**
     * Voiko opiskelijoita valita harkinnanvaraisesti?
     */
    boolean harkinnanvaraisetEnabled(String hakuOid);

    /**
     * Voiko valintakoekutsuja generoida?
     */
    boolean valintakoekutsutEnabled(String hakuOid);

    /**
     * Voiko valintalaskentaa ja sijoittelua suorittaa?
     */
    boolean valintalaskentaEnabled(String hakuOid);

    /**
     * Voiko laskennan ja sijoittelun tulokset näyttää?
     */
    boolean valinnanhallintaEnabled(String hakuOid);

    /**
     * Onko hakijaryhmien listaaminen mahdollista haulle?
     */
    boolean hakijaryhmatEnabled(String hakuOid);
}
