package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import static fi.vm.sade.koodisto.service.types.SearchKoodisVersioSelectionType.LATEST;
import static fi.vm.sade.koodisto.service.types.SearchKoodisVersioSelectionType.SPECIFIC;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.exception.KoodistoException;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.exception.TarjontaException;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Use proxy instead of calling bean:hakukohdeTarjonnaltaKomponentti!
 *         Proxy provides retries!
 */
@Component("linjakoodiKomponentti")
public class LinjakoodiKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(LinjakoodiKomponentti.class);

    @Autowired
    private HakukohdeResource tarjontaResource;

    @Value("${valintalaskentakoostepalvelu.tarjonta.rest.url}")
    private String tarjontaResourceUrl;

    @Autowired
    private KoodiService koodiService;

    public String haeLinjakoodi(@Property("hakukohdeOid") String hakukohdeOid) {
        if (hakukohdeOid == null) {
            throw new SijoittelupalveluException(
                    "Sijoittelu palautti puutteellisesti luodun hakutoiveen! Hakukohteen tunniste puuttuu!");
        } else {
            LOG.debug("Yhteys {}, HakukohdeResource.getHakukohdeNimi({})", new Object[] { tarjontaResourceUrl,
                    hakukohdeOid });
            HakukohdeDTO dto;
            try {
                dto = tarjontaResource.getByOID(hakukohdeOid);
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Ei hakukohdetta {} tarjonnassa!", hakukohdeOid);
                throw new TarjontaException("Tarjonnasta ei löydy hakukohdetta " + hakukohdeOid);
            }
            String uri = dto.getHakukohdeNimiUri();
            if (uri == null) {
                LOG.error("Hakukohteen {} uri oli null!", hakukohdeOid);
                throw new TarjontaException("Tarjonnalla ei ollut hakukohteelle " + hakukohdeOid
                        + " hakukohteenNimiUri:a!");
            }
            SearchKoodisCriteriaType koodistoHaku = new SearchKoodisCriteriaType();
            if (uri.contains("#")) {
                String[] puolikkaat = uri.split("#");
                try {
                    int versio = Integer.parseInt(puolikkaat[1]);
                    koodistoHaku.setKoodiVersio(versio);
                    koodistoHaku.setKoodiVersioSelection(SPECIFIC);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("Versionumeroa ei voitu parsia uri:sta {}", uri);
                    koodistoHaku.setKoodiVersioSelection(LATEST);
                }
                String todellinenUri = puolikkaat[0];
                koodistoHaku.getKoodiUris().add(todellinenUri); // hakukohteet_654#1
            } else {
                koodistoHaku.getKoodiUris().add(uri);
                koodistoHaku.setKoodiVersioSelection(LATEST);
            }
            try {
                for (KoodiType koodi : koodiService.searchKoodis(koodistoHaku)) {
                    String arvo = koodi.getKoodiArvo();
                    if (arvo != null) {
                        if (arvo.length() == 3) {
                            return arvo;
                        } else {
                            LOG.error("Koodistosta palautui virheellinen arvo {} uri:lle {}",
                                    new Object[] { arvo, uri });
                        }
                    } else {
                        LOG.error("Koodistosta palautui null arvo uri:lle {}", new Object[] { uri });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Koodistosta ei saatu arvoa urille {}: Virhe {}", new Object[] { uri, e.getMessage() });
            }
            throw new KoodistoException("Koodistosta ei saatu arvoa urille " + uri);
        }
    }
}
