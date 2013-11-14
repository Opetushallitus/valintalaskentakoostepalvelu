package fi.vm.sade.valinta.cache.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Portaali tilapaisdokumenteille
 */
@Path("cache")
public interface CacheResource {

    /**
     * Lataa dokumentin
     * 
     * @param documentId
     *            esim viestintapalvelu.hyvaksymiskirje.3241234ID
     * @return dokumentti
     */
    @GET
    @Path("lataa/{documentId}")
    Response lataa(String documentId);

    /**
     * Hakee listan kaikista dokumenteista
     * 
     * @return json kaikista taltioiduista dokumenteista
     */
    @GET
    @Path("hae")
    Response hae();

    /**
     * Hakee listan dokumenteista palvelunnimella ja dokumentin tyypilla.
     * Erottelu siksi etta tulevaisuudessa voidaan tehda yksilolliset
     * kaytto-oikeusvaatimukset palveluille.
     * 
     * @param serviceName
     *            viestintapalvelu
     * @param documentType
     *            hyvaksymiskirje
     * @return json kaikista taltioiduista dokumenteista
     */
    @GET
    @Path("hae/{serviceName}/{documentType}")
    Response hae(String serviceName, String documentType);

}
