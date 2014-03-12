package fi.vm.sade.valinta.kooste.test.komponentti;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.codehaus.jettison.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gson.Gson;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.LaskeValintakoeosallistumisetHakemukselleKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy.HakukohteenValintaperusteetProxy;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy.HakukohteenValintaperusteetProxyCachingImpl;

/**
 * User: wuoti Date: 29.8.2013 Time: 14.28
 * 
 */
@Ignore
@Configuration
@Import({ HakukohteenValintaperusteetProxyCachingImpl.class,
		LaskeValintakoeosallistumisetHakemukselleKomponentti.class,
		HaeHakukohteenHakemuksetKomponentti.class })
@ContextConfiguration(classes = { LaskeValintakoeosallistumisetHakemukselleKomponenttiTest.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class LaskeValintakoeosallistumisetHakemukselleKomponenttiTest {

	@Autowired
	private LaskeValintakoeosallistumisetHakemukselleKomponentti laskeValintakoeosallistumisetHakemukselleKomponentti;
	@Autowired
	private HakukohteenValintaperusteetProxy hakukohteenValintaperusteetProxyMock;
	@Autowired
	private ValintalaskentaService valintakoelaskentaProxyMock;
	@Autowired
	private ApplicationResource hakemusProxyMock;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ApplicationResource getHakemusProxy() {
		return mock(ApplicationResource.class);
	}

	@Bean
	public ValintalaskentaService getValintalaskentaService() {
		return mock(ValintalaskentaService.class);
	}

	@Bean
	public ValintaperusteService getValintaperusteService() {
		return mock(ValintaperusteService.class);
	}

	@Bean
	public ApplicationResource getApplicationResourceMock() {
		return mock(ApplicationResource.class);
	}

	@Test
	public void test() throws JSONException, ExecutionException {
		final String hakemusOid = "1.2.3.4.5.00000000039";
		Set<String> hakukohdeParams = new HashSet<String>(
				Arrays.asList(HAKUKOHDE_OID));

		ValintaperusteetTyyppi vp = new ValintaperusteetTyyppi();
		vp.setHakukohdeOid(HAKUKOHDE_OID);

		List<ValintaperusteetTyyppi> vps = Arrays.asList(vp);

		when(hakemusProxyMock.getApplicationByOid(eq(hakemusOid))).thenReturn(
				new Gson().fromJson(HAKEMUS_JSON, Hakemus.class));
		when(
				hakukohteenValintaperusteetProxyMock
						.haeValintaperusteet(hakukohdeParams)).thenReturn(vps);
		laskeValintakoeosallistumisetHakemukselleKomponentti.laske(hakemusOid);

		ArgumentCaptor<HakemusTyyppi> ac = ArgumentCaptor
				.forClass(HakemusTyyppi.class);
		verify(valintakoelaskentaProxyMock).valintakokeet(ac.capture(),
				anyList());

		HakemusTyyppi hakemus = ac.getValue();
		assertEquals(hakemusOid, hakemus.getHakemusOid());
		assertEquals(1, hakemus.getHakutoive().size());
		assertEquals(HAKUKOHDE_OID, hakemus.getHakutoive().get(0)
				.getHakukohdeOid());
		assertEquals(1, hakemus.getHakutoive().get(0).getPrioriteetti());
	}

	private final static String HAKUKOHDE_OID = "1.2.246.562.5.01245_01_114_0125";

	private final static String HAKEMUS_JSON = "{\n"
			+ "type: \"Application\",\n"
			+ "applicationSystemId: \"1.2.246.562.5.2013060313080811526781\",\n"
			+ "answers: {\n"
			+ "henkilotiedot: {\n"
			+ "kansalaisuus: \"FIN\",\n"
			+ "asuinmaa: \"FIN\",\n"
			+ "postitoimipaikka: \"\",\n"
			+ "Sukunimi: \"pFUBjjes\",\n"
			+ "SUKUPUOLI: \"n\",\n"
			+ "matkapuhelinnumero: \"0000000928\",\n"
			+ "Henkilotunnus: \"300582-2022\",\n"
			+ "Postinumero: \"00100\",\n"
			+ "lahiosoite: \"Jokukatu 1\",\n"
			+ "Sähköposti: \"TjtpvjnOuNehQar@oph.fi\",\n"
			+ "Kutsumanimi: \"VXCVX\",\n"
			+ "Etunimet: \"VXCVX XccVrVr\",\n"
			+ "ensisijainenOsoite1: \"true\",\n"
			+ "kotikunta: \"186\",\n"
			+ "aidinkieli: \"FI\",\n"
			+ "syntymaaika: \"30.05.1982\",\n"
			+ "Henkilotunnus_digest: \"d7cfec1111373ba98b0aff6ad4838269fbe3b2bd11aa2cef3becf5496323edf2\"\n"
			+ "},\n" + "lisatiedot: {\n" + "asiointikieli: \"suomi\",\n"
			+ "vaiheId: \"lisatiedot\"\n" + "},\n" + "hakutoiveet: {\n"
			+ "preference1-Koulutus-id: \""
			+ HAKUKOHDE_OID
			+ "\",\n"
			+ "preference1-Harkinnanvarainen: \"false\",\n"
			+ "preference1-Opetuspiste-id: \"1.2.246.562.10.70057800685\",\n"
			+ "preference1-Opetuspiste: \"Helmi Liiketalousopisto\",\n"
			+ "preference1-Koulutus-educationDegree: \"\",\n"
			+ "preference1-Koulutus: \"Liiketalouden perustutkinto, pk\",\n"
			+ "preference1-discretionary: \"\",\n"
			+ "preference1-Opetuspiste-id-parents: \"1.2.246.562.10.56373523374,1.2.246.562.10.80843262926,1.2.246.562.10.70057800685,1.2.246.562.10.00000000001\"\n"
			+ "},\n"
			+ "koulutustausta: {\n"
			+ "LISAKOULUTUS_TALOUS: \"false\",\n"
			+ "LISAKOULUTUS_AMMATTISTARTTI: \"false\",\n"
			+ "LISAKOULUTUS_KANSANOPISTO: \"false\",\n"
			+ "PK_PAATTOTODISTUSVUOSI: \"2012\",\n"
			+ "LISAKOULUTUS_VAMMAISTEN: \"false\",\n"
			+ "KOULUTUSPAIKKA_AMMATILLISEEN_TUTKINTOON: \"false\",\n"
			+ "LISAKOULUTUS_KYMPPI: \"false\",\n"
			+ "POHJAKOULUTUS: \"1\",\n"
			+ "perusopetuksen_kieli: \"FI\",\n"
			+ "osallistunut: \"false\",\n"
			+ "LISAKOULUTUS_MAAHANMUUTTO: \"false\"\n"
			+ "},\n"
			+ "osaaminen: {\n"
			+ "PK_KU_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_TE: \"Ei arvosanaa\",\n"
			+ "PK_KU_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_KS: \"7\",\n"
			+ "PK_KT: \"7\",\n"
			+ "PK_KU: \"9\",\n"
			+ "PK_BI_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_KO: \"9\",\n"
			+ "PK_BI_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_FY: \"6\",\n"
			+ "PK_MU_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_MU_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_BI: \"7\",\n"
			+ "PK_A1_OPPIAINE: \"EN\",\n"
			+ "PK_B22_OPPIAINE: \"PT\",\n"
			+ "PK_A2_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_A2_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_AI_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_MA_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_AI_VAL1: \"Ei arvosanaa\",\n"
			+ "vaiheId: \"osaaminen\",\n"
			+ "PK_A1_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_A1_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_MA_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_HI_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_B1_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_B22: \"6\",\n"
			+ "PK_HI_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_B1_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_KO_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_KO_VAL1: \"8\",\n"
			+ "PK_GE: \"6\",\n"
			+ "PK_A12_OPPIAINE: \"ES\",\n"
			+ "PK_KE: \"5\",\n"
			+ "PK_AI_OPPIAINE: \"FI\",\n"
			+ "PK_A2_OPPIAINE: \"JA\",\n"
			+ "PK_FY_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_MU: \"6\",\n"
			+ "PK_FY_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_B2_OPPIAINE: \"DE\",\n"
			+ "PK_HI: \"7\",\n"
			+ "PK_KS_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_YH_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_YH_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_A1: \"7\",\n"
			+ "PK_B1_OPPIAINE: \"SV\",\n"
			+ "PK_A2: \"6\",\n"
			+ "PK_KE_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_LI_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_KE_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_LI_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_KS_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_AI: \"9\",\n"
			+ "PK_LI: \"7\",\n"
			+ "PK_B2_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_B2_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_A12: \"9\",\n"
			+ "PK_YH: \"7\",\n"
			+ "PK_GE_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_B22_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_B22_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_GE_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_B2: \"7\",\n"
			+ "PK_A12_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_MA: \"7\",\n"
			+ "PK_A12_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_TE_VAL1: \"Ei arvosanaa\",\n"
			+ "PK_KT_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_TE_VAL2: \"Ei arvosanaa\",\n"
			+ "PK_B1: \"6\",\n"
			+ "PK_KT_VAL1: \"Ei arvosanaa\"\n"
			+ "}\n"
			+ "},\n"
			+ "oid: \"1.2.3.4.5.00000000039\",\n"
			+ "state: \"ACTIVE\",\n"
			+ "personOid: \"1.2.246.562.24.37911437777\",\n"
			+ "received: 1377088668958,\n"
			+ "meta: {\n"
			+ "sessionId: \"ef19a88b-9e4c-453d-9c5a-9cda899335f1\"\n"
			+ "},\n"
			+ "notes: [\n"
			+ "{\n"
			+ "type: \"ApplicationNote\",\n"
			+ "noteText: \"Päivitetty vaihetta 'lisatiedot'\",\n"
			+ "added: 1377682598056,\n"
			+ "user: \"1.2.246.562.24.00000000001\"\n"
			+ "},\n"
			+ "{\n"
			+ "type: \"ApplicationNote\",\n"
			+ "noteText: \"Päivitetty vaihetta 'lisatiedot'\",\n"
			+ "added: 1377599364371,\n"
			+ "user: \"1.2.246.562.24.00000000001\"\n"
			+ "},\n"
			+ "{\n"
			+ "type: \"ApplicationNote\",\n"
			+ "noteText: \"Päivitetty vaihetta 'osaaminen'\",\n"
			+ "added: 1377250942628,\n"
			+ "user: \"fi.vm.sade.security.SadeUserDetailsWrapper@5d0369f2\"\n"
			+ "},\n"
			+ "{\n"
			+ "type: \"ApplicationNote\",\n"
			+ "noteText: \"Päivitetty vaihetta 'osaaminen'\",\n"
			+ "added: 1377249644617,\n"
			+ "user: \"fi.vm.sade.security.SadeUserDetailsWrapper@5d0369f2\"\n"
			+ "},\n"
			+ "{\n"
			+ "type: \"ApplicationNote\",\n"
			+ "noteText: \"Päivitetty vaihetta 'osaaminen'\",\n"
			+ "added: 1377249079902,\n"
			+ "user: \"fi.vm.sade.security.SadeUserDetailsWrapper@585c3e99\"\n"
			+ "},\n"
			+ "{\n"
			+ "type: \"ApplicationNote\",\n"
			+ "noteText: \"Kävin katsomassa\",\n"
			+ "added: 1377180205623,\n"
			+ "user: \"fi.vm.sade.security.SadeUserDetailsWrapper@2985158\"\n"
			+ "},\n"
			+ "{\n"
			+ "type: \"ApplicationNote\",\n"
			+ "noteText: \"Hakemus vastaanotettu\",\n"
			+ "added: 1377088668958,\n"
			+ "user: \"anonymousUser\"\n"
			+ "}\n"
			+ "],\n"
			+ "_id: {\n"
			+ "time: 1377088669000,\n"
			+ "new: false,\n"
			+ "inc: 42510648,\n"
			+ "machine: -458164698,\n"
			+ "timeSecond: 1377088669\n" + "}\n" + "}";
}
