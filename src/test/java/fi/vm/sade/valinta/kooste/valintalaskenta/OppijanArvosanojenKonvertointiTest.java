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
    public void aineyhdistelmäroolimuunnin() {
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
                .setAineyhdistelmarooli("")
                .setAine("AINEREAALI")
                .setLisatieto("PS")
                .setAsteikko_yo()
                .setArvosana("L")
                .setPisteet(120)
                .setMyonnetty("30.03.2015")
                .build()
                .build()
                .build();
        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(suoritus));
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
    public void ammatillinenOhitetaanToistaiseksi() {
        // Speksi viela auki
    }


}
