package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValintalaskentaValintakoeAsyncResource {

	Future<List<ValintakoeOsallistuminenDTO>> haeOsallistumiset(
			Collection<String> hakemusOid);

	Future<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(
			String hakukohdeOid);
}
