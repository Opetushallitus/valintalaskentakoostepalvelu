package fi.vm.sade.valinta.kooste.paasykokeet;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Portti(proxy), jonka Camel toteuttaa, hakutoiveiden reitityksen
 *         käynnistykseen
 */
public interface HakuPaasykokeetAktivointiProxy {

    byte[] aktivoiHakuPaasykokeetReitti(String hakukohdeOid);

}
