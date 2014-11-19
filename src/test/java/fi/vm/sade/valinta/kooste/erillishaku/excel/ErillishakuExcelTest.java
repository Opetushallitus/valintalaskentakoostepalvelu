package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.ExcelValidointiPoikkeus;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishakuExcelTest {
	private final static Logger LOG = LoggerFactory.getLogger(ErillishakuExcelTest.class);
	
	@Test
	public void testaaTuontiKustomistaTiedostosta() throws ExcelValidointiPoikkeus, IOException {
		final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
		ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi",Collections.emptyList(), new ErillishakuRiviKuuntelija() {
					
					@Override
					public void erillishakuRiviTapahtuma(ErillishakuRivi rivi) {
						LOG.error("adfsga {}", rivi.getEtunimi());
						//Assert.assertTrue("Syntyma-aika ei tasmaa", syntymaAika.equals(rivi.getSyntymaAika()));
						tarkistusTapahtui.incrementAndGet();
					}
				});
		
		eExcel.getExcel().tuoXlsx(new ClassPathResource("kustom_erillishaku.xlsx").getInputStream());
		Assert.assertTrue(tarkistusTapahtui.get()==1);
	}
	
	@Test
	public void testaaVienti() throws FileNotFoundException, IOException {
		List<ErillishakuRivi> rivit = Lists.newArrayList();
		String syntymaAika = "11.11.2011";
		ErillishakuRivi rivi = new ErillishakuRivi("sukunimi","etunimi1","hetu",syntymaAika, "HYLATTY", "", "");
		rivit.add(rivi);
		ErillishakuRivi rivi2= new ErillishakuRivi("sukunimi","etunimi2","hetu",syntymaAika, "HYLATTY", "", "");
		rivit.add(rivi2);
		final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
		ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", rivit
				, new ErillishakuRiviKuuntelija() {
					
					@Override
					public void erillishakuRiviTapahtuma(ErillishakuRivi rivi) {
						LOG.error("adfsga {}", rivi.getEtunimi());
						Assert.assertTrue("Syntyma-aika ei tasmaa", "HYLATTY".equals(rivi.getHakemuksenTila()));
						tarkistusTapahtui.incrementAndGet();
					}
				});
		Excel excel = eExcel.getExcel();
		excel.tuoXlsx(excel.vieXlsx());
		Assert.assertTrue(tarkistusTapahtui.get()==2);
		if (false) {
			IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
					"erillishaku.xlsx"));
		}
	}
}
