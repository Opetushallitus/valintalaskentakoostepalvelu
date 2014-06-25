package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.util.List;

import com.google.common.collect.Lists;

public class ValintatapajonoDataRiviListAdapter implements
		ValintatapajonoDataRiviKuuntelija {

	private final List<ValintatapajonoRivi> rivit = Lists.newArrayList();

	@Override
	public void valintatapajonoDataRiviTapahtuma(
			ValintatapajonoRivi valintatapajonoRivi) {
		rivit.add(valintatapajonoRivi);
	}

	public List<ValintatapajonoRivi> getRivit() {
		return rivit;
	}
}
