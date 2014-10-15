package fi.vm.sade.valinta.kooste.hakuimport;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1ResourceWrapper;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeHakutulosV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakutuloksetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
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
		ResultV1RDTO<HakukohdeValintaperusteetV1RDTO> obj = new Gson()
				.fromJson(
						IOUtils.toString(new ClassPathResource(
								"hakukohdeimport/data2/1.2.246.562.20.27059719875.json")
								.getInputStream()),
						new TypeToken<HakukohdeValintaperusteetV1RDTO>() {
						}.getType());
		LOG.error("\r\n###\r\n### {}\r\n###", obj.getResult());

	}

	@Ignore
	@Test
	public void testRoute() {

		template.send(new Processor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.setProperty("hakuOid", "hakuOid");
				exchange.setProperty(
						ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI,
						new HakuImportProsessi("", ""));
				exchange.getIn().setBody("hakuOid");
			}
			// )BodyAndProperty(new Object(), "hakuOid", "hakuOid");

		});
	}

	protected RouteBuilder createRouteBuilder() throws Exception {
		PropertyPlaceholderDelegateRegistry registry = (PropertyPlaceholderDelegateRegistry) context()
				.getRegistry();
		JndiRegistry jndiRegistry = (JndiRegistry) registry.getRegistry();
		jndiRegistry.bind("hakuImportValvomo",
				Mockito.mock(ValvomoAdminService.class));
		SuoritaHakuImportKomponentti suoritaHakuImportKomponentti = new SuoritaHakuImportKomponentti() {
			@Override
			public Collection<String> suoritaHakukohdeImport(String hakuOid) {
				return Arrays.asList("1.2.246.562.20.27059719875");
			}
		};
		ValintaperusteetRestResource valintaperusteetRestResource = Mockito
				.mock(ValintaperusteetRestResource.class);
		HakukohdeV1ResourceWrapper w = new HakukohdeV1ResourceWrapper() {
			@Override
			public ResultV1RDTO<HakukohdeValintaperusteetV1RDTO> findValintaperusteetByOid(
					String oid) {
				try {
					System.err.println("JSON");
					return new Gson()
							.fromJson(
									IOUtils.toString(new ClassPathResource(
											"hakukohdeimport/data2/valintaperusteet-1.2.246.562.20.27059719875.json")
											.getInputStream()),
									new TypeToken<ResultV1RDTO<HakukohdeValintaperusteetV1RDTO>>() {
									}.getType());

				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
				System.err.println("ERR");
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

		return new HakuImportRouteImpl(1, suoritaHakuImportKomponentti,
				valintaperusteetRestResource,
				tarjontaJaKoodistoHakukohteenHakuKomponentti);
	}
}
