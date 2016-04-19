package fi.vm.sade.valinta.kooste.erillishaku.excel;

public enum Sukupuoli {
    MIES, NAINEN, EI_SUKUPUOLTA;

    public static Sukupuoli fromString(String s) {
        try {
            if("1".equals(s)) {
                return MIES;
            } else if("2".equals(s)) {
                return NAINEN;
            } else {
                return Sukupuoli.valueOf(s);
            }
        } catch(IllegalArgumentException e) {
            return EI_SUKUPUOLTA;
        }
    }

    public static String toHenkiloString(Sukupuoli sukupuoli) {
        switch (sukupuoli) {
            case MIES: return "1";
            case NAINEN: return "2";
            default: return "";
        }
    }

    public static String toHenkiloString(String sukupuoli) {
        if(MIES.name().equalsIgnoreCase(sukupuoli)) {
            return "1";
        } else if(NAINEN.name().equalsIgnoreCase(sukupuoli)) {
            return "2";
        }
        return "";
    }
}
