package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.exception.NoContentException;
import fi.vm.sade.valinta.kooste.viestintapalvelu.exception.NoReplyException;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         OLETTAA ETTA KAIKILLE VALINTATAPAJONOILLE TEHDAAN HYVAKSYMISKIRJE JOS
 *         HAKEMUS ON HYVAKSYTTY YHDESSAKIN!
 */
@Component("hyvaksyttyjenOsoitteetKomponentti")
public class HyvaksyttyjenOsoitteetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksyttyjenOsoitteetKomponentti.class);

    @Autowired
    private SijoitteluResource sijoitteluResource;

    @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}")
    private String sijoitteluResourceUrl;

    @Autowired
    private HakukohdeResource tarjontaResource;

    @Value("${valintalaskentakoostepalvelu.tarjonta.rest.url}")
    private String tarjontaResourceUrl;

    @Autowired
    private ApplicationResource applicationResource;

    @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}")
    private String hakuAppResourceUrl;

    // private static final String KIELIKOODI = "kieli_fi";

    public String teeHyvaksymiskirjeet(@Simple("${property.kielikoodi}") String kielikoodi,
            @Simple("${property.hakukohdeOid}") String hakukohdeOid, @Simple("${property.hakuOid}") String hakuOid,
            @Simple("${property.sijoitteluajoId}") Long sijoitteluajoId) {
        LOG.debug("Hyvaksymiskirjeet for hakukohde '{}' and haku '{}' and sijoitteluajo '{}'", new Object[] {
                hakukohdeOid, hakuOid, sijoitteluajoId });
        assert (hakukohdeOid != null);
        assert (hakuOid != null);
        assert (sijoitteluajoId != null);
        //
        //
        //
        final List<HakijaDTO> haunHakijat = sijoitteluResource.koulutuspaikalliset(hakuOid, sijoitteluajoId.toString());
        final List<HakijaDTO> hakukohteenHakijat = filterHakijatHakukohteelle(haunHakijat, hakukohdeOid);
        final int kaikkiHakukohteenHyvaksytyt = hakukohteenHakijat.size();
        if (kaikkiHakukohteenHyvaksytyt == 0) {
            LOG.error(
                    "Hyväksyttyjen osoitetarroja yritetään luoda hakukohteelle {} millä ei ole hyväksyttyjä hakijoita!",
                    hakukohdeOid);
            throw new NoContentException(
                    "Hakukohteella on oltava vähintään yksi hyväksytty hakija että hyväksyttyjen osoitetarrat voidaan luoda!");
        }
        final List<Osoite> osoitteet = new ArrayList<Osoite>();
        for (HakijaDTO hakija : hakukohteenHakijat) {
            final String hakemusOid = hakija.getHakemusOid();
            final Osoite osoite = haeOsoite(hakemusOid);
            osoitteet.add(osoite);
        }

        LOG.info("Yritetään luoda viestintapalvelulta osoitteita hyväksytyille hakijoille {} kappaletta!",
                osoitteet.size());
        return new Gson().toJson(new Osoitteet(osoitteet));
    }

    private List<HakijaDTO> filterHakijatHakukohteelle(List<HakijaDTO> haunHakijat, final String hakukohdeOid) {
        List<HakijaDTO> hakijat = new ArrayList<HakijaDTO>(haunHakijat);
        Collections2.filter(hakijat, new Predicate<HakijaDTO>() {
            public boolean apply(HakijaDTO hakija) {
                for (HakutoiveDTO toive : hakija.getHakutoiveet()) {
                    for (HakutoiveenValintatapajonoDTO jono : toive.getHakutoiveenValintatapajonot()) {
                        if (HakemuksenTila.HYVAKSYTTY.equals(jono.getTila())) {
                            if (hakukohdeOid.equals(toive.getHakukohdeOid())) {
                                return true; // hyvaksytty oikeaan kohteeseen
                            }
                            return false; // hyvaksytty muuhun kohteeseen
                        }
                    }
                }
                return false; // ei hakutoiveita
            }
        });
        return hakijat;
    }

    private Osoite haeOsoite(String hakemusOid) {
        try {
            return OsoiteHakemukseltaUtil.osoiteHakemuksesta(applicationResource.getApplicationByOid(hakemusOid));
        } catch (Exception e) {
            LOG.error("Ei voitu hakea osoitetta Haku-palvelusta hakemukselle {}! {}", new Object[] { hakemusOid,
                    hakuAppResourceUrl });
            throw new NoReplyException(
                    "Hakemuspalvelu ei anna hakijoille osoitteita! Tarkista palvelun käyttöoikeudet.");
        }
    }

}
