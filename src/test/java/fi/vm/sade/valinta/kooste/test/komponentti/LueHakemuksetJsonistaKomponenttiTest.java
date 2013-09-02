package fi.vm.sade.valinta.kooste.test.komponentti;

import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.LueHakemuksetJsonistaKomponentti;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;


/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 14.28
 */
public class LueHakemuksetJsonistaKomponenttiTest {

    private LueHakemuksetJsonistaKomponentti lueHakemuksetJsonistaKomponentti = new LueHakemuksetJsonistaKomponentti();

    private final static String[] HAKEMUS_OIDS = new String[]{
            "1.2.3.4.5.00000000039",
            "1.2.3.4.5.00000000042",
            "1.2.3.4.5.00000000057",
    };

    private final static String HAKEMUKSET_JSON =
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
        List<String> oids = lueHakemuksetJsonistaKomponentti.lueHakemuksetJsonista(HAKEMUKSET_JSON);
        assertTrue(oids.containsAll(Arrays.asList(HAKEMUS_OIDS)));
    }
}
