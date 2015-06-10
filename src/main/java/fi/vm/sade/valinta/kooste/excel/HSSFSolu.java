package fi.vm.sade.valinta.kooste.excel;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HSSFSolu {
    private static final Logger LOG = LoggerFactory.getLogger(HSSFSolu.class);

    private HSSFSolu() {
    }

    public static Solu asSolu(HSSFCell cell) {
        if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return new Paivamaara(cell.getDateCellValue());
            } else {
                // LOG.error("{}", cell.getNumericCellValue());
                return new Numero(cell.getNumericCellValue());
            }
        } else {
            String rawValue;
            if (Cell.CELL_TYPE_STRING == cell.getCellType()) {
                rawValue = cell.getStringCellValue();
            } else {
                rawValue = StringUtils.EMPTY;
            }

            // String rawValue = StringUtils.trimToEmpty(cell.getRawValue());
            try {
                String maybeNumber = rawValue.replace(",", ".");
                double d = Double.parseDouble(maybeNumber);
                return new Numero(d);
            } catch (Exception e) {
            }
            return new Teksti(rawValue);
        }
    }
}
