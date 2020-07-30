package fi.vm.sade.valinta.kooste.hakuimport.route;

import fi.vm.sade.valinta.kooste.OPH;
import java.util.concurrent.Future;
import org.apache.camel.Property;

public interface HakuImportRoute {
  final String DIRECT_HAKU_IMPORT = "direct:hakuimport";

  void aktivoiHakuImport(@Property(OPH.HAKUOID) String hakuOid);

  Future<Void> asyncAktivoiHakuImport(@Property(OPH.HAKUOID) String hakuOid);
}
