package fi.vm.sade.valinta.kooste.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusList;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Testaa uudelleen yrityksia, eli epavarmaa verkkoyhteytta!
 * 
 * @Ignore NGinx ongelma on ratkaistu. Ei tarvi toistaiseksi varautua näihin!
 */
@Ignore
@Configuration
@ContextConfiguration(classes = HakuRetryTesti.class)
@ImportResource({ "classpath:test-context.xml",
		"classpath:META-INF/spring/context/haku-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class HakuRetryTesti {

	@Bean
	public ApplicationResource getApplicationResource() {
		ApplicationResource mock = new ApplicationResource() {
			final boolean[] fail = { true, true, false, true, false, false,
					true };
			volatile int counter = 0;

			@Override
			public List<Hakemus> getApplicationsByOidsGet(List<String> oids) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<Hakemus> getApplicationsByOid(String asId,
					String aoOid, List<String> appStates, int rows) {
				return null;
			}

			public void failRandomly() {
				++counter;
				if (fail[counter % fail.length])
					throw new RuntimeException("satunnainen verkkovirhe!");
			}

			public Hakemus getApplicationByOid(String oid) {
				failRandomly();
				return new Hakemus();
			}

			public HakemusList findApplications(String query,
					List<String> state, String aoid, String lopoid,
					String asId, String aoOid, int start, int rows) {
				failRandomly();
				return new HakemusList();
			}

			public List<Hakemus> getApplicationsByOids(List<String> oids) {
				failRandomly();
				return new ArrayList<Hakemus>();
			}

			@Override
			public List<ApplicationAdditionalDataDTO> getApplicationAdditionalData(
					String hakuOid, String hakukohdeOid) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void putApplicationAdditionalData(String hakuOid,
					String hakukohdeOid,
					List<ApplicationAdditionalDataDTO> additionalData) {
				// TODO Auto-generated method stub

			}
		};

		return mock;
	}

	@Autowired
	ApplicationResource hakemusProxy;

	@Test
	public void hakuWithRandomChaos() {
		for (int times = 0; times < 20; ++times) {
			hakemusProxy.getApplicationByOid(StringUtils.EMPTY);
		}
	}
}
