package fi.vm.sade.valinta.kooste.external.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
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
	private final Gson gson;
	private final Consumer<T> callback;
	private final Consumer<Throwable> failureCallback;
	private final String palvelukutsu;
	private final String url;
	// private final Class<T> clazz;
	private final Type type;

	// public Callback(String url, String palvelukutsu, Consumer<T> callback,
	// Class<T> clazz) {
	// this(url, palvelukutsu, callback, (t -> LOG.error(
	// "Asynkroninen palvelukutsu epaonnistui: {}", t.getMessage())),
	// clazz,null);
	// }
	public Callback(String url, String palvelukutsu, Consumer<T> callback,
			Type type) {
		this(url, palvelukutsu, callback, (t -> LOG.error(
				"Asynkroninen palvelukutsu epaonnistui: {}", t.getMessage())),
				type);
	}

	public Callback(String url, String palvelukutsu, Consumer<T> callback,
			Consumer<Throwable> failureCallback, Type type) {
		this(new Gson(), url, palvelukutsu, callback, failureCallback, type);
	}
	public Callback(Gson gson, String url, String palvelukutsu, Consumer<T> callback,
					Consumer<Throwable> failureCallback, Type type) {
		this.callback = callback;
		this.failureCallback = failureCallback;
		this.palvelukutsu = palvelukutsu;
		this.url = url;
		// this.clazz = clazz;
		this.type = type;
		this.gson = gson;
	}
	private boolean isTextHtml(Response response) {
		try {
			return Optional.of(response.getMetadata().getFirst("Content-Type"))
					.orElse(new Object()).toString()
					.contains(MediaType.TEXT_HTML);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void completed(Response response) {
		String json = StringUtils.EMPTY;
		try {
			InputStream stream = (InputStream) response.getEntity();
			json = StringUtils.trimToEmpty(IOUtils.toString(stream));
			IOUtils.closeQuietly(stream);
			if (json.length() == 0) {
				LOG.error(
						"Paluuarvona saadun viestin pituus oli nolla merkkia palvelukutsulle {}{} (Response {} {})",
						url, palvelukutsu, response.getStatus(), response
								.getMetadata().getFirst("Content-Type"));
			}
			T t = gson.fromJson(json, type);
			try {
				callback.accept(t);
			} catch (Exception e) {
				LOG.error(
						"Asynkronisen kutsun ({}{}) paluuarvonkasittelija heitti poikkeuksen {}:\r\nRESPONSE {} ->\r\n{} {}",
						url, palvelukutsu, e.getMessage(),
						response.getStatus(),
						response.getMetadata().getFirst("Content-Type"),
						format(response, json));
				failureCallback.accept(e);
			}
		} catch (Exception e) {
			LOG.error(
					"Gson deserialisointi epaonnistui onnistuneelle asynkroniselle palvelin kutsulle ({}{}), {}:\r\nRESPONSE {} {} ->\r\n{}",
					url, palvelukutsu, e.getMessage(), response.getStatus(),
					response.getMetadata().getFirst("Content-Type"),
					format(response, json));
			try {
				failureCallback.accept(e);
			} catch (Exception ex) {
				LOG.error(
						"Asynkronisen kutsun ({}{}) epaonnistuneesta sarjallistuksesta seuranneelle virheenkasittelijakutsusta lensi poikkeus: {}:\r\nRESPONSE {} {} ->\r\n{}",
						url, palvelukutsu, ex.getMessage(),
						response.getStatus(),
						response.getMetadata().getFirst("Content-Type"),
						format(response, json));
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

	private String format(Response response, String json) {
		if (isTextHtml(response)) {
			CleanerProperties cp = new CleanerProperties();
			cp.setAddNewlineToHeadAndBody(false);
			cp.setOmitHtmlEnvelope(true);
			try {
				return new PrettyXmlSerializer(cp).getAsString(json);
			} catch (Exception ex) {
				return StringUtils.substring(json, 0, 250);
			}
		} else {
			return StringUtils.substring(json, 0, 250);
		}
	}
}
