package fi.vm.sade.valinta.kooste.util;

import java.util.function.Consumer;

import rx.functions.Action1;

public class PoikkeusKasittelijaSovitin implements Consumer<Throwable>, Action1<Throwable> {
    private Consumer<Throwable> kasittelija;

    public PoikkeusKasittelijaSovitin(Consumer<Throwable> kasittelija) {
        this.kasittelija = kasittelija;
    }

    @Override
    public void call(final Throwable throwable) {
        accept(throwable);
    }

    @Override
    public void accept(final Throwable throwable) {
        kasittelija.accept(throwable);
    }
}
