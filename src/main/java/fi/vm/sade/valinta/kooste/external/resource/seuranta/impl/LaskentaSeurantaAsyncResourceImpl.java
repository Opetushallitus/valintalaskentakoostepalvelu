package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.ResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class LaskentaSeurantaAsyncResourceImpl extends HttpResource implements LaskentaSeurantaAsyncResource {
	private final Gson gson = new Gson();
	private final ResponseCallback responseCallback = new ResponseCallback();

	@Autowired
	public LaskentaSeurantaAsyncResourceImpl(@Value("${host.ilb}") String address) {
		super(address, TimeUnit.HOURS.toMillis(1));
	}

	public void laskenta(String uuid, Consumer<LaskentaDto> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + uuid;
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.get(new Callback<LaskentaDto>(address, url, callback,
							failureCallback, new TypeToken<LaskentaDto>() {
							}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

	public void resetoiTilat(String uuid, Consumer<LaskentaDto> callback, Consumer<Throwable> failureCallback) {
		try {
			String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/"+uuid+"/resetoi";
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.put(Entity.entity(uuid, MediaType.APPLICATION_JSON_TYPE),
							new Callback<LaskentaDto>(address, url, callback,
									failureCallback,
									new TypeToken<LaskentaDto>() {
									}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

	public void luoLaskenta(String hakuOid, LaskentaTyyppi tyyppi,
			Boolean erillishaku,
			Integer valinnanvaihe, Boolean valintakoelaskenta,
			List<HakukohdeDto> hakukohdeOids, Consumer<String> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/"+hakuOid+"/tyyppi/"+tyyppi;
			WebClient wc = WebClient.fromClient(webClient).path(url);
			if (erillishaku != null) {
				wc.query("erillishaku", erillishaku);
			}
			if (valinnanvaihe != null) {
				wc.query("valinnanvaihe", valinnanvaihe);
			}
			if (valintakoelaskenta != null) {
				wc.query("valintakoelaskenta", valintakoelaskenta);
			}
			wc.async()
					.post(Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE), new Callback<String>(address, url, callback, failureCallback, new TypeToken<String>() {
						}.getType()));
		} catch (Exception e) {
			failureCallback.accept(e);
		}
	}

	public void merkkaaLaskennanTila(String uuid, LaskentaTila tila) {
		String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/"+uuid+"/tila/"+tila;
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.put(Entity.entity(tila, MediaType.APPLICATION_JSON_TYPE),
							responseCallback);
		} catch (Exception e) {
			LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url,
					e.getMessage());
		}
	}

	public void merkkaaLaskennanTila(String uuid, LaskentaTila tila,
			HakukohdeTila hakukohdetila) {
		String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/"+uuid+"/tila/"+tila+"/hakukohde/"+hakukohdetila;
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.put(Entity.entity(tila, MediaType.APPLICATION_JSON_TYPE),
							responseCallback);
		} catch (Exception e) {
			LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url,
					e.getMessage());
		}
	}

	@Override
	public void merkkaaHakukohteenTila(String uuid, String hakukohdeOid,
			HakukohdeTila tila) {
		String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/"+uuid+"/hakukohde/"+hakukohdeOid+"/tila/"+tila;
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.put(Entity.entity(tila, MediaType.APPLICATION_JSON_TYPE),
							responseCallback);
		} catch (Exception e) {
			LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url,
					e.getMessage());
		}
	}

	public void lisaaIlmoitusHakukohteelle(String uuid, String hakukohdeOid,
			IlmoitusDto ilmoitus) {
		String url = "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/"+uuid+"/hakukohde/"+hakukohdeOid;
		try {
			WebClient
					.fromClient(webClient)
					.path(url)
					.async()
					.post(Entity.entity(gson.toJson(ilmoitus),
							MediaType.APPLICATION_JSON_TYPE), responseCallback);
		} catch (Exception e) {
			LOG.error("Seurantapalvelun kutsu {} paatyi virheeseen: {}", url,
					e.getMessage());
		}
	}

}
