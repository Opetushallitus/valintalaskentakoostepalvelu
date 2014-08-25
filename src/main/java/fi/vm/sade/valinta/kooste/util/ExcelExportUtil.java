package fi.vm.sade.valinta.kooste.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Muuntaa Object[][]:n xls-tiedostoksi!
 */
public class ExcelExportUtil {

	public static final FastDateFormat DATE_FORMAT = FastDateFormat
			.getInstance("dd.MM.yyyy hh.mm");

	public static byte[] exportGridAsXlsBytes(Object[][] grid) {
		assert (grid != null);
		Workbook wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet(DATE_FORMAT.format(new Date()));
		int numberOfcolumns = 0;
		//
		// Create rows!
		//
		short rowIndex = 0;
		for (Object[] dataRow : grid) {
			assert (dataRow != null);
			Row excelRow = sheet.createRow(rowIndex);
			//
			// Create columns!
			//
			short cellIndex = 0;
			for (Object dataCell : dataRow) {
				if (dataCell == null) {
					dataCell = StringUtils.EMPTY;
				}
				numberOfcolumns = Math.max(numberOfcolumns, cellIndex);
				Cell excelCell = excelRow.createCell(cellIndex);
				String value = dataCell.toString();
				excelCell.setCellValue(value);
				++cellIndex;
			}
			++rowIndex;
		}
		//
		// Auto size used columns!
		//
		for (int column = 0; column <= numberOfcolumns; ++column) {
			sheet.autoSizeColumn(column);
		}

		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		try {
			wb.write(bytesOut);
		} catch (IOException e) {
			e.printStackTrace(); // <- not going to happen since not using real
									// I/O
		}
		return bytesOut.toByteArray();
	}

	public static InputStream exportGridAsXls(Object[][] grid) {
		return new ByteArrayInputStream(exportGridAsXlsBytes(grid));// bytesOut.newInputStream();
	}
}
