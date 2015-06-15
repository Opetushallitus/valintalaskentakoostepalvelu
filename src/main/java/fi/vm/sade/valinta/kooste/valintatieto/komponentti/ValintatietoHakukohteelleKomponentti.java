package fi.vm.sade.valinta.kooste.valintatieto.komponentti;

import java.util.Arrays;
import java.util.List;

import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintatietoResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("valintatietoHakukohteelleKomponentti")
public class ValintatietoHakukohteelleKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(ValintatietoHakukohteelleKomponentti.class);
    private ValintatietoResource valintatietoService;

    @Autowired
    public ValintatietoHakukohteelleKomponentti(@Qualifier("ValintatietoRestClient") ValintatietoResource valintatietoService) {
        this.valintatietoService = valintatietoService;
    }

    public List<HakemusOsallistuminenDTO> valintatiedotHakukohteelle(@Property("valintakoeOid") List<String> valintakoeOids, @Property("hakukohdeOid") String hakukohdeOid) {
        List<HakemusOsallistuminenDTO> osallistujat = valintatietoService.haeValintatiedotHakukohteelle(hakukohdeOid, valintakoeOids);
        if (osallistujat == null || osallistujat.isEmpty()) {
            String oids = null;
            if (osallistujat != null) {
                oids = Arrays.toString(osallistujat.toArray());
            }
            LOG.error("Osallistumistietoja ei saatu hakukohteelle({}), kun valintakokeet[{}] oli kyseessa!", new Object[]{hakukohdeOid, oids});
            throw new RuntimeException("ValintatietoService palautti tyhjan osallistujajoukon.");
        }
        return osallistujat;
    }
}
