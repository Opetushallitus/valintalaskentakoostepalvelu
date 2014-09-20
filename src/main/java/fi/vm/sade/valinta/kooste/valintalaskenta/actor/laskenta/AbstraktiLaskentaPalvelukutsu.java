package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.AbstraktiPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategia;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Abstrakti laskenta tekee tilapaivitykset ja palvelukutsut.
 *         Varsinainen laskennan toteuttaja tekee laskennan ja laskentaDTO:n
 *         luonnin.
 * 
 */
public abstract class AbstraktiLaskentaPalvelukutsu extends
		AbstraktiPalvelukutsu implements LaskentaPalvelukutsu {
	private final Logger LOG = LoggerFactory
			.getLogger(AbstraktiLaskentaPalvelukutsu.class);
	private final Collection<PalvelukutsuJaPalvelukutsuStrategia> palvelukutsut;
	private final Consumer<Palvelukutsu> laskuri;
	private volatile HakukohdeTila tila = HakukohdeTila.TEKEMATTA;
	private final AtomicReference<Consumer<LaskentaPalvelukutsu>> takaisinkutsu;

	public AbstraktiLaskentaPalvelukutsu(String hakukohdeOid,
			Collection<PalvelukutsuJaPalvelukutsuStrategia> palvelukutsut) {
		super(hakukohdeOid);
		this.palvelukutsut = palvelukutsut;
		this.takaisinkutsu = new AtomicReference<>();
		final AtomicInteger counter = new AtomicInteger(palvelukutsut.size());
		this.laskuri = pk -> {
			if (pk.onkoPeruutettu()) { // peruutetaan laskenta talle
										// hakukohteelle
				//
				try {
					peruuta();
					takaisinkutsu.getAndUpdate(tk -> {
						if (tk != null) {
							tk.accept(AbstraktiLaskentaPalvelukutsu.this);
						}
						return null;
					});

				} catch (Exception e) {
					LOG.error(
							"Laskentapalvelukutsun takaisinkutsu epaonnistui {}",
							e.getMessage());
					throw e;
				}
			} else {
				int i = counter.decrementAndGet();
				LOG.error("Saatiin palvelukutsu hakukohteelle {}: {}/{}",
						getHakukohdeOid(), i, palvelukutsut.size());
				if (i == 0) {
					tila = HakukohdeTila.VALMIS;
					try {
						takaisinkutsu.getAndUpdate(tk -> {
							if (tk != null) {
								tk.accept(AbstraktiLaskentaPalvelukutsu.this);
							}
							return null;
						});
					} catch (Exception e) {
						LOG.error(
								"Laskentapalvelukutsun takaisinkutsu epaonnistui {}",
								e.getMessage());
						throw e;
					}
				} else if (i < 0) {
					LOG.error("Laskenta sai enemman paluuarvoja palvelukutsuista kuin kutsuja tehtiin!");
					throw new RuntimeException(
							"Laskenta sai enemman paluuarvoja palvelukutsuista kuin kutsuja tehtiin!");
				}
			}
		};
	}

	/**
	 * Peruuttaa kaikki palvelukutsut esitoina talle palvelukutsulle.
	 */
	@Override
	public void peruuta() {
		try {
			super.peruuta();
			palvelukutsut.forEach(pk -> pk.peruuta());
		} catch (Exception e) {
			LOG.error(
					"AbstraktiLaskentaPalvelukutsun peruutus epaonnistui! {}",
					e.getMessage());
		}
	}

	@Override
	public HakukohdeTila getHakukohdeTila() {
		if (onkoPeruutettu()) {
			return HakukohdeTila.KESKEYTETTY;
		}
		return tila;
	}

	public void laitaTyojonoon(Consumer<LaskentaPalvelukutsu> takaisinkutsu) {
		LOG.info("Laitetaan tyot tyojonoon hakukohteelle {}", getHakukohdeOid());
		this.takaisinkutsu.set(takaisinkutsu);
		palvelukutsut.forEach(tyojono -> tyojono
				.laitaPalvelukutsuTyojonoon(laskuri));
	}
}
