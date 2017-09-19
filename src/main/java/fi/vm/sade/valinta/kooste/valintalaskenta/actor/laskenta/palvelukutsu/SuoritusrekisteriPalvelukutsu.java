package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakuUuidHakukohdeJaOrganisaatio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SuoritusrekisteriPalvelukutsu extends AbstraktiPalvelukutsu implements Palvelukutsu {
    private final static Logger LOG = LoggerFactory.getLogger(HakijaryhmatPalvelukutsu.class);
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private final AtomicReference<List<Oppija>> oppijat;
    private final HakuV1RDTO haku;

    public SuoritusrekisteriPalvelukutsu(HakuUuidHakukohdeJaOrganisaatio hakuData, SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
        super(hakuData);
        this.haku = hakuData.getHaku();
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.oppijat = new AtomicReference<>();
    }

    @Override
    public void vapautaResurssit() {
        oppijat.set(null);
    }


    public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
        aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(() -> toPeruutettava(suoritusrekisteriAsyncResource.getOppijatByHakukohde(getHakukohdeOid(), haku.getOid())
                .retryWhen(errors -> errors.zipWith(Observable.range(1,2), (n, i) -> i)
                        .flatMap(i -> Observable.timer(5*i, TimeUnit.SECONDS)))
                .subscribe(oppijat -> {
                    if(oppijat == null) {
                        LOG.error("Tietojen haku suoritusrekisteristä epäonnistui.");
                        failureCallback(takaisinkutsu);
                    } else {
                        SuoritusrekisteriPalvelukutsu.this.oppijat.set(oppijat);
                        takaisinkutsu.accept(SuoritusrekisteriPalvelukutsu.this);
                    }
                }, failureCallback(takaisinkutsu)) ));
        return this;
    }

    private static Peruutettava toPeruutettava(final Subscription subscription) {
        return new Peruutettava() {
            @Override
            public void peruuta() {
                subscription.unsubscribe();
            }
        };
    }


    public List<Oppija> getOppijat() {
        return oppijat.get();
    }
}