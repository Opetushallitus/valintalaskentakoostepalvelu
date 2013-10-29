package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.haku.HakemusProxy;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

@Component
public class HaeOsoiteKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeOsoiteKomponentti.class);

    @Autowired
    private HakemusProxy hakemusProxy;

    @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}")
    private String applicationResourceUrl;

    public Osoite haeOsoite(String hakemusOid) {
        try {
            LOG.info("Haetaan hakemus {}/applications/{}", new Object[] { applicationResourceUrl, hakemusOid });
            Hakemus hakemus = hakemusProxy.haeHakemus(hakemusOid);
            if (hakemus == null) {
                LOG.error("Hakemus {}/applications/{} null-arvo!", new Object[] { applicationResourceUrl, hakemusOid, });
            }
            return OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakemus);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Hakemus {}/applications/{} sisälsi virheellistä tietoa!", new Object[] { applicationResourceUrl,
                    hakemusOid, });
            // throw new
            // HakemuspalveluException("Hakemuspalvelu ei anna hakemusta " +
            // hakemusOid + "!");
        }
        return OsoiteHakemukseltaUtil.osoiteHakemuksesta(null);
    }

}
