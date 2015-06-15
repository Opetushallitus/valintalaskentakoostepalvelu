package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

/**
 *         Poikkeus heitetaan jos samaa palvelukutsua aktivoidaan useaan
 *         otteeseen tai peruutettua palvelukutsua yritetaan aktivoida
 */
public class PalvelukutsunUudelleenAktivointiPoikkeus extends RuntimeException {

    private static final long serialVersionUID = 6784859988454867570L;

    public PalvelukutsunUudelleenAktivointiPoikkeus() {
        super();
    }
}
