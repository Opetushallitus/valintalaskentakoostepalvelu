package fi.vm.sade.valinta.kooste.hakuimport.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.24
 */
public interface HakuImportRoute {

    final String DIRECT_HAKU_IMPORT = "direct:hakuimport";

    void aktivoiHakuImport(@Property(OPH.HAKUOID) String hakuOid);
}
