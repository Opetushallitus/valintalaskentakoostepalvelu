package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class TarjontaAsyncResourceImpl extends HttpResource implements TarjontaAsyncResource {
	@Autowired
	public TarjontaAsyncResourceImpl(@Value("${valintalaskentakoostepalvelu.tarjonta.rest.url}") String address) {
		super(address, TimeUnit.MINUTES.toMillis(5));
	}

	@Override
	public Future<HakuV1RDTO> haeHaku(String hakuOid) {
		String url = "/v1/haku/"+hakuOid+"/";
		return getWebClient()
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async().get(HakuV1RDTO.class);
	}

	@Override
	public Future<HakukohdeDTO> haeHakukohde(String hakukohdeOid) {
		String url = "/hakukohde/"+hakukohdeOid+"/";
		return getWebClient()
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async().get(HakukohdeDTO.class);
	}
	@Override
	public Peruutettava haeHaku(String hakuOid, Consumer<HakuV1RDTO> callback, Consumer<Throwable> failureCallback) {
		String url = "/v1/haku/"+hakuOid+"/";
		return new PeruutettavaImpl(getWebClient()
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async().get(new Callback<HakuV1RDTO>(GSON,address, url, callback,
						failureCallback, new TypeToken<HakuV1RDTO>() {
				}.getType())));
	}
	public Peruutettava haeHakukohde(String hakuOid, String hakukohdeOid, Consumer<HakukohdeDTO> callback, Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder("/hakukohde/").append(hakukohdeOid).append("/").toString();
			return new PeruutettavaImpl(getWebClient()
					.path(url)
					.accept(MediaType.APPLICATION_JSON_TYPE)
					.async()
					.get(new Callback<HakukohdeDTO>(GSON,address, url, callback,
							failureCallback, new TypeToken<HakukohdeDTO>() {
					}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
}
