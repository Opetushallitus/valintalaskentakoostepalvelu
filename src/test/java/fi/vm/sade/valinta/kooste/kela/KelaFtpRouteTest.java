package fi.vm.sade.valinta.kooste.kela;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaFtpRouteImpl;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@ContextConfiguration(classes = { KoostepalveluContext.CamelConfig.class,
		KelaFtpRouteTest.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class KelaFtpRouteTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(KelaFtpRouteTest.class);
	private static final String FTP_MOCK = "mock:ftpMock";
	private static final String FTP_CONFIG = "retainFirst=1";
	private static final String KELA_SIIRTO = "direct:kela_siirto";

	@Bean(name = "kelaValvomo")
	public ValvomoServiceImpl<KelaProsessi> getValvomoServiceImpl() {
		return new ValvomoServiceImpl<KelaProsessi>();
	}

	@Bean
	public KelaFtpRoute getKelaFtpRoute(

	@Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(KELA_SIIRTO), KelaFtpRoute.class);
	}

	@Bean
	public KelaFtpRouteImpl getKelaRouteImpl(

	DokumenttiResource dokumenttiResource) {
		/**
		 * Ylikirjoitetaan kela-ftp endpoint logitusreitilla yksikkotestia
		 * varten!
		 */
		return new KelaFtpRouteImpl(KELA_SIIRTO, FTP_MOCK, FTP_CONFIG,
				dokumenttiResource);
	}

	@Bean
	public DokumenttiResource mockDokumenttiResource() {
		return mock(DokumenttiResource.class);
	}

	@Autowired
	private KelaFtpRoute kelaFtpRoute;
	@Autowired
	private DokumenttiResource dokumenttiResource;
	@Autowired
	private CamelContext context;
	@Autowired
	private KelaFtpRouteImpl ftpRouteImpl;

	@Test
	public void testKelaFtpSiirto() {

		String dokumenttiId = "dokumenttiId";
		Mockito.when(dokumenttiResource.lataa(Mockito.anyString())).thenReturn(
				Response.ok()
						.entity(new ByteArrayInputStream(dokumenttiId
								.getBytes())).build());

		kelaFtpRoute.aloitaKelaSiirto(dokumenttiId);

		MockEndpoint resultEndpoint = context.getEndpoint(
				ftpRouteImpl.getFtpKelaSiirto(), MockEndpoint.class);
		resultEndpoint.assertExchangeReceived(0).getIn(Response.class);
		Mockito.verify(dokumenttiResource).lataa(Mockito.eq(dokumenttiId));
	}

}
