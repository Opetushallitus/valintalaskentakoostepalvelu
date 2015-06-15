package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;

public interface LaskentaPalvelukutsu extends Palvelukutsu {
    String getHakukohdeOid();

    HakukohdeTila getHakukohdeTila();

    void laitaTyojonoon(Consumer<LaskentaPalvelukutsu> takaisinkutsu);
}
