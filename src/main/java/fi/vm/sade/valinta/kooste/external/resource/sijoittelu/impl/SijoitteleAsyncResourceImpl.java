package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.AsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;

/**
 * 
 * @author jussija
 *
 */
@Service
public class SijoitteleAsyncResourceImpl extends AsyncResource implements SijoitteleAsyncResource {
	@Autowired
	public SijoitteleAsyncResourceImpl(@Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}") String address) {
		super(address, TimeUnit.MINUTES.toMillis(50));
	}

	@Override
	public void sijoittele(String hakuOid, Consumer<String> callback, Consumer<Throwable> failureCallback) {
		String url = "/sijoittele/" + hakuOid+ "/";
		try {

			WebClient
					.fromClient(webClient)
					.path(url)
					.accept(MediaType.WILDCARD_TYPE)
					.async()
					.get(new Callback<String>(address, url, callback,
							failureCallback, new TypeToken<String>() {
							}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}
}
