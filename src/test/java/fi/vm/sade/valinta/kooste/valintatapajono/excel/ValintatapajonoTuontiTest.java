package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import static fi.vm.sade.valinta.http.DateDeserializer.GSON;

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

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

/**
 * @author Jussi Jartamo
 */
public class ValintatapajonoTuontiTest {
    private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoTuontiTest.class);
    private static final Type valinnanVaiheListType = new TypeToken<ArrayList<ValintatietoValinnanvaiheDTO>>() {}.getType();
    private static final Type hakemusListType = new TypeToken<ArrayList<Hakemus>>() {}.getType();


    // @Ignore
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
        // Excel excel = valintatapajonoExcel.getExcel();
        // if (false) {
        // IOUtils.copy(excel.vieXlsx(), new FileOutputStream(
        // "valintatapajono.xlsx"));
        // }
    }

    private static String resurssi(String resurssi) throws IOException {
        InputStream i;
        String s = IOUtils.toString(i = new ClassPathResource("valintatapajono/" + resurssi).getInputStream(), "UTF-8");
        IOUtils.closeQuietly(i);
        return s;
    }
}
