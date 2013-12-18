package fi.vm.sade.valinta.kooste.hakuimport.route;

import java.util.concurrent.Future;

import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.24
 */
public interface HakuImportRoute {

    final String DIRECT_HAKU_IMPORT = "direct:hakuimport";

    void aktivoiHakuImport(@Property(OPH.HAKUOID) String hakuOid);

    Future<Void> asyncAktivoiHakuImport(@Property(OPH.HAKUOID) String hakuOid,
            @Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
