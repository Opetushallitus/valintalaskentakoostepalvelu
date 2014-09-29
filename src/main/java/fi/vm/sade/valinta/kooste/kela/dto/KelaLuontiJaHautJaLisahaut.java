package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Collection;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaLuontiJaHautJaLisahaut {
	private final KelaLuonti luonti;
	private final Collection<HakuV1RDTO> haut;
	private final Collection<HakuV1RDTO> lisahaut;

	public KelaLuontiJaHautJaLisahaut(KelaLuonti luonti,
			Collection<HakuV1RDTO> haut, Collection<HakuV1RDTO> lisahaut) {
		this.luonti = luonti;
		this.haut = haut;
		this.lisahaut = lisahaut;
	}

	public Collection<HakuV1RDTO> getHaut() {
		return haut;
	}

	public Collection<HakuV1RDTO> getLisahaut() {
		return lisahaut;
	}

	public KelaLuonti getLuonti() {
		return luonti;
	}

}
