package fi.vm.sade.valinta.kooste.excel;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *         XSSFCell -> Solu
 */
public class XSSFSolu {

    private XSSFSolu() {
    }

    public static Solu asSolu(XSSFCell cell) {
        if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return new Paivamaara(cell.getDateCellValue());
            } else {
                return new Numero(cell.getNumericCellValue());
            }
        } else {
            String rawValue;
            if (Cell.CELL_TYPE_STRING == cell.getCellType()) {
                rawValue = cell.getStringCellValue();
            } else {
                rawValue = StringUtils.EMPTY;
            }
            return new Teksti(rawValue);
        }
    }
}
