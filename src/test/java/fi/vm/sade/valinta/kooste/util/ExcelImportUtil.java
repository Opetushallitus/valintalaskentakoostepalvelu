package fi.vm.sade.valinta.kooste.util;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.excel.HSSFRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.XSSFRivi;
import java.io.InputStream;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Jussi Jartamo
 */
public class ExcelImportUtil {

  public static List<Rivi> importExcel(InputStream excel) throws Throwable {
    XSSFWorkbook workbook = new XSSFWorkbook(excel);
    XSSFSheet sheet = workbook.getSheetAt(workbook.getActiveSheetIndex());
    int lastRowIndex = sheet.getLastRowNum();
    List<Rivi> rivit = Lists.newArrayList();
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

  public static List<Rivi> importHSSFExcel(InputStream excel) throws Throwable {
    HSSFWorkbook workbook = new HSSFWorkbook(excel);
    HSSFSheet sheet = workbook.getSheetAt(workbook.getActiveSheetIndex());
    int lastRowIndex = sheet.getLastRowNum();
    List<Rivi> rivit = Lists.newArrayList();
    for (int i = 0; i <= lastRowIndex; ++i) {
      HSSFRow row = sheet.getRow(i);
      // LOG.error("rivi [{}]", i);
      Rivi rivi;
      if (row == null) {
        rivi = Rivi.tyhjaRivi();
      } else {
        rivi = HSSFRivi.asRivi(row);
      }
      rivit.add(rivi);
    }
    return rivit;
  }
}
