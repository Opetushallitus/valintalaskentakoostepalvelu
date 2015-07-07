package fi.vm.sade.valinta.kooste;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KoostepalveluFunktio<IN, OUT> implements Processor {
    private final static Logger LOG = LoggerFactory.getLogger(KoostepalveluFunktio.class);
    private final Function<IN, OUT> tyo;
    private final BiPredicate<IN, Exception> virhekasittelija;
    private final Function<IN, OUT> oletusluoja;

    public KoostepalveluFunktio(Function<IN, OUT> tyo) {
        this.tyo = tyo;
        this.virhekasittelija = null;
        this.oletusluoja = null;
    }

    public KoostepalveluFunktio(Function<IN, OUT> tyo, BiPredicate<IN, Exception> virhekasittelija, Function<IN, OUT> oletusluoja) {
        this.tyo = tyo;
        this.virhekasittelija = virhekasittelija;
        this.oletusluoja = oletusluoja;
    }

    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) throws Exception {
        IN i = (IN) exchange.getIn().getBody();
        if (i == null) {
            throw new RuntimeException(KoostepalveluContext.TYHJA_ARVO_POIKKEUS);
        }
        try {
            exchange.getOut().setBody(tyo.apply(i));
        } catch (Exception e) {
            if (virhekasittelija != null && virhekasittelija.test(i, e)) {
                if (oletusluoja == null) {
                    throw e;
                } else {
                    try {
                        exchange.getOut().setBody(oletusluoja.apply(i));
                    } catch (Exception ee) {
                        LOG.error("Poikkeuksen kasittelijasta lensi poikkeus:", ee);
                        throw ee;
                    }
                    return;
                }
            }
            throw e;
        }
    }
}
