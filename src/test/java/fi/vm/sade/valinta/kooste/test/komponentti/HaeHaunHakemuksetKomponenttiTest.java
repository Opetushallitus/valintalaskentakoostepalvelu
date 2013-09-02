package fi.vm.sade.valinta.kooste.test.komponentti;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.HaeHaunHakemuksetKomponentti;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 14.28
 */
public class HaeHaunHakemuksetKomponenttiTest {

    private HaeHaunHakemuksetKomponentti haeHaunHakemuksetKomponentti;

    private final static int PORT = 8095;
    private final static String HAKEMUS_URL = "http://localhost:" + PORT;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);


    @Before
    public void setUp() {
        haeHaunHakemuksetKomponentti = new HaeHaunHakemuksetKomponentti();
    }

    private final static String[] HAKEMUS_OIDS = new String[]{
            "1.2.3.4.5.00000000039",
            "1.2.3.4.5.00000000042",
            "1.2.3.4.5.00000000057",
    };

    private final static String RESPONSE_JSON =
            "{" +
                    "totalCount: 3," +
                    "results: [" +
                    "{" +
                    "oid: \"" + HAKEMUS_OIDS[0] + "\"," +
                    "state: \"ACTIVE\"," +
                    "firstNames: \"VXCVX XccVrVr\"," +
                    "lastName: \"pFUBjjes\"," +
                    "ssn: \"300582-2022\"," +
                    "personOid: \"1.2.246.562.24.37911437777\"" +
                    "}," +
                    "{" +
                    "oid: \"" + HAKEMUS_OIDS[1] + "\"," +
                    "state: \"ACTIVE\"," +
                    "firstNames: \"ZYayxck cozVZk\"," +
                    "lastName: \"mqmVgddU\"," +
                    "ssn: \"131194-1412\"," +
                    "personOid: \"1.2.246.562.24.50174373493\"" +
                    "}," +
                    "{" +
                    "oid: \"" + HAKEMUS_OIDS[2] + "\"," +
                    "state: \"ACTIVE\"," +
                    "firstNames: \"ZiUxxz-ippZz\"," +
                    "lastName: \"pZuuYpSpx\"," +
                    "ssn: \"140592-174D\"," +
                    "personOid: \"1.2.246.562.24.23811448414\"" +
                    "}" +
                    "]" +
                    "}";

    @Test
    public void test() {
        ReflectionTestUtils.setField(haeHaunHakemuksetKomponentti, "hakemusUrl", HAKEMUS_URL);
        final String hakuOid = "1.2.246.56552.5.2013060313080811526781";

        stubFor(get(urlEqualTo("/?asId=" + hakuOid + "&appState=ACTIVE&appState=INCOMPLETE"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/json")
                        .withBody(RESPONSE_JSON)));

        List<String> oids = haeHaunHakemuksetKomponentti.haeHaunHakemukset(hakuOid);
        assertEquals(HAKEMUS_OIDS.length, oids.size());
        assertTrue(oids.containsAll(Arrays.asList(HAKEMUS_OIDS)));
    }
}
