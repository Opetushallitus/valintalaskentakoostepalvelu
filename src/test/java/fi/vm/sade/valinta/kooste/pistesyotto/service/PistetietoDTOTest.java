package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.Osallistumistieto;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.PistetietoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.*;
import static org.junit.Assert.assertEquals;

public class PistetietoDTOTest {
    private static final ValintaperusteDTO kielikoeFi = new ValintaperusteDTO();
    private static final ValintaperusteDTO kielikoeSv = new ValintaperusteDTO();

    static {
        kielikoeFi.setTunniste("kielikoe_fi");
        kielikoeFi.setOsallistuminenTunniste("kielikoe_fi-OSALLISTUMINEN");
        kielikoeFi.setVaatiiOsallistumisen(true);
        kielikoeSv.setTunniste("kielikoe_sv");
        kielikoeSv.setOsallistuminenTunniste("kielikoe_sv-OSALLISTUMINEN");
        kielikoeSv.setVaatiiOsallistumisen(true);
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
        PistetietoDTO p = new PistetietoDTO(
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
        PistetietoDTO p = new PistetietoDTO(
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
        PistetietoDTO p = new PistetietoDTO(
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
        PistetietoDTO p = new PistetietoDTO(
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

        oppija.setSuoritukset(Arrays.asList(vanhempiSuoritus, uudempiSuoritus));

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

        PistetietoDTO p = new PistetietoDTO(
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
    public void testOsallistumistietoVaikkeOsallistumistaJosSyotettavissaKaikille() {
        ValintaperusteDTO syotettavissaKaikille = new ValintaperusteDTO();
        syotettavissaKaikille.setTunniste("syotettavissa_kaikille");
        syotettavissaKaikille.setSyotettavissaKaikille(true);
        PistetietoDTO p = new PistetietoDTO(
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

        PistetietoDTO p = new PistetietoDTO(
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

        PistetietoDTO p = new PistetietoDTO(
                new ApplicationAdditionalDataDTO("hakemusOid", "personOid", "etunimi", "sukunimi", new HashMap<>()),
                Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
                new ValintakoeOsallistuminenDTO(),
                oppija,
                new ParametritDTO()
        );
        assertEquals("true", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
        assertEquals(Osallistuminen.MERKITSEMATTA.toString(), p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
    }
}
