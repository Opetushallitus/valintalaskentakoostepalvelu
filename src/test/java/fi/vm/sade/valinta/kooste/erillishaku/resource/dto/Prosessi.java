package fi.vm.sade.valinta.kooste.erillishaku.resource.dto;

import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;

import java.util.Collection;

/**
 * @author Jussi Jartamo
 */
public class Prosessi {
    public Osatyo kokonaistyo = new Osatyo();
    public String dokumenttiId;
    public Collection<Poikkeus> poikkeukset;

    public boolean poikkeuksia() {
        return !(poikkeukset == null || poikkeukset.isEmpty());
    }

    public boolean valmis() {
        return dokumenttiId != null || kokonaistyo.valmis();
    }

    static class Osatyo {
        public int tehty = 0;
        public int kokonaismaara = 0;

        public boolean valmis() {
            return tehty == kokonaismaara;
        }
    }
}