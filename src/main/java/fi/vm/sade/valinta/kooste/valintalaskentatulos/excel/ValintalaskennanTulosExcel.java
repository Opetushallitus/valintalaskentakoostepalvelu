package fi.vm.sade.valinta.kooste.valintalaskentatulos.excel;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti.getTeksti;

import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

public class ValintalaskennanTulosExcel {

    public static XSSFWorkbook luoExcel(final HakukohdeDTO hakukohdeDTO, List<ValintatietoValinnanvaiheDTO> valinnanVaiheet) {

        XSSFWorkbook workbook = new XSSFWorkbook();
        valinnanVaiheet.stream().forEach(vaihe -> {
            vaihe.getValintatapajonot().forEach(jono -> {
                final XSSFSheet sheet = workbook.createSheet(vaihe.getNimi() + " - " + jono.getNimi());
                sheet.createRow(0).createCell(0).setCellValue(getTeksti(hakukohdeDTO.getTarjoajaNimi()));
                sheet.createRow(1).createCell(0).setCellValue(getTeksti(hakukohdeDTO.getHakukohdeNimi()));
            });
        });
        return workbook;
    }
}
