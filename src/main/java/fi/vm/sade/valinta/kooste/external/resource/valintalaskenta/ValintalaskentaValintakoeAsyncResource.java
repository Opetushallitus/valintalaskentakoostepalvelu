package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;

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

	Peruutettava haeHakutoiveelle(
			String hakukohdeOid,
			Consumer<List<ValintakoeOsallistuminenDTO>> callback,
			Consumer<Throwable> failureCallback);

	Peruutettava haeValintatiedotHakukohteelle(String hakukohdeOid,
											   List<String> valintakoeTunnisteet,
											   Consumer<List<HakemusOsallistuminenDTO>> callback, Consumer<Throwable> failureCallback);

}
