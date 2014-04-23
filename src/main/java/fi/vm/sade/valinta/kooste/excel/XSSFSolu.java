package fi.vm.sade.valinta.kooste.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         XSSFCell -> Solu
 */
public class XSSFSolu {

	private static final Logger LOG = LoggerFactory.getLogger(XSSFSolu.class);

	private XSSFSolu() {
	}

	public static Solu asSolu(XSSFCell cell) {
		if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
			// LOG.error("{}", cell.getNumericCellValue());
			return new Numero(cell.getNumericCellValue());
		} else if (Cell.CELL_TYPE_STRING == cell.getCellType()) {
			return new Teksti(cell.getStringCellValue());
		}
		// vakiona palautetaan tyhja teksti
		return new Teksti();
	}
}
