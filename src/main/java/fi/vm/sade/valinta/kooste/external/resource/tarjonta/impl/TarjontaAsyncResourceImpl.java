package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import rx.Observable;

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
	public Observable<HakuV1RDTO> haeHaku(String hakuOid) {
		return getAsObservable("/v1/haku/"+hakuOid+"/", HakuV1RDTO.class);
	}

	@Override
	public Observable<HakukohdeDTO> haeHakukohde(String hakukohdeOid) {
		return getAsObservable("/hakukohde/" + hakukohdeOid + "/", HakukohdeDTO.class);
	}

	@Override
	public Peruutettava haeHaku(String hakuOid, Consumer<HakuV1RDTO> callback, Consumer<Throwable> failureCallback) {
		String url = "/v1/haku/"+hakuOid+"/";
		return new PeruutettavaImpl(getWebClient()
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.get(new Callback<ResultV1RDTO<HakuV1RDTO>>(GSON, address, url,
						result -> callback.accept(result.getResult()),
						failureCallback, new TypeToken<ResultV1RDTO<HakuV1RDTO>>() {}.getType())));
	}
}
