package fi.vm.sade.valinta.cache.resource;

import java.io.InputStream;
import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import fi.vm.sade.valinta.cache.domain.MetaData;
import fi.vm.sade.valinta.cache.dto.DocumentDto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Portaali tilapaisdokumenteille
 */
@Path("cache")
public interface DocumentCacheResource {

    /**
     * Lataa dokumentin
     * 
     * @param documentId
     *            esim viestintapalvelu.hyvaksymiskirje.3241234ID
     * @return dokumentti
     */
    @GET
    @Path("lataa/{documentId}")
    InputStream lataa(String documentId);

    /**
     * Hakee listan kaikista dokumenteista
     * 
     * @return json kaikista taltioiduista dokumenteista
     */
    @GET
    @Path("hae")
    Collection<DocumentDto> hae();

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
    Collection<DocumentDto> hae(String serviceName, String documentType);

    @PUT
    @Path("tallenna")
    void tallenna(MetaData metaData, InputStream tiedosto);
}
