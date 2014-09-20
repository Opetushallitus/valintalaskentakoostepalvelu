package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia;

import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;

/**
 * 
 * @author jussija
 *
 *         Yksi palvelukutsukerrallaan palvelukutsujonoa tyhjentava strategia
 */
public interface PalvelukutsuStrategia {

	void laitaPalvelukutsuJonoon(Palvelukutsu palvelukutsu,
			Consumer<Palvelukutsu> takaisinkutsu);

	void aloitaUusiPalvelukutsu();

}
