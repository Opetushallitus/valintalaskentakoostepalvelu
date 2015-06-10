package fi.vm.sade.valinta.kooste.excel;

import java.util.Collection;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.MonivalintaArvo;

public class DataRivi extends Rivi {
    private final Collection<Collection<Arvo>> s;

    public DataRivi(Collection<Collection<Arvo>> s) {
        super();
        this.s = s;
    }

    @Override
    public boolean validoi(Rivi rivi) {
        return true;
    }

    private Rivi asRivi(Collection<Arvo> arvot) {
        RiviBuilder riviBuilder = new RiviBuilder();
        for (Arvo arvo : arvot) {
            if (ArvoTyyppi.MONIVALINTA.equals(arvo.getTyyppi())) {
                MonivalintaArvo monivalinta = arvo.asMonivalintaArvo();

                riviBuilder.addSolu(monivalinta.asMonivalinta());

            } else if (ArvoTyyppi.NUMERO.equals(arvo.getTyyppi())) {
                riviBuilder.addSolu(arvo.asNumeroArvo().asNumero());
            } else {
                riviBuilder.addSolu(arvo.asTekstiArvo().asTeksti());
            }
        }
        return riviBuilder.build();
    }

    @Override
    public Collection<Rivi> getToisteisetRivit() {
        Collection<Rivi> rivit = Lists.newArrayList();
        for (Collection<Arvo> arvot : s) {
            rivit.add(asRivi(arvot));
        }
        return rivit;
    }
}
