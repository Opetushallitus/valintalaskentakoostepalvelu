package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.AbstraktiPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.PalvelukutsuLaskuri;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategia;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;

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
		final PalvelukutsuLaskuri palvelukutsulaskuri = new PalvelukutsuLaskuri(
				palvelukutsut.size());
		this.laskuri = pk -> {
			yksiVaiheValmistui();
			if (takaisinkutsu.get() == null) {
				return;
			}
			if (pk.onkoPeruutettu()) { // peruutetaan laskenta talle
										// hakukohteelle
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
				peruuta();
			} else {
				int yhteensa = palvelukutsulaskuri.getYhteensa();
				int laskuriNyt = palvelukutsulaskuri.palvelukutsuSaapui();
				LOG.error("Saatiin {} hakukohteelle {}: {}/{}", pk.getClass()
						.getSimpleName(), getHakukohdeOid(), (-laskuriNyt)
						+ yhteensa, yhteensa);
				if (laskuriNyt == 0) {
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
				} else if (laskuriNyt < 0) {
					LOG.error("Laskenta sai enemman paluuarvoja palvelukutsuista kuin kutsuja tehtiin!");
					throw new RuntimeException(
							"Laskenta sai enemman paluuarvoja palvelukutsuista kuin kutsuja tehtiin!");
				}
			}
		};
	}

	/**
	 * Ylikirjoita callbackiksi
	 */
	protected void yksiVaiheValmistui() {

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

	protected List<HakemusDTO> muodostaHakemuksetDTO(List<Hakemus> hakemukset,
			List<Oppija> oppijat) {
		try {
			Map<String, String> hakemusOidToPersonOid = hakemukset.stream()
					.collect(
							Collectors.toMap(h -> h.getOid(),
									h -> h.getPersonOid()));

			List<HakemusDTO> hakemusDtot = hakemukset.parallelStream()
					.map(h -> Converter.hakemusToHakemusDTO(h))
					.collect(Collectors.toList());
			try {
				if (oppijat != null) {
					Map<String, Oppija> oppijaNumeroJaOppija = oppijat.stream()
							.collect(
									Collectors.toMap(o -> o.getOppijanumero(),
											o -> o));
					hakemusDtot
							.forEach(h -> {
								String personOid = hakemusOidToPersonOid.get(h
										.getHakemusoid());
								if (personOid != null
										&& oppijaNumeroJaOppija
												.containsKey(personOid)) {
									Oppija oppija = oppijaNumeroJaOppija.get(personOid);
									h.getAvaimet().addAll(
											OppijaToAvainArvoDTOConverter
													.convert(oppija));
								}
							});
				}
			} catch (Exception e) {
				LOG.error(
						"\r\n###\r\n### SURE YO-arvosanojen konversiossa odottamaton virhe {}\r\n###",
						e.getMessage());
			}
			return hakemusDtot;
		} catch (Exception exx) {
			LOG.error(
					"Hakemusten konvertointi laskennan hakemusDTO:ksi epaonnistui {}!",
					exx.getMessage());

			throw exx;
		}

	}
}
