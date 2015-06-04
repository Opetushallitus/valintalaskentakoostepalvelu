package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.Callback;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintalaskentaAsyncResourceImpl extends HttpResource implements ValintalaskentaAsyncResource {
	private final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaAsyncResourceImpl.class);
	@Autowired
	public ValintalaskentaAsyncResourceImpl(
			@Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}") String address
	) {
		super(address, TimeUnit.HOURS.toMillis(20));
	}

	@Override
	public Observable<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid) {
		return getAsObservable("/valintalaskentakoostepalvelu/hakukohde/" + hakukohdeOid + "/valinnanvaihe", new GenericType<List<ValintatietoValinnanvaiheDTO>>() { }.getType());
	}

	@Override
	public Peruutettava lisaaTuloksia(String hakuOid, String hakukohdeOid, String tarjoajaOid, ValinnanvaiheDTO vaihe, Consumer<ValinnanvaiheDTO> callback, Consumer<Throwable> failureCallback) {
		try {
			///valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
			String url = new StringBuilder("/valintalaskentakoostepalvelu/hakukohde/").append(hakukohdeOid).append("/valinnanvaihe").toString();
			return new PeruutettavaImpl(getWebClient()
					.path(url)
					.query("tarjoajaOid", tarjoajaOid)
					.async()
					.post(Entity.entity(vaihe,
							MediaType.APPLICATION_JSON_TYPE), new Callback<ValinnanvaiheDTO>(GSON, address, url, callback,
							failureCallback, new TypeToken<ValinnanvaiheDTO>() {
					}.getType())));
		} catch (Throwable e) {
			LOG.error("Valintalaskentaan tulosten tuonti epäonnistui: {} {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	@Override
	public Peruutettava laske(LaskeDTO laskeDTO, Consumer<String> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = "/valintalaskenta/laske";
			return new PeruutettavaImpl(getWebClient()
					.path(url)
					.async()
					.post(Entity.entity(laskeDTO,
							MediaType.APPLICATION_JSON_TYPE),
							new Callback<String>(address, url, callback,
									failureCallback, new TypeToken<String>() {
									}.getType())));
		} catch (Exception e) {
			LOG.error("Virhe laskennan kutsussa",e);
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}

	}

	@Override
	public Peruutettava laskeKaikki(LaskeDTO laskeDTO,
			Consumer<String> callback, Consumer<Throwable> failureCallback) {
		try {
			String url = "/valintalaskenta/laskekaikki";
			return new PeruutettavaImpl(getWebClient()
					.path(url)
					.async()
					.post(Entity.entity(laskeDTO,
									MediaType.APPLICATION_JSON_TYPE),
							new Callback<String>(address, url, callback,
									failureCallback, new TypeToken<String>() {
							}.getType())));
		} catch (Exception e) {
			LOG.error("Virhe laske kaikki kutsussa",e);
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	@Override
	public Peruutettava laskeJaSijoittele(List<LaskeDTO> lista,
			Consumer<String> callback, Consumer<Throwable> failureCallback) {
		try {
			String url = "/valintalaskenta/laskejasijoittele";
			return new PeruutettavaImpl(
					getWebClient()
							.path(url)
							.async()
							.post(Entity.entity(lista,
									MediaType.APPLICATION_JSON_TYPE),
									new Callback<String>(address, url,
											callback, failureCallback,
											new TypeToken<String>() {
											}.getType())));
		} catch (Exception e) {
			LOG.error("Virhe laske ja sijoittele kutsussa",e);
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}


	@Override
	public Peruutettava valintakokeet(LaskeDTO laskeDTO,
			Consumer<String> callback, Consumer<Throwable> failureCallback) {
		try {
			String url = "/valintalaskenta/valintakokeet";
			return new PeruutettavaImpl(getWebClient()
					.path(url)
					.async()
					.post(Entity.entity(laskeDTO,
							MediaType.APPLICATION_JSON_TYPE),
							new Callback<String>(address, url, callback,
									failureCallback, new TypeToken<String>() {
									}.getType())));
		} catch (Exception e) {
			LOG.error("Virhe valintakoe kutsussa",e);
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

}
