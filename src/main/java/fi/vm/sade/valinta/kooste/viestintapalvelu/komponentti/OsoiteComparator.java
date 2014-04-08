package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.Comparator;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class OsoiteComparator implements Comparator<Osoite> {

	public static final OsoiteComparator ASCENDING = new OsoiteComparator();

	private OsoiteComparator() {

	}

	@Override
	public int compare(Osoite o1, Osoite o2) {
		try {
			int l = o1.getLastName().compareTo(o2.getLastName());
			if (l == 0) {
				return o1.getFirstName().compareTo(o2.getFirstName());
			}
			return l;
		} catch (Exception e) {
			return 0;
		}
	}

}
