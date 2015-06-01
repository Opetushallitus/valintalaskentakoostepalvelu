package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import fi.vm.sade.valinta.http.Callback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintalaskentaValintakoeAsyncResourceImpl extends AsyncResourceWithCas implements
		ValintalaskentaValintakoeAsyncResource {

	// Ohitetaan päivämäärät tarpeettomana tietona
	private static final Gson GSON = new GsonBuilder()
	.registerTypeAdapter(Date.class, new JsonDeserializer() {
		@Override
		public Object deserialize(JsonElement json,
				Type typeOfT, JsonDeserializationContext context)
		throws JsonParseException {
			return null;
		}

	}).create();

	@Autowired
	public ValintalaskentaValintakoeAsyncResourceImpl(
			//
			@Value("${web.url.cas}") String webCasUrl,
			//
			@Value("${cas.service.valintalaskenta-service}/j_spring_cas_security_check") String targetService,
			//
			@Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
			//
			@Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
			@Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}") String address,ApplicationContext context
	//
	) {
		super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.HOURS.toMillis(1));
	}

	@Override
	public Future<List<ValintakoeOsallistuminenDTO>> haeOsallistumiset(
			Collection<String> hakemusOids) {
		StringBuilder urlBuilder = new StringBuilder()
				.append("/valintakoe/hakemus/");
		String url = urlBuilder.toString();
		return getWebClient()
				//
				.path(url)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				.post(Entity.entity(hakemusOids,
						MediaType.APPLICATION_JSON_TYPE),
						new GenericType<List<ValintakoeOsallistuminenDTO>>() {
						});
	}

	@Override
	public Future<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(
			String hakukohdeOid) {
		// valintalaskentakoostepalvelu.valintalaskenta.rest.url
		// /valintalaskenta-laskenta-service/resources/valintakoe/hakutoive/...
		StringBuilder urlBuilder = new StringBuilder().append(
				"/valintakoe/hakutoive/").append(hakukohdeOid);
		String url = urlBuilder.toString();
		return getWebClient()
				//
				.path(url)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				.get(new GenericType<List<ValintakoeOsallistuminenDTO>>() {
				});
	}

	@Override
	public Peruutettava haeHakutoiveelle(
			String hakukohdeOid,
			Consumer<List<ValintakoeOsallistuminenDTO>> callback,
			Consumer<Throwable> failureCallback) {
		// valintalaskentakoostepalvelu.valintalaskenta.rest.url
		// /valintalaskenta-laskenta-service/resources/valintakoe/hakutoive/...
		StringBuilder urlBuilder = new StringBuilder().append(
				"/valintakoe/hakutoive/").append(hakukohdeOid);
		String url = urlBuilder.toString();
		return new PeruutettavaImpl(getWebClient()
				//
				.path(url)
						//
				.accept(MediaType.APPLICATION_JSON_TYPE)
						//
				.async()
				.get(new Callback<List<ValintakoeOsallistuminenDTO>>(
						GSON,
						address,
						url,
						callback,
						failureCallback,
						new TypeToken<List<ValintakoeOsallistuminenDTO>>() { }.getType())));
	}

	@Override
	public Peruutettava haeValintatiedotHakukohteelle(String hakukohdeOid,
													  List<String> valintakoeTunnisteet,
													  Consumer<List<HakemusOsallistuminenDTO>> callback, Consumer<Throwable> failureCallback) {
		try {
			// valintatieto/hakukohde/{hakukohdeOid}
			String url = new StringBuilder("/valintatieto/hakukohde/").append(hakukohdeOid).toString();
			return new PeruutettavaImpl(getWebClient()
					.path(url)
					.async()
					.post(Entity.entity(valintakoeTunnisteet,
							MediaType.APPLICATION_JSON_TYPE), new Callback<List<HakemusOsallistuminenDTO>>(GSON, address, url, callback,
							failureCallback, new TypeToken<List<HakemusOsallistuminenDTO>>() {
					}.getType())));
		} catch (Throwable e) {
			LOG.error("Hae valintatiedot hakukohteelle epäonnistui:", e);
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
}
