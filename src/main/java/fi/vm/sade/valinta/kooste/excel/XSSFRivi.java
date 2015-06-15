package fi.vm.sade.valinta.kooste.excel;

import java.util.Collection;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

import com.google.common.collect.Lists;

/**
 *         XSSFRow -> Rivi
 */
public class XSSFRivi {

    private XSSFRivi() {
    }

    private static Collection<Solu> soluiksi(XSSFRow row) {
        Collection<Solu> solut = Lists.newArrayList();
        int lastCellIndex = row.getLastCellNum();
        for (int i = 0; i < lastCellIndex; ++i) {
            XSSFCell cell = row.getCell(i, Row.CREATE_NULL_AS_BLANK);
            solut.add(XSSFSolu.asSolu(cell));
        }
        return solut;
    }

    public static Rivi asRivi(XSSFRow row) {
        return new Rivi(soluiksi(row));
    }
}
