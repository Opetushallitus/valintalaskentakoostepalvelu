package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintaperusteetValintakoeAsyncResource {

	Future<List<ValintakoeDTO>> haeValintakokeet(Collection<String> oids);

	Future<Map<String, List<ValintakoeDTO>>> haeValintakokeetHakukohteille(
			Collection<String> hakukohdeOids);
}
