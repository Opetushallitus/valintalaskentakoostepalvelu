package fi.vm.sade.valinta.kooste.util.excel;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SafeCellStyle {
    public static HSSFCellStyle create(HSSFWorkbook workbook) {
        HSSFCellStyle cellStyle = workbook.createCellStyle();
        protectFromFormulaInjection(cellStyle);
        return cellStyle;
    }

    public static XSSFCellStyle create(XSSFWorkbook workbook) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        protectFromFormulaInjection(cellStyle);
        return cellStyle;
    }

    private static void protectFromFormulaInjection(CellStyle cellStyle) {
        cellStyle.setQuotePrefixed(true);
    }
}
