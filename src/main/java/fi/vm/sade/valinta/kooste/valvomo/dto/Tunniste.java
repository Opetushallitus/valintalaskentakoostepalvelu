package fi.vm.sade.valinta.kooste.valvomo.dto;

import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public class Tunniste {
    private final String tunniste;
    private final String tyyppi;

    public Tunniste() {
        tunniste = null;
        tyyppi = null;
    }

    public Tunniste(String tunniste, String tyyppi) {
        this.tunniste = tunniste;
        this.tyyppi = tyyppi;
    }

    public String getTunniste() {
        return tunniste;
    }

    public String getTyyppi() {
        return tyyppi;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tunniste) {
            Tunniste t0 = (Tunniste) obj;
            return Optional.ofNullable(tunniste).orElse(StringUtils.EMPTY).equals(t0.tunniste) &&
                    Optional.ofNullable(tyyppi).orElse(StringUtils.EMPTY).equals(t0.tyyppi);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder(tyyppi).append(": ").append(tunniste).toString();
    }
}
