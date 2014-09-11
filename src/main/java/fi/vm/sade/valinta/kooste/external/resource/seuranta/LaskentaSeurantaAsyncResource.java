package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import fi.vm.sade.valinta.seuranta.dto.YhteenvetoDto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface LaskentaSeurantaAsyncResource {

	void laskenta(String uuid, Consumer<LaskentaDto> callback,
			Consumer<Throwable> failureCallback);

	void resetoiTilat(String uuid, Consumer<LaskentaDto> callback,
			Consumer<Throwable> failureCallback);

	void luoLaskenta(String hakuOid, LaskentaTyyppi tyyppi,
			Integer valinnanvaihe, Boolean valintakoelaskenta,
			List<String> hakukohdeOids, Consumer<String> callback,
			Consumer<Throwable> failureCallback);

	void haeAsync(String hakuOid, Consumer<Collection<YhteenvetoDto>> callback);

	void lisaaIlmoitusHakukohteelle(String uuid, String hakukohdeOid,
			IlmoitusDto ilmoitus);

	void merkkaaHakukohteenTila(String uuid, String hakukohdeOid,
			HakukohdeTila tila);

	void merkkaaLaskennanTila(String uuid, LaskentaTila tila);
}
