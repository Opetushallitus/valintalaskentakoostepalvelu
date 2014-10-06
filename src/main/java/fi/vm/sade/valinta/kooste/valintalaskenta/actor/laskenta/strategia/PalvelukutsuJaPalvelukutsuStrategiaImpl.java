package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class PalvelukutsuJaPalvelukutsuStrategiaImpl<T extends Palvelukutsu>
		implements PalvelukutsuJaPalvelukutsuStrategia {
	private final static Logger LOG = LoggerFactory
			.getLogger(PalvelukutsuJaPalvelukutsuStrategiaImpl.class);
	private final PalvelukutsuStrategia palvelukutsuStrategia;
	private final T palvelukutsu;

	public PalvelukutsuJaPalvelukutsuStrategiaImpl(T palvelukutsu,
			PalvelukutsuStrategia palvelukutsuStrategia) {
		this.palvelukutsuStrategia = palvelukutsuStrategia;
		this.palvelukutsu = palvelukutsu;
	}

	public T get() {
		return palvelukutsu;
	}

	public void laitaPalvelukutsuTyojonoon(Consumer<Palvelukutsu> takaisinkutsu) {
		palvelukutsuStrategia.laitaPalvelukutsuJonoon(palvelukutsu,
				takaisinkutsu);
	}

	public void peruuta() {
		if (!palvelukutsu.onkoPeruutettu()) {
			try {
				palvelukutsu.peruuta();
			} catch (Exception e) {
				LOG.error(
						"Laskennanpalvelukutsustakasin aloitettu alipalvelukutsujen peruutu epaonnistui palvelukutsulle {} syysta {}",
						palvelukutsu.getClass().getSimpleName(), e.getMessage());
			}
		}

	}

}
