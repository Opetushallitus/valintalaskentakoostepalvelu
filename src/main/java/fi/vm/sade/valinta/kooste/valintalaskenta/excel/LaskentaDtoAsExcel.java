package fi.vm.sade.valinta.kooste.valintalaskenta.excel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;

public class LaskentaDtoAsExcel {

	public static byte[] laskentaDtoAsExcel(LaskentaDto laskenta) {
		Map<String, Object[][]> sheetAndGrid = Maps.newHashMap();
		{
			List<Object[]> grid = Lists.newArrayList();
			grid.add(new Object[] { "Suorittamattomat hakukohteet" });
			if (laskenta.getHakukohteet() != null) {
				for (HakukohdeDto hakukohde : laskenta.getHakukohteet()
						.stream()
						.filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
						.collect(Collectors.toList())) {
					List<String> rivi = Lists.newArrayList();
					rivi.add(hakukohde.getHakukohdeOid());
					rivi.addAll(hakukohde.getIlmoitukset().stream()
							.map(i -> i.getOtsikko())
							.collect(Collectors.toList()));
					grid.add(rivi.toArray());

				}
			}
			sheetAndGrid.put("Kesken", grid.toArray(new Object[][] {}));
		}
		{
			List<Object[]> grid = Lists.newArrayList();
			grid.add(new Object[] { "Valmistuneet hakukohteet" });
			if (laskenta.getHakukohteet() != null) {
				for (HakukohdeDto hakukohde : laskenta.getHakukohteet()
						.stream()
						.filter(h -> HakukohdeTila.VALMIS.equals(h.getTila()))
						.collect(Collectors.toList())) {
					grid.add(new Object[] { hakukohde.getHakukohdeOid() });
				}
			}
			sheetAndGrid.put("Valmiit", grid.toArray(new Object[][] {}));
		}
		return ExcelExportUtil.exportGridSheetsAsXlsBytes(sheetAndGrid);
	}
}
