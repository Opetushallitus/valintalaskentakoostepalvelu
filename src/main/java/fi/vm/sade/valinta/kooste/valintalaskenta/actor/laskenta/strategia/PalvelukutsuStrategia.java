package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia;

import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;

public interface PalvelukutsuStrategia {

    void laitaPalvelukutsuJonoon(Palvelukutsu palvelukutsu, Consumer<Palvelukutsu> takaisinkutsu);

    void aloitaUusiPalvelukutsu();

    void peruutaKaikki();
}
