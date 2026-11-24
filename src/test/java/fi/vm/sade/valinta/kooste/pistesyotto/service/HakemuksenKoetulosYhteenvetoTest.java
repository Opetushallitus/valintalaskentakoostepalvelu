package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Piste;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemuksenKoetulosYhteenveto;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.Osallistumistieto;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.OsallistuminenTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

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
  public void testKielikoetulosLuetaanValintapisteista() {
    HakemuksenKoetulosYhteenveto p =
        new HakemuksenKoetulosYhteenveto(
            new Valintapisteet(
                "hakemusOid",
                "personOid",
                "etunimi",
                "sukunimi",
                Collections.singletonList(
                    new Piste(
                        "kielikoe_fi",
                        "true",
                        fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Osallistuminen
                            .OSALLISTUI,
                        "tallettaja"))),
            Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
            new ValintakoeOsallistuminenDTO());
    assertEquals("true", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
    assertEquals(
        Osallistuminen.OSALLISTUI.toString(),
        p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
  }

  @Test
  public void testKaikkiValintakoeavaimetAsetetaanAdditionalDataan() {
    ValintaperusteDTO vaatiiOsallistumisen = new ValintaperusteDTO();
    vaatiiOsallistumisen.setTunniste("vaatii_osallistumisen");
    vaatiiOsallistumisen.setOsallistuminenTunniste("vaatii_osallistumisen-OSALLISTUMINEN");
    vaatiiOsallistumisen.setVaatiiOsallistumisen(true);
    vaatiiOsallistumisen.setSyotettavissaKaikille(false);
    ValintaperusteDTO eiVaadiOsallistumista = new ValintaperusteDTO();
    eiVaadiOsallistumista.setTunniste("ei_vaadi_osallistumista");
    eiVaadiOsallistumista.setOsallistuminenTunniste("ei_vaadi_osallistumista-OSALLISTUMINEN");
    eiVaadiOsallistumista.setVaatiiOsallistumisen(false);
    eiVaadiOsallistumista.setSyotettavissaKaikille(false);
    HakemuksenKoetulosYhteenveto p =
        new HakemuksenKoetulosYhteenveto(
            new Valintapisteet(
                "hakemusOid", "personOid", "etunimi", "sukunimi", Collections.emptyList()),
            Pair.of("hakukohdeOid", Arrays.asList(vaatiiOsallistumisen, eiVaadiOsallistumista)),
            new ValintakoeOsallistuminenDTO());
    assertEquals(
        Map.of(
            "vaatii_osallistumisen",
            "",
            "vaatii_osallistumisen-OSALLISTUMINEN",
            Osallistuminen.MERKITSEMATTA.toString(),
            "ei_vaadi_osallistumista",
            "",
            "ei_vaadi_osallistumista-OSALLISTUMINEN",
            Osallistuminen.EI_VAADITA.toString()),
        p.applicationAdditionalDataDTO.getAdditionalData());
  }

  @Test
  public void testOsallistumistietoVaikkeiOsallistumistaJosSyotettavissaKaikille() {
    ValintaperusteDTO syotettavissaKaikille = new ValintaperusteDTO();
    syotettavissaKaikille.setTunniste("syotettavissa_kaikille");
    syotettavissaKaikille.setSyotettavissaKaikille(true);
    HakemuksenKoetulosYhteenveto p =
        new HakemuksenKoetulosYhteenveto(
            new Valintapisteet(
                "hakemusOid", "personOid", "etunimi", "sukunimi", Collections.emptyList()),
            Pair.of("hakukohdeOid", Collections.singletonList(syotettavissaKaikille)),
            null);
    assertEquals(
        Osallistumistieto.OSALLISTUI,
        p.osallistumistieto("hakukohdeOid", "syotettavissa_kaikille").osallistumistieto);
  }

  @Test
  public void testToisenHakukohteenTyhjaTietoNaytetaanMuodossaMerkitsemattaToisessaHakutoiveessa() {
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

    HakemuksenKoetulosYhteenveto p =
        new HakemuksenKoetulosYhteenveto(
            new Valintapisteet(
                "hakemusOid", "personOid", "etunimi", "sukunimi", Collections.emptyList()),
            Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
            valintakoeOsallistuminenDTO);

    assertEquals(
        Osallistumistieto.OSALLISTUI,
        p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
    assertEquals(
        Osallistumistieto.TOISESSA_HAKUTOIVEESSA,
        p.osallistumistieto("toinenHakukohdeOid", "kielikoe_fi").osallistumistieto);

    assertEquals("", p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi"));
    assertEquals(
        "MERKITSEMATTA",
        p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
    assertEquals(
        Osallistuminen.MERKITSEMATTA.toString(),
        p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
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

    HakemuksenKoetulosYhteenveto p =
        new HakemuksenKoetulosYhteenveto(
            new Valintapisteet(
                "hakemusOid", "personOid", "etunimi", "sukunimi", Collections.emptyList()),
            Pair.of("hakukohdeOid", Collections.singletonList(kielikoeFi)),
            osallistuminen);
    assertTrue(p.applicationAdditionalDataDTO.getAdditionalData().containsKey("kielikoe_fi"));
    assertEquals(
        Osallistuminen.MERKITSEMATTA.toString(),
        p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
    assertEquals(
        Osallistumistieto.EI_KUTSUTTU,
        p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
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

    HakemuksenKoetulosYhteenveto p =
        new HakemuksenKoetulosYhteenveto(
            new Valintapisteet(
                "hakemusOid", "personOid", "etunimi", "sukunimi", Collections.emptyList()),
            Pair.of("hakukohdeOid", Collections.singletonList(vp)),
            osallistuminen);
    assertTrue(p.applicationAdditionalDataDTO.getAdditionalData().containsKey("kielikoe_fi"));
    assertEquals(
        Osallistuminen.MERKITSEMATTA.toString(),
        p.applicationAdditionalDataDTO.getAdditionalData().get("kielikoe_fi-OSALLISTUMINEN"));
    assertEquals(
        Osallistumistieto.OSALLISTUI,
        p.osallistumistieto("hakukohdeOid", "kielikoe_fi").osallistumistieto);
  }
}
