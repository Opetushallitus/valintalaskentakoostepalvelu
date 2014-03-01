package fi.vm.sade.valinta.kooste.integraatio.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Ignore
@Configuration
@ContextConfiguration(locations = "classpath:dokumentti-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class DokumenttipalveluIntegraatioTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(DokumenttipalveluIntegraatioTest.class);

	@Autowired
	private DokumenttiResource dokumenttiResource;

	@Test
	public void testIt() throws IOException {

		LOG.info("Montako dokumenttia? {}",
				dokumenttiResource.hae(Arrays.asList("hep", "skep")).size());
		dokumenttiResource.tallenna(null, "filez", DateTime.now().toDate()
				.getTime(), Arrays.asList("hep", "skep"), "",
				new ByteArrayInputStream("adfgafdg".getBytes()));
		//
		//
	}

}
