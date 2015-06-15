package fi.vm.sade.valinta.kooste.valvomo.dto;

import java.util.Collection;

public abstract class Tyo {

    public double getProsentteina() {
        int kokonaismaara = getKokonaismaara();
        if (kokonaismaara == 0) {
            return 0d;
        }
        return ((double) getTehty()) / ((double) kokonaismaara);
    }

    abstract public Collection<Exception> getPoikkeukset();

    abstract public int getTehty();

    abstract public int getKokonaismaara();

    abstract public String getNimi();

}
