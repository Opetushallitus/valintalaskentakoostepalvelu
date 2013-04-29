package fi.vm.sade.valinta.kooste.sijoittelu;


/**
 * 
 * @author Jussi Jartamo
 * 
 *         Portti(proxy), jonka Camel toteuttaa, valintalaskennan reitityksen
 *         käynnistykseen
 */
public interface SijoitteluAktivointiProxy {

    void aktivoiSijoittelu(String hakuOid);

}
