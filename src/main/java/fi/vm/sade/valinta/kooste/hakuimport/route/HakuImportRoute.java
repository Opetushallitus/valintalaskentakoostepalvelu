package fi.vm.sade.valinta.kooste.hakuimport.route;

import java.util.concurrent.Future;

public interface HakuImportRoute {

  Future<?> asyncAktivoiHakuImport(String hakuOid);
}
