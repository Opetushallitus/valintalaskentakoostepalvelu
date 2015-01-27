package fi.vm.sade.valinta.kooste.erillishaku.util;

import com.google.common.collect.Lists;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import org.apache.poi.util.IOUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fi.vm.sade.valinta.kooste.erillishaku.util.ValidoiTilatUtil.validoi;

/**
 * @author Jussi Jartamo
 */
public class GeneroiTilaKombinaatiot {

    private static <T> List<T> andNull(T[] t) {
        List<T> t0 = Lists.newArrayList(Arrays.asList(t));
        t0.add(null);
        return t0;//.toArray();
    }


    public static void main(String[] args) throws IOException {
        try {
            List<Object[]> oks = Lists.newArrayList();
            List<Object[]> noks = Lists.newArrayList();
            for (HakemuksenTila ht : andNull(HakemuksenTila.values())) {
                for (ValintatuloksenTila vt : andNull(ValintatuloksenTila.values())) {
                    for (IlmoittautumisTila it : andNull(IlmoittautumisTila.values())) {
                        String errcode = validoi(ht, vt, it);
                        if (errcode == null) {
                            oks.add(new Object[]{toString(ht), toString(vt), toString(it)});
                        } else {
                            noks.add(new Object[]{toString(ht), toString(vt), toString(it), errcode});
                        }
                    }
                }
            }
            List<Object[]> grid = Lists.newArrayList();
            grid.add(new Object[]{"OKS " + oks.size() + ", NOKS " + noks.size()});
            grid.add(new Object[]{"#########", "NOK", "#########"});
            grid.addAll(noks);
            grid.add(new Object[]{""});
            grid.add(new Object[]{"#########","OK","#########"});
            grid.addAll(oks);

            IOUtils.copy(
                    ExcelExportUtil.exportGridAsXls(grid.toArray(new Object[][]{})),
                    new FileOutputStream("kombinaatiot.xls")
            );
        } catch(Exception e) {
            System.err.println("D: " + e.getMessage());
            throw e;

        }finally {
            System.err.println("DONE");
        }
    }

    private static String toString(HakemuksenTila hakemuksenTila) {
        if(hakemuksenTila == null) {
            return "TYHJÄ";
        }
        return hakemuksenTila.toString();
    }

    private static String toString(ValintatuloksenTila valintatuloksenTila) {
        if(valintatuloksenTila == null) {
            return "TYHJÄ";
        }
        return valintatuloksenTila.toString();
    }

    private static String toString(IlmoittautumisTila ilmoittautumisTila) {
        if(ilmoittautumisTila == null) {
            return "TYHJÄ";
        }
        return ilmoittautumisTila.toString();
    }
}
