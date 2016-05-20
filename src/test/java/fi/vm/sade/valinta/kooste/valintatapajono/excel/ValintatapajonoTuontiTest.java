package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.valintatapajono.ValintatapajonoTestTools;

import static fi.vm.sade.valinta.http.DateDeserializer.GSON;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

import static org.junit.Assert.assertEquals;

/**
 * @author Jussi Jartamo
 */
public class ValintatapajonoTuontiTest extends ValintatapajonoTestTools {
    private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoTuontiTest.class);
    private static final Type valinnanVaiheListType = new TypeToken<ArrayList<ValintatietoValinnanvaiheDTO>>() {}.getType();
    private static final Type hakemusListType = new TypeToken<ArrayList<Hakemus>>() {}.getType();


    @Test
    public void testaaValintatapajonoTuonti() throws JsonSyntaxException, IOException {
        List<ValintatietoValinnanvaiheDTO> valinnanvaihe = GSON.fromJson(resurssi("valinnanvaihe.json"), valinnanVaiheListType);
        List<Hakemus> hakemukset = GSON.fromJson(resurssi("listfull.json"), hakemusListType);
        ValintatapajonoDataRiviListAdapter listaus = new ValintatapajonoDataRiviListAdapter();
        ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel("1.2.246.562.5.2013080813081926341927", "1.2.246.562.14.2013082110450143806511", "14017934785463582418268204255542", "Haun Nimi", "Hakukohteen Nimi", valinnanvaihe, hakemukset, Arrays.asList(listaus));
        valintatapajonoExcel.getExcel().tuoXlsx(new ClassPathResource("valintatapajono/valintatapajono.xlsx").getInputStream());
        for (ValintatapajonoRivi r : listaus.getRivit()) {
            LOG.info("{} {} {} {}", r.getJonosija(), r.getNimi(), r.isValidi(), r.getTila());
        }
        assertLines(listaus.getRivit());

        /*Excel excel = valintatapajonoExcel.getExcel();
        if (true) {
        IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
        "valintatapajonofii.xlsx"));
        }*/
    }

    private void assertLines(List<ValintatapajonoRivi> rivit) {
        assertEquals(46, rivit.size());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000860732", "1.0", "Huisnen Elina",
                        ValintatapajonoExcel.HYVAKSYTTAVISSA, "", null, null, null),
                rivit.stream().filter(r -> "Huisnen Elina".equals(r.getNimi())).findFirst().get());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000873703", "5.0", "Hoppusalo Pinja",
                        ValintatapajonoExcel.HYLATTY, "", "fuyf", "uyf", "ft"),
                rivit.stream().filter(r -> "Hoppusalo Pinja".equals(r.getNimi())).findFirst().get());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000856717", "", "Huisvaara Eveliina",
                        ValintatapajonoExcel.HYLATTY, "", "msfhgm", "asfdgv", "sd"),
                rivit.stream().filter(r -> "Huisvaara Eveliina".equals(r.getNimi())).findFirst().get());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000846727", "", "Kaksilahti Kasper",
                        ValintatapajonoExcel.MAARITTELEMATON, "", null, null, null),
                rivit.stream().filter(r -> "Kaksilahti Kasper".equals(r.getNimi())).findFirst().get());
    }

    @Test
    public void testaaValintatapajonoKokonaispisteetTuonti() throws JsonSyntaxException, IOException {
        List<ValintatietoValinnanvaiheDTO> valinnanvaihe = GSON.fromJson(resurssi("valinnanvaihe.json"), valinnanVaiheListType);
        List<Hakemus> hakemukset = GSON.fromJson(resurssi("listfull.json"), hakemusListType);
        ValintatapajonoDataRiviListAdapter listaus = new ValintatapajonoDataRiviListAdapter();
        ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel("1.2.246.562.5.2013080813081926341927", "1.2.246.562.14.2013082110450143806511", "14017934785463582418268204255542", "Haun Nimi", "Hakukohteen Nimi", valinnanvaihe, hakemukset, Arrays.asList(listaus));
        valintatapajonoExcel.getExcel().tuoXlsx(new ClassPathResource("valintatapajono/valintatapajono_kokonaispisteet.xlsx").getInputStream());
        for (ValintatapajonoRivi r : listaus.getRivit()) {
            LOG.info("{} {} {} {}", r.getJonosija(), r.getNimi(), r.isValidi(), r.getTila());
        }
        assertKokonaispisteLines(listaus.getRivit());

        /*Excel excel = valintatapajonoExcel.getExcel();
        if (true) {
        IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
        "valintatapajonofii.xlsx"));
        }*/
    }

    private void assertKokonaispisteLines(List<ValintatapajonoRivi> rivit) {
        assertEquals(46, rivit.size());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000860732", "", "Huisnen Elina",
                ValintatapajonoExcel.HYVAKSYTTAVISSA, "50", null, null, null),
                rivit.stream().filter(r -> "Huisnen Elina".equals(r.getNimi())).findFirst().get());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000873703", "", "Hoppusalo Pinja",
                        ValintatapajonoExcel.HYLATTY, "43", "fuyf", "uyf", "ft"),
                rivit.stream().filter(r -> "Hoppusalo Pinja".equals(r.getNimi())).findFirst().get());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000856717", "", "Huisvaara Eveliina",
                        ValintatapajonoExcel.HYLATTY, "", "msfhgm", "asfdgv", "sd"),
                rivit.stream().filter(r -> "Huisvaara Eveliina".equals(r.getNimi())).findFirst().get());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000846727", "", "Kaksilahti Kasper",
                        ValintatapajonoExcel.MAARITTELEMATON, "", null, null, null),
                rivit.stream().filter(r -> "Kaksilahti Kasper".equals(r.getNimi())).findFirst().get());
    }

    private static String resurssi(String resurssi) throws IOException {
        InputStream i;
        String s = IOUtils.toString(i = new ClassPathResource("valintatapajono/" + resurssi).getInputStream(), "UTF-8");
        IOUtils.closeQuietly(i);
        return s;
    }
}
