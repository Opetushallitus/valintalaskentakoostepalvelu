package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import com.google.gson.*;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintalaskentaAsyncResourceImpl extends HttpResource implements ValintalaskentaAsyncResource {
	private static final Gson GSON= new GsonBuilder()
			.registerTypeAdapter(Date.class, new JsonDeserializer() {
				@Override
				public Object deserialize(JsonElement json, Type typeOfT,
										  JsonDeserializationContext context)
						throws JsonParseException {
					return new Date(json.getAsJsonPrimitive().getAsLong());
				}
			})
			.create();
	@Autowired
	public ValintalaskentaAsyncResourceImpl(
			@Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}") String address
	) {
		super(address, TimeUnit.HOURS.toMillis(20));
	}

	@Override
	public Peruutettava laskennantulokset(String hakuOid, String hakukohdeOid, Consumer<List<ValintatietoValinnanvaiheDTO>> callback, Consumer<Throwable> failureCallback) {
		try {
			///valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
			String url = new StringBuilder("/valintalaskentakoostepalvelu/hakukohde/").append(hakukohdeOid).append("/valinnanvaihe").toString();
			return new PeruutettavaImpl(getWebClient()
					.path(url)
					.async()
					.get(new Callback<List<ValintatietoValinnanvaiheDTO>>(GSON,address, url, callback,
							failureCallback, new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {
					}.getType())));
		} catch (Exception e) {
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
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

}
