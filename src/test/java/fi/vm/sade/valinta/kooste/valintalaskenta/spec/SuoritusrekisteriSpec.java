package fi.vm.sade.valinta.kooste.valintalaskenta.spec;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import org.joda.time.DateTime;

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
    public AvainSuoritusBuilder avainsuoritustieto() {
        return new AvainSuoritusBuilder();
    }
    public AvainBuilder avainsuoritustieto(String avain) {
        return new AvainSuoritusBuilder().avain(avain);
    }
    public static class ArvosanaBuilder {
        private final SuoritusBuilder suoritus;
        private final Arvosana arvosana = new Arvosana();

        public ArvosanaBuilder(SuoritusBuilder suoritus) {
            this.suoritus = suoritus;
        }
        public ArvosanaBuilder setMyonnetty(String myonnetty) {
            arvosana.setMyonnetty(myonnetty);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setMyonnetty(DateTime myonnetty) {
            arvosana.setMyonnetty(ArvosanaWrapper.ARVOSANA_DTF.print(myonnetty));
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
        public ArvosanaBuilder setArvosana(String a) {
            arvosana.getArvio().setArvosana(a);
            return ArvosanaBuilder.this;
        }
        public ArvosanaBuilder setPisteet(Integer pisteet) {
            arvosana.getArvio().setPisteet(pisteet);
            return ArvosanaBuilder.this;
        }

        public SuoritusBuilder build() {
            suoritus.suoritusJaArvosanat.getArvosanat().add(arvosana);
            return suoritus;
        }
    }

    public static class SuoritusBuilder {
        private final OppijaBuilder oppija;
        private final SuoritusJaArvosanat suoritusJaArvosanat = new SuoritusJaArvosanat();
        private final Suoritus suoritus = new Suoritus();
        public SuoritusBuilder(OppijaBuilder oppija) {
            this.oppija = oppija;
            this.suoritusJaArvosanat.setSuoritus(suoritus);
        }
        public SuoritusBuilder setSource(String source) {
            suoritus.setSource(source);
            return SuoritusBuilder.this;
        }
        public SuoritusBuilder setHenkiloOid(String henkiloOid) {
            suoritus.setHenkiloOid(henkiloOid);
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
        public SuoritusBuilder setKymppiluokka() {
            suoritus.setKomo(SuoritusJaArvosanatWrapper.PK_10_KOMO);
            return SuoritusBuilder.this;
        }
        public ArvosanaBuilder arvosana() {
            return new ArvosanaBuilder(SuoritusBuilder.this);
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

        public Oppija build() {
            return oppija;
        }
    }

    public OppijaBuilder oppija() {
        return new OppijaBuilder();
    }

    protected static ParametritDTO laskennanalkamisparametri(DateTime alkamisaika) {
        ParametritDTO p = new ParametritDTO();
        ParametriDTO p0 = new ParametriDTO();
        p0.setDateStart(alkamisaika.toDate());
        p.setPH_VLS(p0);
        return p;
    }
}
