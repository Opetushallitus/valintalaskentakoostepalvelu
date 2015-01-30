package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Lists;

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
						tarkistusTapahtui.incrementAndGet();
					}
				});
		
		eExcel.getExcel().tuoXlsx(new ClassPathResource("kustom_erillishaku.xlsx").getInputStream());
		assertTrue(tarkistusTapahtui.get()==1);
	}

	@Test
	public void testaaTuontiKustomistaTiedostostaOtsikoilla() throws ExcelValidointiPoikkeus, IOException {
		final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
		ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi",Collections.emptyList(), new ErillishakuRiviKuuntelija() {

			@Override
			public void erillishakuRiviTapahtuma(ErillishakuRivi rivi) {
				tarkistusTapahtui.incrementAndGet();
			}
		});
		eExcel.getExcel().tuoXlsx(new ClassPathResource("kustom_erillishaku_otsikoilla.xlsx").getInputStream());
		assertEquals(1, tarkistusTapahtui.get());
	}
	@Test
	public void testaaVienti() throws FileNotFoundException, IOException {
		List<ErillishakuRivi> rivit = Lists.newArrayList();
		String syntymaAika = "11.11.2011";
		ErillishakuRivi rivi = new ErillishakuRivi(null, "sukunimi","etunimi1","hetu","test.email@example.com", syntymaAika, "", "HYLATTY", "", "", false, Optional.empty());
		rivit.add(rivi);
		ErillishakuRivi rivi2= new ErillishakuRivi(null, "sukunimi","etunimi2","hetu","test.email@example.com", syntymaAika, "", "HYLATTY", "", "", true, Optional.empty());
		rivit.add(rivi2);
		ErillishakuRivi rivi3 = new ErillishakuRivi();
		rivit.add(rivi3);
		final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
		ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", rivit
				, rv -> tarkistusTapahtui.incrementAndGet()
				);
		Excel excel = eExcel.getExcel();
		excel.tuoXlsx(excel.vieXlsx());

		assertEquals(
				2 // tavalliset rivit
				+1 // tyhjärivi julkaisulupasyötteellä
				, tarkistusTapahtui.get());
		if (false) {
			IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
					"erillishaku.xlsx"));
		}
	}
}
