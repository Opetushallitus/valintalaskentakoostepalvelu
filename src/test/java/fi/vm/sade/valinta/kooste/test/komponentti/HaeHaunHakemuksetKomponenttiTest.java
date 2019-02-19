package fi.vm.sade.valinta.kooste.test.komponentti;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.gson.Gson;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHaunHakemuksetKomponentti;

/**
 * User: wuoti Date: 29.8.2013 Time: 14.28
 */
public class HaeHaunHakemuksetKomponenttiTest {

	private HaeHaunHakemuksetKomponentti haeHaunHakemuksetKomponentti = new HaeHaunHakemuksetKomponentti();

	private ApplicationResource applicationResourceMock;

	@Before
	public void setUp() {
		applicationResourceMock = mock(ApplicationResource.class);
		ReflectionTestUtils.setField(haeHaunHakemuksetKomponentti,
				"applicationResource", applicationResourceMock);
	}

	private final static String[] HAKEMUS_OIDS = new String[] {
			"1.2.3.4.5.00000000039", "1.2.3.4.5.00000000042",
			"1.2.3.4.5.00000000057", };

	private final static String HAKEMUKSET_JSON = "{" + "totalCount: 3,"
			+ "results: [" + "{" + "oid: \""
			+ HAKEMUS_OIDS[0]
			+ "\","
			+ "state: \"ACTIVE\","
			+ "firstNames: \"VXCVX XccVrVr\","
			+ "lastName: \"pFUBjjes\","
			+ "ssn: \"300582-2022\","
			+ "personOid: \"1.2.246.562.24.37911437777\""
			+ "},"
			+ "{"
			+ "oid: \""
			+ HAKEMUS_OIDS[1]
			+ "\","
			+ "state: \"ACTIVE\","
			+ "firstNames: \"ZYayxck cozVZk\","
			+ "lastName: \"mqmVgddU\","
			+ "ssn: \"131194-1412\","
			+ "personOid: \"1.2.246.562.24.50174373493\""
			+ "},"
			+ "{"
			+ "oid: \""
			+ HAKEMUS_OIDS[2]
			+ "\","
			+ "state: \"ACTIVE\","
			+ "firstNames: \"ZiUxxz-ippZz\","
			+ "lastName: \"pZuuYpSpx\","
			+ "ssn: \"140592-174D\","
			+ "personOid: \"1.2.246.562.24.23811448414\"" + "}" + "]" + "}";

	@Test
	public void test() {
		final String hakuOid = "hakuOid1";

		when(
				applicationResourceMock.findApplications(eq(null),
						anyList(), eq(null), eq(null), eq(hakuOid),
						eq(null), anyInt(), anyInt())).thenReturn(
				new Gson().fromJson(HAKEMUKSET_JSON, HakemusList.class));

		List<SuppeaHakemus> hakemukset = haeHaunHakemuksetKomponentti
				.haeHaunHakemukset(hakuOid);
		Set<String> oids = new HashSet<String>();
		for (SuppeaHakemus h : hakemukset) {
			oids.add(h.getOid());
		}

		assertTrue(oids.containsAll(Arrays.asList(HAKEMUS_OIDS)));
	}
}
