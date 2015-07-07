package fi.vm.sade.valinta.kooste;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.component.bean.BeanInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KoostepalveluLauseke<IN, OUT> implements Expression {
    private final static Logger LOG = LoggerFactory.getLogger(KoostepalveluLauseke.class);
    private final Function<IN, OUT> lauseke;
    private final BiPredicate<IN, Exception> virhekasittelija;
    private final OUT vakio;
    private final BiConsumer<IN, Exception> virheloggaus;

    public KoostepalveluLauseke(Function<IN, OUT> lauseke) {
        this.lauseke = lauseke;
        this.virhekasittelija = null;
        this.virheloggaus = null;
        this.vakio = null;
    }

    public KoostepalveluLauseke(Function<IN, OUT> lauseke,
                                BiPredicate<IN, Exception> virhekasittelija, OUT vakio) {
        this.lauseke = lauseke;
        this.virhekasittelija = virhekasittelija;
        this.virheloggaus = null;
        this.vakio = vakio;
    }

    public KoostepalveluLauseke(Function<IN, OUT> lauseke,
                                BiConsumer<IN, Exception> virheloggaus) {
        this.lauseke = lauseke;
        this.virheloggaus = virheloggaus;
        this.virhekasittelija = null;
        this.vakio = null;
    }

    @SuppressWarnings("unchecked")
    public Object evaluate(Exchange exchange,
                           @SuppressWarnings("rawtypes") Class type) {
        IN i = (IN) exchange.getIn().getBody();
        if (i == null) {
            throw new RuntimeException(KoostepalveluContext.TYHJA_ARVO_POIKKEUS);
        }
        try {
            return lauseke.apply(i);
        } catch (Exception e) {
            try {
                if (virheloggaus != null) {
                    virheloggaus.accept(i, e);
                    throw e;
                } else if (virhekasittelija != null && !virhekasittelija.test(i, e)) {
                    throw e;
                }
            } catch (Exception ee) {
                LOG.error("Poikkeuksen kasittelijasta lensi poikkeus:", ee);
                throw ee;
            }
            return vakio;
        }
    }
}
