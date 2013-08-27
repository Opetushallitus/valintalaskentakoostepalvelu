package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
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

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("haeOsallistuneetHakemuksilleKomponentti")
public class HaeOsallistuneetHakemuksilleKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeOsallistuneetHakemuksilleKomponentti.class);

    @Autowired
    private ValintatietoService valintatietoService;

    public String haeOsallistuneetHakemuksille(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.valintakoeOid}") List<String> valintakoeOids,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
        List<HakemusOsallistuminenTyyppi> tiedotHakukohteelle = new ArrayList<HakemusOsallistuminenTyyppi>();
        for (String valintakoeOid : valintakoeOids) {
            tiedotHakukohteelle.addAll(valintatietoService.haeValintatiedotHakukohteelle(hakukohdeOid, valintakoeOid));
        }
        Set<String> osallistujienHakemusOidit = new HashSet<String>();
        for (HakemusOsallistuminenTyyppi o : tiedotHakukohteelle) {
            if (Osallistuminen.OSALLISTUU.equals(o.getOsallistuminen())) {
                osallistujienHakemusOidit.add(o.getHakemusOid());
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
