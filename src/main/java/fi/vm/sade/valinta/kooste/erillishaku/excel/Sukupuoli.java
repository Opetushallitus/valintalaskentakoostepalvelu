package fi.vm.sade.valinta.kooste.erillishaku.excel;

public enum Sukupuoli {
    MIES, NAINEN, EI_SUKUPUOLTA;

    public static Sukupuoli fromString(String s) {
        try {
            return Sukupuoli.valueOf(s);
        } catch(IllegalArgumentException e) {
            return EI_SUKUPUOLTA;
        }
    }
}
