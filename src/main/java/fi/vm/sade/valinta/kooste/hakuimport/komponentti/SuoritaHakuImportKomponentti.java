package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import java.util.Collection;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.OPH;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.46
 */
@Component("suoritaHakuImportKomponentti")
@PreAuthorize("isAuthenticated()")
public class SuoritaHakuImportKomponentti {

    private static final Logger LOG = LoggerFactory
            .getLogger(SuoritaHakuImportKomponentti.class);

    @Autowired
    private fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource hakuResource;

    private static final int MAX_COUNT = -1;

    public Collection<String> suoritaHakukohdeImport(
            @Property(OPH.HAKUOID) String hakuOid) {
        ResultV1RDTO<HakuV1RDTO> a = hakuResource.findByOid(hakuOid);// getByOIDHakukohde(hakuOid,
        // null,
        // MAX_COUNT,
        // 0,
        // null,
        // null,
        // null,
        // null);
        LOG.info("Importoidaan hakukohteita yhteensä kpl");

        Collection<String> hakukohdeOids = a.getResult().getHakukohdeOids();

        // Collection<String> hakukohdeOids =
        // Sets.newHashSet(Collections2.filter(
        // Collections2.transform(a, new Function<OidRDTO, String>() {
        // public String apply(OidRDTO input) {
        // }
        // }), Predicates.notNull()));

        return hakukohdeOids;
    }
}
