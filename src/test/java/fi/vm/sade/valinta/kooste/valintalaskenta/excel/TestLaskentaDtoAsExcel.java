package fi.vm.sade.valinta.kooste.valintalaskenta.excel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

public class TestLaskentaDtoAsExcel {

	@Test
	public void testLaskentaDtoAsExcel() throws IOException {
		// uuid, hakuOid, luotu, tila, hakukohteet, valinnanvaihe,
		// valintakoelaskenta
		String uuid = null;
		String hakuOid = null;
		LaskentaTila tila = null;
		List<HakukohdeDto> hakukohteet = null;
		Integer valinnanvaihe = null;
		Boolean valintakoelaskenta = null;
		LaskentaDto l = new LaskentaDto(uuid, hakuOid, new Date().getTime(),
				tila, LaskentaTyyppi.HAKUKOHDE, hakukohteet, false, valinnanvaihe,
				valintakoelaskenta);
		LaskentaDtoAsExcel.laskentaDtoAsExcel(l);
	}
}
