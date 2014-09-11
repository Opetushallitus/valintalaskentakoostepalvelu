package fi.vm.sade.valinta.kooste.external.resource;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ResponseCallback implements InvocationCallback<Response> {
	private final static Logger LOG = LoggerFactory
			.getLogger(ResponseCallback.class);

	@Override
	public void completed(Response response) {
		LOG.info("Asynkroninen kutsu onnistui: {}", response.getStatus());
	}

	@Override
	public void failed(Throwable throwable) {
		LOG.info("Asynkroninen kutsu epaonnistui: {}", throwable.getMessage());
	}
}
