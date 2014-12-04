package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Queues;

import akka.actor.TypedActor;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
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
	private final Collection<PalvelukutsuStrategia> strategiat;
	private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
	private final PalvelukutsuStrategia laskentaStrategia;
	private final LaskentaSupervisor laskentaSupervisor;
	private final HakukohdeLaskuri hakukohdeLaskuri;

	public LaskentaActorImpl(LaskentaSupervisor laskentaSupervisor,
			String uuid, String hakuOid,
			Collection<LaskentaPalvelukutsu> palvelukutsut,
			Collection<PalvelukutsuStrategia> strategiat,
			PalvelukutsuStrategia laskentaStrategia,
			LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource) {
		this.hakukohdeLaskuri = new HakukohdeLaskuri(palvelukutsut.size());
		this.laskentaSupervisor = laskentaSupervisor;
		this.hakuOid = hakuOid;
		this.uuid = uuid;
		this.strategiat = strategiat;
		this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
		this.laskentaStrategia = laskentaStrategia;
		palvelukutsut.forEach(pk -> pk.laitaTyojonoon(pkk -> {
			LOG.error("Hakukohteen {} tila muuttunut statukseen {}. {}",
					pkk.getHakukohdeOid(), pkk.getHakukohdeTila(),
					tulkinta(pkk.getHakukohdeTila()));
			if (pkk.onkoPeruutettu()) {
				try {
					laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid,
							pkk.getHakukohdeOid(), pkk.getHakukohdeTila());
				} catch (Exception e) {
					LOG.error("Virhe {}", e.getMessage());
				}
				if (hakukohdeLaskuri.done(pkk.getHakukohdeOid())) {
					viimeisteleLaskenta();
					return;
				}
			} else {
				laskentaStrategia.laitaPalvelukutsuJonoon(pkk, p -> {
					try {
						laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(
								uuid, pkk.getHakukohdeOid(),
								pkk.getHakukohdeTila());
					} catch (Exception e) {
						LOG.error("Virhe {}", e.getMessage());
					}
					if (hakukohdeLaskuri.done(pkk.getHakukohdeOid())) {
						viimeisteleLaskenta();
						return;
					}
					uudetPalvelukutsutKayntiin();
				});
			}
			uudetPalvelukutsutKayntiin();
		}));
	}
	@Override
	public void postStop() {
		if(!hakukohdeLaskuri.isDone()) {
			try {
				LOG.error("Actor {} sammutettiin ennen laskennan valmistumista joten merkataan laskenta peruutetuksi!", uuid);
				laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid,
						LaskentaTila.PERUUTETTU);
			} catch (Exception e) {
				LOG.error("Virhe {}", e.getMessage());
			}
		}
	}
	private String tulkinta(HakukohdeTila tila) {
		if (HakukohdeTila.VALMIS.equals(tila)) {
			return "Seuraavaksi suoritetaan hakukohteelle laskentakutsu!";
		} else if (HakukohdeTila.KESKEYTETTY.equals(tila)) {
			return "Merkataan laskenta ohitetuksi seurantapalveluun! Laskentakutsua ei tehda koska tarvittavia resursseja ei saatu!";
		}
		return StringUtils.EMPTY;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public boolean isValmis() {
		return hakukohdeLaskuri.isDone();
	}

	private void viimeisteleLaskenta() {
		try {
			laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid,
					LaskentaTila.VALMIS);
		} catch (Exception e) {
			LOG.error(
					"\r\n####\r\n#### Laskenta paattynyt {} hakukohteelle haussa {} mutta kayttoliittymaa ei saatu paivitettya!\r\n####",
					hakukohdeLaskuri.getYhteensa(), hakuOid);
		}
		try {
			laskentaSupervisor.valmis(uuid);
			LOG.error(
					"\r\n####\r\n#### Laskenta paattynyt {} hakukohteelle haussa {} uuid:lle {}!\r\n####",
					hakukohdeLaskuri.getYhteensa(), hakuOid, uuid);
		} catch (Exception e) {
			LOG.error(
					"\r\n####\r\n#### Laskenta paattynyt {} hakukohteelle haussa {} mutta Actoria ei saatu pysaytettya {}!\r\n####",
					hakukohdeLaskuri.getYhteensa(), hakuOid, uuid);
		}
	}

	public void aloita() {
		uudetPalvelukutsutKayntiin();
	}

	private void uudetPalvelukutsutKayntiin() {
		strategiat.forEach(s -> s.aloitaUusiPalvelukutsu());
		laskentaStrategia.aloitaUusiPalvelukutsu();
	}

	public void lopeta() {
		try {
			strategiat.forEach(s -> {
				try {
					s.peruutaKaikki();
				} catch (Exception e) {
					LOG.error(
							"Palvelukutsu Strategian peruutus epaonnistui! {}",
							e.getMessage());
				}
			});
			laskentaStrategia.peruutaKaikki();
		} catch (Exception e) {
			LOG.error("Virhe {}", e.getMessage());
		}
		try {
			laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid,
					LaskentaTila.PERUUTETTU);
		} catch (Exception e) {
			LOG.error("Virhe {}", e.getMessage());
		}
		try {
			laskentaSupervisor.valmis(uuid);
		} catch (Exception e) {
			LOG.error("Virhe {}", e.getMessage());
		}
	}
}
