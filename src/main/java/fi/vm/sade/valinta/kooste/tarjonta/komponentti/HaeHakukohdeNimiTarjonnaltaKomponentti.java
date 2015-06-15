package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.exception.TarjontaException;

/**
 * Use proxy instead of calling bean:hakukohdeTarjonnaltaKomponentti!
 * Proxy provides retries!
 */
@Component("hakukohdeNimiTarjonnaltaKomponentti")
public class HaeHakukohdeNimiTarjonnaltaKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(HaeHakukohdeNimiTarjonnaltaKomponentti.class);
    private HakukohdeResource tarjontaResource;
    private String tarjontaResourceUrl;

    @Autowired
    public HaeHakukohdeNimiTarjonnaltaKomponentti(HakukohdeResource tarjontaResource, @Value("${valintalaskentakoostepalvelu.tarjonta.rest.url}") String tarjontaResourceUrl) {
        this.tarjontaResource = tarjontaResource;
        this.tarjontaResourceUrl = tarjontaResourceUrl;
    }

    public HakukohdeDTO haeHakukohdeNimi(@Property("hakukohdeOid") String hakukohdeOid) {
        if (hakukohdeOid == null) {
            throw new SijoittelupalveluException("Sijoittelu palautti puutteellisesti luodun hakutoiveen! Hakukohteen tunniste puuttuu!");
        } else {
            LOG.debug("Yhteys {}, HakukohdeResource.getHakukohdeNimi({})", new Object[]{tarjontaResourceUrl, hakukohdeOid});
            try {
                HakukohdeDTO nimi = tarjontaResource.getByOID(hakukohdeOid);
                return nimi;
            } catch (Exception e) {
                throw new TarjontaException("Tarjonnasta ei löydy hakukohdetta " + hakukohdeOid);
            }
        }
    }
}
