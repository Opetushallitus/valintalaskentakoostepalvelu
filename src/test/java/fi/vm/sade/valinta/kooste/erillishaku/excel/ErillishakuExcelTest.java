package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.emptyErillishakuRivi;
import static org.junit.Assert.*;
import org.junit.Test;
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
	@Test
	public void testaaTuontiKustomistaTiedostosta() throws ExcelValidointiPoikkeus, IOException {
		final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
		ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", Collections.emptyList(), rivi -> tarkistusTapahtui.incrementAndGet());
		eExcel.getExcel().tuoXlsx(new ClassPathResource("kustom_erillishaku.xlsx").getInputStream());
		assertEquals(1, tarkistusTapahtui.get());
	}

	@Test
	public void testaaTuontiKustomistaTiedostostaOtsikoilla() throws ExcelValidointiPoikkeus, IOException {
		final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
		ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi",Collections.emptyList(), rivi -> tarkistusTapahtui.incrementAndGet());
		eExcel.getExcel().tuoXlsx(new ClassPathResource("kustom_erillishaku_otsikoilla.xlsx").getInputStream());
		assertEquals(1, tarkistusTapahtui.get());
	}
    
	@Test
	public void testaaVienti() throws IOException {
		List<ErillishakuRivi> rivit = Lists.newArrayList();
		String syntymaAika = "11.11.2011";
		ErillishakuRivi rivi = new ErillishakuRivi(
				null, "sukunimi","etunimi1","hetu","test.email@example.com", syntymaAika, Sukupuoli.MIES.name(), "", "FI", "HYLATTY", false, "", "", false, false, "FI",
				"040123456789", "Esimerkkitie 2", "00100", "HELSINKI", "FIN", "FIN", "HELSINKI", null);
		rivit.add(rivi);
		ErillishakuRivi rivi2= new ErillishakuRivi(null, "sukunimi","etunimi2","hetu","test.email@example.com", syntymaAika, Sukupuoli.NAINEN.name(), "", "FI", "HYLATTY", false, "", "", true, false, "FI",
				"040123456789", "Esimerkkitie 2", "00100", "HELSINKI", "FIN", "FIN", "HELSINKI", "FIN");
		rivit.add(rivi2);
		ErillishakuRivi rivi3 = emptyErillishakuRivi();
		rivit.add(rivi3);
		final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
		ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", rivit, rv -> tarkistusTapahtui.incrementAndGet());
		Excel excel = eExcel.getExcel();
		excel.tuoXlsx(excel.vieXlsx());

		assertEquals(
				2 // tavalliset rivit
				+1 // tyhjärivi julkaisulupasyötteellä
				, tarkistusTapahtui.get());
		if (false) {
			IOUtils.copy(excel.vieXlsx(), new FileOutputStream("erillishaku.xlsx"));
		}
	}
}
