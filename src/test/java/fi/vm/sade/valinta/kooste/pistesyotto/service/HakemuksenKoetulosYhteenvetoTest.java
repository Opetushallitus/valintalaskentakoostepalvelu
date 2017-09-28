package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.ei_osallistunut;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemuksenKoetulosYhteenveto;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.Osallistumistieto;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.OsallistuminenTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

@Ignore
public class HakemuksenKoetulosYhteenvetoTest {
    private static final ValintaperusteDTO kielikoeFi = new ValintaperusteDTO();
    private static final ValintaperusteDTO kielikoeSv = new ValintaperusteDTO();

    static {
        kielikoeFi.setTunniste("kielikoe_fi");
        kielikoeFi.setOsallistuminenTunniste("kielikoe_fi-OSALLISTUMINEN");
        kielikoeFi.setVaatiiOsallistumisen(true);
        kielikoeFi.setSyotettavissaKaikille(false);
        kielikoeSv.setTunniste("kielikoe_sv");
        kielikoeSv.setOsallistuminenTunniste("kielikoe_sv-OSALLISTUMINEN");
        kielikoeSv.setVaatiiOsallistumisen(true);
        kielikoeSv.setSyotettavissaKaikille(false);
    }

    @Test
    public void testKaikkiValintakoeavaimetAsetetaanAdditionalDataan() {
        HashMap<String, String> additionalData = new HashMap<>();
        additionalData.put("hakemuksella", "arvo_hakemuksella");
        ValintaperusteDTO vaatiiOsallistumisen = new ValintaperusteDTO();
        vaatiiOsallistumisen.setTunniste("vaatii_osallistumisen");
        vaatiiOsallistumisen.setOsallistuminenTunniste("vaatii_osallistumisen-OSALLISTUMINEN");
        vaatiiOsallistumisen.setVaatiiOsallistumisen(true);
        ValintaperusteDTO eiVaadiOsallistumista = new ValintaperusteDTO();
        eiVaadiOsallistumista.setTunniste("ei_vaadi_osallistumista");
        eiVaadiOsallistumista.setOsallistuminenTunniste("ei_vaadi_osallistumista-OSALLISTUMINEN");
        eiVaadiOsallistumista.setVaatiiOsallistumisen(false);
        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", additionalData),
                Pair.of("hakukohdeOid", Arrays.asList(vaatiiOsallistumisen, eiVaadiOsallistumista)),
                new ValintakoeOsallistuminenDTO(),
                new Oppija(),
                new ParametritDTO()
        );
        assertEquals("arvo_hakemuksella", p.applicationAdditionalDataDTO.getAdditionalData().get("hakemuksella"));
        assertEquals("", p.applicationAdditionalDataDTO.getAdditionalData().get("vaatii_osallistumisen"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("vaatii_osallistumisen-OSALLISTUMINEN"));
        assertEquals("", p.applicationAdditionalDataDTO.getAdditionalData().get("ei_vaadi_osallistumista"));
        assertEquals(Osallistuminen.EI_VAADITA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("ei_vaadi_osallistumista-OSALLISTUMINEN"));
    }

    @Test
    public void testKielikoetiedotAsetetaanSuoritusrekisterinTiedoista() {
        Oppija oppija = new Oppija();
        SuoritusJaArvosanat kielikoesuoritus = new SuoritusJaArvosanat();
        Suoritus s = new Suoritus();
        s.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        s.setMyontaja("hakemusOid");
        kielikoesuoritus.setSuoritus(s);
        Arvosana a = new Arvosana();
        a.setLisatieto("FI");
        Arvio arvio = new Arvio();
        arvio.setArvosana(hyvaksytty.name());
        a.setArvio(arvio);
        kielikoesuoritus.setArvosanat(Collections.singletonList(a));
        oppija.setSuoritukset(Collections.singletonList(kielikoesuoritus));
        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                new ValintakoeOsallistuminenDTO(),
                oppija,
                new ParametritDTO()
        );
        assertEquals("true", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.OSALLISTUI.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
    }

    @Test
    public void testHylattyKielikoetulosAsetetaanVainSamanHakemuksenSuorituksenTiedoista() {
        Oppija oppija = new Oppija();

        SuoritusJaArvosanat samanHakemuksenSuoritus = new SuoritusJaArvosanat();
        Suoritus sFi = new Suoritus();
        sFi.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sFi.setMyontaja("hakemusOid");
        samanHakemuksenSuoritus.setSuoritus(sFi);
        Arvosana aFi = new Arvosana();
        aFi.setLisatieto("FI");
        Arvio arvioFi = new Arvio();
        arvioFi.setArvosana(hylatty.name());
        aFi.setArvio(arvioFi);
        samanHakemuksenSuoritus.setArvosanat(Collections.singletonList(aFi));

        SuoritusJaArvosanat eriHakemuksenSuoritus = new SuoritusJaArvosanat();
        Suoritus sSv = new Suoritus();
        sSv.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sSv.setMyontaja("eriHakemusOid");
        eriHakemuksenSuoritus.setSuoritus(sSv);
        Arvosana aSv = new Arvosana();
        aSv.setLisatieto("SV");
        Arvio arvioSv = new Arvio();
        arvioSv.setArvosana(hylatty.name());
        aSv.setArvio(arvioSv);
        eriHakemuksenSuoritus.setArvosanat(Collections.singletonList(aSv));

        oppija.setSuoritukset(Arrays.asList(samanHakemuksenSuoritus, eriHakemuksenSuoritus));
        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeSv)),
                new ValintakoeOsallistuminenDTO(),
                oppija,
                new ParametritDTO()
        );
        assertEquals("false", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.OSALLISTUI.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
        assertEquals("", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_sv"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_sv-OSALLISTUMINEN"));
    }

    @Test
    public void testEiOsallistunutKielikoetulosAsetetaanVainSamanHakemuksenSuorituksenTiedoista() {
        Oppija oppija = new Oppija();

        SuoritusJaArvosanat samanHakemuksenSuoritus = new SuoritusJaArvosanat();
        Suoritus sFi = new Suoritus();
        sFi.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sFi.setMyontaja("hakemusOid");
        samanHakemuksenSuoritus.setSuoritus(sFi);
        Arvosana aFi = new Arvosana();
        aFi.setLisatieto("FI");
        Arvio arvioFi = new Arvio();
        arvioFi.setArvosana(ei_osallistunut.name());
        aFi.setArvio(arvioFi);
        samanHakemuksenSuoritus.setArvosanat(Collections.singletonList(aFi));

        SuoritusJaArvosanat eriHakemuksenSuoritus = new SuoritusJaArvosanat();
        Suoritus sSv = new Suoritus();
        sSv.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sSv.setMyontaja("eriHakemusOid");
        eriHakemuksenSuoritus.setSuoritus(sSv);
        Arvosana aSv = new Arvosana();
        aSv.setLisatieto("SV");
        Arvio arvioSv = new Arvio();
        arvioSv.setArvosana(ei_osallistunut.name());
        aSv.setArvio(arvioSv);
        eriHakemuksenSuoritus.setArvosanat(Collections.singletonList(aSv));

        oppija.setSuoritukset(Arrays.asList(samanHakemuksenSuoritus, eriHakemuksenSuoritus));
        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeSv)),
                new ValintakoeOsallistuminenDTO(),
                oppija,
                new ParametritDTO()
        );
        assertEquals("", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.EI_OSALLISTUNUT.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
        assertEquals("", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_sv"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_sv-OSALLISTUMINEN"));
    }

    @Test
    public void testKaytaUusintaSuoritusrekisteristaLoytyvaaArvosanaa() {
        Oppija oppija = new Oppija();

        SuoritusJaArvosanat vanhempiSuoritus = new SuoritusJaArvosanat();
        Suoritus sV = new Suoritus();
        sV.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sV.setMyontaja("eriHakemusOid");
        vanhempiSuoritus.setSuoritus(sV);
        Arvosana aV = new Arvosana();
        aV.setLisatieto("FI");
        aV.setMyonnetty("30.01.2000");
        aV.setSource("organisaatioOid1");
        Arvio arvioV = new Arvio();
        arvioV.setArvosana(hyvaksytty.name());
        aV.setArvio(arvioV);
        vanhempiSuoritus.setArvosanat(Collections.singletonList(aV));

        SuoritusJaArvosanat uudempiSuoritus = new SuoritusJaArvosanat();
        Suoritus sU = new Suoritus();
        sU.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sU.setMyontaja("eriHakemusOid");
        uudempiSuoritus.setSuoritus(sU);
        Arvosana aU = new Arvosana();
        aU.setLisatieto("FI");
        aU.setMyonnetty("31.01.2000");
        aU.setSource("organisaatioOid2");
        Arvio arvioU = new Arvio();
        arvioU.setArvosana(hyvaksytty.name());
        aU.setArvio(arvioU);
        uudempiSuoritus.setArvosanat(Collections.singletonList(aU));

        oppija.setSuoritukset(Arrays.asList(uudempiSuoritus, vanhempiSuoritus));

        ValintakoeOsallistuminenDTO osallistuminen = new ValintakoeOsallistuminenDTO();
        HakutoiveDTO h = new HakutoiveDTO();
        h.setHakukohdeOid("hakukohdeOid");
        ValintakoeValinnanvaiheDTO vv = new ValintakoeValinnanvaiheDTO();
        ValintakoeDTO koe = new ValintakoeDTO();
        koe.setValintakoeTunniste("kielikoe_fi");
        OsallistuminenTulosDTO o = new OsallistuminenTulosDTO();
        o.setOsallistuminen(fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.EI_OSALLISTU);
        koe.setOsallistuminenTulos(o);
        vv.setValintakokeet(Collections.singletonList(koe));
        h.setValinnanVaiheet(Collections.singletonList(vv));
        osallistuminen.setHakutoiveet(Collections.singletonList(h));

        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                osallistuminen,
                oppija,
                new ParametritDTO()
        );
        assertEquals("true", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
        assertEquals(Osallistumistieto.TOISELLA_HAKEMUKSELLA, p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
        assertEquals("organisaatioOid2", p.osallistumistieto("hakukohdeOid", "kielikoe_fi").lahdeMyontajaOid.get());
    }

    @Test
    public void testOsallistumistietoVaikkeiOsallistumistaJosSyotettavissaKaikille() {
        ValintaperusteDTO syotettavissaKaikille = new ValintaperusteDTO();
        syotettavissaKaikille.setTunniste("syotettavissa_kaikille");
        syotettavissaKaikille.setSyotettavissaKaikille(true);
        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(syotettavissaKaikille)),
                null,
                null,
                new ParametritDTO()
        );
        assertEquals(Osallistumistieto.OSALLISTUI, p.osallistumistieto("hakukohdeOid", "syotettavissa_kaikille").osallistumistieto);
    }

    @Test
    public void testValintalaskennanAlkamisenJalkeenMyonnettyjaKielikoetuloksiaEiKayteta() throws ParseException {
        ValintakoeOsallistuminenDTO osallistuminen = new ValintakoeOsallistuminenDTO();
        HakutoiveDTO h = new HakutoiveDTO();
        h.setHakukohdeOid("hakukohdeOid");
        ValintakoeValinnanvaiheDTO vv = new ValintakoeValinnanvaiheDTO();
        ValintakoeDTO koe = new ValintakoeDTO();
        koe.setValintakoeTunniste("kielikoe_fi");
        OsallistuminenTulosDTO o = new OsallistuminenTulosDTO();
        o.setOsallistuminen(fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.OSALLISTUU);
        koe.setOsallistuminenTulos(o);
        vv.setValintakokeet(Collections.singletonList(koe));
        h.setValinnanVaiheet(Collections.singletonList(vv));
        osallistuminen.setHakutoiveet(Collections.singletonList(h));

        SuoritusJaArvosanat suoritus = new SuoritusJaArvosanat();
        Suoritus s = new Suoritus();
        s.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        s.setMyontaja("hakemusOid");
        suoritus.setSuoritus(s);
        Arvosana a = new Arvosana();
        a.setLisatieto("FI");
        a.setMyonnetty("31.01.2000");
        a.setSource("organisaatioOid1");
        Arvio arvio = new Arvio();
        arvio.setArvosana(hyvaksytty.name());
        a.setArvio(arvio);
        suoritus.setArvosanat(Collections.singletonList(a));

        Oppija oppija = new Oppija();
        oppija.setSuoritukset(Collections.singletonList(suoritus));

        ParametritDTO ohjausparametrit = new ParametritDTO();
        ParametriDTO ph_vls = new ParametriDTO();
        ph_vls.setDateStart((new SimpleDateFormat("dd.MM.yyyy").parse("30.01.2000")));
        ohjausparametrit.setPH_VLS(ph_vls);

        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                osallistuminen,
                oppija,
                ohjausparametrit
        );
        assertEquals(Osallistumistieto.OSALLISTUI, p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
        assertEquals("", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
    }

    @Test
    public void testAsetaToiseltaHakemukseltaTulleenKielikoetuloksenOsallistumiseksiMerkitsematta() {
        Oppija oppija = new Oppija();
        SuoritusJaArvosanat kielikoesuoritus = new SuoritusJaArvosanat();
        Suoritus s = new Suoritus();
        s.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        s.setMyontaja("toinenHakemusOid");
        kielikoesuoritus.setSuoritus(s);
        Arvosana a = new Arvosana();
        a.setLisatieto("FI");
        Arvio arvio = new Arvio();
        arvio.setArvosana(hyvaksytty.name());
        a.setArvio(arvio);
        kielikoesuoritus.setArvosanat(Collections.singletonList(a));
        oppija.setSuoritukset(Collections.singletonList(kielikoesuoritus));

        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                new ValintakoeOsallistuminenDTO(),
                oppija,
                new ParametritDTO()
        );
        assertEquals("true", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
    }

    @Test
    public void testToisenHakukohteenTyhjaTietoNaytetaanMuodossaMerkitsemattaToisessaHakutoiveessa() {
        Oppija oppija = new Oppija();
        oppija.setSuoritukset(Collections.emptyList());
        
        ValintakoeOsallistuminenDTO valintakoeOsallistuminenDTO = new ValintakoeOsallistuminenDTO();

        HakutoiveDTO hakutoiveDto = new HakutoiveDTO();
        hakutoiveDto.setHakukohdeOid("hakukohdeOid");
        ValintakoeValinnanvaiheDTO vv = new ValintakoeValinnanvaiheDTO();
        ValintakoeDTO koe = new ValintakoeDTO();
        koe.setValintakoeTunniste("kielikoe_fi");
        OsallistuminenTulosDTO o = new OsallistuminenTulosDTO();
        o.setOsallistuminen(fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.OSALLISTUU);
        koe.setOsallistuminenTulos(o);
        vv.setValintakokeet(Collections.singletonList(koe));
        hakutoiveDto.setValinnanVaiheet(Collections.singletonList(vv));

        HakutoiveDTO toinenHakutoiveDto = new HakutoiveDTO();
        toinenHakutoiveDto.setHakukohdeOid("toinenHakukohdeOid");
        ValintakoeValinnanvaiheDTO vv2 = new ValintakoeValinnanvaiheDTO();
        ValintakoeDTO koe2 = new ValintakoeDTO();
        koe2.setValintakoeTunniste("kielikoe_fi");
        OsallistuminenTulosDTO o2 = new OsallistuminenTulosDTO();
        o2.setOsallistuminen(fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.EI_OSALLISTU);
        koe2.setOsallistuminenTulos(o2);
        vv2.setValintakokeet(Collections.singletonList(koe2));
        toinenHakutoiveDto.setValinnanVaiheet(Collections.singletonList(vv2));

        valintakoeOsallistuminenDTO.setHakutoiveet(Arrays.asList(hakutoiveDto, toinenHakutoiveDto));
        
        
        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                valintakoeOsallistuminenDTO,
                oppija,
                new ParametritDTO()
        );

        assertEquals(Osallistumistieto.OSALLISTUI, p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
        assertEquals(Osallistumistieto.TOISESSA_HAKUTOIVEESSA, p.osallistumistieto("toinenHakukohdeOid", "kielikoe_fi").osallistumistieto);

        assertEquals("", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals("MERKITSEMATTA", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
    }

    @Test
    public void testHyvaksyttyArvosanaEriHakemukseltaPiilottaaHylatynArvosananValintalaskennanJalkeenVaikkaSeOlisiUudempi() {
        Oppija oppija = new Oppija();

        SuoritusJaArvosanat hyvaksyttySuoritus = new SuoritusJaArvosanat();
        Suoritus sHyv = new Suoritus();
        sHyv.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sHyv.setMyontaja("hyvaksyttyHakemusOid");
        hyvaksyttySuoritus.setSuoritus(sHyv);
        Arvosana aHyv = new Arvosana();
        aHyv.setLisatieto("FI");
        aHyv.setMyonnetty("01.04.2016");
        aHyv.setSource("hyvaksyttyOrganisaatioOid");
        Arvio arvioHyv = new Arvio();
        arvioHyv.setArvosana(hyvaksytty.name());
        aHyv.setArvio(arvioHyv);
        hyvaksyttySuoritus.setArvosanat(Collections.singletonList(aHyv));

        SuoritusJaArvosanat hylattySuoritus = new SuoritusJaArvosanat();
        Suoritus sHyl = new Suoritus();
        sHyl.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sHyl.setMyontaja("hylattyHakemusOid");
        hylattySuoritus.setSuoritus(sHyl);
        Arvosana aHyl = new Arvosana();
        aHyl.setLisatieto("FI");
        aHyl.setMyonnetty("02.04.2016");
        aHyl.setSource("hylattyOrganisaatioOid");
        Arvio arvioHyl = new Arvio();
        arvioHyl.setArvosana(hylatty.name());
        aHyl.setArvio(arvioHyl);
        hylattySuoritus.setArvosanat(Collections.singletonList(aHyl));

        oppija.setSuoritukset(Arrays.asList(hylattySuoritus, hyvaksyttySuoritus));

        ValintakoeOsallistuminenDTO hylatynOsallistuminen = new ValintakoeOsallistuminenDTO();
        HakutoiveDTO h = new HakutoiveDTO();
        h.setHakukohdeOid("hakukohdeOid");
        ValintakoeValinnanvaiheDTO vv = new ValintakoeValinnanvaiheDTO();
        ValintakoeDTO koe = new ValintakoeDTO();
        koe.setValintakoeTunniste("kielikoe_fi");
        OsallistuminenTulosDTO o = new OsallistuminenTulosDTO();
        o.setOsallistuminen(fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.EI_OSALLISTU);
        koe.setOsallistuminenTulos(o);
        vv.setValintakokeet(Collections.singletonList(koe));
        h.setValinnanVaiheet(Collections.singletonList(vv));
        hylatynOsallistuminen.setHakutoiveet(Collections.singletonList(h));

        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hylattyHakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                hylatynOsallistuminen,
                oppija,
                new ParametritDTO()
        );
        assertEquals("true", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
        assertEquals(Osallistumistieto.TOISELLA_HAKEMUKSELLA, p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
        assertEquals("hyvaksyttyOrganisaatioOid", p.osallistumistieto("hakukohdeOid", "kielikoe_fi").lahdeMyontajaOid.get());
    }

    @Test
    public void testVirheTilainenKoekutsuSamaKuinEiOsallistu() {
        ValintakoeOsallistuminenDTO osallistuminen = new ValintakoeOsallistuminenDTO();
        HakutoiveDTO h = new HakutoiveDTO();
        h.setHakukohdeOid("hakukohdeOid");
        ValintakoeValinnanvaiheDTO vv = new ValintakoeValinnanvaiheDTO();
        ValintakoeDTO koe = new ValintakoeDTO();
        koe.setValintakoeTunniste("kielikoe_fi");
        OsallistuminenTulosDTO o = new OsallistuminenTulosDTO();
        o.setOsallistuminen(fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.VIRHE);
        koe.setOsallistuminenTulos(o);
        vv.setValintakokeet(Collections.singletonList(koe));
        h.setValinnanVaiheet(Collections.singletonList(vv));
        osallistuminen.setHakutoiveet(Collections.singletonList(h));

        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                osallistuminen,
                null,
                new ParametritDTO()
        );
        assertTrue(p.applicationAdditionalDataDTO.getAdditionalData().containsKey("kielikoe_fi"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
        assertEquals(Osallistumistieto.EI_KUTSUTTU, p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
    }

    @Test
    public void testSyotettavissaKaikilleArvoSyotettavissaVaikkaVirheTilainenKoekutsu() {
        ValintakoeOsallistuminenDTO osallistuminen = new ValintakoeOsallistuminenDTO();
        HakutoiveDTO h = new HakutoiveDTO();
        h.setHakukohdeOid("hakukohdeOid");
        ValintakoeValinnanvaiheDTO vv = new ValintakoeValinnanvaiheDTO();
        ValintakoeDTO koe = new ValintakoeDTO();
        koe.setValintakoeTunniste("kielikoe_fi");
        OsallistuminenTulosDTO o = new OsallistuminenTulosDTO();
        o.setOsallistuminen(fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.VIRHE);
        koe.setOsallistuminenTulos(o);
        vv.setValintakokeet(Collections.singletonList(koe));
        h.setValinnanVaiheet(Collections.singletonList(vv));
        osallistuminen.setHakutoiveet(Collections.singletonList(h));

        ValintaperusteDTO vp = new ValintaperusteDTO();
        vp.setTunniste("kielikoe_fi");
        vp.setOsallistuminenTunniste("kielikoe_fi-OSALLISTUMINEN");
        vp.setVaatiiOsallistumisen(true);
        vp.setSyotettavissaKaikille(true);

        HakemuksenKoetulosYhteenveto p = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(vp)),
                osallistuminen,
                null,
                new ParametritDTO()
        );
        assertTrue(p.applicationAdditionalDataDTO.getAdditionalData().containsKey("kielikoe_fi"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
        assertEquals(Osallistumistieto.OSALLISTUI, p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
    }

    @Test
    public void testKaytaTaltaHakemukseltaTulevaaHyvaksyttyaArvosanaaJosUseitaHyvaksyttyja() {
        Oppija oppija = new Oppija();

        SuoritusJaArvosanat todistusToinen = new SuoritusJaArvosanat();
        Suoritus sToinen = new Suoritus();
        sToinen.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sToinen.setMyontaja("toinenHakemusOid");
        todistusToinen.setSuoritus(sToinen);
        Arvosana aToinen = new Arvosana();
        aToinen.setMyonnetty("02.02.2016");
        aToinen.setLisatieto("FI");
        Arvio arvioToinen = new Arvio();
        arvioToinen.setArvosana(hyvaksytty.name());
        aToinen.setArvio(arvioToinen);
        todistusToinen.setArvosanat(Collections.singletonList(aToinen));

        SuoritusJaArvosanat todistusTama = new SuoritusJaArvosanat();
        Suoritus sTama = new Suoritus();
        sTama.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        sTama.setMyontaja("hakemusOid");
        todistusTama.setSuoritus(sTama);
        Arvosana aTama = new Arvosana();
        aTama.setMyonnetty("01.01.2016");
        aTama.setLisatieto("FI");
        Arvio arvioTama = new Arvio();
        arvioTama.setArvosana(hyvaksytty.name());
        aTama.setArvio(arvioTama);
        todistusTama.setArvosanat(Collections.singletonList(aTama));

        oppija.setSuoritukset(Arrays.asList(todistusToinen, todistusTama));

        HakemuksenKoetulosYhteenveto toinenEnsin = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                new ValintakoeOsallistuminenDTO(),
                oppija,
                new ParametritDTO()
        );
        assertEquals("true", toinenEnsin.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.OSALLISTUI.toString(), toinenEnsin.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));

        oppija.setSuoritukset(Arrays.asList(todistusTama, todistusToinen));

        HakemuksenKoetulosYhteenveto tamaEnsin = new HakemuksenKoetulosYhteenveto(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                new ValintakoeOsallistuminenDTO(),
                oppija,
                new ParametritDTO()
        );
        assertEquals("true", tamaEnsin.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.OSALLISTUI.toString(), tamaEnsin.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));

    }
}
