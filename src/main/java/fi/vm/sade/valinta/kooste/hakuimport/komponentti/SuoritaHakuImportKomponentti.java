package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.tarjonta.service.resources.HakuResource;
import fi.vm.sade.tarjonta.service.resources.dto.OidRDTO;
import org.apache.camel.Body;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

import static fi.vm.sade.valinta.kooste.security.SecurityPreprocessor.SECURITY_CONTEXT_HEADER;

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

    @Autowired
    private SuoritaHakukohdeImportKomponentti suoritaHakukohdeImportKomponentti;

    private static final int MAX_COUNT = 100000;

    public void suoritaHakukohdeImport(@Property(SECURITY_CONTEXT_HEADER) Authentication auth, @Body String hakuOid) {
        assert (auth != null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        List<OidRDTO> a = hakuResource.getByOIDHakukohde(hakuOid, null, MAX_COUNT, 0, null, null, null, null);
        LOG.info("Importoidaan hakukohteita yhteensä {} kpl", a.size());

        for (OidRDTO o : a) {
            suoritaHakukohdeImportKomponentti.suoritaHakukohdeImport(auth, o.getOid());
        }
    }
}
