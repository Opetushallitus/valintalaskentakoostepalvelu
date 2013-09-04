package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluajoResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemuksenTila;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Valintatapajono;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("hyvaksymiskirjeetKomponentti")
public class HyvaksymiskirjeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKomponentti.class);

    @Autowired
    private SijoitteluajoResource sijoitteluAjoResource;

    public String teeHyvaksymiskirjeet(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.hakuOid}") String hakuOid, @Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
        LOG.debug("Hyvaksymiskirjeet for hakukohde '{}' and haku '{}'", new Object[] { hakukohdeOid, hakuOid });

        Hakukohde hakukohde = sijoitteluAjoResource.getHakukohdeBySijoitteluajo(sijoitteluajoId, hakukohdeOid);
        LOG.debug("Hakukohde {}", hakukohde);

        // List<Osoite> osoitteet = new ArrayList<Osoite>();
        /*
         * for (HakemusTyyppi h : hakemukset) { if
         * (osallistujienHakemusOidit.contains(h.getHakemusOid())) {
         * kirjeet.add(new Kirje(OsoiteHakemukseltaUtil.osoiteHakemuksesta(h),
         * "FI", "Testikoulu", "Testikoulutus", Collections.<Map<String,
         * String>> emptyList())); } }
         */
        // ESITIEDOT
        // addressLabel: Object
        // koulu: "Pohjois-Karjalan ammattiopisto Lieksa"
        // koulutus: "Varaosamyynnin koulutusohjelma"
        // languageCode: "FI"
        List<Kirje> kirjeet = new ArrayList<Kirje>();

        for (Valintatapajono jono : hakukohde.getValintatapajonot()) {
            for (Hakemus hakemus : jono.getHakemukset()) {
                if (HakemuksenTila.HYVAKSYTTY.equals(hakemus.getTila())) {
                    Osoite o = new Osoite(hakemus.getEtunimi(), hakemus.getSukunimi(), "Jokutie1", "", "", "00500",
                            "City", "Region", "Country", "FI");
                    Map<String, String> tulokset = new HashMap<String, String>();
                    tulokset.put("alinHyvaksyttyPistemaara", "42");
                    tulokset.put("hakukohteenNimi", "Varaosamyynnin koulutusohjelma");
                    tulokset.put("hylkayksenSyy", "");
                    tulokset.put("hyvaksytyt", "11");
                    tulokset.put("kaikkiHakeneet", "11");
                    tulokset.put("omatPisteet", "11");

                    tulokset.put("oppilaitoksenNimi", "lukio");
                    tulokset.put("organisaationNimi", "Pohjois-Karjalan ammattiopisto Lieksa");
                    tulokset.put("paasyJaSoveltuvuuskoe", "15");
                    tulokset.put("selite", "Hyväksytty");
                    tulokset.put("valinnanTulos", "Hyväksytty");
                    kirjeet.add(new Kirje(o, "FI", Arrays.asList(tulokset)));
                }
            }
        }
        String hyvaksymiskirjeet = new Gson().toJson(new Kirjeet(kirjeet));
        LOG.debug("Hyvaksymiskirjeet {}", hyvaksymiskirjeet);
        return hyvaksymiskirjeet;
    }
}
