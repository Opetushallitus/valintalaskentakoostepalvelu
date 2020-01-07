package fi.vm.sade.valinta.kooste.kela;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.MockOpintopolkuCasAuthenticationFilter;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.impl.DokumenttiAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaFtpRouteImpl;
import fi.vm.sade.valinta.kooste.util.SecurityUtil;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnInputStreamAndHeaders;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static javax.ws.rs.HttpMethod.GET;

import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.charset.StandardCharsets;

@Configuration
@ContextConfiguration(classes = { KoostepalveluContext.CamelConfig.class, KelaFtpRouteTest.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class KelaFtpRouteTest {
	private String HOST = "localhost";
	private String PORT = "1234";
	private String PATH = "/tmp";
	private String USERNAME = "test";
	private String PASSWORD = "Testi123";

	@Rule
	public final FakeSftpServerRule sftpServer = new FakeSftpServerRule()
			.setPort(Integer.parseInt(PORT))
			.addUser(USERNAME, PASSWORD);

	private HttpClient client = new HttpClient(
			java.net.http.HttpClient.newBuilder().build(),
			null,
			DateDeserializer.gsonBuilder().create()
	);

	private DokumenttiAsyncResource dokumenttiAsyncResource = new DokumenttiAsyncResourceImpl(client);
	private KelaFtpRoute kelaFtpRoute = new KelaFtpRouteImpl(HOST, PORT, PATH, USERNAME, PASSWORD, dokumenttiAsyncResource);

	@Bean(name = "kelaValvomo")
	public ValvomoServiceImpl<KelaProsessi> getValvomoServiceImpl() {
		return new ValvomoServiceImpl<>();
	}

	@Before
	public void init() {
		startShared();
		MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_UPDATE_" + SecurityUtil.ROOTOID);
	}

	@Test
	public void testKelaFtpSiirto() {
		String dokumenttiId = "dokumenttiId";

        byte[] bytes = dokumenttiId.getBytes();
        mockToReturnInputStreamAndHeaders(GET, "/dokumenttipalvelu-service/resources/dokumentit/lataa/.*", "kela.txt", bytes);
		Boolean done;

		try {
			sftpServer.createDirectory(PATH);
			assert !sftpServer.existsFile("/tmp/kela.txt");
			done = kelaFtpRoute.aloitaKelaSiirto(dokumenttiId);
			assert sftpServer.existsFile("/tmp/kela.txt");
            String uploaded = sftpServer.getFileContent("/tmp/kela.txt", StandardCharsets.UTF_8);

            String expected = new String(bytes, StandardCharsets.UTF_8);
            String actual = uploaded;

            Assert.assertEquals(expected, actual);
		} catch (Exception e) {
			e.printStackTrace();
			done = false;
		}
		assert done;
	}
}
