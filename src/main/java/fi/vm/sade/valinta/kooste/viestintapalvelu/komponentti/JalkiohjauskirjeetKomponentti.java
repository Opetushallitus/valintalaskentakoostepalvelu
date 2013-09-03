package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteHakemukseltaUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("jalkiohjauskirjeetKomponentti")
public class JalkiohjauskirjeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetKomponentti.class);

    @Autowired
    private ValintatietoService valintatietoService;

    public String teeJalkiohjauskirjeet(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.valintakoeOid}") List<String> valintakoeOids,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
        LOG.debug("Jalkiohjauskirjeet for hakukohde '{}' and valintakokeet '{}'",
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
        List<Kirje> kirjeet = new ArrayList<Kirje>();
        // List<Osoite> osoitteet = new ArrayList<Osoite>();
        for (HakemusTyyppi h : hakemukset) {
            if (osallistujienHakemusOidit.contains(h.getHakemusOid())) {
                kirjeet.add(new Kirje(OsoiteHakemukseltaUtil.osoiteHakemuksesta(h), "FI", Collections
                        .<Map<String, String>> emptyList()));
            }
        }

        String jalkiohjauskirjeet = new Gson().toJson(new Kirjeet(kirjeet));
        LOG.debug("Jalkiohjauskirjeet {}", jalkiohjauskirjeet);
        return jalkiohjauskirjeet;
    }
}
