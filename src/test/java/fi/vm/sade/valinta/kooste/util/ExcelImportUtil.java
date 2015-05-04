package fi.vm.sade.valinta.kooste.util;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.XSSFRivi;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jussi Jartamo
 */
public class ExcelImportUtil {

    public static Collection<Rivi> importExcel(InputStream excel) throws Throwable {
        XSSFWorkbook workbook = new XSSFWorkbook(excel);
        XSSFSheet sheet = workbook.getSheetAt(workbook.getActiveSheetIndex());
        int lastRowIndex = sheet.getLastRowNum();
        Collection<Rivi> rivit = Lists.newArrayList();
        for (int i = 0; i <= lastRowIndex; ++i) {
            XSSFRow row = sheet.getRow(i);
            // LOG.error("rivi [{}]", i);
            Rivi rivi;
            if (row == null) {
                rivi = Rivi.tyhjaRivi();
            } else {
                rivi = XSSFRivi.asRivi(row);
            }
            rivit.add(rivi);
        }
        return rivit;
    }
}
