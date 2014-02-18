package fi.vm.sade.valinta.kooste.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fi.vm.sade.valinta.kooste.external.resource.haku.proxy.HakemusProxyCachingImpl;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gson.Gson;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.LaskeValintakoeosallistumisetHakemukselleKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy.HakukohteenValintaperusteetProxyCachingImpl;
import fi.vm.sade.valinta.kooste.valintakokeet.route.HakukohteenValintakoelaskentaRoute;
import fi.vm.sade.valinta.kooste.valintakokeet.route.impl.HakukohteenValintakoelaskentaRouteImpl;
import fi.vm.sade.valinta.kooste.valintakokeet.route.impl.ValintakoelaskentaConfig;

/**
 * User: wuoti Date: 9.9.2013 Time: 9.24
 */
@Configuration
@Import({ HakukohteenValintakoelaskentaRouteImpl.class, HaeHakemusKomponentti.class,
        HakukohteenValintaperusteetProxyCachingImpl.class, LaskeValintakoeosallistumisetHakemukselleKomponentti.class,
        HaeHakukohteenHakemuksetKomponentti.class, HakemusProxyCachingImpl.class})
@ContextConfiguration(classes = { HakukohteenValintakoelaskentaKoosteReititysTest.class,
        KoostepalveluContext.CamelConfig.class, ValintakoelaskentaConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class HakukohteenValintakoelaskentaKoosteReititysTest {

    private final static int PORT = 8097;

    private final static String HAKEMUS1_OID = "1.2.3.4.5.00000000038";
    private final static String HAKEMUS2_OID = "1.2.3.4.5.00000000039";

    @Autowired
    private HakukohteenValintakoelaskentaRoute hakukohteenValintakoelaskentaRoute;

    @Autowired
    private ApplicationResource applicationResourceMock;

    @Autowired
    private ValintaperusteService valintaperusteServiceMock;

    @Autowired
    private ValintalaskentaService valintalaskentaServiceMock;

    @Bean
    public ValintaperusteService getValintaperusteServiceMock() {
        return mock(ValintaperusteService.class);
    }

    @Bean
    public ValintalaskentaService getValintalaskentaServiceMock() {
        return mock(ValintalaskentaService.class);
    }

    @Bean
    public ApplicationResource getApplicationResourceMock() {
        return mock(ApplicationResource.class);
    }

    @Test
    public void test() throws JSONException {
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        when(
                applicationResourceMock.findApplications(anyString(), anyList(), anyString(), anyString(), anyString(),
                        eq(HAKUKOHDE_OID), anyInt(), anyInt())).thenReturn(
                new Gson().fromJson(HAKEMUKSET_RESPONSE_JSON, HakemusList.class));
        when(applicationResourceMock.getApplicationByOid(eq(HAKEMUS1_OID))).thenReturn(
                new Gson().fromJson(HAKEMUS1_RESPONSE_JSON, Hakemus.class));
        when(applicationResourceMock.getApplicationByOid(eq(HAKEMUS2_OID))).thenReturn(
                new Gson().fromJson(HAKEMUS2_RESPONSE_JSON, Hakemus.class));

        hakukohteenValintakoelaskentaRoute.aktivoiValintakoelaskenta(HAKUKOHDE_OID);

        ArgumentCaptor<HakemusTyyppi> ac = ArgumentCaptor.forClass(HakemusTyyppi.class);
        verify(valintalaskentaServiceMock, times(2)).valintakokeet(ac.capture(), anyList());

        List<HakemusTyyppi> lasketutHakemukset = ac.getAllValues();
        assertEquals(2, lasketutHakemukset.size());
        Collections.sort(lasketutHakemukset, new Comparator<HakemusTyyppi>() {
            @Override
            public int compare(HakemusTyyppi o1, HakemusTyyppi o2) {
                return o1.getHakemusOid().compareTo(o2.getHakemusOid());
            }
        });

        assertEquals(HAKEMUS1_OID, lasketutHakemukset.get(0).getHakemusOid());
        assertEquals(HAKEMUS2_OID, lasketutHakemukset.get(1).getHakemusOid());
    }

    private final static String HAKEMUKSET_RESPONSE_JSON = "{" + "totalCount: 2," + "results: [" + "{" + "oid: \""
            + HAKEMUS1_OID + "\"," + "state: \"ACTIVE\"," + "firstNames: \"VXCVX XccVrVr\","
            + "lastName: \"pFUBjjes\"," + "ssn: \"300582-2022\"," + "personOid: \"1.2.246.562.24.37911437777\"" + "},"
            + "{" + "oid: \"" + HAKEMUS2_OID + "\"," + "state: \"ACTIVE\"," + "firstNames: \"ZYayxck cozVZk\","
            + "lastName: \"mqmVgddU\"," + "ssn: \"131194-1412\"," + "personOid: \"1.2.246.562.24.50174373493\"" + "}"
            + "]" + "}";

    private final static String HAKUKOHDE_OID = "1.2.246.562.5.01245_01_114_0125";

    private final static String HAKEMUS1_RESPONSE_JSON = "{\n" + "type: \"Application\",\n"
            + "applicationSystemId: \"1.2.246.562.5.2013060313080811526781\",\n" + "answers: {\n"
            + "henkilotiedot: {\n" + "kansalaisuus: \"FIN\",\n" + "asuinmaa: \"FIN\",\n" + "postitoimipaikka: \"\",\n"
            + "Sukunimi: \"pFUBjjes\",\n" + "SUKUPUOLI: \"n\",\n" + "matkapuhelinnumero: \"0000000928\",\n"
            + "Henkilotunnus: \"300582-2022\",\n" + "Postinumero: \"00100\",\n" + "lahiosoite: \"Jokukatu 1\",\n"
            + "Sähköposti: \"TjtpvjnOuNehQar@oph.fi\",\n" + "Kutsumanimi: \"VXCVX\",\n"
            + "Etunimet: \"VXCVX XccVrVr\",\n" + "ensisijainenOsoite1: \"true\",\n" + "kotikunta: \"186\",\n"
            + "aidinkieli: \"FI\",\n" + "syntymaaika: \"30.05.1982\",\n"
            + "Henkilotunnus_digest: \"d7cfec1111373ba98b0aff6ad4838269fbe3b2bd11aa2cef3becf5496323edf2\"\n" + "},\n"
            + "lisatiedot: {\n" + "asiointikieli: \"suomi\",\n" + "vaiheId: \"lisatiedot\"\n" + "},\n"
            + "hakutoiveet: {\n" + "preference1-Koulutus-id: \""
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
            + "oid: \""
            + HAKEMUS1_OID
            + "\",\n"
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
            + "inc: 42510648,\n" + "machine: -458164698,\n" + "timeSecond: 1377088669\n" + "}\n" + "}";

    private final static String HAKEMUS2_RESPONSE_JSON = "{\n" + "type: \"Application\",\n"
            + "applicationSystemId: \"1.2.246.562.5.2013060313080811526781\",\n" + "answers: {\n"
            + "henkilotiedot: {\n" + "kansalaisuus: \"FIN\",\n" + "asuinmaa: \"FIN\",\n" + "postitoimipaikka: \"\",\n"
            + "Sukunimi: \"mqmVgddU\",\n" + "SUKUPUOLI: \"n\",\n" + "matkapuhelinnumero: \"0000000928\",\n"
            + "Henkilotunnus: \"131194-1412\",\n" + "Postinumero: \"00100\",\n" + "lahiosoite: \"Jokukatu 1\",\n"
            + "Sähköposti: \"TjtpvjnOuNehQar@oph.fi\",\n" + "Kutsumanimi: \"ZYayxck\",\n"
            + "Etunimet: \"ZYayxck cozVZk\",\n" + "ensisijainenOsoite1: \"true\",\n" + "kotikunta: \"186\",\n"
            + "aidinkieli: \"FI\",\n" + "syntymaaika: \"13.11.1994\",\n"
            + "Henkilotunnus_digest: \"d7cfec1111373ba98b0aff6ad4838269fbe3b2bd11aa2cef3becf5496323edf2\"\n" + "},\n"
            + "lisatiedot: {\n" + "asiointikieli: \"suomi\",\n" + "vaiheId: \"lisatiedot\"\n" + "},\n"
            + "hakutoiveet: {\n" + "preference1-Koulutus-id: \""
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
            + "oid: \""
            + HAKEMUS2_OID
            + "\",\n"
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
            + "inc: 42510648,\n" + "machine: -458164698,\n" + "timeSecond: 1377088669\n" + "}\n" + "}";

}
