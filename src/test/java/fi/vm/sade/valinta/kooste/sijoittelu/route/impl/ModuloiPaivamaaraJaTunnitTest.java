package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import java.text.ParseException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.ModuloiPaivamaaraJaTunnit;
import fi.vm.sade.valinta.kooste.util.Formatter;

public class ModuloiPaivamaaraJaTunnitTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(ModuloiPaivamaaraJaTunnitTest.class);

	@Test
	public void testaaTuntienModulointi() {
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 14, 1) == 14);
		Assert.assertEquals(13,
				ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 13, 1));
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 15, 1) == 15);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 14, 2) == 14);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 15, 2) == 16);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 16, 2) == 16);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 14, 6) == 14);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 15, 6) == 20);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 20, 6) == 20);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 21, 6) == 26);

		Assert.assertEquals(8,
				ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 7, 6));
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 9, 6) == 14);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 7, 6) == 8);
		Assert.assertTrue(ModuloiPaivamaaraJaTunnit.seuraavaAskel(14, 7, 2) == 8);
	}

	@Test
	public void testaaModulointiaVahaEnnen() throws ParseException {

		{
			DateTime start = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
					.parseDateTime("02.10.2014 14:05");

			DateTime now = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
					.parseDateTime("05.10.2015 14:03");

			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 1).toDate()));

			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 2).toDate()));
			Assert.assertEquals("05.10.2015 14:05", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 1).toDate()));

			Assert.assertEquals("05.10.2015 14:05", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 2).toDate()));

		}

	}

	@Test
	public void testaaModulointiaVahanJalkeen() throws ParseException {

		{
			DateTime start = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
					.parseDateTime("02.10.2014 14:05");

			DateTime now = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
					.parseDateTime("05.10.2015 14:07");
			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 1).toDate()));

			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 2).toDate()));

			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 6).toDate()));

			Assert.assertTrue("05.10.2015 15:05".equals(Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 1).toDate())));

			Assert.assertEquals("05.10.2015 16:05", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 2).toDate()));

			Assert.assertTrue("05.10.2015 20:05".equals(Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 6).toDate())));

		}

	}

	@Test
	public void testaaModulointiaPaljonJalkeen() throws ParseException {

		{
			DateTime start = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
					.parseDateTime("02.10.2014 14:05");

			DateTime now = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
					.parseDateTime("05.10.2015 18:03");
			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 1).toDate()));

			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 2).toDate()));

			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 6).toDate()));

			Assert.assertTrue("05.10.2015 18:05".equals(Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 1).toDate())));

			Assert.assertTrue("05.10.2015 18:05".equals(Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 2).toDate())));

			Assert.assertTrue("05.10.2015 20:05".equals(Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 6).toDate())));

		}
	}

	@Test
	public void testaaModulointiaYlipaivan() throws ParseException {

		{
			DateTime start = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
					.parseDateTime("02.10.2014 14:05");

			DateTime now = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
					.parseDateTime("05.10.2015 22:03");
			LOG.info("Aloituspvm {}", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 6).toDate()));

			Assert.assertEquals("06.10.2015 02:05", Formatter
					.paivamaara(ModuloiPaivamaaraJaTunnit.moduloiSeuraava(
							start, now, 6).toDate()));

		}
	}
}
