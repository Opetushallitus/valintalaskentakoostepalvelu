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

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.exception.ViestintapalveluException;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.proxy.ValintatietoHakukohteelleProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.ViestintapalveluOsoitetarratProxy;

/**
 * @author Jussi Jartamo
 */
@Component("osoitetarratKomponentti")
public class OsoitetarratKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(OsoitetarratKomponentti.class);

    @Autowired
    private ValintatietoHakukohteelleProxy valintatietoProxy;

    @Autowired
    private HaeOsoiteKomponentti osoiteKomponentti;

    @Autowired
    private ViestintapalveluOsoitetarratProxy viestintapalveluProxy;

    public Object teeOsoitetarrat(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.valintakoeOid}") List<String> valintakoeOids,
            @Simple("${property.hakemukset}") List<SuppeaHakemus> hakemukset) {
        LOG.debug("Osoitetarrat for hakukohde '{}' and valintakokeet '{}'",
                new Object[] { hakukohdeOid, Arrays.toString(valintakoeOids.toArray()) });
        List<HakemusOsallistuminenTyyppi> tiedotHakukohteelle = valintatietoProxy.haeValintatiedotHakukohteelle(
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
        Osoitteet viesti = new Osoitteet(osoitteet);
        LOG.debug("\r\n{}", new ViestiWrapper(viesti));
        Response response = viestintapalveluProxy.haeOsoitetarrat(viesti);
        return response.getEntity();
    }
}
