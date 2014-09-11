package fi.vm.sade.valinta.kooste.external.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.function.Consumer;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Callback<T> implements InvocationCallback<Response> {
	private final static Logger LOG = LoggerFactory.getLogger(Callback.class);
	private final Type type = new TypeToken<T>() {
	}.getType();
	private final Gson gson = new Gson();
	private final Consumer<T> callback;
	private final Consumer<Throwable> failureCallback;
	private final String palvelukutsu;
	private final String url;

	public Callback(String url, String palvelukutsu, Consumer<T> callback) {
		this(url, palvelukutsu, callback, (t -> LOG.error(
				"Asynkroninen palvelukutsu epaonnistui: {}", t.getMessage())));
	}

	public Callback(String url, String palvelukutsu, Consumer<T> callback,
			Consumer<Throwable> failureCallback) {
		this.callback = callback;
		this.failureCallback = failureCallback;
		this.palvelukutsu = palvelukutsu;
		this.url = url;
	}

	@Override
	public void completed(Response response) {
		String json = StringUtils.EMPTY;
		try {
			InputStream stream = (InputStream) response.getEntity();
			json = IOUtils.toString(stream);
			IOUtils.closeQuietly(stream);
			T t = gson.fromJson(json, type);
			try {
				callback.accept(t);
			} catch (Exception e) {
				LOG.error(
						"Asynkronisen kutsun ({}{}) paluuarvonkasittelija heitti poikkeuksen {}:\r\npaluuarvo->\r\n{}",
						url, palvelukutsu, e.getMessage(),
						StringUtils.substring(json, 0, 250));
				failureCallback.accept(e);
			}
		} catch (Exception e) {
			LOG.error(
					"Gson deserialisointi epaonnistui onnistuneelle asynkroniselle palvelin kutsulle ({}{}), {}:\r\npaluuarvo->\r\n{}",
					url, palvelukutsu, e.getMessage(),
					StringUtils.substring(json, 0, 250));
			try {
				failureCallback.accept(e);
			} catch (Exception ex) {
				LOG.error(
						"Asynkronisen kutsun ({}{}) epaonnistuneesta sarjallistuksesta seuranneelle virheenkasittelijakutsusta lensi poikkeus: {}",
						url, palvelukutsu, ex.getMessage(),
						StringUtils.substring(json, 0, 250));
			}
		}
	}

	@Override
	public void failed(Throwable throwable) {
		try {
			failureCallback.accept(throwable);
		} catch (Exception ex) {
			LOG.error(
					"Epaonnistuneen asynkronisen kutsun ({}{}) virheenkasittelija heitti poikkeuksen: {}",
					url, palvelukutsu, ex.getMessage());
		}
	}
}
