package fi.vm.sade.valinta.kooste.pistesyotto.dto;

public enum Osallistumistieto {
    OSALLISTUI(true), TOISESSA_HAKUTOIVEESSA(false), TOISELLA_HAKEMUKSELLA(false), EI_KUTSUTTU(false);

    private final boolean muokattavissa;

    Osallistumistieto(boolean muokattavissa) {
        this.muokattavissa = muokattavissa;
    }
}
