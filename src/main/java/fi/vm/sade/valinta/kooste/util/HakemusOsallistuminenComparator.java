package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;

import java.util.Comparator;

public class HakemusOsallistuminenComparator implements Comparator<HakemusOsallistuminenDTO> {

    @Override
    public int compare(HakemusOsallistuminenDTO o1, HakemusOsallistuminenDTO o2) {
        String o1sukunimi = o1.getSukunimi().toUpperCase();
        if (o1 == null || o2 == null || o1sukunimi == null
                || o2.getSukunimi() == null) {
            return 0;
        } else {
            int c = o1sukunimi.compareTo(o2.getSukunimi().toUpperCase());
            if (c == 0) {
                String o1etunimi = o1.getEtunimi().toUpperCase();
                if (o1etunimi == null || o2.getEtunimi() == null) {
                    return 0;
                } else {
                    return o1etunimi.compareTo(o2.getEtunimi().toUpperCase());
                }
            } else {
                return c;
            }
        }
    }

    private HakemusOsallistuminenComparator() {
    }

    public static HakemusOsallistuminenComparator DEFAULT = new HakemusOsallistuminenComparator();
}
