package fi.vm.sade.valinta.kooste.hakuimport;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1ResourceWrapper;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeHakutulosV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakutuloksetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakuImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.route.impl.HakuImportRouteImpl;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

public class HakuImportTest extends CamelTestSupport {
	private final static Logger LOG = LoggerFactory
			.getLogger(HakuImportTest.class);
	@Produce(uri = "direct:hakuimport")
	protected ProducerTemplate template;

	@Ignore
	@Test
	public void testData() throws JsonSyntaxException, IOException {
		// ObjectMapper mapper = new ObjectMapper(); // can reuse, share
		// globally
		// HakukohdeValintaperusteetV1RDTO obj = mapper.readValue(new File(
		// "user.json"), User.class);
		HakukohdeValintaperusteetV1RDTO obj = DateDeserializer.gsonBuilder().create()
				.fromJson(
						IOUtils.toString(new ClassPathResource(
								"hakukohdeimport/data2/1.2.246.562.20.27059719875.json")
								.getInputStream()),
						HakukohdeValintaperusteetV1RDTO.class);
		LOG.error("\r\n###\r\n### {}\r\n###", obj);
		LOG.error("\r\n###\r\n### {}\r\n###", new GsonBuilder()
				.setPrettyPrinting().create().toJson(obj));

	}

	@Test
	public void testRoute() {

		template.send(new Processor() {

			@Override
			public void process(Exchange exchange) {
				exchange.setProperty("hakuOid", "hakuOid");
				exchange.setProperty(
						ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI,
						new HakuImportProsessi("", ""));
				exchange.getIn().setBody("hakuOid");
			}
			// )BodyAndProperty(new Object(), "hakuOid", "hakuOid");

		});
	}

	protected RouteBuilder createRouteBuilder() {
		final String VIALLINEN_HAKUKOHDE = "throws";
		PropertyPlaceholderDelegateRegistry registry = (PropertyPlaceholderDelegateRegistry) context()
				.getRegistry();
		JndiRegistry jndiRegistry = (JndiRegistry) registry.getRegistry();
		jndiRegistry.bind("hakuImportValvomo",
				Mockito.mock(ValvomoAdminService.class));
		SuoritaHakuImportKomponentti suoritaHakuImportKomponentti = new SuoritaHakuImportKomponentti() {
			@Override
			public Collection<String> suoritaHakukohdeImport(String hakuOid) {
				return Arrays.asList(VIALLINEN_HAKUKOHDE,
						"1.2.246.562.20.27059719875", VIALLINEN_HAKUKOHDE,
						"1.2.246.562.20.27059719875");
			}
		};
		ValintaperusteetAsyncResource  valintaperusteetRestResource = Mockito
				.mock(ValintaperusteetAsyncResource.class);
		HakukohdeV1ResourceWrapper w = new HakukohdeV1ResourceWrapper() {
			@Override
			public ResultV1RDTO<HakukohdeValintaperusteetV1RDTO> findValintaperusteetByOid(
					String oid) {
				if (VIALLINEN_HAKUKOHDE.equals(oid)) {
					HakukohdeValintaperusteetV1RDTO obj = new HakukohdeValintaperusteetV1RDTO();
					return new ResultV1RDTO<HakukohdeValintaperusteetV1RDTO>(
							obj);
				}
				try {

					HakukohdeValintaperusteetV1RDTO obj = new GsonBuilder()
							.registerTypeAdapter(Date.class,
									new JsonDeserializer() {
										@Override
										public Object deserialize(
												JsonElement json,
												Type typeOfT,
												JsonDeserializationContext context)
												throws JsonParseException {
											return new Date(json
													.getAsJsonPrimitive()
													.getAsLong());
										}
									})
							.create()
							.fromJson(
									IOUtils.toString(new ClassPathResource(
											"hakukohdeimport/data2/1.2.246.562.20.27059719875.json")
											.getInputStream()),
									HakukohdeValintaperusteetV1RDTO.class);
					return new ResultV1RDTO<HakukohdeValintaperusteetV1RDTO>(
							obj);

				} catch (Exception e) {

				}
				return null;
			}

			@Override
			public ResultV1RDTO<HakutuloksetV1RDTO<HakukohdeHakutulosV1RDTO>> search(
					String hakuOid, List<String> hakukohdeTilas) {
				return null;
			}
		};

		SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti = new SuoritaHakukohdeImportKomponentti(
				w);

		return new HakuImportRouteImpl(1, 1, suoritaHakuImportKomponentti,
				valintaperusteetRestResource,
				tarjontaJaKoodistoHakukohteenHakuKomponentti);
	}
}
