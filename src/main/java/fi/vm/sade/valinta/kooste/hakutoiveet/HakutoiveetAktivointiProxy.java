package fi.vm.sade.valinta.kooste.hakutoiveet;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Portti(proxy), jonka Camel toteuttaa, hakutoiveiden reitityksen
 *         käynnistykseen
 */
public interface HakutoiveetAktivointiProxy {

    byte[] aktivoiHakutoiveetReitti(String hakutoiveetOid);

}
