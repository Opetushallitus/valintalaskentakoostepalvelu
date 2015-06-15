package fi.vm.sade.valinta.kooste;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

public class KoostepalveluEhto<T> implements Predicate {
    private final java.util.function.Predicate<T> ehto;

    public KoostepalveluEhto(java.util.function.Predicate<T> ehto) {
        this.ehto = ehto;
    }

    @SuppressWarnings("unchecked")
    public boolean matches(Exchange exchange) {
        T t = (T) exchange.getIn().getBody();
        if (t == null) {
            throw new RuntimeException(KoostepalveluContext.TYHJA_ARVO_POIKKEUS);
        }
        return ehto.test(t);
    }
}
