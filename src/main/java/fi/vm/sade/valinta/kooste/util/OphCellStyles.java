package fi.vm.sade.valinta.kooste.util;

import static org.apache.poi.ss.usermodel.CellType.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

public class OphCellStyles {
  private static final List<CellType> cellTypesWithoutDangerousContent =
      Arrays.asList(NUMERIC, BOOLEAN, ERROR);
  private final CellStyle quotePrefixStyle;
  private final CellStyle unsafeStyle;

  public OphCellStyles(Workbook workbook) {
    this(workbook.createCellStyle(), workbook.createCellStyle());
  }

  protected OphCellStyles(CellStyle quotePrefixStyle, CellStyle unsafeStyle) {
    this.quotePrefixStyle = quotePrefixStyle;
    quotePrefixStyle.setQuotePrefixed(true);
    this.unsafeStyle = unsafeStyle;
  }

  public Cell apply(Cell cell) {
    if (FORMULA.equals(cell.getCellType())) {
      throw new IllegalArgumentException(
          "Are you sure you want to create a " + FORMULA + " cell? " + cell);
    }
    if (cellTypesWithoutDangerousContent.contains(cell.getCellType())) {
      cell.setCellStyle(unsafeStyle);
    } else {
      String value = cell.getStringCellValue();
      if (StringUtils.startsWithAny(value, "=", "@", "-", "+")) {
        cell.setCellStyle(quotePrefixStyle);
      } else {
        cell.setCellStyle(unsafeStyle);
      }
    }
    return cell;
  }

  public Row apply(Row row) {
    row.setRowStyle(
        unsafeStyle); // This should affect only new cells when workbook is created, not override
    // single cell styles
    return row;
  }

  public void visit(Consumer<CellStyle> visitor) {
    visitor.accept(quotePrefixStyle);
    visitor.accept(unsafeStyle);
  }

  public CellStyle getQuotePrefixStyle() {
    return quotePrefixStyle;
  }

  public CellStyle getUnsafeStyle() {
    return unsafeStyle;
  }
}
