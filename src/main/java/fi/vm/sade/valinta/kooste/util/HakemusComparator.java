package fi.vm.sade.valinta.kooste.util;

import java.util.Comparator;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;

public class HakemusComparator implements Comparator<HakemusWrapper> {

    @Override
    public int compare(HakemusWrapper h1, HakemusWrapper h2) {
        int i = h1.getSukunimi().toUpperCase().compareTo(h2.getSukunimi().toUpperCase());
        if (i == 0) {
            return h1.getEtunimi().toUpperCase().compareTo(h2.getEtunimi().toUpperCase());
        } else {
            return i;
        }
    }

    private HakemusComparator() {

    }

    public static HakemusComparator DEFAULT = new HakemusComparator();

}
