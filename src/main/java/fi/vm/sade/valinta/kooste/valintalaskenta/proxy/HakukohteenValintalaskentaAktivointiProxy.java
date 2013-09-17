package fi.vm.sade.valinta.kooste.valintalaskenta.proxy;


/**
 * @author Jussi Jartamo
 *         <p/>
 *         Portti(proxy), jonka Camel toteuttaa, valintalaskennan reitityksen
 *         käynnistykseen
 */
public interface HakukohteenValintalaskentaAktivointiProxy {

    void aktivoiValintalaskenta(String hakukohdeOid, Integer valinnanvaihe);

}
