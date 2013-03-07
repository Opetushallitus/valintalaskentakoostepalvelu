package fi.vm.sade.valinta.kooste.paasykokeet;


/**
 * 
 * @author Jussi Jartamo
 * 
 *         Portti(proxy), jonka Camel toteuttaa, valintalaskennan reitityksen
 *         käynnistykseen
 */
public interface ValintakoelaskentaAktivointiProxy {

    void aktivoiValintakoelaskenta(String hakukohdeOid);

}
