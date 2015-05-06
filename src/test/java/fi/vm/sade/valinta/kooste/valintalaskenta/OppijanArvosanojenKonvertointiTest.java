package fi.vm.sade.valinta.kooste.valintalaskenta;

import com.google.gson.GsonBuilder;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.YoToAvainSuoritustietoDTOConverter;
import fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Jussi Jartamo
 */
public class OppijanArvosanojenKonvertointiTest extends SuoritusrekisteriSpec {
    private static final Logger LOG = LoggerFactory.getLogger(OppijanArvosanojenKonvertointiTest.class);
    // UNOHDA AMMATTISTARTTI,

    // YO , PK , LK , LISAOPETUS(10), AMMATILLINEN

    // PK:
    // PK_TILA = false // default
    // PK-KESKEYTYNYT <- NÄMÄ POIS
    // PK-KAMOISSA JOS SUORITUKSIA USEEMPI KU YKS NIIN POIKKEUS JA PAKSUT VIRHEILMOITUKSET
//    @Test(expected = RuntimeException.class)
//    public void perusopetusVirheellinenKoskaUseampiSuoritus() {
//        Oppija virheellinenKoskaUseampiSuoritus = oppija()
//                .suoritus()
//                .setPerusopetus()
//                .setKesken()
//                .build()
//                .suoritus()
//                .setPerusopetus()
//                .setValmis()
//                .build()
//                .build();
//        OppijaToAvainArvoDTOConverter.convert(virheellinenKoskaUseampiSuoritus, null);
//    }

    @Test
    public void vainItseIlmoitettu() {
        Oppija oppija = oppija()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(false)
                .setValmistuminen("1.1.2013")
                .setValmis()
                .build()
                .suoritus()
                .setLukio()
                .setVahvistettu(false)
                .setValmistuminen("1.1.2013")
                .setValmis()
                .build()
                .build();

        final List<AvainArvoDTO> konvertoitu = OppijaToAvainArvoDTOConverter.convert(oppija, null);
        final String pk_suoritusvuosi = konvertoitu.stream()
                .filter(a -> a.getAvain().equals("PK_SUORITUSVUOSI"))
                .map(a -> a.getArvo())
                .findFirst().get();
        Assert.assertEquals("2013", pk_suoritusvuosi);

        final String lk_suoritusvuosi = konvertoitu.stream()
                .filter(a -> a.getAvain().equals("LK_SUORITUSVUOSI"))
                .map(a -> a.getArvo())
                .findFirst().get();
        Assert.assertEquals("2013", lk_suoritusvuosi);

    }

    @Test
    public void itseIlmoitettuJaValmis() {
        Oppija oppija = oppija()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(false)
                .setValmistuminen("1.1.2013")
                .setValmis()
                .build()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .suoritus()
                .setLukio()
                .setVahvistettu(false)
                .setValmistuminen("1.1.2013")
                .setValmis()
                .build()
                .suoritus()
                .setLukio()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .build();

        final List<AvainArvoDTO> konvertoitu = OppijaToAvainArvoDTOConverter.convert(oppija, null);
        final String pk_suoritusvuosi = konvertoitu.stream()
                .filter(a -> a.getAvain().equals("PK_SUORITUSVUOSI"))
                .map(a -> a.getArvo())
                .findFirst().get();
        Assert.assertEquals("2015", pk_suoritusvuosi);

        final String lk_suoritusvuosi = konvertoitu.stream()
                .filter(a -> a.getAvain().equals("LK_SUORITUSVUOSI"))
                .map(a -> a.getArvo())
                .findFirst().get();
        Assert.assertEquals("2015", lk_suoritusvuosi);

    }

    @Test
    public void itseIlmoitettuJaKaksiValmista() {
        Oppija oppija = oppija()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(false)
                .setValmistuminen("1.1.2013")
                .setValmis()
                .build()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2014")
                .setValmis()
                .build()
                .suoritus()
                .setLukio()
                .setVahvistettu(false)
                .setValmistuminen("1.1.2013")
                .setValmis()
                .build()
                .suoritus()
                .setLukio()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2014")
                .setValmis()
                .build()
                .suoritus()
                .setLukio()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .build();

        final List<AvainArvoDTO> konvertoitu = OppijaToAvainArvoDTOConverter.convert(oppija, null);
        final String pk_suoritusvuosi = konvertoitu.stream()
                .filter(a -> a.getAvain().equals("PK_SUORITUSVUOSI"))
                .map(a -> a.getArvo())
                .findFirst().get();
        Assert.assertEquals("2015", pk_suoritusvuosi);

        final String lk_suoritusvuosi = konvertoitu.stream()
                .filter(a -> a.getAvain().equals("LK_SUORITUSVUOSI"))
                .map(a -> a.getArvo())
                .findFirst().get();
        Assert.assertEquals("2015", lk_suoritusvuosi);

    }

    @Test
    public void useampiSuoritusJoistaEiOllaKiinnostuneita() {
        Oppija virheellinenKoskaUseampiSuoritus = oppija()
                .suoritus()
                .setKomo("koulutus_732101")
                .setKesken()
                .build()
                .suoritus()
                .setKomo("koulutus_732101")
                .setValmis()
                .build()
                .suoritus()
                .setKomo("koulutus_671101")
                .setKesken()
                .build()
                .suoritus()
                .setKomo("koulutus_671101")
                .setValmis()
                .build()
                .build();
        OppijaToAvainArvoDTOConverter.convert(virheellinenKoskaUseampiSuoritus, null);
    }

    @Test
    public void perusopetusOikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia() {
        Oppija oikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia = oppija()
                .suoritus()
                .setPerusopetus()
                .setKeskeytynyt()
                .build()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .build()
                .suoritus()
                .setPerusopetus()
                .setKeskeytynyt()
                .build()
                .build();
        OppijaToAvainArvoDTOConverter.convert(oikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia, null);
    }

    @Test
    public void perusopetusSuoritusvuosiJaSuorituskausi() {
        /*
        PK_SUORITUSLUKUKAUSI = 1/2
        1.1. - 31.7. ->  2
        1.8. -> 31.12. -> 1
                */
        {
            Oppija suoritus = oppija()
                    .suoritus()
                    .setPerusopetus()
                    .setValmistuminen("31.07.2008")
                    .setValmis()
                    .build()
                    .build();

            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, null);
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_SUORITUSVUOSI löytyy ja sen arvo on 2008",
                    aa.stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2008".equals(a.getArvo())).count() == 1L);
            Assert.assertTrue("PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 2",
                    aa.stream().filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()) && "2".equals(a.getArvo())).count() == 1L);
        }
        {
            Oppija suoritus = oppija()
                    .suoritus()
                    .setPerusopetus()
                    .setValmistuminen("01.01.2018")
                    .setValmis()
                    .build()
                    .build();

            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, null);
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_SUORITUSVUOSI löytyy ja sen arvo on 2018",
                    aa.stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2018".equals(a.getArvo())).count() == 1L);
            Assert.assertTrue("PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 2",
                    aa.stream().filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()) && "2".equals(a.getArvo())).count() == 1L);
        }
        {
            Oppija suoritus = oppija()
                    .suoritus()
                    .setPerusopetus()
                    .setValmistuminen("01.08.2008")
                    .setValmis()
                    .build()
                    .build();

            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, null);
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_SUORITUSVUOSI löytyy ja sen arvo on 2008",
                    aa.stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2008".equals(a.getArvo())).count() == 1L);
            Assert.assertTrue("PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 1",
                    aa.stream().filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()) && "1".equals(a.getArvo())).count() == 1L);
        }
        {
            Oppija suoritus = oppija()
                    .suoritus()
                    .setPerusopetus()
                    .setValmistuminen("31.12.2008")
                    .setValmis()
                    .build()
                    .build();

            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, null);
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_SUORITUSVUOSI löytyy ja sen arvo on 2008",
                    aa.stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2008".equals(a.getArvo())).count() == 1L);
            Assert.assertTrue("PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 1",
                    aa.stream().filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()) && "1".equals(a.getArvo())).count() == 1L);
        }
    }
    @Test
    public void perusopetusOikeinKoskaVainValmisSuoritus() {
        Oppija oikeinKoskaVainValmisSuoritus = oppija()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(oikeinKoskaVainValmisSuoritus, null);
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_TILA löytyy ja sen arvo on true",
                    aa.stream().filter(a -> "PK_TILA".equals(a.getAvain()) && "true".equals(a.getArvo())).count() == 1L);
        }
    }

    @Test
    public void pkTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {

        Oppija oikeinKoskaEiSuorituksia = oppija()
                .suoritus()
                .setPerusopetus()
                .setKeskeytynyt()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(oikeinKoskaEiSuorituksia, null);
            Assert.assertTrue("PK_TILA ei löydy",
                    aa.stream().filter(a -> "PK_TILA".equals(a.getAvain())).count() == 0L);
        }
    }

    @Test
    public void pkSuoritusArvosanojenPoisFiltterointi() {
        DateTime nyt = DateTime.now();
        Oppija suoritus = oppija()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("S")
                .setMyonnetty(nyt)
                .build()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, laskennanalkamisparametri(nyt.plusDays(1)));
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_AI ei löydy ja sen arvo ei ole S",
                    aa.stream().filter(a -> "PK_AI".equals(a.getAvain()) && "S".equals(a.getArvo())).count() == 0L);
            Assert.assertTrue("PK_AI_SUORITETTU löytyy arvolla true",
                    aa.stream().filter(a -> "PK_AI_SUORITETTU".equals(a.getAvain()) && "true".equals(a.getArvo())).count() == 1L);

        }
    }
    @Test
    public void pkSamanAineenToistuessaKaytetaanParasta() {
        DateTime nyt = DateTime.now();
        Oppija suoritus = oppija()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("6")
                .setMyonnetty(nyt)
                .build()
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("8")
                .setMyonnetty(nyt)
                .build()
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("7")
                .setMyonnetty(nyt)
                .build()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, laskennanalkamisparametri(nyt.plusDays(1)));
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_AI löytyy ja sen arvo on 8",
                    aa.stream().filter(a -> "PK_AI".equals(a.getAvain()) && "8".equals(a.getArvo())).count() == 1L);
        }
    }

    @Test
    public void pkSamanArvosananToistuessaKaytetaanParastaPaitsiJosMuutOnValinnaisia() {
        Oppija suoritus = oppija()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("6")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setAsteikko_4_10()
                .setArvosana("8")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setAsteikko_4_10()
                .setArvosana("7")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setAsteikko_4_10()
                .setArvosana("9")
                .build()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, null);
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_AI löytyy ja sen arvo on 6",
                    aa.stream().filter(a -> "PK_AI".equals(a.getAvain()) && "6".equals(a.getArvo())).count() == 1L);

            Assert.assertTrue("PK_AI_VAL1 löytyy",
                    aa.stream().filter(a -> "PK_AI_VAL1".equals(a.getAvain())).count() == 1L);
            Assert.assertTrue("PK_AI_VAL2 löytyy",
                    aa.stream().filter(a -> "PK_AI_VAL2".equals(a.getAvain())).count() == 1L);
            Assert.assertTrue("PK_AI_VAL3 löytyy",
                    aa.stream().filter(a -> "PK_AI_VAL3".equals(a.getAvain())).count() == 1L);
        }
    }

    /**
     * Eri asteikot yhdistettavilla arvosanoilla. Yhdistaminen ei mahdollista.
     */
    @Test(expected = RuntimeException.class)
    public void pkSamaArvoMuttaAsteikotEiTasmaa() {
        Oppija samaArvoMuttaAsteikotEiTasmaa = oppija()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("6")
                .build()
                .arvosana()
                .setAine("AI")
                .setAsteikko_1_5()
                .setArvosana("8")
                .build()
                .build()
                .build();
        OppijaToAvainArvoDTOConverter.convert(samaArvoMuttaAsteikotEiTasmaa, null);
    }

    @Test
    public void pkTuntematonAsteikkoMuttaTunnistetaanNumeroksi() {
        Oppija tuntematonAsteikkoMuttaTunnistetaanNumeroksi = oppija()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("6")
                .build()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("8")
                .build()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa =  OppijaToAvainArvoDTOConverter.convert(tuntematonAsteikkoMuttaTunnistetaanNumeroksi, null);
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_AI löytyy ja sen arvo on 8",
                    aa.stream().filter(a -> "PK_AI".equals(a.getAvain()) && "8".equals(a.getArvo())).count() == 1L);
        }
    }

    /**
     * Poikkeus koska yhdistaminen ei mahdollista
     */
    @Test(expected = RuntimeException.class)
    public void pkTunnistamatonArvosana() {
        Oppija tunnistamatonArvosana = oppija()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("KUUS")
                .build()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("KAHDEKSAN")
                .build()
                .build()
                .build();
        OppijaToAvainArvoDTOConverter.convert(tunnistamatonArvosana, null);
    }

    @Test
    public void pkTunnistamatonArvosanaMuttaEiTarviYhdistaanNiinEiValia() {
        Oppija tunnistamatonArvosana = oppija()
                .suoritus()
                .setPerusopetus()
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("KUUS")
                .build()
                .build()
                .build();
        OppijaToAvainArvoDTOConverter.convert(tunnistamatonArvosana, null);
    }

    // LUKIO:
    // LK_TILA = false // defaut jos suorituksen tila on
    // SUORITUS VAAN KERRAN MYÖS LUKIO TOUHUISSA KOSKA TULEE YTL:LTÄ
    // LUKION ARVOSANA MONEEN KERTAAN JA PARASTA KÄYTETÄÄN

    @Test
    public void lkTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {

        Oppija oikeinKoskaEiSuorituksia = oppija()
                .suoritus()
                .setLukio()
                .setKeskeytynyt()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(oikeinKoskaEiSuorituksia, null);
            Assert.assertTrue("LK_TILA ei löydy",
                    aa.stream().filter(a -> "LK_TILA".equals(a.getAvain())).count() == 0L);
        }
    }

    @Test
    public void lkSuoritusVainKerran() {
        Oppija virheellinenKoskaUseampiSuoritus = oppija()
                .suoritus()
                .setLukio()
                .setKesken()
                .build()
                .suoritus()
                .setLukio()
                .setValmis()
                .build()
                .build();
        final List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(virheellinenKoskaUseampiSuoritus, null);
        Assert.assertTrue("LK_TILA löytyy ja sen arvo on true",
                aa.stream().filter(a -> "LK_TILA".equals(a.getAvain()) && "true".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void lkSamanArvosananToistuessaKaytetaanParasta() {
        Oppija tuntematonAsteikkoMuttaTunnistetaanNumeroksi = oppija()
                .suoritus()
                .setLukio()
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("6")
                .build()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("8")
                .build()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa =  OppijaToAvainArvoDTOConverter.convert(tuntematonAsteikkoMuttaTunnistetaanNumeroksi, null);
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("LK_AI löytyy ja sen arvo on 8",
                    aa.stream().filter(a -> "LK_AI".equals(a.getAvain()) && "8".equals(a.getArvo())).count() == 1L);
        }
    }

    // KYMPPILUOKKA:
    // PK_TILA_10 = false // default
    // VAAN YKS SUORITUS (LISÄOPETUS)
    // PK_***_10 SUFFIX
    @Test
    public void kymppiluokanKorotuksissaMyosKeskeytettySuoritusHuomioidaan() {
        Oppija oikeinKoskaKeskenerainenSuoritus = oppija()
                .suoritus()
                .setKymppiluokka()
                .setKeskeytynyt()
                .arvosana()
                .setAine("AI")
                .setArvosana("7")
                .build()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(oikeinKoskaKeskenerainenSuoritus, null);
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            Assert.assertTrue("PK_TILA_10 löytyy ja sen arvo on false",
                    aa.stream().filter(a -> "PK_TILA_10".equals(a.getAvain()) && "false".equals(a.getArvo())).count() == 1L);
            Assert.assertTrue("PK_AI_10 löytyy ja sen arvo on 7",
                    aa.stream().filter(a -> "PK_AI_10".equals(a.getAvain()) && "7".equals(a.getArvo())).count() == 1L);
        }
    }
    @Test
    public void kymppiluokkaTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {
        Oppija oikeinKoskaKeskenerainenSuoritus = oppija()
                .suoritus()
                .setKymppiluokka()
                .setKesken()
                .build()
                .build();
        {
            List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(oikeinKoskaKeskenerainenSuoritus, null);
            Assert.assertTrue("PK_TILA_10 löytyy ja sen arvo on false",
                    aa.stream().filter(a -> "PK_TILA_10".equals(a.getAvain()) && "false".equals(a.getArvo())).count() == 1L);
        }
    }

    @Test
    public void yoItseilmoitetutOhitetaan() {
        DateTime nyt = DateTime.now();

        Oppija suoritus = oppija()
                .suoritus()
                .setHenkiloOid("HENKILO1")
                .setSource("HENKILO1")
                .setYo()
                .setValmis()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("PS")
                .setAsteikko_yo()
                .setArvosana("M")
                .setPisteet(20)
                .setMyonnetty("01.08.2014")
                .build()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("PS")
                .setAsteikko_yo()
                .setArvosana("L")
                .setPisteet(120)
                .setMyonnetty("30.03.2015")
                .build()
                .build()
                .build();
        {
            List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
            Assert.assertTrue(aa.isEmpty());
        }
    }
    @Test
    public void yoArvosanatOtetaanHuomioonVaikkaKirjausOlisiTehtyLiianMyohaan() {
        DateTime nyt = DateTime.now();

        Oppija suoritus = oppija()
                .suoritus()
                .setYo()
                .setValmis()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("PS")
                .setAineyhdistelmarooli("41")
                .setAsteikko_yo()
                .setArvosana("M")
                .setPisteet(20)
                .setMyonnetty("01.08.2014")
                .build()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("PS")
                .setAineyhdistelmarooli("41")
                .setAsteikko_yo()
                .setArvosana("L")
                .setPisteet(120)
                .setMyonnetty("30.03.2015")
                .build()
                .build()
                .build();
        {
            List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));

            Assert.assertTrue("AINEREAALI löytyy ja se sisältää molemmat PS:t",
                    aa.stream().filter(a -> "AINEREAALI".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("AINEREAALI")
                                    .suoritustieto("LISATIETO", "PS")
                                    .suoritustieto("ARVO", "M")
                                    .suoritustieto("PISTEET", "20")
                                            .suoritustieto("SUORITUSVUOSI", "2014")
                                            .suoritustieto("SUORITUSLUKUKAUSI", "2")
                                            .suoritustieto("ROOLI", "41")
                                            .build()
                                    .suoritustieto("LISATIETO", "PS")
                                    .suoritustieto("ARVO", "L")
                                    .suoritustieto("PISTEET", "120")
                                            .suoritustieto("SUORITUSVUOSI", "2015")
                                            .suoritustieto("SUORITUSLUKUKAUSI", "1")
                                            .suoritustieto("ROOLI", "41")
                                    .build()
                                    .build()
                            .build())).count() == 1L);
            Assert.assertTrue("AINEREAALI löytyy ja se sisältää molemmat PS:t",
                    aa.stream().filter(a -> "PS".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("PS")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")
                                            .suoritustieto("SUORITUSVUOSI", "2014")
                                            .suoritustieto("SUORITUSLUKUKAUSI", "2")
                                            .suoritustieto("ROOLI", "41")
                                            .build()
                                            .suoritustieto("ARVO", "L")
                                            .suoritustieto("PISTEET", "120")
                                            .suoritustieto("SUORITUSVUOSI", "2015")
                                            .suoritustieto("SUORITUSLUKUKAUSI", "1")
                                            .suoritustieto("ROOLI", "41")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
        }
    }
    public boolean testEquality(List<Map<String,String>> a,List<Map<String,String>> b) {
        return a.stream().anyMatch(a0 -> b.stream().anyMatch(b0 -> b0.entrySet().containsAll(a0.entrySet())));
        //a.entrySet().containsAll(b.entrySet());
        /*
        Comparator<TreeMap<String,String>> comp = (m0,m1) -> {
            return 0;
        };
        Collections.sort(a, comp);
        Collections.sort(b, comp);
        */
    }

    public boolean testEquality(AvainMetatiedotDTO a, List<AvainMetatiedotDTO> aa) {
        return aa.stream()
                .filter(x -> x.getAvain().equals(a.getAvain()))
                .anyMatch(
                x -> testEquality(a.getMetatiedot(), x.getMetatiedot())
        );
    }

    @Test
    public void yoAinereaalinMolemmatArvosanatTuleeMukaan() {
        Oppija suoritus = oppija()
                .suoritus()
                .setYo()
                .setValmis()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("HI")
                .setAineyhdistelmarooli("41")
                .setAsteikko_yo()
                .setArvosana("M")
                .setPisteet(20)
                .build()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("FY")
                .setAineyhdistelmarooli("41")
                .setAsteikko_yo()
                .setArvosana("L")
                .setPisteet(120)
                .build()
                .build()
                .build();
        {
            List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));

            Assert.assertTrue("AINEREAALI löytyy ja se sisältää molemmat PS:t",
                    aa.stream().filter(a -> "AINEREAALI".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("AINEREAALI")
                                            .suoritustieto("LISATIETO", "HI")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")

                                            .suoritustieto("ROOLI", "41")
                                            .build()
                                            .suoritustieto("LISATIETO", "FY")

                                            .suoritustieto("ROOLI", "41")
                                            .suoritustieto("ARVO", "L")
                                            .suoritustieto("PISTEET", "120")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);

        }
    }

    @Test
    public void kaytetaanKoetunnustaAineenaJosAnnettu() {
        Oppija suoritus = oppija()
                .suoritus()
                .setYo()
                .setValmis()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("HI")
                .setKoetunnus("HI")
                .setAsteikko_yo()
                .setArvosana("M")
                .setPisteet(20)
                .build()
                .build()
                .build();
        {
            List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));

            Assert.assertTrue("HI löytyy",
                    aa.stream().filter(a -> "HI".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("HI")
                                            .suoritustieto("LISATIETO", "HI")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);

        }
    }

    @Test
    public void yoArvosanatRoolitukset() {
        {
            DateTime nyt = DateTime.now();
            Oppija suoritus = oppija()
                    .suoritus()
                    .setYo()
                    .setValmis()
                    .arvosana()
                    .setAine("A")
                    .setLisatieto("SA")
                    .setAineyhdistelmarooli("31")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(20)
                    .build()
                    .build()
                    .build();

            List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            // AIDINKIELI
            Assert.assertTrue("SA löytyy ja se sisältää oikeat roolit",
                    aa.stream().filter(a -> "SA".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("SA")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")
                                            .suoritustieto("ROOLI", "31")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
        }
        //
        {
            DateTime nyt = DateTime.now();
            Oppija suoritus = oppija()
                    .suoritus()
                    .setYo()
                    .setValmis()
                    .arvosana()
                    .setAine("VI2")
                    .setLisatieto("FI")
                    .setAineyhdistelmarooli("14")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(20)
                    //.setMyonnetty(nyt.minusDays(1))
                    .build()
                    .arvosana()
                    .setAine("VI2")
                    .setLisatieto("RU")
                    .setAineyhdistelmarooli("14")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(20)
                    //.setMyonnetty(nyt.minusDays(1))
                    .build()
                    .build()
                    .build();

            List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            // AIDINKIELI
            Assert.assertTrue("O5 löytyy ja se sisältää oikeat roolit",
                    aa.stream().filter(a -> "A5".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("A5")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")
                                            .suoritustieto("ROOLI", "14")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
            Assert.assertTrue("O5 löytyy ja se sisältää oikeat roolit",
                    aa.stream().filter(a -> "O5".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("O5")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")
                                            .suoritustieto("ROOLI", "14")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
            /*
            Assert.assertTrue("A5_ROOLI löytyy ja sen arvo on 14",
                    aa.stream().filter(a -> "A5_ROOLI".equals(a.getAvain()) && "14".equals(a.getArvo())).count() == 1L);
            Assert.assertTrue("O5_ROOLI löytyy ja sen arvo on 14",
                    aa.stream().filter(a -> "O5_ROOLI".equals(a.getAvain()) && "14".equals(a.getArvo())).count() == 1L);
            */
        }

        {
            DateTime nyt = DateTime.now();
            Oppija suoritus = oppija()
                    .suoritus()
                    .setYo()
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setLisatieto("FI")
                    .setAineyhdistelmarooli("11")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(20)
                    //.setMyonnetty(nyt.minusDays(1))
                    .build()
                    .build()
                    .build();
            // AIDINKIELI
            //Assert.assertTrue("A_ROOLI löytyy ja sen arvo on 11",
            //        aa.stream().filter(a -> "A_ROOLI".equals(a.getAvain()) && "11".equals(a.getArvo())).count() == 1L);

            List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            // AIDINKIELI
            Assert.assertTrue("A löytyy ja se sisältää oikeat roolit",
                    aa.stream().filter(a -> "A".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("A")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")
                                            .suoritustieto("ROOLI", "11")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
        }
        {
            DateTime nyt = DateTime.now();
            Oppija suoritus = oppija()
                    .suoritus()
                    .setYo()
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setLisatieto("ZA")
                    .setAineyhdistelmarooli("60")
                    .setAsteikko_yo()
                    // VAIN VALINNAISET VOI OLLA YLIMAARAISIA
                    .setValinnainen()
                    .setArvosana("M")
                    .setPisteet(20)
                    //.setMyonnetty(nyt.minusDays(1))
                    .build()

                    .arvosana()
                    .setAine("AI")
                    .setLisatieto("RU")
                    .setAineyhdistelmarooli("60")
                    .setAsteikko_yo()
                            // VAIN VALINNAISET VOI OLLA YLIMAARAISIA
                    .setValinnainen()
                    .setArvosana("M")
                    .setPisteet(20)
                    //.setMyonnetty(nyt.minusDays(1))
                    .build()

                    .arvosana()
                    .setAine("AI")
                    .setLisatieto("IS")
                    .setAineyhdistelmarooli("12")
                    .setAsteikko_yo()
                    .setArvosana("L")
                    .setPisteet(44)
                    //.setMyonnetty(nyt.minusDays(1))
                    .build()

                    .arvosana()
                    .setAine("KYPSYYS")
                    .setAsteikko_yo()
                    .setLisatieto("EN")
                    .setAineyhdistelmarooli("13")
                    .setArvosana("M")
                    .setPisteet(25)
                    //.setMyonnetty(nyt.minusDays(1))
                    .build()

                    .build()
                    .build();

            List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
            // AIDINKIELI
            Assert.assertTrue("J löytyy ja se sisältää oikeat roolit",
                    aa.stream().filter(a -> "J".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("J")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "25")
                                            .suoritustieto("ROOLI", "13")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
            Assert.assertTrue("I löytyy ja se sisältää oikeat roolit",
                    aa.stream().filter(a -> "I".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("I")
                                            .suoritustieto("ARVO", "L")
                                            .suoritustieto("PISTEET", "44")
                                            .suoritustieto("ROOLI", "12")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
            Assert.assertTrue("Z löytyy ja se sisältää oikeat roolit",
                    aa.stream().filter(a -> "Z".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("Z")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")
                                            .suoritustieto("ROOLI", "60")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
            Assert.assertTrue("O löytyy ja se sisältää oikeat roolit",
                    aa.stream().filter(a -> "O".equals(a.getAvain()) &&
                            testEquality(a,
                                    avainsuoritustieto("O")
                                            .suoritustieto("ARVO", "M")
                                            .suoritustieto("PISTEET", "20")
                                            .suoritustieto("ROOLI", "60")
                                            .build()
                                            .build()
                                            .build())).count() == 1L);
                // KYPSYYSNÄYTE ENGLANNILLE
                //Assert.assertTrue("J_ROOLI löytyy ja sen arvo on 13",
                //        aa.stream().filter(a -> "J_ROOLI".equals(a.getAvain()) && "13".equals(a.getArvo())).count() == 1L);
                // AIDINKIELI (SAAME)
                //Assert.assertTrue("I_ROOLI löytyy ja sen arvo on 12",
                //        aa.stream().filter(a -> "I_ROOLI".equals(a.getAvain()) && "12".equals(a.getArvo())).count() == 1L);
                // TOINEN AIDINKIELI
                //Assert.assertTrue("Z_ROOLI löytyy ja sen arvo on 60",
                //        aa.stream().filter(a -> "Z_ROOLI".equals(a.getAvain()) && "60".equals(a.getArvo())).count() == 1L);
                //Assert.assertTrue("Z_ROOLI löytyy ja sen arvo ei ole 12",
                //    aa.stream().filter(a -> "Z_ROOLI".equals(a.getAvain()) && "12".equals(a.getArvo())).count() == 0L);
                // TOINEN AIDINKIELI
                //Assert.assertTrue("O_ROOLI löytyy ja sen arvo on 60",
                //        aa.stream().filter(a -> "O_ROOLI".equals(a.getAvain()) && "60".equals(a.getArvo())).count() == 1L);
        }

    }

    @Test
    public void osakoeArvosanat() {
        {
            Oppija suoritus = oppija()
                    .suoritus()
                    .setYo()
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setLisatieto("FI")
                    .setAineyhdistelmarooli("11")
                    .setAsteikko_yo()
                    .setArvosana("L")
                    .setPisteet(23)
                    .setMyonnetty("01.08.2000")
                    .build()

                    .arvosana()
                    .setAine("AI")
                    .setLisatieto("FI")
                    .setAineyhdistelmarooli("11")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(30)
                    .setMyonnetty("01.08.2000")
                    .build()

                    .arvosana()
                    .setAine("AI_01")
                    .setLisatieto("FI")
                    .setAsteikko_Osakoe()
                    .setArvosana("45")
                    .setMyonnetty("01.08.2000")
                    .build()

                    .arvosana()
                    .setAine("AI_01")
                    .setLisatieto("FI")
                    .setAsteikko_Osakoe()
                    .setArvosana("15")
                    .setMyonnetty("01.08.2000")
                    .build()
                    .build()
                    .build();
            {
                List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
                //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
                Assert.assertTrue("A löytyy ja se sisältää molemmat",
                        aa.stream().filter(a -> "A".equals(a.getAvain()) &&
                                testEquality(a,
                                        avainsuoritustieto("A")
                                                .suoritustieto("ARVO", "L")
                                                .suoritustieto("PISTEET", "23")
                                                .suoritustieto("ROOLI", "11")
                                                .suoritustieto("SUORITUSVUOSI", "2000")
                                                .suoritustieto("SUORITUSLUKUKAUSI", "2")
                                                .build()

                                                .suoritustieto("ARVO", "M")
                                                .suoritustieto("PISTEET", "30")
                                                .suoritustieto("ROOLI", "11")
                                                .suoritustieto("SUORITUSVUOSI", "2000")
                                                .suoritustieto("SUORITUSLUKUKAUSI", "2")
                                                .build()
                                                .build()
                                                .build())).count() == 1L);
                Assert.assertTrue("01 löytyy",
                        aa.stream().filter(a -> "01".equals(a.getAvain()) &&
                                testEquality(a,
                                        avainsuoritustieto("01")
                                                .suoritustieto("ARVO", "45")
                                                .suoritustieto("SUORITUSVUOSI", "2000")
                                                .suoritustieto("SUORITUSLUKUKAUSI", "2")
                                                .build()
                                                .suoritustieto("ARVO", "15")
                                                .suoritustieto("SUORITUSVUOSI", "2000")
                                                .suoritustieto("SUORITUSLUKUKAUSI", "2")
                                                .build()
                                                .build()
                                                .build())).count() == 1L);

            }
        }
        {
            DateTime nyt = DateTime.now();
            Oppija suoritus = oppija()
                    .suoritus()
                    .setYo()
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setLisatieto("FI")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(20)
                    .setMyonnetty(nyt.minusDays(1))
                    .build()

                    .arvosana()
                    .setAine("AI_R233") // joku osakoetunnus mista ei olla kiinnostuneita
                    .setLisatieto("FI")
                    .setAsteikko_Osakoe()
                    .setArvosana("45")
                    .setMyonnetty(nyt.plusDays(1))
                    .build()

                    .build()
                    .build();
            {

                List<AvainMetatiedotDTO> aa = YoToAvainSuoritustietoDTOConverter.convert(suoritus);
                LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
                //List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, laskennanalkamisparametri(nyt));
                Assert.assertTrue("R233 roolia ei saa löytyä koska ei olla kiinnostuneita tällaisesta roolista",
                        aa.stream().filter(a -> "R233".equals(a.getAvain())).count() == 0L);
            }
        }
    }

    @Test
    public void yoTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {
        {
            Oppija suoritus = oppija()
                    .suoritus()
                    .setYo()
                    .setValmis()
                    .arvosana()
                    .setAine("AINEREAALI")
                    .setLisatieto("PS")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(20)
                    .build()
                    .build()
                    .build();
            {
                // YO-tila tulee edelleen merkitä vaikka yo-arvosanat laitetaan uuteen tietueeseen AvainSuoritusTietoDTO
                List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, null);
                //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
                Assert.assertTrue("YO_TILA löytyy ja sen arvo on true",
                        aa.stream().filter(a -> "YO_TILA".equals(a.getAvain()) && "true".equals(a.getArvo())).count() == 1L);
            }
        }
        {
            Oppija suoritus = oppija()
                    .suoritus()
                    .setYo()
                    .setKesken()
                    .arvosana()
                    .setAine("AINEREAALI")
                    .setLisatieto("PS")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(20)
                    .build()
                    .build()
                    .suoritus()
                    .setYo()
                    .setKeskeytynyt()
                    .arvosana()
                    .setAine("AINEREAALI")
                    .setLisatieto("PS")
                    .setAsteikko_yo()
                    .setArvosana("M")
                    .setPisteet(20)
                    .build()
                    .build()
                    .build();
            {
                List<AvainArvoDTO> aa = OppijaToAvainArvoDTOConverter.convert(suoritus, null);
                //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(aa));
                Assert.assertTrue("YO_TILA löytyy ja sen arvo on false",
                        aa.stream().filter(a -> "YO_TILA".equals(a.getAvain()) && "false".equals(a.getArvo())).count() == 1L);
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void yoSuorituksiaUseampiKuinYksiNiinHeitetaanKuvaavaPoikkeus() {
        Oppija suoritus = oppija()
                .suoritus()
                .setYo()
                .setKesken()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("PS")
                .setAsteikko_yo()
                .setArvosana("M")
                .setPisteet(20)
                .build()
                .build()
                .suoritus()
                .setYo()
                .setKesken()
                .arvosana()
                .setAine("AINEREAALI")
                .setLisatieto("PS")
                .setAsteikko_yo()
                .setArvosana("M")
                .setPisteet(20)
                .build()
                .build()
                .build();
        OppijaToAvainArvoDTOConverter.convert(suoritus, null);
    }

    // AMMATILLINEN:
    // SUORITUKSIA USEAMPI
    // EI VOI MITENKÄÄN MAPATA
    // OLETKO SUORITTANUT AMMATILLISEN TUTKINNON
    // ---> OTETAAN PARAS
    // OHITETAAN

    @Test
    public void ammatillinenOhitetaanToistaiseksi() {
        // Speksi viela auki
    }


}
