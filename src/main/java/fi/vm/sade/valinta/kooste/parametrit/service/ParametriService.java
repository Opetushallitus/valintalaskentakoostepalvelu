package fi.vm.sade.valinta.kooste.parametrit.service;

/**
 * User: tommiha
 * Date: 8/21/13
 * Time: 10:04 AM
 */
public interface ParametriService {

    /**
     * Onko pistesyöttö mahdollista haulle?
     * @param hakuOid
     * @return
     */
    boolean pistesyottoEnabled(String hakuOid);

    /**
     * Onko hakeneiden listaaminen mahdollista haulle?
     * @param hakuOid
     * @return
     */
    boolean hakeneetEnabled(String hakuOid);

    /**
     * Voiko opiskelijoita valita harkinnanvaraisesti?
     * @param hakuOid
     * @return
     */
    boolean harkinnanvaraisetEnabled(String hakuOid);

    /**
     * Voiko valintakoekutsuja generoida?
     * @param hakuOid
     * @return
     */
    boolean valintakoekutsutEnabled(String hakuOid);

    /**
     * Voiko valintalaskentaa ja sijoittelua suorittaa?
     * @param hakuOid
     * @return
     */
    boolean valintalaskentaEnabled(String hakuOid);

    /**
     * Voiko laskennan ja sijoittelun tulokset näyttää?
     * @param hakuOid
     * @return
     */
    boolean valinnanhallintaEnabled(String hakuOid);

    /**
     * Onko hakijaryhmien listaaminen mahdollista haulle?
     * @param hakuOid
     * @return
     */
    boolean hakijaryhmatEnabled(String hakuOid);
}
