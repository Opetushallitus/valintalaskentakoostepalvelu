package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.Collection;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response;

import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;

public interface TilaAsyncResource {

	Response tuoErillishaunTilat(String hakuOid, String hakukohdeOid, String valintatapajononNimi, Collection<ErillishaunHakijaDTO> erillishaunHakijat);
}
