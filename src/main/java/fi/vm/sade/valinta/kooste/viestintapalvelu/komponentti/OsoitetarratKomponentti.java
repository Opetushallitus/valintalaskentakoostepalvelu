package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.gson.Gson;
import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Jussi Jartamo
 */
@Component("osoitetarratKomponentti")
public class OsoitetarratKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(OsoitetarratKomponentti.class);

    @Autowired
    private ValintatietoService valintatietoService;

    @Autowired
    private ApplicationResource applicationResource;

    public String teeOsoitetarrat(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
                                  @Simple("${property.valintakoeOid}") List<String> valintakoeOids,
                                  @Simple("${property.hakemukset}") List<SuppeaHakemus> hakemukset) {
        LOG.debug("Osoitetarrat for hakukohde '{}' and valintakokeet '{}'",
                new Object[]{hakukohdeOid, Arrays.toString(valintakoeOids.toArray())});
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
                osoitteet.add(OsoiteHakemukseltaUtil.osoiteHakemuksesta(applicationResource.getApplicationByOid(h.getOid())));
            }
        }

        String osoitetarrat = new Gson().toJson(new Osoitteet(osoitteet));
        LOG.debug("Osoitetarrat {}", osoitetarrat);
        return osoitetarrat;
    }
}
