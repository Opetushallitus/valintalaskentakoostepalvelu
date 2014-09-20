package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.TypedActor;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class LaskentaActorImpl implements LaskentaActor {
	private final static Logger LOG = LoggerFactory
			.getLogger(LaskentaActorImpl.class);
	private final String uuid;
	private final String hakuOid;
	private final Collection<LaskentaPalvelukutsu> palvelukutsut;
	private final Collection<PalvelukutsuStrategia> strategiat;
	private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
	private volatile boolean valmis = false;
	private final PalvelukutsuStrategia laskentaStrategia;

	public LaskentaActorImpl(String uuid, String hakuOid,
			Collection<LaskentaPalvelukutsu> palvelukutsut,
			Collection<PalvelukutsuStrategia> strategiat,
			PalvelukutsuStrategia laskentaStrategia,
			LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource) {
		this.hakuOid = hakuOid;
		this.uuid = uuid;
		this.palvelukutsut = palvelukutsut;
		this.strategiat = strategiat;
		this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
		this.laskentaStrategia = laskentaStrategia;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public boolean isValmis() {
		return valmis;
	}

	private void tarkista(AtomicInteger counter, LaskentaPalvelukutsu pkk) {
		int now = counter.decrementAndGet();
		LOG.error("Hakukohde {} valmistui statuksella {}. {}/{}",
				pkk.getHakukohdeOid(), pkk.getHakukohdeTila(), now,
				palvelukutsut.size());
		if (now == 0) {
			LOG.warn("Laskenta valmistui {} hakukohteelle haussa {}",
					palvelukutsut.size());
			valmis = true;
			laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid,
					LaskentaTila.VALMIS);
		} else if (now < 0) {
			LOG.error(
					"Laskennassa syntyi enemman palvelukutsujen paluuviesteja kuin laskennassa oli hakukohteita! Hakukohteita oli {} ja ylimaaraisia paluuviesteja {}",
					palvelukutsut.size(), -now);
			valmis = true;
		}
	}

	public void aloita() {
		final AtomicInteger counter = new AtomicInteger(palvelukutsut.size());
		palvelukutsut.forEach(pk -> pk.laitaTyojonoon(pkk -> {
			LOG.error("Hakukohteen {} laskenta valmistui! {}",
					pkk.getHakukohdeOid(), pkk.onkoPeruutettu());
			if (pkk.onkoPeruutettu()) {
				tarkista(counter, pkk);
				laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid,
						pkk.getHakukohdeOid(), pkk.getHakukohdeTila());
			} else {
				laskentaStrategia.laitaPalvelukutsuJonoon(pkk, p -> {
					laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid,
							pkk.getHakukohdeOid(), pkk.getHakukohdeTila());
					tarkista(counter, pkk);
					uudetPalvelukutsutKayntiin();
				});
			}
			uudetPalvelukutsutKayntiin();
		}));
		uudetPalvelukutsutKayntiin();
	}

	private void uudetPalvelukutsutKayntiin() {
		strategiat.forEach(s -> s.aloitaUusiPalvelukutsu());
		laskentaStrategia.aloitaUusiPalvelukutsu();
	}

	public void lopeta() {
		palvelukutsut.forEach(kutsu -> {
			kutsu.peruuta();
		});
	}
}
