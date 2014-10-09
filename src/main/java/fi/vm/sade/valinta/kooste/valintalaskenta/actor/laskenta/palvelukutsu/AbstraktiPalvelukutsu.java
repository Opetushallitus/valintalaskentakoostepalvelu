package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public abstract class AbstraktiPalvelukutsu implements Palvelukutsu {

	private final static Logger LOG = LoggerFactory
			.getLogger(AbstraktiPalvelukutsu.class);
	private final AtomicReference<Peruutettava> peruutettava = new AtomicReference<>();
	private final String hakukohdeOid;

	public AbstraktiPalvelukutsu(String hakukohdeOid) {
		this.hakukohdeOid = hakukohdeOid;
	}

	@Override
	public int compareTo(Palvelukutsu o) {
		return hakukohdeOid.compareTo(o.getHakukohdeOid());
	}

	public boolean onkoPeruutettu() {
		return TyhjaPeruutettava.tyhjaPeruutettava().equals(peruutettava.get());
	}

	public abstract void vapautaResurssit();

	protected Consumer<Throwable> failureCallback(
			final Consumer<Palvelukutsu> takaisinkutsu) {
		AbstraktiPalvelukutsu self = this;
		final AtomicBoolean K = new AtomicBoolean(false);
		return poikkeus -> {
			if (!K.compareAndSet(false, true)) {
				LOG.debug("Silmukka havaittu {}", self.getClass()
						.getSimpleName());
				return;
			}
			LOG.error("{} epaonnistui! {}", self.getClass().getSimpleName(),
					poikkeus.getMessage());
			try {
				self.peruuta();
				takaisinkutsu.accept(self);
			} catch (Exception e) {
				LOG.error("Takaisinkutsun teko epaonnistui {}! {}", self
						.getClass().getSimpleName(), e.getMessage());
			}
		};
	}

	/**
	 * Toimii niin kauan oikein kun peruutus tehdaan talla mekanismilla. Jos
	 * alkuperaista cancellablea kutsutaan muualta peruutetuksi niin se jaa
	 * huomaamatta
	 */
	public void peruuta() {
		peruutettava.updateAndGet(aiempi -> {
			if (aiempi != null) {
				// peruutetaan jos aito palvelukutsu meneillaan
				try {
					aiempi.peruuta();
				} catch (Exception e) {
					LOG.error("Palvelukutsun peruutus epaonnistui {}",
							e.getMessage());
				}
			}
			// palautetaan tyhjareferenssi koska ei tarvita enaa referenssia
			// palvelukutsuun ja peruutuksen tarkastus suoraviivaisempaa
			return TyhjaPeruutettava.tyhjaPeruutettava();
		});
	}

	/**
	 * @return true jos palvelukutsu aloitettiin
	 */
	protected boolean aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(
			Supplier<Peruutettava> palvelukutsunAloittavaFunktio) {
		if (peruutettava.getAndUpdate(aikaisempiArvo -> {
			if (aikaisempiArvo == null) {
				return palvelukutsunAloittavaFunktio.get();
			} else {
				return aikaisempiArvo;
			}
		}) != null) {
			LOG.error("Palvelukutsun uudelleen aktivointi poikkeus!");
			throw new PalvelukutsunUudelleenAktivointiPoikkeus();
		}
		return true;
	}

	public String getHakukohdeOid() {
		return hakukohdeOid;
	}
}
