package fi.vm.sade.valinta.kooste.external.resource;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Arrays;
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
	public ResponseCallback(Consumer<Response> callback, Consumer<Throwable> failure) {
		this(true, callback, failure);
	}
	public ResponseCallback() {
		this.only2xxIsCompleted = false;
		this.callback = null;
		this.failure = null;
	}
	@Override
	public void completed(Response response) {
		try {
			LOG.info("Saatiin jotain !!! {} {} {}", response.getStatus(), callback, failure);
			if (callback != null && failure != null) {
				if (only2xxIsCompleted) {
					int status = response.getStatus();
					if (status >= 200 && status < 300) {
						callback.accept(response);
					} else {
						LOG.error("Expected status code 200-299 but got code {} instead: {}",response.getStatus(), entityToString(response.getEntity()));
						failure.accept(new RuntimeException(entityToString(response.getEntity())));
					}
				} else {
					callback.accept(response);
				}
			} else {
				LOG.info("Ohitettiin ilmoittaminen koska {} {}", callback, failure);
			}
		} catch(Throwable t) {
			LOG.info("Jotain meni pieleen onnistuneen responsen käsittelyssä");
			LOG.info("{} {}", t.getMessage(), Arrays.toString(t.getStackTrace()));
		} finally {
			LOG.info("Oltiin onnistuneessa käsittelyssä");
		}
	}

	@Override
	public void failed(Throwable throwable) {
		try {
			LOG.info("Ei saatu mitään !!!");
			if (callback != null && failure != null) {
				failure.accept(throwable);
			} else {
				LOG.info("Ohitettiin virheilmoittaminen koska {} {}", callback, failure);
			}
		} catch(Throwable t) {
			LOG.info("Jotain meni pieleen epäonnistuneen responsen käsittelyssä");
			LOG.info("{} {}", t.getMessage(), Arrays.toString(t.getStackTrace()));
		} finally {
			LOG.info("Oltiin käsittelyssä");
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
