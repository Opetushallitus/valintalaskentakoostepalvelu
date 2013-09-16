package fi.vm.sade.valinta.kooste.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public class ExcelPalveluTest {

    @Test
    public void testaaXlsTiedostonLuontia() throws IOException {
        final String RANDOM_DATAA = "RANDOM DATAA!";
        List<Object[]> rivit = new ArrayList<Object[]>();
        rivit.add(new Object[] { RANDOM_DATAA, RANDOM_DATAA, RANDOM_DATAA });
        rivit.add(new Object[] { RANDOM_DATAA, RANDOM_DATAA, RANDOM_DATAA, RANDOM_DATAA });
        rivit.add(new Object[] { RANDOM_DATAA, RANDOM_DATAA });

        InputStream xlsData = ExcelExportUtil.exportGridAsXls(rivit.toArray(new Object[][] {}));
        Assert.assertTrue(IOUtils.toByteArray(xlsData).length >= 0);
    }

}
