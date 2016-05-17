package fi.vm.sade.valinta.kooste.external.resource;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class TestCallback {

	private static final Logger LOG = LoggerFactory
			.getLogger(TestCallback.class);

	@Test
	public void testListOfHakemuksetCallback() throws IOException {
		GsonResponseCallback<List<Hakemus>> cb = new GsonResponseCallback<List<Hakemus>>("", "",
				obj -> {
					LOG.error("SUCCESS {}", obj);
				}, poikkeus -> {
					LOG.error("POIKKEUS {}", poikkeus);
				}, new TypeToken<List<Hakemus>>() {
				}.getType());

		cb.completed(javax.ws.rs.core.Response.ok(
				new ClassPathResource("listfull.json").getInputStream())
				.build());
	}

	@Test
	public void testLaskentaDtoCallback() throws IOException {

		GsonResponseCallback<LaskentaDto> cb2 = new GsonResponseCallback<LaskentaDto>("", "", obj -> {
			LOG.error("SUCCESS {}", obj);
		}, poikkeus -> {
			LOG.error("POIKKEUS {}", poikkeus);
		}, new TypeToken<LaskentaDto>() {
		}.getType());
		cb2.completed(javax.ws.rs.core.Response.ok(
				new ClassPathResource("resetoi.json").getInputStream()).build());
	}
}
