package fi.vm.sade.valinta.kooste.valintalaskenta.spec;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import org.joda.time.DateTime;

/**
 * @author Jussi Jartamo
 */
public class SuoritusrekisteriSpec {

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
