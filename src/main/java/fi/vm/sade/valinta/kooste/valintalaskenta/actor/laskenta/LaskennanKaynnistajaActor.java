package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import akka.actor.Props;
import akka.actor.UntypedActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

final public class LaskennanKaynnistajaActor extends UntypedActor {
    private final Logger LOG = LoggerFactory.getLogger(LaskennanKaynnistajaActor.class);

    private final LaskentaSupervisor laskentaSupervisor;
    private final int maxWorkers;
    private AtomicInteger workerCount = new AtomicInteger(0);

    private LaskennanKaynnistajaActor(final LaskentaSupervisor laskentaSupervisor, final int maxWorkers) {
        this.laskentaSupervisor = laskentaSupervisor;
        this.maxWorkers = maxWorkers;
        LOG.info("Creating LaskennanKaynnistajaActor with maxWorkerCount {}", maxWorkers);
    }

    public static Props props(final LaskentaSupervisor laskentaSupervisor, final int maxWorkers) {
        return Props.create(LaskennanKaynnistajaActor.class, () -> new LaskennanKaynnistajaActor(laskentaSupervisor, maxWorkers));
    }

    @Override
    public void onReceive(Object message) {
        if (WorkAvailable.class.isInstance(message)) {
            process();
        } else if (WorkerAvailable.class.isInstance(message)) {
            workerCount.decrementAndGet();
            process();
        } else if (NoWorkAvailable.class.isInstance(message)) {
            workerCount.decrementAndGet();
        }
    }

    void process() {
        LOG.info("Process; maxWorkers: {}, workerCount: {}", maxWorkers, workerCount.get());
        if (workerCount.get() < maxWorkers) {
            int numberOfWorkers = workerCount.incrementAndGet();
            LOG.info("Reserving a new worker, workerCount: {}", numberOfWorkers);
            laskentaSupervisor.haeJaKaynnistaLaskenta();
        }
    }
}
