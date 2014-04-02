package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.Comparator;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Sort descending hakutoiveet by valintatapajono priority
 */
public class HakutoiveenValintatapajonoComparator implements
		Comparator<HakutoiveenValintatapajonoDTO> {

	public static final HakutoiveenValintatapajonoComparator DESCENDING = new HakutoiveenValintatapajonoComparator(
			-1);
	public static final HakutoiveenValintatapajonoComparator ASCENDING = new HakutoiveenValintatapajonoComparator(
			1);
	public static final HakutoiveenValintatapajonoComparator DEFAULT = DESCENDING;

	private final int multiplier;

	private HakutoiveenValintatapajonoComparator(int multiplier) {
		this.multiplier = multiplier;
	}

	public int compare(HakutoiveenValintatapajonoDTO o1,
			HakutoiveenValintatapajonoDTO o2) {

		return multiplier
				* o1.getValintatapajonoPrioriteetti().compareTo(
						o2.getValintatapajonoPrioriteetti());
	}
}
