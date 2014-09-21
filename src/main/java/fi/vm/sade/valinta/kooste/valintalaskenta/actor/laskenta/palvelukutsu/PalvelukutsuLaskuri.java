package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.Laskuri;

public class PalvelukutsuLaskuri extends Laskuri {
	private static final Logger LOG = LoggerFactory
			.getLogger(PalvelukutsuLaskuri.class);

	public PalvelukutsuLaskuri(int palvelukutsuja) {
		super(palvelukutsuja);
	}

	public int palvelukutsuSaapui() {
		int laskuriNyt = kasvataLaskuria();
		return laskuriNyt;
	}
}
