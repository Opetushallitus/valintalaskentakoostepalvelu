package fi.vm.sade.valinta.kooste.excel;

import com.google.common.collect.Lists;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

import java.util.Collection;

/**
 * @author Jussi Jartamo
 */
public class HSSFRivi {

    private HSSFRivi() {
    }

    private static Collection<Solu> soluiksi(HSSFRow row) {
        Collection<Solu> solut = Lists.newArrayList();
        int lastCellIndex = row.getLastCellNum();
        for (int i = 0; i < lastCellIndex; ++i) {
            HSSFCell cell = row.getCell(i, Row.CREATE_NULL_AS_BLANK);
            solut.add(HSSFSolu.asSolu(cell));
        }
        return solut;
    }

    public static Rivi asRivi(HSSFRow row) {
        return new Rivi(soluiksi(row));
    }
}
