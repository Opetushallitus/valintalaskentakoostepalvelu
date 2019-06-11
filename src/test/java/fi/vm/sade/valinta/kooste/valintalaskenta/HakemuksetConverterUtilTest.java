package fi.vm.sade.valinta.kooste.valintalaskenta;

import static fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen.EI_OSALLISTUNUT;
import static fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen.MERKITSEMATTA;
import static fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen.OSALLISTUI;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.ei_osallistunut;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;
import static fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec.laskennanalkamisparametri;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import com.github.npathai.hamcrestopt.OptionalMatchers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakukohdeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.Lisapistekoulutus;
import fi.vm.sade.valintalaskenta.domain.dto.PohjakoulutusToinenAste;
import org.hamcrest.collection.IsMapContaining;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HakemuksetConverterUtilTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    private static final HakuV1RDTO haku = new HakuV1RDTO();
    static {
        haku.setHakukausiUri("kausi_k#1");
        haku.setHakukausiVuosi(2015);
        haku.setKohdejoukkoUri(HakemuksetConverterUtil.KOHDEJOUKKO_AMMATILLINEN_JA_LUKIO + "#1");
    }

    private static final HakuV1RDTO haku_syksy = new HakuV1RDTO();
    static {
        haku_syksy.setHakukausiUri("kausi_s#1");
        haku_syksy.setHakukausiVuosi(2015);
    }

    private static final String HAKEMUS1_OID = "1.2.246.562.11.1";
    private static final String HAKEMUS2_OID = "1.2.246.562.11.2";
    private static final String HAKUKAUDELLA = "1.1.2015";
    private static final String HAKUKAUDEN_ULKOPUOLELLA = "1.1.2014";
    private static final SuoritusJaArvosanat vahvistettuPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(true)
                    .setYksilollistaminen("Ei")
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuLukioValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setLukio().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(false)
                    .setYksilollistaminen("Ei")
                    .setMyontaja(HAKEMUS1_OID)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonPerusopetusValmisEiHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(false)
                    .setYksilollistaminen("Ei")
                    .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonYksilollistettuPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(false)
                    .setYksilollistaminen("Kokonaan")
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuYksilollistettyPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(true)
                    .setYksilollistaminen("Kokonaan")
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuOsittainYksilollistettyPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(true)
                    .setYksilollistaminen("Osittain")
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuAlueittainYksilollistettyPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(true)
                    .setYksilollistaminen("Alueittain")
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuPerusopetusKeskenHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(true).setYksilollistaminen("Ei")
                    .setValmistuminen(HAKUKAUDELLA).setKesken()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuPerusopetusKeskenEiHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA).setKesken()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuPerusopetusKeskeytynytHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setKeskeytynyt()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuPerusopetusKeskeytynytEiHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA).setKeskeytynyt()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuKymppiValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setKymppiluokka().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuKymppiKeskeytynytHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setKymppiluokka().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setKeskeytynyt()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuKymppiKeskeytynytEiHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setKymppiluokka().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA).setKeskeytynyt()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuAmmattistarttiValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setAmmattistartti().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuAmmatilliseenValmistavaValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setAmmatilliseenValmistava().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuLukioonValmistavaValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setLukioonValmistava().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuLisaopetusTalousValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setLisaopetusTalous().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuValmentavaValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValmentava().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonLukioValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setLukio().setVahvistettu(false)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuValmaValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuTelmaValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setTelma().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonLukioKeskenHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setLukio().setVahvistettu(false)
                    .setValmistuminen(HAKUKAUDELLA).setKesken()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonLukioKeskenEiHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setLukio().setVahvistettu(false)
                    .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA).setKesken()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonLukioKeskeytynytHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setLukio().setVahvistettu(false)
                    .setValmistuminen(HAKUKAUDELLA).setKeskeytynyt()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonLukioKeskeytynytEiHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setLukio().setVahvistettu(false)
                    .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA).setKeskeytynyt()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuYOValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setYo().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuYOKeskenHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setYo().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setKesken()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuYOKeskeytynytHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setYo().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setKeskeytynyt()
                    .done();
    private static final SuoritusJaArvosanat vahvistettuUlkomainenValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setUlkomainenKorvaava().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonYksilollistettyPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(false)
                    .setYksilollistaminen("Kokonaan")
                    .setMyontaja(HAKEMUS1_OID)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonOsittainYksilollistettyPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(false)
                    .setYksilollistaminen("Osittain")
                    .setMyontaja(HAKEMUS1_OID)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();
    private static final SuoritusJaArvosanat vahvistamatonAlueittainYksilollistettyPerusopetusValmisHakukaudella =
            new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setPerusopetus().setVahvistettu(false)
                    .setYksilollistaminen("Alueittain")
                    .setMyontaja(HAKEMUS1_OID)
                    .setValmistuminen(HAKUKAUDELLA).setValmis()
                    .done();

    @Test
    public void suodattaaPoisHakukaudenUlkopuolellaKeskenOlleetPeruskoulunSuoritukset() {
        HakemusDTO h = new HakemusDTO();
        Oppija o = new Oppija();
        o.getSuoritukset().add(vahvistettuPerusopetusKeskenHakukaudella);
        o.getSuoritukset().add(vahvistettuPerusopetusKeskenEiHakukaudella);
        List<SuoritusJaArvosanat> suoritukset = HakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
        assertEquals(1, suoritukset.size());
        assertEquals(vahvistettuPerusopetusKeskenHakukaudella, suoritukset.get(0));
    }

    @Test
    public void suodattaaPoisHakukaudenUlkopuolellaKeskeytyneetPeruskoulunSuoritukset() {
        HakemusDTO h = new HakemusDTO();
        Oppija o = new Oppija();
        o.getSuoritukset().add(vahvistettuPerusopetusKeskeytynytEiHakukaudella);
        o.getSuoritukset().add(vahvistettuPerusopetusKeskeytynytHakukaudella);
        List<SuoritusJaArvosanat> suoritukset = HakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
        assertEquals(1, suoritukset.size());
        assertEquals(vahvistettuPerusopetusKeskeytynytHakukaudella, suoritukset.get(0));
    }

    @Test
    public void suodattaaPoisKeskeytyneetLukionSuoritukset() {
        HakemusDTO h = new HakemusDTO();
        Oppija o = new Oppija();
        o.getSuoritukset().add(vahvistamatonLukioKeskeytynytEiHakukaudella);
        o.getSuoritukset().add(vahvistamatonLukioKeskeytynytHakukaudella);
        List<SuoritusJaArvosanat> suoritukset = HakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
        assertEquals(0, suoritukset.size());
    }

    @Test
    public void suodattaaPoisVahvistamattomatLisäpistekoulutusSuoritukset() {
        HakemusDTO h = new HakemusDTO();
        Oppija o = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus().setKymppiluokka()
                .setVahvistettu(false)
                .setValmistuminen(HAKUKAUDELLA)
                .setValmis()
                .build()
                .suoritus().setAmmattistartti()
                .setVahvistettu(false)
                .setValmistuminen(HAKUKAUDELLA)
                .setValmis()
                .build()
                .suoritus().setAmmatilliseenValmistava()
                .setVahvistettu(false)
                .setValmistuminen(HAKUKAUDELLA)
                .setValmis()
                .build()
                .suoritus().setLukioonValmistava()
                .setVahvistettu(false)
                .setValmistuminen(HAKUKAUDEN_ULKOPUOLELLA)
                .setValmis()
                .build()
                .suoritus().setLisaopetusTalous()
                .setVahvistettu(false)
                .setValmistuminen(HAKUKAUDELLA)
                .setValmis()
                .build()
                .suoritus().setValmentava()
                .setVahvistettu(false)
                .setValmistuminen(HAKUKAUDELLA)
                .setValmis()
                .build()
                .build();
        List<SuoritusJaArvosanat> suoritukset = HakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
        assertTrue(suoritukset.isEmpty());
    }

    @Test
    public void suodattaaPoisKeskenjaKeskeytynytTilaisetYOSuoritukset() {
        HakemusDTO h = new HakemusDTO();
        Oppija o = new Oppija();
        o.getSuoritukset().add(vahvistettuYOKeskenHakukaudella);
        o.getSuoritukset().add(vahvistettuYOKeskeytynytHakukaudella);
        List<SuoritusJaArvosanat> suoritukset = HakemuksetConverterUtil.filterUnrelevantSuoritukset(haku, h, o.getSuoritukset());
        assertTrue(suoritukset.isEmpty());
    }

    @Test
    public void pohjakoulutusHakemukseltaJosEiSuorituksia() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>();
        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusPeruskouluJosHakemuksellaLukioJaLukioSuoritusKeskeytynytHakukaudella() {
        HakemusDTO h = new HakemusDTO();
        h.setHakijaOid("1.2.3.4.5.6");
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
            this.add(new AvainArvoDTO("lukioPaattotodistusVuosi", "2015"));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistamatonLukioKeskeytynytHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusPeruskouluJosHakemuksellaLukioJaAbiturienttiJaSuressaEiValmistaSuoritusta() {
        HakemusDTO h = new HakemusDTO();
        h.setHakijaOid("1.2.3.4.5.6");
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
            this.add(new AvainArvoDTO("lukioPaattotodistusVuosi", "2015"));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>();

        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusLukioJosHakemuksellaLukioJaEIAbiturienttiJaSuressaEiValmistaSuoritusta() {
        HakemusDTO h = new HakemusDTO();
        h.setHakijaOid("1.2.3.4.5.6");
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
            this.add(new AvainArvoDTO("lukioPaattotodistusVuosi", "2014"));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>();

        Assert.assertEquals(PohjakoulutusToinenAste.YLIOPPILAS, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusLukioJosLukionValmisSuoritus() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistamatonLukioValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.YLIOPPILAS, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusLukioJosLukionSuoritusKeskenHakukaudella() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistamatonLukioKeskenHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.YLIOPPILAS, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusLukioJosVahvistettuYOSuoritus() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuYOValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.YLIOPPILAS, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusPeruskouluKeskeytynytJosVahvistettuPeruskoulunSuoritusKeskeytynytHakukaudella() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.KESKEYTYNYT, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusPeruskouluJosSekaVahvistettuPeruskoulunSuoritusKeskeytynytEttaKeskenHakukaudella() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
            add(vahvistettuPerusopetusKeskenHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void automaticPreferenceDiscretionarySetJosVahvistettuPeruskoulunSuoritusKeskeytynytHakukaudella() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        h.setHakukohteet(Arrays.asList(new HakukohdeDTO(), new HakukohdeDTO()));
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
        }};
        Map<String,String> answers = HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset);
        Assert.assertEquals("true", answers.get("preference1-discretionary"));
        Assert.assertEquals("todistustenpuuttuminen", answers.get("preference1-discretionary-follow-up"));
        Assert.assertEquals("true", answers.get("preference2-discretionary"));
        Assert.assertEquals("todistustenpuuttuminen", answers.get("preference2-discretionary-follow-up"));
        Assert.assertFalse(answers.containsKey("preference3-discretionary"));
    }

    @Test
    public void pohjakoulutusLukioJosHakemuksellaLukioJaLoytyyValmisLukiosuoritus() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YLIOPPILAS));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuLukioValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.YLIOPPILAS, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusPeruskouluJosVahvistettuPeruskoulunSuoritusValmis() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusYksilollistettyPeruskouluJosYksilollistettyVahvistettuPeruskoulunSuoritusValmis() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YKSILOLLISTETTY));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuYksilollistettyPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.YKSILOLLISTETTY, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusOsittainYksilollistettyPeruskouluJosYksilollistettyVahvistettuPeruskoulunSuoritusValmis() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuOsittainYksilollistettyPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusAlueittainYksilollistettyPeruskouluJosYksilollistettyVahvistettuPeruskoulunSuoritusValmis() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuAlueittainYksilollistettyPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusPeruskouluSurestaVaikkaHakemuksenPohjakoulutusEiVastaaSurenSuoritusta() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuAlueittainYksilollistettyPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusHakemukseltaJosHakemuksellaPeruskouluJaVahvistettuUlkomainenSuoritus() {
        HakemusDTO h = new HakemusDTO();
        h.setHakijaOid("1.2.3.4.5.6");
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuUlkomainenValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusUlkomainenJosVahvistettuUlkomainenSuoritusTaiHakemuksellaUlkomainen() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>();
        Assert.assertEquals(PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
        suoritukset.add(vahvistettuUlkomainenValmisHakukaudella);
        Assert.assertEquals(PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusVahvistamatonYksilollistettuPeruskouluJosVahvistettuaPeruskoulunSuoritusEiOleSuressa() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistamatonYksilollistettyPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.YKSILOLLISTETTY, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusVahvistamatonOsittainYksilollistettyPeruskouluJosVahvistettuaPeruskoulunSuoritusEiOleSuressa() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistamatonOsittainYksilollistettyPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusVahvistamatonAlueittainYksilollistettuPeruskouluJosVahvistettuaPeruskoulunSuoritusEiOleSuressa() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistamatonAlueittainYksilollistettyPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void pohjakoulutusPeruskouluJosVahvistettuaPeruskoulunSuoritusOnSuressa() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistamatonYksilollistettyPerusopetusValmisHakukaudella);
            add(vahvistettuPerusopetusValmisHakukaudella);
        }};
        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, HakemuksetConverterUtil.pohjakoulutus(haku, h, suoritukset).get());
    }

    @Test
    public void lukioJaYoSuorituksetJosPohjakoulutusLukio() {
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuKymppiValmisHakukaudella);
            add(vahvistettuAmmattistarttiValmisHakukaudella);
            add(vahvistettuAmmatilliseenValmistavaValmisHakukaudella);
            add(vahvistettuLukioonValmistavaValmisHakukaudella);
            add(vahvistettuLisaopetusTalousValmisHakukaudella);
            add(vahvistettuValmentavaValmisHakukaudella);
            add(vahvistamatonLukioValmisHakukaudella);
            add(vahvistettuYOValmisHakukaudella);
        }};
        List<SuoritusJaArvosanat> oletetut = new ArrayList<>() {{
            add(vahvistamatonLukioValmisHakukaudella);
            add(vahvistettuYOValmisHakukaudella);
        }};
        assertEquals(oletetut, HakemuksetConverterUtil.pohjakoulutuksenSuoritukset(PohjakoulutusToinenAste.YLIOPPILAS, suoritukset));
    }

    @Test
    public void peruskouluJaLisapistekoulutuksetJosPohjakoulutusJokinPeruskoulu() {
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuKymppiValmisHakukaudella);
            add(vahvistettuAmmattistarttiValmisHakukaudella);
            add(vahvistettuAmmatilliseenValmistavaValmisHakukaudella);
            add(vahvistettuLukioonValmistavaValmisHakukaudella);
            add(vahvistettuLisaopetusTalousValmisHakukaudella);
            add(vahvistettuValmentavaValmisHakukaudella);
            add(vahvistamatonLukioValmisHakukaudella);
            add(vahvistettuYOValmisHakukaudella);
            add(vahvistettuValmaValmisHakukaudella);
            add(vahvistettuTelmaValmisHakukaudella);
        }};
        List<SuoritusJaArvosanat> oletetut = new ArrayList<>() {{
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuKymppiValmisHakukaudella);
            add(vahvistettuAmmattistarttiValmisHakukaudella);
            add(vahvistettuAmmatilliseenValmistavaValmisHakukaudella);
            add(vahvistettuLukioonValmistavaValmisHakukaudella);
            add(vahvistettuLisaopetusTalousValmisHakukaudella);
            add(vahvistettuValmentavaValmisHakukaudella);
            add(vahvistettuValmaValmisHakukaudella);
            add(vahvistettuTelmaValmisHakukaudella);
        }};
        assertEquals(oletetut, HakemuksetConverterUtil.pohjakoulutuksenSuoritukset(PohjakoulutusToinenAste.PERUSKOULU, suoritukset));
        assertEquals(oletetut, HakemuksetConverterUtil.pohjakoulutuksenSuoritukset(PohjakoulutusToinenAste.YKSILOLLISTETTY, suoritukset));
        assertEquals(oletetut, HakemuksetConverterUtil.pohjakoulutuksenSuoritukset(PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY, suoritukset));
        assertEquals(oletetut, HakemuksetConverterUtil.pohjakoulutuksenSuoritukset(PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY, suoritukset));
    }

    @Test
    public void ulkomaisetSuorituksetJosPohjakoulutusUlkomainen() {
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuKymppiValmisHakukaudella);
            add(vahvistamatonLukioValmisHakukaudella);
            add(vahvistettuYOValmisHakukaudella);
            add(vahvistettuUlkomainenValmisHakukaudella);
        }};
        List<SuoritusJaArvosanat> oletetut = new ArrayList<>() {{
            add(vahvistettuUlkomainenValmisHakukaudella);
        }};
        assertEquals(oletetut, HakemuksetConverterUtil.pohjakoulutuksenSuoritukset(PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO, suoritukset));
    }

    @Test
    public void valmaTelmaLisapistekoulutuksetJosValmisPerusopetus() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusValmisHakukaudella);
            add(vahvistettuTelmaValmisHakukaudella);
            add(vahvistettuValmaValmisHakukaudella);
        }};
        Map<String, String> oletettu = new HashMap<>();

        Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));

        Lisapistekoulutus[] oletetutLisapisteet = {
                Lisapistekoulutus.LISAKOULUTUS_TELMA,
                Lisapistekoulutus.LISAKOULUTUS_VALMA
        };
        Arrays.stream(oletetutLisapisteet).forEach(lpk -> oletettu.put(lpk.name(), "true"));

        oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
        oletettu.put("PK_TILA", "true");
        oletettu.put("AM_TILA", "false");
        oletettu.put("LK_TILA", "false");
        oletettu.put("YO_TILA", "false");
        oletettu.put("PK_PAATTOTODISTUSVUOSI", "2015");
        oletettu.put("PK_SUORITUSVUOSI", "2015");
        oletettu.put("PK_SUORITUSLUKUKAUSI", "2");
        Assert.assertEquals(oletettu, HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset));
    }

    @Test
    public void negatiivinenValmaTulosSuoritusrekisteristaAjaaYliHakemuksenTiedot() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            this.add(new AvainArvoDTO("LISAKOULUTUS_VALMA", "true"));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setKeskeytynyt()
                    .done());
        }};
        assertThat(HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset), new IsMapContaining<>(equalTo("LISAKOULUTUS_VALMA"), equalTo("false")));
    }

    @Test
    public void negatiivinenValmaTulosSuoritusrekisteristaEdelliseltaVuodeltaEiAjaYliHakemuksenTietoja() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            this.add(new AvainArvoDTO("LISAKOULUTUS_VALMA", "true"));
        }});
        String edellisenaVuonna = "1.1.2014";
        Assert.assertEquals("1.1.2015", HAKUKAUDELLA); // edellisenaVuonna needs to be fixed if HAKUKAUDELLA changes

        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma().setVahvistettu(true)
                    .setValmistuminen(edellisenaVuonna).setKeskeytynyt()
                    .done());
        }};
        assertThat(HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset), new IsMapContaining<>(equalTo("LISAKOULUTUS_VALMA"), equalTo("true")));
    }

    @Test
    public void positiivinenValmaTulosSuoritusrekisteristaHuomioidaanHakukaudelta() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma().setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA).setKesken()
                    .done());
        }};
        assertThat(HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset), new IsMapContaining<>(equalTo("LISAKOULUTUS_VALMA"), equalTo("true")));
    }

    @Test
    public void positiivistaValmaTulostaSuoritusrekisteristaEiHuomioidaEdelliseltaVuodelta() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        String edellisenaVuonna = "1.1.2014";
        Assert.assertEquals("1.1.2015", HAKUKAUDELLA); // edellisenaVuonna needs to be fixed if HAKUKAUDELLA changes
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(new SuoritusrekisteriSpec.SuoritusBuilder()
                    .setValma().setVahvistettu(true)
                    .setValmistuminen(edellisenaVuonna).setKesken()
                    .done());
        }};
        assertThat(HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset), new IsMapContaining<>(equalTo("LISAKOULUTUS_VALMA"), equalTo("false")));
    }

    @Test
    public void eiLisapistekoulutuksiaJosKeskeytynytPerusopetusTaiLukioTaiUlkomainenSuoritus() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusKeskeytynytHakukaudella);
            add(vahvistettuKymppiValmisHakukaudella);
        }};
        Map<String, String> oletettu = new HashMap<>();
        Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
        oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.KESKEYTYNYT);
        oletettu.put("PK_TILA", "false");
        oletettu.put("AM_TILA", "false");
        oletettu.put("LK_TILA", "false");
        oletettu.put("YO_TILA", "false");
        Assert.assertEquals(oletettu, HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset));
    }

    @Test
    public void eiLisapistekoulutustaHakemukseltaJosKeskeytynytSuoritus() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            add(new AvainArvoDTO(Lisapistekoulutus.LISAKOULUTUS_KYMPPI.name(), "true"));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuKymppiKeskeytynytHakukaudella);
        }};
        Map<String, String> oletettu = new HashMap<>();
        oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
        oletettu.put("PK_TILA", "false");
        oletettu.put("AM_TILA", "false");
        oletettu.put("LK_TILA", "false");
        oletettu.put("YO_TILA", "false");
        Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
        Assert.assertEquals(oletettu, HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset));
    }

    @Test
    public void lisapistekoulutusHakemukseltaJosEiKeskeytynyttaSuoritus() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            add(new AvainArvoDTO(Lisapistekoulutus.LISAKOULUTUS_KYMPPI.name(), "true"));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>();
        Map<String, String> oletettu = new HashMap<>();
        oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
        oletettu.put("PK_TILA", "false");
        oletettu.put("AM_TILA", "false");
        oletettu.put("LK_TILA", "false");
        oletettu.put("YO_TILA", "false");
        Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
        oletettu.put(Lisapistekoulutus.LISAKOULUTUS_KYMPPI.name(), "true");
        Assert.assertEquals(oletettu, HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset));
    }

    @Test
    public void lisapistekoulutusHakemukseltaJosKeskeytynytSuoritusEiHakukaudella() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            add(new AvainArvoDTO(Lisapistekoulutus.LISAKOULUTUS_KYMPPI.name(), "true"));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuKymppiKeskeytynytEiHakukaudella);
        }};
        Map<String, String> oletettu = new HashMap<>();
        oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
        oletettu.put("PK_TILA", "false");
        oletettu.put("AM_TILA", "false");
        oletettu.put("LK_TILA", "false");
        oletettu.put("YO_TILA", "false");
        Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
        oletettu.put(Lisapistekoulutus.LISAKOULUTUS_KYMPPI.name(), "true");
        Assert.assertEquals(oletettu, HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset));
    }

    @Test
    public void suoritusVuosiJaKausiJosPeruskoulunValmistunutSuoritus() {
        HakemusDTO h = new HakemusDTO();
        h.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
        }});
        List<SuoritusJaArvosanat> suoritukset = new ArrayList<>() {{
            add(vahvistettuPerusopetusValmisHakukaudella);
        }};
        Map<String, String> oletettu = new HashMap<>();
        oletettu.put("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU);
        oletettu.put("PK_TILA", "true");
        oletettu.put("AM_TILA", "false");
        oletettu.put("LK_TILA", "false");
        oletettu.put("YO_TILA", "false");
        Arrays.stream(Lisapistekoulutus.values()).forEach(lpk -> oletettu.put(lpk.name(), "false"));
        oletettu.put("PK_PAATTOTODISTUSVUOSI", "2015");
        oletettu.put("PK_SUORITUSVUOSI", "2015");
        oletettu.put("PK_SUORITUSLUKUKAUSI", "2");
        Assert.assertEquals(oletettu, HakemuksetConverterUtil.suoritustenTiedot(haku, h, suoritukset));
    }

    @Test
    public void preferoiArvosanaaYliSuoritusmerkinnan() {
        ParametritDTO parametrit = SuoritusrekisteriSpec.laskennanalkamisparametri(new DateTime());
        HakemusDTO hakemus = new HakemusDTO();
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(false)
                .setValmistuminen("1.1.2014")
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setArvosana("S")
                .setAsteikko_4_10()
                .build()
                .build()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .arvosana()
                .setAine("AI")
                .setArvosana("7")
                .setAsteikko_4_10()
                .build()
                .setValmis()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", parametrit, new HashMap<>(), oppija, hakemus, true);
        final String pk_arvosana = firstHakemusArvo(hakemus, "PK_AI").get();
        assertEquals("7", pk_arvosana);
    }

    @Test
    public void pohjakoulutusHakemukseltaJosYOSuoritusKeskeytynytHakukaudella() {
        HakemusDTO h = new HakemusDTO();
        h.setHakijaOid("1.2.3.4.5.6");
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            this.add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2015"));
        }});
        Oppija o = new Oppija();
        o.setSuoritukset(new ArrayList<>() {{
            add(vahvistamatonPerusopetusValmisHakukaudella);
            add(vahvistettuYOKeskeytynytHakukaudella);
        }});
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), o, h, true);
        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, getFirstHakemusArvo(h, "POHJAKOULUTUS"));
        Assert.assertEquals("2015", getFirstHakemusArvo(h, "PK_PAATTOTODISTUSVUOSI"));
    }

    @Test
    public void pohjakoulutusHakemukseltaJosYOSuoritusKeskenHakukaudella() {
        HakemusDTO h = new HakemusDTO();
        h.setHakijaOid("1.2.3.4.5.6");
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.YKSILOLLISTETTY));
            this.add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2015"));
        }});
        Oppija o = new Oppija();
        o.setSuoritukset(new ArrayList<>() {{
            add(vahvistamatonYksilollistettuPerusopetusValmisHakukaudella);
            add(vahvistettuYOKeskenHakukaudella);
        }});
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), o, h, true);
        Assert.assertEquals(PohjakoulutusToinenAste.YKSILOLLISTETTY, getFirstHakemusArvo(h, "POHJAKOULUTUS"));
        Assert.assertEquals("2015", getFirstHakemusArvo(h, "PK_PAATTOTODISTUSVUOSI"));
    }

    @Test
    public void pohjakoulutusHakemukseltaJosVainLisapistekoulutusVahvistettuValmis() {
        HakemusDTO h = new HakemusDTO();
        h.setHakijaOid("1.2.3.4.5.6");
        h.setAvaimet(new ArrayList<>() {{
            this.add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            this.add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2012"));
            this.add(new AvainArvoDTO(Lisapistekoulutus.LISAKOULUTUS_AMMATTISTARTTI.name(), "true"));
        }});
        Oppija o = new Oppija();
        o.setSuoritukset(new ArrayList<>() {{
            add(vahvistettuAmmattistarttiValmisHakukaudella);
            add(vahvistamatonPerusopetusValmisEiHakukaudella);
        }});
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), o, h, true);
        Assert.assertEquals(PohjakoulutusToinenAste.PERUSKOULU, getFirstHakemusArvo(h, "POHJAKOULUTUS"));
        Assert.assertEquals("2012", getFirstHakemusArvo(h, "PK_PAATTOTODISTUSVUOSI"));
        Assert.assertEquals("true", getFirstHakemusArvo(h, Lisapistekoulutus.LISAKOULUTUS_AMMATTISTARTTI.name()));
    }

    @Test
    public void suurinArvosanaValitaanYliUseanSuorituksen() {
        { // Uudempi
            HakemusDTO hakemus = new HakemusDTO();
            hakemus.setAvaimet(new ArrayList<>() {{
                add(new AvainArvoDTO("PK_BI_VAL1", "7"));
            }});
            Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(false)
                    .setValmistuminen("1.1.2014")
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setArvosana("6")
                    .setAsteikko_4_10()
                    .build()
                    .build()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2015")
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setArvosana("7")
                    .setAsteikko_4_10()
                    .build()
                    .arvosana()
                    .setAine("BI")
                    .setValinnainen()
                    .setJarjestys(1)
                    .setArvosana("8")
                    .setAsteikko_4_10()
                    .build()
                    .build()
                    .suoritus()
                    .setKymppiluokka()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2015")
                    .setValmis()
                    .arvosana()
                    .setAine("BI")
                    .setValinnainen()
                    .setJarjestys(1)
                    .setArvosana("9")
                    .setAsteikko_4_10()
                    .build()
                    .build()
                    .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
            final String pk_arvosana = firstHakemusArvo(hakemus, "PK_AI").get();
            final String pk_val_arvosana = firstHakemusArvo(hakemus, "PK_BI_VAL1").get();
            assertEquals("7", pk_arvosana);
            assertEquals("9", pk_val_arvosana);
            assertFalse(hakemus.getAvaimet().stream()
                    .anyMatch(a -> a.getAvain().equals("PK_BI_VAL2")));
            assertFalse(hakemus.getAvaimet().stream()
                    .anyMatch(a -> a.getAvain().equals("PK_BI")));
        }
        {
            HakemusDTO hakemus = new HakemusDTO();
            Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2014")
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setArvosana("7")
                    .setAsteikko_4_10()
                    .build()
                    .build()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2015")
                    .arvosana()
                    .setAine("AI")
                    .setArvosana("6")
                    .setAsteikko_4_10()
                    .build()
                    .setValmis()
                    .build()
                    .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
            final String pk_arvosana = firstHakemusArvo(hakemus, "PK_AI").get();
            assertEquals("7", pk_arvosana);
        }
        { // Valmistumaton kymppiluokka otetaan huomioon
            HakemusDTO hakemus = new HakemusDTO();
            Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(false)
                    .setValmistuminen("1.1.2014")
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setArvosana("7")
                    .setAsteikko_4_10()
                    .build()
                    .build()
                    .suoritus()
                    .setKymppiluokka()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2015")
                    .arvosana()
                    .setAine("AI")
                    .setArvosana("8")
                    .setAsteikko_4_10()
                    .build()
                    .setKesken()
                    .build()
                    .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
            final String pk_arvosana = hakemus.getAvaimet().stream()
                    .filter(a -> a.getAvain().equals("PK_AI"))
                    .map(a -> a.getArvo())
                    .findFirst().get();
            assertEquals("8", pk_arvosana);
        }
        { // Vanhempi kymppiluokka otetaan huomioon
            HakemusDTO hakemus = new HakemusDTO();
            Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(false)
                    .setValmistuminen("1.1.2015")
                    .setValmis()
                    .arvosana()
                    .setAine("AI")
                    .setArvosana("7")
                    .setAsteikko_4_10()
                    .build()
                    .build()
                    .suoritus()
                    .setKymppiluokka()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2013")
                    .arvosana()
                    .setAine("AI")
                    .setArvosana("8")
                    .setAsteikko_4_10()
                    .build()
                    .setKesken()
                    .build()
                    .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
            final String pk_arvosana = hakemus.getAvaimet().stream()
                    .filter(a -> a.getAvain().equals("PK_AI"))
                    .map(a -> a.getArvo())
                    .findFirst().get();
            Assert.assertEquals("8", pk_arvosana);
        }
    }

    @Test
    public void vainItseIlmoitettu() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2015"));
        }});
        Oppija oppija = new Oppija();
        oppija.setEnsikertalainen(true);

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    }

    @Test
    public void itseIlmoitettuJaValmis() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(HakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
        }});
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setSuoritusKieli("SV")
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
        assertEquals("SV", getFirstHakemusArvo(hakemus,  HakemuksetConverterUtil.PERUSOPETUS_KIELI));
    }

    @Test
    public void hakemukseltaKopioituJaValmisPk() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setHakemusoid(HAKEMUS1_OID);
        hakemus.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(HakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
        }});
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setSuoritusKieli("FI")
                .setPerusopetus()
                .setVahvistettu(false)
                .setMyontaja(HAKEMUS1_OID)
                .setValmistuminen("1.1.2014")
                .setValmis()
                .build()
                .suoritus()
                .setSuoritusKieli("SV")
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
        assertEquals("SV", getFirstHakemusArvo(hakemus,  HakemuksetConverterUtil.PERUSOPETUS_KIELI));
    }

    @Test
    public void hakemukseltaKopioituMuokattuLukioKieli() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setHakemusoid(HAKEMUS1_OID);
        hakemus.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO(HakemuksetConverterUtil.LUKIO_KIELI, "FI"));
        }});
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setSuoritusKieli("SV")
                .setLukio()
                .setVahvistettu(false)
                .setMyontaja(HAKEMUS1_OID)
                .setValmistuminen("1.1.2014")
                .setKesken()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
        assertEquals("SV", getFirstHakemusArvo(hakemus,  HakemuksetConverterUtil.LUKIO_KIELI));
    }

    @Test
    public void keskenJaValmis() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(HakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
        }});
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setSuoritusKieli("FI")
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setKesken()
                .build()
                .suoritus()
                .setSuoritusKieli("SV")
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
        assertEquals("SV", getFirstHakemusArvo(hakemus,  HakemuksetConverterUtil.PERUSOPETUS_KIELI));
    }

    @Test
    public void kaytetaanUudempaaSuoritustaJosMuutenSamatTilat() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2014"));
            add(new AvainArvoDTO(HakemuksetConverterUtil.PERUSOPETUS_KIELI, "FI"));
        }});
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setSuoritusKieli("FI")
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .suoritus()
                .setSuoritusKieli("SV")
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.2.2015")
                .setValmis()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
        assertEquals("SV", getFirstHakemusArvo(hakemus,  HakemuksetConverterUtil.PERUSOPETUS_KIELI));
    }

    @Test
    public void itseIlmoitettuJaKaksiValmista() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2013"));
        }});
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_SUORITUSVUOSI"));
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
    }

    @Test
    public void useampiSuoritusJoistaEiOllaKiinnostuneita() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija virheellinenKoskaUseampiSuoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setKomo("koulutus_732101")
                .setValmistuminen("1.1.2015")
                .setKesken()
                .build()
                .suoritus()
                .setKomo("koulutus_732101")
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .suoritus()
                .setKomo("koulutus_671101")
                .setValmistuminen("1.1.2015")
                .setKesken()
                .build()
                .suoritus()
                .setKomo("koulutus_671101")
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .build();
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), virheellinenKoskaUseampiSuoritus, hakemus, true);
    }

    @Test
    public void perusopetusOikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija oikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setValmistuminen("1.1.2015")
                .setKeskeytynyt()
                .build()
                .suoritus()
                .setPerusopetus()
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .suoritus()
                .setPerusopetus()
                .setValmistuminen("1.1.2015")
                .setKeskeytynyt()
                .build()
                .build();
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oikeinKoskaVainValmisSuoritusJaKeskeytyneitaSuorituksia, hakemus, true);
    }

    @Test
    public void vainHaunHakukaudenPerusopetusSuoritusvuosiJaSuorituskausi() {
        /*
        PK_SUORITUSLUKUKAUSI = 1/2
        1.1. - 31.7. ->  2
        1.8. -> 31.12. -> 1
                */
        {
            HakemusDTO hakemus = new HakemusDTO();
            Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(true)
                    .setValmistuminen("31.5.2015")
                    .setValmis()
                    .build()
                    .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("PK_SUORITUSVUOSI löytyy ja sen arvo on 2015",
                    hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo())).count() == 1L);
            Assert.assertEquals("PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 2",
                    new AvainArvoDTO("PK_SUORITUSLUKUKAUSI", "2"),
                    hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain())).findFirst().get());
            Assert.assertTrue("PK_PAATTOTODISTUSVUOSI löytyy ja sen arvo on 2015",
                    hakemus.getAvaimet().stream().filter(a -> "PK_PAATTOTODISTUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo())).count() == 1L);
        }
        {
            HakemusDTO hakemus = new HakemusDTO();
            Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(true)
                    .setValmistuminen(HAKUKAUDELLA)
                    .setValmis()
                    .build()
                    .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("PK_SUORITUSVUOSI löytyy ja sen arvo on 2015",
                    hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo())).count() == 1L);
            Assert.assertEquals("PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 2",
                    new AvainArvoDTO("PK_SUORITUSLUKUKAUSI", "2"),
                    hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain())).findFirst().get());
            Assert.assertTrue("PK_PAATTOTODISTUSVUOSI löytyy ja sen arvo on 2015",
                    hakemus.getAvaimet().stream().filter(a -> "PK_PAATTOTODISTUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo())).count() == 1L);
        }
        {
            HakemusDTO hakemus = new HakemusDTO();
            Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(true)
                    .setValmistuminen("01.08.2015")
                    .setValmis()
                    .build()
                    .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku_syksy, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("PK_SUORITUSVUOSI löytyy ja sen arvo on 2015",
                    hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo())).count() == 1L);
            assertTrue("PK_SUORITUSLUKUKAUSI löytyy ja sen arvo on 1",
                    hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain()) && "1".equals(a.getArvo())).count() == 1L);
            assertTrue("PK_PAATTOTODISTUSVUOSI löytyy ja sen arvo on 2015",
                    hakemus.getAvaimet().stream().filter(a -> "PK_PAATTOTODISTUSVUOSI".equals(a.getAvain()) && "2015".equals(a.getArvo())).count() == 1L);
        }
        {
            HakemusDTO hakemus = new HakemusDTO();
            Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(true)
                    .setValmistuminen("31.12.2008")
                    .setValmis()
                    .build()
                    .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("PK_SUORITUSVUOSI ei löydy",
                    hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSVUOSI".equals(a.getAvain())).count() == 0L);
            assertTrue("PK_SUORITUSLUKUKAUSI ei löydy",
                    hakemus.getAvaimet().stream().filter(a -> "PK_SUORITUSLUKUKAUSI".equals(a.getAvain())).count() == 0L);
            assertTrue("PK_PAATTOTODISTUSVUOSI ei löydy",
                    hakemus.getAvaimet().stream().filter(a -> "PK_PAATTOTODISTUSVUOSI".equals(a.getAvain())).count() == 0L);
        }
    }

    @Test
    public void perusopetusOikeinKoskaVainValmisSuoritus() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija oikeinKoskaVainValmisSuoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .build()
                .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oikeinKoskaVainValmisSuoritus, hakemus, true);
            Assert.assertEquals("PK_TILA löytyy ja sen arvo on true",
                    new AvainArvoDTO("PK_TILA", "true"),
                    hakemus.getAvaimet().stream().filter(a -> "PK_TILA".equals(a.getAvain())).findFirst().get());
    }

    @Test
    public void pkTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija oikeinKoskaEiSuorituksia = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setValmistuminen("1.1.2015")
                .setKeskeytynyt()
                .build()
                .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oikeinKoskaEiSuorituksia, hakemus, true);
            Assert.assertTrue("PK_TILA löytyy ja sen arvo on false",
                    hakemus.getAvaimet().stream().filter(a -> "PK_TILA".equals(a.getAvain()) && "false".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void pkSuoritusArvosanojenPoisFiltterointi() {
        DateTime nyt = DateTime.now();
        HakemusDTO hakemus = new HakemusDTO();
        Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko("")
                .setArvosana("S")
                .setMyonnetty(nyt)
                .build()
                .build()
                .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", laskennanalkamisparametri(nyt.plusDays(1)), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("PK_AI ei löydy ja sen arvo ei ole S",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI".equals(a.getAvain()) && "S".equals(a.getArvo())).count() == 0L);
            Assert.assertTrue("PK_AI_SUORITETTU löytyy arvolla true",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI_SUORITETTU".equals(a.getAvain()) && "true".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void pkSamanAineenToistuessaKaytetaanParasta() {
        DateTime nyt = DateTime.now();
        HakemusDTO hakemus = new HakemusDTO();
        Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
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
            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", laskennanalkamisparametri(nyt.plusDays(1)), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("PK_AI löytyy ja sen arvo on 8",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI".equals(a.getAvain()) && "8".equals(a.getArvo())).count() == 1L);
        }
    }

    @Test
    public void kaytetanVainKyseisenHakemuksenItseSyotettyjaArvosanoja() {
        DateTime nyt = DateTime.now();
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setHakemusoid(HAKEMUS1_OID);
        Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(false)
                .setMyontaja(HAKEMUS1_OID)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("6")
                .setMyonnetty(nyt)
                .build()
                .build()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(false)
                .setMyontaja(HAKEMUS2_OID)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("8")
                .setMyonnetty(nyt)
                .build()
                .build()
                .build();
        {
            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", laskennanalkamisparametri(nyt.plusDays(1)), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("PK_AI löytyy ja sen arvo on 6",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI".equals(a.getAvain()) && "6".equals(a.getArvo())).count() == 1L);
        }
    }

    @Test
    public void pkSamanArvosananToistuessaKaytetaanParastaPaitsiJosMuutOnValinnaisia() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
                .setValmis()
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("6")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(1)
                .setAsteikko_4_10()
                .setArvosana("8")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(2)
                .setAsteikko_4_10()
                .setArvosana("7")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(3)
                .setAsteikko_4_10()
                .setArvosana("9")
                .build()
                .build()
                .build();

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("PK_AI löytyy ja sen arvo on 6",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI".equals(a.getAvain()) && "6".equals(a.getArvo())).count() == 1L);
            Assert.assertTrue("PK_AI_VAL1 löytyy",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI_VAL1".equals(a.getAvain())).count() == 1L);
            Assert.assertTrue("PK_AI_VAL2 löytyy",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI_VAL2".equals(a.getAvain())).count() == 1L);
            Assert.assertTrue("PK_AI_VAL3 löytyy",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI_VAL3".equals(a.getAvain())).count() == 1L);
    }

    /**
     * Eri asteikot yhdistettavilla arvosanoilla. Yhdistaminen ei mahdollista.
     */
    @Test
    public void pkSamaArvoMuttaAsteikotEiTasmaa() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija samaArvoMuttaAsteikotEiTasmaa = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setValmistuminen(HAKUKAUDELLA)
                .setVahvistettu(true)
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
        expected.expectMessage("Asteikot ei täsmää: 1-5 4-10");
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), samaArvoMuttaAsteikotEiTasmaa, hakemus, true);
    }

    @Test
    public void pkTuntematonAsteikkoMuttaTunnistetaanNumeroksi() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija tuntematonAsteikkoMuttaTunnistetaanNumeroksi = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen("1.1.2015")
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

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), tuntematonAsteikkoMuttaTunnistetaanNumeroksi, hakemus, true);
            Assert.assertTrue("PK_AI löytyy ja sen arvo on 8",
                    hakemus.getAvaimet().stream().filter(a -> "PK_AI".equals(a.getAvain()) && "8".equals(a.getArvo())).count() == 1L);
    }

    /**
     * Poikkeus koska yhdistaminen ei mahdollista
     */
    @Test
    public void pkTunnistamatonArvosana() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija tunnistamatonArvosana = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setPerusopetus()
                .setVahvistettu(true)
                .setValmistuminen(HAKUKAUDELLA)
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
        expected.expect(NumberFormatException.class);
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), tunnistamatonArvosana, hakemus, true);
    }

    @Test
    public void pkVahvistamatonSuoritus() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("POHJAKOULUTUS", PohjakoulutusToinenAste.PERUSKOULU));
            add(new AvainArvoDTO("PK_PAATTOTODISTUSVUOSI", "2015"));
        }});
        Oppija tunnistamatonArvosana = new SuoritusrekisteriSpec
                .OppijaBuilder()
                    .suoritus()
                        .setPerusopetus()
                        .setVahvistettu(false)
                        .setValmistuminen(HAKUKAUDELLA)
                        .setValmis()
                        .build()
                    .build();
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), tunnistamatonArvosana, hakemus, true);
        assertEquals(PohjakoulutusToinenAste.PERUSKOULU, getFirstHakemusArvo(hakemus, "POHJAKOULUTUS"));
        assertEquals("2015", getFirstHakemusArvo(hakemus, "PK_PAATTOTODISTUSVUOSI"));
        assertEquals("not 15:" + hakemus.getAvaimet(), 15, hakemus.getAvaimet().size());
    }

    @Test
    public void pkParempiArvosanaVaikkaVahvistamatonSuoritus() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija tunnistamatonArvosana = new SuoritusrekisteriSpec
                .OppijaBuilder()
                    .suoritus()
                        .setPerusopetus()
                        .setValmistuminen("1.1.2015")
                        .setValmis()
                        .setVahvistettu(false)
                        .arvosana()
                            .setAine("AI")
                            .setAsteikko("")
                            .setArvosana("6")
                            .build()
                        .build()
                    .suoritus()
                        .setPerusopetus()
                        .setValmistuminen("1.1.2015")
                        .setValmis()
                        .setVahvistettu(true)
                        .arvosana()
                            .setAine("AI")
                            .setAsteikko("")
                            .setArvosana("5")
                            .build()
                        .build()
                    .build();
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), tunnistamatonArvosana, hakemus, true);
        assertEquals(8, hakemus.getAvaimet().size());
        assertEquals("6", getFirstHakemusArvo(hakemus, "PK_AI"));
    }

    // LUKIO:
    // LK_TILA = false // defaut jos suorituksen tila on
    // SUORITUS VAAN KERRAN MYÖS LUKIO TOUHUISSA KOSKA TULEE YTL:LTÄ
    // LUKION ARVOSANA MONEEN KERTAAN JA PARASTA KÄYTETÄÄN

    @Test
    public void lkTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija oikeinKoskaEiSuorituksia = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setLukio()
                .setValmistuminen(HAKUKAUDELLA)
                .setKeskeytynyt()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oikeinKoskaEiSuorituksia, hakemus, true);
            Assert.assertEquals("LK_TILA on false",
                    new AvainArvoDTO("LK_TILA", "false"),
                    hakemus.getAvaimet().stream().filter(a -> "LK_TILA".equals(a.getAvain())).findFirst().get());
    }

    @Test
    public void lkSuoritusVainKerran() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setHakemusoid(HAKEMUS1_OID);
        Oppija virheellinenKoskaUseampiSuoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setLukio()
                .setValmistuminen("1.1.2015")
                .setKesken()
                .setMyontaja(HAKEMUS1_OID)
                .build()
                .suoritus()
                .setLukio()
                .setValmistuminen("1.1.2015")
                .setValmis()
                .setVahvistettu(true)
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), virheellinenKoskaUseampiSuoritus, hakemus, true);
        Assert.assertTrue("LK_TILA löytyy ja sen arvo on true",
                hakemus.getAvaimet().stream().filter(a -> "LK_TILA".equals(a.getAvain()) && "true".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void lkSuoritusVainTaltaHakemuseltaJosEiVahvistettu1() {
        DateTime nyt = DateTime.now();
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setHakemusoid(HAKEMUS1_OID);
        Oppija toisellaHakemuksellaKorkeampiArvosana = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setLukio()
                .setValmistuminen("1.1.2015")
                .setKesken()
                .setMyontaja(HAKEMUS2_OID)
                .setVahvistettu(false)
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("8")
                .setMyonnetty(nyt)
                .build()
                .build()
                .suoritus()
                .setLukio()
                .setValmistuminen("1.1.2015")
                .setValmis()
                .setMyontaja(HAKEMUS1_OID)
                .setVahvistettu(false)
                .arvosana()
                .setAine("AI")
                .setAsteikko_4_10()
                .setArvosana("6")
                .setMyonnetty(nyt)
                .build()
                .build()
                .build();
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), toisellaHakemuksellaKorkeampiArvosana, hakemus, true);
        Assert.assertTrue("LK_AI löytyy ja sen arvo on 6",
                hakemus.getAvaimet().stream().filter(a -> "LK_AI".equals(a.getAvain()) && "6".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void lkSuoritusVainTaltaHakemuseltaJosEiVahvistettu2() {
        HakemusDTO hakemus = new HakemusDTO();
        hakemus.setHakemusoid(HAKEMUS1_OID);
        Oppija toisellaHakemuksellaKorkeampiArvosana = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setLukio()
                .setValmistuminen("1.1.2015")
                .setValmis()
                .setMyontaja(HAKEMUS2_OID)
                .build()
                .suoritus()
                .setPerusopetus()
                .setValmistuminen("1.1.2012")
                .setValmis()
                .setMyontaja(HAKEMUS1_OID)
                .setVahvistettu(false)
                .build()
                .build();
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), toisellaHakemuksellaKorkeampiArvosana, hakemus, true);
        Assert.assertTrue("LK_TILA löytyy ja sen arvo on false",
                hakemus.getAvaimet().stream().filter(a -> "LK_TILA".equals(a.getAvain()) && "false".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void lkSamanArvosananToistuessaKaytetaanParasta() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija tuntematonAsteikkoMuttaTunnistetaanNumeroksi = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setLukio()
                .setValmistuminen("1.1.2015")
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

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), tuntematonAsteikkoMuttaTunnistetaanNumeroksi, hakemus, true);
            Assert.assertTrue("LK_AI löytyy ja sen arvo on 8",
                    hakemus.getAvaimet().stream().filter(a -> "LK_AI".equals(a.getAvain()) && "8".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void lkSamanArvosananToistuessaToisessaSuorituksessaKaytetaanParasta() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija tuntematonAsteikkoMuttaTunnistetaanNumeroksi = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                    .setLukio()
                    .setValmistuminen("1.1.2015")
                    .setValmis()
                    .arvosana()
                        .setAine("AI")
                        .setAsteikko("")
                        .setArvosana("9")
                        .build()
                    .arvosana()
                        .setAine("AI")
                        .setAsteikko("")
                        .setArvosana("8")
                        .build()
                    .build()
                .suoritus()
                    .setLukio()
                    .setValmistuminen("1.1.2015")
                    .setValmis()
                    .arvosana()
                        .setAine("AI")
                        .setAsteikko("")
                        .setArvosana("10")
                        .build()
                    .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), tuntematonAsteikkoMuttaTunnistetaanNumeroksi, hakemus, true);

        long count = hakemus.getAvaimet().stream().filter(a -> "LK_AI".equals(a.getAvain())).count();
        assertEquals("käsitellyssä datassa pitäisi olla vain yksi arvosana (paras arvosana) per aine ", 1, count);

        AvainArvoDTO avainArvoDTO = hakemus.getAvaimet().stream().filter(a -> "LK_AI".equals(a.getAvain())).findFirst().get();
        String arvo = "10";
        AvainArvoDTO expected = new AvainArvoDTO("LK_AI", arvo);
        assertEquals(String.format("LK_AI arvo pitäisi olla toisesta suorituksesta löytyvä korotettu numero {}", arvo), expected, avainArvoDTO);
    }

    // KYMPPILUOKKA:
    // PK_TILA_10 = false // default
    // VAAN YKS SUORITUS (LISÄOPETUS)
    // PK_***_10 SUFFIX
    @Test
    public void kymppiluokanKorotuksissaMyosKeskeytettySuoritusHuomioidaan() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija oikeinKoskaKeskenerainenSuoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                .setKymppiluokka()
                .setVahvistettu(true)
                .setValmistuminen(HAKUKAUDELLA)
                .setKeskeytynyt()
                .arvosana()
                .setAine("AI")
                .setArvosana("7")
                .build()
                .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oikeinKoskaKeskenerainenSuoritus, hakemus, true);
        Assert.assertTrue("PK_AI löytyy ja sen arvo on 7",
                hakemus.getAvaimet().stream().filter(a -> "PK_AI".equals(a.getAvain()) && "7".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void perusopetuksenOppiaineenOppimaara() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija oikeinKoskaVainValmisSuoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                .suoritus()
                    .setPerusopetuksenOppiaineenOppimaara()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2015")
                        .arvosana()
                        .setAine("AI")
                        .setArvosana("8")
                        .build()
                    .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oikeinKoskaVainValmisSuoritus, hakemus, true);
        Assert.assertTrue("PK_AI löytyy ja sen arvo on 8",
                hakemus.getAvaimet().stream().filter(a -> "PK_AI".equals(a.getAvain()) && "8".equals(a.getArvo())).count() == 1L);
    }

    @Test
    public void perusopetuksenArvosanaaKorotettuPerusopetuksenOppiaineenOppimaarassa() {
        HakemusDTO hakemus = new HakemusDTO();
        Oppija oikeinKoskaVainValmisSuoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setPerusopetus()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2015")
                    .arvosana()
                        .setAine("AI")
                        .setArvosana("8")
                        .build()
                    .build()
                .suoritus()
                    .setPerusopetuksenOppiaineenOppimaara()
                    .setVahvistettu(true)
                    .setValmistuminen("12.12.2015")
                    .arvosana()
                        .setAine("AI")
                        .setArvosana("9")
                        .build()
                    .build()
                .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oikeinKoskaVainValmisSuoritus, hakemus, true);
        Assert.assertTrue("PK_AI löytyy ja sen arvo on 9",
                hakemus.getAvaimet().stream().filter(a -> "PK_AI".equals(a.getAvain()) && "9".equals(a.getArvo())).count() == 1L);
    }


    public static boolean testEquality(List<Map<String, String>> a, List<Map<String, String>> b) {
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

    public static boolean testEquality(AvainMetatiedotDTO a, List<AvainMetatiedotDTO> aa) {
        return aa.stream()
                .filter(x -> x.getAvain().equals(a.getAvain()))
                .anyMatch(
                        x -> testEquality(a.getMetatiedot(), x.getMetatiedot())
                );
    }

    @Test
    public void yoTilaOnTrueJosSuoritusOnMerkittyValmiiksiMuutoinFalse() {
        {
            HakemusDTO hakemus = new HakemusDTO();
            Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
                    .suoritus()
                    .setYo()
                    .setVahvistettu(true)
                    .setValmistuminen("1.1.2015")
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

            // YO-tila tulee edelleen merkitä vaikka yo-arvosanat laitetaan uuteen tietueeseen AvainSuoritusTietoDTO
            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertEquals("YO_TILA löytyy ja sen arvo on true",
                    new AvainArvoDTO("YO_TILA", "true"),
                    hakemus.getAvaimet().stream().filter(a -> "YO_TILA".equals(a.getAvain())).findFirst().get());

        }
        {
            HakemusDTO hakemus = new HakemusDTO();
            Oppija suoritus = new SuoritusrekisteriSpec.OppijaBuilder()
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

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), suoritus, hakemus, true);
            Assert.assertTrue("YO_TILA löytyy ja sen arvo on false",
                    hakemus.getAvaimet().stream().filter(a -> "YO_TILA".equals(a.getAvain()) && "false".equals(a.getArvo())).count() == 1L);
        }
    }

    @Test
    public void testaaEnsikertalaisuusOppijaTiedoista() {
        {
            HakuV1RDTO haku = new HakuV1RDTO();
            haku.setKohdejoukkoUri("haunkohdejoukko_12#1");
            HakemusDTO hakemus = new HakemusDTO();
            Oppija oppija = new Oppija();
            oppija.setEnsikertalainen(true);

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
            Assert.assertTrue("Ensikertalaisuus asetettu", Boolean.valueOf(hakemus.getAvaimet().stream()
                    .filter(a -> a.getAvain().equals("ensikertalainen")).findFirst().get().getArvo()));
        }

        {
            HakuV1RDTO haku = new HakuV1RDTO();
            haku.setKohdejoukkoUri("haunkohdejoukko_12#1");
            HakemusDTO hakemus = new HakemusDTO();
            Oppija oppija = new Oppija();
            oppija.setEnsikertalainen(false);

            HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, hakemus, true);
            Assert.assertFalse("Ensikertalaisuus asetettu", Boolean.valueOf(hakemus.getAvaimet().stream()
                    .filter(a -> a.getAvain().equals("ensikertalainen")).findFirst().get().getArvo()));
        }
    }

    @Test
    public void valinnaistenArvosanojenValinta() {
        HakemusDTO hakemus = new HakemusDTO();
        SuoritusrekisteriSpec.OppijaBuilder oppijaBuilder = new SuoritusrekisteriSpec.OppijaBuilder();
        // vahvistettu suoritus
        oppijaBuilder.suoritus()
                .setKymppiluokka()
                .setVahvistettu(true)
                .setValmistuminen(HAKUKAUDELLA)
                .setKeskeytynyt()
                .arvosana()
                .setAine("AI")
                .setArvosana("7")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(1)
                .setArvosana("7")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(2)
                .setArvosana("6")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(3)
                .setArvosana("5")
                .build()

                .arvosana()
                .setAine("FY")
                .setArvosana("8")
                .build()
                .arvosana()
                .setAine("FY")
                .setValinnainen()
                .setJarjestys(1)
                .setArvosana("9")
                .build()
                .arvosana()
                .setAine("FY")
                .setValinnainen()
                .setJarjestys(2)
                .setArvosana("S")
                .build()
                .build();

        // vahvistamaton suoritus
        oppijaBuilder.suoritus()
                .setKymppiluokka()
                .setVahvistettu(false)
                .setValmistuminen(HAKUKAUDELLA)
                .setKeskeytynyt()
                .arvosana()
                .setAine("AI")
                .setArvosana("5")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(1)
                .setArvosana("6")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(2)
                .setArvosana("9")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(3)
                .setArvosana("10")
                .build()

                .arvosana()
                .setAine("EN")
                .setValinnainen()
                .setJarjestys(1)
                .setArvosana("6")
                .build()

                .arvosana()
                .setAine("FY")
                .setArvosana("10")
                .build()
                .arvosana()
                .setAine("FY")
                .setValinnainen()
                .setJarjestys(1)
                .setArvosana("S")
                .build()
                .arvosana()
                .setAine("FY")
                .setValinnainen()
                .setJarjestys(2)
                .setArvosana("6")
                .build()
                .build();

        // vahvistamaton suoritus
        oppijaBuilder.suoritus()
                .setKymppiluokka()
                .setVahvistettu(false)
                .setValmistuminen(HAKUKAUDELLA)
                .setKeskeytynyt()
                .arvosana()
                .setAine("FY")
                .setArvosana("5")
                .build()
                .arvosana()
                .setAine("FY")
                .setValinnainen()
                .setJarjestys(1)
                .setArvosana("8")
                .build()
                .arvosana()
                .setAine("FY")
                .setValinnainen()
                .setJarjestys(2)
                .setArvosana("10")
                .build()
                .arvosana()
                .setAine("FY")
                .setValinnainen()
                .setJarjestys(3)
                .setArvosana("10")
                .build()
                .build();


        Map<String, String> kiinnostavatAvainParit = ImmutableMap.of(
                "PK_AI", "7",
                "PK_AI_VAL1", "6",
                "PK_AI_VAL2", "9",
                "PK_AI_VAL3", "10",
                // Yksittäinen valinnainen arvosana suorituksella
                "PK_EN_VAL1", "6"
        );

        Map<String, String> kiinnostavatAvainParitFysiikka = ImmutableMap.of(
                "PK_FY", "10",
                "PK_FY_VAL1", "8",
                "PK_FY_VAL2", "10",
                "PK_FY_VAL3", "10"
        );

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppijaBuilder.build(), hakemus, true);
        Assert.assertEquals("Odotettiin paras arvosana ensimmäisestä suorituksesta ja parhaat valinnaiset jälkimmäisestä suorituksesta",
                kiinnostavatAvainParit,
                hakemus.getAvaimet().stream()
                        .filter(a -> kiinnostavatAvainParit.containsKey(a.getAvain()))
                        .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)));
        Assert.assertEquals(
                kiinnostavatAvainParitFysiikka,
                hakemus.getAvaimet().stream()
                        .filter(a -> kiinnostavatAvainParitFysiikka.containsKey(a.getAvain()))
                        .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)));
    }
    @Test
    public void valinnaistenArvosanojenValintaKunMukanaOnSuoritusmerkintoja() {
        HakemusDTO hakemus = new HakemusDTO();
        SuoritusrekisteriSpec.OppijaBuilder oppijaBuilder = new SuoritusrekisteriSpec.OppijaBuilder();
        // vahvistettu suoritus
        oppijaBuilder.suoritus()
                .setKymppiluokka()
                .setVahvistettu(true)
                .setValmistuminen(HAKUKAUDELLA)
                .setKeskeytynyt()
                .arvosana()
                .setAine("AI")
                .setArvosana("7")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(1)
                .setArvosana("8")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(2)
                .setArvosana("S")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(3)
                .setArvosana("S")
                .build()
                .build();
        // vahvistamaton suoritus
        oppijaBuilder.suoritus()
                .setKymppiluokka()
                .setVahvistettu(true)
                .setValmistuminen(HAKUKAUDELLA)
                .setKeskeytynyt()
                .arvosana()
                .setAine("AI")
                .setArvosana("5")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(1)
                .setArvosana("5")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(2)
                .setArvosana("9")
                .build()
                .arvosana()
                .setAine("AI")
                .setValinnainen()
                .setJarjestys(3)
                .setArvosana("5")
                .build()
                .build();


        Map<String, String> kiinnostavatAvainParit = ImmutableMap.of(
                "PK_AI", "7",
                "PK_AI_VAL1", "8",
                "PK_AI_VAL2_SUORITETTU", "true",
                "PK_AI_VAL3_SUORITETTU", "true"
        );
        Set<String> eiSaaLoytya = ImmutableSet.of(
                "PK_AI_VAL2",
                "PK_AI_VAL3");

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppijaBuilder.build(), hakemus, true);
        Assert.assertEquals("Odotettiin paras arvosana ensimmäisestä suorituksesta ja parhaat valinnaiset ensimmäisestä suorituksesta",
                hakemus.getAvaimet().stream()
                        .filter(a -> kiinnostavatAvainParit.containsKey(a.getAvain()))
                        .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)),
                kiinnostavatAvainParit);
        Assert.assertEquals("Merkintöjä VAL2:lle ja VAL3:lle ei saa löytyä suoritusmerkintöjä huolimatta",
                hakemus.getAvaimet().stream()
                        .filter(a -> eiSaaLoytya.contains(a.getAvain()))
                        .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo)),
                Collections.emptyMap());
    }
    private Set<Map.Entry<String, String>> avaimetToEntrySet(Stream<AvainArvoDTO> avaimet) {
        return avaimet.collect(Collectors.toMap(a -> a.getAvain(), a -> a.getArvo())).entrySet();
    }

    @Test
    public void testaaToAvainMap() {
        {
            Map<String, Exception> poikkeukset = Maps.newHashMap();
            HakemuksetConverterUtil.toAvainMap(Collections.emptyList(), "", "", poikkeukset);
            assertTrue(poikkeukset.isEmpty());
        }
        {
            Map<String, Exception> poikkeukset = Maps.newHashMap();
            HakemuksetConverterUtil.toAvainMap(Arrays.asList(avain("a")), "", "", poikkeukset);
            assertTrue(poikkeukset.isEmpty());
        }
        {
            Map<String, Exception> poikkeukset = Maps.newHashMap();
            HakemuksetConverterUtil.toAvainMap(Arrays.asList(avain("b"), avain("a")), "", "", poikkeukset);
            assertTrue(poikkeukset.isEmpty());
        }
        {
            Map<String, Exception> poikkeukset = Maps.newHashMap();
            HakemuksetConverterUtil.toAvainMap(Arrays.asList(avain("a"), avain("a")), "", "", poikkeukset);
            assertTrue(!poikkeukset.isEmpty());
        }
    }

    @Test
    public void ammatillisenKielikoeLuetaanSuoritusrekisteristaJaHyvaksyttyDominoi() {
        HakemusDTO h = new HakemusDTO();
        h.setHakemusoid(HAKEMUS1_OID);
        h.setAvaimet(new ArrayList<>());
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("1.10.2015").setMyontaja(HAKEMUS1_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hylatty).setMyonnetty(new LocalDate(2015, 10, 1).toDateTimeAtStartOfDay()).build()
                .build()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("18.5.2016").setMyontaja(HAKEMUS2_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hyvaksytty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .build()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("23.9.2016").setMyontaja("eri_hakemus_oid")
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hylatty).setMyonnetty(new LocalDate(2016, 9, 23).toDateTimeAtStartOfDay()).build()
                .build()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("23.9.2016").setMyontaja("eri_hakemus_oid")
                .arvosana().setAine("kielikoe").setLisatieto("SV").setAsteikko_hyvaksytty().setArvosana(hylatty).setMyonnetty(new LocalDate(2016, 9, 23).toDateTimeAtStartOfDay()).build()
                .build()
            .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
        Assert.assertEquals("true", getFirstHakemusArvo(h, "kielikoe_fi"));
        Assert.assertEquals("false", getFirstHakemusArvo(h, "kielikoe_sv"));
    }

    @Test
    public void ammatillisenKielikoetietoHakemukseltaYlikirjoitetaanSuoritusrekisterista() {
        HakemusDTO h = new HakemusDTO();
        h.setHakemusoid(HAKEMUS1_OID);
        h.setAvaimet(new ArrayList<>() {{
            add(new AvainArvoDTO("kielikoe_fi", "true"));
        }});
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("18.5.2016").setMyontaja(HAKEMUS1_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hylatty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .build()
            .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
        Assert.assertEquals("false", getFirstHakemusArvo(h, "kielikoe_fi"));
    }

    @Test
    public void ammatillisenKielikokeenOsallistumistietoTuleeMyosSuoritusrekisterista() {
        HakemusDTO h = new HakemusDTO();
        h.setHakemusoid(HAKEMUS1_OID);
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("18.5.2016").setMyontaja(HAKEMUS1_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hyvaksytty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .arvosana().setAine("kielikoe").setLisatieto("SV").setAsteikko_hyvaksytty().setArvosana(ei_osallistunut).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .build()
            .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
        Assert.assertEquals(OSALLISTUI.name(), getFirstHakemusArvo(h, "kielikoe_fi-OSALLISTUMINEN"));
        Assert.assertEquals(EI_OSALLISTUNUT.name(), getFirstHakemusArvo(h, "kielikoe_sv-OSALLISTUMINEN"));
        Assert.assertEquals("true", getFirstHakemusArvo(h, "kielikoe_fi"));
        Assert.assertEquals("", getFirstHakemusArvo(h, "kielikoe_sv"));
    }

    @Test
    public void ammatillisenKielikokeenOsallistumistiedoksiTuleeMerkitsemattaJosSuoritusOnEriHakemukselta() {
        HakemusDTO h = new HakemusDTO();
        h.setHakemusoid(HAKEMUS1_OID);
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("18.5.2016").setMyontaja(HAKEMUS2_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hyvaksytty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .arvosana().setAine("kielikoe").setLisatieto("SV").setAsteikko_hyvaksytty().setArvosana(ei_osallistunut).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .build()
            .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
        Assert.assertEquals(MERKITSEMATTA.name(), getFirstHakemusArvo(h, "kielikoe_fi-OSALLISTUMINEN"));
        Assert.assertEquals(MERKITSEMATTA.name(), getFirstHakemusArvo(h, "kielikoe_sv-OSALLISTUMINEN"));
        Assert.assertEquals("true", getFirstHakemusArvo(h, "kielikoe_fi"));
        Assert.assertEquals("", getFirstHakemusArvo(h, "kielikoe_sv"));
    }

    @Test
    public void useampiAmmatillisenKielikoeArvosanatietoSamalleSuoritukselleSurestaPalauttaaHyvaksytynArvosananJosSellainenLoytyy() {
        HakemusDTO h = new HakemusDTO();
        h.setHakemusoid(HAKEMUS1_OID);
        h.setAvaimet(new ArrayList<>());
        Oppija oppijaJollaOnSekaHyvaksyttyEttaHylattyArvosana = new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("18.5.2016").setMyontaja(HAKEMUS1_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hylatty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hyvaksytty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .build()
            .build();
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppijaJollaOnSekaHyvaksyttyEttaHylattyArvosana, h, true);
        Assert.assertEquals("true", getFirstHakemusArvo(h, "kielikoe_fi"));

        h.setAvaimet(new ArrayList<>());
        Oppija oppijaJollaOnVainHylattyjaArvosanoja = new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("18.5.2016").setMyontaja(HAKEMUS2_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hylatty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hylatty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .build()
            .build();
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppijaJollaOnVainHylattyjaArvosanoja, h, true);
        Assert.assertEquals("false", getFirstHakemusArvo(h, "kielikoe_fi"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ammatillisenKielikoeArvosanatiedonAsteikkoTuleeOllaHyvaksytty() {
        HakemusDTO h = new HakemusDTO();
        h.setHakemusoid(HAKEMUS1_OID);
        h.setAvaimet(new ArrayList<>());
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("18.5.2016").setMyontaja(HAKEMUS1_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_Osakoe().setArvosana(hylatty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .build()
            .build();

        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", new ParametritDTO(), new HashMap<>(), oppija, h, true);
    }

    @Test
    public void ammatillisenKielikoeArvosanaaEiHuomioidaJosSeOnValintalaskennanJalkeen() {
        HakemusDTO h = new HakemusDTO();
        h.setHakemusoid(HAKEMUS1_OID);
        h.setAvaimet(new ArrayList<>() );
        Oppija oppija = new SuoritusrekisteriSpec.OppijaBuilder()
            .suoritus().setAmmatillisenKielikoe().setVahvistettu(true).setValmis().setValmistuminen("18.5.2016").setMyontaja(HAKEMUS2_OID)
                .arvosana().setAine("kielikoe").setLisatieto("FI").setAsteikko_hyvaksytty().setArvosana(hyvaksytty).setMyonnetty(new LocalDate(2016, 5, 18).toDateTimeAtStartOfDay()).build()
                .build()
            .build();

        ParametritDTO parametritJoissLaskentaAlkoiEnnenSuoritusta = SuoritusrekisteriSpec.laskennanalkamisparametri(new LocalDate(2016, 5, 16).toDateTimeAtStartOfDay());
        HakemuksetConverterUtil.mergeKeysOfOppijaAndHakemus(false, haku, "", parametritJoissLaskentaAlkoiEnnenSuoritusta, new HashMap<>(), oppija, h, true);
        assertThat(firstHakemusArvo(h, "kielikoe_fi"), OptionalMatchers.isEmpty());
    }

    private static String getFirstHakemusArvo(HakemusDTO hakemusDTO, String avain) {
        return firstHakemusArvo(hakemusDTO, avain).get();
    }

    private static Optional<String> firstHakemusArvo(HakemusDTO hakemusDTO, String avain) {
        return hakemusDTO
                .getAvaimet()
                .stream()
                .filter(k -> k.getAvain().equals(avain))
                .map(AvainArvoDTO::getArvo)
                .findFirst();
    }

    private AvainArvoDTO avain(String avain) {
        AvainArvoDTO a = new AvainArvoDTO();
        a.setAvain(avain);
        return a;
    }
}
