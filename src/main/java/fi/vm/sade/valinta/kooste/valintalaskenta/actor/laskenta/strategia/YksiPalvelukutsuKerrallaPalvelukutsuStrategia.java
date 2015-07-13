package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.PalvelukutsunUudelleenAktivointiPoikkeus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YksiPalvelukutsuKerrallaPalvelukutsuStrategia extends AbstraktiPalvelukutsuStrategia {
    private final static Logger LOG = LoggerFactory.getLogger(YksiPalvelukutsuKerrallaPalvelukutsuStrategia.class);

    public void aloitaUusiPalvelukutsu() {
        try {
            if (aloitettujaPalvelukutsuja() == 0) {
                kaynnistaJonossaSeuraavaPalvelukutsu();
            }
        } catch (PalvelukutsunUudelleenAktivointiPoikkeus p) {
            LOG.info("Yritettiin aktivoida palvelua joka oli mahdollisesti peruutettu.");
        } catch (Exception e) {
            LOG.error("Kaynnistyksessa tapahtui virhe!", e);
        }
    }
}
