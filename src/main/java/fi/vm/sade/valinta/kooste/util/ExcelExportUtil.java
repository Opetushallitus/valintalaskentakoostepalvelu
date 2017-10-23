package fi.vm.sade.valinta.kooste.util;

import static org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER;

import fi.vm.sade.valinta.kooste.util.excel.Highlight;
import fi.vm.sade.valinta.kooste.util.excel.SafeCellStyle;
import fi.vm.sade.valinta.kooste.util.excel.Span;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

/**
 *         Muuntaa Object[][]:n xls-tiedostoksi!
 */
public class ExcelExportUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ExcelExportUtil.class);

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy hh.mm");

    public static byte[] exportGridSheetsAsXlsBytes(Map<String, Object[][]> grids) {
        assert (grids != null);
        HSSFWorkbook wb = new HSSFWorkbook();
        CellStyle alignCenterStyle = SafeCellStyle.create(wb);
        alignCenterStyle.setAlignment(CENTER);
        HSSFCellStyle highlight = SafeCellStyle.create(wb);
        HSSFCellStyle spanhighlight = SafeCellStyle.create(wb);
        spanhighlight.setAlignment(CENTER);
        for (Entry<String, Object[][]> sheetAndGrid : grids.entrySet()) {
            Sheet sheet = wb.createSheet(sheetAndGrid.getKey());
            exportGridToSheet(sheetAndGrid.getValue(), sheet, alignCenterStyle, spanhighlight, highlight);
        }

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try {
            wb.write(bytesOut);
        } catch (IOException e) {
            LOG.error("XLS-bytes generointi epäonnistui, ei pitäisi tapahtua, koska ei oikea I/O?", e);
        }
        return bytesOut.toByteArray();
    }

    private static void exportGridToSheet(Object[][] grid, Sheet sheet, CellStyle spanStyle, CellStyle highlightSpanStyle, CellStyle highlightStyle) {
        int numberOfcolumns = 0;
        // Create rows!
        short rowIndex = 0;
        for (Object[] dataRow : grid) {
            assert (dataRow != null);
            Row excelRow = sheet.createRow(rowIndex);
            // Create columns!
            short cellIndex = 0;
            for (Object dataCell : dataRow) {
                if (dataCell == null) {
                    dataCell = StringUtils.EMPTY;
                }
                if (dataCell instanceof Span) {
                    // Span over multiple columns
                    Span span = (Span) dataCell;
                    Cell excelCell = excelRow.createCell(cellIndex);
                    if (span.isAlsoHighlight()) {
                        excelCell.setCellStyle(highlightSpanStyle);
                    } else {
                        excelCell.setCellStyle(spanStyle);
                    }
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, // first
                            // row
                            // (0-based)
                            rowIndex, // last row (0-based)
                            cellIndex, // first column (0-based)
                            cellIndex + span.getSpanColumns() - 1 // last column
                            // (0-based)
                    ));
                    excelCell.setCellValue(span.getText());
                    cellIndex += span.getSpanColumns();
                } else {
                    // Normal cell
                    numberOfcolumns = Math.max(numberOfcolumns, cellIndex);
                    Cell excelCell = excelRow.createCell(cellIndex);
                    if (dataCell instanceof Highlight) {
                        excelCell.setCellStyle(highlightStyle);
                    }
                    String value = dataCell.toString();
                    excelCell.setCellValue(value);
                    ++cellIndex;
                }
            }
            ++rowIndex;
        }
        // Auto size used columns!
        for (int column = 0; column <= numberOfcolumns; ++column) {
            sheet.autoSizeColumn(column);
        }
    }

    private static byte[] exportGridAsXlsBytes(Object[][] grid) {
        assert (grid != null);
        HSSFWorkbook wb = new HSSFWorkbook();
        CellStyle alignCenterStyle = SafeCellStyle.create(wb);
        alignCenterStyle.setAlignment(CENTER);
        HSSFCellStyle highlight = SafeCellStyle.create(wb);
        HSSFCellStyle spanhighlight = SafeCellStyle.create(wb);
        spanhighlight.setAlignment(CENTER);
        Sheet sheet = wb.createSheet(DATE_FORMAT.format(new Date()));
        exportGridToSheet(grid, sheet, alignCenterStyle, spanhighlight, highlight);

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try {
            wb.write(bytesOut);
        } catch (IOException e) {
            LOG.error("XLS-bytes generointi epäonnistui, ei pitäisi tapahtua, koska ei oikea I/O?", e);
        }
        return bytesOut.toByteArray();
    }

    public static InputStream exportGridAsXls(Object[][] grid) {
        return new ByteArrayInputStream(exportGridAsXlsBytes(grid));// bytesOut.newInputStream();
    }
}
