package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import java.util.Collection;
import java.util.List;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.tarjonta.service.resources.HakuResource;
import fi.vm.sade.tarjonta.service.resources.dto.OidRDTO;
import fi.vm.sade.valinta.kooste.OPH;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.46
 */
@Component("suoritaHakuImportKomponentti")
@PreAuthorize("isAuthenticated()")
public class SuoritaHakuImportKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaHakuImportKomponentti.class);

    @Autowired
    private ValintaperusteService valintaperusteService;

    @Autowired
    private HakuResource hakuResource;

    private static final int MAX_COUNT = 100000;

    public Collection<String> suoritaHakukohdeImport(@Property(OPH.HAKUOID) String hakuOid) {
        List<OidRDTO> a = hakuResource.getByOIDHakukohde(hakuOid, null, MAX_COUNT, 0, null, null, null, null);
        LOG.info("Importoidaan hakukohteita yhteensä {} kpl", a.size());

        Collection<String> hakukohdeOids = Collections2.filter(
                Collections2.transform(a, new Function<OidRDTO, String>() {
                    public String apply(OidRDTO input) {
                        return input.getOid();
                    }
                }), Predicates.notNull());
        if (hakukohdeOids.size() != a.size()) {
            LOG.error("Tarjonnasta palautui null hakukohdeoideja haulla {}", hakuOid);
        }
        return hakukohdeOids;
    }
}
