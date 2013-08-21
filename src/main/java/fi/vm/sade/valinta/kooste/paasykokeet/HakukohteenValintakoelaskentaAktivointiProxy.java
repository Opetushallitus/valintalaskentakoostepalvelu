package fi.vm.sade.valinta.kooste.paasykokeet;


/**
 * @author Jussi Jartamo
 *         <p/>
 *         Portti(proxy), jonka Camel toteuttaa, valintalaskennan reitityksen
 *         käynnistykseen
 */
public interface HakukohteenValintakoelaskentaAktivointiProxy {

    void aktivoiValintakoelaskenta(String hakukohdeOid);

}
