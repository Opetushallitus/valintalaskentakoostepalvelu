package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.Collection;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response;

import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;

public interface TilaAsyncResource {

	Future<Response> tuoErillishaunTilat(
			String hakuOid, String hakukohdeOid,
			Collection<ErillishaunHakijaDTO> erillishaunHakijat);
}
