package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia;

import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.PalvelukutsunUudelleenAktivointiPoikkeus;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public abstract class AbstraktiPalvelukutsuStrategia implements
		PalvelukutsuStrategia {
	private static final Logger LOG = LoggerFactory
			.getLogger(AbstraktiPalvelukutsuStrategia.class);
	private final Set<Palvelukutsu> aloitetutPalvelukutsut;
	private final Queue<PalvelukutsuJaTakaisinkutsu> palvelukutsuJono;

	// private final Set<String> done = Sets.newConcurrentHashSet();

	public AbstraktiPalvelukutsuStrategia() {
		this.aloitetutPalvelukutsut = Sets.newConcurrentHashSet();
		this.palvelukutsuJono = Queues.newConcurrentLinkedQueue();
	}

	protected void kaynnistaJonossaSeuraavaPalvelukutsu() {
		final PalvelukutsuJaTakaisinkutsu seuraavaPalvelukutsu = palvelukutsuJono
				.poll();
		if (seuraavaPalvelukutsu != null) {
			LOG.info("Aktivoidaan jonossa seuraava palvelukutsu.");
			// seuraavaPalvelukutsu.aloita();
			aloitetutPalvelukutsut.add(seuraavaPalvelukutsu.palvelukutsu);
			try {

				seuraavaPalvelukutsu.palvelukutsu
						.teePalvelukutsu(palvelukutsu -> {
							try {
								aloitetutPalvelukutsut
										.remove(seuraavaPalvelukutsu.palvelukutsu);
							} catch (Exception e) {
								LOG.error(
										"Palvelustrategiassa aloitetun palvelukutsun poisto tyojonosta epaonnistui {}",
										e.getMessage());
								throw e;
							}
							try {
								seuraavaPalvelukutsu.takaisinkutsu
										.accept(palvelukutsu);
							} catch (Exception e) {
								LOG.error(
										"Palvelustrategiassa alkuperainen takaisinkutsu heitti poikkeuksen {}",
										e.getMessage());
							}
						});

			} catch (Exception e) {
				LOG.error("Palvelukutsun kaynnistys heitti poikkeuksen: {}", e
						.getClass().getSimpleName());
				aloitetutPalvelukutsut
						.remove(seuraavaPalvelukutsu.palvelukutsu);
				throw e;
			}
		}
	}

	protected int aloitettujaPalvelukutsuja() {
		return aloitetutPalvelukutsut.size();
	}

	/**
	 * Yksikkotesteja varten
	 */
	public Set<Palvelukutsu> getAloitetutPalvelukutsut() {
		return aloitetutPalvelukutsut;
	}

	public Queue<PalvelukutsuJaTakaisinkutsu> getPalvelukutsuJono() {
		return palvelukutsuJono;
	}

	public void laitaPalvelukutsuJonoon(Palvelukutsu palvelukutsu,
			Consumer<Palvelukutsu> takaisinkutsu) {
		palvelukutsuJono.add(new PalvelukutsuJaTakaisinkutsu(palvelukutsu,
				takaisinkutsu));
	}

	public abstract void aloitaUusiPalvelukutsu();

	public static class PalvelukutsuJaTakaisinkutsu {
		private final Palvelukutsu palvelukutsu;
		private final Consumer<Palvelukutsu> takaisinkutsu;

		public PalvelukutsuJaTakaisinkutsu(Palvelukutsu palvelukutsu,
				Consumer<Palvelukutsu> takaisinkutsu) {
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
