package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

public class HakemuksetPalvelukutsu extends AbstraktiPalvelukutsu implements Palvelukutsu {
    private final static Logger LOG = LoggerFactory.getLogger(HakemuksetPalvelukutsu.class);
    private final String hakuOid;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtomicReference<List<Hakemus>> hakemukset;

    public HakemuksetPalvelukutsu(String hakuOid, UuidHakukohdeJaOrganisaatio hakukohdeOid, ApplicationAsyncResource applicationAsyncResource) {
        super(hakukohdeOid);
        this.hakuOid = hakuOid;
        this.applicationAsyncResource = applicationAsyncResource;
        this.hakemukset = new AtomicReference<>();
    }

    @Override
    public void vapautaResurssit() {
        hakemukset.set(null);
    }

    public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
        aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(() -> toPeruutettava(applicationAsyncResource.getApplicationsByOid(hakuOid, getHakukohdeOid())
                .retryWhen(createRetryer())
                .subscribe(hakemukset -> {
                    if (hakemukset == null) {
                        LOG.error("Hakemuksetpalvelu palautti null datajoukon!");
                        failureCallback(takaisinkutsu);
                    } else {
                        HakemuksetPalvelukutsu.this.hakemukset.set(hakemukset);
                        takaisinkutsu.accept(HakemuksetPalvelukutsu.this);
                    }
                }, failureCallback(takaisinkutsu))));
        return this;
    }

    private Func1<Observable<? extends Throwable>, Observable<?>> createRetryer() {
        int maxRetries = 2;
        int secondsToWaitMultiplier = 5;
        return errors -> errors.zipWith(Observable.range(1, maxRetries), (n, i) -> i).flatMap(i -> {
            int delaySeconds = secondsToWaitMultiplier * i;
            LOG.warn(toString() + " retry number " + i + "/" + maxRetries + ", waiting for " + delaySeconds + " seconds.");
            return Observable.timer(delaySeconds, TimeUnit.SECONDS);
        });
    }

    private static Peruutettava toPeruutettava(final Subscription subscription) {
        return new Peruutettava() {
            @Override
            public void peruuta() {
                subscription.unsubscribe();
            }
        };
    }

    public List<Hakemus> getHakemukset() {
        List<Hakemus> h = hakemukset.get();
        if (h == null) {
            LOG.error("Hakemukset palvelu palautti null joukon hakukohteelle {}", getHakukohdeOid());
            throw new RuntimeException("Hakemukset palvelu palautti null joukon hakukohteelle " + getHakukohdeOid());
        }
        return h;
    }
}
