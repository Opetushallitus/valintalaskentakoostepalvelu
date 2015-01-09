package fi.vm.sade.valinta.kooste;

import fi.vm.sade.valinta.kooste.exception.OrganisaatioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;

/**
 * Created by jussija on 09/01/15.
 */
public class OrganisaatioOikeuksienTarkistus {

    private static final Logger LOG = LoggerFactory.getLogger(OrganisaatioOikeuksienTarkistus.class);
    public static final String VALINTOJENTOTEUTTAMINEN = "APP_HAKEMUS_CRUD_";

    public static String kayttajanKayttooikeudet() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication.getAuthorities().stream().filter(i -> i.getAuthority().contains(VALINTOJENTOTEUTTAMINEN))
                    .findFirst().get().getAuthority().replaceFirst(VALINTOJENTOTEUTTAMINEN,"");
        }catch(Exception e) {
            LOG.error("Käyttöoikeustarkistus epäonnistui! Käyttäjällä ei ole hakukohteen muokkaukseen tarvittavia oikeuksia! {}", e.getMessage(),
                    Arrays.toString(e.getStackTrace()));
            throw new OrganisaatioException("Käyttöoikeustarkistus epäonnistui! Käyttäjällä ei ole hakukohteen muokkaukseen tarvittavia oikeuksia!");
        }
    }
    public static boolean tarkistaKayttooikeudet(String organisaationOidit) {
        return tarkistaKayttooikeudet(kayttajanKayttooikeudet(),organisaationOidit);
    }

    public static boolean tarkistaKayttooikeudet(String kayttooikeus, String organisaationOidit) {
        return kayttooikeus != null && !kayttooikeus.isEmpty() && organisaationOidit.contains(kayttooikeus);
    }

}
