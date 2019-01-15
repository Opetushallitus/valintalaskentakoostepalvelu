package fi.vm.sade.valinta.kooste.util;

import java.util.function.Consumer;

public class PoikkeusKasittelijaSovitin implements Consumer<Throwable>, io.reactivex.functions.Consumer<Throwable> {
    private Consumer<Throwable> kasittelija;

    public PoikkeusKasittelijaSovitin(Consumer<Throwable> kasittelija) {
        this.kasittelija = kasittelija;
    }

    @Override
    public void accept(final Throwable throwable) {
        kasittelija.accept(throwable);
    }
}
