package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Collection;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaLuontiJaRivit {
	private final KelaLuonti luonti;
	private final Collection<KelaHakijaRivi> rivit;

	public KelaLuontiJaRivit(KelaLuonti luonti, Collection<KelaHakijaRivi> rivit) {
		this.luonti = luonti;
		this.rivit = rivit;
	}

	public KelaLuonti getLuonti() {
		return luonti;
	}

	public Collection<KelaHakijaRivi> getRivit() {
		return rivit;
	}
}
