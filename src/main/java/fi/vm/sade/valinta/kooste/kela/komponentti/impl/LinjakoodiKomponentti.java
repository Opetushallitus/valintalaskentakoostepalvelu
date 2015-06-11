package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.valinta.kooste.exception.KoodistoException;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;

/**
 *         Use proxy instead of calling bean:hakukohdeTarjonnaltaKomponentti!
 *         Proxy provides retries!
 */
@Component
public class LinjakoodiKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(LinjakoodiKomponentti.class);

    private final KoodiService koodiService;

    @Autowired
    public LinjakoodiKomponentti(KoodiService koodiService) {
        this.koodiService = koodiService;
    }

    public String haeLinjakoodi(String hakukohdeNimiUri) {
        String koodiUri = TarjontaUriToKoodistoUtil.cleanUri(hakukohdeNimiUri);
        Integer koodiVersio = TarjontaUriToKoodistoUtil.stripVersion(hakukohdeNimiUri);
        SearchKoodisCriteriaType koodistoHaku = TarjontaUriToKoodistoUtil.toSearchCriteria(koodiUri, koodiVersio);

        List<KoodiType> koodiTypes = koodiService.searchKoodis(koodistoHaku);
        if (koodiTypes.isEmpty()) {
            throw new KoodistoException("Koodisto palautti tyhjän koodijoukon urille " + koodiUri + " ja käytetylle versiolle " + koodiVersio);
        }
        for (KoodiType koodi : koodiTypes) {
            String arvo = koodi.getKoodiArvo();
            if (arvo != null) {
                if (arvo.length() == 3) {
                    return arvo;
                } else {
                    LOG.error("Koodistosta palautui virheellinen arvo {} uri:lle {}, versio {}", new Object[]{arvo, koodiUri, koodiVersio});
                }
            } else {
                LOG.error("Koodistosta palautui null arvo uri:lle {}, versio {}", new Object[]{koodiUri, koodiVersio});
            }
        }
        throw new KoodistoException("Koodistosta ei saatu arvoa urille " + koodiUri + " ja käytetylle versiolle " + koodiVersio);
    }
}
