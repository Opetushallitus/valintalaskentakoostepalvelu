package fi.vm.sade.valinta.kooste.hakuimport.route;

import static fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;

import java.util.concurrent.Future;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;

public interface HakukohdeImportRoute {
    final String DIRECT_HAKUKOHDE_IMPORT = "direct:hakuimport_tarjonnasta_koostepalvelulle";

    Future<Void> asyncAktivoiHakukohdeImport(
            @Body String hakukohdeOid,
            @Property(PROPERTY_VALVOMO_PROSESSI) HakuImportProsessi prosessi,
            @Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
