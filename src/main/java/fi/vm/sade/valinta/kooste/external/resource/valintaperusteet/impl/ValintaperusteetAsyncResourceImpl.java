package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ValintaperusteetAsyncResourceImpl implements
		ValintaperusteetAsyncResource {
	private final WebClient webClient;
	private final String address;
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintaperusteetAsyncResourceImpl.class);

	@Autowired
	public ValintaperusteetAsyncResourceImpl(
			@Value("${host.ilb}") String address
	//
	) {
		this.address = address;
		JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
		bean.setAddress(address);
		bean.setThreadSafe(true);
		List<Object> providers = Lists.newArrayList();
		providers
				.add(new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider());
		providers.add(new fi.vm.sade.valinta.kooste.ObjectMapperProvider());
		bean.setProviders(providers);
		this.webClient = bean.createWebClient();
	}



	// /valintaperusteet/hakijaryhmä/{hakukohdeoid}
	public Peruutettava haeHakijaryhmat(String hakukohdeOid,
			Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback,
			Consumer<Throwable> failureCallback) {
		try {

			StringBuilder urlBuilder = new StringBuilder().append(
					"/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/hakijaryhma/").append(
					hakukohdeOid);
			String url = urlBuilder.toString();
			return new PeruutettavaImpl(
					WebClient.fromClient(webClient)
							.path(url)
							//

							.accept(MediaType.APPLICATION_JSON_TYPE)
							//
							.async()
							.get(new Callback<List<ValintaperusteetHakijaryhmaDTO>>(
									address,
									url,
									callback,
									failureCallback,
									new TypeToken<List<ValintaperusteetHakijaryhmaDTO>>() {
									}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
	public Peruutettava haeValinnanvaiheetHakukohteelle(String hakukohdeOid,
											Consumer<List<ValinnanVaiheJonoillaDTO>> callback,
											Consumer<Throwable> failureCallback) {
		LOG.info("Valinnanvaiheiden haku...");
		try {
			String url = new StringBuilder()
					.append("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/")
					.append(hakukohdeOid).append("/valinnanvaihe").toString();

			WebClient wc = WebClient.fromClient(webClient).path(url);
			return new PeruutettavaImpl(wc
					//
					.accept(MediaType.APPLICATION_JSON_TYPE)
							//
					.async()
					.get(new Callback<List<ValinnanVaiheJonoillaDTO>>(address, url, callback,
							failureCallback,
							new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {
							}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
	public Peruutettava haeValintaperusteet(String hakukohdeOid,
			Integer valinnanVaiheJarjestysluku,
			Consumer<List<ValintaperusteetDTO>> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder()
					.append("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/")
					.append(hakukohdeOid).toString();

			WebClient wc = WebClient.fromClient(webClient).path(url);
			if (valinnanVaiheJarjestysluku != null) {
				wc.query("vaihe", valinnanVaiheJarjestysluku);
			}
			return new PeruutettavaImpl(wc
			//
					.accept(MediaType.APPLICATION_JSON_TYPE)
					//
					.async()
					.get(new Callback<List<ValintaperusteetDTO>>(address, url
							+ "?vaihe=" + valinnanVaiheJarjestysluku, callback,
							failureCallback,
							new TypeToken<List<ValintaperusteetDTO>>() {
							}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}

	public Peruutettava haunHakukohteet(String hakuOid,
			Consumer<List<HakukohdeViiteDTO>> callback,
			Consumer<Throwable> failureCallback) {
		try {
			String url = new StringBuilder()
					.append("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/haku/")
					.append(hakuOid).toString();
			return new PeruutettavaImpl(WebClient
					.fromClient(webClient)
					.path(url)
					//
					.accept(MediaType.APPLICATION_JSON_TYPE)
					//
					.async()
					.get(new Callback<List<HakukohdeViiteDTO>>(address, url,
							callback, failureCallback,
							new TypeToken<List<HakukohdeViiteDTO>>() {
							}.getType())));
		} catch (Exception e) {
			failureCallback.accept(e);
			return TyhjaPeruutettava.tyhjaPeruutettava();
		}
	}
	
	@Override
	public Future<List<ValinnanVaiheJonoillaDTO>> ilmanLaskentaa(String oid) {

		StringBuilder urlBuilder = new StringBuilder().append(
				"/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/").append(oid).append("/ilmanlaskentaa/");
		String url = urlBuilder.toString();
		
		return WebClient
				.fromClient(webClient)
				.path(url)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				.get(new GenericType<List<ValinnanVaiheJonoillaDTO>>() {
				});
	}
	
	@Override
	public Future<Response> tuoHakukohde(HakukohdeImportDTO hakukohde) {
//
		StringBuilder urlBuilder = new StringBuilder().append(
				"/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/tuoHakukohde/");
		String url = urlBuilder.toString();
		return WebClient
				.fromClient(webClient)
				.path(url)
				//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async()
				.post(Entity.entity(hakukohde,
						MediaType.APPLICATION_JSON_TYPE));
	}
	
	@Override
	public Future<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid) {
		String url = new StringBuilder()
				.append("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/")
				.append(hakukohdeOid).append("/").toString();
		return WebClient.fromClient(webClient).path(url)
		//
				.accept(MediaType.APPLICATION_JSON_TYPE)
				//
				.async().get(new GenericType<List<ValintaperusteDTO>>() {
				});
	}
	

	@Override
	public Future<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(
			Collection<String> hakukohdeOids) {
		StringBuilder urlBuilder = new StringBuilder()
				.append("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/valintakoe");
		String url = urlBuilder.toString();
		// LOG.error("POST {}\r\n{}", url,
		// Arrays.toString(hakukohdeOids.toArray()));
		return WebClient
				.fromClient(webClient)
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.post(Entity.entity(hakukohdeOids,
						MediaType.APPLICATION_JSON_TYPE),
						new GenericType<List<HakukohdeJaValintakoeDTO>>() {
						});
	}

	@Override
	public Future<List<ValintakoeDTO>> haeValintakokeet(Collection<String> oids) {
		StringBuilder urlBuilder = new StringBuilder()
				.append("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintakoe/");
		String url = urlBuilder.toString();
		return WebClient
				.fromClient(webClient)
				.path(url)
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.async()
				.post(Entity.entity(oids, MediaType.APPLICATION_JSON_TYPE),
						new GenericType<List<ValintakoeDTO>>() {
						});
	}

}
