package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia;

import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;

public interface PalvelukutsuJaPalvelukutsuStrategia {
    void laitaPalvelukutsuTyojonoon(Consumer<Palvelukutsu> takaisinkutsu);

    void peruuta();
}
