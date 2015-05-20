package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import java.util.List;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.valintalaskenta.resource.LaskentaParams;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface LaskentaSeurantaAsyncResource {

	void otaSeuraavaLaskentaTyonAlle(Consumer<String> uuidCallback, Consumer<Throwable> failureCallback);

	void laskenta(String uuid, Consumer<LaskentaDto> callback, Consumer<Throwable> failureCallback);

	void resetoiTilat(String uuid, Consumer<LaskentaDto> callback, Consumer<Throwable> failureCallback);

	void luoLaskenta(
			LaskentaParams laskentaParams,
			List<HakukohdeDto> hakukohdeOids,
			Consumer<String> callback,
			Consumer<Throwable> failureCallback);

	void lisaaIlmoitusHakukohteelle(String uuid, String hakukohdeOid, IlmoitusDto ilmoitus);

	void merkkaaHakukohteenTila(String uuid, String hakukohdeOid, HakukohdeTila tila);

	void merkkaaLaskennanTila(String uuid, LaskentaTila tila, HakukohdeTila hakukohdetila);

	void merkkaaLaskennanTila(String uuid, LaskentaTila tila);
}
