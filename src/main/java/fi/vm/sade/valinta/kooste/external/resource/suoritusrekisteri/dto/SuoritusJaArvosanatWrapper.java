package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Jussi Jartamo
 */
public class SuoritusJaArvosanatWrapper {
    public static final String YO_KOMO = "1.2.246.562.5.2013061010184237348007";
    public static final String PK_KOMO = "1.2.246.562.13.62959769647";
    public static final String PK_10_KOMO = "1.2.246.562.5.2013112814572435044876";
    public static final String LK_KOMO = "TODO lukio komo oid";
    public static final String AM_KOMO = "TODO ammatillinen komo oid";
    private static final Map<String, String> KOMO_TO_STRING_MAPPER = createKomoToStringMapper();
    private static Map<String, String> createKomoToStringMapper() {
        Map<String, String> tmp = Maps.newHashMap();
        tmp.put(YO_KOMO, "YO-suoritus");
        tmp.put(PK_KOMO, "perusopetussuoritus");
        tmp.put(PK_10_KOMO, "lisäopetussuoritus");
        tmp.put(LK_KOMO, "lukiosuoritus");
        tmp.put(AM_KOMO, "ammatillinen suoritus");
        return Collections.unmodifiableMap(tmp);
    }
    private final SuoritusJaArvosanat suoritusJaArvosanat;

    public static SuoritusJaArvosanatWrapper wrap(SuoritusJaArvosanat s) {
        return new SuoritusJaArvosanatWrapper(s);
    }

    public boolean isValmis() {
        return "VALMIS".equals(suoritusJaArvosanat.getSuoritus().getTila());
    }
    public boolean isKeskeytynyt() {
        return "KESKEYTYNYT".equals(suoritusJaArvosanat.getSuoritus().getTila());
    }
    public String komoToString() {
        return Optional.ofNullable(KOMO_TO_STRING_MAPPER.get(suoritusJaArvosanat.getSuoritus().getKomo())).orElse("Tuntematon suoritus " + suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public SuoritusJaArvosanatWrapper(SuoritusJaArvosanat suoritusJaArvosanat) {
        this.suoritusJaArvosanat = suoritusJaArvosanat;
    }

    public SuoritusJaArvosanat getSuoritusJaArvosanat() {
        return suoritusJaArvosanat;
    }

    public boolean isYoTutkinto() {
        return YO_KOMO.equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isPerusopetus() {
        return PK_KOMO.equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isLisaopetus() { // KYMPPILUOKKA
        return PK_10_KOMO.equals(suoritusJaArvosanat.getSuoritus().getKomo());
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
        return LK_KOMO.equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isAmmatillinen() {
        return AM_KOMO.equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }
    public boolean isLukioonValmistava() {
        return "1.2.246.562.5.2013112814572429142840".equals(suoritusJaArvosanat.getSuoritus().getKomo());
    }

}
