package fi.vm.sade.valinta.kooste.valintalaskentatulos.excel;

import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

public class ValintalaskennanTulosExcel {

    public static XSSFWorkbook luoExcel(List<ValintatietoValinnanvaiheDTO> valinnanVaiheet) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        valinnanVaiheet.stream().forEach(vaihe -> {
            vaihe.getValintatapajonot().forEach(jono -> {
                workbook.createSheet(vaihe.getNimi() + " - " + jono.getNimi());
            });
        });
        return workbook;
    }
}
