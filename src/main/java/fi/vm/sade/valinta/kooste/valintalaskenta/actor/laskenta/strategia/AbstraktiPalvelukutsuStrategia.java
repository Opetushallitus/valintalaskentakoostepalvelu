package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia;

import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.PalvelukutsunUudelleenAktivointiPoikkeus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;

public abstract class AbstraktiPalvelukutsuStrategia implements PalvelukutsuStrategia {
    private static final Logger LOG = LoggerFactory.getLogger(AbstraktiPalvelukutsuStrategia.class);

    private final Set<Palvelukutsu> aloitetutPalvelukutsut;
    private final Queue<PalvelukutsuJaTakaisinkutsu> palvelukutsuJono;
    private volatile boolean kaikkiPeruutettu = false;

    public AbstraktiPalvelukutsuStrategia() {
        this.aloitetutPalvelukutsut = Sets.newConcurrentHashSet();
        this.palvelukutsuJono = Queues.newConcurrentLinkedQueue();
    }

    protected void kaynnistaJonossaSeuraavaPalvelukutsu() {
        if (kaikkiPeruutettu) {
            return;
        }
        final PalvelukutsuJaTakaisinkutsu seuraavaPalvelukutsu = palvelukutsuJono.poll();
        if (seuraavaPalvelukutsu != null) {
            Palvelukutsu kutsu = seuraavaPalvelukutsu.palvelukutsu;
            LOG.info("Aktivoidaan jonossa seuraava {}", kutsu.toString());
            aloitetutPalvelukutsut.add(kutsu);
            try {
                kutsu.teePalvelukutsu(palvelukutsu -> {
                    try {
                        aloitetutPalvelukutsut.remove(kutsu);
                    } catch (Exception e) {
                        LOG.error("Palvelustrategiassa aloitetun palvelukutsun poisto tyojonosta epaonnistui", e);
                        throw e;
                    }
                    try {
                        seuraavaPalvelukutsu.takaisinkutsu.accept(palvelukutsu);
                    } catch (Exception e) {
                        LOG.error("Palvelustrategiassa alkuperainen takaisinkutsu heitti poikkeuksen", e);
                    }
                });
            } catch (PalvelukutsunUudelleenAktivointiPoikkeus p) {
                aloitetutPalvelukutsut.remove(kutsu);
                throw p;
            } catch (Exception e) {
                LOG.error("Palvelukutsun kaynnistys heitti poikkeuksen", e);
                aloitetutPalvelukutsut.remove(kutsu);
                throw e;
            }
        }
    }

    public void peruutaKaikki() {
        kaikkiPeruutettu = true;
        aloitetutPalvelukutsut.forEach(a -> {
            try {
                a.peruuta();
            } catch (Exception ignored) {
            }
        });
    }

    protected int aloitettujaPalvelukutsuja() {
        return aloitetutPalvelukutsut.size();
    }

    public Set<Palvelukutsu> getAloitetutPalvelukutsut() {
        return aloitetutPalvelukutsut;
    }

    public Queue<PalvelukutsuJaTakaisinkutsu> getPalvelukutsuJono() {
        return palvelukutsuJono;
    }

    public void laitaPalvelukutsuJonoon(Palvelukutsu palvelukutsu, Consumer<Palvelukutsu> takaisinkutsu) {
        palvelukutsuJono.add(new PalvelukutsuJaTakaisinkutsu(palvelukutsu, takaisinkutsu));
    }

    public abstract void aloitaUusiPalvelukutsu();

    public static class PalvelukutsuJaTakaisinkutsu {
        private final Palvelukutsu palvelukutsu;
        private final Consumer<Palvelukutsu> takaisinkutsu;

        public PalvelukutsuJaTakaisinkutsu(Palvelukutsu palvelukutsu, Consumer<Palvelukutsu> takaisinkutsu) {
            this.palvelukutsu = palvelukutsu;
            this.takaisinkutsu = takaisinkutsu;
        }

        public Palvelukutsu getPalvelukutsu() {
            return palvelukutsu;
        }

        public Consumer<Palvelukutsu> getTakaisinkutsu() {
            return takaisinkutsu;
        }
    }

}
