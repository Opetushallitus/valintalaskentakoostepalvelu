package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.exception.ViestintapalveluException;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;

/**
 * @author Jussi Jartamo
 */
@Component("osoitetarratKomponentti")
public class OsoitetarratKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(OsoitetarratKomponentti.class);

    @Autowired
    private ValintatietoService valintatietoService;

    @Autowired
    private HaeOsoiteKomponentti osoiteKomponentti;

    @Autowired
    private ViestintapalveluResource viestintapalvelu;

    public Object teeOsoitetarrat(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.valintakoeOid}") List<String> valintakoeOids,
            @Simple("${property.hakemukset}") List<SuppeaHakemus> hakemukset) {
        LOG.debug("Osoitetarrat for hakukohde '{}' and valintakokeet '{}'",
                new Object[] { hakukohdeOid, Arrays.toString(valintakoeOids.toArray()) });
        List<HakemusOsallistuminenTyyppi> tiedotHakukohteelle = valintatietoService.haeValintatiedotHakukohteelle(
                valintakoeOids, hakukohdeOid);

        Set<String> osallistujienHakemusOidit = new HashSet<String>();
        //
        // Haetaan vain johonkin kokeeseen osallistuneet hakemukset!
        //
        for (HakemusOsallistuminenTyyppi o : tiedotHakukohteelle) {
            for (ValintakoeOsallistuminenTyyppi o1 : o.getOsallistumiset()) {
                if (Osallistuminen.OSALLISTUU.equals(o1.getOsallistuminen())) {
                    osallistujienHakemusOidit.add(o.getHakemusOid());
                }
            }
        }
        List<Osoite> osoitteet = new ArrayList<Osoite>();
        for (SuppeaHakemus h : hakemukset) {
            if (osallistujienHakemusOidit.contains(h.getOid())) {
                osoitteet.add(osoiteKomponentti.haeOsoite(h.getOid()));
            }
        }
        if (osoitteet.isEmpty()) {
            throw new ViestintapalveluException("Yritetään luoda nolla kappaletta osoitetarroja!");
        }
        LOG.debug("Luodaan {}kpl osoitetarroja!", osoitteet.size());
        Response response = viestintapalvelu.haeOsoitetarrat(new Osoitteet(osoitteet));
        LOG.debug("Status {} \r\n {} \r\n {}", new Object[] { response.getStatus() });
        if (response.getStatus() == 302) { // FOUND
            throw new ViestintapalveluException("Sinulla ei ole käyttöoikeuksia viestintäpalveluun!");
        }
        if (response.getStatus() != Response.Status.ACCEPTED.getStatusCode()) {
            throw new ViestintapalveluException(
                    "Viestintäpalvelu epäonnistui osoitetarrojen luonnissa. Yritä uudelleen tai ota yhteyttä ylläpitoon!");
        }
        return response.getEntity();
    }
}
