package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;

public interface SijoitteluAsyncResource {

	Future<HakijaPaginationObject> getKoulutuspaikkallisetHakijat(
			String hakuOid, String hakukohdeOid);
}
