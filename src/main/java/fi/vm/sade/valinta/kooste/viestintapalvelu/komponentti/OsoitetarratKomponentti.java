package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("osoitetarratKomponentti")
public class OsoitetarratKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(OsoitetarratKomponentti.class);

    @Autowired
    private ValintatietoService valintatietoService;

    public String teeOsoitetarrat(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.valintakoeOid}") List<String> valintakoeOids,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
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
        for (HakemusTyyppi h : hakemukset) {
            if (osallistujienHakemusOidit.contains(h.getHakemusOid())) {
                osoitteet.add(OsoiteHakemukseltaUtil.osoiteHakemuksesta(h));
            }
        }

        String osoitetarrat = new Gson().toJson(new Osoitteet(osoitteet));
        LOG.debug("Osoitetarrat {}", osoitetarrat);
        return osoitetarrat;
    }
}
