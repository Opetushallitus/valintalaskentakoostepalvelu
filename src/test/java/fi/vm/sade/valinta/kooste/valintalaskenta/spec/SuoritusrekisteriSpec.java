package fi.vm.sade.valinta.kooste.valintalaskenta.spec;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.ArvosanaWrapper;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import org.joda.time.DateTime;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author Jussi Jartamo
 */
public class SuoritusrekisteriSpec {
    public static class SuoritustietoBuilder {
        private final Map<String,String> m = Maps.newHashMap();
        private final AvainBuilder a;
        public SuoritustietoBuilder(AvainBuilder a) {
            this.a = a;
        }
        public SuoritustietoBuilder suoritustieto(String suoritustieto, String arvo) {
            m.put(suoritustieto,arvo);
            return this;
        }
        public AvainBuilder build() {
            a.l.add(m);
            return a;
        }
    }
    public static class AvainBuilder {
        private final String avain;
        private final List<Map<String,String>> l = Lists.newArrayList();
        private final AvainSuoritusBuilder a;
        public AvainBuilder(AvainSuoritusBuilder a, String avain) {
            this.a = a;
            this.avain = avain;
        }
        public SuoritustietoBuilder suoritustieto(String s, String a) {
            return new SuoritustietoBuilder(this).suoritustieto(s,a);
        }

        public AvainSuoritusBuilder build() {
            a.as.add(new AvainMetatiedotDTO(avain, l));
            return a;
        }
    }
    public static class AvainSuoritusBuilder {
        private final List<AvainMetatiedotDTO> as = Lists.newArrayList();
        public AvainBuilder avain(String avain) {
            return new AvainBuilder(this, avain);
        }
        public List<AvainMetatiedotDTO> build() {
            return as;
        }
        public AvainMetatiedotDTO buildOne() {
            return as.iterator().next();
        }
    }
    public static AvainSuoritusBuilder avainsuoritustieto() {
        return new AvainSuoritusBuilder();
    }
    public static AvainBuilder avainsuoritustieto(String avain) {
        return new AvainSuoritusBuilder().avain(avain);
    }
    public static class ArvosanaBuilder {
        private final SuoritusBuilder suoritus;
        private final Arvosana arvosana = new Arvosana();

        public ArvosanaBuilder(SuoritusBuilder suoritus) {
            this.suoritus = suoritus;
        }
        public ArvosanaBuilder setId(String id) {
            arvosana.setId(id);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setMyonnetty(String myonnetty) {
            arvosana.setMyonnetty(myonnetty);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setMyonnetty(DateTime myonnetty) {
            arvosana.setMyonnetty(ArvosanaWrapper.ARVOSANA_DTF.print(myonnetty));
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setSource(String sourceOid) {
            arvosana.setSource(sourceOid);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setAsteikko(String asteikko) {
            arvosana.getArvio().setAsteikko(asteikko);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setAine(String aine) {
            arvosana.setAine(aine);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setLisatieto(String lisatieto) {
            arvosana.setLisatieto(lisatieto);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setValinnainen() {
            arvosana.setValinnainen(true);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setAsteikko_4_10() {
            arvosana.getArvio().setAsteikko("4-10");
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setAsteikko_1_5() {
            arvosana.getArvio().setAsteikko("1-5");
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setAsteikko_Osakoe() {
            arvosana.getArvio().setAsteikko("OSAKOE");
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setAsteikko_yo() {
            arvosana.getArvio().setAsteikko("YO");
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setAsteikko_hyvaksytty() {
            arvosana.getArvio().setAsteikko("HYVAKSYTTY");
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setArvosana(String a) {
            arvosana.getArvio().setArvosana(a);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setArvosana(SureHyvaksyttyArvosana arvosana) {
            return setArvosana(arvosana.name());
        }
        public ArvosanaBuilder setPisteet(Integer pisteet) {
            arvosana.getArvio().setPisteet(pisteet);
            return ArvosanaBuilder.this;
        }

        public ArvosanaBuilder setAineyhdistelmarooli(String aineyhdistelmarooli) {
            arvosana.getLahdeArvot().put("aineyhdistelmarooli", aineyhdistelmarooli);
            return ArvosanaBuilder.this;
        }

        public ArvosanaBuilder setKoetunnus(String koetunnus) {
            arvosana.getLahdeArvot().put("koetunnus", koetunnus);
            return ArvosanaBuilder.this;
        }

        public ArvosanaBuilder setJarjestys(int jarjestys) {
            arvosana.setJarjestys(jarjestys);
            return ArvosanaBuilder.this;
        }

        public SuoritusBuilder build() {
            if(arvosana.isValinnainen()) {// lisataan indeksi jos valinnainen
                arvosana.setJarjestys(
                        (int)
                                suoritus.suoritusJaArvosanat.getArvosanat().stream().filter(
                                        a -> {
                                            return a.isValinnainen() && arvosana.getAine().equals(a.getAine());
                                        }
                                ).count());
            }
            suoritus.suoritusJaArvosanat.getArvosanat().add(arvosana);
            return suoritus;
        }
    }

    public static class SuoritusBuilder {
        private final OppijaBuilder oppija;
        private final SuoritusJaArvosanat suoritusJaArvosanat = new SuoritusJaArvosanat();
        private final Suoritus suoritus = new Suoritus();
        public SuoritusBuilder() {
            this.oppija = null;
            this.suoritusJaArvosanat.setSuoritus(suoritus);
        }
        public SuoritusBuilder(OppijaBuilder oppija) {
            this.oppija = oppija;
            this.suoritusJaArvosanat.setSuoritus(suoritus);
        }
        public SuoritusBuilder setId(String id) {
            suoritus.setId(id);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setSource(String source) {
            suoritus.setSource(source);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setHenkiloOid(String henkiloOid) {
            suoritus.setHenkiloOid(henkiloOid);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setVahvistettu(boolean vahvistettu) {
            suoritus.setVahvistettu(vahvistettu);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setValmis() {
            suoritus.setTila("VALMIS");
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setKesken() {
            suoritus.setTila("KESKEN");
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setKeskeytynyt() {
            suoritus.setTila("KESKEYTYNYT");
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setValmistuminen(String valmistuminen) {
            suoritus.setValmistuminen(valmistuminen);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setYksilollistaminen(String y) {
            suoritus.setYksilollistaminen(y);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setPerusopetus() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.PK_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setKomo(String komo) {
            suoritus.setKomo(komo);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setYo() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.YO_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setLukio() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.LK_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setMyontaja(String myontaja) {
            suoritus.setMyontaja(myontaja);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setKymppiluokka() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.PK_10_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setPerusopetuksenOppiaineenOppimaara() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.POO_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setUlkomainenKorvaava() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.ULKOMAINENKORVAAVA);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setValmentava() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.PK_VALMENTAVA);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setAmmatilliseenValmistava() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.PK_AMMATILLISEENVALMISTAVA);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setLukioonValmistava() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.PK_LUKIOON_VALMISTAVA);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setLisaopetusTalous() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.PK_LISAOPETUSTALOUS_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setAmmattistartti() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.PK_AMMATTISTARTTI_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setTelma() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.TELMA_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setValma() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.VALMA_KOMO);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setSuoritusKieli(String suoritusKieli) {
            suoritus.setSuoritusKieli(suoritusKieli);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setAmmatillisenKielikoe() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
            return SuoritusBuilder.this;
        }
        public ArvosanaBuilder arvosana() {
            return new ArvosanaBuilder(SuoritusBuilder.this);
        }

        public SuoritusJaArvosanat done() {
            return suoritusJaArvosanat;
        }

        public OppijaBuilder build() {
            SuoritusBuilder.this.oppija.oppija.getSuoritukset().add(suoritusJaArvosanat);
            return SuoritusBuilder.this.oppija;
        }
    }

    public static class OppijaBuilder {
        private final Oppija oppija = new Oppija();

        public SuoritusBuilder suoritus() {
            return new SuoritusBuilder(OppijaBuilder.this);
        }

        public OppijaBuilder setOppijanumero(String oppijanumero) {
            oppija.setOppijanumero(oppijanumero);
            return this;
        }

        public Oppija build() {
            oppija.setEnsikertalainen(true);
            return oppija;
        }
    }

    public OppijaBuilder oppija() {
        return new OppijaBuilder();
    }

    public static ParametritDTO laskennanalkamisparametri(DateTime alkamisaika) {
        ParametritDTO p = new ParametritDTO();
        ParametriDTO p0 = new ParametriDTO();
        p0.setDateStart(alkamisaika.toDate());
        p.setPH_VLS(p0);
        return p;
    }
}
