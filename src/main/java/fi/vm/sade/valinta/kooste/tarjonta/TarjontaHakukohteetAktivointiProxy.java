package fi.vm.sade.valinta.kooste.tarjonta;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Portti(proxy), jonka Camel toteuttaa, valintalaskennan reitityksen
 *         käynnistykseen
 */
public interface TarjontaHakukohteetAktivointiProxy {

    void aktivoiTarjontaHakukohteet(String hakuOid);
}
