package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

import java.util.function.Function;

/**
 * @author Jussi Jartamo
 */
public class SuoritusJaArvosanatWrapper {
    private final SuoritusJaArvosanat suoritusJaArvosanat;

    public static SuoritusJaArvosanatWrapper wrap(SuoritusJaArvosanat s) {
        return new SuoritusJaArvosanatWrapper(s);
    }

    public SuoritusJaArvosanatWrapper(SuoritusJaArvosanat suoritusJaArvosanat) {
        this.suoritusJaArvosanat = suoritusJaArvosanat;
    }

    public SuoritusJaArvosanat getSuoritusJaArvosanat() {
        return suoritusJaArvosanat;
    }

    public boolean isYoTutkinto() {
        return "1.2.246.562.5.2013061010184237348007".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isPerusopetus() {
        return "1.2.246.562.13.62959769647".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isLisaopetus() {
        return "1.2.246.562.5.2013112814572435044876".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isAmmattistartti() {
        return "1.2.246.562.5.2013112814572438136372".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isValmentava() {
        return "1.2.246.562.5.2013112814572435755085".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isAmmatilliseenValmistava() {
        return "1.2.246.562.5.2013112814572441001730".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isUlkomainenKorvaava() {
        return "1.2.246.562.13.86722481404".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isLukio() {
        return "TODO lukio komo oid".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isAmmatillinen() {
        return "TODO ammatillinen komo oid".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isLukioonValmistava() {
        return "1.2.246.562.5.2013112814572429142840".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }

}
