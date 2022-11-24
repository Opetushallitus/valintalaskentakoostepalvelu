package fi.vm.sade.valinta.kooste.hakuimport.route;

import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import java.util.concurrent.Future;
import org.springframework.security.core.Authentication;

public interface HakukohdeImportRoute {

  Future<?> asyncAktivoiHakukohdeImport(
      String hakukohdeOid, HakuImportProsessi prosessi, Authentication auth);
}
