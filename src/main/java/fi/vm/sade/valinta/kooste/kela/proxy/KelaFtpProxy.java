package fi.vm.sade.valinta.kooste.kela.proxy;

import java.io.InputStream;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Camelin tarjoama Proxy : rajapinta tiedostojen siirtoon Kelalle
 * 
 *         Katso KelaMockFtpTesti
 * 
 */
public interface KelaFtpProxy {

    void lahetaTiedosto(String tiedostonNimi, InputStream tiedosto);
}
