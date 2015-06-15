package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.Comparator;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

public class OsoiteComparator implements Comparator<Osoite> {
    public static final OsoiteComparator ASCENDING = new OsoiteComparator();

    private OsoiteComparator() {
    }

    @Override
    public int compare(Osoite o1, Osoite o2) {
        try {
            int l = o1.getLastName().toUpperCase().compareTo(o2.getLastName().toUpperCase());
            if (l == 0) {
                return o1.getFirstName().toUpperCase().compareTo(o2.getFirstName().toUpperCase());
            }
            return l;
        } catch (Exception e) {
            return 0;
        }
    }

}
