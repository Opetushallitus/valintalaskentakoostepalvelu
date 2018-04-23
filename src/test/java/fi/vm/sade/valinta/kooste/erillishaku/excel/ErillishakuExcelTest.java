package fi.vm.sade.valinta.kooste.erillishaku.excel;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.excel.Excel;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ErillishakuExcelTest {

    private static List<ErillishakuRivi> createErillishakuRivisWithTwoValidRows() {
        List<ErillishakuRivi> rivit = Lists.newArrayList();
        String syntymaAika = "11.11.2011";
        ErillishakuRivi rivi = new ErillishakuRiviBuilder()
                .sukunimi("sukunimi")
                .etunimi("etunimi1")
                .henkilotunnus("hetu")
                .sahkoposti("test.email@example.com")
                .syntymaAika(syntymaAika)
                .sukupuoli(Sukupuoli.MIES)
                .personOid("")
                .aidinkieli("FI")
                .hakemuksenTila("HYLATTY")
                .ehdollisestiHyvaksyttavissa(false)
                .hyvaksymiskirjeLahetetty(null)
                .vastaanottoTila("")
                .ilmoittautumisTila("")
                .julkaistaankoTiedot(false)
                .poistetaankoRivi(false)
                .asiointikieli("FI")
                .puhelinnumero("040123456789")
                .osoite("Esimerkkitie 2")
                .postinumero("00100")
                .postitoimipaikka("HELSINKI")
                .asuinmaa("FIN")
                .kansalaisuus("FIN")
                .kotikunta("HELSINKI")
                .toisenAsteenSuoritus(true)
                .toisenAsteenSuoritusmaa("SWE")
                .maksuvelvollisuus(Maksuvelvollisuus.NOT_CHECKED)
                .build();
        rivit.add(rivi);
        ErillishakuRivi rivi2 = new ErillishakuRiviBuilder()
                .sukunimi("sukunimi")
                .etunimi("etunimi2")
                .henkilotunnus("hetu")
                .sahkoposti("test.email@example.com")
                .syntymaAika(syntymaAika)
                .sukupuoli(Sukupuoli.NAINEN)
                .personOid("")
                .aidinkieli("FI")
                .hakemuksenTila("HYLATTY")
                .ehdollisestiHyvaksyttavissa(true)
                .ehdollisenHyvaksymisenEhtoKoodi("muu")
                .ehdollisenHyvaksymisenEhtoFI("Muu")
                .ehdollisenHyvaksymisenEhtoSV("Annan")
                .ehdollisenHyvaksymisenEhtoEN("Other")
                .hyvaksymiskirjeLahetetty(null)
                .vastaanottoTila("")
                .ilmoittautumisTila("")
                .julkaistaankoTiedot(true)
                .poistetaankoRivi(false)
                .asiointikieli("FI")
                .puhelinnumero("040123456789")
                .osoite("Esimerkkitie 2")
                .postinumero("00100")
                .postitoimipaikka("HELSINKI")
                .asuinmaa("FIN")
                .kansalaisuus("FIN")
                .kotikunta("HELSINKI")
                .toisenAsteenSuoritus(true)
                .toisenAsteenSuoritusmaa("FIN")
                .maksuvelvollisuus(Maksuvelvollisuus.REQUIRED)
                .build();
        rivit.add(rivi2);
        return rivit;
    }

    @Test
    public void testaaTuontiKustomistaTiedostosta() throws Exception {
        final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
        ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", Collections.emptyList(), rivi -> tarkistusTapahtui.incrementAndGet());
        eExcel.getExcel().tuoXlsx(new ClassPathResource("kustom_erillishaku.xlsx").getInputStream());
        assertEquals(1, tarkistusTapahtui.get());
    }

    @Test
    public void testaaTuontiKustomistaTiedostostaOtsikoilla() throws Exception {
        final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
        ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi",Collections.emptyList(), rivi -> tarkistusTapahtui.incrementAndGet());
        eExcel.getExcel().tuoXlsx(new ClassPathResource("kustom_erillishaku_otsikoilla.xlsx").getInputStream());
        assertEquals(1, tarkistusTapahtui.get());
    }
    
    @Test
    public void testaaVienti() throws Exception {
        List<ErillishakuRivi> rivit = createErillishakuRivisWithTwoValidRows();
        ErillishakuRivi rivi3 = new ErillishakuRiviBuilder().build();
        rivit.add(rivi3);
        final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
        ErillishakuExcel eExcel = new ErillishakuExcel(null, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", rivit, rv -> tarkistusTapahtui.incrementAndGet());
        Excel excel = eExcel.getExcel();
        excel.tuoXlsx(excel.vieXlsx());

        assertEquals(2, tarkistusTapahtui.get());
        // Tulosta tiedostoksi testausta varten
        // IOUtils.copy(excel.vieXlsx(), new FileOutputStream("erillishaku.xlsx"));
    }

    @Test
    public void testaaVientiKKWithValidRows() throws Exception {
        List<ErillishakuRivi> rivit = createErillishakuRivisWithTwoValidRows();
        final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
        ErillishakuExcel eExcel = new ErillishakuExcel(Hakutyyppi.KORKEAKOULU, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", rivit, rv -> tarkistusTapahtui.incrementAndGet());
        Excel excel = eExcel.getExcel();
        excel.tuoXlsx(excel.vieXlsx());

        assertEquals(2, tarkistusTapahtui.get());

        // Tulosta tiedostoksi testausta varten
         IOUtils.copy(excel.vieXlsx(), new FileOutputStream("erillishaku.xlsx"));
    }

    @Test
    public void testaaVientiKKWithInvalidRow() throws Exception {
        List<ErillishakuRivi> rivit = createErillishakuRivisWithTwoValidRows();
        ErillishakuRivi rivi3 = new ErillishakuRiviBuilder().sukunimi("X-sukunimi").etunimi("X-etunimi").build();
        rivit.add(rivi3);
        final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
        ErillishakuExcel eExcel = new ErillishakuExcel(Hakutyyppi.KORKEAKOULU, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", rivit, rv -> tarkistusTapahtui.incrementAndGet());
        Excel excel = eExcel.getExcel();
        excel.tuoXlsx(excel.vieXlsx());

        assertEquals(
                2 // tavalliset rivit
                + 1 // epävalidi
                , tarkistusTapahtui.get());

        // Tulosta tiedostoksi testausta varten
        // IOUtils.copy(excel.vieXlsx(), new FileOutputStream("erillishaku.xlsx"));
    }

    @Test
    public void testaaVientiToinenAste() throws Exception {
        List<ErillishakuRivi> rivit = createErillishakuRivisWithTwoValidRows();
        ErillishakuRivi rivi3 = new ErillishakuRiviBuilder().build();
        rivit.add(rivi3);
        final AtomicInteger tarkistusTapahtui = new AtomicInteger(0);
        ErillishakuExcel eExcel = new ErillishakuExcel(Hakutyyppi.TOISEN_ASTEEN_OPPILAITOS, "Haun nimi", "Hakukohteen nimi", "Tarjoajan nimi", rivit, rv -> tarkistusTapahtui.incrementAndGet());
        Excel excel = eExcel.getExcel();
        excel.tuoXlsx(excel.vieXlsx());

        assertEquals(2, tarkistusTapahtui.get());

        // Tulosta tiedostoksi testausta varten
        // IOUtils.copy(excel.vieXlsx(), new FileOutputStream("erillishaku.xlsx"));
    }
}
