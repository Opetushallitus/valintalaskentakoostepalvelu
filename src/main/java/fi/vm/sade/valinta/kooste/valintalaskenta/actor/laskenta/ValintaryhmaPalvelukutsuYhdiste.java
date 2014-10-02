package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.LisatiedotPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.SuoritusrekisteriPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintaryhmaPalvelukutsuYhdiste {

	private final LisatiedotPalvelukutsu lisatiedotPalvelukutsu;
	private final HakemuksetPalvelukutsu hakemuksetPalvelukutsu;
	private final ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu;
	private final HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu;
	private final SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsut;

	public ValintaryhmaPalvelukutsuYhdiste(
			LisatiedotPalvelukutsu lisatiedotPalvelukutsu,
			HakemuksetPalvelukutsu hakemuksetPalvelukutsu,
			ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu,
			HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu,
			SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsut) {
		this.lisatiedotPalvelukutsu = lisatiedotPalvelukutsu;
		this.hakemuksetPalvelukutsu = hakemuksetPalvelukutsu;
		this.valintaperusteetPalvelukutsu = valintaperusteetPalvelukutsu;
		this.hakijaryhmatPalvelukutsu = hakijaryhmatPalvelukutsu;
		this.suoritusrekisteriPalvelukutsut = suoritusrekisteriPalvelukutsut;
	}

	public HakemuksetPalvelukutsu getHakemuksetPalvelukutsu() {
		return hakemuksetPalvelukutsu;
	}

	public HakijaryhmatPalvelukutsu getHakijaryhmatPalvelukutsu() {
		return hakijaryhmatPalvelukutsu;
	}

	public LisatiedotPalvelukutsu getLisatiedotPalvelukutsu() {
		return lisatiedotPalvelukutsu;
	}

	public SuoritusrekisteriPalvelukutsu getSuoritusrekisteriPalvelukutsut() {
		return suoritusrekisteriPalvelukutsut;
	}

	public ValintaperusteetPalvelukutsu getValintaperusteetPalvelukutsu() {
		return valintaperusteetPalvelukutsu;
	}

}
