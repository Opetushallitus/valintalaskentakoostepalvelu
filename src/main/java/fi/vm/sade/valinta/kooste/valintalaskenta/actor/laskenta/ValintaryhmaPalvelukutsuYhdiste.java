package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.SuoritusrekisteriPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintaryhmaPalvelukutsuYhdiste {

	private final HakemuksetPalvelukutsu hakemuksetPalvelukutsu;
	private final ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu;
	private final HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu;
	private final SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu;

	public ValintaryhmaPalvelukutsuYhdiste(
			HakemuksetPalvelukutsu hakemuksetPalvelukutsu,
			ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu,
			HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu,
			SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu) {
		this.hakemuksetPalvelukutsu = hakemuksetPalvelukutsu;
		this.valintaperusteetPalvelukutsu = valintaperusteetPalvelukutsu;
		this.hakijaryhmatPalvelukutsu = hakijaryhmatPalvelukutsu;
		this.suoritusrekisteriPalvelukutsu = suoritusrekisteriPalvelukutsu;
	}

	public HakemuksetPalvelukutsu getHakemuksetPalvelukutsu() {
		return hakemuksetPalvelukutsu;
	}

	public HakijaryhmatPalvelukutsu getHakijaryhmatPalvelukutsu() {
		return hakijaryhmatPalvelukutsu;
	}

	public SuoritusrekisteriPalvelukutsu getSuoritusrekisteriPalvelukutsut() {
		return suoritusrekisteriPalvelukutsu;
	}

	public ValintaperusteetPalvelukutsu getValintaperusteetPalvelukutsu() {
		return valintaperusteetPalvelukutsu;
	}

}
