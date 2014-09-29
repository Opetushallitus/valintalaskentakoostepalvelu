package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Collection;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaLuontiJaHaut {
	private final KelaLuonti luonti;
	private final Collection<Haku> haut;

	public KelaLuontiJaHaut(KelaLuonti kelaLuonti, Collection<Haku> haut) {
		this.luonti = kelaLuonti;
		this.haut = haut;
	}

	public Collection<Haku> getHaut() {
		return haut;
	}

	public KelaLuonti getLuonti() {
		return luonti;
	}
}
