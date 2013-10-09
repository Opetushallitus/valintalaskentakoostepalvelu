package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.exception.HakemuspalveluException;
import fi.vm.sade.valinta.kooste.exception.ViestintapalveluException;
import fi.vm.sade.valinta.kooste.viestintapalvelu.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;

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
    private HaeOsoiteKomponentti osoiteKomponentti;
    @Autowired
    private ViestintapalveluResource viestintapalvelu;

    // private static final String KIELIKOODI = "kieli_fi";

    public Object teeHyvaksymiskirjeet(@Simple("${property.kielikoodi}") String kielikoodi,
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
        final Collection<HakijaDTO> hakukohteenHakijat = sijoitteluResource.koulutuspaikalliset(hakuOid, hakukohdeOid,
                sijoitteluajoId.toString());
        final int kaikkiHakukohteenHyvaksytyt = hakukohteenHakijat.size();
        if (kaikkiHakukohteenHyvaksytyt == 0) {
            LOG.error(
                    "Hyväksyttyjen osoitetarroja yritetään luoda hakukohteelle {} millä ei ole hyväksyttyjä hakijoita!",
                    hakukohdeOid);
            throw new HakemuspalveluException(
                    "Hakukohteella on oltava vähintään yksi hyväksytty hakija että hyväksyttyjen osoitetarrat voidaan luoda!");
        }
        final List<Osoite> osoitteet = new ArrayList<Osoite>();
        for (HakijaDTO hakija : hakukohteenHakijat) {
            final String hakemusOid = hakija.getHakemusOid();
            final Osoite osoite = osoiteKomponentti.haeOsoite(hakemusOid);
            osoitteet.add(osoite);
        }

        LOG.info("Yritetään luoda viestintapalvelulta osoitteita hyväksytyille hakijoille {} kappaletta!",
                osoitteet.size());
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
