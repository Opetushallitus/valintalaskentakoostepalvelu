package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;

/**
 * @author Jussi Jartamo
 */
public class ValintaryhmaLaskentaActorImpl implements LaskentaActor, Runnable {
    private final static Logger LOG = LoggerFactory.getLogger(ValintaryhmaLaskentaActorImpl.class);

    private final String uuid;
    private final String hakuOid;
    private final Collection<PalvelukutsuStrategia> strategiat;
    private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
    private final PalvelukutsuStrategia laskentaStrategia;
    private final LaskentaSupervisor laskentaSupervisor;
    private final AtomicBoolean valmis = new AtomicBoolean(false);

    public ValintaryhmaLaskentaActorImpl(
            LaskentaSupervisor laskentaSupervisor,
            String uuid,
            String hakuOid,
            LaskentaPalvelukutsu laskenta,
            Collection<PalvelukutsuStrategia> strategiat,
            PalvelukutsuStrategia laskentaStrategia,
            LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource
    ) {
        this.laskentaSupervisor = laskentaSupervisor;
        this.hakuOid = hakuOid;
        this.uuid = uuid;
        this.strategiat = strategiat;
        this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
        this.laskentaStrategia = laskentaStrategia;
        laskenta.laitaTyojonoon(pkk -> {
            LOG.error("Hakukohteen {} tila muuttunut statukseen {}. {}", pkk.getHakukohdeOid(), pkk.getHakukohdeTila(), tulkinta(pkk.getHakukohdeTila()));
            if (pkk.onkoPeruutettu()) {
                merkkaaLaskennanTila(uuid, laskentaSeurantaAsyncResource, pkk);
            } else {
                LOG.error("Aloitetaan valintaryhman laskenta!");
                laskentaStrategia.laitaPalvelukutsuJonoon(pkk, p -> merkkaaLaskennanTila(uuid, laskentaSeurantaAsyncResource, pkk));
                laskentaStrategia.aloitaUusiPalvelukutsu();
            }
        });
    }

    private void merkkaaLaskennanTila(String uuid, LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource, LaskentaPalvelukutsu pkk) {
        try {
            if (!viimeisteleLaskentaJaPalautaOlikoJoViimeistelty()) {
                try {
                    HakukohdeTila hakukohdetila = HakukohdeTila.VALMIS.equals(pkk.getHakukohdeTila()) ? HakukohdeTila.VALMIS : HakukohdeTila.KESKEYTETTY;
                    laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, hakukohdetila);
                } finally {
                    viimeistele();
                }
            }
        } catch (Exception e) {
            LOG.error("Laskennan tilan merkkaaminen epäonnistui {}", e.getMessage());
        }
    }

    @Override
    public void postStop() {
        if (!valmis.get()) {
            try {
                LOG.error("Actor {} sammutettiin ennen laskennan valmistumista joten merkataan laskenta peruutetuksi!", uuid);
                laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.PERUUTETTU);
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
        return valmis.get();
    }

    private boolean viimeisteleLaskentaJaPalautaOlikoJoViimeistelty() {
        return valmis.getAndSet(true);
    }

    public void aloita() {
        uudetPalvelukutsutKayntiin();
    }

    public void run() {
        uudetPalvelukutsutKayntiin();
    }

    private void uudetPalvelukutsutKayntiin() {
        strategiat.forEach(PalvelukutsuStrategia::aloitaUusiPalvelukutsu);
        laskentaStrategia.aloitaUusiPalvelukutsu();
    }

    public void viimeistele() {
        LOG.info(
                "\r\n####\r\n#### Valintaryhmälaskenta on paattynyt haussa {} uuid:lle {}!\r\n####",
                hakuOid, uuid);
        laskentaSupervisor.valmis(uuid);
    }

    public void lopeta() {
        try {
            strategiat.forEach(s -> {
                try {
                    s.peruutaKaikki();
                } catch (Exception e) {
                    LOG.error("Palvelukutsu Strategian peruutus epaonnistui! {}", e.getMessage());
                }
            });
            laskentaStrategia.peruutaKaikki();
        } catch (Exception e) {
            LOG.error("Virhe {}", e.getMessage());
        }
        try {
            laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid,
                    LaskentaTila.PERUUTETTU, HakukohdeTila.KESKEYTETTY);
        } catch (Exception e) {
            LOG.error("Virhe {}", e.getMessage());
        }
        viimeistele();
    }
}
