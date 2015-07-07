package fi.vm.sade.valinta.kooste;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *         Jos BiPredicate palauttaa true niin poikkeus on kasitelty
 */
public class KoostepalveluKuluttaja<IN> implements Processor {
    private final static Logger LOG = LoggerFactory.getLogger(KoostepalveluKuluttaja.class);
    private final Consumer<IN> kuluttaja;
    private final BiPredicate<IN, Exception> virhekasittelija;
    private final int retries;
    private final int delay;

    public KoostepalveluKuluttaja(Consumer<IN> kuluttaja) {
        this.kuluttaja = kuluttaja;
        this.virhekasittelija = null;
        this.retries = 0;
        this.delay = 0;
    }

    public KoostepalveluKuluttaja(Consumer<IN> kuluttaja, BiPredicate<IN, Exception> virhekasittelija) {
        this.kuluttaja = kuluttaja;
        this.virhekasittelija = virhekasittelija;
        this.retries = 0;
        this.delay = 0;
    }

    public KoostepalveluKuluttaja(Consumer<IN> kuluttaja, BiPredicate<IN, Exception> virhekasittelija, int retries, int delay) {
        this.kuluttaja = kuluttaja;
        this.virhekasittelija = virhekasittelija;
        this.retries = retries;
        this.delay = delay;
    }

    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) throws Exception {
        IN i = (IN) exchange.getIn().getBody();
        if (i == null) {
            throw new RuntimeException(KoostepalveluContext.TYHJA_ARVO_POIKKEUS);
        }

        for (int i0 = 0; i0 <= retries; ++i0) {
            try {
                kuluttaja.accept(i);
                break;
            } catch (Exception e) {
                if (i0 == retries) {
                    try {
                        if (virhekasittelija != null && !virhekasittelija.test(i, e)) {
                            throw e;
                        }
                    } catch (Exception ee) {
                        LOG.error("Poikkeuksen kasittelijasta lensi poikkeus:", ee);
                        throw ee;
                    }
                }
                Thread.sleep(delay);
            }
        }
    }
}
