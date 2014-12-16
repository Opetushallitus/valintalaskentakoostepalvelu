package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

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
		return WebClient.fromClient(webClient)
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async().get(HakuV1RDTO.class);
	}

	@Override
	public Future<HakukohdeDTO> haeHakukohde(String hakukohdeOid) {
		String url = "/hakukohde/"+hakukohdeOid+"/";
		return WebClient.fromClient(webClient)
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async().get(HakukohdeDTO.class);
	}
}
