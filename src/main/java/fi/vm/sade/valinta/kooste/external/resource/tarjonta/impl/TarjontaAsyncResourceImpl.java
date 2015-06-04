package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
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
		return this.<ResultV1RDTO<HakuV1RDTO>>getAsObservable("/v1/haku/" + hakuOid + "/", new TypeToken<ResultV1RDTO<HakuV1RDTO>>() {}.getType())
			.map(result -> result.getResult());
	}

	@Override
	public Observable<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid) {
		return this.<ResultV1RDTO<HakukohdeV1RDTO>>getAsObservable("/v1/hakukohde/" + hakukohdeOid + "/", new TypeToken<ResultV1RDTO<HakukohdeV1RDTO>>() {
		}.getType()).map(result -> result.getResult());
	}
}
