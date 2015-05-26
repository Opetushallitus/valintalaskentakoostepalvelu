package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import akka.actor.Props;
import akka.actor.UntypedActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

final public class LaskentaStarterActor extends UntypedActor {
    private final Logger LOG = LoggerFactory.getLogger(LaskentaStarterActor.class);

    private final LaskentaSupervisor laskentaSupervisor;
    private final int maxWorkers;
    private AtomicInteger workerCount = new AtomicInteger(0);

    private LaskentaStarterActor(final LaskentaSupervisor laskentaSupervisor, final int maxWorkers) {
        this.laskentaSupervisor = laskentaSupervisor;
        this.maxWorkers = maxWorkers;
        LOG.info("Creating LaskentaStarterActor with maxWorkerCount {}", maxWorkers);
    }

    public static Props props(final LaskentaSupervisor laskentaSupervisor, final int maxWorkers) {
        return Props.create(LaskentaStarterActor.class, () -> new LaskentaStarterActor(laskentaSupervisor, maxWorkers));
    }

    @Override
    public void onReceive(Object message) {
        if (WorkAvailable.class.isInstance(message)) {
            startLaskentaIfWorkersAvailable();
        } else if (WorkerAvailable.class.isInstance(message)) {
            decrementWorkerCount();
            startLaskentaIfWorkersAvailable();
        } else if (NoWorkAvailable.class.isInstance(message)) {
            decrementWorkerCount();
        }
    }

    private void startLaskentaIfWorkersAvailable() {
        LOG.info("Process; maxWorkers: {}, workerCount: {}", maxWorkers, workerCount.get());
        if (workerCount.get() < maxWorkers) {
            int numberOfWorkers = workerCount.incrementAndGet();
            LOG.info("Reserving a new worker, workerCount: {}", numberOfWorkers);
            laskentaSupervisor.fetchAndStartLaskenta(this.getSelf());
        }
    }

    public int getWorkerCount() {
        return workerCount.get();
    }

    private void decrementWorkerCount() {
        workerCount.updateAndGet(i -> i > 0 ? i - 1 : i);
    }

    public static class WorkAvailable {}

    public static class NoWorkAvailable {}

    public static class WorkerAvailable {}
}
