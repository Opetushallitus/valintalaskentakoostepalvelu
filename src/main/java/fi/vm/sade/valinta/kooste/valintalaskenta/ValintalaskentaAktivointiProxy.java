package fi.vm.sade.valinta.kooste.valintalaskenta;


/**
 * 
 * @author Jussi Jartamo
 * 
 *         Portti(proxy), jonka Camel toteuttaa, valintalaskennan reitityksen
 *         käynnistykseen
 */
public interface ValintalaskentaAktivointiProxy {

    void aktivoiValintalaskenta(String hakukohdeOid, Integer valinnanvaihe);

}
