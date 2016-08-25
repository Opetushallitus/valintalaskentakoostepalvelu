package fi.vm.sade.valinta.kooste.hakuimport.route;

import java.util.concurrent.Future;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

public interface HakuImportRoute {
    final String DIRECT_HAKU_IMPORT = "direct:hakuimport";

    void aktivoiHakuImport(@Property(OPH.HAKUOID) String hakuOid);

    Future<Void> asyncAktivoiHakuImport(@Property(OPH.HAKUOID) String hakuOid);
}
