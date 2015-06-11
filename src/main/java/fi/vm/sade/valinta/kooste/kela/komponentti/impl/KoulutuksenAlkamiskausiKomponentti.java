package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import static fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil.toSearchCriteria;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KoodiType;

@Component
public class KoulutuksenAlkamiskausiKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(KoulutuksenAlkamiskausiKomponentti.class);

    @Autowired
    private KoodiService koodiService;

    public int koulutuksenAlkamiskausiUri(String koulutuksenAlkamiskausiUri) {
        for (KoodiType koodi : koodiService
                .searchKoodis(toSearchCriteria(koulutuksenAlkamiskausiUri))) {
            if ("S".equals(StringUtils.upperCase(koodi.getKoodiArvo()))) { // syksy
                return 8;
            } else if ("K".equals(StringUtils.upperCase(koodi.getKoodiArvo()))) { // kevat
                return 1;
            } else {
                LOG.error("Viallinen arvo {}, koodilla {} ", new Object[]{
                        koodi.getKoodiArvo(), koulutuksenAlkamiskausiUri});
                break;
            }
        }
        throw new RuntimeException("Koodistosta ei saatu alkamiskautta URI:lle " + koulutuksenAlkamiskausiUri);
    }
}
