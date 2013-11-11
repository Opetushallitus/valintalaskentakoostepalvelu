package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.haku.HakemusProxy;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.ViestintapalveluMessageProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HaeOsoiteKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeOsoiteKomponentti.class);

    @Autowired
    private HakemusProxy hakemusProxy;

    @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}")
    private String applicationResourceUrl;

    @Autowired
    private ViestintapalveluMessageProxy messageProxy;

    private void notFound(String hakemusOid) {
        try {
            messageProxy.message("Haku-palvelusta ei löytynyt hakemusta oid:lla " + hakemusOid);
        } catch (Exception ex) {
            LOG.error("Viestintäpalvelun message rajapinta ei ole käytettävissä! Hakemusta {} ei löydy!", hakemusOid);
        }
    }

    public Osoite haeOsoite(String hakemusOid) {
        try {
            LOG.info("Haetaan hakemus {}/applications/{}", new Object[] { applicationResourceUrl, hakemusOid });
            Hakemus hakemus = hakemusProxy.haeHakemus(hakemusOid);
            if (hakemus == null) {
                notFound(hakemusOid);
                LOG.error("Hakemus {}/applications/{} null-arvo!", new Object[] { applicationResourceUrl, hakemusOid, });
            }
            return OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakemus);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Hakemus {}/applications/{} sisälsi virheellistä tietoa!", new Object[] { applicationResourceUrl,
                    hakemusOid, });
            notFound(hakemusOid);
            // throw new
            // HakemuspalveluException("Hakemuspalvelu ei anna hakemusta " +
            // hakemusOid + "!");
        }
        return OsoiteHakemukseltaUtil.osoiteHakemuksesta(null);
    }

}
