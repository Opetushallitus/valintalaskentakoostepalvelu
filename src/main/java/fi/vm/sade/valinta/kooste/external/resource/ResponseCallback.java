package fi.vm.sade.valinta.kooste.external.resource;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ResponseCallback implements InvocationCallback<Response> {
	private final static Logger LOG = LoggerFactory
			.getLogger(ResponseCallback.class);

	private final boolean only2xxIsCompleted;
	private final Consumer<Response> callback;
	private final Consumer<Throwable> failure;

	public ResponseCallback(boolean only2xxIsCompleted, Consumer<Response> callback, Consumer<Throwable> failure) {
		this.callback = callback;
		this.failure = failure;
		this.only2xxIsCompleted = only2xxIsCompleted;
	}
	public ResponseCallback() {
		this.only2xxIsCompleted = false;
		this.callback = null;
		this.failure = null;
	}
	@Override
	public void completed(Response response) {
		if(callback != null && failure != null) {
			if(only2xxIsCompleted) {
				int status = response.getStatus();
				if(200 >= status && 300 < status) {
					callback.accept(response);
				} else {
					failure.accept(new RuntimeException(entityToString(response.getEntity())));
				}
			} else {
				callback.accept(response);
			}
		} else {
			LOG.error("Ohitettiin ilmoittaminen koska {} {}", callback, failure);
		}
	}

	@Override
	public void failed(Throwable throwable) {
		if(callback != null && failure != null) {
			failure.accept(throwable);
		} else {
			LOG.error("Ohitettiin virheilmoittaminen koska {} {}", callback, failure);
		}
	}

	private String entityToString(Object entity){
		if(entity == null) {
			return "Palvelin ei antanut virheelle syytä";
		} else if(entity instanceof InputStream){
			try {
				return IOUtils.toString((InputStream) entity);
			} catch(Exception e) {
				return "Palvelin virhettä ei pystytty lukemaan";
			}
		} else {
			return entity.toString();
		}
	}
}
