package fi.vm.sade.valinta.kooste.util;

import java.util.Comparator;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;

public class ApplicationAdditionalDataComparator implements
		Comparator<ApplicationAdditionalDataDTO> {

	public static final ApplicationAdditionalDataComparator ASCENDING = new ApplicationAdditionalDataComparator();

	private ApplicationAdditionalDataComparator() {

	}

	@Override
	public int compare(ApplicationAdditionalDataDTO o1,
			ApplicationAdditionalDataDTO o2) {
		try {
			int l = o1.getLastName().toUpperCase()
					.compareTo(o2.getLastName().toUpperCase());
			if (l == 0) {
				return o1.getFirstNames().toUpperCase()
						.compareTo(o2.getFirstNames().toUpperCase());
			}
			return l;
		} catch (Exception e) {
			return 0;
		}
	}

}
